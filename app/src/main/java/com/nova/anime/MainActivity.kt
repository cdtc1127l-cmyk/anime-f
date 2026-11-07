package com.nova.anime

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private var webFileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingEpisode: EpisodeTarget? = null
    private var addonInjected = false
    private data class EpisodeTarget(val animeId: String, val season: Int, val episode: Int)

    private val webFilePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        webFileCallback?.onReceiveValue(uri?.let { arrayOf(it) })
        webFileCallback = null
    }
    private val episodePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = pendingEpisode
        pendingEpisode = null
        if (uri != null && target != null) importEpisode(uri, target)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this).apply {
            setBackgroundColor(0xFF080211.toInt())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadsImagesAutomatically = true
            addJavascriptInterface(BankaiBridge(), "NOVA")
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    view: WebView?,
                    callback: ValueCallback<Array<Uri>>?,
                    params: FileChooserParams?
                ): Boolean {
                    webFileCallback?.onReceiveValue(null)
                    webFileCallback = callback
                    val types = params?.acceptTypes?.map { it.trim() }?.filter { it.isNotEmpty() }
                        ?.toTypedArray()?.takeIf { it.isNotEmpty() } ?: arrayOf("*/*")
                    return try {
                        webFilePicker.launch(types)
                        true
                    } catch (_: Exception) {
                        webFileCallback?.onReceiveValue(null)
                        webFileCallback = null
                        false
                    }
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return false
                    if (uri.scheme == "http" || uri.scheme == "https") {
                        try { startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) {}
                        return true
                    }
                    return false
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (!addonInjected) {
                        addonInjected = true
                        val script = """
                            (function(){
                              var l=document.createElement('link');l.rel='stylesheet';l.href='file:///android_asset/bankai-theme.css';document.head.appendChild(l);
                              var s=document.createElement('script');s.src='file:///android_asset/bankai-addon.js';document.body.appendChild(s);
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(script, null)
                    }
                }
            }
        }
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    private fun displayName(uri: Uri): String {
        return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "episode-video.mp4"
    }

    private fun importEpisode(uri: Uri, target: EpisodeTarget) {
        try {
            VideoStore.importEpisode(this, uri, displayName(uri), target.animeId, target.season, target.episode)
            web.evaluateJavascript(
                "window.bankaiNativeVideoChanged && window.bankaiNativeVideoChanged(" +
                    "${org.json.JSONObject.quote(target.animeId)},${target.season},${target.episode});", null
            )
            Toast.makeText(this, "Episode video app-এর private storage-এ copy হয়েছে", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Video import হয়নি: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun externalPlayer(item: StoredVideo) {
        try {
            val uri = FileProvider.getUriForFile(this, "${applicationId}.fileprovider", File(item.path))
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri(item.name, uri)
            }
            startActivity(Intent.createChooser(intent, "Play with VLC / MX Player"))
        } catch (_: Exception) {
            Toast.makeText(this, "Compatible external player পাওয়া যায়নি। Built-in Player ব্যবহার করো।", Toast.LENGTH_LONG).show()
        }
    }

    private fun playInternal(item: StoredVideo) {
        startActivity(Intent(this, PlayerActivity::class.java)
            .putExtra("path", item.path).putExtra("title", item.name).putExtra("videoId", item.id))
    }

    inner class BankaiBridge {
        @JavascriptInterface fun pickEpisodeVideo(animeId: String, season: Int, episode: Int) {
            runOnUiThread {
                pendingEpisode = EpisodeTarget(animeId, season.coerceAtLeast(1), episode.coerceAtLeast(1))
                episodePicker.launch(arrayOf("video/*", "application/octet-stream"))
            }
        }
        @JavascriptInterface fun episodeVideoMap(): String = VideoStore.episodeMapJson(this@MainActivity)
        @JavascriptInterface fun playEpisodeVideo(animeId: String, season: Int, episode: Int) {
            runOnUiThread {
                val item = VideoStore.findEpisode(this@MainActivity, animeId, season, episode)
                if (item == null) Toast.makeText(this@MainActivity, "এই Episode-এ video নেই", Toast.LENGTH_SHORT).show()
                else playInternal(item)
            }
        }
        @JavascriptInterface fun openEpisodeExternalPlayer(animeId: String, season: Int, episode: Int) {
            runOnUiThread {
                val item = VideoStore.findEpisode(this@MainActivity, animeId, season, episode)
                if (item == null) Toast.makeText(this@MainActivity, "এই Episode-এ video নেই", Toast.LENGTH_SHORT).show()
                else externalPlayer(item)
            }
        }
        @JavascriptInterface fun deleteEpisodeVideo(animeId: String, season: Int, episode: Int): Boolean {
            val deleted = VideoStore.deleteEpisode(this@MainActivity, animeId, season, episode)
            runOnUiThread {
                web.evaluateJavascript(
                    "window.bankaiNativeVideoDeleted && window.bankaiNativeVideoDeleted(" +
                        "${org.json.JSONObject.quote(animeId)},$season,$episode);", null
                )
            }
            return deleted
        }
        @JavascriptInterface fun storageBytes(): Long = VideoStore.bytes(this@MainActivity)
        @JavascriptInterface fun openReels() {
            runOnUiThread { startActivity(Intent(this@MainActivity, ReelsActivity::class.java)) }
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
    override fun onDestroy() {
        webFileCallback?.onReceiveValue(null)
        webFileCallback = null
        if (::web.isInitialized) web.destroy()
        super.onDestroy()
    }
}
