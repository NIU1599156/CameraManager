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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_camera)

        val etName: EditText = findViewById(R.id.etName)
        val etIp: EditText = findViewById(R.id.etIp)
        val btnAddCamera: Button = findViewById(R.id.btnAddCamera)

        btnAddCamera.setOnClickListener {
            val name = etName.text.toString()
            val ip = etIp.text.toString()

            if (name.isNotEmpty() && ip.isNotEmpty()) {
                val newCamera = Camera(id = 0, name = name, ip = ip)
                val apiInterface = ApiClient.apiInterface
                val call = apiInterface.addCamera(newCamera)
                call.enqueue(object : Callback<Camera> {
                    override fun onResponse(call: Call<Camera>, response: Response<Camera>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@AddCameraActivity, "Cámara añadida", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            Toast.makeText(this@AddCameraActivity, "Error al añadir la cámara", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Camera>, t: Throwable) {
                        Toast.makeText(this@AddCameraActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}