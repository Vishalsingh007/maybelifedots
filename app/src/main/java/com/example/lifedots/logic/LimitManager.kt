package com.example.lifedots.logic

import android.content.Context
import java.util.Calendar

object LimitManager {
    private const val PREF_NAME = "AppLimits"
    private const val PREF_EXTENSIONS = "ExtensionCounts"

    fun saveLimit(context: Context, packageName: String, minutes: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt(packageName, minutes).apply()
    }

    fun removeLimit(context: Context, packageName: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().remove(packageName).apply()
    }

    fun getLimit(context: Context, packageName: String): Int {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(packageName, 0)
    }

    // --- NEW: THE SHAME COUNTER ---

    fun getExtensionCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_EXTENSIONS, Context.MODE_PRIVATE)
        val lastDay = prefs.getInt("last_extension_day", -1)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        // If it's a new day, reset the counter to 0
        if (today != lastDay) {
            prefs.edit().putInt("last_extension_day", today).putInt("count", 0).apply()
            return 0
        }
        return prefs.getInt("count", 0)
    }

    fun addExtension(context: Context, packageName: String, extraMinutes: Int) {
        val currentLimit = getLimit(context, packageName)
        if (currentLimit > 0) {
            // 1. Add time to the limit
            saveLimit(context, packageName, currentLimit + extraMinutes)

            // 2. Increase the "Shame Counter"
            val prefs = context.getSharedPreferences(PREF_EXTENSIONS, Context.MODE_PRIVATE)
            val currentCount = getExtensionCount(context)
            prefs.edit().putInt("count", currentCount + 1).apply()
        }
    }
}