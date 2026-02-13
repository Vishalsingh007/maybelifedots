package com.example.lifedots

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
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

        // 1. Setup UI
        findViewById<TextView>(R.id.tvBlockedAppName).text = appName

        // Load App Icon
        try {
            val icon = packageManager.getApplicationIcon(packageName)
            findViewById<ImageView>(R.id.imgBlockedIcon).setImageDrawable(icon)
        } catch (e: Exception) {}

        // 2. Load Graph Data
        val hourlyData = UsageStatsHelper.getAppUsageHourly(this, packageName)
        val graphView = findViewById<UsageGraphView>(R.id.usageGraph)
        graphView.setData(hourlyData)

        // 3. Random Quote
        val randomQuote = QuoteManager.getMessage("stoic") // Use Stoic for serious blocks
        findViewById<TextView>(R.id.tvQuote).text = "“$randomQuote”"

        // 4. Buttons
        findViewById<Button>(R.id.btnCloseApp).setOnClickListener { goHome() }

        findViewById<Button>(R.id.btnAddTime).setOnClickListener {
            handleExtensionRequest(packageName)
        }
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
        finish()
    }

    private fun handleExtensionRequest(packageName: String) {
        val extensionCount = LimitManager.getExtensionCount(this)
        if (extensionCount < 1) {
            LimitManager.addExtension(this, packageName, 5)
            Toast.makeText(this, "5 minutes added.", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            showTypingChallenge(packageName)
        }
    }

    private fun showTypingChallenge(packageName: String) {
        val challengePhrase = "I need 5 more minutes"
        val input = EditText(this)
        input.hint = "Type: '$challengePhrase'"
        input.setPadding(50, 50, 50, 50)

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(50, 0, 50, 0)
        container.addView(input)

        AlertDialog.Builder(this)
            .setTitle("Really?")
            .setMessage("To unlock, type:\n\n\"$challengePhrase\"")
            .setView(container)
            .setCancelable(false)
            .setPositiveButton("UNLOCK") { _, _ ->
                val text = input.text.toString().trim()
                if (text == challengePhrase) {
                    LimitManager.addExtension(this, packageName, 5)
                    finish()
                } else {
                    Toast.makeText(this, "WRONG.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("GIVE UP") { dialog, _ ->
                dialog.dismiss()
                goHome() // FIX: Now actually goes home!
            }
            .show()
    }
}