package com.example.lifedots

import android.accessibilityservice.AccessibilityService
import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.lifedots.logic.LimitManager
import com.example.lifedots.logic.UsageStatsHelper
import java.util.Locale

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

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        if (enabledServices != null) {
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals("${context.packageName}/${service.name}", ignoreCase = true) ||
                    componentName.equals("${context.packageName}/${service.canonicalName}", ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    private fun checkPermissionsAndStartService() {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)

        if (mode != AppOpsManager.MODE_ALLOWED) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else if (!isAccessibilityServiceEnabled(this, AppBlockerService::class.java)) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun loadUsageData() {
        val usageList = UsageStatsHelper.getTodayUsage(this)

        var totalTimeMillis = 0L
        usageList.forEach { totalTimeMillis += it.timeInForeground }

        val sessions = UsageStatsHelper.getAppLaunchCount(this)

        findViewById<TextView>(R.id.txtTotalTime).text = UsageStatsHelper.getTimeString(totalTimeMillis)

        val wakingHoursMillis = 16 * 60 * 60 * 1000L
        val percent = ((totalTimeMillis.toFloat() / wakingHoursMillis.toFloat()) * 100).coerceAtMost(100f)
        findViewById<TextView>(R.id.txtLifeDrainDesc).text = String.format(Locale.US, "Used %.0f%% of waking hours", percent)

        val hours = totalTimeMillis / 1000 / 3600
        val (grade, color) = calculateGrade(hours, sessions)

        val txtGrade = findViewById<TextView>(R.id.txtFocusGrade)
        txtGrade.text = grade
        txtGrade.setTextColor(color)

        findViewById<TextView>(R.id.txtSessionCount).text = "$sessions"

        val container = findViewById<LinearLayout>(R.id.containerAppList)
        container.removeAllViews()

        val maxUsage = usageList.firstOrNull()?.timeInForeground ?: 1L

        for (app in usageList) {
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
                btnLimit.text = UsageStatsHelper.getTimeString(limitMinutes * 60 * 1000L)
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

            val progress = (app.timeInForeground.toFloat() / maxUsage.toFloat()) * 100
            progressBar.progress = progress.toInt()

            btnLimit.setOnClickListener { showDialPicker(app.appName, app.packageName, limitMinutes) }
            container.addView(view)
        }
    }

    private fun calculateGrade(hours: Long, sessions: Int): Pair<String, Int> {
        return when {
            hours < 2 -> Pair("A", Color.parseColor("#4ADE80"))
            hours < 4 -> Pair("B", Color.parseColor("#A3E635"))
            hours < 6 -> Pair("C", Color.parseColor("#FACC15"))
            hours < 8 -> Pair("D", Color.parseColor("#FB923C"))
            else -> Pair("F", Color.parseColor("#F87171"))
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