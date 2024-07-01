package com.example.cameramanager

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import android.widget.PopupMenu

// Adaptador para la vista que maneja la lista de cámaras en la pantalla principal
class CameraAdapter(
    private val context: Context,
    private var cameraList: List<Camera>,
    private val onEditCamera: (Camera) -> Unit,
    private val onDeleteCamera: (Camera) -> Unit
) : RecyclerView.Adapter<CameraAdapter.CameraViewHolder>() {

    // Inflamos el layout para cada cámara de la vista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CameraViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.camera_item, parent, false)
        return CameraViewHolder(view)
    }

    // Vinculamos los datos de las cámaras con las vistas para cada elemento de la vista
    override fun onBindViewHolder(holder: CameraViewHolder, position: Int) {
        val camera = cameraList[position]
        holder.bind(camera)

        // Configurar el clic en cada elemento del RecyclerView para iniciar el stream
        holder.itemView.setOnClickListener {
            val apiInterface = ApiClient.apiInterface
            val call = apiInterface.startCamera(camera.id)
            // Hacer la llamada a la API para iniciar el stream en la Raspberry Pi
            call.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        val intent = Intent(context, VideoPlayerActivity::class.java)
                        val streamUrl = "rtmp://192.168.1.160/live"
                        val cameraId = camera.id
                        Log.d("CameraAdapter", "Starting stream with URL: $streamUrl")
                        intent.putExtra("url", streamUrl)
                        intent.putExtra("cameraId", cameraId)
                        context.startActivity(intent)
                    } else {
                        Log.e("CameraAdapter", "Error starting stream: ${response.message()}")
                        Toast.makeText(context, "Error al iniciar el stream", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("CameraAdapter", "Failed to start stream: ${t.message}")
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    // Obtenemos el número de elementos en la lista de cámaras
    override fun getItemCount(): Int {
        return cameraList.size
    }

    // Creamos un ViewHolder para representar y implementar acciones a cada cámara en la vista
    inner class CameraViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cameraName: TextView = itemView.findViewById(R.id.cameraName)
        private val cameraIp: TextView = itemView.findViewById(R.id.cameraIp)

        // Vinculamos los datos de la cámara con las vistas
        fun bind(camera: Camera) {
            cameraName.text = camera.name
            cameraIp.text = camera.ip

            // Configuramos la pulsación larga para mostrar el popup
            itemView.setOnLongClickListener {
                showPopupMenu(it, camera)
                true
            }
        }

        // Mostramos el popup con opciones para editar o eliminar la cámara
        private fun showPopupMenu(view: View, camera: Camera) {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.camera_popup_menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        onEditCamera(camera)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteCamera(camera)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }
}
