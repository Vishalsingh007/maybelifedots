package com.example.lifedots.logic

import android.content.Context
import java.util.Calendar

object LimitManager {
    private const val PREF_NAME = "AppLimits"
    private const val PREF_EXTENSIONS = "ExtensionCounts"
    private const val PREF_WHITELIST = "TempWhitelist"

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

    // --- GOLDEN TICKET (Precision Timer) ---
    fun setWhitelist(context: Context, packageName: String, minutes: Int) {
        val expiryTime = System.currentTimeMillis() + (minutes * 60 * 1000)
        context.getSharedPreferences(PREF_WHITELIST, Context.MODE_PRIVATE)
            .edit().putLong(packageName, expiryTime).apply()

        // Track stats for the shame counter
        incrementExtensionCount(context)
    }

    fun isWhitelisted(context: Context, packageName: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_WHITELIST, Context.MODE_PRIVATE)
        val expiry = prefs.getLong(packageName, 0)
        return System.currentTimeMillis() < expiry
    }

    // --- SHAME COUNTER ---
    fun getExtensionCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_EXTENSIONS, Context.MODE_PRIVATE)
        val lastDay = prefs.getInt("last_extension_day", -1)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        if (today != lastDay) {
            prefs.edit().putInt("last_extension_day", today).putInt("count", 0).apply()
            return 0
        }
        return prefs.getInt("count", 0)
    }

    private fun incrementExtensionCount(context: Context) {
        val prefs = context.getSharedPreferences(PREF_EXTENSIONS, Context.MODE_PRIVATE)
        val currentCount = getExtensionCount(context)
        prefs.edit().putInt("count", currentCount + 1).apply()
    }
}