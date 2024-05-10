package de.moekadu.tuner.ui.plot2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toRect
import de.moekadu.tuner.ui.plot.Transformation
import kotlin.math.max
import kotlin.math.min

class Line2 private constructor(
    val coordinates: Coordinates,
    val lineWidth: Dp,
    val color: Color,
    val transformation: Transformation,
    val path: Path
): PlotItem2 {
    override val hasClippedDraw = true
    override val hasUnclippedDraw = false

    data class Coordinates(
        val xValues: FloatArray, val yValues: FloatArray,
        val indexBegin: Int = 0, val indexEnd: Int = min(xValues.size, yValues.size)
    )

    private fun generatePath() {
        val xValues = coordinates.xValues
        val yValues = coordinates.yValues
        val numAvailableValues = min(xValues.size, yValues.size)
        val indexBeginResolved = max(0, coordinates.indexBegin)

        val indexEndResolved = min(coordinates.indexEnd, numAvailableValues)
        val numValuesResolved = max(0, indexEndResolved - indexBeginResolved)

        path.rewind()
        if (numValuesResolved > 0) {
//            Log.v("Tuner", "Line: move to ${xValues[0]}, ${yValues[0]}")
            path.moveTo(xValues[0], yValues[0])
        }
        for (i in indexBeginResolved + 1 until indexEndResolved) {
            path.lineTo(xValues[i], yValues[i])
        }
        path.transform(transformation.matrixRawToScreen)
    }
    fun modify(
        coordinates: Coordinates,
        lineWidth: Dp,
        color: Color,
        transformation: Transformation
    ): Line2 {
        return if (
            coordinates == this.coordinates &&
            lineWidth == this.lineWidth &&
            color == this.color &&
            transformation == this.transformation
        ) {
            return this
        } else {
            val newLine = Line2(coordinates, lineWidth, color, transformation, path)
            if (coordinates != this.coordinates || transformation != this.transformation)
                newLine.generatePath()
            newLine
        }
    }

    @Composable
    override fun DrawClipped() {
        val lineColor = color.takeOrElse {
            LocalContentColor.current.takeOrElse {
                MaterialTheme.colorScheme.onSurface
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPath(
                path,
                color = lineColor,
                style = Stroke(width = lineWidth.toPx())
            )
        }
    }

    @Composable
    override fun DrawUnclipped() { }

    companion object {
        fun create(
            coordinates: Coordinates, lineWidth: Dp, color: Color,
            transformation: Transformation
        ): Line2 {
            return Line2(coordinates, lineWidth, color, transformation, Path()).apply {
                generatePath()
            }
        }
    }
}