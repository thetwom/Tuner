package de.moekadu.tuner

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.android.material.card.MaterialCardView
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class PlotView(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    : View(context, attrs, defStyleAttr)
{
    var strokeColor = Color.BLACK
    var strokeWidth = 5f
    var paint = Paint()
    var arrowPath = Path()
    var rawPlotLine = Path()
    var transformedPlotLine = Path()
    var plotTransformationMatrix = Matrix()
    var rawPlotBounds = RectF()
    var viewPlotBounds = RectF()

    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, R.attr.plotViewStyle)
    { }

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.PlotView, defStyleAttr, R.style.PlotViewStyle);
            //val ta = context.obtainStyledAttributes(it, R.styleable.PlotView)
            strokeColor = ta.getColor(R.styleable.PlotView_strokeColor, Color.BLACK)
            strokeWidth = ta.getDimension(R.styleable.PlotView_strokeWidth, 5f)
            ta.recycle()
        }
        paint.color = strokeColor
        paint.strokeWidth = strokeWidth
        paint.isAntiAlias = true

        rawPlotLine.moveTo(0f, 0f)
        val nvals = 100
        for(i in 0..nvals){
            val x = i.toFloat() / nvals.toFloat() * 2f * PI.toFloat()
            val y = sin(x)
            rawPlotLine.lineTo(x, y)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        viewPlotBounds.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (width - paddingRight).toFloat(),
            (height - paddingBottom).toFloat()
        )

        rawPlotBounds.set(0f, -0.8f, 2.0f*PI.toFloat(), 0.8f)

        plotTransformationMatrix.setRectToRect(rawPlotBounds, viewPlotBounds, Matrix.ScaleToFit.FILL)
        plotTransformationMatrix.postScale(1f, -1f, 0f, viewPlotBounds.centerY())

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

    fun drawArrow(canvas: Canvas?, startX : Float, startY : Float, endX : Float, endY : Float, paint : Paint) {
        val lineLength = sqrt((endX - startX).pow(2) + (endY - startY).pow(2))
        val dx = (endX-startX) / lineLength
        val dy = (endY-startY) / lineLength
        val sW = strokeWidth

        val arrowLength = 5 * sW
        val strokeEndX = endX - dx*arrowLength
        val strokeEndY = endY - dy*arrowLength
        canvas?.drawLine(startX, startY, strokeEndX, strokeEndY, paint)
        paint.setStyle(Paint.Style.FILL)

        arrowPath.reset()
        val arrowWidth = 3 * sW
        arrowPath.moveTo(strokeEndX+0.5f*dy*arrowWidth, strokeEndY-0.5f*dx*arrowWidth)
        arrowPath.lineTo(endX, endY)
        arrowPath.lineTo(strokeEndX-0.5f*dy*arrowWidth, strokeEndY+0.5f*dx*arrowWidth)
        arrowPath.close()
        canvas?.drawPath(arrowPath, paint)
    }
}
