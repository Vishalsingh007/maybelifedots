package com.example.lifedots.graphics

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

class UsageGraphView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val themeColor = Color.parseColor("#BB86FC")
    private val axisColor = Color.parseColor("#66FFFFFF")

    private val linePaint = Paint().apply {
        color = themeColor
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val dotPaint = Paint().apply {
        color = themeColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val dotCenterPaint = Paint().apply {
        color = Color.parseColor("#DD000000")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = axisColor
        textSize = 24f
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#33FFFFFF")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private var dataPoints: List<Float> = listOf(0f, 0f, 0f, 0f, 0f)

    // --- NEW: Animation State ---
    private var animProgress = 1f

    fun setData(data: List<Float>, animate: Boolean = true) {
        this.dataPoints = data
        if (animate) {
            animProgress = 0f
            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.duration = 1300L // 1.5 second reveal animation
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener {
                animProgress = it.animatedValue as Float
                invalidate() // Redraws the frame
            }
            animator.start()
        } else {
            animProgress = 1f
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val paddingLeft = 60f
        val paddingBottom = 40f
        val graphWidth = width - paddingLeft
        val graphHeight = height - paddingBottom

        val maxVal = (dataPoints.maxOrNull() ?: 1f).coerceAtLeast(10f)

        // 1. Draw Axes FIRST (So they are static and don't animate)
        drawYAxis(canvas, maxVal, paddingLeft, graphHeight, graphWidth)
        drawXAxis(canvas, paddingLeft, height, graphWidth)

        // 2. Save Canvas and Clip it for the "Wipe" effect
        canvas.save()
        // This acts like a window blind opening from left to right
        canvas.clipRect(0f, 0f, paddingLeft + (graphWidth * animProgress), height)

        val stepX = graphWidth / (dataPoints.size - 1)
        val path = Path()
        val fillPath = Path()

        fillPath.moveTo(paddingLeft, graphHeight)
        val dotCoords = ArrayList<Pair<Float, Float>>()

        dataPoints.forEachIndexed { index, value ->
            val x = paddingLeft + (index * stepX)
            val h = (value / maxVal) * graphHeight
            val y = graphHeight - h

            dotCoords.add(Pair(x, y))

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.lineTo(x, y)
            } else {
                val prevX = paddingLeft + ((index - 1) * stepX)
                val prevVal = dataPoints[index - 1]
                val prevH = (prevVal / maxVal) * graphHeight
                val prevY = graphHeight - prevH

                val cpX = (prevX + x) / 2
                path.cubicTo(cpX, prevY, cpX, y, x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, graphHeight)
        fillPath.close()

        fillPaint.shader = LinearGradient(0f, 0f, 0f, graphHeight,
            Color.parseColor("#44BB86FC"), Color.TRANSPARENT, Shader.TileMode.CLAMP)

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)

        for (coord in dotCoords) {
            canvas.drawCircle(coord.first, coord.second, 8f, dotPaint)
            canvas.drawCircle(coord.first, coord.second, 4f, dotCenterPaint)
        }

        // Restore canvas so we don't accidentally clip anything else drawn later
        canvas.restore()
    }

    private fun drawYAxis(canvas: Canvas, maxVal: Float, startX: Float, height: Float, width: Float) {
        canvas.drawText("${maxVal.toInt()}m", 0f, 24f, textPaint)
        canvas.drawLine(startX, 0f, startX + width, 0f, gridPaint)
        val midY = height / 2
        canvas.drawText("${(maxVal / 2).toInt()}m", 0f, midY, textPaint)
        canvas.drawLine(startX, midY, startX + width, midY, gridPaint)
        canvas.drawText("0m", 10f, height, textPaint)
        canvas.drawLine(startX, height, startX + width, height, gridPaint)
    }

    private fun drawXAxis(canvas: Canvas, startX: Float, yPos: Float, width: Float) {
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("-12h", startX, yPos, textPaint)
        canvas.drawText("-6h", startX + (width / 2), yPos, textPaint)
        canvas.drawText("Now", startX + width, yPos, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
    }
}