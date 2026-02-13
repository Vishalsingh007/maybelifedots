package com.example.lifedots.logic

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

        // 1. Time Range: Midnight to Now
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val start = calendar.timeInMillis
        val end = System.currentTimeMillis()

        // 2. Get the raw stats map
        val statsMap = usm.queryAndAggregateUsageStats(start, end)

        // 3. Get ALL Installed Apps
        val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val displayList = ArrayList<AppUsageInfo>()

        for (appInfo in allApps) {
            // FILTER 1: Must be a "Launchable" app (Hides system background services)
            if (pm.getLaunchIntentForPackage(appInfo.packageName) != null) {

                val packageName = appInfo.packageName

                // Get usage from the map (Default to 0)
                val usage = statsMap[packageName]?.totalTimeInForeground ?: 0L

                // FILTER 2: THE FIX - Only show apps with actual usage (> 0 minutes)
                if (usage > 0) {
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo)

                    displayList.add(AppUsageInfo(packageName, label, usage, icon))
                }
            }
        }

        // 4. Sort: High usage at the top
        return displayList.sortedByDescending { it.timeInForeground }
    }

    fun getTimeString(millis: Long): String {
        val hours = millis / 1000 / 3600
        val minutes = (millis / 1000 % 3600) / 60
        // Since we filter > 0, we generally won't need the 0m check, but good to keep safe
        if (hours == 0L && minutes == 0L) return "< 1m"
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}