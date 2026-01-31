package com.example.lifedots.logic

import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

object TimeManager {

    // --- YEAR MATH ---
    fun getTotalDaysInYear(): Int {
        val calendar = Calendar.getInstance()
        return if (isLeapYear(calendar.get(Calendar.YEAR))) 366 else 365
    }

    fun getCurrentDayOfYear(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    }

    // --- MONTH MATH ---
    fun getTotalDaysInMonth(): Int {
        val calendar = Calendar.getInstance()
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun getCurrentDayOfMonth(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    }

    // --- DAY MATH (0.0 to 1.0) ---
    fun getDayProgress(): Float {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val totalMinutesPassed = (hour * 60) + minute
        return totalMinutesPassed / 1440f
    }

    // --- GOAL MATH (NEW) ---
    // Returns Pair(Days Passed, Total Days Duration)
    fun getGoalProgress(startMillis: Long, endMillis: Long): Pair<Int, Int> {
        val now = System.currentTimeMillis()

        // Calculate total duration in days (ensure at least 1 day to avoid divide by zero)
        val totalMillis = endMillis - startMillis
        val totalDays = (totalMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)

        // Calculate passed duration
        val passedMillis = now - startMillis
        val passedDays = (passedMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)

        return Pair(passedDays, totalDays)
    }

    // --- TEXT GENERATORS ---
    fun getYearProgressString(): String {
        val current = getCurrentDayOfYear().toFloat()
        val total = getTotalDaysInYear().toFloat()
        val percent = (current / total) * 100
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return "%.1f%% of %d is DONE".format(percent, year)
    }

    fun getMonthProgressString(): String {
        val current = getCurrentDayOfMonth().toFloat()
        val total = getTotalDaysInMonth().toFloat()
        val percent = (current / total) * 100
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Calendar.getInstance().time)
        return "%.1f%% of %s is DONE".format(percent, monthName)
    }

    fun getDayProgressString(): String {
        val percent = getDayProgress() * 100
        val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(Calendar.getInstance().time)
        return "%.1f%% of %s is DONE".format(percent, dayName)
    }

    fun getDaysLeftString(endMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = endMillis - now
        val daysLeft = (diff / (1000 * 60 * 60 * 24)).toInt() + 1 // +1 to include today
        return if (daysLeft > 0) "$daysLeft Days Left" else "GOAL REACHED!"
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}