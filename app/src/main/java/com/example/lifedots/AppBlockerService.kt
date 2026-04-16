package com.example.lifedots

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.example.lifedots.logic.LimitManager
import com.example.lifedots.logic.UsageStatsHelper

class AppBlockerService : AccessibilityService() {

    private var lastBlockedPackage = ""
    private var lastBlockTime = 0L

    // --- LIVE MONITOR (Fixes the "only blocks on reopen" bug) ---
    private val handler = Handler(Looper.getMainLooper())
    private var monitoredPackage = ""
    private val CHECK_INTERVAL_MS = 20_000L

    private val usageCheckRunnable = object : Runnable {
        override fun run() {
            val pkg = monitoredPackage
            if (pkg.isEmpty()) return

            if (!Settings.canDrawOverlays(this@AppBlockerService)) return

            // If a whitelist (golden ticket) is still active, keep polling until it expires
            if (LimitManager.isWhitelisted(this@AppBlockerService, pkg)) {
                handler.postDelayed(this, CHECK_INTERVAL_MS)
                return
            }

            val limitMinutes = LimitManager.getLimit(this@AppBlockerService, pkg)
            if (limitMinutes <= 0) {
                stopMonitoring()
                return
            }

            val usageMillis = UsageStatsHelper.getRealTimeUsageMillis(this@AppBlockerService, pkg)
            val limitMillis = limitMinutes * 60 * 1000L

            if (usageMillis >= limitMillis) {
                // Limit crossed mid-session — trigger the block now
                if (lastBlockedPackage != pkg || (System.currentTimeMillis() - lastBlockTime > 1500)) {
                    triggerBlock(pkg)
                }
                // Stop polling — block screen is now showing
                stopMonitoring()
            } else {
                // Still under the limit, keep watching
                handler.postDelayed(this, CHECK_INTERVAL_MS)
            }
        }
    }

    private fun startMonitoring(packageName: String) {
        if (monitoredPackage == packageName) return // Already watching this app
        stopMonitoring()
        monitoredPackage = packageName
        handler.postDelayed(usageCheckRunnable, CHECK_INTERVAL_MS)
    }

    private fun stopMonitoring() {
        handler.removeCallbacks(usageCheckRunnable)
        monitoredPackage = ""
    }

    private fun triggerBlock(currentPackage: String) {
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

    // This fires the exact millisecond an app opens on the screen. No lag. No polling.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        if (!Settings.canDrawOverlays(this)) return

        val currentPackage = event.packageName?.toString() ?: return

        // Ignore LifeDots itself
        if (currentPackage == packageName) return

        // --- THE FIX: IGNORE KEYBOARDS AND SYSTEM MENUS ---
        // If the package cannot be launched from the app drawer, it's a background process or keyboard.
        // We ignore it so we don't accidentally stop monitoring the real app underneath.
        val isRealApp = packageManager.getLaunchIntentForPackage(currentPackage) != null
        if (!isRealApp) return

        // If the user switched to a different real app, stop monitoring the old one
        if (currentPackage != monitoredPackage) {
            stopMonitoring()
        }

        // 1. GOLDEN TICKET CHECK (Whitelist)
        if (LimitManager.isWhitelisted(this, currentPackage)) {
            if (lastBlockedPackage == currentPackage) lastBlockedPackage = ""

            // Start monitoring anyway, so when the ticket expires, the 20s loop catches it
            startMonitoring(currentPackage)
            return
        }

        // 2. LIMIT CHECK
        val limitMinutes = LimitManager.getLimit(this, currentPackage)

        if (limitMinutes > 0) {
            val usageMillis = UsageStatsHelper.getRealTimeUsageMillis(this, currentPackage)
            val limitMillis = limitMinutes * 60 * 1000L

            if (usageMillis >= limitMillis) {
                // Already over limit the moment the app opened — block immediately
                if (lastBlockedPackage != currentPackage || (System.currentTimeMillis() - lastBlockTime > 1500)) {
                    triggerBlock(currentPackage)
                }
            } else {
                // Under limit — start the live monitor to catch the moment it crosses
                if (currentPackage == lastBlockedPackage) lastBlockedPackage = ""
                startMonitoring(currentPackage)
            }
        }
    }

    override fun onInterrupt() {
        stopMonitoring()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    override fun onDestroy() {
        stopMonitoring()
        super.onDestroy()
    }
}