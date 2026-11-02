package com.nova.anime

import android.net.Uri
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var fallbackView: PlayerView
    private lateinit var status: TextView
    private var usingExo = false
    private var path = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        path = intent.getStringExtra("path") ?: ""
        if (path.isBlank() || !File(path).exists()) { finish(); return }
        root = FrameLayout(this).apply { setBackgroundColor(0xFF000000.toInt()) }
        surface = SurfaceView(this)
        fallbackView = PlayerView(this)
        status = TextView(this).apply { text = "NOVA Player · Starting LibVLC…"; setTextColor(-1); setPadding(12,12,12,12) }
        root.addView(surface, FrameLayout.LayoutParams(-1,-1))
        root.addView(fallbackView, FrameLayout.LayoutParams(-1,-1))
        root.addView(status, FrameLayout.LayoutParams(-1,-2))
        fallbackView.visibility = View.GONE
        setContentView(root)
        surface.holder.addCallback(this)
        startVlc()
    }
    private fun startVlc() {
        try {
            libVlc = LibVLC(this, arrayListOf("--no-drop-late-frames", "--no-skip-frames", "--avcodec-hw=any"))
            vlcPlayer = MediaPlayer(libVlc).also { player ->
                player.setEventListener { event ->
                    if (event.type == MediaPlayer.Event.EncounteredError) runOnUiThread { startFallback("LibVLC playback error") }
                }
            }
            if (surface.holder.surface.isValid) attachVlcSurface()
        } catch (e: Exception) { startFallback("LibVLC unavailable: ${e.message}") }
    }
    private fun attachVlcSurface() {
        try {
            val p = vlcPlayer ?: return
            p.vlcVout.setVideoView(surface)
            p.vlcVout.attachViews()
            val media = Media(libVlc, Uri.fromFile(File(path))).apply { setHWDecoderEnabled(true, false) }
            p.media = media
            media.release()
            p.play()
            status.text = "NOVA Player · LibVLC"
        } catch (e: Exception) { startFallback("Switching to Media3…") }
    }
    private fun startFallback(reason: String) {
        if (usingExo || isFinishing) return
        usingExo = true
        try {
            vlcPlayer?.stop()
            vlcPlayer?.vlcVout?.detachViews()
            fallbackView.visibility = View.VISIBLE
            surface.visibility = View.GONE
            status.text = "$reason · Media3 Backup"
            exo = ExoPlayer.Builder(this).build().also { player ->
                fallbackView.player = player
                player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
                player.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        status.text = "Media3 could not play this file: ${error.errorCodeName}"
                    }
                })
                player.prepare(); player.playWhenReady = true
            }
        } catch (e: Exception) { Toast.makeText(this, "দুই Player-ই চালু করা যায়নি: ${e.message}", Toast.LENGTH_LONG).show() }
    }
    override fun surfaceCreated(holder: SurfaceHolder) { if (!usingExo) attachVlcSurface() }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) { try { vlcPlayer?.vlcVout?.detachViews() } catch (_: Exception) {} }
    override fun onStop() { super.onStop(); try { vlcPlayer?.pause(); exo?.pause() } catch (_: Exception) {} }
    override fun onDestroy() {
        exo?.release(); exo = null
        vlcPlayer?.stop(); vlcPlayer?.release(); vlcPlayer = null
        libVlc?.release(); libVlc = null
        super.onDestroy()
    }
}
