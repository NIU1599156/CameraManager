package com.example.cameramanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    // Este método se llamará cuando recivamos una alarma
    override fun onReceive(context: Context, intent: Intent) {
        // Obtenemos las preferencias compartidas para la ip
        val sharedPreferences = context.getSharedPreferences("com.example.cameramanager.prefs", Context.MODE_PRIVATE)
        val serverIp = sharedPreferences.getString("serverIp", null)

        // Comprobamos ya hay una IP del servidor rpi
        if (serverIp != null) {
            Log.d("AlarmReceiver", "Alarma recibida, reiniciando servicio con ip $serverIp")

            // Creamos e iniciamos el servicio de detección de movimiento en primer plano
            val serviceIntent = Intent(context, MotionDetectionService::class.java).apply {
                putExtra("serverIp", serverIp) // Pasar la IP del servidor al servicio
            }

            context.startForegroundService(serviceIntent)
        } else {
            // Si no tenemos ip registrada mostraremos un mensaje de error en los logs
            Log.e("AlarmReceiver", "No server IP found in SharedPreferences")
        }
    }
}