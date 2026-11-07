package com.nova.anime

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class StoredVideo(
    val id: String,
    val name: String,
    val path: String,
    val added: Long,
    val animeId: String? = null,
    val season: Int = 0,
    val episode: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastPlayed: Long = added
)

object VideoStore {
    private const val PREF = "nova_video_store"
    private const val KEY = "items"

    @Synchronized fun all(context: Context): List<StoredVideo> {
        val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<StoredVideo>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val item = StoredVideo(
                o.optString("id"), o.optString("name"), o.optString("path"), o.optLong("added"),
                o.optString("animeId").takeIf { it.isNotBlank() && it != "null" },
                o.optInt("season"), o.optInt("episode"), o.optLong("positionMs"), o.optLong("durationMs"),
                o.optLong("lastPlayed", o.optLong("added"))
            )
            if (item.id.isNotBlank() && File(item.path).isFile) out.add(item)
        }
        return out.sortedByDescending { it.added }
    }

    fun findEpisode(context: Context, animeId: String, season: Int, episode: Int): StoredVideo? =
        all(context).firstOrNull { it.animeId == animeId && it.season == season && it.episode == episode }
    fun findById(context: Context, id: String): StoredVideo? = all(context).firstOrNull { it.id == id }

    fun import(context: Context, uri: Uri, name: String): StoredVideo =
        copyToPrivate(context, uri, name, null, 0, 0)

    @Synchronized fun importEpisode(context: Context, uri: Uri, name: String, animeId: String, season: Int, episode: Int): StoredVideo {
        require(animeId.isNotBlank() && season > 0 && episode > 0) { "Anime/Season/Episode তথ্য সঠিক নয়" }
        val old = all(context).firstOrNull { it.animeId == animeId && it.season == season && it.episode == episode }
        val fresh = copyToPrivate(context, uri, name, animeId, season, episode)
        val latest = all(context).filterNot { it.animeId == animeId && it.season == season && it.episode == episode } + fresh
        persist(context, latest)
        old?.let { File(it.path).delete() }
        return fresh
    }

    @Synchronized fun deleteEpisode(context: Context, animeId: String, season: Int, episode: Int): Boolean {
        val current = all(context)
        val target = current.firstOrNull { it.animeId == animeId && it.season == season && it.episode == episode } ?: return false
        val removed = File(target.path).delete()
        persist(context, current.filterNot { it.id == target.id })
        return removed
    }

    @Synchronized fun delete(context: Context, id: String): Boolean {
        val current = all(context)
        val item = current.firstOrNull { it.id == id } ?: return false
        val removed = File(item.path).delete()
        persist(context, current.filterNot { it.id == id })
        return removed
    }

    @Synchronized fun updateProgress(context: Context, id: String, position: Long, duration: Long) {
        persist(context, all(context).map {
            if (it.id == id) it.copy(positionMs = position.coerceAtLeast(0L), durationMs = duration.coerceAtLeast(0L), lastPlayed = System.currentTimeMillis()) else it
        })
    }

    fun episodeMapJson(context: Context): String {
        val result = JSONObject()
        all(context).filter { it.animeId != null && it.season > 0 && it.episode > 0 }.forEach { item ->
            result.put(episodeKey(item.animeId!!, item.season, item.episode), JSONObject()
                .put("id", item.id).put("name", item.name).put("size", File(item.path).length())
                .put("positionMs", item.positionMs).put("durationMs", item.durationMs).put("lastPlayed", item.lastPlayed).put("native", true))
        }
        return result.toString()
    }

    fun episodeKey(id: String, season: Int, episode: Int) = "${id}__s${season}__e${episode}"
    fun bytes(context: Context): Long = all(context).sumOf { File(it.path).length() }

    @Synchronized private fun copyToPrivate(context: Context, uri: Uri, name: String, animeId: String?, season: Int, episode: Int): StoredVideo {
        val directory = File(context.filesDir, "nova_media").apply { mkdirs() }
        val safeName = name.replace(Regex("[^A-Za-z0-9._ -]"), "_").take(100).ifBlank { "video.mp4" }
        val id = UUID.randomUUID().toString()
        val target = File(directory, "${id}_$safeName")
        try {
            val input = context.contentResolver.openInputStream(uri) ?: error("ফাইল পড়া যাচ্ছে না")
            input.use { source -> target.outputStream().use { sink -> source.copyTo(sink) } }
            val item = StoredVideo(id, safeName, target.absolutePath, System.currentTimeMillis(), animeId, season, episode)
            persist(context, all(context) + item)
            return item
        } catch (e: Exception) {
            target.delete()
            throw e
        }
    }

    private fun persist(context: Context, items: List<StoredVideo>) {
        val array = JSONArray()
        items.forEach { v ->
            array.put(JSONObject().put("id", v.id).put("name", v.name).put("path", v.path).put("added", v.added)
                .put("animeId", v.animeId).put("season", v.season).put("episode", v.episode)
                .put("positionMs", v.positionMs).put("durationMs", v.durationMs).put("lastPlayed", v.lastPlayed))
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).commit()
    }
}
