package com.example.lifedots

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.example.lifedots.logic.AppCategoryHelper
import com.example.lifedots.logic.LimitManager
import com.example.lifedots.logic.UsageStatsHelper

class AppBlockerService : AccessibilityService() {

    private var lastBlockedPackage = ""
    private var lastBlockTime = 0L

    private val handler = Handler(Looper.getMainLooper())
    private var monitoredPackage = ""
    private val CHECK_INTERVAL_MS = 20_000L

    private val usageCheckRunnable = object : Runnable {
        override fun run() {
            val pkg = monitoredPackage
            if (pkg.isEmpty()) return
            if (!Settings.canDrawOverlays(this@AppBlockerService)) return

            // --- 0. PROACTIVE DEEP WORK SPRINT INTERCEPT ---
            if (LimitManager.isFocusModeActive(this@AppBlockerService)) {
                if (!LimitManager.isEssentialApp(pkg)) {
                    if (lastBlockedPackage != pkg || (System.currentTimeMillis() - lastBlockTime > 1500)) {
                        triggerBlock(pkg, "DEEP WORK ACTIVE") // Special flag title triggers Level 5 challenge
                    }
                    stopMonitoring()
                    return
                }
            }

            // Check whitelist ticket
            if (LimitManager.isWhitelisted(this@AppBlockerService, pkg)) {
                handler.postDelayed(this, CHECK_INTERVAL_MS)
                return
            }

            // --- 1. CHECK CATEGORY LIMIT FIRST ---
            val category = AppCategoryHelper.getCategory(this@AppBlockerService, pkg)
            val catLimitMins = LimitManager.getCategoryLimit(this@AppBlockerService, category)

            if (catLimitMins > 0) {
                val catPackages = AppCategoryHelper.getPackagesInCategory(this@AppBlockerService, category)
                val catUsageMillis = UsageStatsHelper.getRealTimeCategoryUsageMillis(this@AppBlockerService, catPackages)

                if (catUsageMillis >= catLimitMins * 60 * 1000L) {
                    if (lastBlockedPackage != pkg || (System.currentTimeMillis() - lastBlockTime > 1500)) {
                        triggerBlock(pkg, "$category Limit Reached")
                    }
                    stopMonitoring()
                    return
                }
            }

            // --- 2. CHECK INDIVIDUAL APP LIMIT ---
            val appLimitMins = LimitManager.getLimit(this@AppBlockerService, pkg)
            if (appLimitMins > 0) {
                val appUsageMillis = UsageStatsHelper.getRealTimeUsageMillis(this@AppBlockerService, pkg)
                if (appUsageMillis >= appLimitMins * 60 * 1000L) {
                    if (lastBlockedPackage != pkg || (System.currentTimeMillis() - lastBlockTime > 1500)) {
                        val appName = try {
                            val ai = packageManager.getApplicationInfo(pkg, 0)
                            packageManager.getApplicationLabel(ai).toString()
                        } catch (e: Exception) { "App" }
                        triggerBlock(pkg, appName)
                    }
                    stopMonitoring()
                    return
                }
            }

            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    private fun startMonitoring(packageName: String) {
        if (monitoredPackage == packageName) return
        stopMonitoring()
        monitoredPackage = packageName
        handler.postDelayed(usageCheckRunnable, CHECK_INTERVAL_MS)
    }

    private fun stopMonitoring() {
        handler.removeCallbacks(usageCheckRunnable)
        monitoredPackage = ""
    }

    private fun triggerBlock(currentPackage: String, displayTitle: String) {
        val blockIntent = Intent(this, BlockedActivity::class.java)
        blockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        blockIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        blockIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        blockIntent.putExtra("BLOCKED_PACKAGE", currentPackage)
        blockIntent.putExtra("BLOCKED_APP_NAME", displayTitle)
        startActivity(blockIntent)

        lastBlockedPackage = currentPackage
        lastBlockTime = System.currentTimeMillis()
    }

    private fun isLaunchableApp(packageName: String): Boolean {
        return packageManager.getLaunchIntentForPackage(packageName) != null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!Settings.canDrawOverlays(this)) return

        val currentPackage = event.packageName?.toString() ?: return
        if (currentPackage == packageName) return
        if (!isLaunchableApp(currentPackage)) return

        if (currentPackage != monitoredPackage) stopMonitoring()

        // Immediate intercept for Deep Work
        if (LimitManager.isFocusModeActive(this)) {
            if (!LimitManager.isEssentialApp(currentPackage)) {
                if (lastBlockedPackage != currentPackage) lastBlockedPackage = ""
                startMonitoring(currentPackage)
                handler.post(usageCheckRunnable)
                return
            }
        }

        if (LimitManager.isWhitelisted(this, currentPackage)) {
            if (lastBlockedPackage == currentPackage) lastBlockedPackage = ""
            startMonitoring(currentPackage)
            return
        }

        if (lastBlockedPackage != currentPackage) lastBlockedPackage = ""
        startMonitoring(currentPackage)
        handler.post(usageCheckRunnable)
    }

    override fun onInterrupt() { stopMonitoring() }
    override fun onServiceConnected() { super.onServiceConnected() }
    override fun onDestroy() { stopMonitoring(); super.onDestroy() }
}