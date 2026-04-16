package com.example.lifedots.logic

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object AppCategoryHelper {

    const val CAT_SOCIAL = "Social Media"
    const val CAT_VIDEO = "Video & Entertainment"
    const val CAT_GAMES = "Games"
    const val CAT_PRODUCTIVITY = "Productivity"
    const val CAT_OTHER = "Other"

    // Hardcoded fallback nets for developers who forget to tag their apps
    private val socialPackages = listOf("instagram", "tiktok", "facebook", "snapchat", "twitter", "threads", "reddit", "pinterest", "linkedin", "tumblr", "weibo")
    private val videoPackages = listOf("youtube", "netflix", "hulu", "amazon.avod", "disney", "hbo", "crunchyroll", "twitch")

    /**
     * Determines which category an app belongs to.
     */
    fun getCategory(context: Context, packageName: String): String {
        val lowerPkg = packageName.lowercase()

        // 1. Check our aggressive fallback lists first
        if (socialPackages.any { lowerPkg.contains(it) }) return CAT_SOCIAL
        if (videoPackages.any { lowerPkg.contains(it) }) return CAT_VIDEO

        // 2. Ask Android for the official category (API 26+)
        try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(packageName, 0)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                return when (ai.category) {
                    ApplicationInfo.CATEGORY_SOCIAL -> CAT_SOCIAL
                    ApplicationInfo.CATEGORY_VIDEO -> CAT_VIDEO
                    ApplicationInfo.CATEGORY_GAME -> CAT_GAMES
                    ApplicationInfo.CATEGORY_PRODUCTIVITY -> CAT_PRODUCTIVITY
                    else -> CAT_OTHER
                }
            }
        } catch (e: Exception) { }

        // 3. Last resort guess based on common keywords
        if (lowerPkg.contains("game")) return CAT_GAMES

        return CAT_OTHER
    }

    /**
     * Returns a list of all installed package names that belong to a specific category.
     */
    fun getPackagesInCategory(context: Context, targetCategory: String): List<String> {
        val pm = context.packageManager
        val packages = mutableListOf<String>()

        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in installedApps) {
                // Only care about launchable apps (ignore system background processes)
                if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                    if (getCategory(context, app.packageName) == targetCategory) {
                        packages.add(app.packageName)
                    }
                }
            }
        } catch (e: Exception) {}

        return packages
    }
}