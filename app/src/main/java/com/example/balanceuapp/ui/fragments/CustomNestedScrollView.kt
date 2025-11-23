package com.example.balanceuapp.ui.fragments

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView

/**
 * NestedScrollView personalizado que permite que el ViewPager2 maneje
 * los gestos horizontales mientras mantiene el scroll vertical.
 */
class CustomNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    private var startX = 0f
    private var startY = 0f

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
                // Permitir que el padre (ViewPager2) maneje el evento si es necesario
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = Math.abs(ev.x - startX)
                val dy = Math.abs(ev.y - startY)
                
                // Si el movimiento horizontal es mayor que el vertical,
                // permitir que el ViewPager2 maneje el gesto
                if (dx > dy && dx > 10) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return false
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}






