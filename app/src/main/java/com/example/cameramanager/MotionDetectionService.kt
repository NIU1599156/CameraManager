package com.example.cameramanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class MotionDetectionService : Service() {

    private lateinit var webSocket: WebSocket
    private var serverIp: String? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serverIp = intent?.getStringExtra("serverIp")
        serverIp?.let {
            connectWebSocket(it)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.close(1000, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun connectWebSocket(ip: String) {
        val client = OkHttpClient()
        val request = Request.Builder().url("ws://$ip:5000/ws").build()
        val listener = MotionWebSocketListener()
        webSocket = client.newWebSocket(request, listener)
    }

    inner class MotionWebSocketListener : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("MotionDetectionService", "Message received: $text")
            sendNotification("Motion Detection", text)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
            Log.e("MotionDetectionService", "WebSocket error: ${t.message}")
        }
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "motion_detection_channel"

        val channel = NotificationChannel(channelId, "Motion Detection", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(1, notification)
        Log.d("MotionDetectionService", "Notification sent: $title - $message")
    }
}