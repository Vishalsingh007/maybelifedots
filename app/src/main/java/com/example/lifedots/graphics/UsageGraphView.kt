package com.example.lifedots.graphics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class UsageGraphView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val linePaint = Paint().apply {
        color = Color.parseColor("#BB86FC") // Purple
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var dataPoints: List<Float> = listOf(0f, 0f, 0f, 0f, 0f)

    fun setData(data: List<Float>) {
        this.dataPoints = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val maxVal = dataPoints.maxOrNull() ?: 1f
        val stepX = width / (dataPoints.size - 1)

        val path = Path()
        val fillPath = Path()

        fillPath.moveTo(0f, height) // Start bottom left

        dataPoints.forEachIndexed { index, value ->
            val x = index * stepX
            // Normalize height (leave 10% padding at top)
            val h = if (maxVal > 0) (value / maxVal) * (height * 0.8f) else 0f
            val y = height - h

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.lineTo(x, y)
            } else {
                // Simple bezier curve for smoothness
                val prevX = (index - 1) * stepX
                val prevVal = dataPoints[index - 1]
                val prevH = if (maxVal > 0) (prevVal / maxVal) * (height * 0.8f) else 0f
                val prevY = height - prevH

                val cpX = (prevX + x) / 2
                path.cubicTo(cpX, prevY, cpX, y, x, y)
                fillPath.cubicTo(cpX, prevY, cpX, y, x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        // Draw Gradient Fill
        fillPaint.shader = LinearGradient(0f, 0f, 0f, height,
            Color.parseColor("#44BB86FC"), // Semi-transparent Purple
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP)
        canvas.drawPath(fillPath, fillPaint)

        // Draw Line
        canvas.drawPath(path, linePaint)
    }
}