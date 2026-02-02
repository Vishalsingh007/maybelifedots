package com.example.lifedots

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class AppBlockerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    // The "Loop" that checks apps every second
    private val checkRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            checkCurrentApp()

            handler.postDelayed(this, 1000) // Run again in 1 second
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startForegroundService()
            handler.post(checkRunnable)
        }
        return START_STICKY // Restart if killed
    }

    private fun startForegroundService() {
        val channelId = "blocker_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Focus Mode Monitor", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Focus Mode Active")
            .setContentText("Monitoring usage...")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .build()

        startForeground(1, notification)
    }

    private fun checkCurrentApp() {
        // --- THIS IS WHERE WE WILL DETECT THE APP LATER ---
        // We need Stage 2 (Permissions) before this code works.
        // For now, it just loops silently.
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(checkRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}