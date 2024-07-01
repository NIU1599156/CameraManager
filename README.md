# Guía de Instalación de la API en Raspberry Pi

Esta guía te llevará a configurar y ejecutar la API de la Raspberry Pi, consiguiendo que se inicie automáticamente al arrancar el sistema.

## Requisitos

- Raspberry Pi con Raspbian instalado
- Conexión a Internet
- Acceso a una terminal

## Pasos de Instalación

### 1. Clonar el Repositorio

Clona el repositorio de la aplicación desde GitHub:

```sh
git clone https://github.com/NIU1599156/CameraManager.git
cd CameraManager
```

### 2. Crear y Activar un entorno virtual

Raspberry Pi necesita de la creación de un entorno virtual para la instalación de paquetes mediante pip, así que vamos a activar el entorno virtual:

```sh
python3 -m venv myenv
source myenv/bin/activate
```

### 3. Instalar dependencias

Necesitaremos que las dependencias se instalen correctamente en nuestro entorno virtual:

```sh
pip install -r requirements.txt
```

### 4. Instalar aplicaciones adicionales

La API hace uso de streaming para la retransmisión de las cámaras, por tanto necesitamos instalar paquetes adicioneles:

```sh
sudo apt update
sudo apt install nginx, libnginx-mod-rtmp, ffmpeg
```

### 5. Editar preferencias de nginx

Necesitaremos editar las preferencias de nginx para poder hacer streaming correctamente, por ello tendremos que editar el archivo de configuración:

```sh
sudo nano /etc/nginx/nginx.conf
```

Una vez abierto el archivo, nos iremos abajo del todo para incluir este trozo de configuración y así poder abrir el servidor para el streaming:

```
rtmp {
    server {
        listen 1935;
        chunk_size 4096;

        application live {
            live on;
            record off;
        }
    }
}
```

Una vez hecho esto podemos reiniciar la Raspberry Pi o bien podemos reiniciar el servicio de nginx con este comando:

```sh
sudo systemctl restart nginx
```

A partir de este punto, podremos iniciar nuestra api manualmente yendo al directorio donde se encuentra la propia api e introduciendo:

```sh
source venv/bin/activate
python api.py
```

Sin embargo, si deseas que la API se inicie automáticamente cada vez que se encienda la Raspberry Pi, continua siguiendo los pasos.

### 6. Configuración del script de inicio

Ahora debemos darle permisos de ejecución al script que utilizaremos cada vez que queramos usar la API:

```sh
echo -e "#!/bin/bash\nsource $(pwd)/myenv/bin/activate\npython $(pwd)/api.py" > start_flask_api.sh
chmod +x start_flask_api.sh
```

A partir de ahora, podremos iniciar la api cada vez que ejecutemos este script.

### 7. Creación del servicio systemd

Si queremos que la API se ejecute cada vez que se inicie la Raspberry Pi, necesitaremos añadirla como un servicio systemd. Asegurate de introducir tu usuario en el comando.

```sh
sudo tee /etc/systemd/system/flask_app.service > /dev/null <<EOL
[Unit]
Description=Flask Application
After=network.target

[Service]
User=INTRODUCE_TU_USUARIO
WorkingDirectory=$(pwd)
ExecStart=$(pwd)/start_flask_api.sh
Restart=always

[Install]
WantedBy=multi-user.target
EOL
```

### 8. Habilitar el servicio

Para habilitar el servicio que acabamos de crear cada inicio, debemos iniciarlo manualmente por primera vez:

```sh
sudo systemctl enable flask_app.service
sudo systemctl start flask_app.service
sudo systemctl status flask_app.service
```

Ahora el servicio estará siempre activo y se reiniciará cada vez que encendamos la Raspberry Pi.