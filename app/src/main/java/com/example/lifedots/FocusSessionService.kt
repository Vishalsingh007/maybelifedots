package com.example.lifedots

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.example.lifedots.logic.LimitManager

class FocusSessionService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val CHANNEL_ID = "focus_session_channel"
    private val NOTIF_ID = 9001

    private val tickRunnable = object : Runnable {
        override fun run() {
            val endTime = LimitManager.getFocusModeEndTime(this@FocusSessionService)
            val remaining = endTime - System.currentTimeMillis()

            if (remaining <= 0) {
                LimitManager.stopFocusMode(this@FocusSessionService)
                showFinishedNotification()
                stopSelf()
                return
            }

            val mins = (remaining / 60_000).toInt()
            val secs = ((remaining % 60_000) / 1000).toInt()

            // Update the live notification
            updateNotification("🔒 Deep Work Active — %02d:%02d remaining".format(mins, secs))
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_FOCUS") {
            LimitManager.stopFocusMode(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("🔒 Deep Work starting…"))
        handler.post(tickRunnable)
        return START_STICKY
    }

    private fun buildNotification(text: String): Notification {
        // Notice there is NO "End Early" action. They must face the typing challenge to abort!
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LifeDots Focus Sprint")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun showFinishedNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val done = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sprint Complete!")
            .setContentText("Great focus. Your apps are unblocked.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID + 1, done)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Focus Session", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Active Deep Work countdown"
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}