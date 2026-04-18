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
import android.view.View
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

    private var isDeepWorkMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.overlay_blocked)

        triggerHeavyBuzz()
        startBreathingAnimation()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })

        refreshUI() // Load it the first time
    }

    // --- FIX: THE STUCK ICON BUG ---
    // This triggers when the Block Screen is ALREADY open, but a NEW app tries to launch
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the Intent data to the NEW app
        refreshUI() // Reload the UI with the new icon and name
    }

    private fun refreshUI() {
        val appName = intent.getStringExtra("BLOCKED_APP_NAME") ?: "App"
        val packageName = intent.getStringExtra("BLOCKED_PACKAGE") ?: ""

        isDeepWorkMode = appName == "DEEP WORK ACTIVE"

        findViewById<TextView>(R.id.tvBlockedAppName).text = appName

        // Update Icon dynamically
        try {
            val icon = packageManager.getApplicationIcon(packageName)
            findViewById<ImageView>(R.id.imgBlockedIcon).setImageDrawable(icon)
        } catch (e: Exception) {}

        // Update Graph dynamically
        val hourlyData = UsageStatsHelper.getAppUsageHourly(this, packageName)
        findViewById<UsageGraphView>(R.id.usageGraph).setData(hourlyData)

        // Update Quote dynamically
        val totalMinutes = hourlyData.sum().toLong()
        val oppCost = QuoteManager.getOpportunityCost(totalMinutes)
        findViewById<TextView>(R.id.tvQuote).text = "\"$oppCost\""

        val btnQuick = findViewById<Button>(R.id.btnIntentQuick)
        val btnBored = findViewById<Button>(R.id.btnIntentBored)
        val btnCustom = findViewById<Button>(R.id.btnIntentShame)
        val btnClose = findViewById<Button>(R.id.btnCloseApp)

        // --- DEEP WORK ENGINE UI OVERRIDE ---
        if (isDeepWorkMode) {
            btnQuick.visibility = View.GONE
            btnBored.visibility = View.GONE
            btnCustom.visibility = View.GONE

            btnClose.text = "EMERGENCY ABORT SPRINT"
            btnClose.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF4444")) // Red
            btnClose.setTextColor(Color.WHITE)

            val remainingMillis = LimitManager.getFocusModeEndTime(this) - System.currentTimeMillis()
            val remainingMins = (remainingMillis / 1000 / 60).coerceAtLeast(1)
            findViewById<TextView>(R.id.tvQuote).text = "$remainingMins Minutes remaining in your sprint. Get back to work."

            btnClose.setOnClickListener { view ->
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                showTypingChallenge(packageName, 0, 5) // Force a Level 5 paragraph challenge to abort
            }
        } else {
            // Standard Reactive Limits Mode
            btnQuick.visibility = View.VISIBLE
            btnBored.visibility = View.VISIBLE
            btnCustom.visibility = View.VISIBLE

            btnClose.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#18181B"))

            btnQuick.setOnClickListener { view ->
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                handleUnlockRequest(packageName, 1)
            }

            btnBored.setOnClickListener { view ->
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                handleUnlockRequest(packageName, 5)
            }

            btnCustom.text = "CUSTOM TIME"
            btnCustom.setOnClickListener { view ->
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                showSliderDialog(packageName)
            }

            btnClose.text = "I CHOOSE TO WIN"
            btnClose.setOnClickListener { view ->
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                goHome()
            }
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

    private fun showTypingChallenge(packageName: String, minutes: Int, level: Int) {
        val activeLevel = if (isDeepWorkMode) 5 else level
        val challengePhrase = QuoteManager.getProgressiveChallenge(activeLevel)

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

        val titleText = if (isDeepWorkMode) "ABORT SPRINT" else "Level $level Friction"
        val titleColor = if (isDeepWorkMode) "#FF4444" else "#00F0FF"

        val title = TextView(this).apply {
            text = titleText
            setTextColor(Color.parseColor(titleColor))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 20 }
        }
        container.addView(title)

        val descText = if (isDeepWorkMode) "To ABORT your deep work session, type exactly:\n\n\"$challengePhrase\"" else "To unlock $minutes more minutes, type exactly:\n\n\"$challengePhrase\""

        val desc = TextView(this).apply {
            text = descText
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
            text = if (isDeepWorkMode) "STAY FOCUSED" else "GIVE UP"
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#333333"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 10 }
            setOnClickListener {
                dialog.dismiss()
                goHome()
            }
        }

        val btnUnlock = Button(this).apply {
            text = if (isDeepWorkMode) "ABORT" else "UNLOCK"
            setTextColor(Color.BLACK)
            backgroundTintList = ColorStateList.valueOf(if (isDeepWorkMode) Color.parseColor("#FF4444") else Color.parseColor("#00F0FF"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 10 }
            setOnClickListener {
                val text = input.text.toString().trim()
                if (text.equals(challengePhrase, ignoreCase = true)) {
                    if (isDeepWorkMode) {
                        val stopIntent = Intent(this@BlockedActivity, FocusSessionService::class.java)
                        stopIntent.action = "STOP_FOCUS"
                        startService(stopIntent)
                        LimitManager.stopFocusMode(this@BlockedActivity)
                        Toast.makeText(this@BlockedActivity, "Sprint Aborted.", Toast.LENGTH_SHORT).show()
                        goHome()
                    } else {
                        applyWhitelist(packageName, minutes)
                    }
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