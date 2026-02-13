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
    private var lastBlockedPackage = ""

    private val checkRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            checkCurrentApp()
            handler.postDelayed(this, 1000) // Check every second
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
            .setContentTitle("LifeDots Focus Active")
            .setContentText("Monitoring usage...")
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
                // FIXED: Use safe call (?.) and Elvis operator (?:) to handle potential null
                val currentStats = mySortedMap[mySortedMap.lastKey()]
                val currentPackage = currentStats?.packageName ?: return

                // Ignore LifeDots itself
                if (currentPackage == packageName) return

                val limitMinutes = LimitManager.getLimit(this, currentPackage)

                if (limitMinutes > 0) {
                    val todayUsageList = UsageStatsHelper.getTodayUsage(this)
                    val appUsage = todayUsageList.find { it.packageName == currentPackage }

                    if (appUsage != null) {
                        val minutesUsed = appUsage.timeInForeground / 1000 / 60

                        if (minutesUsed >= limitMinutes) {
                            if (lastBlockedPackage != currentPackage) {
                                val blockIntent = Intent(this, BlockedActivity::class.java)
                                blockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                blockIntent.putExtra("BLOCKED_PACKAGE", currentPackage)
                                blockIntent.putExtra("BLOCKED_APP_NAME", appUsage.appName)
                                startActivity(blockIntent)
                                lastBlockedPackage = currentPackage
                            }
                        } else {
                            lastBlockedPackage = ""
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