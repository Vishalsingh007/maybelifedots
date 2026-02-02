package com.example.lifedots.logic

import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeManager {

    // --- EXISTING FUNCTIONS (KEPT SAFE) ---
    fun getTotalDaysInYear(): Int {
        val calendar = Calendar.getInstance()
        return if (isLeapYear(calendar.get(Calendar.YEAR))) 366 else 365
    }

    fun getCurrentDayOfYear(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    }

    fun getTotalDaysInMonth(): Int {
        val calendar = Calendar.getInstance()
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun getCurrentDayOfMonth(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    }

    fun getDayProgress(): Float {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val totalMinutes = (hour * 60) + minute
        return totalMinutes / 1440f // 1440 minutes in a day
    }

    // --- NEW FUNCTIONS (THE FIX) ---

    // Returns raw float (e.g. 0.45 for 45%) instead of a String
    fun getYearProgress(): Float {
        val current = getCurrentDayOfYear().toFloat()
        val total = getTotalDaysInYear().toFloat()
        return current / total
    }

    fun getMonthProgress(): Float {
        val current = getCurrentDayOfMonth().toFloat()
        val total = getTotalDaysInMonth().toFloat()
        return current / total
    }

    fun getDaysLeft(endMillis: Long): Long {
        val start = System.currentTimeMillis()
        val diff = endMillis - start
        return TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(0)
    }

    // --- STRING HELPERS (Updated to use new math) ---
    fun getYearProgressString(): String {
        val p = getYearProgress() * 100
        return String.format(Locale.US, "%.1f%% of Year is DONE", p)
    }

    fun getMonthProgressString(): String {
        val p = getMonthProgress() * 100
        val monthName = Calendar.getInstance().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US)
        return String.format(Locale.US, "%.1f%% of %s is DONE", p, monthName)
    }

    fun getDayProgressString(): String {
        val p = getDayProgress() * 100
        val dayName = Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US)
        return String.format(Locale.US, "%.1f%% of %s is DONE", p, dayName)
    }

    fun getDaysLeftString(endMillis: Long): String {
        val days = getDaysLeft(endMillis)
        return "$days Days Left"
    }

    fun getGoalProgress(startMillis: Long, endMillis: Long): Pair<Int, Int> {
        val now = System.currentTimeMillis()
        val totalDuration = endMillis - startMillis
        val timePassed = now - startMillis

        val totalDays = TimeUnit.MILLISECONDS.toDays(totalDuration).toInt().coerceAtLeast(1)
        val daysPassed = TimeUnit.MILLISECONDS.toDays(timePassed).toInt().coerceAtLeast(0)

        return Pair(daysPassed, totalDays)
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}