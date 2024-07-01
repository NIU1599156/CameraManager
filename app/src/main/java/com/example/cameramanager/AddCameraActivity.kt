package com.example.cameramanager

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddCameraActivity : AppCompatActivity() {

    // Variable para almacenar la cámara que se va a editar si estamos editandola
    private var camera: Camera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_camera)

        // Obtenemos elementos de la interfaz
        val etName: EditText = findViewById(R.id.etName)
        val etIp: EditText = findViewById(R.id.etIp)
        val btnAddCamera: Button = findViewById(R.id.btnAddCamera)

        // Si se han pasado datos en el intent significa que queremos editar una cámara existente
        camera = intent.getParcelableExtra("camera") as? Camera
        if (camera != null) {
            etName.setText(camera?.name)
            etIp.setText(camera?.ip)
            btnAddCamera.text = "Actualizar Cámara"
        }

        // Configuramos el botón de confirmar en función de si vamos a editar o añadir una cámara
        btnAddCamera.setOnClickListener {
            val name = etName.text.toString()
            val ip = etIp.text.toString()

            if (name.isNotEmpty() && ip.isNotEmpty()) {
                if (camera == null) {
                    // Si llegamos aquí queremos añadir cámara
                    val newCamera = Camera(id = 0, name = name, ip = ip)
                    addCamera(newCamera)
                } else {
                    // Si llegamos aquí queremos editar cámara
                    val updatedCamera = camera!!.copy(name = name, ip = ip)
                    updateCamera(updatedCamera)
                }
            } else {
                // Si los campos están vacíos no continuamos
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Añadimos una nueva cámara a través de una llamada a la API
    private fun addCamera(camera: Camera) {
        val apiInterface = ApiClient.apiInterface
        val call = apiInterface.addCamera(camera)
        call.enqueue(object : Callback<Camera> {
            override fun onResponse(call: Call<Camera>, response: Response<Camera>) {
                if (response.isSuccessful) {
                    // Cámara añadida con éxito
                    Toast.makeText(this@AddCameraActivity, "Cámara añadida", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    // Error en la respuesta del servidor
                    Toast.makeText(this@AddCameraActivity, "El servidor no ha podido añadir la cámara", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Camera>, t: Throwable) {
                // Error en la llamada a la API
                Toast.makeText(this@AddCameraActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Actualizamos una cámara existente a través de una llamada a la API
    private fun updateCamera(camera: Camera) {
        val apiInterface = ApiClient.apiInterface
        val call = apiInterface.updateCamera(camera.id, camera)
        call.enqueue(object : Callback<Camera> {
            override fun onResponse(call: Call<Camera>, response: Response<Camera>) {
                if (response.isSuccessful) {
                    // Si llegamos aquí la cámara se ha actualizado correctamente
                    Toast.makeText(this@AddCameraActivity, "Cámara actualizada", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    // Si llegamos aquí el servidor ha tenido algun problema al actualizar la cámara
                    Toast.makeText(this@AddCameraActivity, "El servidor no ha podido editar la cámara", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Camera>, t: Throwable) {
                // Si llegamos aquí se ha producido un error
                Toast.makeText(this@AddCameraActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
