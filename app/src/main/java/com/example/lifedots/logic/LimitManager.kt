package com.example.lifedots.logic

import android.content.Context

object LimitManager {
    private const val PREFS = "LifeDotsLimits"
    private const val PREFS_CUSTOM_CAT = "LifeDotsCustomCategories"

    // --- DEEP WORK ENGINE (PROACTIVE LOCKDOWN) ---
    val essentialApps = listOf(
        "com.android.dialer", "com.google.android.dialer", "com.samsung.android.dialer", // Phone
        "com.android.mms", "com.google.android.apps.messaging", "com.samsung.android.messaging", // Text
        "com.android.settings", "com.android.systemui", // System UI
        "com.example.lifedots" // LifeDots App
    )

    fun startFocusMode(context: Context, durationMinutes: Int) {
        val endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("FOCUS_END", endTime).apply()
    }

    fun stopFocusMode(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("FOCUS_END").apply()
    }

    fun isFocusModeActive(context: Context): Boolean {
        val endTime = getFocusModeEndTime(context)
        if (endTime > System.currentTimeMillis()) return true
        if (endTime > 0) stopFocusMode(context) // Auto-clean expired timer
        return false
    }

    fun getFocusModeEndTime(context: Context): Long {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("FOCUS_END", 0L)
    }

    fun isEssentialApp(packageName: String): Boolean {
        return essentialApps.contains(packageName)
    }

    // --- APP LIMITS ---
    fun saveLimit(context: Context, packageName: String, limitMinutes: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(packageName, limitMinutes).apply()
    }

    fun getLimit(context: Context, packageName: String): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(packageName, 0)
    }

    fun removeLimit(context: Context, packageName: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(packageName).apply()
    }

    // --- CATEGORY LIMITS ---
    fun saveCategoryLimit(context: Context, categoryName: String, limitMinutes: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("CAT_$categoryName", limitMinutes).apply()
    }

    fun getCategoryLimit(context: Context, categoryName: String): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("CAT_$categoryName", 0)
    }

    fun removeCategoryLimit(context: Context, categoryName: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("CAT_$categoryName").apply()
    }

    // --- USER "GOD MODE" CATEGORY OVERRIDES ---
    fun saveCustomCategory(context: Context, packageName: String, categoryName: String) {
        context.getSharedPreferences(PREFS_CUSTOM_CAT, Context.MODE_PRIVATE).edit().putString(packageName, categoryName).apply()
    }

    fun getCustomCategory(context: Context, packageName: String): String? {
        return context.getSharedPreferences(PREFS_CUSTOM_CAT, Context.MODE_PRIVATE).getString(packageName, null)
    }

    // --- WHITELIST (GOLDEN TICKETS) ---
    fun setWhitelist(context: Context, packageName: String, extraMinutes: Int) {
        val endTime = System.currentTimeMillis() + (extraMinutes * 60 * 1000L)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("WHITE_$packageName", endTime).apply()
    }

    fun isWhitelisted(context: Context, packageName: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val endTime = prefs.getLong("WHITE_$packageName", 0L)

        if (endTime > System.currentTimeMillis()) return true

        if (endTime > 0) prefs.edit().remove("WHITE_$packageName").apply()
        return false
    }

    // --- TYPING CHALLENGE COUNTER ---
    fun getExtensionCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val count = prefs.getInt("EXT_COUNT", 0)
        prefs.edit().putInt("EXT_COUNT", count + 1).apply()
        return count
    }
}