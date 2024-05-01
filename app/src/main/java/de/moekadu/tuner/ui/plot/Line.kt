package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toRect
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlin.math.max
import kotlin.math.min

private class WrappedPath(val path: Path = Path())

class Line(
    coordinates: Coordinates? = null,
    styleKey: Int = 0
) : PlotItem {
    data class Style(
        val color: Color,
        val lineWidth: Dp
    ) : PlotStyle

    class Coordinates(
        val xValues: FloatArray, val yValues: FloatArray,
        val indexBegin: Int = 0, val indexEnd: Int = min(xValues.size, yValues.size)
    )
    override val hasClippedDraw = true
    override val hasUnclippedDraw = false

    private var styleKey by mutableIntStateOf(styleKey)

    private var pathRaw by mutableStateOf(WrappedPath())
    private val pathTransformed = WrappedPath()

    private var _boundingBox = mutableStateOf(
        Rect(//0f, 30f, 15f, 15f
            Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY
        )
    )

    private val boundingBox: State<Rect> get() = _boundingBox

    init {
        coordinates?.let { modify(it) }
    }

    fun modify(
        coordinates: Coordinates? = null,
        styleKey: Int? = null
    ) {
        if (styleKey != null)
            this.styleKey = styleKey
        if (coordinates != null) {
            val xValues = coordinates.xValues
            val yValues = coordinates.yValues
            val numAvailableValues = min(xValues.size, yValues.size)
            val indexBeginResolved = max(0, coordinates.indexBegin)

            val indexEndResolved = min(coordinates.indexEnd, numAvailableValues)
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
            for (i in indexBeginResolved + 1 until indexEndResolved) {
                xMin = min(xMin, xValues[i])
                xMax = max(xMax, xValues[i])
                yMin = min(yMin, yValues[i])
                yMax = max(yMax, yValues[i])
                path.lineTo(xValues[i], yValues[i])
            }
            pathRaw = WrappedPath(path)
            _boundingBox.value = Rect(xMin, yMax, xMax, yMin)
        }
    }

    @Composable
    override fun DrawClipped(
        transformation: Transformation,
        plotStyles: ImmutableMap<Int, PlotStyle>
    ) {
        val plotStyle = remember(plotStyles, styleKey) {
            plotStyles[styleKey] as? Style
        }
        val lineColor = (plotStyle?.color ?: Color.Unspecified).takeOrElse {
            LocalContentColor.current.takeOrElse {
                MaterialTheme.colorScheme.onSurface
            }
        }
        val lineWidth = plotStyle?.lineWidth ?: 1.dp
        val lineWidthPx = with(LocalDensity.current) { lineWidth.toPx() }
        
        val pathTransformed = remember(transformation, pathRaw) {
            val path = this.pathTransformed.path
            path.rewind()
            path.addPath(pathRaw.path)
            path.transform(transformation.matrixRawToScreen)
            WrappedPath(path)
        }

        val isVisible = remember(transformation, boundingBox.value, lineWidthPx) {
            val bbS = transformation.toScreen(boundingBox.value).inflate(0.5f * lineWidthPx)
            bbS.overlaps(transformation.viewPortScreen.toRect())
        }
        if (isVisible) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                //.background(Color.Gray)
            ) {
                drawPath(
                    pathTransformed.path,
                    color = lineColor,
                    style = Stroke(width = lineWidth.toPx())
                )
            }
        }
    }

    @Composable
    override fun DrawUnclipped(
        transformation: Transformation,
        plotStyles: ImmutableMap<Int, PlotStyle>
    ) { }
}

@Composable
private fun rememberTransformation(
    screenWidth: Dp, screenHeight: Dp,
    viewPortRaw: Rect
): Transformation {
    val widthPx = with(LocalDensity.current) { screenWidth.roundToPx() }
    val heightPx = with(LocalDensity.current) { screenHeight.roundToPx() }

    val transformation = remember(widthPx, heightPx, viewPortRaw) {
        Transformation(IntRect(0, 0, widthPx, heightPx), viewPortRaw)
    }
    return transformation
}
@Preview(widthDp = 100, heightDp = 50, showBackground = true)
@Composable
private fun LinePreview() {
    TunerTheme {
        BoxWithConstraints {
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-10f, 5f, 10f, -5f)
            )
            val plotStyles = persistentMapOf<Int, PlotStyle>(
                0 to Line.Style(MaterialTheme.colorScheme.error, 2.dp),
                1 to Line.Style(MaterialTheme.colorScheme.primary, 1.dp),
            )

            val line = Line(
                Line.Coordinates(
                    xValues = floatArrayOf(-9f, -5f, 0f,  7f,  8f),
                    yValues = floatArrayOf(-3f,  5f, 0f, -4f, -2f)
                ),
                styleKey = 0
            )
            
            line.DrawClipped(transformation = transformation, plotStyles = plotStyles)

            val line2 = Line(
                Line.Coordinates(
                    xValues =  floatArrayOf(-9f, -5f, 0f,  7f,  8f),
                    yValues =  floatArrayOf(-30f,  -50f, -30f, -40f, -20f)
                ),
                styleKey = 1
            )

            line2.DrawClipped(transformation = transformation, plotStyles = plotStyles)
        }
    }
}
