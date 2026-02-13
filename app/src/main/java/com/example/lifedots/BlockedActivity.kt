package com.example.lifedots

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.lifedots.logic.LimitManager

class BlockedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.overlay_blocked) // Ensure this XML exists

        val blockedApp = intent.getStringExtra("BLOCKED_APP_NAME") ?: "App"
        val packageName = intent.getStringExtra("BLOCKED_PACKAGE") ?: ""

        val title = findViewById<TextView>(R.id.tvBlockedAppName)
        if (title != null) title.text = "STOP SCROLLING!"

        // BUTTON 1: CLOSE APP (Go Home)
        findViewById<Button>(R.id.btnCloseApp)?.setOnClickListener {
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(homeIntent)
            finish()
        }

        // BUTTON 2: ADD 5 MINUTES (Shame Extension)
        findViewById<Button>(R.id.btnAddTime)?.setOnClickListener {
            if (packageName.isNotEmpty()) {
                LimitManager.addExtension(this, packageName, 5)
                finish()
            }
        }
    }

    // Disable the back button so you can't escape
    override fun onBackPressed() {
        // Do nothing
    }
}