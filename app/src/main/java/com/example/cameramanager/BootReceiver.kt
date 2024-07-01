package com.example.cameramanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

// Creamos clase para recibir el evento de inicio del sistema y que se nos puedan notificar la
// detección de movimiento
class BootReceiver : BroadcastReceiver() {

    // Cuando se inicie android se llamará esta función
    override fun onReceive(context: Context, intent: Intent) {
        // Comprobar que el dispositivo ha terminado de arrancar
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            // Obtenemos las preferencias compartidas para la ip
            val sharedPreferences = context.getSharedPreferences("com.example.cameramanager.prefs", Context.MODE_PRIVATE)
            val serverIp = sharedPreferences.getString("serverIp", null)

            // Comprobar si tenemos guardado una IP del servidor
            if (serverIp != null) {
                Log.d("BootReceiver", "Inicio completado, iniciando servicio con ip $serverIp")

                // Creamos y iniciamos el servicio de detección de movimiento en primer plano
                val serviceIntent = Intent(context, MotionDetectionService::class.java).apply {
                    putExtra("serverIp", serverIp) // Pasar la IP del servidor al servicio
                }
                context.startForegroundService(serviceIntent)
            } else {
                // Si no encontramos la ip en las preferencias compartidas ponemos un log de error
                Log.e("BootReceiver", "No se ha registrado una ip para la API")
            }
        }
    }
}
