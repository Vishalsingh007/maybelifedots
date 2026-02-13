package com.example.lifedots

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.lifedots.logic.LimitManager

class BlockedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.overlay_blocked)

        // FIX: The Modern "No Escape" Logic
        // This replaces the old onBackPressed() function
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing. The Back button is disabled.
            }
        })

        val appName = intent.getStringExtra("BLOCKED_APP_NAME") ?: "This App"
        val packageName = intent.getStringExtra("BLOCKED_PACKAGE") ?: ""

        // 1. Dynamic Text: Show exactly what is blocked
        val titleText = findViewById<TextView>(R.id.tvBlockedAppName)
        titleText.text = "$appName LIMIT REACHED!"

        // CLOSE APP BUTTON
        findViewById<Button>(R.id.btnCloseApp).setOnClickListener {
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(homeIntent)
            finish()
        }

        // ADD TIME BUTTON (With Challenge)
        findViewById<Button>(R.id.btnAddTime).setOnClickListener {
            handleExtensionRequest(packageName)
        }
    }

    private fun handleExtensionRequest(packageName: String) {
        val extensionCount = LimitManager.getExtensionCount(this)

        if (extensionCount < 1) {
            // First time is free (Just adds the time)
            LimitManager.addExtension(this, packageName, 5)
            Toast.makeText(this, "5 minutes added. Use them wisely.", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            // CHALLENGE MODE: Force them to type
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
            .setMessage("To unlock 5 more minutes, type this phrase exactly:\n\n\"$challengePhrase\"")
            .setView(container)
            .setCancelable(false) // Cannot click outside to dismiss
            .setPositiveButton("UNLOCK") { _, _ ->
                val text = input.text.toString().trim()
                if (text == challengePhrase) {
                    LimitManager.addExtension(this, packageName, 5)
                    Toast.makeText(this, "Time Added.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "WRONG. Try again.", Toast.LENGTH_SHORT).show()
                    // Don't close the activity, make them stare at the block screen
                }
            }
            .setNegativeButton("GIVE UP") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}