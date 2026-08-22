package com.talha.ultron.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.animation.ValueAnimator

enum class OrbState { IDLE, LISTENING, THINKING, SPEAKING }

class VoiceOrbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var currentState = OrbState.IDLE
    private var pulseRadius = 0f
    private var animator: ValueAnimator? = null

    private val colors = mapOf(
        OrbState.IDLE to Color.parseColor("#3F51B5"),
        OrbState.LISTENING to Color.parseColor("#4CAF50"),
        OrbState.THINKING to Color.parseColor("#FF9800"),
        OrbState.SPEAKING to Color.parseColor("#2196F3")
    )

    init {
        startPulseAnimation()
    }

    fun setState(state: OrbState) {
        currentState = state
        invalidate()
    }

    private fun startPulseAnimation() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                pulseRadius = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = (width.coerceAtMost(height) / 2f) * 0.7f
        val color = colors[currentState] ?: Color.GRAY

        // Pulse glow
        val glowRadius = baseRadius + (pulseRadius * 20f)
        paint.shader = RadialGradient(cx, cy, glowRadius, color, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        paint.alpha = (50 + pulseRadius * 50).toInt()
        canvas.drawCircle(cx, cy, glowRadius, paint)

        // Core orb
        paint.shader = null
        paint.color = color
        paint.alpha = 255
        canvas.drawCircle(cx, cy, baseRadius, paint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
