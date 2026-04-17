package com.example.lifedots

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
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
        findViewById<UsageGraphView>(R.id.usageGraph).setData(hourlyData)

        val totalMinutes = hourlyData.sum().toLong()
        val oppCost = QuoteManager.getOpportunityCost(totalMinutes)
        findViewById<TextView>(R.id.tvQuote).text = "\"$oppCost\""

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

    private fun startBreathingAnimation() {
        val rootView = findViewById<android.view.View>(android.R.id.content).rootView
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

    private fun tickHaptic() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

    private fun handleUnlockRequest(packageName: String, minutes: Int) {
        val count = LimitManager.getExtensionCount(this)

        if (count == 0) {
            applyWhitelist(packageName, minutes)
            Toast.makeText(this, "Time Added. Use it wisely.", Toast.LENGTH_SHORT).show()
        } else {
            showTypingChallenge(packageName, minutes, count)
        }
    }

    private fun applyWhitelist(packageName: String, minutes: Int) {
        LimitManager.setWhitelist(this, packageName, minutes)
        launchBlockedApp(packageName)
    }

    // --- 💎 100% CUSTOM DARK UI FOR SLIDER DIALOG ---
    private fun showSliderDialog(packageName: String) {
        val dialog = AlertDialog.Builder(this).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#18181B"))
                cornerRadius = 50f
                setStroke(2, Color.parseColor("#33FFFFFF"))
            }
        }

        val title = TextView(this).apply {
            text = "Custom Time"
            setTextColor(Color.parseColor("#A1A1AA"))
            textSize = 14f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 20 }
        }
        container.addView(title)

        val tvTime = TextView(this).apply {
            text = "5 minutes"
            textSize = 36f
            setTextColor(Color.parseColor("#00F0FF"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 40 }
        }
        container.addView(tvTime)

        val seekBar = SeekBar(this).apply {
            max = 14
            progress = 4
            progressTintList = ColorStateList.valueOf(Color.parseColor("#00F0FF"))
            thumbTintList = ColorStateList.valueOf(Color.parseColor("#00F0FF"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 60 }
        }
        container.addView(seekBar)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, b: Boolean) {
                tvTime.text = "${p + 1} minutes"
                tickHaptic()
                s?.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        val btnCancel = Button(this).apply {
            text = "CANCEL"
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#333333"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 10 }
            setOnClickListener { dialog.dismiss() }
        }

        val btnNext = Button(this).apply {
            text = "NEXT"
            setTextColor(Color.BLACK)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00F0FF"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 10 }
            setOnClickListener {
                val minutes = seekBar.progress + 1
                handleUnlockRequest(packageName, minutes)
                dialog.dismiss()
            }
        }

        btnRow.addView(btnCancel)
        btnRow.addView(btnNext)
        container.addView(btnRow)

        dialog.setView(container)
        dialog.show()
    }

    // --- 💎 100% CUSTOM DARK UI FOR TYPING CHALLENGE ---
    private fun showTypingChallenge(packageName: String, minutes: Int, level: Int) {
        val challengePhrase = QuoteManager.getProgressiveChallenge(level)
        val dialog = AlertDialog.Builder(this).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#18181B"))
                cornerRadius = 50f
                setStroke(2, Color.parseColor("#33FFFFFF"))
            }
        }

        val title = TextView(this).apply {
            text = "Level $level Friction"
            setTextColor(Color.parseColor("#FF4444")) // Red warning text
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 20 }
        }
        container.addView(title)

        val desc = TextView(this).apply {
            text = "To unlock $minutes more minutes, type exactly:\n\n\"$challengePhrase\""
            setTextColor(Color.parseColor("#A1A1AA"))
            textSize = 14f
            setLineSpacing(0f, 1.2f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 30 }
        }
        container.addView(desc)

        val input = EditText(this).apply {
            hint = "Type the phrase here..."
            setHintTextColor(Color.parseColor("#666666"))
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(40, 40, 40, 40)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            maxLines = 6
            gravity = Gravity.TOP or Gravity.START
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#09090B"))
                cornerRadius = 20f
                setStroke(2, Color.parseColor("#333333"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 40 }
        }
        container.addView(input)

        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        val btnGiveUp = Button(this).apply {
            text = "GIVE UP"
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#333333"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 10 }
            setOnClickListener {
                dialog.dismiss()
                goHome()
            }
        }

        val btnUnlock = Button(this).apply {
            text = "UNLOCK"
            setTextColor(Color.BLACK)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00F0FF"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 10 }
            setOnClickListener {
                val text = input.text.toString().trim()
                if (text.equals(challengePhrase, ignoreCase = true)) {
                    applyWhitelist(packageName, minutes)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this@BlockedActivity, "WRONG. Focus preserved.", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnRow.addView(btnGiveUp)
        btnRow.addView(btnUnlock)
        container.addView(btnRow)

        dialog.setView(container)
        dialog.show()
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