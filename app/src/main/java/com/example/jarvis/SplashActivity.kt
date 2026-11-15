package com.example.jarvis

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

class SplashActivity : Activity() {
    private var went = false

    private fun go() {
        if (went) return
        went = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        try {
            window.statusBarColor = Color.BLACK
            window.navigationBarColor = Color.BLACK
            val v = ReactorView(this)
            setContentView(v)
            v.start { go() }
        } catch (e: Throwable) {
            go(); return
        }
        // safety net: never get stuck on the splash screen
        Handler(Looper.getMainLooper()).postDelayed({ go() }, 4500)
    }
}

/** Arc-reactor style animation with the title and credit text fading in. */
class ReactorView(ctx: Context) : View(ctx) {
    private val cyan = Color.parseColor("#00E5FF")
    private var t = 0f

    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = cyan; strokeCap = Paint.Cap.ROUND
    }
    private val core = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cyan; textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-light", Typeface.BOLD); letterSpacing = 0.2f
    }
    private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); letterSpacing = 0.25f
    }
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3500
        interpolator = LinearInterpolator()
        addUpdateListener { t = it.animatedValue as Float; invalidate() }
    }

    fun start(done: () -> Unit) {
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(a: Animator) { done() }
        })
        anim.start()
    }

    private fun cl(x: Float) = x.coerceIn(0f, 1f)

    override fun onDraw(c: Canvas) {
        try { drawFrame(c) } catch (e: Throwable) { anim.cancel() }
    }

    private fun drawFrame(c: Canvas) {
        c.drawColor(Color.BLACK)
        val cx = width / 2f
        val cy = height * 0.38f
        val u = width / 360f
        val ease = min(1f, t * 2.2f)
        val scale = 0.6f + 0.4f * ease
        val a = (255 * ease).toInt()
        val pulse = 1f + 0.06f * sin(t * PI * 8).toFloat()

        ring.alpha = a
        ring.strokeWidth = 4 * u
        ring.pathEffect = DashPathEffect(floatArrayOf(40 * u, 18 * u), 0f)
        c.save(); c.rotate(t * 360f, cx, cy)
        c.drawCircle(cx, cy, 110 * u * scale * pulse, ring)
        c.restore()

        ring.strokeWidth = 3 * u
        ring.pathEffect = DashPathEffect(floatArrayOf(14 * u, 10 * u), 0f)
        c.save(); c.rotate(-t * 540f, cx, cy)
        c.drawCircle(cx, cy, 75 * u * scale, ring)
        c.restore()

        ring.pathEffect = null
        for (i in 0 until 6) {
            c.save(); c.rotate(i * 60f + t * 120f, cx, cy)
            c.drawLine(cx + 45 * u * scale, cy, cx + 68 * u * scale, cy, ring)
            c.restore()
        }

        core.color = Color.parseColor("#B2F5FF")
        core.alpha = a
        c.drawCircle(cx, cy, 26 * u * scale * pulse, core)

        title.textSize = 28 * u
        title.alpha = (255 * cl((t - 0.35f) / 0.25f)).toInt()
        c.drawText("J.A.R.V.I.S", cx, cy + 170 * u, title)

        sub.textSize = 14 * u
        sub.alpha = (255 * cl((t - 0.6f) / 0.25f)).toInt()
        c.drawText("MADE BY THAKSHIL", cx, cy + 205 * u, sub)
    }
}
