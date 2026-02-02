package com.example.lifedots

import java.util.Locale

object QuoteManager {

    // --- 1. EXPANDED QUOTE LIBRARY (NOTIFICATIONS) ---
    // (This part stays the same, keeping it short here for clarity)
    private val sergeantQuotes = listOf(
        "MAGGOT! WHY ARE YOU ON THIS PHONE?!", "DROP AND GIVE ME 20 MINUTES OF WORK!", "IS THIS VICTORY? NO! THIS IS SCROLLING!", "PAIN IS WEAKNESS LEAVING THE BODY!", "LOCK THE PHONE!", "YOUR ANCESTORS ARE WEEPING!", "GET OFF THE SCREEN SOLDIER!", "DISCIPLINE EQUALS FREEDOM!", "YOU HAVE TIME TO SCROLL? YOU HAVE TIME TO WORK!", "I AM WATCHING YOU!", "STOP WHINING AND START GRINDING!", "YOUR COMPETITION IS WORKING!", "LOCK IT UP!", "FAILURE IS NOT AN OPTION!", "DO NOT TEST MY PATIENCE!"
    )

    private val stoicQuotes = listOf(
        "Time is the most valuable thing a man can spend.", "Waste no more time arguing what a good man should be. Be one.", "You could leave life right now.", "Is this necessary?", "Focus on what is in your control.", "Death smiles at us all.", "The present moment is all you have.", "It is not that we have a short time to live, but that we waste a lot of it.", "He who fears death will never do anything worth of a man who is alive.", "No man is free who is not master of himself.", "To be everywhere is to be nowhere.", "Review your day. Did you waste it?", "Act as if what you do makes a difference."
    )

    private val chillQuotes = listOf(
        "Yo, maybe take a break?", "Screen time is high, vibe is low.", "Touch grass, my friend.", "You got this, just put the phone down.", "Life is happening outside, bro.", "Do it for the plot. Lock the phone.", "Protect your peace. Close the app.", "Digital detox? Just a thought.", "Hydrate and lock the screen.", "Don't doom scroll. Go for a walk.", "You look tired. Rest your eyes.", "The internet will still be here later.", "Go pet a dog.", "Less scrolling, more living."
    )

    fun getMessage(style: String): String {
        val list = when (style) { "sergeant" -> sergeantQuotes; "stoic" -> stoicQuotes; "chill" -> chillQuotes; else -> stoicQuotes }
        return list.random()
    }

    fun getTitle(style: String): String {
        return when (style) { "sergeant" -> "DRILL SERGEANT SAYS:"; "stoic" -> "Memento Mori"; "chill" -> "Hey Bestie"; else -> "LifeDots" }
    }

    // --- 2. FIXED WALLPAPER TEXT LOGIC (Added explicit Time Unit) ---

    fun getWallpaperText(style: String, percent: Float, timeUnit: String): String {
        val p = String.format(Locale.US, "%.1f", percent)

        return when (style) {
            // Now explicitly says "of the Year", "of the Day", etc.
            "sergeant" -> "$p% of $timeUnit WASTED! MOVE IT!"
            "stoic" -> "$p% of $timeUnit has passed. Memento Mori."
            "chill" -> "$p% of $timeUnit gone. No stress."
            else -> "$p% of $timeUnit is DONE"
        }
    }

    fun getGoalText(style: String, daysLeft: Long, goalName: String): String {
        return when (style) {
            "sergeant" -> "$daysLeft DAYS LEFT! DON'T FAIL $goalName!"
            "stoic" -> "$daysLeft days remain for $goalName. Act now."
            "chill" -> "$daysLeft days til $goalName. You got this."
            else -> "$goalName\n$daysLeft Days Left"
        }
    }
}