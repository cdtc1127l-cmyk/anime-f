package com.nova.anime

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private val pickVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importPicked(uri)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF07020F.toInt()) }
        web = WebView(this).apply {
            setBackgroundColor(0xFF07020F.toInt())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            addJavascriptInterface(NovaBridge(), "NOVA")
        }
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            setPadding(6, 8, 6, 8); setBackgroundColor(0xFF120922.toInt())
        }
        bar.addView(action("＋ Import") { pickVideo.launch(arrayOf("video/*")) })
        bar.addView(action("My Videos") { startActivity(Intent(this, LibraryActivity::class.java)) })
        bar.addView(action("Reels") { startActivity(Intent(this, ReelsActivity::class.java)) })
        bar.addView(action("Storage") { showStorage() })
        root.addView(bar)
        setContentView(root)
        web.loadUrl("file:///android_asset/index.html")
    }
    private fun action(label: String, click: () -> Unit) = Button(this).apply {
        text = label; textSize = 11f; isAllCaps = false; setTextColor(0xFFFFFFFF.toInt())
        setBackgroundColor(0xFF54218A.toInt()); setOnClickListener { click() }
        layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(3, 0, 3, 0) }
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun importPicked(uri: Uri) {
        try {
            val name = contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else "video.mp4"
            } ?: "video.mp4"
            val item = VideoStore.import(this, uri, name)
            Toast.makeText(this, "NOVA-তে কপি হয়েছে: ${item.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) { Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show() }
    }
    private fun showStorage() {
        val mb = VideoStore.bytes(this) / (1024.0 * 1024.0)
        Toast.makeText(this, "NOVA Videos: %.1f MB · %d files".format(mb, VideoStore.all(this).size), Toast.LENGTH_LONG).show()
    }
    inner class NovaBridge {
        @JavascriptInterface fun openLibrary() { runOnUiThread { startActivity(Intent(this@MainActivity, LibraryActivity::class.java)) } }
        @JavascriptInterface fun openReels() { runOnUiThread { startActivity(Intent(this@MainActivity, ReelsActivity::class.java)) } }
        @JavascriptInterface fun storageBytes(): Long = VideoStore.bytes(this@MainActivity)
    }
}
