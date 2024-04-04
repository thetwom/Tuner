package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.draw.clip
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
import kotlin.math.max
import kotlin.math.min

private class WrappedPath2(val path: Path = Path())

class Line(
    initialLineWidth: Dp,
    initialXValues: FloatArray? = null,
    initialYValues: FloatArray? = null,
    initialLineColor: @Composable () -> Color = { Color.Unspecified },
    indexBegin: Int = 0,
    indexEnd: Int = min(initialXValues?.size ?: 0, initialYValues?.size ?: 0)
) {
    private var lineColor by mutableStateOf(initialLineColor)
    private var lineWidth by mutableStateOf(initialLineWidth)
    private var pathRaw by mutableStateOf(WrappedPath2())
    private val pathTransformed = WrappedPath2()

    private var _boundingBox = mutableStateOf(
        Rect(//0f, 30f, 15f, 15f
            Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY
        )
    )

    val boundingBox: State<Rect> get() = _boundingBox

    init {
        if (initialXValues != null && initialYValues != null) {
            setLine(
                xValues = initialXValues,
                yValues = initialYValues,
                indexBegin = indexBegin,
                indexEnd = indexEnd,
                lineWidth = initialLineWidth
            )
        }
    }

    fun setLine(
        xValues: FloatArray, yValues: FloatArray,
        indexBegin: Int = 0, indexEnd: Int = min(xValues.size, yValues.size),
        lineWidth: Dp? = null,
        lineColor: (@Composable () -> Color)? = null
    ) {
        if (lineColor != null)
            this.lineColor = lineColor
        if (lineWidth != null)
            this.lineWidth = lineWidth
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
        for (i in indexBeginResolved + 1 until indexEndResolved) {
            xMin = min(xMin, xValues[i])
            xMax = max(xMax, xValues[i])
            yMin = min(yMin, yValues[i])
            yMax = max(yMax, yValues[i])
            path.lineTo(xValues[i], yValues[i])
        }
        pathRaw = WrappedPath2(path)
        _boundingBox.value = Rect(xMin, yMax, xMax, yMin)
    }

    @Composable
    fun Draw(transformation: Transformation) {
        val lineColor = this.lineColor().takeOrElse {
            LocalContentColor.current.takeOrElse {
                MaterialTheme.colorScheme.onSurface
            }
        }

        val pathTransformed = remember(transformation, pathRaw) {
            val path = this.pathTransformed.path
            path.rewind()
            path.addPath(pathRaw.path)
            path.transform(transformation.matrixRawToScreen)
            WrappedPath2(path)
        }

        val lineWidthPx = with(LocalDensity.current) { lineWidth.toPx() }

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
}

class LineGroup: PlotGroup {
    private val lines = mutableMapOf<Int, Line>()
    fun setLine(
        key: Int,
        xValues: FloatArray, yValues: FloatArray,
        indexBegin: Int = 0,
        indexEnd: Int = min(xValues.size, yValues.size),
        lineWidth: Dp? = null,
        lineColor: (@Composable () -> Color)? = null
    ) {
        lines[key]?.setLine(xValues, yValues, indexBegin, indexEnd, lineWidth, lineColor)
        if (key !in lines) {
            lines[key] = Line(
                lineWidth ?: 2.dp,
                xValues, yValues,
                lineColor ?: { Color.Unspecified },
                indexBegin, indexEnd
            )
        }
    }

    @Composable
    override fun Draw(transformation: Transformation) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(transformation.rememberClipShape())
        ) {
            for (l in lines.values)
                l.Draw(transformation)
        }
    }
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

            val line = Line(2.dp,
                initialXValues = floatArrayOf(-9f, -5f, 0f,  7f,  8f),
                initialYValues = floatArrayOf(-3f,  5f, 0f, -4f, -2f)
            )
            
            line.Draw(transformation = transformation)

            val line2 = Line(2.dp,
                initialXValues = floatArrayOf(-9f, -5f, 0f,  7f,  8f),
                initialYValues = floatArrayOf(-30f,  -50f, -30f, -40f, -20f)
            )

            line2.Draw(transformation = transformation)
        }
    }
}

@Preview(widthDp = 100, heightDp = 50, showBackground = true)
@Composable
private fun LineGroupPreview() {
    TunerTheme {
        BoxWithConstraints {
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-10f, 5f, 10f, -5f)
            )

            val lineGroup = remember {
                LineGroup().apply {
                    setLine(
                        key = 1,
                        xValues = floatArrayOf(-9f, -5f, 0f,  7f,  8f),
                        yValues = floatArrayOf(-3f,  5f, 0f, -4f, -2f),
                        lineWidth = 3.dp,
                        lineColor = { MaterialTheme.colorScheme.primary }
                    )
                    setLine(
                        key = 2,
                        xValues = floatArrayOf(-8f, -6f, 1f,  5f,  7f),
                        yValues = floatArrayOf(-2f,  -3f, 4f, 3f, -2f),
                        lineWidth = 1.dp,
                        lineColor = { MaterialTheme.colorScheme.error }
                    )
                }
            }
            lineGroup.Draw(transformation = transformation)
        }
    }
}