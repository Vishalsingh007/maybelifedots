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
import java.util.Locale

class UsageGraphView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val themeColor = Color.parseColor("#BB86FC") // Purple
    private val axisColor = Color.parseColor("#66FFFFFF") // Light Grey for grid

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

    fun setData(data: List<Float>) {
        this.dataPoints = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()

        // Margins for axes text
        val paddingLeft = 60f
        val paddingBottom = 40f

        val graphWidth = width - paddingLeft
        val graphHeight = height - paddingBottom

        // 1. Calculate Y-Axis Scale
        val maxVal = (dataPoints.maxOrNull() ?: 1f).coerceAtLeast(10f) // Min 10 mins

        // 2. Draw Y-Axis Grid & Labels (0, 50%, 100%)
        drawYAxis(canvas, maxVal, paddingLeft, graphHeight, graphWidth)

        // 3. Draw X-Axis Labels (Time)
        drawXAxis(canvas, paddingLeft, height, graphWidth)

        // 4. Draw The Graph Line
        val stepX = graphWidth / (dataPoints.size - 1)
        val path = Path()
        val fillPath = Path()

        fillPath.moveTo(paddingLeft, graphHeight)

        dataPoints.forEachIndexed { index, value ->
            val x = paddingLeft + (index * stepX)
            val h = (value / maxVal) * graphHeight
            val y = graphHeight - h

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.lineTo(x, y)
            } else {
                val prevX = paddingLeft + ((index - 1) * stepX)
                val prevVal = dataPoints[index - 1]
                val prevH = (prevVal / maxVal) * graphHeight
                val prevY = graphHeight - prevH

                // Bezier Curve for smoothness
                val cpX = (prevX + x) / 2
                path.cubicTo(cpX, prevY, cpX, y, x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, graphHeight)
        fillPath.close()

        fillPaint.shader = LinearGradient(0f, 0f, 0f, graphHeight,
            Color.parseColor("#44BB86FC"),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP)

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)
    }

    private fun drawYAxis(canvas: Canvas, maxVal: Float, startX: Float, height: Float, width: Float) {
        // Top Line (Max)
        canvas.drawText("${maxVal.toInt()}m", 0f, 24f, textPaint)
        canvas.drawLine(startX, 0f, startX + width, 0f, gridPaint)

        // Middle Line (Half)
        val midY = height / 2
        canvas.drawText("${(maxVal / 2).toInt()}m", 0f, midY, textPaint)
        canvas.drawLine(startX, midY, startX + width, midY, gridPaint)

        // Bottom Line (0)
        canvas.drawText("0m", 10f, height, textPaint)
        canvas.drawLine(startX, height, startX + width, height, gridPaint)
    }

    private fun drawXAxis(canvas: Canvas, startX: Float, yPos: Float, width: Float) {
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("-12h", startX, yPos, textPaint)
        canvas.drawText("-6h", startX + (width / 2), yPos, textPaint)
        canvas.drawText("Now", startX + width, yPos, textPaint)
        textPaint.textAlign = Paint.Align.LEFT // Reset
    }
}