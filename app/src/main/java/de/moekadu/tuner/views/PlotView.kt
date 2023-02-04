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

import android.animation.FloatArrayEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.view.ViewCompat
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatValueHolder
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalNotePrintOptions
import de.moekadu.tuner.temperaments.NoteNamePrinter
import de.moekadu.tuner.temperaments.NoteNameScale
import kotlinx.parcelize.Parcelize
import kotlin.math.*

private fun RectF.contentEquals(other: RectF): Boolean {
    return this.left == other.left && this.top == other.top && this.right == other.right && this.bottom == other.bottom
}

/** Interface for an array of floats internally used by the PlotView. */
private interface PlotViewArray {
    /** Get value at specific index. */
    operator fun get(index: Int): Float
    /** Check if size is zero. */
    fun isEmpty(): Boolean
    /** Return last element or throw. */
    fun last(): Float
    /** Size of array. */
    val size: Int

    /** Binary search within the array
     * @param element Element to search
     * @param fromIndex Start search at this index
     * @param toIndex Stop search at this index (toIndex is excluded)
     * @return Index of element or inverted insertion point if not in array (-insertion_point-1)
     */
    fun binarySearch(element: Float, fromIndex: Int = 0, toIndex: Int = size) : Int
}

/** PlotViewArray containing a FloatArray. */
private class FloatArrayPlotViewArray(private val a: FloatArray) : PlotViewArray {
    override fun get(index: Int) = a[index]
    override fun isEmpty() = a.isEmpty()
    override fun last() = a.last()
    override val size: Int = a.size
    override fun binarySearch(element: Float, fromIndex: Int, toIndex: Int) = a.binarySearch(element, fromIndex, toIndex)
}

/** PlotViewArray containing a ArrayList of floats. */
private class ArrayListPlotViewArray(private val a: ArrayList<Float>) : PlotViewArray {
    override fun get(index: Int) = a[index]
    override fun isEmpty() = a.isEmpty()
    override fun last() = a.last()
    override val size: Int = a.size
    override fun binarySearch(element: Float, fromIndex: Int, toIndex: Int) = a.binarySearch(element, fromIndex, toIndex)
}

/** Convert FloatArray to PlotViewArray. */
private fun FloatArray.asPlotViewArray(): PlotViewArray = FloatArrayPlotViewArray(this)
/** Convert ArrayList of floats to PlotViewArray. */
private fun ArrayList<Float>.asPlotViewArray(): PlotViewArray = ArrayListPlotViewArray(this)


class PlotRange(private val allowTouchControl: Boolean = true)  {

    enum class AnimationStrategy {Direct, ExtendShrink}

    /** Range which was set manually using setRange.
     * If this is not AUTO  and if no touch-based range is on, this is the range, to be used.
     */
    private val fixedRange = FloatArray(2) { AUTO }

    /** Range which was set by touch input.
     * If this range is not OFF, this is what we will use
     */
    private val touchBasedRange = FloatArray(2) { OFF }
    /**he range limits which should be not exceeded, when in touch mode. */
    val touchBasedRangeLimits = floatArrayOf (Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)

    /** Current range to be displayed.
     * In contrary to targetRange, the current range is what is actually be displayed. E.g. at
     * animation, the target will define the animation target, but the current range is the
     * current state of the animation.
     */
    private val currentRange = floatArrayOf(0f, 1f)

    /** Tell if the range is controlled by touch input. */
    val isTouchControlled
        get() = touchBasedRange[0] != OFF && touchBasedRange[1] != OFF

    @Parcelize
    class SavedState(
        val fixedRange: FloatArray,
        val touchBasedRange: FloatArray,
        val dataRange: FloatArray,
        val ticksRange: FloatArray) : Parcelable

    fun interface RangeChangedListener {
        fun onRangeChanged(range: PlotRange, minValue: Float, maxValue: Float)
    }

    /** Temporary memory for resolveAuto return value (avoid memory allocation if this function is called from animation). */
    private val targetRange = floatArrayOf(BOUND_UNDEFINED, BOUND_UNDEFINED)
    private val targetRangeOld = FloatArray(2)

    private val dataRange = FloatArray(2) { BOUND_UNDEFINED }
    private val ticksRange = FloatArray(2) { BOUND_UNDEFINED }

    /** Callback when the range to be displayed has changed. */
    var rangeChangedListener: RangeChangedListener? = null

    val rangeMin get() = currentRange[0]
    val rangeMax get() = currentRange[1]

    /** Evaluator for range needed for animation. */
    private val rangeEvaluator = FloatArrayEvaluator(floatArrayOf(0f, 0f))
    /** Animator for animating between different ranges. */
    private val rangeAnimator = ValueAnimator.ofObject(rangeEvaluator, floatArrayOf(0f, 100f), floatArrayOf(100f, 200f)).apply {
        addUpdateListener {
            val range = it.animatedValue as FloatArray
            range.copyInto(currentRange)
            rangeChangedListener?.onRangeChanged(this@PlotRange, range[0], range[1])
        }
    }

    fun setDataRange(minValue: Float = BOUND_UNDEFINED, maxValue: Float = BOUND_UNDEFINED) {
//        Log.v("Tuner", "PlotRange.setDataRange: minValue=$minValue, maxValue=$maxValue")
        if (dataRange[0] == minValue && dataRange[1] == maxValue)
            return
        dataRange[0] = minValue
        dataRange[1] = maxValue

        targetRange.copyInto(targetRangeOld)
        determineTargetRange()
        if (targetRange.contentEquals(targetRangeOld))
            return
        targetRange.copyInto(currentRange)
        if (rangeAnimator.isStarted)
            rangeAnimator.end()
        rangeChangedListener?.onRangeChanged(this, currentRange[0], currentRange[1])
    }

    fun setTicksRange(minValue: Float = BOUND_UNDEFINED, maxValue: Float = BOUND_UNDEFINED) {
//        Log.v("Tuner", "PlotView.PlotRange.setTicksRange: minValue=$minValue, maxValue=$maxValue")
        if (ticksRange[0] == minValue && ticksRange[1] == maxValue)
            return
        ticksRange[0] = minValue
        ticksRange[1] = maxValue

        targetRange.copyInto(targetRangeOld)
        determineTargetRange()
        if (targetRange.contentEquals(targetRangeOld))
            return
        targetRange.copyInto(currentRange)
        if (rangeAnimator.isStarted)
            rangeAnimator.end()
        rangeChangedListener?.onRangeChanged(this, currentRange[0], currentRange[1])
    }

    fun setRange(minValue: Float, maxValue: Float, animationStrategy: AnimationStrategy, animationDuration: Long) {
//        Log.v("Tuner", "PlotView.PlotRange.setRange: minValue=$minValue, maxValue=$maxValue")
        if (minValue == fixedRange[0] && maxValue == fixedRange[1])
            return
//        Log.v("Tuner", "PlotView.PlotRange.setRange: targetRange=${targetRange[0]} -- ${targetRange[1]}")
        fixedRange[0] = minValue
        fixedRange[1] = maxValue

        targetRange.copyInto(targetRangeOld)
        determineTargetRange()
        if (targetRange.contentEquals(targetRangeOld))
            return
//        Log.v("Tuner", "PlotView.PlotRange.setRange: targetRange=${targetRange[0]} -- ${targetRange[1]}")
        if (animationDuration == 0L) {
            if (rangeAnimator.isStarted)
                rangeAnimator.end()
            targetRange.copyInto(currentRange)
            rangeChangedListener?.onRangeChanged(this, currentRange[0], currentRange[1])
        } else if (animationDuration > 0L) {
            animateTo(targetRange, animationStrategy, animationDuration)
        }
        //Log.v("Tuner", "PlotView.PlotRange.setRange: currentRange = ${currentRange[0]}, ${currentRange[1]}")
    }

    fun setTouchLimits(minValue: Float, maxValue: Float, animationStrategy: AnimationStrategy, animationDuration: Long) {
//        Log.v("Tuner", "PlotRanges.setTouchLimits: $minValue -- $maxValue")
        touchBasedRangeLimits[0] = minValue
        touchBasedRangeLimits[1] = maxValue
        // reset the touch based range to make sure its within the limits
        setTouchRange(touchBasedRange[0], touchBasedRange[1], animationStrategy, animationDuration)
    }

    fun setTouchRange(minValue: Float, maxValue:Float, animationStrategy: AnimationStrategy, animationDuration: Long) {
        if (!allowTouchControl)
            return
        val minBackup = touchBasedRange[0]
        val maxBackup = touchBasedRange[1]
//        Log.v("Tuner", "PlotView.PlotRange.setTouchRange, minValue=$minValue, maxValue=$maxValue")
        touchBasedRange[0] = if (minValue == OFF) OFF else max(minValue, touchBasedRangeLimits[0])
        touchBasedRange[1] = if (maxValue == OFF) OFF else min(maxValue, touchBasedRangeLimits[1])

        if (minBackup == touchBasedRange[0] && maxBackup == touchBasedRange[1]) {
            return
        }

        determineTargetRange()

        if (isTouchControlled || animationDuration == 0L) {
            if (rangeAnimator.isStarted)
                rangeAnimator.end()
            targetRange.copyInto(currentRange)
            rangeChangedListener?.onRangeChanged(this, currentRange[0], currentRange[1])
        } else {
            animateTo(targetRange, animationStrategy, animationDuration)
        }
    }

    fun getSavedState(): SavedState {
        return SavedState(fixedRange = fixedRange, touchBasedRange = touchBasedRange,
            dataRange = dataRange, ticksRange = ticksRange)
    }

    fun restore(state: SavedState) {
        setTicksRange(state.ticksRange[0], state.ticksRange[1])
        setDataRange(state.dataRange[0], state.dataRange[1])
        setRange(state.fixedRange[0], state.fixedRange[1], AnimationStrategy.Direct, 0L)
        state.touchBasedRange.copyInto(touchBasedRange)
        determineTargetRange()
        targetRange.copyInto(currentRange)
    }

    /** Determine the target range, which should be shown or serves as a target, when we animate.
     * This function resolves correct value based on the different options we have
     * (touch base range, fixed range, range defined through limits of input data, ...)
     */
    private fun determineTargetRange() {
        if (touchBasedRange[0] == OFF || touchBasedRange[1] == OFF) {
            for (i in 0..1) {
                targetRange[i] = when {
                    fixedRange[i] != AUTO -> fixedRange[i]
                    dataRange[i] != BOUND_UNDEFINED -> dataRange[i]
                    ticksRange[i] != BOUND_UNDEFINED -> ticksRange[i]
                    else -> BOUND_UNDEFINED
                }
            }

            if (targetRange[0] == BOUND_UNDEFINED && targetRange[1] == BOUND_UNDEFINED) {
                targetRange[0] = 0f
                targetRange[1] = 1f
            } else if (targetRange[0] == BOUND_UNDEFINED) {
                targetRange[1] =
                    if (targetRange[0] == 0f) 1f else targetRange[0] + targetRange[0].absoluteValue
            } else if (targetRange[1] == BOUND_UNDEFINED) {
                targetRange[0] =
                    if (targetRange[1] == 0f) -1f else targetRange[1] - targetRange[1].absoluteValue
            } else if (targetRange[0] == targetRange[1]) {
                val extraSpacing = if (targetRange[0] == 0f) 1f else 0.1f * targetRange[0]
                targetRange[0] -= extraSpacing
                targetRange[1] += extraSpacing
            }
        } else {
            targetRange[0] = touchBasedRange[0]
            targetRange[1] = touchBasedRange[1]
        }
//        Log.v("Tuner", "PlotRange.determineTargetRange: ${targetRange[0]} ${targetRange[1]}")
    }

    private fun animateTo(targetRange: FloatArray, strategy: AnimationStrategy, animationDuration: Long) {
        if (rangeAnimator.isStarted)
            rangeAnimator.end()
        when (strategy) {
            AnimationStrategy.Direct -> rangeAnimator.setObjectValues(
                floatArrayOf(currentRange[0], currentRange[1]),
                floatArrayOf(targetRange[0], targetRange[1])
            )
            AnimationStrategy.ExtendShrink -> rangeAnimator.setObjectValues(
                floatArrayOf(currentRange[0], currentRange[1]),
                floatArrayOf(min(currentRange[0], targetRange[0]), max(currentRange[1], targetRange[1])),
                floatArrayOf(min(currentRange[0], targetRange[0]), max(currentRange[1], targetRange[1])),
                floatArrayOf(targetRange[0], targetRange[1])
            )
        }
        rangeAnimator.duration = animationDuration
        rangeAnimator.start()
    }

    companion object {
        const val AUTO = Float.MAX_VALUE
        const val OFF = Float.MAX_VALUE
        const val BOUND_UNDEFINED = Float.MAX_VALUE
    }
}

private class PlotTransformation {
    fun interface TransformationChangedListener {
        fun transformationChanged(transform: PlotTransformation)
    }

    private val transformationChangedListeners = ArrayList<TransformationChangedListener>()

    // same as transformationChangedListeners, but called after all others have been called
    var transformationFinishedListener: TransformationChangedListener? = null

    /** Transformation matrix for transforming from raw coordinates to canvas coordinates. */
    private val matrixRawToView = Matrix()
    /** Transformation matrix for transforming from canvas coordinates to raw coordinates. */
    private val matrixViewToRaw = Matrix()
    /** Plot bounds for coordinates in original space. */
    val rawPlotBounds = RectF(0f, 0f, 1f, 1f)

    /** Plot bounds in canvas coordinates. */
    val viewPlotBounds = RectF()

    fun setRawDataBounds(xMin: Float = KEEP_VALUE, xMax: Float = KEEP_VALUE, yMin: Float = KEEP_VALUE, yMax: Float = KEEP_VALUE) {

//        Log.v("Tuner", "PlotView.PlotTransformation.setRawBounds: xMin = $xMin, xMax = $xMax, yMin = $yMin, yMax = $yMax, rawPlotBonds=$rawPlotBounds")
        var changed = false
        if (xMin != KEEP_VALUE && xMin != rawPlotBounds.left) {
            rawPlotBounds.left = xMin
            changed = true
        }
        if (xMax != KEEP_VALUE && xMax != rawPlotBounds.right) {
            rawPlotBounds.right = xMax
            changed = true
        }
        if (yMin != KEEP_VALUE && yMin != rawPlotBounds.top) {
            rawPlotBounds.top = yMin
            changed = true
        }
        if (yMax != KEEP_VALUE && yMax != rawPlotBounds.bottom) {
            rawPlotBounds.bottom = yMax
            changed = true
        }
        // Log.v("Tuner", "PlotView.PlotTransformation.setRawBounds: ${rawPlotBounds}, $xMin, $xMax")
        if (changed)
            updateTransformationMatrices() // this also calls the listener
    }

    fun setViewBounds(left: Float = KEEP_VALUE, top: Float = KEEP_VALUE, right: Float = KEEP_VALUE, bottom: Float = KEEP_VALUE) {
        var changed = false
        if (left != KEEP_VALUE && left != viewPlotBounds.left) {
            viewPlotBounds.left = left
            changed = true
        }
        if (right != KEEP_VALUE && right != viewPlotBounds.right) {
            viewPlotBounds.right = right
            changed = true
        }
        if (top != KEEP_VALUE && top != viewPlotBounds.top) {
            viewPlotBounds.top = top
            changed = true
        }
        if (bottom != KEEP_VALUE && bottom != viewPlotBounds.bottom) {
            viewPlotBounds.bottom = bottom
            changed = true
        }
        if (changed)
            updateTransformationMatrices() // this also calls the listener
    }

    fun transformRawToView(raw: Path, view: Path) {
        raw.transform(matrixRawToView, view)
    }

    fun transformViewToRaw(view: Path, raw: Path) {
        view.transform(matrixViewToRaw, raw)
    }

    fun transformRawToView(raw: FloatArray, view: FloatArray, numValues: Int = raw.size / 2) {
        matrixRawToView.mapPoints(view, 0, raw, 0, numValues)
    }

    fun transformRawToView(inOut: FloatArray) {
        matrixRawToView.mapPoints(inOut)
    }

    fun transformViewToRaw(view: FloatArray, raw: FloatArray, numValues: Int = view.size / 2) {
        matrixViewToRaw.mapPoints(raw, 0, view, 0, numValues)
    }

    fun transformViewToRaw(view: RectF, raw: RectF) {
        matrixViewToRaw.mapRect(raw, view)
    }

    fun transformVectorViewTorRaw(inOut: FloatArray) {
        matrixViewToRaw.mapVectors(inOut)
    }

    fun registerTransformationChangedListener(transformationChangedListener: TransformationChangedListener) {
        if (!transformationChangedListeners.contains(transformationChangedListener))
            transformationChangedListeners.add(transformationChangedListener)
    }

    fun unregisterTransformationChangedListener(transformationChangedListener: TransformationChangedListener) {
        transformationChangedListeners.remove(transformationChangedListener)
    }

    fun rawPlotBoundsIntersect(rect: RectF) = (RectF.intersects(rawPlotBounds, rect))
    fun viewBoundsContain(x: Float, y: Float) = viewPlotBounds.contains(x, y)

    private fun updateTransformationMatrices() {
        matrixRawToView.setRectToRect(rawPlotBounds, viewPlotBounds, Matrix.ScaleToFit.FILL)
        matrixRawToView.postScale(1f, -1f, 0f, viewPlotBounds.centerY())

        matrixRawToView.invert(matrixViewToRaw)

        //matrixViewToRaw.preScale(1f, -1f, 0f, viewPlotBounds.centerY())
        //matrixViewToRaw.setRectToRect(viewPlotBounds, rawPlotBounds, Matrix.ScaleToFit.FILL)

        for (transformationChangedListener in transformationChangedListeners)
            transformationChangedListener.transformationChanged(this)
        transformationFinishedListener?.transformationChanged(this)
    }

    companion object {
        const val KEEP_VALUE = Float.MAX_VALUE
    }
}

private abstract class PlotTransformable(transformation: PlotTransformation) {
    private val transformationChangedListener =
        PlotTransformation.TransformationChangedListener { t, ->
            transform(t)
        }

    protected var transformation: PlotTransformation? = transformation

    init {
        registerToPlotTransformation(transformation)
    }

    private fun registerToPlotTransformation(transform: PlotTransformation) {
        this.transformation = transform
        transform.registerTransformationChangedListener(transformationChangedListener)
    }

    fun unregisterFromPlotTransformation() {
        transformation?.unregisterTransformationChangedListener(transformationChangedListener)
        transformation = null
    }

    protected abstract fun transform(transform: PlotTransformation?)
}

private class PlotLine(transformation: PlotTransformation, val colors: IntArray, val widths: FloatArray)
    : PlotTransformable(transformation) {
    fun interface PlotLineChangedListener {
        fun onPlotLineChanged(plotLine: PlotLine, hasNewBoundingBox: Boolean)
    }
    var plotLineChangedListener: PlotLineChangedListener? = null

    private var styleIndex = 0

    /** Paint used for plotting line and title. */
    private val paint = Paint().apply {
        strokeWidth = widths[styleIndex]
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    var emptyLine = true
        private set
    /** Path with plot line of coordinates as given during plot. */
    private val rawPlotLine = Path()
    /** Plot line after transforming it to the canvas. */
    private val transformedPlotLine = Path()

    private val oldBoundingBox = RectF(0f, 0f, 0f, 0f)
    val boundingBox = RectF(0f, 0f, 0f, 0f)

    fun setStyleIndex(index: Int) {
        if (index == styleIndex)
            return
        styleIndex = index
        plotLineChangedListener?.onPlotLineChanged(this, false)
    }

    fun setLine(yValues: PlotViewArray, indexBegin: Int, indexEnd: Int) {
//        Log.v("Tuner", "PlotLine.setLine: yValues.size=${yValues.size}")
        val indexBeginResolved = max(0, indexBegin)
        val indexEndResolved = min(yValues.size, indexEnd)
        val numValues = indexEndResolved - indexBeginResolved

        rawPlotLine.rewind()
        oldBoundingBox.set(boundingBox)

        if (numValues == 0) {
            boundingBox.set(0f, 0f, 0f, 0f)
        } else {
            rawPlotLine.moveTo(0f, yValues[indexBeginResolved])
            boundingBox.set(0f, yValues[indexBeginResolved], (indexEndResolved - 1).toFloat(), yValues[indexBeginResolved])

            for (i in indexBeginResolved + 1 until indexEndResolved) {
                rawPlotLine.lineTo(i.toFloat(), yValues[i])
                boundingBox.top = min(boundingBox.top, yValues[i])
                boundingBox.bottom = max(boundingBox.bottom, yValues[i])
            }
//            Log.v("Tuner", "PlotLine.setLine: boundingBox=$boundingBox")
        }

        val emptyLineBackup = emptyLine
        emptyLine = (numValues <= 1) // one value doesn't give a line

        if (!emptyLine || emptyLine != emptyLineBackup) {
            transformAndCallListener(transformation, !boundingBox.contentEquals(oldBoundingBox))
        }
    }

    fun setLine(
        xValues: PlotViewArray, yValues: PlotViewArray, indexBegin: Int, indexEnd: Int
    ) {
//        Log.v("Tuner", "PlotLine.setLine: xValues.size=${xValues.size}, yValues.size=${yValues.size}")
        val indexBeginResolved = max(0, indexBegin)
        val indexEndResolved = min(xValues.size, min(yValues.size, indexEnd))
        val numValues = indexEndResolved - indexBeginResolved

        rawPlotLine.rewind()
        oldBoundingBox.set(boundingBox)
        if (yValues.size == 0) {
            boundingBox.set(0f, 0f, 0f, 0f)
        } else {
            rawPlotLine.moveTo(xValues[indexBeginResolved], yValues[indexBeginResolved])
            boundingBox.set(
                xValues[indexBeginResolved], yValues[indexBeginResolved],
                xValues[indexBeginResolved], yValues[indexBeginResolved]
            )

            for (i in indexBeginResolved + 1 until indexEndResolved) {
                rawPlotLine.lineTo(xValues[i], yValues[i])
                boundingBox.top = min(boundingBox.top, yValues[i])
                boundingBox.bottom = max(boundingBox.bottom, yValues[i])
                boundingBox.left = min(boundingBox.left, xValues[i])
                boundingBox.right = max(boundingBox.right, xValues[i])
            }
        }

        val emptyLineBackup = emptyLine
        emptyLine = (numValues <= 1) // one value doesn't give a line

        if (!emptyLine || emptyLine != emptyLineBackup) {
            transformAndCallListener(transformation, !boundingBox.contentEquals(oldBoundingBox))
        }
    }

    fun drawToCanvas(canvas: Canvas?) {
        if (transformation != null && !emptyLine
            && transformation?.rawPlotBoundsIntersect(boundingBox) == true) {
//            Log.v("Tuner", "PlotLine.drawToCanvas")
            paint.color = colors[styleIndex]
            paint.strokeWidth = widths[styleIndex]
            canvas?.drawPath(transformedPlotLine, paint)
        }
    }

    fun transformAndCallListener(transform: PlotTransformation?, hasNewBoundingBox: Boolean) {
        transform?.transformRawToView(rawPlotLine, transformedPlotLine)
        plotLineChangedListener?.onPlotLineChanged(this, hasNewBoundingBox)
    }

    override fun transform(transform: PlotTransformation?) {
        transformAndCallListener(transform, false)
    }
}

enum class PointShapes {Circle, TriangleUp, TriangleDown}

private class PlotPoints(transformation: PlotTransformation, val colors: IntArray, val sizes: FloatArray, val shapes: Array<PointShapes>)
    : PlotTransformable(transformation) {
    fun interface PlotPointsChangedListener {
        fun onPlotPointsChanged(plotPoints: PlotPoints, hasNewBoundingBox: Boolean)
    }
    var plotPointsChangedListener: PlotPointsChangedListener? = null

    /** Coordinates for points to be plotted.
    (as filled circles, even indices are x-coordinates, odd indices are y-coordinates) */
    private var rawPoints: FloatArray? = null
    private var transformedPoints = FloatArray(0)

    /** Only the first numPoints in the points-array are plotted. */
    var numPoints = 0
        private set

    private var styleIndex = 0

    private var offsetX = 0f
    private var offsetY = 0f

    /** Paint used for drawing points. */
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val path = Path()
    private val pathShifted = Path()

    private val oldBoundingBox = RectF(0f, 0f, 0f, 0f)
    val boundingBox = RectF(0f, 0f, 0f, 0f)

    val pointSize
        get() = sizes[styleIndex]

    private var isVisible = true

    fun setStyleIndex(index: Int) {
        if (index == styleIndex)
            return

        styleIndex = index

        plotPointsChangedListener?.onPlotPointsChanged(this, hasNewBoundingBox = false)
    }

    fun setOffset(offsetX: Float, offsetY: Float) {
        if (offsetX == this.offsetX && offsetY == this.offsetY)
            return
        this.offsetX = offsetX
        this.offsetY = offsetY
        plotPointsChangedListener?.onPlotPointsChanged(this, hasNewBoundingBox = false)
    }

    fun setPoints(points: FloatArray?) {
//        Log.v("Tuner", "PlotPoints.setPoints, points.size = ${points?.size}")
        val numPointsBackup = numPoints
        var boundingBoxChanged = false

        if(points == null || points.size < 2) {
            numPoints = 0
            boundingBox.set(0f, 0f, 0f, 0f)
        }
        else {
            numPoints = points.size / 2
            val currentCapacity = rawPoints?.size ?: 0
            if(currentCapacity < numPoints * 2)
                rawPoints = FloatArray(numPoints * 2)
            rawPoints?.let {
                points.copyInto(it, 0, 0, numPoints * 2)
                boundingBoxChanged = computeBoundingBox()
            }
        }

        if (numPoints > 0 || numPoints != numPointsBackup) {
//            Log.v("Tuner", "PlotPoints.setPoints: transforming point")
            transformAndCallListener(transformation, boundingBoxChanged)
        }
    }

    private fun computeBoundingBox(): Boolean {
        oldBoundingBox.set(boundingBox)
        rawPoints?.let { points ->
            boundingBox.set(points[0], points[1],points[0], points[1])
            for (i in 0 until numPoints) {
                val x = points[2 * i]
                val y = points[2 * i + 1]
                boundingBox.top = min(boundingBox.top, y)
                boundingBox.bottom = max(boundingBox.bottom, y)
                boundingBox.left = min(boundingBox.left, x)
                boundingBox.right = max(boundingBox.right, x)
            }
        }
        return !boundingBox.contentEquals(oldBoundingBox)
    }

    fun setVisible(isVisible: Boolean) {
        if (this.isVisible == isVisible)
            return
        this.isVisible = isVisible
        transformAndCallListener(transformation, hasNewBoundingBox = false)
    }

    fun drawToCanvas(canvas: Canvas?) {
        if (canvas == null || !isVisible)
            return
//        if (transformation != null && numPoints > 0
//            && transformation?.rawPlotBoundsIntersect(boundingBox) == true) {
        // bounding box check would not be reliable due to the potential offsetX/Y
        if (transformation != null && numPoints > 0) {
            paint.color = colors[styleIndex]
            when (shapes[styleIndex]) {
                PointShapes.Circle -> drawFilledCircles(canvas)
                PointShapes.TriangleUp -> drawTrianglesUp(canvas)
                PointShapes.TriangleDown -> drawTrianglesDown(canvas)
            }
        }
    }

    fun transformAndCallListener(transformation: PlotTransformation?, hasNewBoundingBox: Boolean) {
        rawPoints?.let { raw ->
            if (transformedPoints.size < 2 * numPoints)
                transformedPoints = FloatArray(2 * numPoints)

            transformation?.transformRawToView(raw, transformedPoints, numPoints)
        }
        // Log.v("Tuner", "PlotPoints.transformAndCallListener: rP0 = ${rawPoints?.get(0)} rP1 = ${rawPoints?.get(1)}, v0=${transformedPoints?.get(0)} v1=${transformedPoints?.get(1)}")
        plotPointsChangedListener?.onPlotPointsChanged(this, hasNewBoundingBox)
    }

    override fun transform(transform: PlotTransformation?) {
        transformAndCallListener(transform, hasNewBoundingBox = false)
    }

    private fun drawFilledCircles(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        for(i in 0 until numPoints) {
            val x = transformedPoints[2 * i] + offsetX
            val y = transformedPoints[2 * i + 1] + offsetY
            // TODO: take point size into account (also for other shapes)
            if (transformation?.viewBoundsContain(x, y) == true)
                canvas.drawCircle(x, y, sizes[styleIndex], paint)
        }
    }

    private fun drawTrianglesUp(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        path.rewind()
        path.moveTo(-sizes[styleIndex], 0f)
        path.lineTo(sizes[styleIndex], 0f)
        path.lineTo(0f, -sizes[styleIndex])

        for(i in 0 until numPoints) {
            val x = transformedPoints[2 * i] + offsetX
            val y = transformedPoints[2 * i + 1] + offsetY

            if (transformation?.viewBoundsContain(x, y) == true) {
                path.offset(x, y, pathShifted)
                canvas.drawPath(pathShifted, paint)
            }
        }
    }

    private fun drawTrianglesDown(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        path.rewind()
        path.moveTo(-sizes[styleIndex], 0f)
        path.lineTo(sizes[styleIndex], 0f)
        path.lineTo(0f, sizes[styleIndex])

        for(i in 0 until numPoints) {
            val x = transformedPoints[2 * i] + offsetX
            val y = transformedPoints[2 * i + 1] + offsetY

            if (transformation?.viewBoundsContain(x, y) == true) {
                path.offset(x, y, pathShifted)
                canvas.drawPath(pathShifted, paint)
            }
        }
    }
}

/** Definitions of how large the background of a mark label should be. */
enum class MarkLabelBackgroundSize {FitIndividually, FitLargest}

/** Class managing plotting marks with and without background.
 *  @param transformation Transoformation between raw data and plot window
 *  @param colors Array of mark colors (color of line and/or label background). Each entry belongs
 *    to one style.
 *  @param labelColors Array of label colors (i.e. the text color). Each entry belongs to one
 *    style. (array size must be the same as colors)
 *  @param lineWidths Array with line width for each style. (array size must be the same as
 *    colors). Line widths are only needed if marks are actually lines at constant x or y values.
 *  @param textSizes Array with text for each style. (array size must be the same as colors)
 *  @param disableLabelBackground If false, the labels will have no background.
 *  @param labelPaddingHorizontal Horizontal padding around the label text.
 *  @param labelPaddingVertical Vertical padding around the label text.
 *  @param cornerRadius Corner radius of the label background.
 */
private class PlotMarks(transformation: PlotTransformation,
                        val colors: IntArray,
                        val labelColors: IntArray,
                        val lineWidths: FloatArray,
                        val textSizes: FloatArray,
                        val disableLabelBackground: Boolean,
                        val labelPaddingHorizontal: Float,
                        val labelPaddingVertical: Float,
                        val cornerRadius: Float)
    : PlotTransformable(transformation) {

    /** Interface for callbacks when the plot marks are changed. */
    fun interface PlotMarksChangedListener {
        fun onPlotMarksChanged(plotMarks: PlotMarks, hasNewBoundingBox: Boolean)
    }

    /** Callback for label mark changes. */
    var plotMarksChangedListener: PlotMarksChangedListener? = null

    /** Class describing a single mark.
     * @param xPositionRaw x-position in coordinate system of raw data. This can be DRAW_LINE
     *   if you want to have an y-mark (line along constant y)
     * @param yPositionRaw y-position in coordinate system of raw data. This can be DRAW_LINE
     *   if you want to have an x-mark (line along constant x).
     * @param index Array index of mark.
     * @param anchor Mark anchor how it should be aligned relative to the input coordinates.
     */
    data class Mark(val xPositionRaw: Float, val yPositionRaw: Float, val index: Int, // val label: CharSequence?,
                    val anchor: LabelAnchor = LabelAnchor.Center) {
        /** Style with which the label was created. */
        var labelStyle = 0
        /** Label layout. */
        var layout: Label? = null
    }

    /** Array of marks. */
    private val marks = ArrayList<Mark>()
    /** Flag which tells if this class contains marks or not. */
    val hasMarks: Boolean get() = marks.size > 0

    /** null, if each label should have its own size, else the width to be used for all labels. */
    private var labelWidth: Float? = null
    /** null, if each label should have its own size, else the height to be used for all labels. */
    private var labelHeight: Float? = null

    /** Maximum distance from label top to baseline.
     * Unlike labelWidth/Height, this must always be determined since we need this for baseline
     * alignment if a label is below the plot.
     */
    private var labelTopAboveBaseline = 0f

    /** Maximum distance from label bottom to baseline.
     * Unlike labelWidth/Height, this must always be determined since we need this for baseline
     * alignment if a label is above the plot.
     */
    private var labelBottomBelowBaseline = 0f

    /** Temporary point class to avoid allocation. */
    private val temporaryPointRaw = FloatArray(2) {0f}
    /** Another temporary point class to avoid allocation. */
    private val temporaryPointTransformed = FloatArray(2) {0f}
    /** Path class to draw the lines of x-marks or y-marks. */
    private val path = Path()
    /** Flag if class contains only x-line marks. */
    private var hasOnlyYMarks = false
    /** Flag if class contains only y-line marks. */
    private var hasOnlyXMarks = false

    /** Paint used for drawing label backgrounds, one for each style. If null, no label background
     * will be drawn.
     */
    private val backgroundPaints = Array(colors.size) {
        if (disableLabelBackground) {
            null
        } else {
            Paint().apply {
                isAntiAlias = true
                color = colors[it]
                style = Paint.Style.FILL
            }
        }
    }

    /** Paint for drawing the lines of x-line-marks and y-line marks. One paint for each style. */
    private val linePaints = Array(colors.size) {
        Paint().apply {
            isAntiAlias = true
            color = colors[it]
            style = Paint.Style.STROKE
            strokeWidth = lineWidths[it]
        }
    }

    /** Stroke width of marks in x-raw coordinate space. */
    private val strokeWidthsRawX = FloatArray(colors.size)
    /** Stroke width of marks in y-raw coordinate space. */
    private val strokeWidthsRawY = FloatArray(colors.size)

    /** Paint for drawing the label text. One paint for each style. */
    private val textPaints = Array(colors.size) {
        TextPaint().apply {
            isAntiAlias = true
            color = labelColors[it]
            textSize = textSizes[it]
        }
    }

    /** The maximum label sizes for each style. This can be null if it is not yet determined. */
    private val maxLabelSizes = Array<Label.LabelSetBounds?>(colors.size) { null }

    /** Temporary for storing a bounding box. */
    private val oldBoundingBox = RectF(0f, 0f, 0f, 0f)
    /** The bounding box of all marks (this does not take the label size into account) */
    val boundingBox = RectF(0f, 0f, 0f, 0f)

    /** Current style index, which will be used for drawing marks.
     *   (0 <= styleIndex < numStyles, where numStyles is colors.size)
     */
    private var styleIndex = 0

    /** Tell if the background should be tightly fitted along a label or all backgrounds
     * should be of same size.
     */
    private var backgroundSizeType = MarkLabelBackgroundSize.FitIndividually

    /** If true, the labels will be drawn outside the plot box. This parameter is only
     * used for x-line-marks or y-line-marks.
     */
    var placeLabelsOutsideBoundsIfPossible: Boolean = true
        private set
    /** Functor for creating labels or null if no labels should be drawn. */
    private var labelCreator: ((index: Int, xPosition: Float?, yPosition: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity:LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius: Float) -> Label?)? = null

    /** Functor for computing the maximum size of labels. If null, we will temporarily
     * layout all labels and then use the largest one. So this only is for optimizing if many
     * labels are in an PlotMark object.
     */
    private var maxLabelBoundComputer: ((TextPaint) -> Label.LabelSetBounds)? = null

    /** Set the marks to be plotted.
     * @param xPositions x-coordinates of all marks or null if the marks should be lines
     *   along the x-direction. Alternatively, single entries in the array can be set to
     *   DRAW_LINE to set only some marks to be lines along the x-direction.
     * @param yPositions y-coordinates of all marks or null if the marks should be lines
     *   along the y-direction. Alternatively, single entries in the array can be set to
     *   DRAW_LINE to set only some marks to be lines along the y-direction.
     * @param indexBegin First index in the positions-arrays to be used (included).
     * @param indexEnd End index in the positions-arrays to be used (excluded).
     * @param styleIndex Style index which should be used for drawing.
     * @param anchors Anchor for each mark of how the mark labels should be aligned.
     *   If null, the marks will be centered.
     * @param placeLabelsOutsideBoundsIfPossible If possible (meaning, in case of line marks)
     *   the label is placed outside the plot bounds.
     * @param maxLabelBounds Functor to compute maximum label bounds based on the paint.
     *   This allows optimizations such that only visible marks are actually created.
     *   If null, we will compute the max bounds internally, but this will be more expensive.
     * @param labelCreator Define how to plot the mark label. The lambda will provide the mark index
     *   (the index in xPositions or yPositions), the coordinates (or null for line
     *   marks) and a lot of default values for creating a label, which must be returned.
     */
    fun setMarks(
        xPositions: FloatArray?, yPositions: FloatArray?,
        indexBegin: Int = 0,
        indexEnd: Int = max(xPositions?.size ?: 0, yPositions?.size ?: 0),
        styleIndex: Int = 0,
        anchors: Array<LabelAnchor>? = null,
        backgroundSizeType: MarkLabelBackgroundSize = MarkLabelBackgroundSize.FitIndividually,
        placeLabelsOutsideBoundsIfPossible: Boolean = false,
        maxLabelBounds: ((TextPaint) -> Label.LabelSetBounds)? = null,
        labelCreator: ((index: Int, xPos: Float?, yPos: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity:LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius: Float) -> Label?)? = null
    ) {
        this.placeLabelsOutsideBoundsIfPossible = placeLabelsOutsideBoundsIfPossible
        this.backgroundSizeType = backgroundSizeType
        this.styleIndex = styleIndex
        this.labelCreator = labelCreator
        this.maxLabelBoundComputer = maxLabelBounds

        oldBoundingBox.set(boundingBox)
        val numMarksBefore = marks.size

        val indexBeginResolved = max(indexBegin, 0)
        val indexEndResolved = min(indexEnd, max(xPositions?.size ?: 0, yPositions?.size ?: 0))
        marks.clear()
        val numMarks = max(0, indexEndResolved - indexBeginResolved)
        //val numMarks = max(xPositions?.size ?: 0, yPositions?.size ?: 0)
//        Log.v("Tuner", "PlotMarks.setMarks: numMarks = $numMarks")
        if (numMarks == 0) {
            boundingBox.set(0f, 0f, 0f, 0f)
        } else {
            boundingBox.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
        }

        for (iMark in 0 until numMarks) {
            val i = iMark + indexBeginResolved
            val x = if (i < (xPositions?.size ?: 0)) xPositions?.get(i) ?: DRAW_LINE else DRAW_LINE
            val y = if (i < (yPositions?.size ?: 0)) yPositions?.get(i) ?: DRAW_LINE else DRAW_LINE
            val a = if (i < (anchors?.size ?: 0)) anchors?.get(i) ?: LabelAnchor.Center else LabelAnchor.Center
            marks.add(Mark(x, y, i, a))
//            Log.v("Tuner", "PlotMarks.setMarks: Mark: x=$x, y=$y, s=$s, a=$a")
            if (x != DRAW_LINE) {
                boundingBox.left = min(boundingBox.left, x)
                boundingBox.right = max(boundingBox.right, x)
            }

            if (y != DRAW_LINE) {
                boundingBox.top = min(boundingBox.top, y)
                boundingBox.bottom = max(boundingBox.bottom, y)
            }
        }

        if (boundingBox.left == Float.POSITIVE_INFINITY)
            boundingBox.left = BOUND_UNDEFINED
        if (boundingBox.top == Float.POSITIVE_INFINITY)
            boundingBox.top = BOUND_UNDEFINED
        if (boundingBox.right == Float.NEGATIVE_INFINITY)
            boundingBox.right = BOUND_UNDEFINED
        if (boundingBox.bottom == Float.NEGATIVE_INFINITY)
            boundingBox.bottom = BOUND_UNDEFINED

        maxLabelSizes.fill(null)
        maxLabelSizes[styleIndex] = computeMaxLabelBounds(styleIndex)
        labelWidth = when (backgroundSizeType) {
            MarkLabelBackgroundSize.FitLargest -> maxLabelSizes[styleIndex]?.maxWidth
            MarkLabelBackgroundSize.FitIndividually -> null
        }
        labelHeight = when (backgroundSizeType) {
            MarkLabelBackgroundSize.FitLargest -> maxLabelSizes[styleIndex]?.maxWidth
            MarkLabelBackgroundSize.FitIndividually -> null
        }
        labelTopAboveBaseline = maxLabelSizes[styleIndex]?.maxDistanceAboveBaseline ?: 0f
        labelBottomBelowBaseline = maxLabelSizes[styleIndex]?.maxDistanceBelowBaseline ?: 0f

        if (numMarks > 0 || numMarks != numMarksBefore)
            transformAndCallListener(transformation, !boundingBox.contentEquals(oldBoundingBox))

        if (xPositions == null && yPositions != null) {
            marks.sortBy { it.yPositionRaw }
            hasOnlyYMarks = true
            hasOnlyXMarks = false
        } else if (xPositions != null && yPositions == null) {
            marks.sortBy { it.xPositionRaw }
            hasOnlyYMarks = false
            hasOnlyXMarks = true
        } else {
            hasOnlyYMarks = false
            hasOnlyXMarks = false
        }
    }

    /** Change the style of a label.
     * @param index Style index (0 <= index < numStyles, where numStyles = colors.size)
     */
    fun setStyleIndex(index: Int) {
        if (index == styleIndex)
            return
        styleIndex = index
        if (maxLabelSizes[index] == null)
            maxLabelSizes[index] = computeMaxLabelBounds(index)

        labelWidth = when (backgroundSizeType) {
            MarkLabelBackgroundSize.FitLargest -> maxLabelSizes[styleIndex]?.maxWidth
            MarkLabelBackgroundSize.FitIndividually -> null
        }
        labelHeight = when (backgroundSizeType) {
            MarkLabelBackgroundSize.FitLargest -> maxLabelSizes[styleIndex]?.maxHeight
            MarkLabelBackgroundSize.FitIndividually -> null
        }
        labelTopAboveBaseline = maxLabelSizes[styleIndex]?.maxDistanceAboveBaseline ?: 0f
        labelBottomBelowBaseline = maxLabelSizes[styleIndex]?.maxDistanceBelowBaseline ?: 0f

        plotMarksChangedListener?.onPlotMarksChanged(this, hasNewBoundingBox = false)
    }

    fun drawToCanvas(canvas: Canvas?) {
        val viewBounds = transformation?.viewPlotBounds ?: return
        val rawBounds = transformation?.rawPlotBounds ?: return

        if (marks.size == 0)
            return

        var startIndex = 0
        var endIndex = marks.size

        if (hasOnlyXMarks) {
            if (marks.size > 1)
                require(marks[1].xPositionRaw > marks[0].xPositionRaw)
            temporaryPointTransformed[0] = (maxLabelSizes[styleIndex]?.maxWidth ?: 0f) + 2 * labelPaddingHorizontal
            temporaryPointTransformed[0] = max(temporaryPointTransformed[0], strokeWidthsRawY[styleIndex])
            temporaryPointTransformed[1] = 0f
            transformLabelSizeToRaw(transformation, temporaryPointTransformed, temporaryPointRaw)
            val maxLabelWidthRaw = temporaryPointRaw[0]
            startIndex = marks.binarySearchBy(rawBounds.left - maxLabelWidthRaw) {it.xPositionRaw}
            endIndex = marks.binarySearchBy(rawBounds.right + maxLabelWidthRaw) {it.xPositionRaw}
        } else if (hasOnlyYMarks) {
            if (marks.size > 1)
                require(marks[1].yPositionRaw > marks[0].yPositionRaw) // here we should also take equals, since marks could be at the same position, but then, we must make sure that the binary search finds the correct one
            temporaryPointTransformed[0] = 0f
            temporaryPointTransformed[1] = (maxLabelSizes[styleIndex]?.maxHeight ?: 0f) + 2 * labelPaddingVertical
            temporaryPointTransformed[1] = max(temporaryPointTransformed[1], strokeWidthsRawX[styleIndex])
            transformLabelSizeToRaw(transformation, temporaryPointTransformed, temporaryPointRaw)
            val maxLabelHeightRaw = temporaryPointRaw[1]
            startIndex = marks.binarySearchBy(rawBounds.top - maxLabelHeightRaw) {it.yPositionRaw}
            endIndex = marks.binarySearchBy(rawBounds.bottom + maxLabelHeightRaw) {it.yPositionRaw}
        }

        if(startIndex < 0)
            startIndex = -(startIndex + 1)
        if(endIndex < 0)
            endIndex = -(endIndex + 1)

        for (i in startIndex until endIndex) {
            val mark = marks[i]
            // only plot if mark is within bounds
            val xRaw = mark.xPositionRaw
            val yRaw = mark.yPositionRaw
            transformPointToView(transformation, xRaw, yRaw, temporaryPointTransformed)
            var x = temporaryPointTransformed[0]
            var y = temporaryPointTransformed[1]
            // horizontal line
            if (xRaw == DRAW_LINE && yRaw > rawBounds.top && yRaw < rawBounds.bottom) {
                path.rewind()
                path.moveTo(viewBounds.left, y)
                path.lineTo(viewBounds.right, y)
                canvas?.drawPath(path, linePaints[styleIndex])
            }
            // vertical line
            else if (yRaw == DRAW_LINE && xRaw > rawBounds.left && xRaw < rawBounds.right) {
                path.rewind()
                path.moveTo(x, viewBounds.bottom)
                path.lineTo(x, viewBounds.top)
                canvas?.drawPath(path, linePaints[styleIndex])
            }

            var labelAnchorResolved: LabelAnchor
            getLabelFromMark(mark, styleIndex)?.let { layout ->
                // override mark position based on anchor
                if (xRaw == DRAW_LINE) {
                    x = when (mark.anchor) {
                            LabelAnchor.Center, LabelAnchor.North, LabelAnchor.South, LabelAnchor.Baseline -> viewBounds.centerX()
                            LabelAnchor.West, LabelAnchor.NorthWest, LabelAnchor.SouthWest, LabelAnchor.BaselineWest -> viewBounds.left
                            LabelAnchor.East, LabelAnchor.NorthEast, LabelAnchor.SouthEast, LabelAnchor.BaselineEast -> viewBounds.right
                        }

                    labelAnchorResolved = if (placeLabelsOutsideBoundsIfPossible) {
                        when(mark.anchor) {
                            LabelAnchor.West -> LabelAnchor.East
                            LabelAnchor.East -> LabelAnchor.West
                            LabelAnchor.NorthEast -> LabelAnchor.NorthWest
                            LabelAnchor.NorthWest -> LabelAnchor.NorthEast
                            LabelAnchor.SouthEast -> LabelAnchor.SouthWest
                            LabelAnchor.SouthWest -> LabelAnchor.SouthEast
                            LabelAnchor.BaselineWest -> LabelAnchor.BaselineEast
                            LabelAnchor.BaselineEast -> LabelAnchor.BaselineWest
                            else -> mark.anchor
                        }
                    } else {
                        mark.anchor
                    }
                } else if (y == DRAW_LINE) {
                    y = if (placeLabelsOutsideBoundsIfPossible) {
                        when (mark.anchor) {
                            LabelAnchor.Center, LabelAnchor.West, LabelAnchor.East -> viewBounds.centerY()
                            LabelAnchor.South, LabelAnchor.SouthWest, LabelAnchor.SouthEast -> viewBounds.bottom + labelTopAboveBaseline + labelPaddingVertical
                            LabelAnchor.North, LabelAnchor.NorthWest, LabelAnchor.NorthEast -> viewBounds.top - labelBottomBelowBaseline - labelPaddingVertical
                            else -> viewBounds.centerY() // baseline really does not make sense ...
                        }
                    } else {
                        when (mark.anchor) {
                            LabelAnchor.Center, LabelAnchor.West, LabelAnchor.East -> viewBounds.centerY()
                            LabelAnchor.South, LabelAnchor.SouthWest, LabelAnchor.SouthEast -> viewBounds.bottom
                            LabelAnchor.North, LabelAnchor.NorthWest, LabelAnchor.NorthEast -> viewBounds.top
                            else -> viewBounds.centerY() // baseline really does not make sense ...
                        }

                    }
                    labelAnchorResolved = if (placeLabelsOutsideBoundsIfPossible) {
                        when (mark.anchor) {
                            LabelAnchor.North -> LabelAnchor.Baseline
                            LabelAnchor.South ->  LabelAnchor.Baseline
                            LabelAnchor.NorthEast -> LabelAnchor.BaselineEast
                            LabelAnchor.NorthWest -> LabelAnchor.BaselineWest
                            LabelAnchor.SouthEast -> LabelAnchor.BaselineEast
                            LabelAnchor.SouthWest -> LabelAnchor.BaselineWest
                            else -> mark.anchor
                        }
                    } else {
                        mark.anchor
                    }
                } else { // -> (x != DRAW_LINE && y != DRAW_LINE)
                    labelAnchorResolved = mark.anchor
                }

                if (isMarkInBoundingBox(mark, x, y, labelAnchorResolved)) {
                    if (backgroundSizeType == MarkLabelBackgroundSize.FitLargest) {
                        layout.drawToCanvasWithFixedSizeBackground(
                            x, y,
                            (maxLabelSizes[styleIndex]?.maxWidth ?: 0f) + 2 * labelPaddingHorizontal,
                            (maxLabelSizes[styleIndex]?.maxHeight ?: 0f) + 2 * labelPaddingVertical,
                            0f,
                            labelAnchorResolved,
                            canvas
                        )
                    } else {
                        layout.drawToCanvasWithPaddedBackground(x, y, labelAnchorResolved, canvas)
                    }
                }
            }
        }
    }

    fun transformAndCallListener(transform: PlotTransformation?, hasNewBoundingBox: Boolean) {
        lineWidths.forEachIndexed { index, lineWidth ->
            transformStrokeWidthToRaw(transform, lineWidth, temporaryPointTransformed)
            strokeWidthsRawX[index] = temporaryPointTransformed[0]
            strokeWidthsRawY[index] = temporaryPointTransformed[1]
        }

        plotMarksChangedListener?.onPlotMarksChanged(this, hasNewBoundingBox)
    }

    fun numXMarksWithinRange(minValue: Float, maxValue: Float): Int {
        require(hasOnlyXMarks)
        val startIndex = marks.binarySearchBy(minValue) {it.xPositionRaw}
        val endIndex = marks.binarySearchBy(maxValue) {it.xPositionRaw}

        val startIndexResolved  = if (startIndex < 0) -startIndex - 1 else startIndex
        val endIndexResolved  = if (endIndex < 0) -endIndex - 1 else endIndex + 1
        return endIndexResolved - startIndexResolved
    }

    fun numYMarksWithinRange(minValue: Float, maxValue: Float): Int {
        require(hasOnlyYMarks)
        val startIndex = marks.binarySearchBy(minValue) {it.yPositionRaw}
        val endIndex = marks.binarySearchBy(maxValue) {it.yPositionRaw}

        val startIndexResolved  = if (startIndex < 0) -startIndex - 1 else startIndex
        val endIndexResolved  = if (endIndex < 0) -endIndex - 1 else endIndex + 1
        return endIndexResolved - startIndexResolved
    }

    override fun transform(transform: PlotTransformation?) {
        transformAndCallListener(transform, hasNewBoundingBox = false)
    }

    private fun transformStrokeWidthToRaw(transform: PlotTransformation?, strokeWidth: Float, result: FloatArray) {
        require(result.size == 2)
        result[0] = strokeWidth
        result[1] = strokeWidth
        transform?.transformVectorViewTorRaw(result)
        result[0] = result[0].absoluteValue
        result[1] = result[1].absoluteValue
    }

    private fun transformLabelSizeToRaw(transform: PlotTransformation?, labelSize: FloatArray, rawLabelSize: FloatArray) {
        require(labelSize.size == 2)
        require(rawLabelSize.size == 2)
        labelSize.copyInto(rawLabelSize)
        transform?.transformVectorViewTorRaw(rawLabelSize)
        rawLabelSize[0] = rawLabelSize[0].absoluteValue
        rawLabelSize[1] = rawLabelSize[1].absoluteValue
    }

    private fun transformPointToView(transform: PlotTransformation?, xRaw: Float, yRaw: Float, result: FloatArray) {
        result[0] = if (xRaw == DRAW_LINE) 0f else xRaw
        result[1] = if (yRaw == DRAW_LINE) 0f else yRaw
        transform?.transformRawToView(result)
        result[0] = if (xRaw == DRAW_LINE) DRAW_LINE else result[0]
        result[1] = if (yRaw == DRAW_LINE) DRAW_LINE else result[1]
    }

    /** Check if a mark is in the currently shown view.
     * @param mark Mark for which we do the check.
     * @param labelX Resolved x-position of label (taking into account that line labels are at the
     *   side of the plot).
     * @param labelY Resolved y-position of label (taking into account that line labels are at the
     *   side of the plot).
     * @param labelAnchor Resolved anchor of a label (relative to labelX, labelY), taking into
     *   account that anchors of x-line or y-line labels have different meanings.
     */
    private fun isMarkInBoundingBox(mark: Mark, labelX: Float, labelY: Float, labelAnchor: LabelAnchor): Boolean {
        val bb = transformation?.viewPlotBounds ?: return false

        if (placeLabelsOutsideBoundsIfPossible && mark.xPositionRaw == DRAW_LINE) {
            val strokeWidth = linePaints[styleIndex].strokeWidth
            return labelY - 0.5f * strokeWidth <= bb.bottom && labelY + 0.5f * strokeWidth >= bb.top
        } else if (placeLabelsOutsideBoundsIfPossible && mark.yPositionRaw == DRAW_LINE) {
            val strokeWidth = linePaints[styleIndex].strokeWidth
            return labelX - 0.5f * strokeWidth <= bb.right && labelX + 0.5f * strokeWidth >= bb.left
        }

        val labelWidth: Float
        val labelHeight: Float
        if (backgroundSizeType == MarkLabelBackgroundSize.FitLargest) {
            labelWidth = (maxLabelSizes[styleIndex]?.maxWidth ?: 0f) + 2 * labelPaddingHorizontal
            labelHeight = when (labelAnchor) {
                LabelAnchor.Baseline, LabelAnchor.BaselineEast, LabelAnchor.BaselineWest ->
                    (maxLabelSizes[styleIndex]?.maxDistanceAboveBaseline ?: 0f) + (maxLabelSizes[styleIndex]?.maxDistanceBelowBaseline ?: 0f) + 2 * labelPaddingVertical
                else -> (maxLabelSizes[styleIndex]?.maxHeight ?: 0f) + 2 * labelPaddingVertical
            }
        } else {
            labelWidth = (mark.layout?.labelWidth ?: 0f) + 2 * labelPaddingHorizontal
            labelHeight = when (labelAnchor) {
                LabelAnchor.Baseline, LabelAnchor.BaselineEast, LabelAnchor.BaselineWest ->
                    (mark.layout?.labelBaselineBelowTop ?: 0f) + (mark.layout?.labelBottomBelowBaseline ?: 0f) + 2 * labelPaddingVertical
                else -> (mark.layout?.labelHeight ?: 0f) + 2 * labelPaddingVertical
            }
        }

        val xMin = when (labelAnchor) {
            LabelAnchor.Center, LabelAnchor.North, LabelAnchor.South, LabelAnchor.Baseline -> labelX - 0.5f * labelWidth
            LabelAnchor.West, LabelAnchor.NorthWest, LabelAnchor.SouthWest, LabelAnchor.BaselineWest -> labelX
            LabelAnchor.East, LabelAnchor.NorthEast, LabelAnchor.SouthEast, LabelAnchor.BaselineEast -> labelX - labelWidth
        }
        val yMin = when (labelAnchor) {
            LabelAnchor.Center, LabelAnchor.East, LabelAnchor.West -> labelY - 0.5f * labelHeight
            LabelAnchor.North, LabelAnchor.NorthEast, LabelAnchor.NorthWest -> labelY
            LabelAnchor.South, LabelAnchor.SouthEast, LabelAnchor.SouthWest -> labelY - labelHeight
            LabelAnchor.Baseline, LabelAnchor.BaselineEast, LabelAnchor.BaselineWest -> labelY - (mark.layout?.labelBaselineBelowTop ?: 0f)
        }
        val xMax = xMin + labelWidth
        val yMax = yMin + labelHeight
        return (yMax >= bb.top && yMin <= bb.bottom && xMax >= bb.left && xMin <= bb.right)
    }

    fun computeMaxLabelBounds(styleIndex: Int): Label.LabelSetBounds? {
        // we only need to compute the max sizes if there are actually labels
        if (labelCreator == null)
            return null

        maxLabelBoundComputer?.let {
            return it(textPaints[styleIndex])
        }

        var maxWidth = 0f
        var maxHeight = 0f
        var maxDistanceAboveBaseline = 0f
        var maxDistanceBelowBaseline = 0f
        var verticalCenterAboveBaseline = 0f
        marks.forEach { mark ->
            val label = getLabelFromMark(mark, styleIndex)
            maxWidth = max(label?.labelWidth ?: 0f, maxWidth)
            maxHeight = max(label?.labelHeight ?: 0f, maxHeight)
            maxDistanceAboveBaseline = max(label?.labelBaselineBelowTop ?: 0f, maxDistanceAboveBaseline)
            maxDistanceBelowBaseline = max(label?.labelBottomBelowBaseline ?: 0f, maxDistanceBelowBaseline)
            verticalCenterAboveBaseline += label?.verticalCenterAboveBaseline ?: 0f
        }
        verticalCenterAboveBaseline /= marks.size
        return Label.LabelSetBounds(maxWidth, maxHeight, maxDistanceAboveBaseline, maxDistanceBelowBaseline, verticalCenterAboveBaseline)
    }

    private fun getLabelFromMark(mark: Mark, styleIndex: Int): Label? {
        if (mark.labelStyle == styleIndex && mark.layout != null)
            return mark.layout
        val creator = labelCreator ?: return null
        val label = creator(
            mark.index,
            if (mark.xPositionRaw == DRAW_LINE) null else mark.xPositionRaw,
            if (mark.yPositionRaw == DRAW_LINE) null else mark.yPositionRaw,
            textPaints[styleIndex],
            backgroundPaints[styleIndex], LabelGravity.Center,
            labelPaddingHorizontal, labelPaddingVertical, cornerRadius
        )
        mark.layout = label
        mark.labelStyle = styleIndex
        return label
    }

    companion object {
        /// Special setting for defining marks with horizontal or vertical lines.
        const val DRAW_LINE = Float.MAX_VALUE
        const val BOUND_UNDEFINED = Float.MAX_VALUE
    }
}

private class AdaptiveTicks(
    transformation: PlotTransformation,
    val direction: Char,
    val tickPosition: LabelAnchor,
    val colors: IntArray,
    val lineWidths: FloatArray,
    val textSizes: FloatArray,
    val labelPaddingHorizontal: Float,
    val labelPaddingVertical: Float): PlotTransformable(transformation) {

    /** Interface for callbacks when the plot marks are changed. */
    fun interface AdaptiveTicksChangedListener {
        fun onTicksChanged(ticks: AdaptiveTicks, marks: PlotMarks, boundingBoxChanged: Boolean)
    }

    /** Callback for tick changes. */
    var adaptiveTicksChangedListener: AdaptiveTicksChangedListener? = null

    private val marks = ArrayList<PlotMarks>()
    /** Cached last used plot marks for quicker access if not much changes. */
    private var currentlyUsedPlotMarks: PlotMarks? = null

    private var targetNumPlotMarks = 7
    private val targetTolerance = 1

    private var currentTolerance = 1

    private var maxLabelWidth = 1f
    private var maxLabelHeight = 1f

    var hasTicks = false
        private set

    // required:
    // - maximum width / height OR number of marks
    // - minimum distance between marks (we assume that the marks are roughly equally distributed)
    // function which gets raw range (only distance, no min/max) and returns the number of marks
    // or better: mapping from raw-range to Marks-class

    fun drawToCanvas(canvas: Canvas?) {
        currentlyUsedPlotMarks?.drawToCanvas(canvas)
    }

    fun clear() {
        for (m in marks) {
            m.unregisterFromPlotTransformation()
        }
        marks.clear()
    }

    private fun resetCurrentlyUsedPlotMarks(minValue: Float, maxValue: Float) {
        if (marks.size <= 1) {
            currentlyUsedPlotMarks = marks.firstOrNull()
            return
        }

        val currentNumMarks = when (direction) {
            'x' -> currentlyUsedPlotMarks?.numXMarksWithinRange(minValue, maxValue) ?: 0
            'y' -> currentlyUsedPlotMarks?.numYMarksWithinRange(minValue, maxValue) ?: 0
            else -> 0
        }

        if ((currentNumMarks - targetNumPlotMarks).absoluteValue <= currentTolerance)
            return

        var bestSuitedMarks: PlotMarks? = null
        var numBestSuitedMarks = Int.MAX_VALUE
        //var distBestSuitedMarks = Int.MAX_VALUE
        for (m in marks) {
            val numMarks = when (direction) {
                'x' -> m.numXMarksWithinRange(minValue, maxValue)
                'y' -> m.numYMarksWithinRange(minValue, maxValue)
                else -> 0
            }
            val dist = (numMarks - targetNumPlotMarks).absoluteValue
            val distBestSuited = (numBestSuitedMarks - targetNumPlotMarks).absoluteValue
            var replaceBestMarksWithCurrentValue = false
            // we try to stay below targetNumPlotMarks + targetTolerance, and then use the value
            // closest to targetNumPlotMarks.
            // if all available plot mark classes will result in more plot marks than allowed
            // we use the one with the least number of marks
            if (numMarks <= targetNumPlotMarks + targetTolerance) {
                if (numBestSuitedMarks > targetNumPlotMarks + targetTolerance || dist < distBestSuited)
                    replaceBestMarksWithCurrentValue = true
            } else {
                if (numBestSuitedMarks > targetNumPlotMarks + targetTolerance && dist < distBestSuited)
                    replaceBestMarksWithCurrentValue = true
            }

            if (replaceBestMarksWithCurrentValue) {
                bestSuitedMarks = m
                numBestSuitedMarks = numMarks
            }
        }

        if (bestSuitedMarks != null) {
            currentTolerance =
                max(targetTolerance, (numBestSuitedMarks - targetNumPlotMarks).absoluteValue)
            currentlyUsedPlotMarks = bestSuitedMarks
        }
    }

    fun addTicksLevel(
        positions: FloatArray,
        indexBegin: Int = 0,
        indexEnd: Int = positions.size,
        maxLabelBounds: ((TextPaint) -> Label.LabelSetBounds)? = null,
        labelCreator: ((index: Int, xPos: Float?, yPos: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity:LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius: Float) -> Label?)? = null
    ) {
        val styleIndex = 0
        transformation?.let {
            if (indexEnd - indexBegin > 0)
                hasTicks = true

            val newMarks = PlotMarks(
                it, colors, colors, lineWidths, textSizes,
                disableLabelBackground = true, labelPaddingHorizontal, labelPaddingVertical,
                0f
            )
            val xPositions = if (direction == 'x') positions else null
            val yPositions = if (direction == 'y') positions else null

            newMarks.setMarks(
                xPositions, yPositions, indexBegin, indexEnd,
                styleIndex = styleIndex, anchors = Array(positions.size) { tickPosition },
                backgroundSizeType = MarkLabelBackgroundSize.FitIndividually,
                placeLabelsOutsideBoundsIfPossible = true,
                maxLabelBounds = maxLabelBounds,
                labelCreator = labelCreator
            )

            newMarks.plotMarksChangedListener = PlotMarks.PlotMarksChangedListener { m, _ ->
                if (m === currentlyUsedPlotMarks)
                    adaptiveTicksChangedListener?.onTicksChanged(this, m, false)
            }
            marks.add(newMarks)

            val bounds = newMarks.computeMaxLabelBounds(styleIndex)
            maxLabelWidth = max(bounds?.maxWidth ?: 1f, maxLabelWidth)
            maxLabelHeight = max(bounds?.maxWidth ?: 1f, maxLabelHeight)
            recomputeTargetNumMarks()
        }
        checkAndSetMarks(callChangedListener = true)
    }

    private fun checkAndSetMarks(callChangedListener: Boolean) {
        val rawBounds = transformation?.rawPlotBounds ?: return
        val rangeMin: Float
        val rangeMax: Float
        when (direction) {
            'x' -> {
                rangeMin = rawBounds.left
                rangeMax = rawBounds.right
            }
            'y' -> {
                rangeMin = rawBounds.top
                rangeMax = rawBounds.bottom
            }
            else -> {
                return
            }
        }

        val bestSuitedMarksBefore = currentlyUsedPlotMarks
        resetCurrentlyUsedPlotMarks(rangeMin, rangeMax)
        val marksUnchanged = (bestSuitedMarksBefore === currentlyUsedPlotMarks)
        if (callChangedListener && !marksUnchanged && currentlyUsedPlotMarks != null)
            adaptiveTicksChangedListener?.onTicksChanged(this, currentlyUsedPlotMarks!!, false)
    }
    private fun recomputeTargetNumMarks() {
        targetNumPlotMarks = when (direction) {
            'x' -> ((transformation?.viewPlotBounds?.width() ?: 1f) / (maxLabelWidth + labelPaddingHorizontal)).toInt() - 1
            'y' -> ((transformation?.viewPlotBounds?.height() ?: 1f) / (maxLabelHeight + labelPaddingVertical)).toInt() - 1
            else -> 7
        }
    }
    override fun transform(transform: PlotTransformation?) {
        recomputeTargetNumMarks()
        checkAndSetMarks(callChangedListener = false)
        currentlyUsedPlotMarks?.let { m ->
            adaptiveTicksChangedListener?.onTicksChanged(this, m, false)
        }
    }
}

//class AutoTicks(
//    private val tickDistance: Float,
//    private val precision: Int,
//    private val maxNumTicks: Int,
//    private val baseDist: Float) {
//
//    /** First tick index (included) */
//    fun getBeginIndex(rangeStart: Float) =  ceil(rangeStart / tickDistance).toInt()
//    /** Last tick index (excluded) */
//    fun getEndIndex(rangeEnd: Float) =  ceil(rangeEnd / tickDistance).toInt()
//
//    fun getTickValue(distanceFactor: Int): Float {
//        return tickDistance * distanceFactor
//    }
//
//    fun getTickString(distanceFactor: Int): String {
//        val v = getTickValue(distanceFactor)
//        return "%.${precision}f".format(v)
//    }
//
//    fun checkValidity(rangeStart: Float, rangeEnd: Float): Boolean {
//        val diff = rangeEnd - rangeStart
//        var tickDist = 0f
//        for (m in allowedMultipliers) {
//            tickDist = baseDist * m
//            val numTicks = floor(diff / tickDist).toInt()
//            if (numTicks <= maxNumTicks)
//                break
//        }
//        return tickDist == tickDistance
//    }
//
//    companion object {
//        private val allowedMultipliers = floatArrayOf(0.1f, 0.2f, 0.5f, 1f)
//
//        fun build(rangeStart: Float, rangeEnd: Float, maxNumTicks: Int): AutoTicks {
//            val diff = rangeEnd - rangeStart
//            val diffOrder = log10(diff)
//            val diffOrderRounded = diffOrder.roundToInt()
//            val baseDist = 10f.pow(diffOrderRounded)
//            var tickDist = 0f
//
//            for (m in allowedMultipliers) {
//                tickDist = baseDist * m
//                val numTicks = floor(diff / tickDist).toInt()
//                if (numTicks <= maxNumTicks)
//                    break
//            }
//            val precision = max(0, -diffOrderRounded + 1)
//            return AutoTicks(tickDist, precision, maxNumTicks, baseDist)
//        }
//    }
//}

class PlotRectangleAndPoint {
    var isEnabled = false
    private val rect = RectF()
    private val point = FloatArray(2)
    private val paint = Paint().apply {
        color = Color.RED
        strokeWidth = 2f
    }

    fun drawToCanvas(canvas: Canvas?) {
        if (!isEnabled)
            return
        paint.style = Paint.Style.STROKE
        canvas?.drawRect(rect, paint)

        paint.style = Paint.Style.FILL
        canvas?.drawCircle(point[0], point[1], 5f, paint)
    }
}

/** PlotView class. */
class PlotView(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    : View(context, attrs, defStyleAttr)
{
    private val rawViewTransformation = PlotTransformation().apply {
        transformationFinishedListener = PlotTransformation.TransformationChangedListener { _ ->
            invalidate()
        }
    }

    /** Class for measuring and printing notes. */
    private val noteNamePrinter = NoteNamePrinter(context)

    private val rectangleAndPoint = PlotRectangleAndPoint()

    private lateinit var _xRange: PlotRange
    private lateinit var _yRange: PlotRange

    private var allowTouchX = true
    private var allowTouchY = true

    private var touchManualControlDrawable: TouchControlDrawable

    var enableExtraPadding = false
        set(value) {
            if (value != field) {
                field = value
                invalidate()
            }
        }
    private var extraPaddingLeft = 0f
    private var extraPaddingRight = 0f

    /** Bounding box of all data which are inside the plot. */
    private val boundingBox = RectF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
    /** Storage for boundingBox-backup. */
    private val boundingBoxOld = RectF(0f, 0f, 0f, 0f)

    /** Number of available mark styles. */
    private val numMarkStyles = 3
    /** Color of x- and y-marks. */
    private var markColor = IntArray(numMarkStyles) {Color.BLACK}
    /** Line width of x- and y-marks. */
    private var markLineWidth = FloatArray(numMarkStyles) {2f}
    /** Text size of x- and y-mark labels. */
    private var markTextSize = FloatArray(numMarkStyles) {10f}
    /** Text color of mark labels. */
    private var markLabelColor = IntArray(numMarkStyles) {Color.WHITE}
    /** Horizontal padding for mark labels. */
    private var markPaddingHorizontal = 2f
    /** Vertical padding for mark labels. */
    private var markPaddingVertical = 2f
    /** Corner radius for mark labels. */
    private var markCornerRadius = 2f

    /** Marks groups. */
    private val markGroups = mutableMapOf<Long, PlotMarks>()

    /** Color list for different styles of plot line. */
    private var plotLineColors = IntArray(3) {Color.BLACK}
    /** Widths for different styles of plot line. */
    private var plotLineWidths = FloatArray(3) {5f}
    /** Plot lines classes. */
    private val plotLines = mutableMapOf<Long, PlotLine>()

    /** Symbol colors for different styles of points. */
    private val pointColors = IntArray(7) {Color.BLACK}
    /** Symbol size for different styles of the points. */
    val pointSizes = FloatArray(7) {5f}
    /** Point shape for different styles of the points. */
    private val pointShapes = Array(7) { PointShapes.Circle }
    /** Point instances. */
    private val plotPoints = mutableMapOf<Long, PlotPoints>()

    /** Color of ticks and tick labels. */
    private var tickColor = Color.BLACK
    /** Line width of ticks. */
    private var tickLineWidth = 2f
    /** Text size of ticks. */
    private var tickTextSize = 10f
    /** Horizontal padding of tick labels. */
    private var tickPaddingHorizontal = 2f
    /** Vertical padding of tick labels. */
    private var tickPaddingVertical = 2f
    /** Width of y-tick labels (defines the horizontal space required, must me larger zero if y-tick labels are defined). */
    private var yTickLabelWidth = 0.0f
    /** Position of y ticks. */
    private var yTickPosition = LabelAnchor.West

    /** Object handling the x-ticks. */
    private var xTicks: AdaptiveTicks
    /** Object handling the y-ticks. */
    private var yTicks: AdaptiveTicks

    /** Plot title. */
    private var title : String? = null
    /** Font size of title. */
    private var titleSize = 10f
    /** Color of title. */
    private var titleColor = Color.BLACK
    /** Paint used for plotting title and frame. */
    private val titlePaint = Paint()

    /** Stroke width used for frame. */
    private var frameStrokeWidth = 1f
    private var frameColor = Color.BLACK
    private var frameColorOnTouch = Color.RED
    private val framePaint = Paint()
    private var frameCornerRadius = 0f

    @Parcelize
    private class SavedState(
        val xRange: PlotRange.SavedState,
        val yRange: PlotRange.SavedState,
    ): Parcelable

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        val rectView = RectF()
        val rectRaw = RectF()
        val flingAnimation = FlingAnimation(FloatValueHolder()).apply {
            addUpdateListener { _, value, _ ->
                val distanceX = flingDirX * (value - lastFlingValue)
                val distanceY = flingDirY * (value - lastFlingValue)
//                Log.v("Tuner", "PlotView: gestureListener, flingAnimation: value=$value, lastValue=$lastFlingValue, distanceX=$distanceX, distanceY=$distanceY")
                lastFlingValue = value
                scrollDistance(-distanceX, -distanceY)
            }
        }
        var flingDirX = 0f
        var flingDirY = 0f
        var lastFlingValue = 0f

        private fun scrollDistance(distanceX: Float,distanceY: Float) {
            rectView.set(rawViewTransformation.viewPlotBounds)
            rectView.offset(distanceX, distanceY)
            rawViewTransformation.transformViewToRaw(rectView, rectRaw)

            // don't scroll over the limits
            if (rectRaw.left < _xRange.touchBasedRangeLimits[0]) {
                val w = rectRaw.width()
                rectRaw.left = _xRange.touchBasedRangeLimits[0]
                rectRaw.right = rectRaw.left + w
            } else if (rectRaw.right > _xRange.touchBasedRangeLimits[1]) {
                val w = rectRaw.width()
                rectRaw.right = _xRange.touchBasedRangeLimits[1]
                rectRaw.left = rectRaw.right - w
            }
            if (rectRaw.top < _yRange.touchBasedRangeLimits[0]) {
                val h = rectRaw.height()
                rectRaw.top = _yRange.touchBasedRangeLimits[0]
                rectRaw.bottom = rectRaw.top + h
            } else if (rectRaw.bottom > _yRange.touchBasedRangeLimits[1]) {
                val h = rectRaw.height()
                rectRaw.bottom = _yRange.touchBasedRangeLimits[1]
                rectRaw.top = rectRaw.bottom - h
            }

            _xRange.setTouchRange(rectRaw.left, rectRaw.right,
                PlotRange.AnimationStrategy.Direct, 0L
            )
            _yRange.setTouchRange(rectRaw.top, rectRaw.bottom,
                PlotRange.AnimationStrategy.Direct, 0L
            )
            ViewCompat.postInvalidateOnAnimation(this@PlotView)
        }

        override fun onDown(e: MotionEvent): Boolean {
//            Log.v("Tuner", "PlotView: gestureListener.OnDown")
            flingAnimation.cancel()
            return true
        }
        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
//            Log.v("Tuner", "PlotView: gestureListener.OnScroll x=$distanceX, y=$distanceY")
            scrollDistance(distanceX, distanceY)
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val velocityTotal = (velocityX.pow(2) + velocityY.pow(2)).pow(0.5f)
            if (velocityTotal < 1f)
                return true
            flingDirX = velocityX / velocityTotal
            flingDirY = velocityY / velocityTotal
            lastFlingValue = 0f
            flingAnimation.cancel()
            flingAnimation.setStartValue(0f)
            flingAnimation.setStartVelocity(velocityTotal)
            flingAnimation.start()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
//            Log.v("Tuner", "PlotView: onSingleTapUp")
//            if (e == null)
//                return true
//            rectangleAndPoint.isEnabled = true
//            rectangleAndPoint.point[0] = e.x
//            rectangleAndPoint.point[1] = e.y
//            rectangleAndPoint.rect.set(rawViewTransformation.viewPlotBounds)
//            rectangleAndPoint.rect.offset(-e.x, -e.y)
//            rectangleAndPoint.rect.left = rectangleAndPoint.rect.left / 2f
//            rectangleAndPoint.rect.top = rectangleAndPoint.rect.top / 2f
//            rectangleAndPoint.rect.right = rectangleAndPoint.rect.right / 2f
//            rectangleAndPoint.rect.bottom = rectangleAndPoint.rect.bottom / 2f
//            rectangleAndPoint.rect.offset(e.x, e.y)
//            val testRect = RectF()
//            rawViewTransformation.transformViewToRaw(rectangleAndPoint.rect, testRect)
//            Log.v("Tuner", "PlotView: onSingleTapUp, view= ${rawViewTransformation.viewPlotBounds}, raw=${rawViewTransformation.rawPlotBounds}")
//            Log.v("Tuner", "PlotView: onSingleTapUp, rawrect= $testRect")
//
//            invalidate()
//            return true

            performClick()
            return true
        }
    }

    private val scaleGestureListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        val rectView = RectF()
        val rectRaw = RectF()
        var lastSpanX = 0f
        var lastSpanY = 0f

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            lastSpanX = detector.currentSpanX
            lastSpanY = detector.currentSpanY
            return true
        }
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val spanX = detector.currentSpanX
            val spanY = detector.currentSpanY
            val scaleX = lastSpanX / spanX
            val scaleY = lastSpanY / spanY
            val focusX = detector.focusX
            val focusY = detector.focusY

//            Log.v("Tuner", "PlotView onScale: scaleX=$scaleX, scaleY=$scaleY, spanX=$spanX, spanY=$spanY, focusX=$focusX, focusY=$focusY")
            rectView.set(rawViewTransformation.viewPlotBounds)
            rectView.offset(-focusX, -focusY)
            rectView.set(scaleX * rectView.left, scaleY * rectView.top,
                scaleX * rectView.right, scaleY * rectView.bottom)
            rectView.offset(focusX, focusY)
            rawViewTransformation.transformViewToRaw(rectView, rectRaw)
            _xRange.setTouchRange(rectRaw.left, rectRaw.right,
                PlotRange.AnimationStrategy.Direct, 0L
            )
            _yRange.setTouchRange(rectRaw.top, rectRaw.bottom,
                PlotRange.AnimationStrategy.Direct, 0L
            )


//            rectangleAndPoint.isEnabled = true
//            rectangleAndPoint.point[0] = focusX
//            rectangleAndPoint.point[1] = focusY
//            rectangleAndPoint.rect.set(rectView)
//            rectangleAndPoint.rect.set(rawViewTransformation.viewPlotBounds)
//            rectangleAndPoint.rect.offset(-focusX, -focusY)
//            rectangleAndPoint.rect.left = rectangleAndPoint.rect.left / 2f
//            rectangleAndPoint.rect.top = rectangleAndPoint.rect.top / 2f
//            rectangleAndPoint.rect.right = rectangleAndPoint.rect.right / 2f
//            rectangleAndPoint.rect.bottom = rectangleAndPoint.rect.bottom / 2f
//            rectangleAndPoint.rect.offset(focusX, focusY)
//            val testRect = RectF()
//            rawViewTransformation.transformViewToRaw(rectangleAndPoint.rect, testRect)

            ViewCompat.postInvalidateOnAnimation(this@PlotView)

            lastSpanX = spanX
            lastSpanY = spanY
            //invalidate()

            return true
        }
    }

    private val gestureDetector = GestureDetector(context, gestureListener)
    private val scaleGestureDetector = ScaleGestureDetector(context, scaleGestureListener)

    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs,
        R.attr.plotViewStyle
    )

    init {
        isSaveEnabled = true

        var touchDrawableId = R.drawable.ic_manual
        var touchManualControlDrawableWidth = 10f
        var touchDrawableBackgroundTint = Color.WHITE

        attrs?.let {
            val ta = context.obtainStyledAttributes(attrs,
                R.styleable.PlotView, defStyleAttr,
                R.style.PlotViewStyle
            )

            extraPaddingLeft = ta.getDimension(R.styleable.PlotView_extraPaddingLeft, extraPaddingLeft)
            extraPaddingRight = ta.getDimension(R.styleable.PlotView_extraPaddingRight, extraPaddingRight)

            //val ta = context.obtainStyledAttributes(it, R.styleable.PlotView)
            plotLineColors[0] = ta.getColor(R.styleable.PlotView_plotLineColor, plotLineColors[0])
            plotLineColors[1] = ta.getColor(R.styleable.PlotView_plotLineColor2, plotLineColors[1])
            plotLineColors[2] = ta.getColor(R.styleable.PlotView_plotLineColor3, plotLineColors[2])
            plotLineWidths[0] = ta.getDimension(R.styleable.PlotView_plotLineWidth, plotLineWidths[0])
            plotLineWidths[1] = ta.getDimension(R.styleable.PlotView_plotLineWidth2, plotLineWidths[1])
            plotLineWidths[2] = ta.getDimension(R.styleable.PlotView_plotLineWidth3, plotLineWidths[2])

            pointSizes[0] = ta.getDimension(R.styleable.PlotView_pointSize, pointSizes[0])
            pointSizes[1] = ta.getDimension(R.styleable.PlotView_pointSize2, pointSizes[1])
            pointSizes[2] = ta.getDimension(R.styleable.PlotView_pointSize3, pointSizes[2])
            pointSizes[3] = ta.getDimension(R.styleable.PlotView_pointSize4, pointSizes[3])
            pointSizes[4] = ta.getDimension(R.styleable.PlotView_pointSize5, pointSizes[4])
            pointSizes[5] = ta.getDimension(R.styleable.PlotView_pointSize6, pointSizes[5])
            pointSizes[6] = ta.getDimension(R.styleable.PlotView_pointSize7, pointSizes[6])
            pointColors[0] = ta.getColor(R.styleable.PlotView_pointColor, pointColors[0])
            pointColors[1] = ta.getColor(R.styleable.PlotView_pointColor2, pointColors[1])
            pointColors[2] = ta.getColor(R.styleable.PlotView_pointColor3, pointColors[2])
            pointColors[3] = ta.getColor(R.styleable.PlotView_pointColor4, pointColors[3])
            pointColors[4] = ta.getColor(R.styleable.PlotView_pointColor5, pointColors[4])
            pointColors[5] = ta.getColor(R.styleable.PlotView_pointColor6, pointColors[5])
            pointColors[6] = ta.getColor(R.styleable.PlotView_pointColor7, pointColors[6])
            pointShapes[0] = enumToShape(ta.getInt(R.styleable.PlotView_pointShape, 0))
            pointShapes[1] = enumToShape(ta.getInt(R.styleable.PlotView_pointShape2, 0))
            pointShapes[2] = enumToShape(ta.getInt(R.styleable.PlotView_pointShape3, 0))
            pointShapes[3] = enumToShape(ta.getInt(R.styleable.PlotView_pointShape4, 0))
            pointShapes[4] = enumToShape(ta.getInt(R.styleable.PlotView_pointShape5, 0))
            pointShapes[5] = enumToShape(ta.getInt(R.styleable.PlotView_pointShape6, 0))
            pointShapes[6] = enumToShape(ta.getInt(R.styleable.PlotView_pointShape7, 0))

            markColor[0] = ta.getColor(R.styleable.PlotView_markColor, markColor[0])
            markLineWidth[0] = ta.getDimension(R.styleable.PlotView_markLineWidth, markLineWidth[0])
            markTextSize[0] = ta.getDimension(R.styleable.PlotView_markTextSize, markTextSize[0])
            markLabelColor[0] = ta.getColor(R.styleable.PlotView_markLabelColor, markLabelColor[0])

            markColor[1] = ta.getColor(R.styleable.PlotView_markColor2, markColor[1])
            markLineWidth[1] = ta.getDimension(R.styleable.PlotView_markLineWidth2, markLineWidth[1])
            markTextSize[1] = ta.getDimension(R.styleable.PlotView_markTextSize2, markTextSize[1])
            markLabelColor[1] = ta.getColor(R.styleable.PlotView_markLabelColor2, markLabelColor[1])

            markColor[2] = ta.getColor(R.styleable.PlotView_markColor3, markColor[2])
            markLineWidth[2] = ta.getDimension(R.styleable.PlotView_markLineWidth3, markLineWidth[2])
            markTextSize[2] = ta.getDimension(R.styleable.PlotView_markTextSize3, markTextSize[2])
            markLabelColor[2] = ta.getColor(R.styleable.PlotView_markLabelColor3, markLabelColor[2])

            markPaddingHorizontal = ta.getDimension(R.styleable.PlotView_markPaddingHorizontal, markPaddingHorizontal)
            markPaddingVertical = ta.getDimension(R.styleable.PlotView_markPaddingVertical, markPaddingVertical)
            markCornerRadius = ta.getDimension(R.styleable.PlotView_markCornerRadius, markCornerRadius)

            tickColor = ta.getColor(R.styleable.PlotView_tickColor, tickColor)
            tickLineWidth = ta.getDimension(R.styleable.PlotView_tickLineWidth, tickLineWidth)
            tickTextSize = ta.getDimension(R.styleable.PlotView_tickTextSize, tickTextSize)
            tickPaddingHorizontal = ta.getDimension(R.styleable.PlotView_tickPaddingHorizontal, tickPaddingHorizontal)
            tickPaddingVertical = ta.getDimension(R.styleable.PlotView_tickPaddingVertical, tickPaddingVertical)
            yTickLabelWidth = ta.getDimension(R.styleable.PlotView_yTickLabelWidth, yTickLabelWidth)
            yTickPosition = if (ta.getInt(R.styleable.PlotView_yTickPosition, 0) == 0)
                LabelAnchor.West
            else
                LabelAnchor.East

            title = ta.getString(R.styleable.PlotView_title)
            titleSize = ta.getDimension(R.styleable.PlotView_titleSize, titleSize)
            titleColor = ta.getColor(R.styleable.PlotView_titleColor, titleColor)

            frameStrokeWidth = ta.getDimension(R.styleable.PlotView_frameStrokeWidth, frameStrokeWidth)
            frameColor = ta.getColor(R.styleable.PlotView_frameColor, frameColor)
            frameColorOnTouch = ta.getColor(R.styleable.PlotView_frameColorOnTouch, frameColorOnTouch)
            frameCornerRadius = ta.getDimension(R.styleable.PlotView_frameCornerRadius, frameCornerRadius)

            allowTouchX = ta.getBoolean(R.styleable.PlotView_enableTouchX, allowTouchX)
            allowTouchY = ta.getBoolean(R.styleable.PlotView_enableTouchY, allowTouchY)

            touchDrawableId = ta.getResourceId(R.styleable.PlotView_touchDrawable, touchDrawableId)
            touchManualControlDrawableWidth = ta.getDimension(R.styleable.PlotView_touchDrawableWidth, touchManualControlDrawableWidth)
            touchDrawableBackgroundTint = ta.getColor(R.styleable.PlotView_touchDrawableBackgroundTint, touchDrawableBackgroundTint)
            ta.recycle()
        }

        titlePaint.color = titleColor
        titlePaint.isAntiAlias = true
        titlePaint.textSize = titleSize
        titlePaint.style = Paint.Style.FILL
        titlePaint.textAlign = Paint.Align.CENTER

        framePaint.color = frameColor
        framePaint.isAntiAlias = true
        framePaint.strokeWidth = frameStrokeWidth
        framePaint.style = Paint.Style.STROKE

        touchManualControlDrawable = TouchControlDrawable(context, frameColorOnTouch, touchDrawableBackgroundTint, touchDrawableId)
        touchManualControlDrawable.setSize(width = touchManualControlDrawableWidth)

        _xRange = PlotRange(allowTouchX).apply {
            rangeChangedListener = PlotRange.RangeChangedListener { _, minValue, maxValue ->
//                Log.v("Tuner", "PlotView: xrange changed: minValue=$minValue, maxValue=$maxValue")
                rawViewTransformation.setRawDataBounds(xMin = minValue, xMax = maxValue)
            }
        }
        _yRange = PlotRange(allowTouchY).apply {
            rangeChangedListener = PlotRange.RangeChangedListener { _, minValue, maxValue ->
//            Log.v("Tuner", "PlotView: yrange changed: minValue=$minValue, maxValue=$maxValue")
                rawViewTransformation.setRawDataBounds(yMin = minValue, yMax = maxValue)
            }
        }

        //rawPlotBounds.set(0f, -0.8f, 2.0f*PI.toFloat(), 0.8f)
        xTicks = AdaptiveTicks(rawViewTransformation, 'x', LabelAnchor.South,
            intArrayOf(tickColor),
            floatArrayOf(tickLineWidth), floatArrayOf(tickTextSize),
            tickPaddingHorizontal, tickPaddingVertical + frameStrokeWidth)
        yTicks = AdaptiveTicks(rawViewTransformation, 'y', yTickPosition,
            intArrayOf(tickColor),
            floatArrayOf(tickLineWidth), floatArrayOf(tickTextSize),
            tickPaddingHorizontal + frameStrokeWidth, tickPaddingVertical)

        xTicks.adaptiveTicksChangedListener = AdaptiveTicks.AdaptiveTicksChangedListener { _, ticks, bbChanged ->
            if (ticks.hasMarks && bbChanged)
                _xRange.setTicksRange(ticks.boundingBox.left, ticks.boundingBox.right)
            else if (!ticks.hasMarks)
                _xRange.setTicksRange(PlotRange.BOUND_UNDEFINED, PlotRange.BOUND_UNDEFINED)
            invalidate()
        }
        yTicks.adaptiveTicksChangedListener = AdaptiveTicks.AdaptiveTicksChangedListener { _, ticks, bbChanged ->
            if (ticks.hasMarks && bbChanged) {
//                Log.v("Tuner", "PlotView: yTicks.plotMarksChanges: ${ticks.boundingBox}")
                _yRange.setTicksRange(ticks.boundingBox.top, ticks.boundingBox.bottom)
            }
            else if (!ticks.hasMarks) {
                _yRange.setTicksRange(PlotRange.BOUND_UNDEFINED, PlotRange.BOUND_UNDEFINED)
            }
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rawViewTransformation.setRawDataBounds(xMin = _xRange.rangeMin, xMax = _xRange.rangeMax,
            yMin = _yRange.rangeMin, yMax = _yRange.rangeMax)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val totalPaddingLeft = paddingLeft + (if (enableExtraPadding) extraPaddingLeft else 0f)
        val totalPaddingRight = paddingRight + (if (enableExtraPadding) extraPaddingRight else 0f)

        var bottom = (height - paddingBottom).toFloat()
        if(xTicks.hasTicks)
            bottom -= tickTextSize + frameStrokeWidth + tickPaddingVertical
        var top = paddingTop.toFloat()
        if(title != null)
            top += 1.2f * titleSize

        val left = when (yTickPosition) {
            LabelAnchor.West -> totalPaddingLeft + yTickLabelWidth
            else -> totalPaddingLeft
        }
        val right = when (yTickPosition) {
            LabelAnchor.East -> (width - totalPaddingRight) - yTickLabelWidth
            else -> width - totalPaddingRight
        }
        rawViewTransformation.setViewBounds(
            left + frameStrokeWidth,top + frameStrokeWidth,
            right - frameStrokeWidth, bottom - frameStrokeWidth
        )

        xTicks.drawToCanvas(canvas)
        yTicks.drawToCanvas(canvas)

//        Log.v("StaticLayoutTest", "PlotView.onDraw: rawViewTransformation: ${rawViewTransformation.rawPlotBounds}, ${rawViewTransformation.viewPlotBounds}")
//        Log.v("StaticLayoutTest", "PlotView.onDraw: xRange values -> ${_xRange.rangeMin} -- ${_xRange.rangeMax}")
        canvas?.save()
        canvas?.clipRect(rawViewTransformation.viewPlotBounds)
        for (l in plotLines.values)
            l.drawToCanvas(canvas)

        for (p in plotPoints.values)
            p.drawToCanvas(canvas)

        markGroups.filterValues { !it.placeLabelsOutsideBoundsIfPossible }.forEach {
            it.value.drawToCanvas(canvas)
        }
        canvas?.restore()

        framePaint.color = if (_xRange.isTouchControlled || _yRange.isTouchControlled)
            frameColorOnTouch
        else
            frameColor
        //canvas?.drawRect(rawViewTransformation.viewPlotBounds, framePaint)
//        canvas?.drawRect(left + 0.5f * frameStrokeWidth, top + 0.5f * frameStrokeWidth,
//            right - 0.5f * frameStrokeWidth, bottom - 0.5f * frameStrokeWidth, framePaint)
        canvas?.drawRoundRect(left + 0.5f * frameStrokeWidth, top + 0.5f * frameStrokeWidth,
            right - 0.5f * frameStrokeWidth, bottom - 0.5f * frameStrokeWidth,
            frameCornerRadius, frameCornerRadius, framePaint)

        title?.let {
            canvas?.drawText(it,
                0.5f * (left + right),
                paddingTop + titleSize,
                titlePaint)
        }

        markGroups.filterValues { it.placeLabelsOutsideBoundsIfPossible }.forEach {
            it.value.drawToCanvas(canvas)
        }

        if (_xRange.isTouchControlled || _yRange.isTouchControlled) {
            touchManualControlDrawable.drawToCanvas(
                rawViewTransformation.viewPlotBounds.right + 1,
                rawViewTransformation.viewPlotBounds.top - 1,
                LabelAnchor.NorthEast,
                canvas
            )
        }
        rectangleAndPoint.drawToCanvas(canvas)
//        canvas?.drawLine(0f, 0f, getWidth().toFloat(), getHeight().toFloat(), paint)
//        drawArrow(canvas, 0.1f*getWidth(), 0.9f*getHeight(), 0.9f*getWidth(), 0.1f*getHeight(), paint)
    }

    override fun performClick(): Boolean {
        if (_xRange.isTouchControlled || _yRange.isTouchControlled) {
//            Log.v("Tuner", "PlotView: performClick switch touch control off")
            _xRange.setTouchRange(
                PlotRange.OFF,
                PlotRange.OFF,
                PlotRange.AnimationStrategy.Direct, 200L)
            _yRange.setTouchRange(
                PlotRange.OFF,
                PlotRange.OFF,
                PlotRange.AnimationStrategy.Direct, 200L)
            // the invalidate is needed for the case, that no animation runs, since the target range is already set
            ViewCompat.postInvalidateOnAnimation(this@PlotView)
        } else {
//            Log.v("Tuner", "PlotView: performClick switch touch control on")
            _xRange.setTouchRange(
                rawViewTransformation.rawPlotBounds.left,
                rawViewTransformation.rawPlotBounds.right,
                PlotRange.AnimationStrategy.Direct,
                0L
            )
            _yRange.setTouchRange(
                rawViewTransformation.rawPlotBounds.top,
                rawViewTransformation.rawPlotBounds.bottom,
                PlotRange.AnimationStrategy.Direct,
                0L
            )
            ViewCompat.postInvalidateOnAnimation(this@PlotView)
        }

        if (isSoundEffectsEnabled)
            playSoundEffect(android.view.SoundEffectConstants.CLICK)

        return super.performClick()
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        return gestureDetector.onTouchEvent(event) || scaleGestureDetector.onTouchEvent(event)
        if (event == null)
            return super.onTouchEvent(null)
        val s = scaleGestureDetector.onTouchEvent(event)
        val g = gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event) || s || g
//        return (scaleGestureDetector.onTouchEvent(event)
//                || gestureDetector.onTouchEvent(event)
//                || super.onTouchEvent(event))
//                return (scaleGestureDetector.onTouchEvent(event)
//                || super.onTouchEvent(event))

    }
    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable("super state", super.onSaveInstanceState())

        val plotState = SavedState(_xRange.getSavedState(), _yRange.getSavedState())
        bundle.putParcelable("plot state", plotState)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
//        Log.v("Tuner", "PlotView.onRestoreInstanceState")
        val superState = if (state is Bundle) {
            state.getParcelable<SavedState>("plot state")?.let { plotState ->
                _xRange.restore(plotState.xRange)
                _yRange.restore(plotState.yRange)
            }
            state.getParcelable("super state")
        } else {
            state
        }
        super.onRestoreInstanceState(superState)
    }

    private fun getPlotLine(tag: Long): PlotLine {
        plotLines[tag]?.let {
            return it
        }

        val line = PlotLine(rawViewTransformation, plotLineColors, plotLineWidths)
        plotLines[tag] = line

        line.plotLineChangedListener = PlotLine.PlotLineChangedListener { _, hasNewBoundingBox ->
//            Log.v("Tuner", "PlotView.getPlotLine: Listener: newBoundingBox=$hasNewBoundingBox")
            if (hasNewBoundingBox)
                computeBoundingBox()
            invalidate()
        }
        return line
    }

    private fun getPlotPoints(tag: Long): PlotPoints {
        plotPoints[tag]?.let {
            return it
        }

        val points = PlotPoints(rawViewTransformation, pointColors, pointSizes, pointShapes)
        plotPoints[tag] = points

        points.plotPointsChangedListener = PlotPoints.PlotPointsChangedListener { _, hasNewBoundingBox ->
            if (hasNewBoundingBox)
                computeBoundingBox()
            invalidate()
        }
        return points
    }

    private fun getPlotMarks(tag: Long): PlotMarks {
        markGroups[tag]?.let {
            return it
        }

        val marks = PlotMarks(rawViewTransformation, markColor, markLabelColor,
            markLineWidth, markTextSize, disableLabelBackground = false, markPaddingHorizontal,
            markPaddingVertical, markCornerRadius)
        markGroups[tag] = marks

        marks.plotMarksChangedListener = PlotMarks.PlotMarksChangedListener { _, _ ->
            invalidate()
        }

        return marks
    }

    private fun computeBoundingBox() {
        boundingBoxOld.set(boundingBox)
        // set inverse max values
        boundingBox.left = Float.POSITIVE_INFINITY
        boundingBox.top = Float.POSITIVE_INFINITY
        boundingBox.right = Float.NEGATIVE_INFINITY
        boundingBox.bottom = Float.NEGATIVE_INFINITY

        plotLines.values.filter { !it.emptyLine }.forEach { line ->
            boundingBox.left = min(boundingBox.left, line.boundingBox.left)
            boundingBox.top = min(boundingBox.top, line.boundingBox.top)
            boundingBox.right = max(boundingBox.right, line.boundingBox.right)
            boundingBox.bottom = max(boundingBox.bottom, line.boundingBox.bottom)
        }

        plotPoints.values.filter { it.numPoints > 0 }.forEach { point ->
            boundingBox.left = min(boundingBox.left, point.boundingBox.left)
            boundingBox.top = min(boundingBox.top, point.boundingBox.top)
            boundingBox.right = max(boundingBox.right, point.boundingBox.right)
            boundingBox.bottom = max(boundingBox.bottom, point.boundingBox.bottom)
        }
//        Log.v("Tuner", "PlotView.computeBoundingBox: boundingBox=$boundingBox")
        _xRange.setDataRange(
            if (boundingBox.left == Float.POSITIVE_INFINITY) PlotRange.BOUND_UNDEFINED else boundingBox.left,
            if (boundingBox.right == Float.NEGATIVE_INFINITY) PlotRange.BOUND_UNDEFINED else boundingBox.right
        )
        _yRange.setDataRange(
            if (boundingBox.top == Float.POSITIVE_INFINITY) PlotRange.BOUND_UNDEFINED else boundingBox.top,
            if (boundingBox.bottom == Float.NEGATIVE_INFINITY) PlotRange.BOUND_UNDEFINED else boundingBox.bottom
        )

        if (!boundingBox.contentEquals(boundingBoxOld))
            invalidate()
    }

    fun removePlotLine(tag: Long) {
        plotLines.remove(tag)?.unregisterFromPlotTransformation()
        invalidate()
    }

    fun removePlotPoints(tag: Long) {
        plotPoints.remove(tag)?.unregisterFromPlotTransformation()
        invalidate()
    }

    fun removePlotMarks(tag: Long?) {
        if (tag == null) {
            for (t in markGroups.keys)
                markGroups.remove(t)?.unregisterFromPlotTransformation()
        } else {
            markGroups.remove(tag)?.unregisterFromPlotTransformation()
        }
        invalidate()
    }

    fun setLineStyle(styleIndex: Int, tag: Long = 0L) {
        plotLines[tag]?.setStyleIndex(styleIndex)
    }

    fun setPointStyle(styleIndex: Int, tag: Long = 0L) {
        plotPoints[tag]?.setStyleIndex(styleIndex)
    }

    fun setPointOffset(offsetX: Float, offsetY: Float, tag: Long = 0L) {
        plotPoints[tag]?.setOffset(offsetX, offsetY)
    }

    fun setPointVisible(isVisible: Boolean, tag: Long = 0L) {
        plotPoints[tag]?.setVisible(isVisible)
    }

    fun setMarkStyle(styleIndex: Int, tag: Long = 0L) {
        markGroups[tag]?.setStyleIndex(styleIndex)
    }

    /// Set x-range.
    /**
     * @param minValue Minimum value of x-range (or PlotView.autoLimit for determining the limit based on the plot data).
     * @param maxValue Minimum value of x-range (or PlotView.autoLimit for determining the limit based on the plot data).
     * @param animationDuration Duration for animating to the new range (0L for no animation)
     */
    fun xRange(minValue : Float, maxValue : Float, animationDuration: Long = 0L) {
        _xRange.setRange(minValue, maxValue,
            PlotRange.AnimationStrategy.ExtendShrink, animationDuration)
    }

    /// Set y-range.
    /**
     * @param minValue Minimum value of y-range (or PlotView.autoLimit for determining the limit based on the plot data).
     * @param maxValue Minimum value of y-range (or PlotView.autoLimit for determining the limit based on the plot data).
     * @param animationDuration Duration for animating to the new range (0L for no animation)
     */
    fun yRange(minValue : Float, maxValue : Float, animationDuration: Long = 0L) {
        _yRange.setRange(minValue, maxValue,
            PlotRange.AnimationStrategy.ExtendShrink, animationDuration)
    }

    fun setXTouchLimits(minValue: Float, maxValue: Float, animationDuration: Long = 0L) {
        _xRange.setTouchLimits(minValue, maxValue,
            PlotRange.AnimationStrategy.Direct, animationDuration)
    }

    fun setYTouchLimits(minValue: Float, maxValue: Float, animationDuration: Long = 0L) {
        _yRange.setTouchLimits(minValue, maxValue,
            PlotRange.AnimationStrategy.Direct, animationDuration)
    }

    /** Plot equidistant values (Taking a FloatArray).
     * @param yValues Array with equidistant y-values.
     * @param tag Tag to identify the line.
     * @param indexBegin First index in yValues to use (included).
     * @param indexEnd End index in yValues to use (excluded).
     */
    fun plot(
        yValues : FloatArray, tag: Long = 0L,
        indexBegin: Int = 0, indexEnd: Int = yValues.size
    ) {
        plot(yValues.asPlotViewArray(), tag, indexBegin, indexEnd)
    }

    /** Plot equidistant values (Taking an ArrayList<Float>).
     * @param yValues Array with equidistant y-values.
     * @param tag Tag to identify the line.
     * @param indexBegin First index in yValues to use (included).
     * @param indexEnd End index in yValues to use (excluded).
     */
    fun plot(
        yValues : ArrayList<Float>, tag: Long = 0L,
        indexBegin: Int = 0, indexEnd: Int = yValues.size
    ) {
        plot(yValues.asPlotViewArray(), tag, indexBegin, indexEnd)
    }

    /**  Plot values (Taking FloatArrays).
     * @param xValues Array with equidistant x-values.
     * @param yValues Array with equidistant y-values.
     * @param tag Line identifier. If method is called with a same tag as before
     *   we will overwrite the line.
     * @param indexBegin First index in x/yValues to use (included).
     * @param indexEnd End index in x/yValues to use (excluded).
     */
    fun plot(
        xValues : FloatArray, yValues : FloatArray,
        tag: Long = 0L,
        indexBegin: Int = 0, indexEnd: Int = min(xValues.size, yValues.size)
    ) {
        plot(xValues.asPlotViewArray(), yValues.asPlotViewArray(), tag, indexBegin, indexEnd)
    }

    /** Plot values (Taking ArrayLists<Float>).
     * @param xValues Array with equidistant x-values.
     * @param yValues Array with equidistant y-values.
     * @param tag Line identifier. If method is called with a same tag as before
     *   we will overwrite the line.
     * @param indexBegin First index in x/yValues to use (included).
     * @param indexEnd End index in x/yValues to use (excluded).
     */
    fun plot(
        xValues : ArrayList<Float>, yValues : ArrayList<Float>,
        tag: Long = 0L,
        indexBegin: Int = 0, indexEnd: Int = min(xValues.size, yValues.size)
    ) {
        plot(xValues.asPlotViewArray(), yValues.asPlotViewArray(), tag, indexBegin, indexEnd)
    }

    /** Set marks.
     * @param xPositions List of xPositions for all marks or null to draw lines along the xPosition.
     * @param yPositions List of yPositions for all marks or null to draw lines along the yPosition.
     * @param tag Identifier for the mark group. If the identifier was used before
     *   we will overwrite these marks.
     * @param styleIndex Style index which should be used for the marks.
     * @param indexBegin First index in the positions-arrays to be used (included).
     * @param indexEnd End index in the positions-arrays to be used (excluded).
     * @param anchors An anchor for each mark label, or null for using the defaults (centered)
     * @param backgroundSizeType Defines if the background for the labels should all be
     *   the same size or if they should be individually fitted to each label size.
     * @param maxLabelBounds Functor to compute maximum label bounds based on the paint.
     *   This allows optimizations such that only visible marks are actually created.
     *   If null, we will compute the max bounds internally, but this will be more expensive.
     * @param labelCreator Define how to plot the mark label. The lambda will provide the mark index
     *   (the index in xPositions or yPositions), the coordinates (or null for line
     *   marks) and a lot of default values for creating a label. If null is returned by the lambda,
     *   no label will be drawn.
     */
    fun setMarks(
        xPositions: FloatArray?, yPositions: FloatArray?,
        tag: Long,
        styleIndex: Int = 0,
        indexBegin: Int = 0,
        indexEnd: Int = max(xPositions?.size ?: 0, yPositions?.size ?: 0),
        anchors: Array<LabelAnchor>? = null,
        backgroundSizeType: MarkLabelBackgroundSize = MarkLabelBackgroundSize.FitIndividually,
        placeLabelsOutsideBoundsIfPossible: Boolean = false,
        maxLabelBounds: ((TextPaint) -> Label.LabelSetBounds)? = null,
        labelCreator: ((Int, Float?, Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity:LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius: Float) -> Label?)? = null
    ) {
                 //format: ((Int, Float?, Float?) -> CharSequence?)? = null) {
        val marks = getPlotMarks(tag)
        marks.setMarks(xPositions, yPositions, indexBegin, indexEnd, styleIndex, anchors, backgroundSizeType,
            placeLabelsOutsideBoundsIfPossible = placeLabelsOutsideBoundsIfPossible,
            maxLabelBounds, labelCreator
        )
    }

    /** Helper to create a label creator based on note name scales .*/
    fun musicalNoteLabelCreator(firstNoteIndex: Int, scale: NoteNameScale, notePrintOptions: MusicalNotePrintOptions)
            : ((Int, Float?, Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity:LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius: Float) -> Label?) {
        return { index: Int, _: Float?, _: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity: LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius:Float ->
            val noteIndex = index + firstNoteIndex
            val note = scale.getNoteOfIndex(noteIndex)
            MusicalNoteLabel(note, textPaint, noteNamePrinter, backgroundPaint, cornerRadius, gravity, notePrintOptions, true, paddingHorizontal, paddingHorizontal, paddingVertical, paddingVertical)
        }
    }

    /** Helper to create a label creator for MusicalNoteLabels .*/
    fun musicalNoteLabelCreator(note: MusicalNote, notePrintOptions: MusicalNotePrintOptions)
            : ((Int, Float?, Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity:LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius: Float) -> Label) {
        return { _: Int, _: Float?, _: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity: LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius:Float ->
            MusicalNoteLabel(note, textPaint, noteNamePrinter, backgroundPaint, cornerRadius, gravity, notePrintOptions, true, paddingHorizontal, paddingHorizontal, paddingVertical, paddingVertical)
        }
    }

    /** Helper for creating string StringLabels. */
    fun stringLabelCreator(string: String)
            : ((Int, Float?, Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity:LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius: Float) -> Label) {
        return { _: Int, _: Float?, _: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity: LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius:Float ->
            StringLabel(string, textPaint, backgroundPaint, cornerRadius, gravity, paddingHorizontal, paddingHorizontal, paddingVertical, paddingVertical)
        }
    }

    /** Convenience method to set a single string mark.
     * @param xPosition x-position of mark.
     * @param yPosition y-position of mark.
     * @param label Label of mark or null if the mark should have no label (i.e. if lines are drawn)
     * @param tag Identifier for the mark group. If the identifier was used before
     *   we well overwrite these marks.
     * @param anchor Anchor which defines how to align the label relative to the mark position.
     * @param style Mark style to use (0 -> use mark-xml-attributes without prefix,
     *   1 -> use the xml-attributes with prefix 2).
     */
    fun setMark(xPosition: Float, yPosition: Float, label: String?, tag: Long,
                anchor: LabelAnchor = LabelAnchor.Center,
                placeLabelsOutsideBoundsIfPossible: Boolean = false,
                style: Int = 0
    ) {
        setMarks(floatArrayOf(xPosition), floatArrayOf(yPosition), tag, style,
            anchors = arrayOf(anchor),
            backgroundSizeType = MarkLabelBackgroundSize.FitIndividually,
            placeLabelsOutsideBoundsIfPossible = placeLabelsOutsideBoundsIfPossible,
            maxLabelBounds = null,
            labelCreator = if (label == null) null else stringLabelCreator(label)
        )
    }

    /** Convenience method to set a single note mark.
     * @param xPosition x-position of mark.
     * @param yPosition y-position of mark.
     * @param note Note which will be used for drawing the label.
     * @param notePrintOptions Options for printint the note (prefer flat/sharp, ...)
     * @param tag Identifier for the mark group. If the identifier was used before
     *   we well overwrite these marks.
     * @param anchor Anchor which defines how to align the label relative to the mark position.
     * @param style Mark style to use (0 -> use mark-xml-attributes without prefix,
     *   1 -> use the xml-attributes with prefix 2).
     */
    fun setMark(xPosition: Float, yPosition: Float, note: MusicalNote,
                notePrintOptions: MusicalNotePrintOptions,
                tag: Long,
                anchor: LabelAnchor = LabelAnchor.Center,
                placeLabelsOutsideBoundsIfPossible: Boolean = false,
                style: Int = 0
    ) {
        setMarks(floatArrayOf(xPosition), floatArrayOf(yPosition), tag, style,
            anchors = arrayOf(anchor),
            backgroundSizeType = MarkLabelBackgroundSize.FitIndividually,
            placeLabelsOutsideBoundsIfPossible = placeLabelsOutsideBoundsIfPossible,
            maxLabelBounds = null,
            labelCreator = musicalNoteLabelCreator(note, notePrintOptions)
        )
    }

    /// Convenience method to set a single string mark with a vertical line.
    /**
     * @param xPosition x-position of mark.
     * @param label Label of mark or null if the mark should have no label (i.e. if lines are drawn)
     * @param tag Identifier for the mark group. If the identifier was used before
     *   we well overwrite these marks.
     * @param anchor Anchor which defines how to align the label relative to the mark position.
     * @param style Mark style to use (0 -> use mark-xml-attributes without prefix,
     *   1 -> use the xml-attributes with prefix 2).
     */
    fun setXMark(xPosition: Float, label: String?, tag: Long,
                 anchor: LabelAnchor = LabelAnchor.Center, style: Int = 0,
                 placeLabelsOutsideBoundsIfPossible: Boolean = false
    ) {
        setMarks(floatArrayOf(xPosition), null, tag, style,
            anchors = arrayOf(anchor),
            backgroundSizeType = MarkLabelBackgroundSize.FitIndividually,
            placeLabelsOutsideBoundsIfPossible = placeLabelsOutsideBoundsIfPossible,
            maxLabelBounds = null,
            labelCreator = if (label == null) null else stringLabelCreator(label)
        )
    }

    /** Convenience method to set a single note mark with a vertical line.
     * @param xPosition x-position of mark.
     * @param note Note which will be used for drawing the label.
     * @param notePrintOptions Options for printint the note (prefer flat/sharp, ...)
     * @param tag Identifier for the mark group. If the identifier was used before
     *   we well overwrite these marks.
     * @param anchor Anchor which defines how to align the label relative to the mark position.
     * @param style Mark style to use (0 -> use mark-xml-attributes without prefix,
     *   1 -> use the xml-attributes with prefix 2).
     */
    fun setXMark(xPosition: Float, note: MusicalNote,
                 notePrintOptions: MusicalNotePrintOptions,
                 tag: Long,
                 anchor: LabelAnchor = LabelAnchor.Center, style: Int = 0,
                 placeLabelsOutsideBoundsIfPossible: Boolean = false
    ) {
        setMarks(floatArrayOf(xPosition), null, tag, style,
            anchors = arrayOf(anchor),
            backgroundSizeType = MarkLabelBackgroundSize.FitIndividually,
            placeLabelsOutsideBoundsIfPossible = placeLabelsOutsideBoundsIfPossible,
            maxLabelBounds = null,
            labelCreator = musicalNoteLabelCreator(note, notePrintOptions)
        )
    }

    /// Convenience method to set a single string mark with a horizontal line.
    /**
     * @param yPosition y-position of mark.
     * @param label Label of mark or null if the mark should have no label (i.e. if lines are drawn)
     * @param tag Identifier for the mark group. If the identifier was used before
     *   we well overwrite these marks.
     * @param anchor Anchor which defines how to align the label relative to the mark position.
     * @param style Mark style to use (0 -> use mark-xml-attributes without prefix,
     *   1 -> use the xml-attributes with prefix 2).
     */
    fun setYMark(yPosition: Float, label: String?, tag: Long,
                 anchor: LabelAnchor = LabelAnchor.Center, style: Int = 0,
                 placeLabelsOutsideBoundsIfPossible: Boolean = false
    ) {
        setMarks(null, floatArrayOf(yPosition), tag, style,
            anchors = arrayOf(anchor),
            backgroundSizeType = MarkLabelBackgroundSize.FitIndividually,
            placeLabelsOutsideBoundsIfPossible = placeLabelsOutsideBoundsIfPossible,
            maxLabelBounds = null,
            labelCreator = if (label == null) null else stringLabelCreator(label)
        )
    }

    /** Convenience method to set a single note mark with a horizontal line.
     * @param yPosition y-position of mark.
     * @param note Note which will be used for drawing the label.
     * @param notePrintOptions Options for printint the note (prefer flat/sharp, ...)
     * @param tag Identifier for the mark group. If the identifier was used before
     *   we well overwrite these marks.
     * @param anchor Anchor which defines how to align the label relative to the mark position.
     * @param style Mark style to use (0 -> use mark-xml-attributes without prefix,
     *   1 -> use the xml-attributes with prefix 2).
     */
    fun setYMark(yPosition: Float, note: MusicalNote,
                 notePrintOptions: MusicalNotePrintOptions,
                 tag: Long,
                 anchor: LabelAnchor = LabelAnchor.Center, style: Int = 0,
                 placeLabelsOutsideBoundsIfPossible: Boolean = false
    ) {
        setMarks(null, floatArrayOf(yPosition), tag, style,
            anchors = arrayOf(anchor),
            backgroundSizeType = MarkLabelBackgroundSize.FitIndividually,
            placeLabelsOutsideBoundsIfPossible = placeLabelsOutsideBoundsIfPossible,
            maxLabelBounds = null,
            labelCreator = musicalNoteLabelCreator(note, notePrintOptions)
        )
    }

    /// Set points which should be drawn as filled circles.
    /**
     * @param value Array with point coordinates (of form x0, y0, x1, y1, ...)
     * @param tag Identifier of points. If this method is called again with the same tag
     *   we will overwrite these points.
     */
    fun setPoints(value: FloatArray?, tag: Long = 0L) {
        // Log.v("Tuner", "PlotView.setPoints")
        val points = getPlotPoints(tag)
        points.setPoints(value)
        invalidate()
    }

    fun getPointSize(tag: Long = 0L): Float {
        return plotPoints[tag]?.pointSize ?: 0f
    }

    /** Remove the x-ticks. */
    fun clearXTicks() {
        xTicks.clear()
        invalidate()
    }

    /** Set x-ticks.
     * @param values Values of x-ticks to be drawn.
     * @param indexBegin First index in values to be used (included).
     * @param indexEnd End index in values to be used (excluded).
     * @param maxLabelBounds Functor to compute maximum label bounds based on the paint.
     *   This allows optimizations such that only visible marks are actually created.
     *   If null, we will compute the max bounds internally, but this will be more expensive.
     * @param labelCreator Define how to plot the mark label. The lambda will provide the mark index
     *   (the index in values), the x-coordinate and a lot of default values for creating a
     *   label. If null is returned by the lambda, no label will be drawn.
     */
    fun addXTicksLevel(
        values: FloatArray?,
        indexBegin: Int = 0,
        indexEnd: Int = values?.size ?: 0,
        maxLabelBounds: ((TextPaint) -> Label.LabelSetBounds)?,
        labelCreator: ((index: Int, xPos: Float?, yPos: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity:LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius: Float) -> Label)?) {

        if (values == null) {
            return
        } else {
            xTicks.addTicksLevel(
                values,
                indexBegin, indexEnd,
                maxLabelBounds = maxLabelBounds,
                labelCreator = labelCreator
            )
        }
        invalidate()
    }


    fun addXTicksLevel(
        values: FloatArray?,
        indexBegin: Int = 0,
        indexEnd: Int = values?.size ?: 0,
        format: ((Int, Float) -> String)?) {
        if (values == null)
            return
        val creator = if (format == null) {
            null
        } else {
            { index: Int, xPosition: Float?, _: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity: LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius: Float ->
                val string = format(index, xPosition ?: 0f)
                StringLabel(string, textPaint, backgroundPaint, cornerRadius, gravity, paddingHorizontal, paddingHorizontal, paddingVertical, paddingVertical)
            }
        }
        addXTicksLevel(values, indexBegin, indexEnd, null, creator)
    }

    fun addXTicksLevel(
        values: FloatArray?,
        indexBegin: Int = 0,
        indexEnd: Int = values?.size ?: 0,
        noteNameScale: NoteNameScale,
        noteIndexBegin: Int,
        notePrintOptions: MusicalNotePrintOptions) {
        if (values == null)
            return
        val octaveBegin = noteNameScale.getNoteOfIndex(noteIndexBegin).octave
        val octaveEnd = noteNameScale.getNoteOfIndex(noteIndexBegin + values.size - 1).octave + 1
        val labelBounds = { paint: TextPaint ->
            MusicalNoteLabel.getLabelSetBounds(
                noteNameScale.notes,
                octaveBegin,
                octaveEnd,
                paint,
                noteNamePrinter,
                notePrintOptions,
                true
            )
        }
        val labelCreator = { index: Int, _: Float?, _: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity: LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius:Float ->
            val note = noteNameScale.getNoteOfIndex(index + noteIndexBegin)
            MusicalNoteLabel(note, textPaint, noteNamePrinter, backgroundPaint, cornerRadius, gravity, notePrintOptions, true, paddingHorizontal, paddingHorizontal, paddingVertical, paddingVertical)
        }
        addXTicksLevel(values, indexBegin, indexEnd, labelBounds, labelCreator)
    }

    /** Remove the x-ticks. */
    fun clearYTicks() {
        yTicks.clear()
        invalidate()
    }

    /** Set y-ticks.
     * @param values Values of y-ticks to be drawn.
     * @param indexBegin First index in values to be used (included).
     * @param indexEnd End index in values to be used (excluded).
     * @param maxLabelBounds Functor to compute maximum label bounds based on the paint.
     *   This allows optimizations such that only visible marks are actually created.
     *   If null, we will compute the max bounds internally, but this will be more expensive.
     * @param labelCreator Define how to plot the mark label. The lambda will provide the mark index
     *   (the index in yPositions), the y-coordinate and a lot of default values for creating a
     *   label. If null is returned by the lambda, no label will be drawn.
     */
    fun addYTicksLevel(
        values: FloatArray?,
        indexBegin: Int = 0, indexEnd: Int = values?.size ?: 0,
        maxLabelBounds: ((TextPaint) -> Label.LabelSetBounds)?,
        labelCreator: ((index: Int, xPos: Float?, yPos: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity:LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius: Float) -> Label)?) {
//        Log.v("Tuner", "PlotView.setYTicks: numValues = ${value?.size}")

        if (values == null) {
            return
        } else {
            yTicks.addTicksLevel(
                values,
                indexBegin, indexEnd,
                maxLabelBounds = maxLabelBounds,
                labelCreator = labelCreator
            )
        }
        invalidate()
    }

    fun addYTicksLevel(
        values: FloatArray?,
        indexBegin: Int = 0, indexEnd: Int = values?.size ?: 0,
        format: ((Int, Float) -> String)?) {
        if (values == null)
            return
        val creator = if (format == null) {
            null
        } else {
            { index: Int, _: Float?, yPosition: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity: LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius: Float ->
                val string = format(index, yPosition ?: 0f)
                StringLabel(string, textPaint, backgroundPaint, cornerRadius, gravity, paddingHorizontal, paddingHorizontal, paddingVertical, paddingVertical)
            }
        }
        addYTicksLevel(values, indexBegin, indexEnd, null, creator)
    }

    /** Set y-ticks with note labels.
     * @param values Values where the note labels should be plotted.
     * @param indexBegin First value in values to be plotted (included).
     * @param indexEnd End value in values to be plotted (excluded).
     * @param noteNameScale Note name scale, which transfers indices to note names.
     * @param noteIndexBegin The note index in noteNameScale for values[0].
     * @param notePrintOptions How to print notes.
     */
    fun addYTicksLevel(
        values: FloatArray?,
        indexBegin: Int = 0, indexEnd: Int = values?.size ?: 0,
        noteNameScale: NoteNameScale,
        noteIndexBegin: Int,
        notePrintOptions: MusicalNotePrintOptions
    ) {
        if (values == null)
            return
        val octaveBegin = noteNameScale.getNoteOfIndex(noteIndexBegin).octave
        val octaveEnd = noteNameScale.getNoteOfIndex(noteIndexBegin + values.size - 1).octave + 1
        val labelBounds = { paint: TextPaint ->
            MusicalNoteLabel.getLabelSetBounds(
                noteNameScale.notes,
                octaveBegin,
                octaveEnd,
                paint,
                noteNamePrinter,
                notePrintOptions,
                true
            )
        }
        val labelCreator = { index: Int, _: Float?, _: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity: LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius:Float ->
            val note = noteNameScale.getNoteOfIndex(index + noteIndexBegin)
            MusicalNoteLabel(note, textPaint, noteNamePrinter, backgroundPaint, cornerRadius, gravity, notePrintOptions, true, paddingHorizontal, paddingHorizontal, paddingVertical, paddingVertical)
        }
        addYTicksLevel(values, indexBegin, indexEnd, labelBounds, labelCreator)
    }

    fun addYTicksLevel(
        values: FloatArray?,
        indexBegin: Int = 0, indexEnd: Int = values?.size ?: 0,
        noteNameScale: NoteNameScale,
        noteIndices: IntArray?,
        notePrintOptions: MusicalNotePrintOptions
    ) {
        if (values == null || noteIndices == null)
            return
        val octaveBegin = if (noteIndices.isNotEmpty())
            noteNameScale.getNoteOfIndex(noteIndices[0]).octave
        else
            0
        val octaveEnd = if (noteIndices.isNotEmpty())
            noteNameScale.getNoteOfIndex(noteIndices.last()).octave + 1
        else
            1
        val labelBounds = { paint: TextPaint ->
            MusicalNoteLabel.getLabelSetBounds(
                noteNameScale.notes,
                octaveBegin,
                octaveEnd,
                paint,
                noteNamePrinter,
                notePrintOptions,
                true
            )
        }
        val labelCreator = { index: Int, _: Float?, _: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity: LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius:Float ->
            val note = noteNameScale.getNoteOfIndex(noteIndices[index])
            MusicalNoteLabel(note, textPaint, noteNamePrinter, backgroundPaint, cornerRadius, gravity, notePrintOptions, true, paddingHorizontal, paddingHorizontal, paddingVertical, paddingVertical)
        }
        addYTicksLevel(values, indexBegin, indexEnd, labelBounds, labelCreator)
    }


    /** Plot equidistant values (general version with PlotViewArray).
     * @param yValues Array with equidistant y-values.
     * @param tag Identifier for the line. If this method is called again with the same tag
     *   we will overwrite the line.
     * @param indexBegin First index in yValues to use (included).
     * @param indexEnd End index in yValues to use (excluded).
     */
    private fun plot(
        yValues : PlotViewArray, tag: Long = 0L,
        indexBegin: Int = 0, indexEnd: Int = yValues.size
    ) {
        val line = getPlotLine(tag)
        line.setLine(yValues, indexBegin, indexEnd)
        invalidate()
    }

    /** Plot values (general version take in PlotViewArrays).
     * @param xValues Array with equidistant x-values.
     * @param yValues Array with equidistant y-values.
     * @param tag Identifier for the line. If this method is called again with the same tag
     *   we will overwrite the line.
     * @param indexBegin First index in x/yValues to use (included).
     * @param indexEnd End index in x/yValues to use (excluded).
     */
    private fun plot(
        xValues : PlotViewArray, yValues : PlotViewArray,
        tag: Long = 0L,
        indexBegin: Int = 0, indexEnd: Int = min(xValues.size, yValues.size)
    ) {
        val line = getPlotLine(tag)
        line.setLine(xValues, yValues, indexBegin, indexEnd)
        invalidate()
    }


    private fun enumToShape(number: Int) = when(number) {
        0 -> PointShapes.Circle
        1 -> PointShapes.TriangleUp
        2 -> PointShapes.TriangleDown
        else -> throw RuntimeException("Unknown shape index: $number")
    }
//    fun drawArrow(canvas: Canvas?, startX : Float, startY : Float, endX : Float, endY : Float, paint : Paint) {
//        val lineLength = sqrt((endX - startX).pow(2) + (endY - startY).pow(2))
//        val dx = (endX-startX) / lineLength
//        val dy = (endY-startY) / lineLength
//        val sW = plotLineWidth
//
//        val arrowLength = 5 * sW
//        val strokeEndX = endX - dx*arrowLength
//        val strokeEndY = endY - dy*arrowLength
//        canvas?.drawLine(startX, startY, strokeEndX, strokeEndY, paint)
//        paint.style = Paint.Style.FILL
//
//        arrowPath.reset()
//        val arrowWidth = 3 * sW
//        arrowPath.moveTo(strokeEndX+0.5f*dy*arrowWidth, strokeEndY-0.5f*dx*arrowWidth)
//        arrowPath.lineTo(endX, endY)
//        arrowPath.lineTo(strokeEndX-0.5f*dy*arrowWidth, strokeEndY+0.5f*dx*arrowWidth)
//        arrowPath.close()
//        canvas?.drawPath(arrowPath, paint)
//    }
}
