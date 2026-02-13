package com.example.lifedots.logic

import android.content.Context
import android.content.SharedPreferences

object LimitManager {
    private const val PREF_NAME = "AppLimits"

    // Save a limit (e.g., "com.instagram.android" -> 30 minutes)
    fun saveLimit(context: Context, packageName: String, minutes: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(packageName, minutes).apply()
    }

    // Remove a limit
    fun removeLimit(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(packageName).apply()
    }

    // Get the limit for an app (Returns 0 if no limit exists)
    fun getLimit(context: Context, packageName: String): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(packageName, 0)
    }

    // "I need 5 more minutes" logic
    fun addExtension(context: Context, packageName: String, extraMinutes: Int) {
        val current = getLimit(context, packageName)
        if (current > 0) {
            saveLimit(context, packageName, current + extraMinutes)
        }
    }
}