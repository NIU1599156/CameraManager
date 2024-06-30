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

    private var camera: Camera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_camera)

        val etName: EditText = findViewById(R.id.etName)
        val etIp: EditText = findViewById(R.id.etIp)
        val btnAddCamera: Button = findViewById(R.id.btnAddCamera)

        // Comprobar si se han pasado datos de una cámara existente para editar
        camera = intent.getParcelableExtra("camera") as? Camera
        if (camera != null) {
            etName.setText(camera?.name)
            etIp.setText(camera?.ip)
            btnAddCamera.text = "Actualizar Cámara"
        }

        btnAddCamera.setOnClickListener {
            val name = etName.text.toString()
            val ip = etIp.text.toString()

            if (name.isNotEmpty() && ip.isNotEmpty()) {
                if (camera == null) {
                    // Añadir nueva cámara
                    val newCamera = Camera(id = 0, name = name, ip = ip)
                    addCamera(newCamera)
                } else {
                    // Actualizar cámara existente
                    val updatedCamera = camera!!.copy(name = name, ip = ip)
                    updateCamera(updatedCamera)
                }
            } else {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addCamera(camera: Camera) {
        val apiInterface = ApiClient.apiInterface
        val call = apiInterface.addCamera(camera)
        call.enqueue(object : Callback<Camera> {
            override fun onResponse(call: Call<Camera>, response: Response<Camera>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AddCameraActivity, "Cámara añadida", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@AddCameraActivity, "El servidor no ha podido añadir la cámara", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Camera>, t: Throwable) {
                Toast.makeText(this@AddCameraActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateCamera(camera: Camera) {
        val apiInterface = ApiClient.apiInterface
        val call = apiInterface.updateCamera(camera.id, camera)
        call.enqueue(object : Callback<Camera> {
            override fun onResponse(call: Call<Camera>, response: Response<Camera>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AddCameraActivity, "Cámara actualizada", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@AddCameraActivity, "El servidor no ha podido editar la cámara", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Camera>, t: Throwable) {
                Toast.makeText(this@AddCameraActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
