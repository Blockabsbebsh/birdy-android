package dev.blockabsbebsh.birdy

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BirdyScheduler.start(this)

        val padding = (24 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(Color.rgb(244, 247, 242))
        }
        layout.addView(TextView(this).apply {
            text = "🐦\nBirdy"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(28, 45, 28))
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        layout.addView(TextView(this).apply {
            text = "Birdy downloads each daily feed automatically, stores all five birds for offline rotation, and keeps showing the previous feed if a refresh fails."
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, padding)
        })
        layout.addView(Button(this).apply {
            text = "Add Birdy to home screen"
            setOnClickListener { requestWidget() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        layout.addView(Button(this).apply {
            text = "Check GitHub feed for updates"
            setOnClickListener {
                BirdyScheduler.syncNow(this@MainActivity)
                Toast.makeText(
                    this@MainActivity,
                    "Checking latest.json — the widget changes only when a new feed is published.",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(layout)
    }

    private fun requestWidget() {
        val manager = getSystemService(AppWidgetManager::class.java)
        val provider = ComponentName(this, BirdyWidgetReceiver::class.java)
        if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(provider, null, null)
    }
}
