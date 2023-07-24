/*
 * Copyright 2020 Michael Moessner
 *
 * This file is part of Tuner.
 *
 * Tuner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tuner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.moekadu.tuner.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import de.moekadu.tuner.R

class VolumeMeter(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    : View(context, attrs, defStyleAttr){
    enum class Orientation {Vertical, Horizontal}

    private var barColor = Color.BLACK
    private var peakMarkerColor = Color.RED
    private var orientation = Orientation.Horizontal

    var minValue = 0.0f
       private set
    private var maxValue = 1.0f
    var volume = 0.0f
       set(value) {
           field = value
           resetPeakMarker(value)
           invalidate()
       }

    private var startDelay = 100L
       set(value) {
           field = value
           peakMarkerAnimator.startDelay = value
       }

    private var peakVolume = 0.0f
    private var peakMarkerSize = 1.0f

    private val paint = Paint()
    private val barRect = RectF()

    private val peakMarkerAnimator = ValueAnimator()

    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs,
        R.attr.volumeMeterStyle
    )

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(
                attrs,
                R.styleable.VolumeMeter,
                defStyleAttr,
                R.style.VolumeMeterStyle
            )

            barColor = ta.getColor(R.styleable.VolumeMeter_barColor, Color.BLACK)
            peakMarkerColor = ta.getColor(R.styleable.VolumeMeter_peakMarkerColor, Color.RED)
            peakMarkerSize = ta.getDimension(R.styleable.VolumeMeter_peakMarkerSize, 1.0f)
            minValue = ta.getFloat(R.styleable.VolumeMeter_volumeMin, 0.0f)
            maxValue = ta.getFloat(R.styleable.VolumeMeter_volumeMax, 1.0f)
            orientation = if (ta.getInt(R.styleable.VolumeMeter_orientation, 0) == 0)
                Orientation.Horizontal
            else
                Orientation.Vertical
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val minPos = when (orientation) {
            Orientation.Vertical -> (height - paddingBottom).toFloat()
            Orientation.Horizontal -> paddingLeft.toFloat()
        }
        val maxPos = when (orientation) {
            Orientation.Vertical -> paddingTop.toFloat() - peakMarkerSize
            Orientation.Horizontal -> width - paddingRight - peakMarkerSize
        }
        val volumePos = (maxPos - minPos) / (maxValue - minValue) * (volume - minValue) + minPos
        val peakVolumePos =
            (maxPos - minPos) / (maxValue - minValue) * (peakVolume - minValue) + minPos

        when (orientation) {
            Orientation.Vertical -> {
                barRect.left = paddingLeft.toFloat()
                barRect.right = (width - paddingRight).toFloat()
                barRect.bottom = minPos
                barRect.top = volumePos
            }
            Orientation.Horizontal -> {
                barRect.left = minPos
                barRect.right = volumePos
                barRect.bottom = (height - paddingBottom).toFloat()
                barRect.top = paddingTop.toFloat()
            }
        }

        paint.color = barColor
        canvas.drawRect(barRect, paint)

        when (orientation) {
            Orientation.Vertical -> {
                barRect.bottom = peakVolumePos
                barRect.top = barRect.bottom - peakMarkerSize
            }
            Orientation.Horizontal -> {
                barRect.left = peakVolumePos
                barRect.right = barRect.left + peakMarkerSize
            }
        }
        paint.color = peakMarkerColor
        canvas.drawRect(barRect, paint)
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