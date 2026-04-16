package com.example.lifedots

import java.util.Locale
import kotlin.random.Random

object QuoteManager {

    // --- TYPING CHALLENGES (Updated for The Bouncer) ---
    private val challengePhrases = listOf(
        "I have no self control",
        "I am feeding my addiction",
        "I choose dopamine over goals",
        "I am wasting my potential",
        "Tomorrow I will regret this",
        "I surrender to the algorithm",
        "My discipline is weak today",
        "This app owns me"
    )

    fun getChallengePhrase(): String {
        return challengePhrases.random()
    }

    // --- OPPORTUNITY COST ENGINE ---
    fun getOpportunityCost(minutesUsed: Long): String {
        val list = when {
            minutesUsed < 15 -> listOf(
                "That's 15 minutes gone forever.",
                "You could have done 20 pushups.",
                "You could have just breathed.",
                "You could have drank a glass of water."
            )
            minutesUsed < 30 -> listOf(
                "You could have read a chapter.",
                "You could have called your parents.",
                "You could have written a journal entry.",
                "You could have learned 5 new words."
            )
            minutesUsed < 60 -> listOf(
                "You could have finished a workout.",
                "You could have cooked a meal.",
                "You could have learned a coding concept.",
                "You could have gone for a run."
            )
            minutesUsed < 120 -> listOf(
                "You could have watched a masterpiece movie.",
                "You could have finished a small book.",
                "You could have deep-cleaned your house."
            )
            else -> listOf(
                "You could have driven to another city.",
                "You could have mastered a new skill.",
                "You could have built something valuable."
            )
        }
        return list.random()
    }

    // --- NOTIFICATION & WALLPAPER HELPERS ---
    fun getMessage(style: String, severity: Int): String {
        return "Stay Focused." // Default fallback, replace with full lists if needed
    }

    fun getTitle(style: String): String {
        return "LIFEDOTS"
    }

    fun getWallpaperText(style: String, percent: Float, timeUnit: String): String {
        val p = String.format(Locale.US, "%.1f", percent)
        return "$p% of $timeUnit is DONE"
    }

    fun getGoalText(style: String, daysLeft: Long, goalName: String): String {
        return "$daysLeft Days Left: $goalName"
    }
}