package com.example.lifedots.graphics

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.lifedots.R
import com.example.lifedots.logic.TimeManager
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class GridDrawer(private val context: Context) {

    private data class Ripple(val x: Float, val y: Float, var radius: Float)

    private var activeColorInt = 0
    private var activeFilter: ColorFilter? = null

    private val filledFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.dot_filled), PorterDuff.Mode.SRC_IN)
    private val emptyFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.dot_empty), PorterDuff.Mode.SRC_IN)

    private val bitmapPaint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }
    private val textPaint = Paint().apply { color = ContextCompat.getColor(context, R.color.text_color); textSize = 60f; textAlign = Paint.Align.CENTER; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
    private val backgroundPaint = Paint().apply { color = ContextCompat.getColor(context, R.color.dot_bg); style = Paint.Style.FILL }

    private val dimmerPaint = Paint().apply { color = Color.BLACK; alpha = 100 }

    private var customBgBitmap: Bitmap? = null
    private var useCustomBg = false

    private val reusedDstRect = RectF()
    private val reusedSrcRect = Rect()
    private val activeRipples = ArrayList<Ripple>()
    private val MAX_RIPPLES = 3

    private var cachedBitmap: Bitmap? = null
    private var cachedShapeName: String = ""
    private var cachedRadius: Float = 0f
    private var maxWaveRadius = 0f
    private val waveWidth = 150f

    private var gridSpacing = 0f
    private var gridRadius = 0f
    private var gridStartY = 0f
    private var gridStartX = 0f

    fun onSurfaceChanged(width: Int, height: Int) {
        maxWaveRadius = height * 1.2f
        val prefs = context.getSharedPreferences("LifeDotsSettings", Context.MODE_PRIVATE)

        checkCustomBackground(prefs, width, height)
        val mode = getEffectiveMode(prefs)
        updateGridMetrics(width.toFloat(), height.toFloat(), mode)

        val shapeName = prefs.getString("chosen_shape", "circle") ?: "circle"
        ensureBitmapLoaded(shapeName, gridRadius)
    }

    // --- PREVIEW MODE ---
    fun drawPreview(canvas: Canvas) {
        val prefs = context.getSharedPreferences("LifeDotsSettings", Context.MODE_PRIVATE)
        updateFont(prefs.getString("chosen_font", "system"))

        val unlocks = prefs.getInt("daily_unlock_count", 0)
        val newColor = when { unlocks < 20 -> Color.parseColor("#4CAF50"); unlocks < 50 -> Color.parseColor("#FF9800"); else -> Color.parseColor("#F44336") }
        if (activeColorInt != newColor || activeFilter == null) {
            activeColorInt = newColor
            activeFilter = PorterDuffColorFilter(activeColorInt, PorterDuff.Mode.SRC_IN)
        }

        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        val mode = getEffectiveMode(prefs)
        updateGridMetrics(width, height, mode)

        val shapeName = prefs.getString("chosen_shape", "circle") ?: "circle"
        ensureBitmapLoaded(shapeName, gridRadius)

        when (mode) {
            "goal" -> drawGoalGrid(canvas, prefs, 0f)
            "month" -> drawCenteredGrid(canvas, 7, TimeManager.getTotalDaysInMonth(), TimeManager.getCurrentDayOfMonth(), TimeManager.getMonthProgressString(), 0f)
            "day" -> drawGiantDayShape(canvas)
            else -> drawYearGrid(canvas, 0f)
        }
    }

    private fun checkCustomBackground(prefs: SharedPreferences, reqWidth: Int, reqHeight: Int) {
        useCustomBg = prefs.getBoolean("use_custom_bg", false)
        val dimLevel = prefs.getInt("bg_dim_amount", 100)
        dimmerPaint.alpha = dimLevel

        if (useCustomBg) {
            val file = File(context.filesDir, "custom_bg.png")
            if (file.exists()) {
                if (customBgBitmap == null) {
                    try {
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(file.absolutePath, options)
                        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                        options.inJustDecodeBounds = false
                        customBgBitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                    } catch (e: Exception) { useCustomBg = false }
                }
            } else { useCustomBg = false }
        } else {
            customBgBitmap?.recycle()
            customBgBitmap = null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun draw(canvas: Canvas) {
        val prefs = context.getSharedPreferences("LifeDotsSettings", Context.MODE_PRIVATE)
        updateFont(prefs.getString("chosen_font", "system"))

        val unlocks = prefs.getInt("daily_unlock_count", 0)
        val newColor = when { unlocks < 20 -> Color.parseColor("#4CAF50"); unlocks < 50 -> Color.parseColor("#FF9800"); else -> Color.parseColor("#F44336") }
        if (activeColorInt != newColor || activeFilter == null) {
            activeColorInt = newColor
            activeFilter = PorterDuffColorFilter(activeColorInt, PorterDuff.Mode.SRC_IN)
        }

        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        if (useCustomBg && customBgBitmap != null) {
            val bg = customBgBitmap!!

            bitmapPaint.colorFilter = null // Clean brush before background

            // --- APPLY GESTURE TRANSFORMS ---
            val scale = prefs.getFloat("bg_scale", 1.0f)
            val posX = prefs.getFloat("bg_pos_x", 0f)
            val posY = prefs.getFloat("bg_pos_y", 0f)

            canvas.save()

            canvas.translate(width / 2f + posX, height / 2f + posY)
            canvas.scale(scale, scale)
            canvas.translate(-width / 2f, -height / 2f)

            val baseScale = max(width / bg.width, height / bg.height)
            val scaledWidth = bg.width * baseScale
            val scaledHeight = bg.height * baseScale

            val left = (width - scaledWidth) / 2
            val top = (height - scaledHeight) / 2

            reusedDstRect.set(left, top, left + scaledWidth, top + scaledHeight)
            canvas.drawBitmap(bg, null, reusedDstRect, bitmapPaint)

            canvas.restore()

            canvas.drawRect(0f, 0f, width, height, dimmerPaint)
        } else {
            canvas.drawRect(0f, 0f, width, height, backgroundPaint)
        }

        val mode = getEffectiveMode(prefs)
        updateGridMetrics(width, height, mode)

        var waveSpeed = 45f; var jumpIntensity = 0.8f
        if (mode == "month" || mode == "day") { waveSpeed = 25f; jumpIntensity = 0.3f }

        for (i in activeRipples.size - 1 downTo 0) {
            val r = activeRipples[i]; r.radius += waveSpeed
            if (r.radius > maxWaveRadius) activeRipples.removeAt(i)
        }

        when (mode) {
            "goal" -> drawGoalGrid(canvas, prefs, jumpIntensity)
            "month" -> drawCenteredGrid(canvas, 7, TimeManager.getTotalDaysInMonth(), TimeManager.getCurrentDayOfMonth(), TimeManager.getMonthProgressString(), jumpIntensity)
            "day" -> drawGiantDayShape(canvas)
            else -> drawYearGrid(canvas, jumpIntensity)
        }
    }

    private fun getEffectiveMode(prefs: SharedPreferences): String { var mode = prefs.getString("chosen_mode", "year") ?: "year"; if (mode == "auto") { val count = prefs.getInt("auto_cycle_count", 0); mode = when (count % 3) { 0 -> "day"; 1 -> "month"; else -> "year" } }; return mode }

    private fun updateGridMetrics(width: Float, height: Float, mode: String) {
        val columns = if (mode == "month" || (mode == "goal")) 7 else 14; val rows = when (mode) { "month" -> (TimeManager.getTotalDaysInMonth() / columns) + 1; "year" -> (TimeManager.getTotalDaysInYear() / columns) + 1; "goal" -> 14; else -> 1 }; val availableWidth = width * 0.9f; val spacingByWidth = availableWidth / columns; val topPadding = height * 0.15f; val bottomTextSpace = 350f; val availableHeight = height - topPadding - bottomTextSpace; val spacingByHeight = availableHeight / rows; gridSpacing = min(spacingByWidth, spacingByHeight); gridRadius = gridSpacing * 0.45f; val totalGridWidth = (columns - 1) * gridSpacing; gridStartX = (width - totalGridWidth) / 2; if (mode == "year") { gridStartY = height * 0.15f } else { val totalGridHeight = (rows - 1) * gridSpacing; gridStartY = ((height - totalGridHeight) / 2).coerceAtLeast(height * 0.15f) }
    }

    private fun ensureBitmapLoaded(shapeName: String, radius: Float) {
        if (shapeName == cachedShapeName && abs(radius - cachedRadius) < 1f && cachedBitmap != null) return
        cachedShapeName = shapeName
        cachedRadius = radius

        val resId = when (shapeName) {
            "banana" -> R.drawable.shape_banana
            "diamond" -> R.drawable.shape_diamond
            "gamepad" -> R.drawable.shape_gamepad
            "tank" -> R.drawable.shape_tank
            "tree" -> R.drawable.shape_tree
            else -> 0
        }

        val original = if (resId != 0) {
            try { BitmapFactory.decodeResource(context.resources, resId) } catch (e: Exception) { null }
        } else null

        val targetSize = (radius * 2).toInt().coerceAtLeast(1)

        if (original != null) {
            cachedBitmap = Bitmap.createScaledBitmap(original, targetSize, targetSize, true)
        } else {
            val fallback = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val c = Canvas(fallback)
            val p = Paint().apply { color = Color.WHITE; isAntiAlias = true }
            c.drawCircle(radius, radius, radius * 0.9f, p)
            cachedBitmap = fallback
        }
    }

    private fun drawYearGrid(canvas: Canvas, jump: Float) { val columns = 14; val rows = (TimeManager.getTotalDaysInYear() / columns) + 1; drawDots(canvas, TimeManager.getTotalDaysInYear(), TimeManager.getCurrentDayOfYear(), columns, jump); val gridBottom = gridStartY + (rows * gridSpacing); canvas.drawText(TimeManager.getYearProgressString(), canvas.width / 2f, gridBottom + 150f, textPaint) }
    private fun drawCenteredGrid(canvas: Canvas, columns: Int, totalDots: Int, currentDotIndex: Int, label: String, jump: Float) { val rows = (totalDots / columns) + 1; drawDots(canvas, totalDots, currentDotIndex, columns, jump); val gridHeight = (rows - 1) * gridSpacing; val textYStart = gridStartY + gridHeight + 180f; label.split("\n").forEachIndexed { index, line -> canvas.drawText(line, canvas.width / 2f, textYStart + (index * 80f), textPaint) } }
    private fun drawGoalGrid(canvas: Canvas, prefs: SharedPreferences, jump: Float) { val start = prefs.getLong("goal_start", 0); val end = prefs.getLong("goal_end", 0); val name = prefs.getString("goal_name", "Goal") ?: "Goal"; val (daysPassed, totalDays) = TimeManager.getGoalProgress(start, end); val columns = if (totalDays <= 60) 7 else 14; updateGridMetrics(canvas.width.toFloat(), canvas.height.toFloat(), "goal"); drawCenteredGrid(canvas, columns, totalDays, daysPassed, "$name\n${TimeManager.getDaysLeftString(end)}", jump) }

    // --- THIS IS THE FIXED FUNCTION ---
    private fun drawGiantDayShape(canvas: Canvas) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        val radius = width * 0.40f
        val centerX = width / 2
        val centerY = height * 0.5f

        ensureBitmapLoaded(cachedShapeName, radius)
        drawPartiallyFilledIcon(canvas, centerX, centerY, radius, TimeManager.getDayProgress(), activeFilter!!)

        // FIX: If 200f is too far down (off screen), pull it up to fit!
        // We ensure text is at least 40px from the bottom edge.
        val textY = (centerY + radius + 200f).coerceAtMost(height - 40f)

        canvas.drawText(TimeManager.getDayProgressString(), centerX, textY, textPaint)
    }

    private fun drawDots(canvas: Canvas, totalDots: Int, currentIndex: Int, columns: Int, maxJump: Float) {
        val bmp = cachedBitmap ?: return
        for (i in 1..totalDots) {
            val row = (i - 1) / columns
            val col = (i - 1) % columns
            val cx = gridStartX + (col * gridSpacing)
            val cy = gridStartY + (row * gridSpacing)
            var currentRadius = gridRadius
            if (activeRipples.isNotEmpty()) {
                var maxScale = 1f
                for (j in 0 until activeRipples.size) {
                    val ripple = activeRipples[j]
                    val dx = cx - ripple.x
                    val dy = cy - ripple.y
                    if (abs(dx) > ripple.radius + waveWidth || abs(dy) > ripple.radius + waveWidth) continue
                    val distSq = dx * dx + dy * dy
                    val dist = sqrt(distSq)
                    val distToWave = abs(dist - ripple.radius)
                    if (distToWave < waveWidth) {
                        val decay = (1f - (dist / maxWaveRadius)).coerceAtLeast(0f)
                        val waveIntensity = (1f - (distToWave / waveWidth)) * decay
                        val scale = 1f + (maxJump * waveIntensity)
                        maxScale = max(maxScale, scale)
                    }
                }
                currentRadius *= maxScale
            }
            if (currentRadius != gridRadius) {
                reusedDstRect.set(cx - currentRadius, cy - currentRadius, cx + currentRadius, cy + currentRadius)
                if (i < currentIndex) { bitmapPaint.colorFilter = filledFilter; canvas.drawBitmap(bmp, null, reusedDstRect, bitmapPaint) }
                else if (i > currentIndex) { bitmapPaint.colorFilter = emptyFilter; canvas.drawBitmap(bmp, null, reusedDstRect, bitmapPaint) }
                else { drawPartiallyFilledIcon(canvas, cx, cy, currentRadius, TimeManager.getDayProgress(), activeFilter!!) }
            } else {
                if (i < currentIndex) { bitmapPaint.colorFilter = filledFilter; canvas.drawBitmap(bmp, cx - gridRadius, cy - gridRadius, bitmapPaint) }
                else if (i > currentIndex) { bitmapPaint.colorFilter = emptyFilter; canvas.drawBitmap(bmp, cx - gridRadius, cy - gridRadius, bitmapPaint) }
                else { drawPartiallyFilledIcon(canvas, cx, cy, gridRadius, TimeManager.getDayProgress(), activeFilter!!) }
            }
        }
    }

    private fun drawPartiallyFilledIcon(canvas: Canvas, x: Float, y: Float, radius: Float, progress: Float, activeFilter: ColorFilter) {
        val bmp = cachedBitmap ?: return
        bitmapPaint.colorFilter = emptyFilter
        canvas.drawBitmap(bmp, x - radius, y - radius, bitmapPaint)
        bitmapPaint.colorFilter = activeFilter
        val bmpWidth = bmp.width; val bmpHeight = bmp.height; val fillHeight = (bmpHeight * progress).toInt(); val topOffset = bmpHeight - fillHeight; reusedSrcRect.set(0, topOffset, bmpWidth, bmpHeight); val screenTop = (y - radius) + topOffset; reusedDstRect.set(x - radius, screenTop, x + radius, y + radius); canvas.drawBitmap(bmp, reusedSrcRect, reusedDstRect, bitmapPaint)
    }

    private fun updateFont(fontName: String?) { textPaint.typeface = when (fontName) { "patrick" -> getFontSafe(R.font.patrick); "signature" -> getFontSafe(R.font.signature); "console" -> getFontSafe(R.font.console); "lobster" -> getFontSafe(R.font.lobster); else -> Typeface.DEFAULT_BOLD } }
    private fun getFontSafe(resId: Int): Typeface = try { ResourcesCompat.getFont(context, resId) ?: Typeface.DEFAULT_BOLD } catch (e: Exception) { Typeface.DEFAULT_BOLD }
    fun startRipple(x: Float, y: Float) { if (activeRipples.size >= MAX_RIPPLES) activeRipples.removeAt(0); activeRipples.add(Ripple(x, y, 0f)) }
    fun clearRipples() { activeRipples.clear() }
    fun isAnimating(): Boolean { return activeRipples.isNotEmpty() }
}