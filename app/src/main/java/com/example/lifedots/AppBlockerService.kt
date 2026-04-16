package com.example.lifedots

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.example.lifedots.logic.LimitManager
import com.example.lifedots.logic.UsageStatsHelper

class AppBlockerService : AccessibilityService() {

    private var lastBlockedPackage = ""
    private var lastBlockTime = 0L

    // This fires the exact millisecond an app opens on the screen. No lag. No polling.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        if (!Settings.canDrawOverlays(this)) return

        val currentPackage = event.packageName?.toString() ?: return

        // Ignore LifeDots itself
        if (currentPackage == packageName) return

        // 1. GOLDEN TICKET CHECK (Whitelist)
        if (LimitManager.isWhitelisted(this, currentPackage)) {
            if (lastBlockedPackage == currentPackage) lastBlockedPackage = ""
            return
        }

        // 2. LIMIT CHECK
        val limitMinutes = LimitManager.getLimit(this, currentPackage)

        if (limitMinutes > 0) {
            // UsageStatsHelper is still used here to calculate TOTAL TIME spent today.
            val usageMillis = UsageStatsHelper.getRealTimeUsageMillis(this, currentPackage)
            val limitMillis = limitMinutes * 60 * 1000L

            if (usageMillis >= limitMillis) {
                // BLOCK TRIGGER
                if (lastBlockedPackage != currentPackage || (System.currentTimeMillis() - lastBlockTime > 1500)) {

                    val appName = try {
                        val ai = packageManager.getApplicationInfo(currentPackage, 0)
                        packageManager.getApplicationLabel(ai).toString()
                    } catch (e: Exception) { "App" }

                    val blockIntent = Intent(this, BlockedActivity::class.java)
                    blockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    blockIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    blockIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    blockIntent.putExtra("BLOCKED_PACKAGE", currentPackage)
                    blockIntent.putExtra("BLOCKED_APP_NAME", appName)

                    startActivity(blockIntent)

                    lastBlockedPackage = currentPackage
                    lastBlockTime = System.currentTimeMillis()
                }
            } else {
                if (currentPackage == lastBlockedPackage) lastBlockedPackage = ""
            }
        }
    }

    override fun onInterrupt() {
        // Required by AccessibilityService, but we don't need to handle anything here
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Service has been successfully activated in Android Settings
    }
}