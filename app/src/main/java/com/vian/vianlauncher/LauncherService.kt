package com.vian.vianlauncher

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class LauncherService : Service() {

    override fun onCreate() {
        super.onCreate()
        AppLogger.d("LauncherService", "Service created")
        ModuleRegistry.init(this)
        startForegroundService()
        
        // Placeholder hooks for future modules
        // if (ModuleRegistry.sidebarEnabled) SidebarManager.start()
        // if (ModuleRegistry.netSpeedEnabled) NetSpeedManager.start()
        // if (ModuleRegistry.callRecorderEnabled) CallRecorderManager.start()
        // if (ModuleRegistry.epubReaderEnabled) EpubReaderManager.start()
        // if (ModuleRegistry.appTrackerEnabled) AppTrackerManager.start()
        // if (ModuleRegistry.pwaloaderEnabled) PwaLoaderManager.start()
        // if (ModuleRegistry.llmServiceEnabled) LlmServiceManager.start()
        // if (ModuleRegistry.schedulerEnabled) SchedulerManager.start()
    }

    private fun startForegroundService() {
        val channelId = "vian_launcher_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Vian Launcher Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val settingsIntent = Intent(this, SettingsActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, settingsIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Vian Launcher")
            .setContentText("Vian Launcher running")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // built-in icon placeholder
            .addAction(android.R.drawable.ic_menu_preferences, "Settings", pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLogger.d("LauncherService", "Service started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        AppLogger.d("LauncherService", "Service destroyed, restarting")
        super.onDestroy()
        val restartIntent = Intent(applicationContext, LauncherService::class.java)
        restartIntent.setPackage(packageName)
        val pendingIntent = PendingIntent.getService(
            this, 1, restartIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.setExact(
            android.app.AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 1000,
            pendingIntent
        )
    }
}
