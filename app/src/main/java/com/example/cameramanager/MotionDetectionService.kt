package com.example.cameramanager

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

// Servicio para escuchar si la raspberry pi detecta el movimiento
class MotionDetectionService : Service() {
    private var webSocket: WebSocket? = null
    private var serverIp: String? = null
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("com.example.cameramanager.prefs", Context.MODE_PRIVATE)
        setAlarm() // Configuramos la alarma al iniciar el servicio
    }

    // Configuramos el socket para que tenga la ip de la rpi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serverIp = intent?.getStringExtra("serverIp")
        serverIp?.let {
            connectWebSocket(it) // Conectamos al socket del servidor
        }
        return START_STICKY // Hacemos que el servicio continúe ejecutándose hasta que se detenga específicamente
    }

    // Cerramos el socket cuando se destruye el servicio
    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, "Servicio destruido")
    }

    // Método requerido por la interfaz Service así que retornamos null
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Método para conectarnos al socket del servidor
    private fun connectWebSocket(ip: String) {
        val client = OkHttpClient()
        val request = Request.Builder().url("ws://$ip:5000/ws").build()
        val listener = MotionWebSocketListener()
        webSocket = client.newWebSocket(request, listener)
    }

    // Clase para controlar los eventos del socket
    inner class MotionWebSocketListener : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("MotionDetectionService", "Mensaje recibido: $text")
            sendNotification("Detección de movimiento", text) // Enviar notificación al recibir un mensaje
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
            Log.e("MotionDetectionService", "Error WebSocket: ${t.message}")
        }
    }

    // Método para enviar la notificación de movimiento
    private fun sendNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "motion_detection_channel"

        // Creamos un canal de notificación
        val channel = NotificationChannel(channelId, "Detección de movimiento", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        // Creamos y enviamos la notificación
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(1, notification)
        Log.d("MotionDetectionService", "Notificación mandada con $title - $message")
    }

    // Método para que el servicio sea recurrente si hay alguna desconnexión, se reconectará cada 10 minutos
    private fun setAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 600000, // 10 minutos desde el momento actual
            600000, // Repetiremos cada 10 minutos
            pendingIntent
        )
    }
}
