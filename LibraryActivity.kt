package com.nova.anime

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class LibraryActivity : AppCompatActivity() {
    private lateinit var list: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }
    override fun onResume() { super.onResume(); if (::list.isInitialized) render() }
    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 20, 16, 16); setBackgroundColor(0xFF07020F.toInt()) }
        root.addView(TextView(this).apply { text = "NOVA · MY OFFLINE VIDEOS"; textSize = 20f; setTextColor(-1); setPadding(0, 0, 0, 16) })
        val stats = TextView(this).apply { setTextColor(0xFF9BEAF5.toInt()); text = "Storage used: %.1f MB".format(VideoStore.bytes(this@LibraryActivity) / 1048576.0); setPadding(0,0,0,12) }
        root.addView(stats)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        VideoStore.all(this).forEach { item ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(8, 8, 8, 8) }
            val name = TextView(this).apply { text = item.name + "\n%.1f MB".format(File(item.path).length()/1048576.0); setTextColor(-1); layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
            val play = Button(this).apply { text = "Play"; setOnClickListener { startActivity(Intent(this@LibraryActivity, PlayerActivity::class.java).putExtra("path", item.path).putExtra("title", item.name)) } }
            val delete = Button(this).apply { text = "Delete"; setOnClickListener {
                android.app.AlertDialog.Builder(this@LibraryActivity).setMessage("NOVA থেকে এই কপিটি মুছে ফেলবে?").setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ -> VideoStore.delete(this@LibraryActivity, item.id); render() }.show()
            } }
            row.addView(name); row.addView(play); row.addView(delete); list.addView(row)
        }
        if (VideoStore.all(this).isEmpty()) root.addView(TextView(this).apply { text = "এখনো কোনো ভিডিও Import করা হয়নি।"; setTextColor(0xFFB9A9D6.toInt()) })
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(Button(this).apply { text = "Back"; setOnClickListener { finish() } })
        setContentView(root)
    }
}
