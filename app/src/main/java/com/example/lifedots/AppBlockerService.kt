package com.example.lifedots

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.lifedots.logic.LimitManager
import com.example.lifedots.logic.UsageStatsHelper

class AppBlockerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    // Tracks the last package to prevent "spamming" the launch intent
    private var lastBlockedPackage = ""
    private var lastBlockTime = 0L

    // Loop faster (500ms) for instant reaction
    private val checkRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            checkCurrentApp()
            handler.postDelayed(this, 500)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startForegroundService()
            handler.post(checkRunnable)
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "blocker_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Focus Monitor", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("LifeDots Active")
            .setContentText("Guarding your time.")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .build()
        startForeground(1, notification)
    }

    private fun checkCurrentApp() {
        // 1. Safety Check: Can we draw the popup?
        if (!Settings.canDrawOverlays(this)) return

        // 2. REAL-TIME DETECTION (The Fix)
        // usageStatsManager.queryUsageStats lags by 30 seconds.
        // usageStatsManager.queryEvents is instant.
        val currentPackage = detectForegroundApp() ?: return

        // Ignore LifeDots itself or the Home Screen (Launcher)
        if (currentPackage == packageName || isLauncher(currentPackage)) return

        val limitMinutes = LimitManager.getLimit(this, currentPackage)

        if (limitMinutes > 0) {
            // Check how much time has been used today
            val todayUsageList = UsageStatsHelper.getTodayUsage(this)
            val appUsage = todayUsageList.find { it.packageName == currentPackage }

            if (appUsage != null) {
                val minutesUsed = appUsage.timeInForeground / 1000 / 60

                if (minutesUsed >= limitMinutes) {
                    // BLOCK TRIGGER
                    // Only launch if it's a new block OR if 2 seconds passed (to prevent flickering)
                    if (lastBlockedPackage != currentPackage || (System.currentTimeMillis() - lastBlockTime > 2000)) {

                        val blockIntent = Intent(this, BlockedActivity::class.java)
                        blockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        blockIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        blockIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

                        blockIntent.putExtra("BLOCKED_PACKAGE", currentPackage)
                        blockIntent.putExtra("BLOCKED_APP_NAME", appUsage.appName)

                        startActivity(blockIntent)

                        lastBlockedPackage = currentPackage
                        lastBlockTime = System.currentTimeMillis()
                    }
                } else {
                    // If we are under the limit, reset so we can block again later
                    if (currentPackage == lastBlockedPackage) {
                        lastBlockedPackage = ""
                    }
                }
            }
        }
    }

    private fun detectForegroundApp(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 // Look back 1 minute

        // Raw event log
        val events = usm.queryEvents(startTime, endTime)
        var lastPackage: String? = null
        val eventOut = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(eventOut)
            // We look for the MOST RECENT app to move to foreground
            if (eventOut.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPackage = eventOut.packageName
            }
        }
        return lastPackage
    }

    private fun isLauncher(packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(checkRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}