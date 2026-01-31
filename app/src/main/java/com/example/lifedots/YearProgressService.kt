package com.example.lifedots

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.example.lifedots.graphics.GridDrawer
import java.util.Calendar

class YearProgressService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return YearProgressEngine()
    }

    inner class YearProgressEngine : Engine() {

        private lateinit var drawer: GridDrawer
        private val handler = Handler(Looper.getMainLooper())
        private val frameRate = 33L // ~30 FPS (Battery Saver)

        private val drawRunner = object : Runnable {
            override fun run() {
                draw()
                if (drawer.isAnimating()) {
                    handler.postDelayed(this, frameRate)
                }
            }
        }

        private val unlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_PRESENT) handleUnlock()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            drawer = GridDrawer(applicationContext)
            val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
            applicationContext.registerReceiver(unlockReceiver, filter)
        }

        override fun onDestroy() {
            super.onDestroy()
            try { applicationContext.unregisterReceiver(unlockReceiver) } catch (e: Exception) {}
            handler.removeCallbacks(drawRunner)
        }

        override fun onTouchEvent(event: MotionEvent?) {
            val prefs = getSharedPreferences("LifeDotsSettings", Context.MODE_PRIVATE)
            if (prefs.getBoolean("ripple_enabled", true)) {
                if (event?.action == MotionEvent.ACTION_DOWN) {
                    drawer.startRipple(event.x, event.y)
                    handler.removeCallbacks(drawRunner)
                    handler.post(drawRunner)
                }
            }
            super.onTouchEvent(event)
        }

        private fun handleUnlock() {
            drawer.clearRipples()
            val prefs = getSharedPreferences("LifeDotsSettings", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val savedDay = prefs.getInt("last_unlock_day", -1)
            var count = prefs.getInt("daily_unlock_count", 0)

            if (today != savedDay) { count = 1; editor.putInt("last_unlock_day", today) } else { count++ }
            editor.putInt("daily_unlock_count", count)

            val mode = prefs.getString("chosen_mode", "year")
            if (mode == "auto") {
                val cycleCount = prefs.getInt("auto_cycle_count", 0)
                editor.putInt("auto_cycle_count", cycleCount + 1)
            }
            editor.apply()
            draw()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                draw()
            } else {
                handler.removeCallbacks(drawRunner)
                drawer.clearRipples()
            }
        }

        // --- THE CRITICAL FIX IS HERE ---
        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            // PRE-LOAD EVERYTHING NOW so it doesn't lag later
            drawer.onSurfaceChanged(width, height)

            draw()
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas = holder.lockCanvas()
            if (canvas != null) {
                try { drawer.draw(canvas) } finally { holder.unlockCanvasAndPost(canvas) }
            }
        }
    }
}