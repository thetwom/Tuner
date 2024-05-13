package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
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
import de.moekadu.tuner.ui.theme.TunerTheme

data class Line3Coordinates(
    val size: Int,
    val x: (i: Int) -> Float,
    val y: (i: Int) -> Float,
)
//data class Line3Coordinates(
//    val xValues: FloatArray, val yValues: FloatArray,
//    val indexBegin: Int = 0, val indexEnd: Int = min(xValues.size, yValues.size)
//)

private data class Line3Cache(
    private var coordinates: Line3Coordinates,
    private var transformation: Transformation
) {
    val path = Path()

    init {
        _update(coordinates, transformation, init = true)
    }

    fun update(coordinates: Line3Coordinates, transformation: Transformation) {
        _update(coordinates, transformation, init = false)
    }
    private fun _update(coordinates: Line3Coordinates, transformation: Transformation, init: Boolean) {
        if (coordinates == this.coordinates && transformation == this.transformation && !init)
            return
        this.coordinates = coordinates
        this.transformation = transformation
        path.rewind()
//        val xValues = coordinates.xValues
//        val yValues = coordinates.yValues
//        val numAvailableValues = min(xValues.size, yValues.size)
//        val indexBeginResolved = max(0, coordinates.indexBegin)
//
//        val indexEndResolved = min(coordinates.indexEnd, numAvailableValues)
//        val numValuesResolved = max(0, indexEndResolved - indexBeginResolved)

        path.rewind()
//        if (numValuesResolved > 0) {
        if (coordinates.size > 0) {
//            Log.v("Tuner", "Line: move to ${xValues[0]}, ${yValues[0]}")
            //path.moveTo(xValues[0], yValues[0])
            path.moveTo(coordinates.x(0), coordinates.y(0))
        }
        //for (i in indexBeginResolved + 1 until indexEndResolved) {
        for (i in 1 until coordinates.size) {
            //path.lineTo(xValues[i], yValues[i])
            path.lineTo(coordinates.x(i), coordinates.y(i))
        }
        path.transform(transformation.matrixRawToScreen)
    }
}
@Composable
fun Line3(
    data: Line3Coordinates,
    color: Color,
    width: Dp,
    transformation: () -> Transformation
) {
    val c = color.takeOrElse { MaterialTheme.colorScheme.onSurface }

    Spacer(modifier = Modifier
        .fillMaxSize()
        .drawWithCache {
            val cachedData = Line3Cache(data, transformation())
            onDrawBehind {
                cachedData.update(data, transformation())
                drawPath(
                    cachedData.path,
                    color = c,
                    style = Stroke(width = width.toPx())
                )
            }
        }
    )
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

@Preview(widthDp = 200, heightDp = 200, showBackground = true)
@Composable
private fun Line3Preview() {
    TunerTheme {
        BoxWithConstraints {
            val x = remember { floatArrayOf(0f, 1f, 2f, 3f, 4f) }
            val y = remember { floatArrayOf(3f, 1f, 2f, -2f, 0f) }
            val coords = remember {
                Line3Coordinates(
                    size = 5, x = { x[it] }, y = { y[it] }
                )
            }
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-1f, 5f, 5f, -3f)
            )

            Line3(coords, MaterialTheme.colorScheme.primary, 2.dp, { transformation })
        }
    }
}


