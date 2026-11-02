package com.nova.anime

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import java.io.File

class ReelsActivity : AppCompatActivity() {
    private lateinit var pager: ViewPager2
    private lateinit var adapter: ReelAdapter
    private var player: ExoPlayer? = null
    private var current = -1
    private val videos by lazy { VideoStore.all(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pager = ViewPager2(this).apply { orientation = ViewPager2.ORIENTATION_VERTICAL; setBackgroundColor(0xFF07020F.toInt()) }
        adapter = ReelAdapter(videos.map { it.path to it.name })
        pager.adapter = adapter
        setContentView(pager)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { playAt(position) }
        })
        if (videos.isEmpty()) {
            val empty = TextView(this).apply { text = "NOVA Reels\n\nপ্রথমে Import দিয়ে ফোন থেকে ভিডিও NOVA-তে কপি করো।"; textSize = 18f; gravity = Gravity.CENTER; setTextColor(-1); setBackgroundColor(0xFF07020F.toInt()) }
            setContentView(empty)
        } else pager.post { playAt(0) }
    }
    private fun playAt(position: Int) {
        if (position !in videos.indices) return
        player?.release(); player = null
        val rv = pager.getChildAt(0) as? RecyclerView ?: return
        val holder = rv.findViewHolderForAdapterPosition(position) as? ReelAdapter.Holder ?: return
        val p = ExoPlayer.Builder(this).build()
        holder.playerView.player = p
        p.setMediaItem(MediaItem.fromUri(File(videos[position].path).let { android.net.Uri.fromFile(it) }))
        p.repeatMode = ExoPlayer.REPEAT_MODE_ONE
        p.prepare(); p.playWhenReady = true
        player = p; current = position
    }
    override fun onPause() { super.onPause(); player?.pause() }
    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }

    private class ReelAdapter(private val data: List<Pair<String,String>>) : RecyclerView.Adapter<ReelAdapter.Holder>() {
        class Holder(val frame: FrameLayout, val playerView: PlayerView, val label: TextView) : RecyclerView.ViewHolder(frame)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val frame = FrameLayout(parent.context).apply { setBackgroundColor(0xFF000000.toInt()) }
            val pv = PlayerView(parent.context).apply { useController = true }
            val label = TextView(parent.context).apply { setTextColor(-1); textSize = 16f; setPadding(18,18,18,18); setBackgroundColor(0x66000000) }
            frame.addView(pv, FrameLayout.LayoutParams(-1,-1))
            frame.addView(label, FrameLayout.LayoutParams(-1,-2, android.view.Gravity.BOTTOM))
            return Holder(frame,pv,label)
        }
        override fun onBindViewHolder(holder: Holder, position: Int) { holder.label.text = data[position].second }
        override fun getItemCount() = data.size
        override fun onViewRecycled(holder: Holder) { holder.playerView.player = null; super.onViewRecycled(holder) }
    }
}
