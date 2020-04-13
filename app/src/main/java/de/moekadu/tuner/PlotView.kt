package de.moekadu.tuner

import android.animation.FloatArrayEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.*

class PlotView(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    : View(context, attrs, defStyleAttr)
{
    companion object {
        const val NO_REDRAW = Long.MAX_VALUE
        const val TAKE_ALL = -1
        const val TYPE_MIN = 0
        const val TYPE_MAX = 1
    }
    val autoLimit = Float.MAX_VALUE

    private var plotCalled = false

    private var plotLineColor = Color.BLACK
    private var plotLineWidth = 5f

    private val paint = Paint()
    private val arrowPath = Path()
    private val rawPlotLine = Path()
    private val transformedPlotLine = Path()
    private val plotTransformationMatrix = Matrix()
    private val rawPlotBounds = RectF()
    private val viewPlotBounds = RectF()

    private var xRange = FloatArray(2) { autoLimit }
    private var yRange = FloatArray(2) { autoLimit }

    private val xRangeEvaluator = FloatArrayEvaluator(xRange)
    private val yRangeEvaluator = FloatArrayEvaluator(yRange)
    private val xRangeAnimator = ValueAnimator.ofObject(xRangeEvaluator)
    private val yRangeAnimator = ValueAnimator.ofObject(yRangeEvaluator, floatArrayOf(0f, 100f), floatArrayOf(100f, 200f))

    private val markPaint = Paint()
    private var markColor = Color.BLACK
    private var markLineWidth = 2f
    private var markTextSize = 10f

    private var xMarks : FloatArray? = null
    private var numXMarks = 0
    private var xMarkLabels : Array<String> ?= null

    private var yMarks : FloatArray? = null
    private var numYMarks = 0
    private var yMarkLabels : Array<String> ?= null
    private var yTickLabelWidth = 0.0f

    private var points : FloatArray? = null
    private var numPoints = 0
    private var pointSize = 5f
    private var pointColor = Color.BLACK
    private val pointPaint = Paint()

    private val tickPaint = Paint()
    private var tickColor = Color.BLACK
    private var tickLineWidth = 2f
    private var tickTextSize = 10f

    private var xTicks : FloatArray ?= null
    private var xTickLabels : Array<String> ?= null
    //private var xTickTextFormatter : ((Float) -> String)? = null
    private var yTicks : FloatArray ?= null
    private var yTickLabels : Array<String> ?= null
    //private var yTickTextFormatter : ((Float) -> String)? = null

    private val straightLinePath = Path()
    private val point = FloatArray(2)

    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, R.attr.plotViewStyle)

    init {
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
            ta.recycle()
        }
        paint.color = plotLineColor
        paint.strokeWidth = plotLineWidth
        paint.isAntiAlias = true

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
            if(yRange[0] != autoLimit)
                rawPlotBounds.bottom = yRange[1]
            if(yRange[1] != autoLimit)
                rawPlotBounds.top = yRange[0]
            invalidate()
        }
    }

    fun xRange(minValue : Float, maxValue : Float, animationDuration: Long = 0L) {
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

    fun yRange(minValue : Float, maxValue : Float, animationDuration: Long = 0L) {
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

    fun plot(yValues : FloatArray, redraw: Boolean = true) {
        rawPlotLine.rewind()
        plotCalled = true

        if(yValues.isEmpty()) {
            resolveBounds(0f, 0f, 0f, 0f)
            return
        }
        else if(yValues.size == 1) {
            resolveBounds(0f, 0f, yValues[0], yValues[1])
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
        val loopEnd = min(yValues.size,endIdx)

        for(i in loopStart until loopEnd) {
            rawPlotLine.lineTo((i - startIdx).toFloat(), yValues[i])
            yMinData = min(yMinData, yValues[i])
            yMaxData = max(yMaxData, yValues[i])
        }

        resolveBounds(0f, (endIdx-startIdx).toFloat(), yMinData, yMaxData)

        if(redraw)
            invalidate()
    }

    fun plot(xValues : FloatArray, yValues : FloatArray, redraw : Boolean = true) {
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

        viewPlotBounds.set(
            paddingLeft + yTickLabelWidth,
            paddingTop.toFloat(),
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

//        canvas?.drawLine(0f, 0f, getWidth().toFloat(), getHeight().toFloat(), paint)
//        drawArrow(canvas, 0.1f*getWidth(), 0.9f*getHeight(), 0.9f*getWidth(), 0.1f*getHeight(), paint)
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
        xTicks?.let {
            straightLinePath.rewind()
            point[1] = 0f
            var startIndex = it.binarySearch(rawPlotBounds.left)
            var endIndex = it.binarySearch(rawPlotBounds.right)
            if(startIndex < 0)
                startIndex = -(startIndex + 1)
            if(endIndex < 0)
                endIndex = -(endIndex + 1)
            // Log.v("Tuner", "PlotView:drawXTicks: startidx = $startIndex, endindex = $endIndex, xTicks.size = "+ it.size)
            for(i in startIndex until endIndex) {
                val xVal = it[i]
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
        yTicks?.let {
            straightLinePath.rewind()
            point[0] = 0f
            var startIndex = it.binarySearch(rawPlotBounds.top)
            var endIndex = it.binarySearch(rawPlotBounds.bottom)
            if(startIndex < 0)
                startIndex = -(startIndex + 1)
            if(endIndex < 0)
                endIndex = -(endIndex + 1)

            for(i in startIndex until endIndex) {
                val yVal = it[i]
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

    fun drawArrow(canvas: Canvas?, startX : Float, startY : Float, endX : Float, endY : Float, paint : Paint) {
        val lineLength = sqrt((endX - startX).pow(2) + (endY - startY).pow(2))
        val dx = (endX-startX) / lineLength
        val dy = (endY-startY) / lineLength
        val sW = plotLineWidth

        val arrowLength = 5 * sW
        val strokeEndX = endX - dx*arrowLength
        val strokeEndY = endY - dy*arrowLength
        canvas?.drawLine(startX, startY, strokeEndX, strokeEndY, paint)
        paint.style = Paint.Style.FILL

        arrowPath.reset()
        val arrowWidth = 3 * sW
        arrowPath.moveTo(strokeEndX+0.5f*dy*arrowWidth, strokeEndY-0.5f*dx*arrowWidth)
        arrowPath.lineTo(endX, endY)
        arrowPath.lineTo(strokeEndX-0.5f*dy*arrowWidth, strokeEndY+0.5f*dx*arrowWidth)
        arrowPath.close()
        canvas?.drawPath(arrowPath, paint)
    }

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

