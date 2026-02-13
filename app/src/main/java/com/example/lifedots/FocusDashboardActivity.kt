package com.example.lifedots

import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.lifedots.logic.LimitManager
import com.example.lifedots.logic.UsageStatsHelper

class FocusDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focus_dashboard)
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndStartService()
        loadUsageData()
    }

    private fun checkPermissionsAndStartService() {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        if (mode != AppOpsManager.MODE_ALLOWED) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            startService(Intent(this, AppBlockerService::class.java))
        }
    }

    private fun loadUsageData() {
        val usageList = UsageStatsHelper.getTodayUsage(this)

        // 1. Calculate Total Time
        var totalTimeMillis = 0L
        usageList.forEach { totalTimeMillis += it.timeInForeground }

        val txtTotalTime = findViewById<TextView>(R.id.txtTotalTime)
        txtTotalTime.text = UsageStatsHelper.getTimeString(totalTimeMillis)

        // Red Bar Logic
        val wakingHoursMillis = 16 * 60 * 60 * 1000L
        val percentage = (totalTimeMillis.toFloat() / wakingHoursMillis.toFloat()) * 100
        val viewUsageBar = findViewById<View>(R.id.viewUsageBar)
        val params = viewUsageBar.layoutParams as LinearLayout.LayoutParams
        params.weight = percentage.coerceAtMost(100f)
        viewUsageBar.layoutParams = params

        // 2. Populate Time Thieves
        val container = findViewById<LinearLayout>(R.id.containerAppList)
        container.removeAllViews()

        // FIXED: Removed .take(5) so it shows ALL apps
        val topApps = usageList

        // Use the first app's time as the "100%" benchmark for the progress bars
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

            val limitMinutes = LimitManager.getLimit(this, app.packageName)
            if (limitMinutes > 0) {
                btnLimit.text = "${limitMinutes}m"
                btnLimit.backgroundTintList = ColorStateList.valueOf(Color.RED)

                val usageMinutes = app.timeInForeground / 1000 / 60
                if (usageMinutes >= limitMinutes) {
                    txtName.setTextColor(Color.RED)
                    txtTime.setTextColor(Color.RED)
                }
            } else {
                btnLimit.text = "LIMIT"
                btnLimit.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00F0FF"))
                txtName.setTextColor(Color.WHITE)
                txtTime.setTextColor(Color.parseColor("#00F0FF"))
            }

            // Scale bar relative to the most used app
            val progress = (app.timeInForeground.toFloat() / maxUsage.toFloat()) * 100
            progressBar.progress = progress.toInt()

            btnLimit.setOnClickListener { showDialPicker(app.appName, app.packageName, limitMinutes) }
            container.addView(view)
        }
    }

    private fun showDialPicker(appName: String, packageName: String, currentLimit: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_limit_picker, null)
        val pickerHours = dialogView.findViewById<NumberPicker>(R.id.pickerHours).apply { minValue = 0; maxValue = 12; value = currentLimit / 60 }
        val pickerMinutes = dialogView.findViewById<NumberPicker>(R.id.pickerMinutes).apply { minValue = 0; maxValue = 59; value = currentLimit % 60 }

        val alert = AlertDialog.Builder(this).setView(dialogView).create()
        alert.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<Button>(R.id.btnSaveLimit).setOnClickListener {
            val total = (pickerHours.value * 60) + pickerMinutes.value
            if (total == 0) LimitManager.removeLimit(this, packageName)
            else LimitManager.saveLimit(this, packageName, total)
            loadUsageData()
            alert.dismiss()
        }
        alert.show()
    }
}