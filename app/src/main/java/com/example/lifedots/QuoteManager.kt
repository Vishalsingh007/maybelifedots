package com.example.lifedots

import java.util.Locale

object QuoteManager {

    // --- 1. EXPANDED QUOTE LIBRARY (NOTIFICATIONS) ---

    private val sergeantQuotes = listOf(
        "MAGGOT! WHY ARE YOU ON THIS PHONE?!",
        "DROP AND GIVE ME 20 MINUTES OF WORK!",
        "IS THIS VICTORY? NO! THIS IS SCROLLING!",
        "PAIN IS WEAKNESS LEAVING THE BODY. SCROLLING IS WEAKNESS ENTERING IT!",
        "DO YOU WANT TO BE A LOSER FOREVER? LOCK THE PHONE!",
        "YOUR ANCESTORS ARE WEEPING LOOKING AT YOU!",
        "GET OFF THE SCREEN SOLDIER! MOVE MOVE MOVE!",
        "DISCIPLINE EQUALS FREEDOM. YOU HAVE NEITHER!",
        "YOU HAVE TIME TO SCROLL? YOU HAVE TIME TO WORK!",
        "I AM WATCHING YOU WASTE YOUR POTENTIAL!",
        "STOP WHINING AND START GRINDING!",
        "YOUR COMPETITION IS WORKING RIGHT NOW. ARE YOU?",
        "LOCK IT UP! THAT IS AN ORDER!",
        "FAILURE IS NOT AN OPTION. SCROLLING IS FAILURE!",
        "DO NOT TEST MY PATIENCE MAGGOT!"
    )

    private val stoicQuotes = listOf(
        "Time is the most valuable thing a man can spend.",
        "Waste no more time arguing what a good man should be. Be one.",
        "You could leave life right now. Let that determine what you do.",
        "Is this necessary?",
        "Focus on what is in your control. This screen is distractions.",
        "Death smiles at us all, but all a man can do is smile back.",
        "The present moment is all you have.",
        "It is not that we have a short time to live, but that we waste a lot of it.",
        "He who fears death will never do anything worth of a man who is alive.",
        "No man is free who is not master of himself.",
        "To be everywhere is to be nowhere.",
        "Suffering arises from trying to control what is uncontrollable.",
        "The soul becomes dyed with the color of its thoughts.",
        "Review your day. Did you waste it?",
        "Act as if what you do makes a difference. It does."
    )

    private val chillQuotes = listOf(
        "Yo, maybe take a break?",
        "Screen time is high, vibe is low.",
        "Touch grass, my friend.",
        "You got this, just put the phone down.",
        "Life is happening outside, bro.",
        "Do it for the plot. Lock the phone.",
        "Protect your peace. Close the app.",
        "Digital detox? Just a thought.",
        "Hydrate and lock the screen.",
        "Don't doom scroll. Go for a walk.",
        "You look tired. Rest your eyes.",
        "The internet will still be here later.",
        "Go pet a dog or something.",
        "Less scrolling, more living.",
        "Breathe in. Breathe out. Lock phone."
    )

    fun getMessage(style: String): String {
        val list = when (style) {
            "sergeant" -> sergeantQuotes
            "stoic" -> stoicQuotes
            "chill" -> chillQuotes
            else -> stoicQuotes
        }
        return list.random()
    }

    fun getTitle(style: String): String {
        return when (style) {
            "sergeant" -> "DRILL SERGEANT SAYS:"
            "stoic" -> "Memento Mori"
            "chill" -> "Hey Bestie"
            else -> "LifeDots"
        }
    }

    // --- 2. DYNAMIC WALLPAPER TEXT LOGIC ---

    fun getWallpaperText(style: String, percent: Float, timeUnit: String): String {
        val p = String.format(Locale.US, "%.1f", percent)

        return when (style) {
            "sergeant" -> "$p% WASTED! MOVE IT!"
            "stoic" -> "$p% has passed. Memento Mori."
            "chill" -> "$p% gone. No stress, just vibes."
            else -> "$p% of $timeUnit is DONE" // Default
        }
    }

    // Helper for Goal Text
    fun getGoalText(style: String, daysLeft: Long, goalName: String): String {
        return when (style) {
            "sergeant" -> "$daysLeft DAYS LEFT! DON'T FAIL $goalName!"
            "stoic" -> "$daysLeft days remain for $goalName. Act now."
            "chill" -> "$daysLeft days til $goalName. You got this."
            else -> "$goalName\n$daysLeft Days Left"
        }
    }
}