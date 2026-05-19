package com.aboneliklerim.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ColorPickerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val huePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val svPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.WHITE
    }

    private var hue = 0f
    private var saturation = 1f
    private var value = 1f
    private var currentColor = Color.BLUE

    private var onColorChanged: ((Int) -> Unit)? = null

    fun setOnColorChangedListener(listener: (Int) -> Unit) {
        onColorChanged = listener
    }

    override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        val hueHeight = 40f
        val svHeight = height - hueHeight - 40f

        // Draw Hue Bar
        if (huePaint.shader == null) {
            val colors = intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED)
            huePaint.shader = LinearGradient(0f, 0f, width, 0f, colors, null, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width, hueHeight, huePaint)
        
        // Hue Thumb
        val hueX = (hue / 360f) * width
        canvas.drawCircle(hueX, hueHeight / 2, 20f, thumbPaint)

        // Draw SV Square
        val svRect = RectF(0f, hueHeight + 40f, width, height)
        val shader1 = LinearGradient(svRect.left, svRect.top, svRect.right, svRect.top, Color.WHITE, Color.HSVToColor(floatArrayOf(hue, 1f, 1f)), Shader.TileMode.CLAMP)
        val shader2 = LinearGradient(svRect.left, svRect.top, svRect.left, svRect.bottom, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)
        svPaint.shader = ComposeShader(shader1, shader2, PorterDuff.Mode.SRC_OVER)
        canvas.drawRect(svRect, svPaint)

        // SV Thumb
        val thumbX = saturation * width
        val thumbY = svRect.top + (1f - value) * svHeight
        canvas.drawCircle(thumbX, thumbY, 20f, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x.coerceIn(0f, width.toFloat())
        val y = event.y.coerceIn(0f, height.toFloat())
        val hueHeight = 40f

        if (y <= hueHeight + 20f) {
            hue = (x / width) * 360f
        } else {
            val svHeight = height - hueHeight - 40f
            saturation = x / width
            value = 1f - ((y - (hueHeight + 40f)) / svHeight).coerceIn(0f, 1f)
        }

        currentColor = Color.HSVToColor(floatArrayOf(hue, saturation, value))
        onColorChanged?.invoke(currentColor)
        invalidate()
        return true
    }

    fun getColor() = currentColor
}
