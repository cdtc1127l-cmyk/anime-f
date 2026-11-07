package com.nova.anime

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import java.io.File

class PlayerActivity : AppCompatActivity(), SurfaceHolder.Callback {
    private var libVlc: LibVLC? = null
    private var vlcPlayer: MediaPlayer? = null
    private var exo: ExoPlayer? = null
    private lateinit var root: FrameLayout
    private lateinit var surface: SurfaceView
    private lateinit var fallback: PlayerView
    private lateinit var status: TextView
    private var path = ""
    private var videoId = ""
    private var usingExo = false
    private var restored = false
    private var startPosition = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val saver = object : Runnable {
        override fun run() {
            saveProgress()
            if (!isFinishing) handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        path = intent.getStringExtra("path") ?: ""
        videoId = intent.getStringExtra("videoId") ?: ""
        if (path.isBlank() || !File(path).isFile) { finish(); return }
        startPosition = VideoStore.findById(this, videoId)?.positionMs ?: 0L

        root = FrameLayout(this).apply { setBackgroundColor(0xFF000000.toInt()) }
        surface = SurfaceView(this)
        fallback = PlayerView(this).apply { useController = true }
        status = TextView(this).apply {
            text = "BANKAI Player"
            setTextColor(-1); setPadding(14, 12, 14, 12); setBackgroundColor(0x88080018.toInt())
        }
        root.addView(surface, FrameLayout.LayoutParams(-1, -1))
        root.addView(fallback, FrameLayout.LayoutParams(-1, -1))
        root.addView(status, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            setPadding(8, 7, 8, 7); setBackgroundColor(0xCC080018.toInt())
        }
        actions.addView(Button(this).apply {
            text = "VLC / MX Player"; isAllCaps = false
            setOnClickListener { saveProgress(); openExternal() }
        }, LinearLayout.LayoutParams(0, 50, 1f))
        actions.addView(Button(this).apply {
            text = "Close"; isAllCaps = false
            setOnClickListener { saveProgress(); finish() }
        })
        root.addView(actions, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        fallback.visibility = View.GONE
        setContentView(root)
        surface.holder.addCallback(this)
        startVlc()
        handler.post(saver)
    }

    private fun startVlc() {
        try {
            libVlc = LibVLC(this, arrayListOf("--no-drop-late-frames", "--no-skip-frames", "--avcodec-hw=any"))
            vlcPlayer = MediaPlayer(libVlc).also { p ->
                p.setEventListener { e ->
                    if (e.type == MediaPlayer.Event.EncounteredError) runOnUiThread { startFallback("LibVLC error") }
                    if (e.type == MediaPlayer.Event.Playing) restoreVlcPosition()
                }
            }
            if (surface.holder.surface.isValid) attachVlc()
        } catch (e: Exception) { startFallback("LibVLC unavailable: ${e.message}") }
    }

    private fun attachVlc() {
        try {
            val p = vlcPlayer ?: return
            p.vlcVout.setVideoView(surface)
            p.vlcVout.attachViews()
            val media = Media(libVlc, Uri.fromFile(File(path))).apply { setHWDecoderEnabled(true, false) }
            p.media = media
            media.release()
            p.play()
            status.text = "BANKAI Player · LibVLC"
            restoreVlcPosition()
        } catch (e: Exception) { startFallback("Switching to Media3") }
    }

    private fun restoreVlcPosition() {
        if (restored || startPosition <= 0L) return
        restored = true
        handler.postDelayed({ try { vlcPlayer?.setTime(startPosition) } catch (_: Exception) {} }, 750)
    }

    private fun startFallback(reason: String) {
        if (usingExo || isFinishing) return
        usingExo = true
        try {
            vlcPlayer?.stop()
            vlcPlayer?.vlcVout?.detachViews()
            fallback.visibility = View.VISIBLE
            surface.visibility = View.GONE
            status.text = "$reason · Media3 fallback"
            exo = ExoPlayer.Builder(this).build().also { p ->
                fallback.player = p
                p.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
                p.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        status.text = "Playback failed: ${error.errorCodeName}"
                    }
                })
                p.prepare()
                if (startPosition > 0L) p.seekTo(startPosition)
                p.playWhenReady = true
                restored = true
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Player চালু হয়নি: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveProgress() {
        if (videoId.isBlank()) return
        try {
            val pos = if (usingExo) exo?.currentPosition ?: 0L else vlcPlayer?.time ?: 0L
            val dur = if (usingExo) (exo?.duration ?: 0L).coerceAtLeast(0L) else vlcPlayer?.length ?: 0L
            VideoStore.updateProgress(this, videoId, pos, dur)
        } catch (_: Exception) {}
    }

    private fun openExternal() {
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", File(path))
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri("Video", uri)
            }
            startActivity(Intent.createChooser(intent, "Play with VLC / MX Player"))
        } catch (_: Exception) {
            Toast.makeText(this, "External player পাওয়া যায়নি", Toast.LENGTH_LONG).show()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) { if (!usingExo) attachVlc() }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) { saveProgress(); try { vlcPlayer?.vlcVout?.detachViews() } catch (_: Exception) {} }
    override fun onPause() { saveProgress(); try { vlcPlayer?.pause(); exo?.pause() } catch (_: Exception) {}; super.onPause() }
    override fun onStop() { saveProgress(); super.onStop() }
    override fun onDestroy() {
        handler.removeCallbacks(saver); saveProgress()
        exo?.release(); exo = null
        vlcPlayer?.stop(); vlcPlayer?.release(); vlcPlayer = null
        libVlc?.release(); libVlc = null
        super.onDestroy()
    }
}
