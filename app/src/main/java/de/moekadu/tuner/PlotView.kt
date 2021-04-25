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

package de.moekadu.tuner

import android.animation.FloatArrayEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import kotlinx.parcelize.Parcelize
import kotlin.math.*

/// Interface for an array of floats internally used by the PlotView
private interface PlotViewArray {
    /// Get value at specific index
    operator fun get(index: Int): Float
    /// Check if size is zero
    fun isEmpty(): Boolean
    /// Return last element or throw
    fun last(): Float
    /// Size of array
    val size: Int
    /// Binary search within the array
    /**
     * @param element Element to search
     * @param fromIndex Start search at this index
     * @param toIndex Stop search at this index (toIndex is excluded)
     * @return Index of element or inverted insertion point if not in array (-insertion_point-1)
     */
    fun binarySearch(element: Float, fromIndex: Int = 0, toIndex: Int = size) : Int
}

/// PlotViewArray containing a FloatArray
private class FloatArrayPlotViewArray(private val a: FloatArray) : PlotViewArray {
    override fun get(index: Int) = a[index]
    override fun isEmpty() = a.isEmpty()
    override fun last() = a.last()
    override val size: Int = a.size
    override fun binarySearch(element: Float, fromIndex: Int, toIndex: Int) = a.binarySearch(element, fromIndex, toIndex)
}

/// PlotViewArray containing a ArrayList of floats
private class ArrayListPlotViewArray(private val a: ArrayList<Float>) : PlotViewArray {
    override fun get(index: Int) = a[index]
    override fun isEmpty() = a.isEmpty()
    override fun last() = a.last()
    override val size: Int = a.size
    override fun binarySearch(element: Float, fromIndex: Int, toIndex: Int) = a.binarySearch(element, fromIndex, toIndex)
}

/// Convert FloatArray to PlotViewArray
private fun FloatArray.asPlotViewArray(): PlotViewArray = FloatArrayPlotViewArray(this)
/// Convert ArrayList of floats to PlotViewArray
private fun ArrayList<Float>.asPlotViewArray(): PlotViewArray = ArrayListPlotViewArray(this)

/// PlotView class
class PlotView(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    : View(context, attrs, defStyleAttr)
{
    companion object {
        const val NO_REDRAW = Long.MAX_VALUE
        const val TAKE_ALL = -1
        const val TYPE_MIN = 0
        const val TYPE_MAX = 1
        const val autoLimit = Float.MAX_VALUE
    }

    /// This is just for making sure, that bounds initialized and tells if "plot" has been called at least once since the creation of the class.
    private var plotCalled = false

    /// Color of plot line.
    private var plotLineColor = Color.BLACK
    /// Width of plot line.
    private var plotLineWidth = 5f

    /// Paint used for plotting line and title
    private val paint = Paint()
    private val arrowPath = Path()
    /// Path with plot line of coordinates as given during plot
    private val rawPlotLine = Path()
    /// Plot line after transforming it to the canvas
    private val transformedPlotLine = Path()
    /// Transformation matrix for transforming from raw coordinates to canvas coordinates
    private val plotTransformationMatrix = Matrix()
    /// Plot bounds for coordinates in original space.
    private val rawPlotBounds = RectF()
    /// Plot bounds in canvas coordinates.
    private val viewPlotBounds = RectF()

    /// x-range to plot (or autoLimit for determining the limits based on the plot data).
    private val xRange = FloatArray(2) { autoLimit }
    /// y-range to plot (or autoLimit for determining the limits based on the plot data).
    private val yRange = FloatArray(2) { autoLimit }

    /// Evaluator for x-range needed for x-range animation.
    private val xRangeEvaluator = FloatArrayEvaluator(xRange)
    /// Evaluator for y-range needed for y-range animation.
    private val yRangeEvaluator = FloatArrayEvaluator(yRange)
    /// Animator for animating between different x-ranges.
    private val xRangeAnimator = ValueAnimator.ofObject(xRangeEvaluator, floatArrayOf(0f, 100f), floatArrayOf(100f, 200f))
    /// Animator for animating between different y-ranges.
    private val yRangeAnimator = ValueAnimator.ofObject(yRangeEvaluator, floatArrayOf(0f, 100f), floatArrayOf(100f, 200f))

    /// Paint used for drawing x- and y-marks.
    private val markPaint = Paint()
    /// Color of x- and y-marks.
    private var markColor = Color.BLACK
    /// Line width of x- and y-marks.
    private var markLineWidth = 2f
    /// Text size of x- and y-mark labels
    private var markTextSize = 10f

    /// Extra x-marks with labels
    private var xMarks : FloatArray? = null
    /// Actual number of x-marks (which can be smaller than the x-marks array)
    private var numXMarks = 0
    /// Labels of the x-marks
    private var xMarkLabels : Array<String> ?= null

    /// Extra y-marks with labels
    private var yMarks : FloatArray? = null
    /// Actual number of y-marks (which can be smaller than the y-marks array)
    private var numYMarks = 0
    /// Labels of the y-marks
    private var yMarkLabels : Array<String> ?= null

    /** Coordinates for points to be plotted.
      (as filled circles, even indices are x-coordinates, odd indices are y-coordinates) */
    private var points : FloatArray? = null
    /// Only the first numPoints in the points-array are plotted.
    private var numPoints = 0
    /// Circle radius the points.
    private var pointSize = 5f
    /// Circle color the points.
    private var pointColor = Color.BLACK
    /// Paint used for drawing points.
    private val pointPaint = Paint()

    /// Paint for drawing ticks an their labels.
    private val tickPaint = Paint()
    /// Color of ticks and tick labels
    private var tickColor = Color.BLACK
    /// Line width of ticks
    private var tickLineWidth = 2f
    /// Text size of ticks
    private var tickTextSize = 10f

    /// Ticks for x-axis (must be sorted)
    private var xTicks : FloatArray? = null
    /// List with an label for each x-tick or null if there are no labels
    private var xTickLabels : Array<String>? = null
    //private var xTickTextFormatter : ((Float) -> String)? = null

    /// Ticks for x-axis (must be sorted)
    private var yTicks : FloatArray? = null
    /// List with an label for each x-tick or null if there are no labels
    private var yTickLabels : Array<String>? = null
    //private var yTickTextFormatter : ((Float) -> String)? = null
    /// Width of y-tick labels (defines the horizontal space required, must me larger zero if y-tick labels are defined)
    private var yTickLabelWidth = 0.0f

    /// Plot title
    private var title : String? = null
    /// Font size of title
    private var titleSize = 10f

    /// Temporary which is used for drawing paths.
    private val straightLinePath = Path()
    /// Temporary which is used for drawing points.
    private val point = FloatArray(2)

    @Parcelize
    private class SavedState(
        val xRange: FloatArray,
        val yRange: FloatArray,
        val xTicks: FloatArray?,
        val xTickLabels: Array<String>?,
        val yTicks: FloatArray?,
        val yTickLabels: Array<String>?,
        val rawPlotBounds: RectF,
        val numXMarks: Int,
        val xMarks: FloatArray?,
        val xMarkLabels: Array<String>?,
        val numYMarks: Int,
        val yMarks: FloatArray?,
        val yMarkLabels: Array<String>?,
        val numPoints: Int,
        val points: FloatArray?
    ): Parcelable

    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, R.attr.plotViewStyle)

    init {
        isSaveEnabled = true

        attrs?.let {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.PlotView, defStyleAttr, R.style.PlotViewStyle)
            //val ta = context.obtainStyledAttributes(it, R.styleable.PlotView)
            plotLineColor = ta.getColor(R.styleable.PlotView_plotLineColor, plotLineColor)
            plotLineWidth = ta.getDimension(R.styleable.PlotView_plotLineWidth, plotLineWidth)
            markColor = ta.getColor(R.styleable.PlotView_markColor, markColor)
            markLineWidth = ta.getDimension(R.styleable.PlotView_markLineWidth, markLineWidth)
            markTextSize = ta.getDimension(R.styleable.PlotView_markTextSize, markTextSize)
            tickColor = ta.getColor(R.styleable.PlotView_tickColor, tickColor)
            tickLineWidth = ta.getDimension(R.styleable.PlotView_tickLineWidth, tickLineWidth)
            tickTextSize = ta.getDimension(R.styleable.PlotView_tickTextSize, tickTextSize)
            yTickLabelWidth = ta.getDimension(R.styleable.PlotView_yTickLabelWidth, yTickLabelWidth)
            pointSize = ta.getDimension(R.styleable.PlotView_pointSize, pointSize)
            pointColor = ta.getColor(R.styleable.PlotView_pointColor, pointColor)
            title = ta.getString(R.styleable.PlotView_title)
            titleSize = ta.getDimension(R.styleable.PlotView_titleSize, titleSize)
            ta.recycle()
        }
        paint.color = plotLineColor
        paint.strokeWidth = plotLineWidth
        paint.isAntiAlias = true
        paint.textSize = titleSize

        rawPlotLine.moveTo(0f, 0f)
        val numValues = 100
        for(i in 0..numValues){
            val x = i.toFloat() / numValues.toFloat() * 2f * PI.toFloat()
            val y = sin(x)
            rawPlotLine.lineTo(x, y)
        }
        rawPlotBounds.set(0f, -0.8f, 2.0f*PI.toFloat(), 0.8f)

        markPaint.color = markColor
        markPaint.strokeWidth = markLineWidth
        markPaint.isAntiAlias = true
        markPaint.style = Paint.Style.STROKE
        markPaint.textSize = markTextSize

        pointPaint.color = pointColor
        pointPaint.isAntiAlias = true
        pointPaint.style = Paint.Style.FILL

        tickPaint.color = tickColor
        tickPaint.strokeWidth = tickLineWidth
        tickPaint.isAntiAlias = true
        tickPaint.textSize = tickTextSize

        xRangeAnimator.addUpdateListener {
            if(xRange[0] != autoLimit)
                rawPlotBounds.left = xRange[0]
            if(xRange[1] != autoLimit)
                rawPlotBounds.right = xRange[1]
            invalidate()
        }
        yRangeAnimator.addUpdateListener {
//            Log.v("TestRecordFlow", "PlotView: yRangeAnimator: Updating yRange: ${yRange[0]}, ${yRange[1]}")
            if(yRange[0] != autoLimit)
                rawPlotBounds.bottom = yRange[1]
            if(yRange[1] != autoLimit)
                rawPlotBounds.top = yRange[0]
            invalidate()
        }
    }

    /// Set x-range.
    /**
     * @param minValue Minimum value of x-range (or PlotView.autoLimit for determining the limit based on the plot data).
     * @param maxValue Minimum value of x-range (or PlotView.autoLimit for determining the limit based on the plot data).
     * @param animationDuration Duration for animating to the new range (0L for no animation)
     */
    fun xRange(minValue : Float, maxValue : Float, animationDuration: Long = 0L) {
        if (minValue == xRange[0] && maxValue == xRange[1])
            return

        if(xRangeAnimator.isStarted)
            xRangeAnimator.end()
        if(animationDuration in 1 until NO_REDRAW) {
            xRangeAnimator.setObjectValues(floatArrayOf(rawPlotBounds.left, rawPlotBounds.right), floatArrayOf(minValue, maxValue))
            xRangeAnimator.duration = animationDuration
            xRangeAnimator.start()
            return
        }

        xRange[0] = minValue
        xRange[1] = maxValue

        if(minValue != autoLimit)
            rawPlotBounds.left = minValue
        if(maxValue != autoLimit)
            rawPlotBounds.right = maxValue

        if(animationDuration == 0L) {
            invalidate()
        }
    }

    /// Set y-range.
    /**
     * @param minValue Minimum value of y-range (or PlotView.autoLimit for determining the limit based on the plot data).
     * @param maxValue Minimum value of y-range (or PlotView.autoLimit for determining the limit based on the plot data).
     * @param animationDuration Duration for animating to the new range (0L for no animation)
     */
    fun yRange(minValue : Float, maxValue : Float, animationDuration: Long = 0L) {
        if (minValue == yRange[0] && maxValue == yRange[1])
            return

        if(yRangeAnimator.isStarted)
            yRangeAnimator.end()
        if(animationDuration in 1 until NO_REDRAW) {
            yRangeAnimator.setObjectValues(floatArrayOf(rawPlotBounds.top, rawPlotBounds.bottom), floatArrayOf(minValue, maxValue))
            yRangeAnimator.duration = animationDuration
            yRangeAnimator.start()
            return
        }

        yRange[0] = minValue
        yRange[1] = maxValue

        if(minValue != autoLimit)
            rawPlotBounds.top = minValue
        if(maxValue != autoLimit)
            rawPlotBounds.bottom = maxValue

        if(animationDuration == 0L)
            invalidate()
    }

    /// Plot equidistant values (Taking a FloatArray).
    /**
     * @param yValues Array with equidistant y-values.
     * @param redraw Set this to false in order to not redraw directly (e.g. if you plan to
     *   change something else which also needs to redraw the screen, so you can avoid an
     *   unnecessary redraw.)
     */
    fun plot(yValues : FloatArray, redraw: Boolean = true) {
        plot(yValues.asPlotViewArray(), redraw)
    }

    /// Plot equidistant values (Taking an ArrayList<Float>).
    /**
     * @param yValues Array with equidistant y-values.
     * @param redraw Set this to false in order to not redraw directly (e.g. if you plan to
     *   change something else which also needs to redraw the screen, so you can avoid an
     *   unnecessary redraw.)
     */
    fun plot(yValues : ArrayList<Float>, redraw: Boolean = true) {
        plot(yValues.asPlotViewArray(), redraw)
    }

    /// Plot equidistant values (general version with PlotViewArray).
    /**
     * @param yValues Array with equidistant y-values.
     * @param redraw Set this to false in order to not redraw directly (e.g. if you plan to
     *   change something else which also needs to redraw the screen, so you can avoid an
     *   unnecessary redraw.)
     */
    private fun plot(yValues : PlotViewArray, redraw: Boolean = true) {
        rawPlotLine.rewind()
        plotCalled = true

        if(yValues.isEmpty()) {
            resolveBounds(0f, 0f, 0f, 0f)
            return
        }
        else if(yValues.size == 1) {
            resolveBounds(0f, 0f, yValues[0], yValues[0])
            return
        }

        var startIdx = 0
        var endIdx = yValues.size

        if (xRange[0] != autoLimit)
            startIdx = xRange[0].roundToInt()
        if (xRange[1] != autoLimit)
            endIdx = xRange[1].roundToInt() + 1

        var yMinData = yValues[startIdx]
        var yMaxData = yValues[startIdx]

        val loopStart = max(0, startIdx)
        val loopEnd = min(yValues.size, endIdx)

        for(i in loopStart until loopEnd) {
            rawPlotLine.lineTo(i.toFloat(), yValues[i])
            yMinData = min(yMinData, yValues[i])
            yMaxData = max(yMaxData, yValues[i])
        }

        resolveBounds(startIdx.toFloat(), endIdx.toFloat(), yMinData, yMaxData)

        if(redraw)
            invalidate()
    }

    /// Plot values (Taking FloatArrays).
    /**
     * @param xValues Array with equidistant x-values.
     * @param yValues Array with equidistant y-values.
     * @param redraw Set this to false in order to not redraw directly (e.g. if you plan to
     *   change something else which also needs to redraw the screen, so you can avoid an
     *   unnecessary redraw.)
     */
    fun plot(xValues : FloatArray, yValues : FloatArray, redraw : Boolean = true) {
        plot(xValues.asPlotViewArray(), yValues.asPlotViewArray(), redraw)
    }

    /// Plot values (Taking ArrayLists<Float>).
    /**
     * @param xValues Array with equidistant x-values.
     * @param yValues Array with equidistant y-values.
     * @param redraw Set this to false in order to not redraw directly (e.g. if you plan to
     *   change something else which also needs to redraw the screen, so you can avoid an
     *   unnecessary redraw.)
     */
    fun plot(xValues : ArrayList<Float>, yValues : ArrayList<Float>, redraw : Boolean = true) {
        plot(xValues.asPlotViewArray(), yValues.asPlotViewArray(), redraw)
    }

    /// Plot values (general version take in PlotViewArrays).
    /**
     * @param xValues Array with equidistant x-values.
     * @param yValues Array with equidistant y-values.
     * @param redraw Set this to false in order to not redraw directly (e.g. if you plan to
     *   change something else which also needs to redraw the screen, so you can avoid an
     *   unnecessary redraw.)
     */
    private fun plot(xValues : PlotViewArray, yValues : PlotViewArray, redraw : Boolean = true) {
        plotCalled = true
        require(xValues.size == yValues.size) {"Size of x-values and y-values not equal: " + xValues.size + " != " + yValues.size}
        rawPlotLine.rewind()

        if(yValues.isEmpty()) {
            resolveBounds(0f, 0f, 0f, 0f)
            return
        }
        else if(yValues.size == 1) {
            resolveBounds(xValues[0], xValues[0], yValues[0], yValues[0])
            return
        }

        var startIndex = 0
        var endIndex = xValues.size

        if(xRange[0] != autoLimit) {
            startIndex = xValues.binarySearch(xRange[0])
            if(startIndex < 0)
                startIndex = - (startIndex + 1)
        }

        if(xRange[1] != autoLimit) {
            endIndex = xValues.binarySearch(xRange[1]) + 1
            if(endIndex < 0)
                endIndex = -endIndex
        }

        var yMinData = yValues[startIndex]
        var yMaxData = yValues[startIndex]

        for(i in startIndex until endIndex) {
            rawPlotLine.lineTo(xValues[i], yValues[i])
            yMinData = min(yMinData, yValues[i])
            yMaxData = max(yMaxData, yValues[i])
        }

        resolveBounds(xValues[0], xValues.last(), yMinData, yMaxData)

        if(redraw)
            invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        var bottom = (height - paddingBottom).toFloat()
        if(xTicks?.size != null)
            bottom -= tickTextSize
        var top = paddingTop.toFloat()
        if(title != null)
            top += 1.2f * titleSize

        viewPlotBounds.set(
            paddingLeft + yTickLabelWidth,
            top,
            (width - paddingRight).toFloat(),
            bottom
        )

        plotTransformationMatrix.setRectToRect(rawPlotBounds, viewPlotBounds, Matrix.ScaleToFit.FILL)
        plotTransformationMatrix.postScale(1f, -1f, 0f, viewPlotBounds.centerY())

        drawXTicks(canvas)
        drawYTicks(canvas)

        drawXMarks(canvas)
        drawYMarks(canvas)

        rawPlotLine.transform(plotTransformationMatrix, transformedPlotLine)

        canvas?.save()
        canvas?.clipRect(viewPlotBounds)
        paint.style = Paint.Style.STROKE
        canvas?.drawPath(transformedPlotLine, paint)
        drawPoints(canvas)
        canvas?.restore()
        canvas?.drawRect(viewPlotBounds, paint)

        title?.let {
            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.CENTER
            canvas?.drawText(it,
                        0.5f * width,
                        paddingTop + titleSize,
                        paint)
        }
//        canvas?.drawLine(0f, 0f, getWidth().toFloat(), getHeight().toFloat(), paint)
//        drawArrow(canvas, 0.1f*getWidth(), 0.9f*getHeight(), 0.9f*getWidth(), 0.1f*getHeight(), paint)
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable("super state", super.onSaveInstanceState())
        // TODO: store line
        val plotState = SavedState(xRange, yRange, xTicks, xTickLabels, yTicks, yTickLabels,
            rawPlotBounds, numXMarks, xMarks, xMarkLabels, numYMarks, yMarks, yMarkLabels,
            numPoints, points)
        bundle.putParcelable("plot state", plotState)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {

        val superState = if (state is Bundle) {
            state.getParcelable<SavedState>("plot state")?.let { plotState ->
                plotState.xRange.copyInto(xRange)
                plotState.yRange.copyInto(yRange)
                xTicks = plotState.xTicks
                xTickLabels = plotState.xTickLabels
                yTicks = plotState.yTicks
                yTickLabels = plotState.yTickLabels
                rawPlotBounds.set(plotState.rawPlotBounds)
                numXMarks = plotState.numXMarks
                xMarks = plotState.xMarks
                xMarkLabels = plotState.xMarkLabels
                numYMarks = plotState.numYMarks
                yMarks = plotState.yMarks
                yMarkLabels = plotState.yMarkLabels
                numPoints = plotState.numPoints
                points = plotState.points
            }
            state.getParcelable("super state")
        }
        else {
            state
        }
        super.onRestoreInstanceState(superState)
    }

    private fun drawXMarks(canvas: Canvas?) {
        xMarks?.let {
            straightLinePath.rewind()
            point[1] = 0f

            for(i in 0 until numXMarks) {
                val xVal = it[i]
                if(xVal >= rawPlotBounds.left && xVal <= rawPlotBounds.right) {
                    point[0] = xVal
                    plotTransformationMatrix.mapPoints(point)
                    //Log.v("Tuner", "PlotView.drawXMarks: xRaw="+xVal + " xView=" + point[0])
                    straightLinePath.moveTo(point[0], viewPlotBounds.bottom)
                    straightLinePath.lineTo(point[0], viewPlotBounds.top)
                    markPaint.style = Paint.Style.STROKE
                    canvas?.drawPath(straightLinePath, markPaint)

                    markPaint.style = Paint.Style.FILL
                    markPaint.textAlign = Paint.Align.LEFT
                    canvas?.drawText(
                        xMarkLabels?.get(i) ?: "",
                        point[0] + markTextSize / 2,
                        viewPlotBounds.top - markPaint.ascent(),
                        markPaint
                    )
                }
            }
        }
    }

    private fun drawYMarks(canvas: Canvas?) {
        yMarks?.let {
            straightLinePath.rewind()
            point[0] = 0f

            for(i in 0 until numYMarks) {
                val yVal = it[i]
                if(yVal >= rawPlotBounds.top && yVal <= rawPlotBounds.bottom) {
                    point[1] = yVal
                    plotTransformationMatrix.mapPoints(point)
                    straightLinePath.moveTo(viewPlotBounds.left, point[1])
                    straightLinePath.lineTo(viewPlotBounds.right, point[1])
                    markPaint.style = Paint.Style.STROKE
                    canvas?.drawPath(straightLinePath, markPaint)

                    markPaint.style = Paint.Style.FILL
                    markPaint.textAlign = Paint.Align.RIGHT
                    canvas?.drawText(
                        yMarkLabels?.get(i) ?: "",
                        viewPlotBounds.right-markTextSize/2,
                        point[1] - markTextSize / 2,
                        markPaint
                    )
                }
            }
        }
    }

    private fun drawPoints(canvas: Canvas?) {
        points?.let {
            for(i in 0 until numPoints) {
                point[0] = it[2*i]
                point[1] = it[2*i+1]
                if(point[0] >= rawPlotBounds.left && point[0] <= rawPlotBounds.right
                    && point[1] >= rawPlotBounds.top && point[1] <= rawPlotBounds.bottom) {
                    plotTransformationMatrix.mapPoints(point)
                    canvas?.drawCircle(point[0], point[1], pointSize, pointPaint)
                }
            }
        }
    }

    private fun drawXTicks(canvas: Canvas?) {
        xTicks?.let { ticks ->
            straightLinePath.rewind()
            point[1] = 0f
            var startIndex = ticks.binarySearch(rawPlotBounds.left)
            var endIndex = ticks.binarySearch(rawPlotBounds.right)
            if(startIndex < 0)
                startIndex = -(startIndex + 1)
            if(endIndex < 0)
                endIndex = -(endIndex + 1)
            // Log.v("Tuner", "PlotView:drawXTicks: startIndex = $startIndex, endIndex = $endIndex, xTicks.size = "+ ticks.size)
            for(i in startIndex until endIndex) {
                val xVal = ticks[i]
                point[0] = xVal
                // Log.v("Tuner", "PlotView:drawXTicks: xTicks[$i] = $xVal")
                plotTransformationMatrix.mapPoints(point)
                straightLinePath.moveTo(point[0], viewPlotBounds.bottom)
                straightLinePath.lineTo(point[0], viewPlotBounds.top)
                tickPaint.style = Paint.Style.STROKE
                canvas?.drawPath(straightLinePath, tickPaint)

                xTickLabels?.let { tickLabels ->
                    tickPaint.style = Paint.Style.FILL
                    tickPaint.textAlign = Paint.Align.CENTER
                    // Log.v("Tuner", "PlotView:drawXTicks: tickLabels[$i] = " + tickLabels[i])
                    canvas?.drawText(
                        tickLabels[i],
                        point[0],
                        viewPlotBounds.bottom - tickPaint.ascent(),
                        tickPaint
                    )
                }
            }
        }
    }

    private fun drawYTicks(canvas: Canvas?) {
        yTicks?.let { ticks ->
            straightLinePath.rewind()
            point[0] = 0f
            var startIndex = ticks.binarySearch(rawPlotBounds.top)
            var endIndex = ticks.binarySearch(rawPlotBounds.bottom)
            if(startIndex < 0)
                startIndex = -(startIndex + 1)
            if(endIndex < 0)
                endIndex = -(endIndex + 1)

            for(i in startIndex until endIndex) {
                val yVal = ticks[i]
                point[1] = yVal
                plotTransformationMatrix.mapPoints(point)
                straightLinePath.moveTo(viewPlotBounds.left, point[1])
                straightLinePath.lineTo(viewPlotBounds.right, point[1])
                tickPaint.style = Paint.Style.STROKE
                canvas?.drawPath(straightLinePath, tickPaint)

                yTickLabels?.let { tickLabels ->
                    require(yTickLabelWidth > 0.0f) { "PlotView::drawYTicks: If you define yTick labels you must specify a yTickLabelWidth larger than 0.0f" }
                    tickPaint.style = Paint.Style.FILL
                    tickPaint.textAlign = Paint.Align.RIGHT
                    canvas?.drawText(
                        tickLabels[i],
                        viewPlotBounds.left - tickTextSize / 2,
                        point[1],
                        tickPaint
                    )
                }
            }
        }
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

    /// Set x-marks (a line with an optional label).
    /**
     * @param value List of values for x-marks.
     * @param numValues Number of values to be taken values-array (use PlotView.TAKE_ALL, to
     *   use all values).
     * @param redraw Set this to false in order to not redraw directly (e.g. if you plan to
     *   change something else which also needs to redraw the screen, so you can avoid an
     *   unnecessary redraw.)
     * @param format A formatting function which converts the x-mark-values to strings. (null for
     *   no labels)
     */
    fun setXMarks(value : FloatArray?, numValues : Int = TAKE_ALL, redraw : Boolean = true, format : ((Float) -> String)? = null) {
        if (value == null) {
            numXMarks = 0
        }
        else {
            var resolvedNumValues = numValues
            if (numValues == TAKE_ALL)
                resolvedNumValues = value.size
            val currentCapacity = xMarks?.size ?: 0
            if (currentCapacity < resolvedNumValues) {
                xMarks = FloatArray(resolvedNumValues)
                xMarkLabels = Array(resolvedNumValues) {""}
            }
            xMarks?.let {
                value.copyInto(it, 0, 0, resolvedNumValues)
                numXMarks = resolvedNumValues

                for (i in 0 until resolvedNumValues)
                    xMarkLabels?.set(i, format?.invoke(value[i]) ?: "")
            }
        }
        if(redraw)
            invalidate()
    }

    /// Set y-marks (a line with an optional label).
    /**
     * @param value List of values for y-marks.
     * @param numValues Number of values to be taken values-array (use PlotView.TAKE_ALL, to
     *   use all values).
     * @param redraw Set this to false in order to not redraw directly (e.g. if you plan to
     *   change something else which also needs to redraw the screen, so you can avoid an
     *   unnecessary redraw.)
     * @param format A formatting function which converts the x-mark-values to strings. (null for
     *   no labels)
     */
    fun setYMarks(value : FloatArray?, numValues : Int = TAKE_ALL, redraw : Boolean = true, format : ((Float) -> String)? = null) {
        if (value == null) {
            numYMarks = 0
        }
        else {
            var resolvedNumValues = numValues
            if (numValues == TAKE_ALL)
                resolvedNumValues = value.size
            val currentCapacity = yMarks?.size ?: 0
            if (currentCapacity < resolvedNumValues) {
                yMarks = FloatArray(resolvedNumValues)
                yMarkLabels = Array(resolvedNumValues) {""}
            }
            yMarks?.let {
                value.copyInto(it, 0, 0, resolvedNumValues)
                numYMarks = resolvedNumValues

                for (i in 0 until resolvedNumValues)
                    yMarkLabels?.set(i, format?.invoke(value[i]) ?: "")
            }
        }
        if(redraw)
            invalidate()
    }

    /// Set points which should be drawn as filled circles.
    /**
     * @param value Array with point coordinates (of form x0, y0, x1, y1, ...)
     * @param redraw Set this to false in order to not redraw directly (e.g. if you plan to
     *   change something else which also needs to redraw the screen, so you can avoid an
     *   unnecessary redraw.)
     */
    fun setPoints(value : FloatArray?, redraw : Boolean = true) {
        if(value == null) {
            numPoints = 0
        }
        else {
            numPoints = value.size / 2
            val currentCapacity = points?.size ?: 0
            if(currentCapacity < numPoints * 2)
                points = FloatArray(numPoints * 2)
            points?.let {
                value.copyInto(it, 0, 0, numPoints * 2)
            }
        }
        if(redraw)
            invalidate()
    }

    /// Set x-ticks.
    /**
     * @param value Values of x-ticks to be drawn.
     * @param redraw Set this to false in order to not redraw directly (e.g. if you plan to
     *   change something else which also needs to redraw the screen, so you can avoid an
     *   unnecessary redraw.)
     * @param format Functions for creating the tick labels from the values. Use null to draw
     *   no labels.
     */
    fun setXTicks(value : FloatArray?, redraw: Boolean = true, format : ((Float) -> String)?) {
        if (value == null) {
            xTicks = null
            xTickLabels = null
        }
        else {
            xTicks = FloatArray(value.size) {i -> value[i]}
            xTickLabels = if (format != null)
                Array(value.size) { i -> format(value[i]) }
            else
                Array(value.size) { i -> value[i].toString() }
        }
        if(!plotCalled)
            resolveBounds(0f,0f, 0f, 0f)
        if(redraw)
            invalidate()
    }

    /// Set y-ticks.
    /**
     * @param value Values of y-ticks to be drawn.
     * @param redraw Set this to false in order to not redraw directly (e.g. if you plan to
     *   change something else which also needs to redraw the screen, so you can avoid an
     *   unnecessary redraw.)
     * @param format Functions for creating the tick labels from the values. Use null to draw
     *   no labels.
     */
    fun setYTicks(value : FloatArray?, redraw: Boolean = true, format : ((Float) -> String)?) {
        if (value == null) {
            yTicks = null
            yTickLabels = null
        }
        else {
            yTicks = FloatArray(value.size) {i -> value[i]}
            yTickLabels = if (format != null)
                Array(value.size) { i -> format(value[i]) }
            else
                Array(value.size) { i -> value[i].toString() }
        }
        if(!plotCalled)
            resolveBounds(0f,0f, 0f, 0f)
        if(redraw)
            invalidate()
    }

    private fun resolveBounds(xMinData : Float, xMaxData : Float, yMinData : Float, yMaxData : Float) {
        if(xRange[0] == autoLimit)
            rawPlotBounds.left = resolveBound(TYPE_MIN, xMinData, xMaxData, xTicks)
        if(xRange[1] == autoLimit)
            rawPlotBounds.right = resolveBound(TYPE_MAX, xMinData, xMaxData, xTicks)
        if(yRange[0] == autoLimit)
            rawPlotBounds.top = resolveBound(TYPE_MIN, yMinData, yMaxData, yTicks)
        if(yRange[1] == autoLimit)
            rawPlotBounds.bottom = resolveBound(TYPE_MAX, yMinData, yMaxData, yTicks)
    }

    private fun resolveBound(type : Int, minValue : Float, maxValue : Float, ticks : FloatArray?) : Float{

        if(maxValue != minValue) {
            return if(type == TYPE_MIN) minValue else maxValue
        }

        if(ticks != null && ticks.size >=2) {
            return if(type == TYPE_MIN) ticks[0] else ticks.last()
        }

        // Remember: for this if minValue is equal to maxValue, thus, here we could also have usd maxValue
        if(minValue == 0f)
            return if(type == TYPE_MIN) -1f else 1f

        return if(type == TYPE_MIN) 0.9f * minValue else 1.1f * maxValue
    }

    // fun setXTickTextFormat(format : ((Float) -> String)?) {
    //     xTickTextFormatter = format
    // }

    // fun setYTickTextFormat(format : ((Float) -> String)?) {
    //     yTickTextFormatter = format
    // }
}
