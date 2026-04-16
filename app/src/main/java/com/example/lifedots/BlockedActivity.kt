package com.example.lifedots

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.lifedots.graphics.UsageGraphView
import com.example.lifedots.logic.LimitManager
import com.example.lifedots.logic.UsageStatsHelper

class BlockedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.overlay_blocked)

        triggerHeavyBuzz()
        startBreathingAnimation()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })

        val appName = intent.getStringExtra("BLOCKED_APP_NAME") ?: "App"
        val packageName = intent.getStringExtra("BLOCKED_PACKAGE") ?: ""

        findViewById<TextView>(R.id.tvBlockedAppName).text = appName

        try {
            val icon = packageManager.getApplicationIcon(packageName)
            findViewById<ImageView>(R.id.imgBlockedIcon).setImageDrawable(icon)
        } catch (e: Exception) {}

        val hourlyData = UsageStatsHelper.getAppUsageHourly(this, packageName)
        // The graph view will now automatically animate when data is set!
        findViewById<UsageGraphView>(R.id.usageGraph).setData(hourlyData)

        val totalMinutes = hourlyData.sum().toLong()
        val oppCost = QuoteManager.getOpportunityCost(totalMinutes)
        findViewById<TextView>(R.id.tvQuote).text = "\"$oppCost\""

        // --- BUTTONS ---
        findViewById<Button>(R.id.btnIntentQuick).setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            handleUnlockRequest(packageName, 1)
        }

        findViewById<Button>(R.id.btnIntentBored).setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            handleUnlockRequest(packageName, 5)
        }

        val btnCustom = findViewById<Button>(R.id.btnIntentShame)
        btnCustom.text = "CUSTOM TIME"
        btnCustom.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showSliderDialog(packageName)
        }

        val btnClose = findViewById<Button>(R.id.btnCloseApp)
        btnClose.text = "I CHOOSE TO WIN"
        btnClose.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            goHome()
        }
    }

    // --- ZEN PSYCHOLOGY: VISIBLE RED BREATHING ---
    private fun startBreathingAnimation() {
        val rootView = findViewById<android.view.View>(android.R.id.content).rootView

        // Pulses from a dark crimson red to pitch black so it is clearly visible
        val colorAnim = ValueAnimator.ofArgb(Color.parseColor("#44FF0000"), Color.parseColor("#FF000000"))
        colorAnim.duration = 3500L
        colorAnim.repeatMode = ValueAnimator.REVERSE
        colorAnim.repeatCount = ValueAnimator.INFINITE
        colorAnim.interpolator = AccelerateDecelerateInterpolator()

        colorAnim.addUpdateListener { animator ->
            rootView.setBackgroundColor(animator.animatedValue as Int)
        }
        colorAnim.start()
    }

    // --- EXPLICIT HARDWARE HAPTIC TICK ---
    private fun tickHaptic() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Extremely short 15ms pulse for a crisp "tick" feel
                vibrator.vibrate(VibrationEffect.createOneShot(15, 100))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        } catch (e: Exception) {}
    }

    private fun triggerHeavyBuzz() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 150, 100, 150)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 150, 100, 150), -1)
            }
        } catch (e: Exception) {}
    }

    // --- GATEKEEPER LOGIC ---
    private fun handleUnlockRequest(packageName: String, minutes: Int) {
        val count = LimitManager.getExtensionCount(this)

        if (count == 0) {
            applyWhitelist(packageName, minutes)
            Toast.makeText(this, "Time Added. Use it wisely.", Toast.LENGTH_SHORT).show()
        } else {
            showTypingChallenge(packageName, minutes)
        }
    }

    private fun applyWhitelist(packageName: String, minutes: Int) {
        LimitManager.setWhitelist(this, packageName, minutes)
        launchBlockedApp(packageName)
    }

    private fun showSliderDialog(packageName: String) {
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(60, 40, 60, 20)

        val tvTime = TextView(this)
        tvTime.text = "5 minutes"
        tvTime.textSize = 24f
        tvTime.gravity = android.view.Gravity.CENTER
        container.addView(tvTime)

        val seekBar = SeekBar(this)
        seekBar.max = 14
        seekBar.progress = 4
        container.addView(seekBar)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, b: Boolean) {
                tvTime.text = "${p + 1} minutes"
                tickHaptic() // Hardware motor pulse

                // --- NEW: Audio tick sound effect ---
                s?.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        AlertDialog.Builder(this)
            .setTitle("How much do you need?")
            .setView(container)
            .setPositiveButton("NEXT") { _, _ ->
                val minutes = seekBar.progress + 1
                handleUnlockRequest(packageName, minutes)
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun showTypingChallenge(packageName: String, minutes: Int) {
        // Fetch challenge phrase, prioritizing custom quotes if they exist
        val challengePhrase = QuoteManager.getChallengePhrase(this)

        val input = EditText(this)
        input.hint = "Type: '$challengePhrase'"
        input.setPadding(50, 50, 50, 50)

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(50, 0, 50, 0)
        container.addView(input)

        AlertDialog.Builder(this)
            .setTitle("Prove your need.")
            .setMessage("To unlock $minutes more minutes, type exactly:\n\n\"$challengePhrase\"")
            .setView(container)
            .setCancelable(false)
            .setPositiveButton("UNLOCK") { _, _ ->
                val text = input.text.toString().trim()
                if (text.equals(challengePhrase, ignoreCase = true)) {
                    applyWhitelist(packageName, minutes)
                } else {
                    Toast.makeText(this, "WRONG. Focus preserved.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("GIVE UP") { dialog, _ ->
                dialog.dismiss()
                goHome()
            }
            .show()
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
        finish()
    }

    private fun launchBlockedApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            finish()
        } catch (e: Exception) {
            finish()
        }
    }
}