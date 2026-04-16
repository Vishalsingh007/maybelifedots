package com.example.lifedots.logic

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val timeInForeground: Long,
    val icon: android.graphics.drawable.Drawable?
)

object UsageStatsHelper {

    // --- REAL-TIME CALCULATOR (Used by the Blocker) ---
    fun getRealTimeUsageMillis(context: Context, targetPackage: String): Long {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val midnight = getStartOfDay()

        // Look back 24 hours to catch sessions that started yesterday
        val rangeStart = midnight - (24 * 60 * 60 * 1000)

        val events = usm.queryEvents(rangeStart, now)
        val event = UsageEvents.Event()

        var totalTimeToday = 0L
        var lastStartTime = 0L
        var isForeground = false

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName == targetPackage) {
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastStartTime = event.timeStamp
                    isForeground = true
                } else if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    if (isForeground && lastStartTime > 0) {
                        // CLAMP: Only count time that happened AFTER midnight
                        val overlapStart = max(lastStartTime, midnight)
                        val overlapEnd = min(event.timeStamp, now)

                        if (overlapEnd > overlapStart) {
                            totalTimeToday += (overlapEnd - overlapStart)
                        }
                    }
                    isForeground = false
                    lastStartTime = 0L
                }
            }
        }

        // CRITICAL: If the app is STILL open right now
        if (isForeground && lastStartTime > 0) {
            val overlapStart = max(lastStartTime, midnight)
            val overlapEnd = now

            if (overlapEnd > overlapStart) {
                totalTimeToday += (overlapEnd - overlapStart)
            }
        }

        return totalTimeToday
    }

    // --- THE FIX: Custom Aggregation to stop Android's bucket overlap bug ---
    fun getTodayUsage(context: Context): List<AppUsageInfo> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager
        val now = System.currentTimeMillis()
        val midnight = getStartOfDay()

        // Look back 24 hours to catch any open sessions perfectly
        val rangeStart = midnight - (24 * 60 * 60 * 1000)

        val events = usm.queryEvents(rangeStart, now)
        val event = UsageEvents.Event()

        val usageMap = mutableMapOf<String, Long>()
        val startTimes = mutableMapOf<String, Long>()
        val isForeground = mutableMapOf<String, Boolean>()

        // Process every single event for every app
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName

            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                startTimes[pkg] = event.timeStamp
                isForeground[pkg] = true
            } else if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                if (isForeground[pkg] == true) {
                    val lastStartTime = startTimes[pkg] ?: 0L
                    if (lastStartTime > 0) {
                        // The Midnight Clamp applied to all apps
                        val overlapStart = max(lastStartTime, midnight)
                        val overlapEnd = min(event.timeStamp, now)

                        if (overlapEnd > overlapStart) {
                            usageMap[pkg] = usageMap.getOrDefault(pkg, 0L) + (overlapEnd - overlapStart)
                        }
                    }
                }
                isForeground[pkg] = false
                startTimes[pkg] = 0L
            }
        }

        // Handle apps that are still open right now
        for ((pkg, foreground) in isForeground) {
            if (foreground) {
                val lastStartTime = startTimes[pkg] ?: 0L
                if (lastStartTime > 0) {
                    val overlapStart = max(lastStartTime, midnight)
                    val overlapEnd = now

                    if (overlapEnd > overlapStart) {
                        usageMap[pkg] = usageMap.getOrDefault(pkg, 0L) + (overlapEnd - overlapStart)
                    }
                }
            }
        }

        val appList = ArrayList<AppUsageInfo>()

        for ((pkg, time) in usageMap) {
            if (time > 0) {
                try {
                    // Only show apps that can be launched (ignores system background processes)
                    if (pm.getLaunchIntentForPackage(pkg) != null) {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        val label = pm.getApplicationLabel(appInfo).toString()
                        val icon = pm.getApplicationIcon(appInfo)
                        appList.add(AppUsageInfo(pkg, label, time, icon))
                    }
                } catch (e: Exception) { }
            }
        }
        return appList.sortedByDescending { it.timeInForeground }
    }

    // --- Counts Sessions ---
    fun getAppLaunchCount(context: Context): Int {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = getStartOfDay()
        val events = usm.queryEvents(start, end)
        var launches = 0
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) launches++
        }
        return launches
    }

    // --- Hourly Graph ---
    fun getAppUsageHourly(context: Context, packageName: String): List<Float> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - (12 * 60 * 60 * 1000)
        val events = usm.queryEvents(start, end)
        val hourlyData = FloatArray(12) { 0f }
        var lastStartTime = 0L
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName == packageName) {
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastStartTime = event.timeStamp
                } else if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    if (lastStartTime > 0) {
                        val duration = event.timeStamp - lastStartTime
                        val offset = lastStartTime - start
                        val hourIndex = (offset / (1000 * 60 * 60)).toInt()
                        if (hourIndex in 0..11) hourlyData[hourIndex] += (duration / 1000f / 60f)
                        lastStartTime = 0L
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

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0) // Exact millisecond precision
        return calendar.timeInMillis
    }
}