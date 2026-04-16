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

    // Option 4: Expanded Offline Community Dataset
    private val socialPackages = listOf("instagram", "tiktok", "facebook", "snapchat", "twitter", "threads", "reddit", "pinterest", "linkedin", "tumblr", "weibo", "discord", "whatsapp", "telegram", "bereal")
    private val videoPackages = listOf("youtube", "netflix", "hulu", "amazon.avod", "disney", "hbo", "crunchyroll", "twitch", "prime", "vimeo", "max")
    private val productivityPackages = listOf("slack", "gmail", "calendar", "keep", "notion", "trello", "asana", "evernote", "docs", "sheets", "drive", "dropbox", "zoom", "teams")

    /**
     * Determines which category an app belongs to using the Hybrid Engine.
     */
    fun getCategory(context: Context, packageName: String): String {
        // 1. GOD MODE: Check if the user manually changed this app's category
        val customOverride = LimitManager.getCustomCategory(context, packageName)
        if (customOverride != null) return customOverride

        val lowerPkg = packageName.lowercase()

        // 2. Option 4: Check our aggressive offline dictionary
        if (socialPackages.any { lowerPkg.contains(it) }) return CAT_SOCIAL
        if (videoPackages.any { lowerPkg.contains(it) }) return CAT_VIDEO
        if (productivityPackages.any { lowerPkg.contains(it) }) return CAT_PRODUCTIVITY
        if (lowerPkg.contains("game") || lowerPkg.contains("nintendo") || lowerPkg.contains("ea") || lowerPkg.contains("roblox") || lowerPkg.contains("supercell")) return CAT_GAMES

        // 3. Option 2: Ask Android for the official category (API 26+)
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
                if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                    if (getCategory(context, app.packageName) == targetCategory) {
                        packages.add(app.packageName)
                    }
                }
            }
        } catch (e: Exception) {}

        return packages
    }

    // Helper to get all categories for the UI dropdown
    fun getAllCategories(): Array<String> {
        return arrayOf(CAT_SOCIAL, CAT_VIDEO, CAT_GAMES, CAT_PRODUCTIVITY, CAT_OTHER)
    }
}