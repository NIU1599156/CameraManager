package com.example.cameramanager

import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.MediaPlayer.Event
import org.videolan.libvlc.MediaPlayer.EventListener
import org.videolan.libvlc.util.VLCVideoLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var libVLC: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var videoLayout: VLCVideoLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var backButton: ImageButton
    private var cameraId: Int = 0
    private var playbackPosition = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        // Mantenemos la pantalla encendida para el video
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        videoLayout = findViewById(R.id.videoLayout)
        progressBar = findViewById(R.id.progressBar)
        backButton = findViewById(R.id.backButton)

        // Confgiguramos LibVLC
        libVLC = LibVLC(this)
        mediaPlayer = MediaPlayer(libVLC)

        mediaPlayer.setEventListener(mediaPlayerEventListener)

        if (savedInstanceState != null) {
            playbackPosition = savedInstanceState.getLong("playbackPosition", 0L)
        }

        // Obtenemos la URL del intent
        val url = intent.getStringExtra("url")
        cameraId = intent.getIntExtra("cameraId", 0)

        if (url != null) {
            playStream(url)
        } else {
            Toast.makeText(this, "URL is null", Toast.LENGTH_SHORT).show()
        }

        applyImmersiveModeIfNeeded()

        // Configurar el botón de retroceso
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Maneja el botón de atrás del sistema
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Finalizamos actividad
                finish()
            }
        })
    }

    private fun playStream(url: String) {
        try {
            progressBar.visibility = View.VISIBLE
            val media = Media(libVLC, Uri.parse(url))
            media.setHWDecoderEnabled(true, false)
            media.addOption(":network-caching=1500")
            mediaPlayer.media = media
            mediaPlayer.attachViews(videoLayout, null, false, false)
            mediaPlayer.play()
            mediaPlayer.time = playbackPosition
        } catch (e: Exception) {
            Toast.makeText(this, "Error iniciando VLC: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val mediaPlayerEventListener = EventListener { event ->
        when (event.type) {
            Event.Buffering -> {
                if (event.buffering >= 100.0f) {
                    progressBar.visibility = View.GONE
                } else {
                    progressBar.visibility = View.VISIBLE
                }
            }
            Event.Playing -> progressBar.visibility = View.GONE
            Event.Stopped, Event.EndReached, Event.EncounteredError -> progressBar.visibility = View.GONE
            else -> {}
        }
    }

    private fun applyImmersiveModeIfNeeded() {
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            WindowInsetsControllerCompat(window, window.decorView).let {
                it.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveModeIfNeeded()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("playbackPosition", mediaPlayer.time)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCameraStream()
        // Quitar el flag de mantener la pantalla encendida
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mediaPlayer.stop()
        mediaPlayer.detachViews()
        mediaPlayer.release()
        libVLC.release()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.pause()
        playbackPosition = mediaPlayer.time
    }

    override fun onStart() {
        super.onStart()
        mediaPlayer.play()
        mediaPlayer.time = playbackPosition
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveModeIfNeeded()
    }

    private fun stopCameraStream() {
        val apiInterface = ApiClient.apiInterface
        apiInterface.stopCamera(cameraId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@VideoPlayerActivity, "Stream detenido", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@VideoPlayerActivity, "Error al detener el stream", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@VideoPlayerActivity, "Error de red al detener el stream", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
