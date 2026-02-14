package com.example.lifedots

import java.util.Locale

object QuoteManager {

    // LEVEL 0: Standard Logic/Warning
    private val stoicLevel0 = listOf("Time is the currency of life.", "Focus on what matters.", "Is this necessary?", "Memento Mori.")
    private val sergeantLevel0 = listOf("LOCK IT UP!", "DISCIPLINE EQUALS FREEDOM!", "GET BACK TO WORK!", "NO SLACKING!")
    private val chillLevel0 = listOf("Take a break, maybe?", "Vibe check: Failed.", "Touch grass.", "Just put it down.")

    // LEVEL 1: Annoyed / Urgent
    private val stoicLevel1 = listOf("You are wasting your potential.", "A man who delays is lost.", "Why do you persist in error?", "Regret is expensive.")
    private val sergeantLevel1 = listOf("ARE YOU DEAF SOLDIER?!", "I SAID DROP IT!", "WEAKNESS DISGUSTS ME!", "DO NOT TEST ME!")
    private val chillLevel1 = listOf("Bro, for real?", "You're cooking your brain.", "Stop scrolling, start living.", "It's not worth it.")

    // LEVEL 2: Critical / Insulting (High Usage/Multiple Extensions)
    private val stoicLevel2 = listOf("You are a slave to your impulses.", "Your life is slipping away.", "You have no discipline.", "Shameful display.")
    private val sergeantLevel2 = listOf("YOU ARE PATHETIC!", "MY GRANDMOTHER HAS MORE DISCIPLINE!", "DO I NEED TO CONFISCATE THIS?!", "FAILURE!")
    private val chillLevel2 = listOf("You're addicted, fam.", "This is actually sad.", "Go outside. Now.", "Brain rot level: Critical.")

    fun getMessage(style: String, severity: Int): String {
        val list = when (style) {
            "sergeant" -> when (severity) { 0 -> sergeantLevel0; 1 -> sergeantLevel1; else -> sergeantLevel2 }
            "chill" -> when (severity) { 0 -> chillLevel0; 1 -> chillLevel1; else -> chillLevel2 }
            else -> when (severity) { 0 -> stoicLevel0; 1 -> stoicLevel1; else -> stoicLevel2 }
        }
        return list.random()
    }

    fun getTitle(style: String): String {
        return when (style) { "sergeant" -> "DRILL SERGEANT"; "stoic" -> "MEMENTO MORI"; "chill" -> "HEY BESTIE"; else -> "LIFEDOTS" }
    }

    // Wallpaper Text Logic
    fun getWallpaperText(style: String, percent: Float, timeUnit: String): String {
        val p = String.format(Locale.US, "%.1f", percent)
        return when (style) {
            "sergeant" -> "$p% of $timeUnit WASTED! MOVE IT!"
            "stoic" -> "$p% of $timeUnit has passed."
            "chill" -> "$p% of $timeUnit gone. No stress."
            else -> "$p% of $timeUnit is DONE"
        }
    }

    fun getGoalText(style: String, daysLeft: Long, goalName: String): String {
        return "$daysLeft days til $goalName."
    }
}