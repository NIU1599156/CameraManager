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

        // Usamos modo claro siempre
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Configuramos la barra de herramientas (toolbar)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Configuramos RecyclerView para poder recargar las cámaras
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Obtenemos las preferencias compartidas para la ip de la rpi
        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val ip = sharedPref.getString("raspberry_ip", null)

        // Si no hay IP guardada, la pedimos al usuario
        if (ip == null) {
            askForIP(sharedPref)
        } else {
            ApiClient.setBaseUrl(ip)
            getCameras()
        }

        // Iniciamos el servicio de detección de movimiento
        startMotionDetectionService(ip)

        // Configuramos el boton flotante para añadir una nueva cámara
        val fabAddCamera: FloatingActionButton = findViewById(R.id.fab_add_camera)
        fabAddCamera.setOnClickListener {
            val intent = Intent(this, AddCameraActivity::class.java)
            addCameraLauncher.launch(intent)
        }

        // Registramos el lanzador de actividad para que si se añade la cámara se recargue la vista
        addCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                getCameras()
            }
        }

        // Configuramos la acción de deslizar hacia abajo para refrescar la lista de cámaras
        swipeRefreshLayout.setOnRefreshListener {
            refreshApiAndServices()
        }

        // Registramos el lanzador de actividad para que si volvemos al main activity se recarguen las cámaras
        settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                refreshApiAndServices()
            }
        }
    }

    // Inflamos el menú de opciones existente
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // Manejamos las opciones del menú para que se puedan activar las alarmas y ir a configuración
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

    // Pedimos la IP al usuario por un popup
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

    // Obtenemos la lista de cámaras des de la API de la rpi
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

    // Refrescamos la API y los servicios de detección de movimiento para prevenir errores
    private fun refreshApiAndServices() {
        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val newIp = sharedPref.getString("raspberry_ip", null)
        if (newIp != null) {
            ApiClient.setBaseUrl(newIp)
            getCameras()
            restartMotionDetectionService(newIp)
        }
    }

    // Iniciamos el servicio de detección de movimiento
    private fun startMotionDetectionService(ip: String?) {
        if (ip != null) {
            val serviceIntent = Intent(this, MotionDetectionService::class.java)
            serviceIntent.putExtra("serverIp", ip)
            startService(serviceIntent)
        }
    }

    // Reiniciamos el servicio de detección de movimiento
    private fun restartMotionDetectionService(ip: String?) {
        if (ip != null) {
            stopService(Intent(this, MotionDetectionService::class.java))
            startMotionDetectionService(ip)
        }
    }

    // Mostramos el popup de confirmación para activar la alarma
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

    // Activamos la alarma a través de la API
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

    // Editamos una cámara
    private fun editCamera(camera: Camera) {
        val intent = Intent(this, AddCameraActivity::class.java)
        intent.putExtra("camera", camera)
        startActivity(intent)
    }

    // Eliminamos una cámara
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

    // Realizamos la eliminación de la cámara a través de una llamada a la API
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

    // Refrescamos la interfaz y servicios siempre que la actividad vuelve al primer plano
    override fun onResume() {
        super.onResume()
        refreshApiAndServices()
    }
}
