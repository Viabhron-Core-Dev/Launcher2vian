package com.vian.vianlauncher

import android.app.Application
import android.graphics.Bitmap
import android.os.Build
import android.util.LruCache
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LauncherApplication : Application() {

    val iconCache = LruCache<String, Bitmap>(60)

    override fun onCreate() {
        super.onCreate()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleCrash(thread, throwable)
        }
    }

    private fun handleCrash(thread: Thread, throwable: Throwable) {
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val timestamp = dateFormat.format(Date())
            
            val crashReport = buildString {
                appendLine("--- CRASH REPORT ---")
                appendLine("Timestamp: $timestamp")
                appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("OS Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Thread: ${thread.name}")
                appendLine("Exception:")
                appendLine(throwable.stackTraceToString())
            }

            val timestampFile = File(downloadsDir, "vian_launcher_crash_$timestamp.txt")
            val latestFile = File(downloadsDir, "vian_launcher_crash_latest.txt")

            FileWriter(timestampFile, false).use { it.write(crashReport) }
            FileWriter(latestFile, false).use { it.write(crashReport) }
            
            AppLogger.e("CrashLogger", "Uncaught exception", throwable)

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(10)
        }
    }
}
