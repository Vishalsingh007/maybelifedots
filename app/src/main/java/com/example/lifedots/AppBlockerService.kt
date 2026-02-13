package com.example.lifedots

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.lifedots.logic.LimitManager
import com.example.lifedots.logic.UsageStatsHelper
import java.util.SortedMap
import java.util.TreeMap

class AppBlockerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    // Tracks the last app we blocked so we don't spam-open the activity 60 times a second
    private var lastBlockedPackage = ""
    private var lastBlockTime = 0L

    private val checkRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            checkCurrentApp()
            handler.postDelayed(this, 1000)
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
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("LifeDots Active")
            .setContentText("Guarding your time.")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .build()
        startForeground(1, notification)
    }

    private fun checkCurrentApp() {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time)

        if (stats != null && stats.isNotEmpty()) {
            val mySortedMap: SortedMap<Long, UsageStats> = TreeMap()
            for (usageStats in stats) {
                mySortedMap[usageStats.lastTimeUsed] = usageStats
            }

            if (mySortedMap.isNotEmpty()) {
                val currentStats = mySortedMap[mySortedMap.lastKey()]
                val currentPackage = currentStats?.packageName ?: return

                if (currentPackage == packageName) return // Don't block LifeDots

                val limitMinutes = LimitManager.getLimit(this, currentPackage)

                if (limitMinutes > 0) {
                    val todayUsageList = UsageStatsHelper.getTodayUsage(this)
                    val appUsage = todayUsageList.find { it.packageName == currentPackage }

                    if (appUsage != null) {
                        val minutesUsed = appUsage.timeInForeground / 1000 / 60

                        if (minutesUsed >= limitMinutes) {
                            // BLOCK TRIGGER
                            // We check if we already blocked this package recently to avoid flickering
                            // BUT if you switch apps and come back, we block again immediately.
                            if (lastBlockedPackage != currentPackage || (System.currentTimeMillis() - lastBlockTime > 3000)) {
                                val blockIntent = Intent(this, BlockedActivity::class.java)
                                blockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                blockIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // Forces it to front
                                blockIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                blockIntent.putExtra("BLOCKED_PACKAGE", currentPackage)
                                blockIntent.putExtra("BLOCKED_APP_NAME", appUsage.appName)
                                startActivity(blockIntent)

                                lastBlockedPackage = currentPackage
                                lastBlockTime = System.currentTimeMillis()
                            }
                        } else {
                            // If we are under the limit, reset the blocker memory
                            // This ensures that as soon as you hit the limit, it fires again
                            if (currentPackage == lastBlockedPackage) {
                                lastBlockedPackage = ""
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(checkRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}