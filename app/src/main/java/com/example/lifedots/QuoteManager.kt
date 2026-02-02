package com.example.lifedots

object QuoteManager {

    // 1. The Drill Sergeant (Tough Love)
    private val sergeantQuotes = listOf(
        "MAGGOT! WHY ARE YOU ON THIS PHONE?!",
        "DROP AND GIVE ME 20 MINUTES OF WORK!",
        "IS THIS VICTORY? NO! THIS IS SCROLLING!",
        "PAIN IS WEAKNESS LEAVING THE BODY. SCROLLING IS WEAKNESS ENTERING IT!",
        "DO YOU WANT TO BE A LOSER FOREVER? LOCK THE PHONE!",
        "YOUR ANCESTORS ARE WEEPING LOOKING AT YOU!",
        "GET OFF THE SCREEN SOLDIER! MOVE MOVE MOVE!"
    )

    // 2. The Stoic Philosopher (Gentle/Deep)
    private val stoicQuotes = listOf(
        "Time is the most valuable thing a man can spend.",
        "Waste no more time arguing what a good man should be. Be one.",
        "You could leave life right now. Let that determine what you do.",
        "Is this necessary?",
        "Focus on what is in your control. This screen is distractions.",
        "Death smiles at us all, but all a man can do is smile back.",
        "The present moment is all you have."
    )

    // 3. The Chill Bestie (Relaxed)
    private val chillQuotes = listOf(
        "Yo, maybe take a break?",
        "Screen time is high, vibe is low.",
        "Touch grass, my friend.",
        "You got this, just put the phone down.",
        "Life is happening outside, bro.",
        "Do it for the plot. Lock the phone."
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
}