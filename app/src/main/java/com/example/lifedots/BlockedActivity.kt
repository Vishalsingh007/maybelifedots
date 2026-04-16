package com.example.lifedots

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
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

        // --- BUTTONS ---

        // 1. Quick Check (1 min)
        findViewById<Button>(R.id.btnIntentQuick).setOnClickListener {
            handleUnlockRequest(packageName, 1)
        }

        // 2. I'm Bored (5 mins)
        findViewById<Button>(R.id.btnIntentBored).setOnClickListener {
            handleUnlockRequest(packageName, 5)
        }

        // 3. Custom Slider (1-15 mins)
        val btnCustom = findViewById<Button>(R.id.btnIntentShame)
        btnCustom.text = "CUSTOM TIME"
        btnCustom.setOnClickListener {
            showSliderDialog(packageName)
        }

        // 4. Win Button
        val btnClose = findViewById<Button>(R.id.btnCloseApp)
        btnClose.text = "I CHOOSE TO WIN"
        btnClose.setOnClickListener { goHome() }
    }

    // --- GATEKEEPER LOGIC ---
    private fun handleUnlockRequest(packageName: String, minutes: Int) {
        val count = LimitManager.getExtensionCount(this)

        if (count == 0) {
            // First time is FREE -> Calls Whitelist (Golden Ticket)
            applyWhitelist(packageName, minutes)
            Toast.makeText(this, "Time Added. Use it wisely.", Toast.LENGTH_SHORT).show()
        } else {
            // Second time requires WORK
            showTypingChallenge(packageName, minutes)
        }
    }

    // FIX: Use setWhitelist instead of addExtension
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
        val challengePhrase = QuoteManager.getChallengePhrase()

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