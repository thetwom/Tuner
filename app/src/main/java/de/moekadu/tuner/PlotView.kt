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
import android.text.StaticLayout
import android.text.TextPaint
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
        /// Special option for animationDuration which means that we don't animate and don't invalidate.
        const val NO_REDRAW = Long.MAX_VALUE
        /// Option to set the plot limits according to the underlying data.
        const val AUTO_LIMIT = Float.MAX_VALUE
        /// Special setting for defining marks with horizontal or vertical lines.
        const val DRAW_LINE = Float.MAX_VALUE
    }

    /// Definition of bound types.
    enum class BoundType {Min, Max}

    /// Anchor for placing marks relative to given point
    enum class MarkAnchor {Center, North, West, East, NorthWest, NorthEast, South, SouthWest, SouthEast}
    /// Definitions of how large the background of a mark label should be
    enum class MarkLabelBackgroundSize {FitIndividually, FitLargest}

    /// Class defining a plot mark.
    /**
     * @param xPosition x-position of mark (or PlotView.DrawLine to draw a horizontal line).
     * @param yPosition y-position of mark (or PlotView.DrawLine to draw a vertical line).
     * @param label Mark label (or null, if just a line should be drawn).
     * @param anchor Placement of mark label relative to position.
     * @param style Mark style to be used (0 -> use xml mark attributes without postfix,
     *   1 -> use xml mark attributes with "2" postfix)
     */
    @Parcelize
    data class Mark(val xPosition: Float, val yPosition: Float, val label: CharSequence?,
                    val anchor: MarkAnchor, val style: Int = 0) : Parcelable

    /// Class combining a group of marks
    /**
     * @param marks List of all marks within this group.
     * @param backgroundSizeType Definition of how large the background of the marks should be.
     *   MarkLabelBackgroundSize.FitIndividually will fit the background according to each label individually
     *   MarkLabelBackgroundSize.FitLargest will determine the largest background size within in the group
     *     and use this for all marks within the group.
     */
    @Parcelize
    data class MarkGroup(val marks: ArrayList<Mark>, val backgroundSizeType: MarkLabelBackgroundSize) : Parcelable

    /// This is just for making sure, that bounds initialized and tells if "plot" has been called at least once since the creation of the class.
    private var plotCalled = false

    /// Color of plot line.
    private var plotLineColor = Color.BLACK
    /// Color of plot line when inactive.
    private var plotLineColorInactive = Color.GRAY
    /// Width of plot line.
    private var plotLineWidth = 5f

    /// Paint used for plotting line and title
    private val plotLinePaint = Paint()

    //    private val arrowPath = Path()
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
    private val xRange = FloatArray(2) { AUTO_LIMIT }
    /// y-range to plot (or autoLimit for determining the limits based on the plot data).
    private val yRange = FloatArray(2) { AUTO_LIMIT }

    /// Evaluator for x-range needed for x-range animation.
    private val xRangeEvaluator = FloatArrayEvaluator(xRange)
    /// Evaluator for y-range needed for y-range animation.
    private val yRangeEvaluator = FloatArrayEvaluator(yRange)
    /// Animator for animating between different x-ranges.
    private val xRangeAnimator = ValueAnimator.ofObject(xRangeEvaluator, floatArrayOf(0f, 100f), floatArrayOf(100f, 200f))
    /// Animator for animating between different y-ranges.
    private val yRangeAnimator = ValueAnimator.ofObject(yRangeEvaluator, floatArrayOf(0f, 100f), floatArrayOf(100f, 200f))

    /// Number of available mark styles
    private val numMarkStyles = 2
    /// Paint used for drawing x- and y-marks.
    private val markPaint = Array(numMarkStyles) {Paint()}
    /// Color of x- and y-marks.
    private var markColor = Array(numMarkStyles) {Color.BLACK}
    /// Line width of x- and y-marks.
    private var markLineWidth = Array(numMarkStyles) {2f}
    /// Text size of x- and y-mark labels
    private var markTextSize = Array(numMarkStyles) {10f}
    /// Text color of mark labels
    private var markLabelColor = Array(numMarkStyles) {Color.WHITE}
    /// Paint used for drawing mark labels.
    private val markLabelPaint = Array(numMarkStyles) {TextPaint()}

    /// Marks groups.
    private val markGroups = HashMap<Int, MarkGroup>()
    /// StaticLayout for each mark label or null, if the mark has no label
    private val markLabelLayouts = HashMap<Int, ArrayList<StaticLayout?> >()

    /** Coordinates for points to be plotted.
      (as filled circles, even indices are x-coordinates, odd indices are y-coordinates) */
    private var points : FloatArray? = null
    /// Only the first numPoints in the points-array are plotted.
    private var numPoints = 0
    /// Circle radius the points.
    private var pointSize = 5f
    /// Circle color the points.
    private var pointColor = Color.BLACK
    /// Circle color the points when inactive.
    private var pointColorInactive = Color.GRAY
    /// Paint used for drawing points.
    private val pointPaint = Paint()

    /// Paint for drawing tick lines.
    private val tickPaint = Paint()
    /// Paint for drawing tick labels
    private val tickLabelPaint = TextPaint()
    /// Color of ticks and tick labels
    private var tickColor = Color.BLACK
    /// Line width of ticks
    private var tickLineWidth = 2f
    /// Text size of ticks
    private var tickTextSize = 10f

    /// Ticks for x-axis (must be sorted)
    private var xTicks : FloatArray? = null
    /// List with an label for each x-tick or null if there are no labels
    private var xTickLabels : Array<CharSequence>? = null
    /// List with layouts for each label for each x-tick or null if there are no labels
    private var xTickLabelLayouts : Array<StaticLayout>? = null
    //private var xTickTextFormatter : ((Float) -> String)? = null

    /// Ticks for x-axis (must be sorted)
    private var yTicks : FloatArray? = null
    /// List with an label for each y-tick or null if there are no labels
    private var yTickLabels : Array<CharSequence>? = null
    /// List with layouts for each label for each y-tick or null if there are no labels
    private var yTickLabelLayouts : Array<StaticLayout>? = null
    /// Width of y-tick labels (defines the horizontal space required, must me larger zero if y-tick labels are defined)
    private var yTickLabelWidth = 0.0f
    //private var yTickTextFormatter : ((Float) -> String)? = null

    /// Plot title
    private var title : String? = null
    /// Font size of title
    private var titleSize = 10f
    private var titleColor = Color.BLACK
    /// Paint used for plotting title and frame
    private val titlePaint = Paint()
    /// Stroke width used for frame
    private var frameStrokeWidth = 1f

    /// Temporary which is used for drawing paths.
    private val straightLinePath = Path()
    /// Temporary which is used for drawing points.
    private val point = FloatArray(2)

    var linesAndPointsInactive = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    @Parcelize
    private class SavedState(
        val xRange: FloatArray,
        val yRange: FloatArray,
        val xTicks: FloatArray?,
        val xTickLabels: Array<CharSequence>?,
        val yTicks: FloatArray?,
        val yTickLabels: Array<CharSequence>?,
        val markGroups: HashMap<Int, MarkGroup>,
        val rawPlotBounds: RectF,
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
            plotLineColorInactive = ta.getColor(R.styleable.PlotView_plotLineColorInactive, plotLineColorInactive)
            plotLineWidth = ta.getDimension(R.styleable.PlotView_plotLineWidth, plotLineWidth)

            markColor[0] = ta.getColor(R.styleable.PlotView_markColor, markColor[0])
            markLineWidth[0] = ta.getDimension(R.styleable.PlotView_markLineWidth, markLineWidth[0])
            markTextSize[0] = ta.getDimension(R.styleable.PlotView_markTextSize, markTextSize[0])
            markLabelColor[0] = ta.getColor(R.styleable.PlotView_markLabelColor, markLabelColor[0])

            markColor[1] = ta.getColor(R.styleable.PlotView_markColor2, markColor[1])
            markLineWidth[1] = ta.getDimension(R.styleable.PlotView_markLineWidth2, markLineWidth[1])
            markTextSize[1] = ta.getDimension(R.styleable.PlotView_markTextSize2, markTextSize[1])
            markLabelColor[1] = ta.getColor(R.styleable.PlotView_markLabelColor2, markLabelColor[1])

            tickColor = ta.getColor(R.styleable.PlotView_tickColor, tickColor)
            tickLineWidth = ta.getDimension(R.styleable.PlotView_tickLineWidth, tickLineWidth)
            tickTextSize = ta.getDimension(R.styleable.PlotView_tickTextSize, tickTextSize)
            yTickLabelWidth = ta.getDimension(R.styleable.PlotView_yTickLabelWidth, yTickLabelWidth)
            pointSize = ta.getDimension(R.styleable.PlotView_pointSize, pointSize)
            pointColor = ta.getColor(R.styleable.PlotView_pointColor, pointColor)
            pointColorInactive = ta.getColor(R.styleable.PlotView_pointColorInactive, pointColorInactive)
            title = ta.getString(R.styleable.PlotView_title)
            titleSize = ta.getDimension(R.styleable.PlotView_titleSize, titleSize)
            titleColor = ta.getColor(R.styleable.PlotView_titleColor, titleColor)
            frameStrokeWidth = ta.getDimension(R.styleable.PlotView_frameStrokeWidth, frameStrokeWidth)
            ta.recycle()
        }
        plotLinePaint.color = plotLineColor
        plotLinePaint.strokeWidth = plotLineWidth
        plotLinePaint.style = Paint.Style.STROKE
        plotLinePaint.isAntiAlias = true

        titlePaint.color = titleColor
        titlePaint.isAntiAlias = true
        titlePaint.textSize = titleSize
        titlePaint.strokeWidth = frameStrokeWidth

        rawPlotLine.moveTo(0f, 0f)
        val numValues = 100
        for(i in 0..numValues){
            val x = i.toFloat() / numValues.toFloat() * 2f * PI.toFloat()
            val y = sin(x)
            rawPlotLine.lineTo(x, y)
        }
        rawPlotBounds.set(0f, -0.8f, 2.0f*PI.toFloat(), 0.8f)

        for (i in 0 until numMarkStyles) {
            markPaint[i].color = markColor[i]
            markPaint[i].strokeWidth = markLineWidth[i]
            markPaint[i].isAntiAlias = true

            markLabelPaint[i].color = markLabelColor[i]
            markLabelPaint[i].isAntiAlias = true
            markLabelPaint[i].textSize = markTextSize[i]
        }
        pointPaint.color = pointColor
        pointPaint.isAntiAlias = true
        pointPaint.style = Paint.Style.FILL

        tickPaint.color = tickColor
        tickPaint.strokeWidth = tickLineWidth
        tickPaint.isAntiAlias = true
        tickPaint.style = Paint.Style.STROKE

        tickLabelPaint.color = tickColor
        tickLabelPaint.isAntiAlias = true
        tickLabelPaint.textSize = tickTextSize
        tickLabelPaint.style = Paint.Style.FILL

        xRangeAnimator.addUpdateListener {
            if(xRange[0] != AUTO_LIMIT)
                rawPlotBounds.left = xRange[0]
            if(xRange[1] != AUTO_LIMIT)
                rawPlotBounds.right = xRange[1]
            invalidate()
        }
        yRangeAnimator.addUpdateListener {
//            Log.v("TestRecordFlow", "PlotView: yRangeAnimator: Updating yRange: ${yRange[0]}, ${yRange[1]}")
            if(yRange[0] != AUTO_LIMIT)
                rawPlotBounds.bottom = yRange[1]
            if(yRange[1] != AUTO_LIMIT)
                rawPlotBounds.top = yRange[0]
            invalidate()
        }
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

        rawPlotLine.transform(plotTransformationMatrix, transformedPlotLine)

        canvas?.save()
        canvas?.clipRect(viewPlotBounds)
        plotLinePaint.color = if (linesAndPointsInactive) plotLineColorInactive else plotLineColor
        canvas?.drawPath(transformedPlotLine, plotLinePaint)
        drawPoints(canvas)
        canvas?.restore()
        titlePaint.style = Paint.Style.STROKE
        canvas?.drawRect(viewPlotBounds, titlePaint)
        drawMarks(canvas)

        title?.let {
            titlePaint.style = Paint.Style.FILL
            titlePaint.textAlign = Paint.Align.CENTER
            canvas?.drawText(it,
                0.5f * width,
                paddingTop + titleSize,
                titlePaint)
        }
//        canvas?.drawLine(0f, 0f, getWidth().toFloat(), getHeight().toFloat(), paint)
//        drawArrow(canvas, 0.1f*getWidth(), 0.9f*getHeight(), 0.9f*getWidth(), 0.1f*getHeight(), paint)
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable("super state", super.onSaveInstanceState())
        // TODO: store line
        val plotState = SavedState(xRange, yRange, xTicks, xTickLabels, yTicks, yTickLabels,
            markGroups, rawPlotBounds, numPoints, points)
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
                markGroups.clear()
                markGroups.putAll(plotState.markGroups)
                rawPlotBounds.set(plotState.rawPlotBounds)
                numPoints = plotState.numPoints
                points = plotState.points

                buildMarkLabels()
                xTickLabelLayouts = buildTickLabelLayouts(xTickLabels)
                yTickLabelLayouts = buildTickLabelLayouts(yTickLabels)
            }
            state.getParcelable("super state")
        }
        else {
            state
        }
        super.onRestoreInstanceState(superState)
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

        if(minValue != AUTO_LIMIT)
            rawPlotBounds.left = minValue
        if(maxValue != AUTO_LIMIT)
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

        if(minValue != AUTO_LIMIT)
            rawPlotBounds.top = minValue
        if(maxValue != AUTO_LIMIT)
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

    /// Remove marks.
    /**
     * @param groupIdentifier Group identifier, as provided to setMarks, setMark, setXMark, setYMark
     *   or null if all groups should be removed.
     * @param redraw Set this to false in order to not redraw directly (e.g. if you plan to
     *   change something else which also needs to redraw the screen, so you can avoid an
     *   unnecessary redraw.)
     */
    fun unsetMarks(groupIdentifier: Int?, redraw: Boolean = true) {
        if (groupIdentifier == null) {
            markGroups.clear()
            markLabelLayouts.clear()
        }
        else {
            markGroups.remove(groupIdentifier)
            markLabelLayouts.remove(groupIdentifier)
        }

        if (redraw)
            invalidate()
    }

    /// Set marks.
    /**
     * @param marks List of marks.
     * @param groupIdentifier Identifier for the mark group. If the identifier was used before
     *   we well overwrite these marks.
     * @param markLabelBackgroundSize Defines if the background for the labels should all be
     *   the same size or if they should be individually fitted to each label size.
     * @param redraw Set this to false in order to not redraw directly (e.g. if you plan to
     *   change something else which also needs to redraw the screen, so you can avoid an
     *   unnecessary redraw.)
     */
    fun setMarks(marks: ArrayList<Mark>,
                 groupIdentifier: Int,
                 markLabelBackgroundSize: MarkLabelBackgroundSize = MarkLabelBackgroundSize.FitIndividually,
                 redraw: Boolean = true) {

        unsetMarks(groupIdentifier, false)

        val markGroup = MarkGroup(marks, markLabelBackgroundSize)
        markGroups[groupIdentifier] = markGroup
        buildMarkLabels(groupIdentifier)

        if (redraw)
            invalidate()
    }

    /// Convenience method to set a single mark.
    /**
     * @param xPosition x-position of mark.
     * @param yPosition y-position of mark.
     * @param label Label of mark or null if the mark should have no label (i.e. if lines are drawn)
     * @param groupIdentifier Identifier for the mark group. If the identifier was used before
     *   we well overwrite these marks.
     * @param anchor Anchor which defines how to align the label relative to the mark position.
     * @param style Mark style to use (0 -> use mark-xml-attributes without prefix,
     *   1 -> use the xml-attributes with prefix 2).
     * @param redraw Set this to false in order to not redraw directly (e.g. if you plan to
     *   change something else which also needs to redraw the screen, so you can avoid an
     *   unnecessary redraw.)
     */
    fun setMark(xPosition: Float, yPosition: Float, label: CharSequence?, groupIdentifier: Int,
                        anchor: MarkAnchor = MarkAnchor.Center, style: Int = 0,
                        redraw: Boolean = true) {
        val marks = ArrayList<Mark>(1)
        marks.add(Mark(xPosition, yPosition, label, anchor, style))
        setMarks(marks, groupIdentifier = groupIdentifier, redraw = redraw)
    }

    /// Convenience method to set a single mark with a vertical line.
    /**
     * @param xPosition x-position of mark.
     * @param label Label of mark or null if the mark should have no label (i.e. if lines are drawn)
     * @param groupIdentifier Identifier for the mark group. If the identifier was used before
     *   we well overwrite these marks.
     * @param anchor Anchor which defines how to align the label relative to the mark position.
     * @param style Mark style to use (0 -> use mark-xml-attributes without prefix,
     *   1 -> use the xml-attributes with prefix 2).
     * @param redraw Set this to false in order to not redraw directly (e.g. if you plan to
     *   change something else which also needs to redraw the screen, so you can avoid an
     *   unnecessary redraw.)
     */
    fun setXMark(xPosition: Float, label: CharSequence?, groupIdentifier: Int,
                 anchor: MarkAnchor = MarkAnchor.Center, style: Int = 0,
                 redraw: Boolean = true) {
        setMark(xPosition, DRAW_LINE, label, groupIdentifier, anchor, style, redraw)
    }

    /// Convenience method to set a single mark with a horizontal line.
    /**
     * @param yPosition y-position of mark.
     * @param label Label of mark or null if the mark should have no label (i.e. if lines are drawn)
     * @param groupIdentifier Identifier for the mark group. If the identifier was used before
     *   we well overwrite these marks.
     * @param anchor Anchor which defines how to align the label relative to the mark position.
     * @param style Mark style to use (0 -> use mark-xml-attributes without prefix,
     *   1 -> use the xml-attributes with prefix 2).
     * @param redraw Set this to false in order to not redraw directly (e.g. if you plan to
     *   change something else which also needs to redraw the screen, so you can avoid an
     *   unnecessary redraw.)
     */
    fun setYMark(yPosition: Float, label: CharSequence?, groupIdentifier: Int,
                 anchor: MarkAnchor = MarkAnchor.Center, style: Int = 0,
                 redraw: Boolean = true) {
        setMark(DRAW_LINE, yPosition, label, groupIdentifier, anchor, style, redraw)
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
    fun setXTicks(value : FloatArray?, redraw: Boolean = true, format : ((Float) -> CharSequence)?) {
        if (value == null) {
            xTicks = null
            xTickLabels = null
        }
        else {
            xTicks = FloatArray(value.size) {i -> value[i]}
            xTickLabels = if (format != null) {
                Array(value.size) { i -> format(value[i]) }
            } else {
                null
            }
            xTickLabelLayouts = buildTickLabelLayouts(xTickLabels)
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
    fun setYTicks(value : FloatArray?, redraw: Boolean = true, format : ((Float) -> CharSequence)?) {
        if (value == null) {
            yTicks = null
            yTickLabels = null
        }
        else {
            yTicks = FloatArray(value.size) {i -> value[i]}
            yTickLabels = if (format != null) {
                Array(value.size) { i -> format(value[i]) }
            } else {
                null
            }
            yTickLabelLayouts = buildTickLabelLayouts(yTickLabels)
        }
        if(!plotCalled)
            resolveBounds(0f,0f, 0f, 0f)
        if(redraw)
            invalidate()
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

        if (xRange[0] != AUTO_LIMIT)
            startIdx = xRange[0].roundToInt()
        if (xRange[1] != AUTO_LIMIT)
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

        if(xRange[0] != AUTO_LIMIT) {
            startIndex = xValues.binarySearch(xRange[0])
            if(startIndex < 0)
                startIndex = - (startIndex + 1)
        }

        if(xRange[1] != AUTO_LIMIT) {
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

    private fun drawPoints(canvas: Canvas?) {
        points?.let {
            pointPaint.color = if (linesAndPointsInactive) pointColorInactive else pointColor
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
                canvas?.drawPath(straightLinePath, tickPaint)

                xTickLabelLayouts?.let { tickLabels ->
                    // Log.v("Tuner", "PlotView:drawXTicks: tickLabels[$i] = " + tickLabels[i])
                    drawStaticLayout(canvas, tickLabels[i], point[0] , viewPlotBounds.bottom + tickLabels[i].height / 2)
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
                canvas?.drawPath(straightLinePath, tickPaint)

                yTickLabelLayouts?.let { tickLabels ->
                    //require(yTickLabelWidth > 0.0f) { "PlotView::drawYTicks: If you define yTick labels you must specify a yTickLabelWidth larger than 0.0f" }
                    val textWidth = tickLabels[i].getLineWidth(0) + tickLabels[i].getLineBottom(0) - tickLabels[i].getLineDescent(0)
                    drawStaticLayout(canvas, tickLabels[i], viewPlotBounds.left - textWidth / 2, point[1])
                }
            }
        }
    }

    private fun drawMarks(canvas: Canvas?) {
        if (canvas == null)
            return

        for ((groupIdentifier, markGroup) in markGroups) {
            val marks = markGroup.marks
            val layouts = markLabelLayouts[groupIdentifier] ?: continue
            val labelWidth = when (markGroup.backgroundSizeType) {
                MarkLabelBackgroundSize.FitLargest ->
                    layouts.maxOf { computeLabelBackgroundWidth(it) }
                MarkLabelBackgroundSize.FitIndividually ->
                    null
            }

            val labelHeight = when (markGroup.backgroundSizeType) {
                MarkLabelBackgroundSize.FitLargest ->
                    layouts.maxOf { it?.height?.toFloat() ?: 0f }
                MarkLabelBackgroundSize.FitIndividually ->
                    null
            }

            marks.zip(layouts) { mark, label ->
                // only plot if mark is within bounds
                if ((mark.xPosition == DRAW_LINE && mark.yPosition >= rawPlotBounds.top && mark.yPosition <= rawPlotBounds.bottom)
                    || (mark.yPosition == DRAW_LINE && mark.xPosition >= rawPlotBounds.left && mark.xPosition <= rawPlotBounds.right)
                    || (mark.xPosition != DRAW_LINE && mark.yPosition != DRAW_LINE
                            && mark.yPosition >= rawPlotBounds.top && mark.yPosition <= rawPlotBounds.bottom
                            && mark.xPosition >= rawPlotBounds.left && mark.xPosition <= rawPlotBounds.right)
                ) {

                    val style = mark.style

                    // find point in view-coordinates
                    point[0] = if (mark.xPosition == DRAW_LINE) 0f else mark.xPosition
                    point[1] = if (mark.yPosition == DRAW_LINE) 0f else mark.yPosition
                    plotTransformationMatrix.mapPoints(point)

                    // horizontal line
                    if (mark.xPosition == DRAW_LINE) {
                        straightLinePath.rewind()
                        straightLinePath.moveTo(viewPlotBounds.left, point[1])
                        straightLinePath.lineTo(viewPlotBounds.right, point[1])
                        markPaint[style].style = Paint.Style.STROKE
                        canvas.drawPath(straightLinePath, markPaint[style])
                    }
                    // vertical line
                    else if (mark.yPosition == DRAW_LINE) {
                        straightLinePath.rewind()
                        straightLinePath.moveTo(point[0], viewPlotBounds.bottom)
                        straightLinePath.lineTo(point[0], viewPlotBounds.top)
                        markPaint[style].style = Paint.Style.STROKE
                        canvas.drawPath(straightLinePath, markPaint[style])
                    }

                    if (label != null) {
                        val backgroundWidth = labelWidth ?: computeLabelBackgroundWidth(label)
                        val backgroundHeight = labelHeight ?: label.height.toFloat()

                        // override mark position based on anchor
                        if (mark.xPosition == DRAW_LINE) {
                            point[0] = when (mark.anchor) {
                                MarkAnchor.Center, MarkAnchor.North, MarkAnchor.South -> viewPlotBounds.centerX()
                                MarkAnchor.West, MarkAnchor.NorthWest, MarkAnchor.SouthWest -> viewPlotBounds.left + backgroundWidth / 2
                                MarkAnchor.East, MarkAnchor.NorthEast, MarkAnchor.SouthEast -> viewPlotBounds.right - backgroundWidth / 2
                            }
                            point[1] = when (mark.anchor) {
                                MarkAnchor.Center, MarkAnchor.West, MarkAnchor.East -> point[1]
                                MarkAnchor.South, MarkAnchor.SouthWest, MarkAnchor.SouthEast -> point[1] - backgroundHeight / 2
                                MarkAnchor.North, MarkAnchor.NorthWest, MarkAnchor.NorthEast -> point[1] + backgroundHeight / 2
                            }
                        } else if (mark.yPosition == DRAW_LINE) {
                            point[0] = when (mark.anchor) {
                                MarkAnchor.Center, MarkAnchor.North, MarkAnchor.South -> point[0]
                                MarkAnchor.West, MarkAnchor.NorthWest, MarkAnchor.SouthWest -> point[0] + backgroundWidth / 2
                                MarkAnchor.East, MarkAnchor.NorthEast, MarkAnchor.SouthEast -> point[0] - backgroundWidth / 2
                            }
                            point[1] = when (mark.anchor) {
                                MarkAnchor.Center, MarkAnchor.West, MarkAnchor.East -> viewPlotBounds.centerY()
                                MarkAnchor.South, MarkAnchor.SouthWest, MarkAnchor.SouthEast -> viewPlotBounds.bottom - backgroundHeight / 2
                                MarkAnchor.North, MarkAnchor.NorthWest, MarkAnchor.NorthEast -> viewPlotBounds.top + backgroundHeight / 2
                            }
                        } else if (mark.xPosition != DRAW_LINE && mark.yPosition != DRAW_LINE) {
                            point[0] = when (mark.anchor) {
                                MarkAnchor.Center, MarkAnchor.North, MarkAnchor.South -> point[0]
                                MarkAnchor.West, MarkAnchor.NorthWest, MarkAnchor.SouthWest -> point[0] + backgroundWidth / 2
                                MarkAnchor.East, MarkAnchor.NorthEast, MarkAnchor.SouthEast -> point[0] - backgroundWidth / 2
                            }
                            point[1] = when (mark.anchor) {
                                MarkAnchor.Center, MarkAnchor.West, MarkAnchor.East -> point[1]
                                MarkAnchor.South, MarkAnchor.SouthWest, MarkAnchor.SouthEast -> point[1] - backgroundHeight / 2
                                MarkAnchor.North, MarkAnchor.NorthWest, MarkAnchor.NorthEast -> point[1] + backgroundHeight / 2
                            }
                        } else {
                            throw RuntimeException("Invalid mark position")
                        }

                        markPaint[style].style = Paint.Style.FILL
                        canvas.drawRect(
                            point[0] - backgroundWidth / 2,
                            point[1] - backgroundHeight / 2,
                            point[0] + backgroundWidth / 2,
                            point[1] + backgroundHeight / 2,
                            markPaint[style]
                        )
                        drawStaticLayout(canvas, label, point[0], point[1])
                    }
                }
            }
        }
    }

    private fun drawStaticLayout(canvas: Canvas?, layout: StaticLayout, xCenter: Float, yCenter: Float) {
        if (canvas == null)
            return
        val textWidth = layout.getLineWidth(0)
        val textHeight = layout.height.toFloat()

        canvas.save()
        canvas.translate(xCenter - textWidth / 2, yCenter - textHeight / 2)
        layout.draw(canvas)
        canvas.restore()
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


    private fun buildMarkLabels() {
        markLabelLayouts.clear()
        for (groupIdentifier in markGroups.keys)
            buildMarkLabels(groupIdentifier)
    }

    private fun buildMarkLabels(groupIdentifier: Int) {
        markLabelLayouts.remove(groupIdentifier)
        markGroups[groupIdentifier]?.let { group ->
            val layouts = ArrayList<StaticLayout?>(group.marks.size)
            for (mark in group.marks) {
                if (mark.label != null) {
                    val style = mark.style
                    val desiredWidth = ceil(StaticLayout.getDesiredWidth(mark.label, markLabelPaint[style])).toInt()
                    val builder = StaticLayout.Builder.obtain(mark.label,0, mark.label.length, markLabelPaint[style], desiredWidth)
                    layouts.add(builder.build())
                }
                else {
                    layouts.add(null)
                }
            }
            markLabelLayouts[groupIdentifier] = layouts
        }
    }

    private fun buildTickLabelLayouts(tickLabels: Array<CharSequence>?) : Array<StaticLayout>? {
        return if (tickLabels == null) {
            null
        } else {
            Array(tickLabels.size) { i ->
                val name = tickLabels[i]
                val desiredWidth =
                    ceil(StaticLayout.getDesiredWidth(name, tickLabelPaint)).toInt()
                val builder =
                    StaticLayout.Builder.obtain(name, 0, name.length, tickLabelPaint, desiredWidth)
                builder.build()
            }
        }
    }

    private fun computeLabelBackgroundWidth(layout: StaticLayout?) = if (layout == null) {
        0f
    } else {
        layout.getLineWidth(0) + (layout.getLineBottom(0) - layout.getLineDescent(0)) / 2
    }

    private fun resolveBounds(xMinData : Float, xMaxData : Float, yMinData : Float, yMaxData : Float) {
        if(xRange[0] == AUTO_LIMIT)
            rawPlotBounds.left = resolveBound(BoundType.Min, xMinData, xMaxData, xTicks)
        if(xRange[1] == AUTO_LIMIT)
            rawPlotBounds.right = resolveBound(BoundType.Max, xMinData, xMaxData, xTicks)
        if(yRange[0] == AUTO_LIMIT)
            rawPlotBounds.top = resolveBound(BoundType.Min, yMinData, yMaxData, yTicks)
        if(yRange[1] == AUTO_LIMIT)
            rawPlotBounds.bottom = resolveBound(BoundType.Max, yMinData, yMaxData, yTicks)
    }

    private fun resolveBound(type : BoundType, minValue : Float, maxValue : Float, ticks : FloatArray?) : Float{

        if(maxValue != minValue) {
            return if(type == BoundType.Min) minValue else maxValue
        }

        if(ticks != null && ticks.size >=2) {
            return if(type == BoundType.Min) ticks[0] else ticks.last()
        }

        // Remember: for this if minValue is equal to maxValue, thus, here we could also have usd maxValue
        if(minValue == 0f)
            return if(type == BoundType.Min) -1f else 1f

        return if(type == BoundType.Min) 0.9f * minValue else 1.1f * maxValue
    }

    // fun setXTickTextFormat(format : ((Float) -> String)?) {
    //     xTickTextFormatter = format
    // }

    // fun setYTickTextFormat(format : ((Float) -> String)?) {
    //     yTickTextFormatter = format
    // }
}
