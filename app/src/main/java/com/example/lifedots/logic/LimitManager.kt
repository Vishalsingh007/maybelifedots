package com.example.lifedots.logic

import android.content.Context

object LimitManager {
    private const val PREFS = "LifeDotsLimits"

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

    // --- WHITELIST (GOLDEN TICKETS) ---
    fun setWhitelist(context: Context, packageName: String, extraMinutes: Int) {
        val endTime = System.currentTimeMillis() + (extraMinutes * 60 * 1000L)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("WHITE_$packageName", endTime).apply()
    }

    fun isWhitelisted(context: Context, packageName: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val endTime = prefs.getLong("WHITE_$packageName", 0L)

        if (endTime > System.currentTimeMillis()) return true

        if (endTime > 0) prefs.edit().remove("WHITE_$packageName").apply() // Clean up expired ticket
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