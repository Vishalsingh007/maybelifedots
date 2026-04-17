package com.example.lifedots

import android.content.Context
import java.util.Locale

object QuoteManager {

    private const val PREF_NAME = "CustomQuotesPrefs"
    private const val KEY_QUOTES = "saved_custom_quotes"
    private const val KEY_MIX_MODE = "quote_mode_mix"

    // --- ESCALATING TYPING CHALLENGES (Progressive Friction for Strict Mode) ---
    fun getProgressiveChallenge(level: Int): String {
        return when (level) {
            1 -> "I am feeding my addiction."
            2 -> "I am actively choosing dopamine over my goals."
            3 -> "I acknowledge that I am abandoning my discipline for a temporary distraction."
            4 -> "I am surrendering my self-control. I am deciding to sabotage my own productivity, knowing this screen is stealing my time."
            else -> "I have completely lost my discipline. I am actively deciding to throw away my future for cheap dopamine. I know my time is finite and I am never getting these minutes back, yet I am choosing to waste them anyway. I accept my weakness."
        }
    }

    // --- STANDARD TYPING CHALLENGES (Fallback) ---
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

    fun getChallengePhrase(context: Context): String {
        val customQuotes = getCustomQuotes(context)

        if (customQuotes.isNotEmpty()) {
            if (isMixMode(context)) {
                // Combine default phrases with the user's custom phrases
                val combinedPool = challengePhrases + customQuotes.toList()
                return combinedPool.random()
            } else {
                // ONLY use the user's custom phrases
                return customQuotes.random()
            }
        }
        return challengePhrases.random() // Fallback to existing defaults if no custom quotes
    }

    // --- CUSTOM QUOTE STORAGE LOGIC ---
    fun getCustomQuotes(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_QUOTES, emptySet()) ?: emptySet()
    }

    fun addCustomQuote(context: Context, quote: String): Boolean {
        val currentSet = getCustomQuotes(context).toMutableSet()
        if (currentSet.size >= 5) return false // Enforce the limit of 5

        currentSet.add(quote)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_QUOTES, currentSet).apply()
        return true
    }

    fun clearCustomQuotes(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_QUOTES).apply()
    }

    // --- QUOTE MODE LOGIC (Mix vs Custom Only) ---
    fun isMixMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_MIX_MODE, true) // Default is true (Mix mode)
    }

    fun setMixMode(context: Context, isMix: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MIX_MODE, isMix).apply()
    }

    // --- MASSIVE OPPORTUNITY COST ENGINE ---
    fun getOpportunityCost(minutesUsed: Long): String {
        val list = when {
            minutesUsed < 15 -> listOf(
                "That's 15 minutes gone forever.",
                "You could have done 20 pushups.",
                "You could have just breathed.",
                "You could have drank a glass of water.",
                "You could have stretched your back.",
                "You could have organized your desk.",
                "You could have brainstormed 3 new ideas.",
                "You could have reviewed your daily goals.",
                "You could have made your bed.",
                "You could have messaged an old friend.",
                "You could have washed your face.",
                "You could have written down one thing you are grateful for.",
                "You could have practiced a new language for a few minutes.",
                "You could have done a quick plank.",
                "You could have cleared your computer's downloads folder.",
                "You could have stepped outside for some sunlight.",
                "You could have outlined tomorrow's to-do list.",
                "You could have read an informative article.",
                "You could have practiced mindful meditation.",
                "You could have rested your eyes from the screen.",
                "You could have done a quick 10-minute yoga routine."
            )
            minutesUsed < 30 -> listOf(
                "You could have read a chapter.",
                "You could have called your parents.",
                "You could have written a journal entry.",
                "You could have learned 5 new words.",
                "You could have gone for a brisk walk.",
                "You could have prepped healthy snacks for the day.",
                "You could have listened to a highly educational podcast.",
                "You could have cleaned your room.",
                "You could have drafted an important email.",
                "You could have sketched or doodled.",
                "You could have done a HIIT workout.",
                "You could have learned a new guitar chord.",
                "You could have researched a topic you're curious about.",
                "You could have watered your plants and tidied up.",
                "You could have read a case study in your field.",
                "You could have practiced deep breathing exercises.",
                "You could have paid your bills and organized finances.",
                "You could have reviewed your weekly budget.",
                "You could have planned your meals for the week.",
                "You could have taken a refreshing power nap."
            )
            minutesUsed < 60 -> listOf(
                "You could have finished a workout.",
                "You could have cooked a healthy meal.",
                "You could have learned a coding concept.",
                "You could have gone for a long run.",
                "You could have read 50 pages of a book.",
                "You could have written a blog post.",
                "You could have fixed that broken thing in your house.",
                "You could have watched an entire documentary.",
                "You could have finished a module in an online course.",
                "You could have done a full grocery run.",
                "You could have completely zeroed out your inbox.",
                "You could have called a mentor or networked.",
                "You could have started learning a new software tool.",
                "You could have done a deep stretching or mobility session.",
                "You could have baked something from scratch.",
                "You could have mapped out a side hustle business plan.",
                "You could have caught up on important industry news.",
                "You could have practiced an instrument seriously.",
                "You could have decluttered your entire closet.",
                "You could have spent quality, uninterrupted time with family."
            )
            minutesUsed < 120 -> listOf(
                "You could have watched a masterpiece movie.",
                "You could have finished a small book.",
                "You could have deep-cleaned your house.",
                "You could have coded an entire feature.",
                "You could have attended a masterclass.",
                "You could have written a short story.",
                "You could have visited a museum or park.",
                "You could have driven to a scenic hiking trail.",
                "You could have built a landing page for a project.",
                "You could have detailed your monthly goals and budget.",
                "You could have meal-prepped for the entire week.",
                "You could have had a long, deep conversation over coffee.",
                "You could have completed a complex jigsaw puzzle.",
                "You could have designed a new logo or artwork.",
                "You could have taken a professional certification exam.",
                "You could have learned the basics of a new framework.",
                "You could have completely detailed and washed your car.",
                "You could have sorted through months of digital clutter.",
                "You could have played a full game of chess.",
                "You could have crafted a comprehensive financial plan."
            )
            else -> listOf(
                "You could have driven to another city.",
                "You could have mastered a new skill.",
                "You could have built something valuable.",
                "You could have completely redesigned your portfolio.",
                "You could have read half of a novel.",
                "You could have launched a minimal viable product (MVP).",
                "You could have gone on a mini road trip.",
                "You could have re-painted a room in your house.",
                "You could have coded a basic mobile app from scratch.",
                "You could have attended a half-day seminar.",
                "You could have planted a small garden.",
                "You could have binged a highly educational lecture series.",
                "You could have explored a neighboring town.",
                "You could have completed a massive physical challenge.",
                "You could have built a piece of furniture.",
                "You could have written a business proposal.",
                "You could have gone surfing or skiing.",
                "You could have organized a community event or meetup.",
                "You could have completely reset your sleep schedule.",
                "You could have spent a transformative afternoon offline."
            )
        }
        return list.random()
    }

    // --- NOTIFICATION & WALLPAPER HELPERS ---
    fun getMessage(style: String, severity: Int): String {
        return "Stay Focused."
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