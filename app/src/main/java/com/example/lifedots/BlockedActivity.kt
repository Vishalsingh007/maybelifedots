package com.example.lifedots

import android.app.AlertDialog
import android.content.Context
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

        // 1. Graph & Stats
        val hourlyData = UsageStatsHelper.getAppUsageHourly(this, packageName)
        findViewById<UsageGraphView>(R.id.usageGraph).setData(hourlyData)

        // 2. Dynamic Opportunity Cost (Now Random)
        val totalMinutes = hourlyData.sum().toLong()
        val oppCost = QuoteManager.getOpportunityCost(totalMinutes)
        findViewById<TextView>(R.id.tvQuote).text = "\"$oppCost\""

        // 3. Dynamic Button Text (With "UNLOCK")
        val extensionCount = LimitManager.getExtensionCount(this)
        val btnAdd = findViewById<Button>(R.id.btnAddTime)

        val severity = if (extensionCount > 1) 2 else if (extensionCount > 0) 1 else 0
        btnAdd.text = QuoteManager.getButtonText(severity)

        findViewById<Button>(R.id.btnCloseApp).setOnClickListener { goHome() }

        // 4. Slider Challenge
        btnAdd.setOnClickListener {
            showTimePickerChallenge(packageName)
        }
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
        finish()
    }

    private fun showTimePickerChallenge(packageName: String) {
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(60, 40, 60, 20)

        val tvLabel = TextView(this)
        tvLabel.text = "How much more time do you need?"
        tvLabel.textSize = 16f
        container.addView(tvLabel)

        val seekBar = SeekBar(this)
        seekBar.max = 10
        seekBar.progress = 5
        container.addView(seekBar)

        val tvTime = TextView(this)
        tvTime.text = "5 minutes"
        tvTime.gravity = android.view.Gravity.CENTER
        tvTime.textSize = 24f
        container.addView(tvTime)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, b: Boolean) {
                tvTime.text = "$p minutes"
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        AlertDialog.Builder(this)
            .setTitle("Really?")
            .setView(container)
            .setPositiveButton("NEXT") { _, _ ->
                val minutes = seekBar.progress
                if (minutes > 0) {
                    showTypingChallenge(packageName, minutes)
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun showTypingChallenge(packageName: String, minutes: Int) {
        // FIXED: Now gets a random different phrase every time
        val challengePhrase = QuoteManager.getChallengePhrase()

        val input = EditText(this)
        input.hint = "Type: '$challengePhrase'"
        input.setPadding(50, 50, 50, 50)

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(50, 0, 50, 0)
        container.addView(input)

        AlertDialog.Builder(this)
            .setTitle("Last Chance")
            .setMessage("To unlock $minutes minutes, type exactly:\n\n\"$challengePhrase\"")
            .setView(container)
            .setCancelable(false)
            .setPositiveButton("CONFIRM") { _, _ ->
                val text = input.text.toString().trim()
                if (text.equals(challengePhrase, ignoreCase = true)) {
                    LimitManager.addExtension(this, packageName, minutes)
                    launchBlockedApp(packageName)
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