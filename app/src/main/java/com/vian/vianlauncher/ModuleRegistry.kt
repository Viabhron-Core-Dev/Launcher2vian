package com.vian.vianlauncher

import android.content.Context
import android.content.SharedPreferences

object ModuleRegistry {
    private const val PREFS_NAME = "vian_launcher_prefs"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var sidebarEnabled: Boolean
        get() = prefs.getBoolean("sidebarEnabled", false)
        set(value) = prefs.edit().putBoolean("sidebarEnabled", value).apply()

    var netSpeedEnabled: Boolean
        get() = prefs.getBoolean("netSpeedEnabled", false)
        set(value) = prefs.edit().putBoolean("netSpeedEnabled", value).apply()

    var callRecorderEnabled: Boolean
        get() = prefs.getBoolean("callRecorderEnabled", false)
        set(value) = prefs.edit().putBoolean("callRecorderEnabled", value).apply()

    var epubReaderEnabled: Boolean
        get() = prefs.getBoolean("epubReaderEnabled", false)
        set(value) = prefs.edit().putBoolean("epubReaderEnabled", value).apply()

    var appTrackerEnabled: Boolean
        get() = prefs.getBoolean("appTrackerEnabled", false)
        set(value) = prefs.edit().putBoolean("appTrackerEnabled", value).apply()

    var pwaloaderEnabled: Boolean
        get() = prefs.getBoolean("pwaloaderEnabled", false)
        set(value) = prefs.edit().putBoolean("pwaloaderEnabled", value).apply()

    var llmServiceEnabled: Boolean
        get() = prefs.getBoolean("llmServiceEnabled", false)
        set(value) = prefs.edit().putBoolean("llmServiceEnabled", value).apply()

    var schedulerEnabled: Boolean
        get() = prefs.getBoolean("schedulerEnabled", false)
        set(value) = prefs.edit().putBoolean("schedulerEnabled", value).apply()
}
