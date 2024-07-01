from flask import Flask, jsonify, request
from flask_sock import Sock
import subprocess
import os
import signal
import atexit
import threading
import logging
import time
import cv2
import numpy as np
import copy
import json
import RPi.GPIO as GPIO

# Inicializamos la aplicación Flask y el WebSocket
app = Flask(__name__)
sock = Sock(app)

# Inicializamos el pin deseado para la alarma
alarm_pin = 2

# Configuramos logging para mensajes de advertencia
logging.basicConfig(level=logging.WARNING, format='%(asctime)s - %(levelname)s - %(message)s')

# Lista global de cámaras
cameras = []

# Diccionario para los procesos de transmisión
processes = {}

# Clientes conectado al WebSocket para escuchar notificaciones
clients = []

# Controla si queremos seguir detectando movimiento
motion_detection_active = True

# Ruta del archivo para guardar la lista de cámaras
camera_file = 'cameras.json'

# Función para guardar la lista de cámaras para el siguiente inicio
def save_cameras():
    with open(camera_file, 'w') as f:
        json.dump(cameras, f)
    logging.debug("Cameras saved to file.")

# Función para cargar la lista de cámaras si es que están guardadas
def load_cameras():
    global cameras
    if os.path.exists(camera_file):
        with open(camera_file, 'r') as f:
            cameras = json.load(f)
        logging.debug("Cameras loaded from file.")
    else:
        logging.debug("No cameras file found, starting with default cameras.")

# Ruta WebSocket para manejar conexiones de clientes para detección de movimiento
@sock.route('/ws')
def websocket(ws):
    clients.append(ws)
    logging.debug("New WebSocket client connected.")
    try:
        while True:
            data = ws.receive()
            if data is None:
                break
    finally:
        clients.remove(ws)
        logging.debug("WebSocket client disconnected.")

# Función para notificar a los clientes WebSocket
def notify_clients(message):
    disconnected_clients = []
    for ws in clients:
        try:
            ws.send(message)
        except Exception as e:
            logging.error(f"Failed to send message to client: {e}")
            disconnected_clients.append(ws)
    
    # Remove disconnected clients
    for ws in disconnected_clients:
        clients.remove(ws)
    logging.debug(f"Notified clients with: {message}")

# Función para detectar movimiento de una cámara
def detect_motion(camera):
    # Abrimos el stream de video de la cámara
    cap = cv2.VideoCapture(camera['ip'])
    if not cap.isOpened():
        logging.error(f"Failed to open video of {camera['ip']}")
        return False

    # Leemos los primeros dos frames saltandonos un par
    ret, frame1 = cap.read()
    cap.grab()
    cap.grab()
    ret, frame2 = cap.read()

    height, width, _ = frame1.shape
    
    # Definimos la región de interés verticalmente
    roi_y1 = int(height * 0.1)
    roi_y2 = int(height * 0.9)

    while motion_detection_active:
        # Calculamos la diferencia cada dos frames
        diff = cv2.absdiff(frame1, frame2)
        gray = cv2.cvtColor(diff, cv2.COLOR_BGR2GRAY)
        blur = cv2.GaussianBlur(gray, (5, 5), 0)
        _, thresh = cv2.threshold(blur, 20, 255, cv2.THRESH_BINARY)
        dilated = cv2.dilate(thresh, None, iterations=3)
        contours, _ = cv2.findContours(dilated, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)

        for contour in contours:
            # Si el contorno de cambio es muy pequeño no ha habido movimiento
            if cv2.contourArea(contour) < 2500:
                continue

            x, y, w, h = cv2.boundingRect(contour)
            # Sino verificamos si el contorno está dentro del roi
            if y > roi_y1 and y + h < roi_y2:
                logging.debug(f"Detected motion on {camera['name']}")
                return True

        # Actualizamos los frames para la siguiente iteración
        frame1 = frame2
        cap.grab()
        cap.grab()
        ret, frame2 = cap.read()

        if not ret:
            break

        # Esperamos un poco antes de la siguiente comprobación
        time.sleep(0.1)

    cap.release()
    cv2.destroyAllWindows()
    return False

# Detección de movimiento que se ejecutará constantemente
def motion_detection_loop():
    global motion_detection_active
    logging.debug("Motion loop started.")
    while motion_detection_active:
        # Copiamos la lista de cámaras para evitar modificaciones concurrentes de otro cliente
        current_cameras = copy.deepcopy(cameras)
        for camera in current_cameras:
            if detect_motion(camera):
                message = f"Motion detected {camera['name']}"
                logging.debug(message)
                notify_clients(message)
        time.sleep(1)  # Esperamos un poco antes de volver a comprobar
    logging.debug("Motion loop stopped.")

# Función para detener la detección de movimiento
def stop_motion_detection():
    global motion_detection_active
    motion_detection_active = False
    logging.debug("Motion detection stopped.")

# Ruta para obtener la lista de cámaras
@app.route('/cameras', methods=['GET'])
def get_cameras():
    return jsonify(cameras)

# Ruta para añadir una cámara
@app.route('/cameras', methods=['POST'])
def add_camera():
    # Añadimos la cámara a la lista global de cámaras
    new_camera = request.get_json()
    new_camera['id'] = cameras[-1]['id'] + 1 if cameras else 1
    cameras.append(new_camera)
    
    save_cameras()

    return jsonify(new_camera), 201

# Ruta para iniciar el stream de una cámara
@app.route('/cameras/<int:camera_id>/start', methods=['POST'])
def start_live_camera(camera_id):
    for camera in cameras:
        # Si encontramos la cámara seguimos con el código
        if camera['id'] == camera_id:
            break
    else:
        # No se ha encontrado la cámara en las registradas así que paramos la ejecución
        return jsonify({'error': 'Camera not found'}), 404
    
    # Creamos el proceso para el broadcast de la cámara seleccionada con calidad reducida para que el stream sea fluido
    broadcastCommand = f"ffmpeg -rtsp_transport tcp -i {camera['ip']} -c:v libx264 -preset ultrafast -tune zerolatency -b:v 300k -maxrate 200k -bufsize 600k -s 320x240 -f flv -reconnect 1 -reconnect_at_eof 1 -reconnect_streamed 1 -reconnect_delay_max 2 rtmp://192.168.1.160/live"
    
    processes[camera_id] = subprocess.Popen(broadcastCommand, shell=True, preexec_fn=os.setsid)

    return jsonify({'message': 'Camera stream started'}), 200

# Ruta para detener el stream de una cámara
@app.route('/cameras/<int:camera_id>/stop', methods=['POST'])
def stop_live_camera(camera_id):
    process = processes.get(camera_id)
    if process:
        # Si existe el proceso lo terminamos
        os.killpg(os.getpgid(process.pid), signal.SIGTERM)
        process.wait()
        processes.pop(camera_id)

        return jsonify({'message': 'Camera stream stopped'}), 200
    else:
        # Si no existe el proceso lo indicamos con un mensaje de error
        return jsonify({'error': 'Camera stream not running'}), 404

# Ruta para eliminar todas las cámaras
@app.route('/cameras', methods=['DELETE'])
def delete_all_cameras():
    global cameras
    
    stop_all_cameras()
    cameras = []
    save_cameras()
    
    return jsonify({'message': 'All cameras deleted'}), 200

# Ruta para activar la alarma
@app.route('/alarm', methods=['POST'])
def activate_alarm():
    try:
        GPIO.output(alarm_pin, GPIO.HIGH)
        logging.debug("Alarm activated")
        time.sleep(3)  # Mantenemos la alarma activada durante 3 segundos
        GPIO.output(alarm_pin, GPIO.LOW)
        logging.debug("Alarm deactivated")
        return jsonify({'message': 'Alarm activated for 3 seconds'}), 200
    except Exception as e:
        logging.error(f"Alarm activation failed: {e}")
        return jsonify({'error': 'Alarm activation failed', 'details': str(e)}), 500

# Ruta para eliminar una cámara
@app.route('/cameras/<int:camera_id>', methods=['DELETE'])
def delete_camera(camera_id):
    global cameras
    camera = next((camera for camera in cameras if camera['id'] == camera_id), None)
    if camera is None:
        return jsonify({'error': 'Camera not found'}), 404
    cameras = [camera for camera in cameras if camera['id'] != camera_id]
    save_cameras()
    return jsonify({'message': 'Camera deleted'}), 200

# Ruta para editar los detalles de una cámara
@app.route('/cameras/<int:camera_id>', methods=['PUT'])
def edit_camera(camera_id):
    updated_camera = request.get_json()
    camera = next((camera for camera in cameras if camera['id'] == camera_id), None)
    if camera is None:
        return jsonify({'error': 'Camera not found'}), 404
    
    camera.update({
        'name': updated_camera.get('name', camera['name']),
        'ip': updated_camera.get('ip', camera['ip'])
    })
    save_cameras()
    return jsonify(camera), 200

# Función para detener todas las transmisiones registradas
def stop_all_cameras():
    for process in processes.values():
        os.killpg(os.getpgid(process.pid), signal.SIGTERM)
        process.wait()
        logging.debug("Camera stream stopped.")

# Función para detener tanto transmisiones como el servicio de detección de movimiento
def stop_api_processes():
    stop_all_cameras()
    stop_motion_detection()

if __name__ == '__main__':
    # Registramos un manejador de salida para que si se cierra el proceso que no queden encendidos
    atexit.register(stop_api_processes)
    
    # Cargamos las cámaras del archivo
    load_cameras()
    
    # Iniciamos el hilo de detección de movimiento
    detection_thread = threading.Thread(target=motion_detection_loop, daemon=True)
    detection_thread.start()
    
    # Inicializamos GPIO
    GPIO.setmode(GPIO.BCM)
    GPIO.setup(alarm_pin, GPIO.OUT)
    GPIO.output(alarm_pin, GPIO.LOW)

    # Iniciamos la api
    app.run(host='0.0.0.0', port=5000)
