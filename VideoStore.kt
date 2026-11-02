package com.nova.anime

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class StoredVideo(val id: String, val name: String, val path: String, val added: Long)

object VideoStore {
    private const val PREF = "nova_video_store"
    private const val KEY = "items"
    fun all(context: Context): List<StoredVideo> {
        val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<StoredVideo>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val item = StoredVideo(o.optString("id"), o.optString("name"), o.optString("path"), o.optLong("added"))
            if (File(item.path).exists()) out.add(item)
        }
        return out.sortedByDescending { it.added }
    }
    fun import(context: Context, uri: Uri, displayName: String): StoredVideo {
        val dir = File(context.filesDir, "nova_media").apply { mkdirs() }
        val clean = displayName.replace(Regex("[^A-Za-z0-9._ -]"), "_").take(90).ifBlank { "video.mp4" }
        val target = File(dir, "${UUID.randomUUID()}_$clean")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "ফাইল পড়া যাচ্ছে না" }.use { source ->
                target.outputStream().use { sink -> source.copyTo(sink) }
            }
        }
        val item = StoredVideo(UUID.randomUUID().toString(), clean, target.absolutePath, System.currentTimeMillis())
        val arr = JSONArray()
        all(context).forEach { arr.put(json(it)) }
        arr.put(json(item))
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
        return item
    }
    fun delete(context: Context, id: String): Boolean {
        val current = all(context)
        val target = current.firstOrNull { it.id == id } ?: return false
        val deleted = File(target.path).delete()
        val arr = JSONArray()
        current.filter { it.id != id }.forEach { arr.put(json(it)) }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
        return deleted
    }
    fun bytes(context: Context): Long = all(context).sumOf { File(it.path).length() }
    private fun json(v: StoredVideo) = JSONObject().put("id", v.id).put("name", v.name).put("path", v.path).put("added", v.added)
}
