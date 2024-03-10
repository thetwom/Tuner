package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.times
import kotlin.math.max
import kotlin.math.min

private class WrappedPath(val path: Path = Path())

class Line(
    val lineWidth: Dp,
    initialXValues: FloatArray? = null,
    initialYValues: FloatArray? = null,
) : PlotItem {

    private var pathRaw by mutableStateOf(WrappedPath())
    private val pathTransformed = WrappedPath()

    private var _boundingBox = mutableStateOf(
        Rect(//0f, 30f, 15f, 15f
            Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY
        )
    )

    override val boundingBox: State<Rect> get() = _boundingBox

    init {
        if (initialXValues != null && initialYValues != null) {
            setLine(xValues = initialXValues, yValues = initialYValues)
        }
    }
    override fun getExtraExtentsScreen(density: Density): Rect {
        return with (density) {
            val halfLineWidthPx = 0.5f * lineWidth.toPx()
            Rect(halfLineWidthPx, halfLineWidthPx, halfLineWidthPx, halfLineWidthPx)
        }
    }

    fun setLine(xValues: FloatArray, yValues: FloatArray, indexBegin: Int = 0, indexEnd: Int = min(xValues.size, yValues.size)) {
        val numAvailableValues = min(xValues.size, yValues.size)
        val indexBeginResolved = max(0, indexBegin)
        val indexEndResolved = min(indexEnd, numAvailableValues)
        val numValuesResolved = max(0, indexEndResolved - indexBeginResolved)

        var xMin = Float.POSITIVE_INFINITY
        var xMax = Float.NEGATIVE_INFINITY
        var yMin = Float.POSITIVE_INFINITY
        var yMax = Float.NEGATIVE_INFINITY
        val path = pathRaw.path
        path.rewind()
        if (numValuesResolved > 0) {
            xMin = xValues[0]
            xMax = xValues[0]
            yMin = yValues[0]
            yMax = yValues[0]
//            Log.v("Tuner", "Line: move to ${xValues[0]}, ${yValues[0]}")
            path.moveTo(xValues[0], yValues[0])
        }
        for (i in indexBeginResolved+1 until indexEndResolved) {
            xMin = min(xMin, xValues[i])
            xMax = max(xMax, xValues[i])
            yMin = min(yMin, yValues[i])
            yMax = max(yMax, yValues[i])
            path.lineTo(xValues[i], yValues[i])
        }
        pathRaw = WrappedPath(path)
        _boundingBox.value = Rect(xMin, yMax, xMax, yMin)
    }


    @Composable
    override fun Item(transformation: Transformation) {
    //override fun Item(transformToScreen: Matrix) {
        //val pathTransformed = remember(transformToScreen, boundingBox.value) {
        val pathTransformed = remember(transformation, boundingBox.value) {
            val path = this.pathTransformed.path
            path.rewind()
            path.addPath(pathRaw.path)
            //path.transform(transformToScreen)
            path.transform(transformation.matrixRawToScreen)
            WrappedPath(path)
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                //.background(Color.Gray)
        ) {
            drawPath(pathTransformed.path, color = Color.Black, style = Stroke(width=lineWidth.toPx()))  // TODO: color must be set differently
        }
    }
}