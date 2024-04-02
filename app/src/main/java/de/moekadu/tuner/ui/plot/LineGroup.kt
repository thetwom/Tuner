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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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

class Line2(
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

//class Points(
//    initialLineWidth: Dp,
//    initialXValues: FloatArray? = null,
//    initialYValues: FloatArray? = null,
//    indexBegin: Int = 0,
//    indexEnd: Int = min(initialXValues?.size ?: 0, initialYValues?.size ?: 0),
//    initialDrawPointShape: DrawScope.(position: Offset) -> Unit
//) {
//    data class PointsArray(
//        val size: Int,
//        val points: Array<Offset>
//    ) {
//        fun setValues(
//            x: FloatArray?,
//            y: FloatArray?,
//            indexBegin: Int = 0,
//            indexEnd: Int = min(x?.size ?: 0, y?.size ?: 0)
//        ): PointsArray {
//            val newSize = indexEnd - indexBegin
//            return if (newSize == 0)
//                this.copy(size = newSize)
//            else if (size > points.size) {
//                val newArray = Array(newSize) { Offset(x!![indexBegin + it], y!![indexBegin + it]) }
//                PointsArray(newSize, newArray)
//            } else {
//                for (i in 0 until newSize)
//                    points[i] = Offset(x!![indexBegin + i], y!![indexBegin + i])
//                this.copy(size = newSize)
//            }
//        }
//
//        fun transformToScreen(
//            transformation: Transformation,
//            recycle: PointsArray
//        ): PointsArray {
//            return if (recycle.size >= size) {
//                for (i in 0 until size)
//                    recycle.points[i] = transformation.toScreen(points[i])
//                recycle.copy(size = size)
//            } else {
//                val newPoints = Array(size) { transformation.toScreen(points[it]) }
//                PointsArray(size, newPoints)
//            }
//        }
//
//        companion object {
//            fun create(
//                x: FloatArray? = null,
//                y: FloatArray? = null,
//                indexBegin: Int = 0,
//                indexEnd: Int = min(x?.size ?: 0, y?.size ?: 0)
//            ): PointsArray {
//                val size = indexEnd - indexBegin
//                val points = Array(size) {
//                    Offset(x!![indexBegin + it], y!![indexBegin + it])
//                }
//                return PointsArray(size, points)
//            }
//        }
//    }
//
//    private var drawPointShape by mutableStateOf(initialDrawPointShape)
//    private var points by mutableStateOf(
//        PointsArray.create(
//            initialXValues,
//            initialYValues,
//            indexBegin,
//            indexEnd
//        )
//    )
//    private var pointsTransformed = PointsArray.create()
//
//    private var _boundingBox = mutableStateOf(
//        Rect(//0f, 30f, 15f, 15f
//            Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
//            Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY
//        )
//    )
//
//    val boundingBox: State<Rect> get() = _boundingBox
//
//    fun setPoints(
//        xValues: FloatArray, yValues: FloatArray,
//        indexBegin: Int = 0, indexEnd: Int = min(xValues.size, yValues.size),
//        drawPointShape: (DrawScope.(position: Offset) -> Unit)? = null
//    ) {
//        if (drawPointShape != null)
//            this.drawPointShape = drawPointShape
//        points = points.setValues(xValues, yValues, indexBegin, indexEnd)
//
//        var xMin = Float.POSITIVE_INFINITY
//        var xMax = Float.NEGATIVE_INFINITY
//        var yMin = Float.POSITIVE_INFINITY
//        var yMax = Float.NEGATIVE_INFINITY
//        if (points.size > 0) {
//            xMin = points.points[0].x
//            xMax = points.points[0].x
//            yMin = points.points[0].y
//            yMax = points.points[0].y
//        }
//        for (i in 1 until points.size) {
//            xMin = min(xMin, points.points[i].x)
//            xMax = max(xMax, points.points[i].x)
//            yMin = min(yMin, points.points[i].y)
//            yMax = max(yMax, points.points[i].y)
//        }
//        _boundingBox.value = Rect(xMin, yMax, xMax, yMin)
//    }
//
//    @Composable
//    fun Draw(transformation: Transformation) {
//
//        val pointsTransformed = remember(transformation, points) {
//            // TODO: transform only points in bounding box? or do this somewhere else
//            this.pointsTransformed =
//                points.transformToScreen(transformation, this.pointsTransformed)
//            this.pointsTransformed
//        }
//
//        val maxPointExtent =
//            with(LocalDensity.current) { 4.dp.toPx() } // TODO: read this as input argument
//
//        val isVisible = remember(transformation, boundingBox.value, maxPointExtent) {
//            val bbS = transformation.toScreen(boundingBox.value).inflate(maxPointExtent)
//            bbS.overlaps(transformation.viewPortScreen.toRect())
//        }
//        if (isVisible) {
//            Canvas(
//                modifier = Modifier
//                    .fillMaxSize()
//                //.background(Color.Gray)
//            ) {
//                for (i in 0 until pointsTransformed.size)
//                    drawPointShape(pointsTransformed.points[i])
//            }
//        }
//    }
//
//    companion object {
//        fun drawCircle(color: Color, radius: Float): (DrawScope.(position: Offset) -> Unit) {
//            return {position ->
//                drawCircle(color, radius, position)
//            }
//        }
//    }
//}


class LineGroup: PlotGroup {
    private val lines = mutableMapOf<Int, Line2>()
    fun setLine(
        xValues: FloatArray, yValues: FloatArray,
        key: Int = 0,
        indexBegin: Int = 0,
        indexEnd: Int = min(xValues.size, yValues.size),
        lineWidth: Dp? = null,
        lineColor: (@Composable () -> Color)? = null
    ) {
        lines[key]?.setLine(xValues, yValues, indexBegin, indexEnd, lineWidth, lineColor)
        if (key !in lines) {
            lines[key] = Line2(
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
private fun Line2Preview() {
    TunerTheme {
        BoxWithConstraints {
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-10f, 5f, 10f, -5f)
            )

            val line = Line2(2.dp,
                initialXValues = floatArrayOf(-9f, -5f, 0f,  7f,  8f),
                initialYValues = floatArrayOf(-3f,  5f, 0f, -4f, -2f)
            )
            
            line.Draw(transformation = transformation)

            val line2 = Line2(2.dp,
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
                        xValues = floatArrayOf(-9f, -5f, 0f,  7f,  8f),
                        yValues = floatArrayOf(-3f,  5f, 0f, -4f, -2f),
                        key = 1,
                        lineWidth = 3.dp,
                        lineColor = { MaterialTheme.colorScheme.primary }
                    )
                    setLine(
                        xValues = floatArrayOf(-8f, -6f, 1f,  5f,  7f),
                        yValues = floatArrayOf(-2f,  -3f, 4f, 3f, -2f),
                        key = 2,
                        lineWidth = 1.dp,
                        lineColor = { MaterialTheme.colorScheme.error }
                    )
                }
            }
            lineGroup.Draw(transformation = transformation)
        }
    }
}