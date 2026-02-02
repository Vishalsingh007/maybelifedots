package com.example.lifedots

import android.app.usage.UsageStatsManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.lifedots.logic.AppUsageInfo
import com.example.lifedots.logic.UsageStatsHelper

class FocusDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focus_dashboard)

        loadUsageData()
    }

    private fun loadUsageData() {
        val usageList = UsageStatsHelper.getTodayUsage(this)

        // 1. Calculate Total Time (Metric A)
        var totalTimeMillis = 0L
        usageList.forEach { totalTimeMillis += it.timeInForeground }

        val txtTotalTime = findViewById<TextView>(R.id.txtTotalTime)
        txtTotalTime.text = UsageStatsHelper.getTimeString(totalTimeMillis)

        // Update the Red Bar (Assume 16 hours waking time = 100%)
        val wakingHoursMillis = 16 * 60 * 60 * 1000L
        val percentage = (totalTimeMillis.toFloat() / wakingHoursMillis.toFloat()) * 100
        val viewUsageBar = findViewById<View>(R.id.viewUsageBar)

        // Dynamically set weight for the bar graph
        val params = viewUsageBar.layoutParams as LinearLayout.LayoutParams
        params.weight = percentage.coerceAtMost(100f)
        viewUsageBar.layoutParams = params

        // 2. Populate Time Thieves (Metric B)
        val container = findViewById<LinearLayout>(R.id.containerAppList)
        container.removeAllViews()

        // Show top 5 apps
        val topApps = usageList.take(5)
        // Find the max usage to scale progress bars relative to the biggest thief
        val maxUsage = topApps.firstOrNull()?.timeInForeground ?: 1L

        for (app in topApps) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_app_usage, container, false)

            val imgIcon = view.findViewById<ImageView>(R.id.imgAppIcon)
            val txtName = view.findViewById<TextView>(R.id.txtAppName)
            val txtTime = view.findViewById<TextView>(R.id.txtAppTime)
            val progressBar = view.findViewById<ProgressBar>(R.id.progressBarUsage)
            val btnLimit = view.findViewById<Button>(R.id.btnBlockApp)

            imgIcon.setImageDrawable(app.icon)
            txtName.text = app.appName
            txtTime.text = UsageStatsHelper.getTimeString(app.timeInForeground)

            // Scale bar relative to the top app
            val progress = (app.timeInForeground.toFloat() / maxUsage.toFloat()) * 100
            progressBar.progress = progress.toInt()

            btnLimit.setOnClickListener {
                // TODO: Stage 3 - Open Timer Dialog for this app
                android.widget.Toast.makeText(this, "Limit set for ${app.appName}", android.widget.Toast.LENGTH_SHORT).show()
            }

            container.addView(view)
        }
    }
}