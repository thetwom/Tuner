package de.moekadu.tuner

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class PlotView(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    : View(context, attrs, defStyleAttr)
{
    val noLimit = Float.MAX_VALUE

    private var strokeColor = Color.BLACK
    private var strokeWidth = 5f
    private val paint = Paint()
    private val arrowPath = Path()
    private val rawPlotLine = Path()
    private val transformedPlotLine = Path()
    private val plotTransformationMatrix = Matrix()
    private val rawPlotBounds = RectF()
    private val viewPlotBounds = RectF()

    private var xMin = noLimit
    private var xMax = noLimit

    private var yMin = noLimit
    private var yMax = noLimit

    private var xMarks : FloatArray? = null

    private val markPaint = Paint()
    private var markColor = Color.BLACK
    private var markWidth = 2f
    private var markTextSize = 10f

    private var xMarkTextFormatter : ((Float) -> String)? = null

    private val tickPaint = Paint()
    private var xTicks : FloatArray ?= null
    private var tickColor = Color.BLACK
    private var tickWidth = 2f
    private var tickTextSize = 10f

    private var xTickTextFormatter : ((Float) -> String)? = null

    private val straightLinePath = Path()
    private val point = FloatArray(2)

    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, R.attr.plotViewStyle)

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.PlotView, defStyleAttr, R.style.PlotViewStyle)
            //val ta = context.obtainStyledAttributes(it, R.styleable.PlotView)
            strokeColor = ta.getColor(R.styleable.PlotView_strokeColor, strokeColor)
            strokeWidth = ta.getDimension(R.styleable.PlotView_strokeWidth, strokeWidth)
            markColor = ta.getColor(R.styleable.PlotView_markColor, markColor)
            markWidth = ta.getDimension(R.styleable.PlotView_markWidth, markWidth)
            markTextSize = ta.getDimension(R.styleable.PlotView_markTextSize, markTextSize)
            tickColor = ta.getColor(R.styleable.PlotView_tickColor, tickColor)
            tickWidth = ta.getDimension(R.styleable.PlotView_tickWidth, tickWidth)
            tickTextSize = ta.getDimension(R.styleable.PlotView_tickTextSize, tickTextSize)
            ta.recycle()
        }
        paint.color = strokeColor
        paint.strokeWidth = strokeWidth
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
        markPaint.strokeWidth = markWidth
        markPaint.isAntiAlias = true
        markPaint.style = Paint.Style.STROKE
        markPaint.textAlign = Paint.Align.LEFT
        markPaint.textSize = markTextSize

        tickPaint.color = tickColor
        tickPaint.strokeWidth = tickWidth
        tickPaint.isAntiAlias = true
        tickPaint.style = Paint.Style.STROKE
        tickPaint.textAlign = Paint.Align.CENTER
        tickPaint.textSize = tickTextSize
    }

    fun xRange(minValue : Float, maxValue : Float, redraw: Boolean = true) {
        xMin = minValue
        xMax = maxValue
        if(redraw)
            invalidate()
    }

    fun yRange(minValue : Float, maxValue : Float, redraw : Boolean = true) {
        yMin = minValue
        yMax = maxValue
        if(redraw)
            invalidate()
    }

    fun plot(yValues : FloatArray, redraw: Boolean = true) {
        rawPlotLine.rewind()

        var startIdx = 0
        var endIdx = yValues.size

        if (xMin != noLimit)
            startIdx = round(xMin).toInt()
        if (xMax != noLimit)
            endIdx = round(xMax).toInt() + 1

        var yMinData = yValues[startIdx]
        var yMaxData = yValues[startIdx]

        val loopStart = max(0, startIdx)
        val loopEnd = min(yValues.size,endIdx)

        for(i in loopStart until loopEnd) {
            rawPlotLine.lineTo((i - startIdx).toFloat(), yValues[i])
            yMinData = min(yMinData, yValues[i])
            yMaxData = max(yMaxData, yValues[i])
        }

        val yMinVal = if(yMin == noLimit) yMinData else yMin
        val yMaxVal = if(yMax == noLimit) yMaxData else yMax

        rawPlotBounds.set(0.0f, yMinVal, (endIdx-startIdx).toFloat(), yMaxVal)

        if(redraw)
            invalidate()
    }

    fun plot(xValues : FloatArray, yValues : FloatArray, redraw : Boolean = true) {
        require(xValues.size == yValues.size) {"Size of x-values and y-values not equal: " + xValues.size + " != " + yValues.size}
        rawPlotLine.rewind()

        var startIndex = 0
        var endIndex = xValues.size

        if(xMin != noLimit) {
            startIndex = xValues.binarySearch(xMin)
            if(startIndex < 0)
                startIndex = - (startIndex + 1)
        }

        if(xMax != noLimit) {
            endIndex = xValues.binarySearch(xMax) + 1
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

        val xMinVal = if(xMin == noLimit) xValues[0] else xMin
        val xMaxVal = if(xMax == noLimit) xValues.last() else xMax
        val yMinVal = if(yMin == noLimit) yMinData else yMin
        val yMaxVal = if(yMax == noLimit) yMaxData else yMax

        rawPlotBounds.set(xMinVal, yMinVal, xMaxVal, yMaxVal)

        if(redraw)
            invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        var bottom = (height - paddingBottom).toFloat()
        if(xTicks?.size != null)
            bottom -= tickTextSize

        viewPlotBounds.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (width - paddingRight).toFloat(),
            bottom
        )

        plotTransformationMatrix.setRectToRect(rawPlotBounds, viewPlotBounds, Matrix.ScaleToFit.FILL)
        plotTransformationMatrix.postScale(1f, -1f, 0f, viewPlotBounds.centerY())

        drawXTicks(canvas)
        drawXMarks(canvas)

        rawPlotLine.transform(plotTransformationMatrix, transformedPlotLine)

        canvas?.save()
        canvas?.clipRect(viewPlotBounds)
        paint.style = Paint.Style.STROKE
        canvas?.drawPath(transformedPlotLine, paint)
        canvas?.restore()
        canvas?.drawRect(viewPlotBounds, paint)

//        canvas?.drawLine(0f, 0f, getWidth().toFloat(), getHeight().toFloat(), paint)
//        drawArrow(canvas, 0.1f*getWidth(), 0.9f*getHeight(), 0.9f*getWidth(), 0.1f*getHeight(), paint)
    }

    private fun drawXMarks(canvas: Canvas?) {
        xMarks?.let {
            straightLinePath.rewind()
            point[1] = 0f

            for(x in it) {
                if(x >= rawPlotBounds.left && x <= rawPlotBounds.right) {
                    point[0] = x
                    plotTransformationMatrix.mapPoints(point)
                    straightLinePath.moveTo(point[0], viewPlotBounds.bottom)
                    straightLinePath.lineTo(point[0], viewPlotBounds.top)
                    markPaint.style = Paint.Style.STROKE
                    canvas?.drawPath(straightLinePath, markPaint)

                    xMarkTextFormatter?. let{ textFormatter ->
                        markPaint.style = Paint.Style.FILL
                        canvas?.drawText(
                            textFormatter(x),
                            point[0] + markTextSize / 2,
                            viewPlotBounds.top - markPaint.ascent(),
                            markPaint
                        )
                    }
                }
            }
        }
    }

    private fun drawXTicks(canvas: Canvas?) {
        xTicks?.let {
            straightLinePath.rewind()
            point[1] = 0f

            for(x in it) {
                if(x >= rawPlotBounds.left && x <= rawPlotBounds.right) {
                    point[0] = x
                    plotTransformationMatrix.mapPoints(point)
                    straightLinePath.moveTo(point[0], viewPlotBounds.bottom)
                    straightLinePath.lineTo(point[0], viewPlotBounds.top)
                    tickPaint.style = Paint.Style.STROKE
                    canvas?.drawPath(straightLinePath, tickPaint)

                    xTickTextFormatter?.let { textFormatter ->
                        tickPaint.style = Paint.Style.FILL
                        canvas?.drawText(
                            textFormatter(x),
                            point[0],
                            viewPlotBounds.bottom - tickPaint.ascent(),
                            tickPaint
                        )
                    }
                }
            }
        }
    }

    fun drawArrow(canvas: Canvas?, startX : Float, startY : Float, endX : Float, endY : Float, paint : Paint) {
        val lineLength = sqrt((endX - startX).pow(2) + (endY - startY).pow(2))
        val dx = (endX-startX) / lineLength
        val dy = (endY-startY) / lineLength
        val sW = strokeWidth

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

    fun setXMarks(value : FloatArray?, redraw: Boolean = true) {
        if (value == null) {
            xMarks = null
        }
        else {
            if (xMarks == null || xMarks?.size != value.size)
                xMarks = FloatArray(value.size)
            xMarks?.let {
                value.copyInto(it)
            }
        }
        if(redraw)
            invalidate()
    }

    fun setXMarkTextFormat(format : ((Float) -> String)?) {
        xMarkTextFormatter = format
    }

    fun setXTicks(value : FloatArray?, redraw: Boolean = true) {
        if (value == null) {
            xTicks = null
        }
        else {
            if (xTicks == null || xTicks?.size != value.size)
                xTicks = FloatArray(value.size)
            xTicks?.let {
                value.copyInto(it)
            }
        }
        if(redraw)
            invalidate()
    }

    fun setXTickTextFormat(format : ((Float) -> String)?) {
        xTickTextFormatter = format
    }
}

