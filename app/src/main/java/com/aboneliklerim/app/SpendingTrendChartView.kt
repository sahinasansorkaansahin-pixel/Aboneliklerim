package com.aboneliklerim.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.roundToInt

class SpendingTrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var values: DoubleArray = doubleArrayOf()
    private var labels: Array<String> = emptyArray()
    private var currencySymbol: String = ""
    private var selectedIndex: Int = -1
    private val pointPositions = mutableListOf<Pair<Float, Float>>()

    private val dp = resources.displayMetrics.density

    // Paddings
    private val padTop    = 52f * dp
    private val padBottom = 34f * dp
    private val padLeft   = 16f * dp
    private val padRight  = 16f * dp

    // Line paint (gradient purple stroke)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9D4EDD")
        strokeWidth = 3f * dp
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Dot — outer white ring
    private val dotWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // Dot — inner purple fill
    private val dotPurplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9D4EDD")
        style = Paint.Style.FILL
    }

    // Dot — selected ring (brighter)
    private val dotSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E040FB")
        style = Paint.Style.FILL
    }

    // Grid dashed horizontal lines
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#229D4EDD")
        strokeWidth = 1f * dp
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(6f * dp, 4f * dp), 0f)
    }

    // X-axis month labels
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888")
        textSize = 10f * dp
        textAlign = Paint.Align.CENTER
    }

    // Y-axis value labels (right side)
    private val yLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA")
        textSize = 9f * dp
        textAlign = Paint.Align.RIGHT
    }

    // Vertical dashed line on selection
    private val vertLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BB9D4EDD")
        strokeWidth = 1.5f * dp
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(5f * dp, 3f * dp), 0f)
    }

    // Tooltip background
    private val tooltipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#730692")
        style = Paint.Style.FILL
    }

    // Tooltip text
    private val tooltipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 11.5f * dp
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    // Tooltip sub text (month label inside tooltip)
    private val tooltipSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DDB3FF")
        textSize = 9.5f * dp
        textAlign = Paint.Align.CENTER
    }

    fun setData(values: DoubleArray, labels: Array<String>, symbol: String, defaultSelected: Int = -1) {
        this.values = values
        this.labels = labels
        this.currencySymbol = symbol
        this.selectedIndex = if (defaultSelected < 0 && values.isNotEmpty()) values.size - 1 else defaultSelected
        requestLayout()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val chartTop = padTop
        val chartBottom = h - padBottom
        val chartLeft = padLeft
        val chartRight = w - padRight
        val chartH = chartBottom - chartTop
        val chartW = chartRight - chartLeft

        val n = values.size
        val maxVal = values.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
        val spacing = if (n > 1) chartW / (n - 1).toFloat() else 0f

        // Build point positions
        pointPositions.clear()
        for (i in 0 until n) {
            val x = if (n > 1) chartLeft + i * spacing else w / 2f
            val normalized = (values[i] / maxVal).toFloat().coerceIn(0f, 1f)
            val y = chartBottom - normalized * chartH
            pointPositions.add(Pair(x, y))
        }

        // ── Grid lines (3 levels) ──────────────────────────────────────────
        val gridLevels = 3
        for (gi in 0..gridLevels) {
            val gy = chartTop + chartH / gridLevels * gi
            canvas.drawLine(chartLeft, gy, chartRight, gy, gridPaint)

            // Y-axis label
            val fraction = 1f - gi.toFloat() / gridLevels
            val labelVal = (maxVal * fraction).roundToInt()
            if (labelVal > 0) {
                val yLabelX = chartLeft - 4f * dp
                canvas.drawText(
                    formatShort(labelVal.toDouble(), currencySymbol),
                    yLabelX + (chartRight - chartLeft),
                    gy - 3f * dp,
                    yLabelPaint
                )
            }
        }

        // ── Build cubic Bezier path ────────────────────────────────────────
        val curvePath = Path()
        if (n == 1) {
            val (x, y) = pointPositions[0]
            curvePath.moveTo(x, y)
            curvePath.lineTo(x + 0.001f, y)
        } else {
            curvePath.moveTo(pointPositions[0].first, pointPositions[0].second)
            for (i in 0 until n - 1) {
                val (x0, y0) = pointPositions[i]
                val (x1, y1) = pointPositions[i + 1]
                val cpX = (x0 + x1) / 2f
                curvePath.cubicTo(cpX, y0, cpX, y1, x1, y1)
            }
        }

        // ── Gradient fill ──────────────────────────────────────────────────
        val fillPath = Path(curvePath)
        fillPath.lineTo(pointPositions.last().first, chartBottom)
        fillPath.lineTo(chartLeft, chartBottom)
        fillPath.close()

        val gradShader = LinearGradient(
            0f, chartTop, 0f, chartBottom,
            intArrayOf(
                Color.parseColor("#CC9D4EDD"),
                Color.parseColor("#559D4EDD"),
                Color.parseColor("#00730692")
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradShader
            style = Paint.Style.FILL
        }
        canvas.drawPath(fillPath, fillPaint)

        // ── Line stroke ───────────────────────────────────────────────────
        // Gradient line shader
        val lineShader = LinearGradient(
            chartLeft, 0f, chartRight, 0f,
            intArrayOf(Color.parseColor("#BB8B5CF6"), Color.parseColor("#FF9D4EDD")),
            null, Shader.TileMode.CLAMP
        )
        linePaint.shader = lineShader
        canvas.drawPath(curvePath, linePaint)
        linePaint.shader = null

        // ── Selection vertical line ────────────────────────────────────────
        if (selectedIndex in pointPositions.indices) {
            val (sx, sy) = pointPositions[selectedIndex]
            canvas.drawLine(sx, chartTop, sx, chartBottom, vertLinePaint)
        }

        // ── Dots ──────────────────────────────────────────────────────────
        for (i in 0 until n) {
            val (x, y) = pointPositions[i]
            val isSelected = i == selectedIndex
            val outerR = if (isSelected) 8f * dp else 4.5f * dp
            val innerR = if (isSelected) 5f * dp else 2.8f * dp

            canvas.drawCircle(x, y, outerR, dotWhitePaint)
            canvas.drawCircle(x, y, innerR, if (isSelected) dotSelectedPaint else dotPurplePaint)

            // Glow ring for selected
            if (isSelected) {
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#44E040FB")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(x, y, 13f * dp, glowPaint)
                canvas.drawCircle(x, y, outerR, dotWhitePaint)
                canvas.drawCircle(x, y, innerR, dotSelectedPaint)
            }
        }

        // ── Tooltip ───────────────────────────────────────────────────────
        if (selectedIndex in pointPositions.indices) {
            val (sx, sy) = pointPositions[selectedIndex]
            val monthLabel = labels.getOrNull(selectedIndex) ?: ""
            val numFmt = java.text.NumberFormat.getInstance(java.util.Locale.getDefault()).apply {
                minimumFractionDigits = 0
                maximumFractionDigits = 0
            }
            val valText = "${numFmt.format(values[selectedIndex])}$currencySymbol"

            // Tooltip dimensions
            val tPad = 10f * dp
            val tH = 42f * dp
            val valW = tooltipTextPaint.measureText(valText)
            val subW = tooltipSubPaint.measureText(monthLabel)
            val tW = maxOf(valW, subW) + tPad * 2.5f

            var tx = sx - tW / 2f
            var ty = sy - tH - 12f * dp

            // Clamp to chart bounds
            if (tx < chartLeft) tx = chartLeft
            if (tx + tW > chartRight) tx = chartRight - tW
            if (ty < 2f * dp) ty = sy + 14f * dp

            val tRect = RectF(tx, ty, tx + tW, ty + tH)
            canvas.drawRoundRect(tRect, 10f * dp, 10f * dp, tooltipBgPaint)

            // Small arrow tip
            if (sy - tH - 12f * dp >= 2f * dp) {
                val arrowPath = Path().apply {
                    moveTo(sx - 6f * dp, ty + tH)
                    lineTo(sx, ty + tH + 7f * dp)
                    lineTo(sx + 6f * dp, ty + tH)
                    close()
                }
                canvas.drawPath(arrowPath, tooltipBgPaint)
            }

            // Tooltip texts
            val centerX = tx + tW / 2f
            canvas.drawText(valText, centerX, ty + tH * 0.58f, tooltipTextPaint)
            canvas.drawText(monthLabel, centerX, ty + tH * 0.88f, tooltipSubPaint)
        }

        // ── X-axis month labels ────────────────────────────────────────────
        val labelStep = when {
            n <= 6  -> 1
            n <= 13 -> 1
            n <= 24 -> 2
            else    -> 3
        }
        for (i in 0 until n step labelStep) {
            val (x, _) = pointPositions[i]
            val lbl = labels.getOrNull(i) ?: continue
            val paint = if (i == selectedIndex) {
                Paint(labelPaint).apply { color = Color.parseColor("#9D4EDD"); isFakeBoldText = true }
            } else labelPaint
            canvas.drawText(lbl, x, h - 6f * dp, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (values.isEmpty()) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val touchX = event.x
                var closest = -1
                var minDist = Float.MAX_VALUE
                for ((i, pos) in pointPositions.withIndex()) {
                    val dist = abs(pos.first - touchX)
                    if (dist < minDist) { minDist = dist; closest = i }
                }
                if (closest != selectedIndex) {
                    selectedIndex = closest
                    invalidate()
                }
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /** Compact number: 1850 → "1.8K₺",  850 → "850₺" */
    private fun formatShort(v: Double, sym: String): String {
        return when {
            v >= 1_000_000 -> "${(v / 1_000_000).toInt()}M$sym"
            v >= 1_000     -> "${"%.1f".format(v / 1_000)}K$sym"
            else           -> "${v.toInt()}$sym"
        }
    }
}
