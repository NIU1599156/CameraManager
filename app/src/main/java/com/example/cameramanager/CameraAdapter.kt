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

class CameraAdapter(private val context: Context, private var cameraList: List<Camera>) :
    RecyclerView.Adapter<CameraAdapter.CameraViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CameraViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.camera_item, parent, false)
        return CameraViewHolder(view)
    }

    override fun onBindViewHolder(holder: CameraViewHolder, position: Int) {
        val camera = cameraList[position]
        holder.cameraName.text = camera.name

        // Para todas las cámaras miramos la cámara pulsada
        holder.itemView.setOnClickListener {
            val apiInterface = ApiClient.apiInterface
            val call = apiInterface.startCamera(camera.id)
            // Hacemos la llamada a la api para empezar el streaming en la rpi
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

    override fun getItemCount(): Int {
        return cameraList.size
    }

    class CameraViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cameraName: TextView = itemView.findViewById(R.id.cameraName)
    }
}