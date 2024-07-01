package com.example.cameramanager

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Actividad para configurar la aplicación
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Obtenemos las preferencias compartidas
        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // Obtenemos los botones del diseño
        val btnChangeIP: Button = findViewById(R.id.btn_change_ip)
        val btnDeleteCameras: Button = findViewById(R.id.btn_delete_cameras)

        // Configuramos el evento para el botón de cambiar IP
        btnChangeIP.setOnClickListener {
            changeIP(sharedPref)
        }

        // Configuramos el evento para el botón de eliminar todas las cámaras
        btnDeleteCameras.setOnClickListener {
            confirmDeleteCameras()
        }
    }

    // Implementamos el cambio de la IP del servidor rpi
    private fun changeIP(sharedPref: SharedPreferences) {
        // Creamos un popup para el cambio de ip
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cambiar IP de la Raspberry Pi")

        // Usamos el diseño establecido
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.popup_text, null)
        val input = dialogLayout.findViewById<EditText>(R.id.editText)
        input.setText(sharedPref.getString("raspberry_ip", "")) // Establecer el texto actual de la IP
        builder.setView(dialogLayout)

        // Configuramos los botones del popup
        builder.setPositiveButton("OK") { dialog, _ ->
            val ip = input.text.toString()
            with(sharedPref.edit()) {
                putString("raspberry_ip", ip)
                apply()
                setResult(RESULT_OK)
                dialog.dismiss()
                finish()
            }
            Toast.makeText(this, "IP cambiada a $ip", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        // Mostramos el popup
        builder.show()
    }

    // Método para crear un popup para preguntar por la eliminación de todas las cámaras
    private fun confirmDeleteCameras() {
        // Creamos un popup de confirmación
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Eliminar todas las cámaras")
        builder.setMessage("¿Estás seguro de que quieres eliminar todas las cámaras?")

        // Configuramos los botones del diálogo
        builder.setPositiveButton("Sí") { dialog, _ ->
            deleteAllCameras()
            dialog.dismiss()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.cancel()
        }

        // Mostramos el popup
        builder.show()
    }

    // Método para eliminar todas las cámaras
    private fun deleteAllCameras() {
        val apiInterface = ApiClient.apiInterface
        val call = apiInterface.deleteCameras()

        // Realizamos la llamada a la API del servidor para eliminar todas las cámaras
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@SettingsActivity, "Cámaras borradas correctamente", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@SettingsActivity, "El servidor no ha podido borrar todas las cámaras", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@SettingsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
