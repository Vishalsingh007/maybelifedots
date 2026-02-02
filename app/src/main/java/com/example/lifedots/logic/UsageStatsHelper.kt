package com.example.lifedots.logic

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
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
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager

        // Set range: Start of today (Midnight) to Now
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        // Get Data
        val usageStatsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        val appList = ArrayList<AppUsageInfo>()

        for ((packageName, stats) in usageStatsMap) {
            if (stats.totalTimeInForeground > 0) {
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    // Filter out system apps (mostly) to keep list clean
                    if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                        val label = packageManager.getApplicationLabel(appInfo).toString()
                        val icon = packageManager.getApplicationIcon(appInfo)
                        appList.add(AppUsageInfo(packageName, label, stats.totalTimeInForeground, icon))
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    // App might be uninstalled but still has stats, skip it
                }
            }
        }

        // Sort by most used (Descending)
        return appList.sortedByDescending { it.timeInForeground }
    }

    fun getTimeString(millis: Long): String {
        val hours = millis / 1000 / 3600
        val minutes = (millis / 1000 % 3600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}