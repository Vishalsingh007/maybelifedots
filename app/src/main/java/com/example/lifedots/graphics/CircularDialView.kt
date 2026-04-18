package com.example.lifedots.graphics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class CircularDialView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#18181B"); style = Paint.Style.STROKE; strokeWidth = 50f }
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00F0FF"); style = Paint.Style.STROKE; strokeWidth = 50f; strokeCap = Paint.Cap.ROUND }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }

    var maxMinutes = 120
    var currentMinutes = 25
        set(value) { field = value; invalidate() }

    var onTimeChanged: ((Int) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) / 2f) - 60f

        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawCircle(cx, cy, radius, bgPaint)

        val sweepAngle = (currentMinutes.toFloat() / maxMinutes) * 360f
        canvas.drawArc(rect, -90f, sweepAngle, false, activePaint)

        val thumbAngle = Math.toRadians((-90f + sweepAngle).toDouble())
        val tx = cx + radius * cos(thumbAngle).toFloat()
        val ty = cy + radius * sin(thumbAngle).toFloat()
        canvas.drawCircle(tx, ty, 35f, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cx = width / 2f
        val cy = height / 2f
        val angle = Math.toDegrees(atan2((event.y - cy).toDouble(), (event.x - cx).toDouble())).toFloat()

        var adjustedAngle = angle + 90f
        if (adjustedAngle < 0) adjustedAngle += 360f

        currentMinutes = ((adjustedAngle / 360f) * maxMinutes).toInt().coerceIn(1, maxMinutes)
        onTimeChanged?.invoke(currentMinutes)
        invalidate()
        return true
    }
}