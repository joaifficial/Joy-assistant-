package com.example.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator

/**
 * RgbBorderOverlay displays a smooth rotating RGB neon border around the entire screen
 * for 10 seconds when Joy is active in the background.
 */
class RgbBorderOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: RgbEdgeView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var removeRunnable: Runnable? = null

    fun showForTenSeconds() {
        handler.post {
            try {
                // Check if can draw overlays
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                    return@post
                }

                // If already visible, remove existing
                remove()

                val view = RgbEdgeView(context)
                overlayView = view

                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    android.graphics.PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                }

                windowManager.addView(view, params)
                view.startAnimation()

                // Auto remove after 10 seconds
                removeRunnable = Runnable {
                    remove()
                }
                handler.postDelayed(removeRunnable!!, 10000L)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun remove() {
        handler.post {
            try {
                removeRunnable?.let { handler.removeCallbacks(it) }
                removeRunnable = null
                overlayView?.let {
                    it.stopAnimation()
                    windowManager.removeView(it)
                }
                overlayView = null
            } catch (e: Exception) {
                // View might not be attached
            }
        }
    }

    private class RgbEdgeView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(5f)
        }

        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(14f)
        }

        private var rotationAngle = 0f
        private var animator: ValueAnimator? = null
        private val rectF = RectF()

        private val rgbColors = intArrayOf(
            Color.parseColor("#FF0055"), // Neon Pink/Red
            Color.parseColor("#FF9900"), // Amber/Orange
            Color.parseColor("#FFFF00"), // Electric Yellow
            Color.parseColor("#00FF66"), // Neon Green
            Color.parseColor("#00E5FF"), // Vivid Cyan
            Color.parseColor("#7C4DFF"), // Neon Purple
            Color.parseColor("#FF0055")  // Seamless loop
        )

        fun startAnimation() {
            animator?.cancel()
            animator = ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 2000L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    rotationAngle = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        fun stopAnimation() {
            animator?.cancel()
            animator = null
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            val inset = glowPaint.strokeWidth / 2f
            rectF.set(inset, inset, w.toFloat() - inset, h.toFloat() - inset)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (width == 0 || height == 0) return

            val cx = width / 2f
            val cy = height / 2f
            val radius = dpToPx(24f)

            canvas.save()
            canvas.rotate(rotationAngle, cx, cy)

            val sweepGradient = SweepGradient(cx, cy, rgbColors, null)
            
            glowPaint.shader = sweepGradient
            glowPaint.alpha = 110 // Soft diffused outer glow

            paint.shader = sweepGradient
            paint.alpha = 255 // Intense crisp center neon beam

            canvas.restore()

            // Draw outer soft glow
            canvas.drawRoundRect(rectF, radius, radius, glowPaint)
            // Draw inner crisp border
            canvas.drawRoundRect(rectF, radius, radius, paint)
        }

        private fun dpToPx(dp: Float): Float {
            return dp * resources.displayMetrics.density
        }
    }
}
