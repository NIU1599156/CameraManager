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
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var cameraAdapter: CameraAdapter
    private lateinit var cameraList: List<Camera>
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var addCameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var settingsLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val ip = sharedPref.getString("raspberry_ip", null)

        if (ip == null) {
            askForIP(sharedPref)
        } else {
            ApiClient.setBaseUrl(ip)
            getCameras()
        }

        startMotionDetectionService(ip)

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
            refreshApiAndServices()
        }

        settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                refreshApiAndServices()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_alarm -> {
                showAlarmConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun askForIP(sharedPref: SharedPreferences) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Introduce la dirección IP de la raspberry Pi")

        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.popup_text, null)
        val input = dialogLayout.findViewById<EditText>(R.id.editText)
        builder.setView(dialogLayout)

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
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun getCameras() {
        swipeRefreshLayout.isRefreshing = true

        cameraList = emptyList()
        cameraAdapter = CameraAdapter(this@MainActivity, cameraList, ::editCamera, ::deleteCamera)
        recyclerView.adapter = cameraAdapter

        val apiInterface = ApiClient.apiInterface
        val call = apiInterface.getCameras()

        call.enqueue(object : Callback<List<Camera>> {
            override fun onResponse(call: Call<List<Camera>>, response: Response<List<Camera>>) {
                if (response.isSuccessful && response.body() != null) {
                    cameraList = response.body()!!
                    cameraAdapter = CameraAdapter(this@MainActivity, cameraList, ::editCamera, ::deleteCamera)
                    recyclerView.adapter = cameraAdapter
                } else {
                    Toast.makeText(this@MainActivity, "El servidor no ha podido enviar el listado de cámaras", Toast.LENGTH_SHORT).show()
                }
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onFailure(call: Call<List<Camera>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    private fun refreshApiAndServices() {
        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val newIp = sharedPref.getString("raspberry_ip", null)
        if (newIp != null) {
            ApiClient.setBaseUrl(newIp)
            getCameras()
            restartMotionDetectionService(newIp)
        }
    }

    private fun startMotionDetectionService(ip: String?) {
        if (ip != null) {
            val serviceIntent = Intent(this, MotionDetectionService::class.java)
            serviceIntent.putExtra("serverIp", ip)
            startService(serviceIntent)
        }
    }

    private fun restartMotionDetectionService(ip: String?) {
        if (ip != null) {
            stopService(Intent(this, MotionDetectionService::class.java))
            startMotionDetectionService(ip)
        }
    }

    private fun showAlarmConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Activar Alarma")
        builder.setMessage("¿Estás seguro de que quieres activar la alarma?")
        builder.setPositiveButton("Sí") { dialog, _ ->
            activateAlarm()
            dialog.dismiss()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun activateAlarm() {
        val apiInterface = ApiClient.apiInterface
        val call = apiInterface.activateAlarm()
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Alarma activada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "El servidor no ha podido activar la alarma", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun editCamera(camera: Camera) {
        val intent = Intent(this, AddCameraActivity::class.java)
        intent.putExtra("camera", camera)
        startActivity(intent)
    }

    private fun deleteCamera(camera: Camera) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Borrar Cámara")
        builder.setMessage("¿Estás seguro de que quieres borrar la cámara?")
        builder.setPositiveButton("Sí") { dialog, _ ->
            performDeleteCamera(camera)
            dialog.dismiss()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun performDeleteCamera(camera: Camera) {
        val apiInterface = ApiClient.apiInterface
        val call = apiInterface.deleteCamera(camera.id)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    refreshApiAndServices()
                    cameraAdapter.notifyDataSetChanged()
                    Toast.makeText(this@MainActivity, "Cámara borrada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "El servidor no ha podido borrar la cámara", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        refreshApiAndServices()
    }
}
