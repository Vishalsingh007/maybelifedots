package com.example.lifedots.logic

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import java.util.Calendar

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val timeInForeground: Long,
    val icon: android.graphics.drawable.Drawable?
)

object UsageStatsHelper {

    fun getTodayUsage(context: Context): List<AppUsageInfo> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager

        // Midnight to Now
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val start = calendar.timeInMillis
        val end = System.currentTimeMillis()

        val statsMap = usm.queryAndAggregateUsageStats(start, end)
        val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val displayList = ArrayList<AppUsageInfo>()

        for (appInfo in allApps) {
            if (pm.getLaunchIntentForPackage(appInfo.packageName) != null) {
                val packageName = appInfo.packageName
                val usage = statsMap[packageName]?.totalTimeInForeground ?: 0L
                if (usage > 0) {
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo)
                    displayList.add(AppUsageInfo(packageName, label, usage, icon))
                }
            }
        }
        return displayList.sortedByDescending { it.timeInForeground }
    }

    // --- FIXED: MANUAL HOURLY CALCULATION ---
    fun getAppUsageHourly(context: Context, packageName: String): List<Float> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val startTime = endTime - (12 * 60 * 60 * 1000) // 12 Hours ago

        // We use queryEvents because INTERVAL_HOURLY does not exist
        val events = usm.queryEvents(startTime, endTime)
        val hourlyData = FloatArray(12) { 0f }

        var lastStartTime = 0L
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            if (event.packageName == packageName) {
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastStartTime = event.timeStamp
                }
                else if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    if (lastStartTime > 0) {
                        val duration = event.timeStamp - lastStartTime

                        // Calculate which "Hour Bucket" (0-11) this session belongs to
                        val offset = lastStartTime - startTime
                        val hourIndex = (offset / (1000 * 60 * 60)).toInt()

                        if (hourIndex in 0..11) {
                            // Add minutes to that hour
                            hourlyData[hourIndex] += (duration / 1000f / 60f)
                        }
                        lastStartTime = 0L // Reset
                    }
                }
            }
        }
        return hourlyData.toList()
    }

    fun getTimeString(millis: Long): String {
        val hours = millis / 1000 / 3600
        val minutes = (millis / 1000 % 3600) / 60
        if (hours == 0L && minutes == 0L) return "< 1m"
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}