package de.moekadu.tuner

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class VolumeMeter(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    : View(context, attrs, defStyleAttr){

    var barColor = Color.BLACK
       private set
    var peakMarkerColor = Color.RED
        private set
    var minValue = 0.0f
       private set
    var maxValue = 1.0f
       private set
    var volume = 0.0f
       set(value) {
           field = value
           resetPeakMarker(value)
           invalidate()
       }

    var startDelay = 100L
       set(value) {
           field = startDelay
           peakMarkerAnimator.startDelay = value
       }

    var peakVolume = 0.0f
      private set
    var peakMarkerSize = 1.0f
      private set

    private val paint = Paint()
    private val barRect = RectF()

    private val peakMarkerAnimator = ValueAnimator()

    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, R.attr.volumeMeterStyle)

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(
                attrs,
                R.styleable.VolumeMeter,
                defStyleAttr,
                R.style.VolumeMeterStyle
            );

            barColor = ta.getColor(R.styleable.VolumeMeter_barColor, Color.BLACK)
            peakMarkerColor = ta.getColor(R.styleable.VolumeMeter_peakMarkerColor, Color.RED)
            peakMarkerSize = ta.getDimension(R.styleable.VolumeMeter_peakMarkerSize, 1.0f)
            minValue = ta.getFloat(R.styleable.VolumeMeter_min, 0.0f)
            maxValue = ta.getFloat(R.styleable.VolumeMeter_max, 1.0f)
            ta.recycle()
        }

        paint.color = barColor
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL

        volume = minValue
        peakVolume = volume

        // peakMarkerAnimator.interpolator = LinearInterpolator()
        peakMarkerAnimator.addUpdateListener {
            val value = it.animatedValue as Float
            peakVolume = kotlin.math.max(value, volume)
            invalidate()
        }
        peakMarkerAnimator.startDelay = 100
        peakMarkerAnimator.duration = 1000
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val minPos = (height - paddingBottom).toFloat()
        val maxPos = paddingTop.toFloat() - peakMarkerSize
        val volumePos = (maxPos - minPos) / (maxValue - minValue) * (volume - minValue) + minPos
        val peakVolumePos = (maxPos - minPos) / (maxValue - minValue) * (peakVolume - minValue) + minPos

        barRect.left = paddingLeft.toFloat()
        barRect.right = (width - paddingRight).toFloat()
        barRect.bottom = minPos
        barRect.top = volumePos

        paint.color = barColor
        canvas?.drawRect(barRect, paint)

        barRect.bottom = peakVolumePos
        barRect.top = barRect.bottom - peakMarkerSize

        paint.color = peakMarkerColor
        canvas?.drawRect(barRect, paint)
    }

    private fun resetPeakMarker(currentVolume : Float) {
        if(currentVolume > peakVolume) {
            peakMarkerAnimator.cancel()
            peakMarkerAnimator.setFloatValues(currentVolume, minValue)
            peakVolume = currentVolume
            invalidate()
            peakMarkerAnimator.start()

        }
    }

}