package com.example.cameramanager

import android.content.Context
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.EditText

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var cameraAdapter: CameraAdapter
    private lateinit var cameraList: List<Camera>
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var addCameraLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        /* DEBUG por si hay que borrar el sharedPref para testear
        with(sharedPref.edit()) {
            clear()
            apply()
        }
        */

        val ip = sharedPref.getString("raspberry_ip", null)

        if (ip == null) {
            askForIP(sharedPref)
        } else {
            ApiClient.setBaseUrl(ip)
            getCameras()
        }

        val serviceIntent = Intent(this, MotionDetectionService::class.java)
        serviceIntent.putExtra("serverIp", ip)
        startService(serviceIntent)

        val fabAddCamera: FloatingActionButton = findViewById(R.id.fab_add_camera)
        fabAddCamera.setOnClickListener {
            val intent = Intent(this, AddCameraActivity::class.java)
            addCameraLauncher.launch(intent)
        }

        addCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                getCameras()
            }
        }

        swipeRefreshLayout.setOnRefreshListener {
            getCameras()
        }
    }

    private fun askForIP(sharedPref: SharedPreferences) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Introduce la IP de la raspberry Pi")

        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val ip = input.text.toString()
            with(sharedPref.edit()) {
                putString("raspberry_ip", ip)
                apply()
            }
            ApiClient.setBaseUrl(ip)
            getCameras()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun getCameras() {
        swipeRefreshLayout.isRefreshing = true

        cameraList = emptyList()
        cameraAdapter = CameraAdapter(this@MainActivity, cameraList)
        recyclerView.adapter = cameraAdapter

        val apiInterface = ApiClient.apiInterface
        val call = apiInterface.getCameras()

        call.enqueue(object : Callback<List<Camera>> {
            override fun onResponse(call: Call<List<Camera>>, response: Response<List<Camera>>) {
                if (response.isSuccessful && response.body() != null) {
                    cameraList = response.body()!!
                    cameraAdapter = CameraAdapter(this@MainActivity, cameraList)
                    recyclerView.adapter = cameraAdapter
                } else {
                    Toast.makeText(this@MainActivity, "Error al obtener la lista de cámaras", Toast.LENGTH_SHORT).show()
                }
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onFailure(call: Call<List<Camera>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
            }
        })
    }
}
