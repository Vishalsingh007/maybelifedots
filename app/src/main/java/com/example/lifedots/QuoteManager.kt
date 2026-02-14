package com.example.lifedots

import java.util.Locale
import kotlin.random.Random

object QuoteManager {

    // --- BUTTON TEXT ---
    fun getButtonText(severity: Int): String {
        return when (severity) {
            0 -> "I NEED A FEW MINUTES"
            1 -> "I HAVE NO SELF CONTROL (UNLOCK)"
            else -> "I AM WASTING MY LIFE (UNLOCK)"
        }
    }

    // --- TYPING CHALLENGES ---
    private val challengePhrases = listOf(
        "I am wasting my life",
        "I have no discipline",
        "I surrender to dopamine",
        "Time is gone forever",
        "I choose distraction",
        "Focus is for the strong",
        "I am feeding the addiction",
        "Tomorrow I will regret this",
        "Comfort is the enemy",
        "I am killing my potential"
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
                "You could have planned your day.",
                "You could have just breathed.",
                "You could have drank a glass of water."
            )
            minutesUsed < 30 -> listOf(
                "You could have read a chapter.",
                "You could have called your parents.",
                "You could have cleaned your room.",
                "You could have meditated.",
                "You could have written a journal entry.",
                "You could have learned 5 new words."
            )
            minutesUsed < 60 -> listOf(
                "You could have finished a workout.",
                "You could have cooked a meal.",
                "You could have learned a coding concept.",
                "You could have gone for a run.",
                "You could have worked on your side project.",
                "You could have listened to a podcast."
            )
            minutesUsed < 120 -> listOf(
                "You could have watched a masterpiece movie.",
                "You could have finished a small book.",
                "You could have visited a friend.",
                "You could have deep-cleaned your house."
            )
            else -> listOf(
                "You could have driven to another city.",
                "You could have mastered a new skill.",
                "You could have built something valuable.",
                "You could have changed your life today."
            )
        }
        return list.random()
    }

    // --- QUOTE LIBRARY ---

    // 1. STOIC (Philosophical, Dark, Serious)
    private val stoicLevel0 = listOf(
        "Time is the currency of life.",
        "Focus on what matters.",
        "Is this necessary?",
        "Memento Mori.",
        "The sun sets without your permission."
    )
    private val stoicLevel1 = listOf(
        "You are wasting your potential.",
        "A man who delays is lost.",
        "Why do you persist in error?",
        "Regret is expensive.",
        "You act as if you have a thousand years.",
        "What you leave undone, dies with you."
    )
    private val stoicLevel2 = listOf(
        "You are a slave to your impulses.",
        "Your life is slipping away.",
        "You have no discipline.",
        "Shameful display.",
        "You are digging your own grave with comfort.",
        "History will not remember this moment."
    )

    // 2. SERGEANT (Aggressive, Military, Shouting)
    private val sergeantLevel0 = listOf(
        "LOCK IT UP!",
        "DISCIPLINE EQUALS FREEDOM!",
        "GET BACK TO WORK!",
        "NO SLACKING!",
        "EYES FRONT, SOLDIER!"
    )
    private val sergeantLevel1 = listOf(
        "ARE YOU DEAF SOLDIER?!",
        "I SAID DROP IT!",
        "WEAKNESS DISGUSTS ME!",
        "DO NOT TEST ME!",
        "MY GRANDMA SCROLLS FASTER!",
        "PAIN IS WEAKNESS LEAVING THE BODY!"
    )
    private val sergeantLevel2 = listOf(
        "YOU ARE PATHETIC!",
        "DO I NEED TO CONFISCATE THIS?!",
        "FAILURE IS YOUR ONLY SKILL!",
        "GET OUT OF MY SIGHT!",
        "YOU ARE A DISGRACE TO THE UNIFORM!",
        "DROP AND GIVE ME 50!"
    )

    // 3. CHILL (Gen Z, Relaxed, Passive-Aggressive)
    private val chillLevel0 = listOf(
        "Take a break, maybe?",
        "Vibe check: Failed.",
        "Touch grass.",
        "Just put it down.",
        "Bestie, no.",
        "It's not that deep."
    )
    private val chillLevel1 = listOf(
        "Bro, for real?",
        "You're cooking your brain.",
        "Stop scrolling, start living.",
        "It's not worth it.",
        "Low key addicted.",
        "This ain't it, chief."
    )
    private val chillLevel2 = listOf(
        "You're down bad.",
        "This is actually sad.",
        "Go outside. Now.",
        "Brain rot level: Critical.",
        "I'm embarrassed for you.",
        "Touch grass immediately."
    )

    fun getMessage(style: String, severity: Int): String {
        val list = when (style) {
            "sergeant" -> when (severity) { 0 -> sergeantLevel0; 1 -> sergeantLevel1; else -> sergeantLevel2 }
            "chill" -> when (severity) { 0 -> chillLevel0; 1 -> chillLevel1; else -> chillLevel2 }
            else -> when (severity) { 0 -> stoicLevel0; 1 -> stoicLevel1; else -> stoicLevel2 }
        }
        return list.random()
    }

    fun getTitle(style: String): String {
        return when (style) {
            "sergeant" -> "DRILL SERGEANT"
            "stoic" -> "MEMENTO MORI"
            "chill" -> "HEY BESTIE"
            else -> "LIFEDOTS"
        }
    }

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
        return when (style) {
            "sergeant" -> "$daysLeft DAYS MAGGOT! WORK!"
            "stoic" -> "$daysLeft days remain for $goalName."
            "chill" -> "$daysLeft days left. You got this."
            else -> "$daysLeft Days Left: $goalName"
        }
    }
}