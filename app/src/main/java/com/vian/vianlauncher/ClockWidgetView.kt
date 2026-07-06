package com.vian.vianlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.provider.AlarmClock
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClockWidgetView(context: Context) : LinearLayout(context) {

    private val timeText: TextView
    private val dateText: TextView
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEE d MMMM", Locale.getDefault())

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_TIME_TICK) {
                updateTime()
                AppLogger.d("ClockWidget", "Received TIME_TICK, updated time")
            }
        }
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setBackgroundColor(Color.TRANSPARENT)

        timeText = TextView(context).apply {
            textSize = 64f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        dateText = TextView(context).apply {
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }

        addView(timeText)
        addView(dateText)

        updateTime()

        AppLogger.d("ClockWidget", "Clock view created")
    }

    fun launchClockApp() {
        AppLogger.d("ClockWidget", "Tapped, attempting to launch clock app")
        try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            AppLogger.d("ClockWidget", "Launched clock app successfully")
        } catch (e: Exception) {
            AppLogger.e("ClockWidget", "Failed to launch clock app, attempting fallback", e)
            try {
                val fallbackIntent = context.packageManager.getLaunchIntentForPackage("com.android.deskclock")
                    ?: context.packageManager.getLaunchIntentForPackage("com.android.deskclock.go")
                if (fallbackIntent != null) {
                    fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(fallbackIntent)
                    AppLogger.d("ClockWidget", "Launched fallback clock app successfully")
                } else {
                    AppLogger.e("ClockWidget", "No fallback clock app found")
                }
            } catch (e2: Exception) {
                AppLogger.e("ClockWidget", "Fallback launch failed", e2)
            }
        }
    }

    private fun updateTime() {
        val now = Date()
        timeText.text = timeFormat.format(now)
        dateText.text = dateFormat.format(now)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        context.registerReceiver(timeReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        context.unregisterReceiver(timeReceiver)
    }
}
