package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlin.random.Random

data class LineCoordinates(
    val size: Int,
    val x: (i: Int) -> Float,
    val y: (i: Int) -> Float,
)
//data class Line3Coordinates(
//    val xValues: FloatArray, val yValues: FloatArray,
//    val indexBegin: Int = 0, val indexEnd: Int = min(xValues.size, yValues.size)
//)

private data class LineCache(
    private var coordinates: LineCoordinates,
    private var transformation: Transformation
) {
    val path = Path()

    init {
        _update(coordinates, transformation, init = true)
    }

    fun update(coordinates: LineCoordinates, transformation: Transformation) {
        _update(coordinates, transformation, init = false)
    }
    private fun _update(coordinates: LineCoordinates, transformation: Transformation, init: Boolean) {
        if (coordinates == this.coordinates && transformation == this.transformation && !init)
            return
        this.coordinates = coordinates
        this.transformation = transformation

        path.rewind()
        if (coordinates.size > 0) {
            path.moveTo(coordinates.x(0), coordinates.y(0))
        }

        for (i in 1 until coordinates.size) {
            path.lineTo(coordinates.x(i), coordinates.y(i))
        }
        path.transform(transformation.matrixRawToScreen)
    }
}
@Composable
fun Line(
    data: LineCoordinates,
    color: Color,
    width: Dp,
    transformation: () -> Transformation
) {
    val c = color.takeOrElse { MaterialTheme.colorScheme.onSurface }

    Spacer(modifier = Modifier
        .fillMaxSize()
        .drawWithCache {
            val cachedData = LineCache(data, transformation())
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
private fun LinePreview() {
    TunerTheme {
        BoxWithConstraints {
            val x = remember { floatArrayOf(0f, 1f, 2f, 3f, 4f) }
            val y = remember { floatArrayOf(3f, 1f, 2f, -2f, 0f) }
            val coords = remember {
                LineCoordinates(
                    size = 5, x = { x[it] }, y = { y[it] }
                )
            }
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-1f, 5f, 5f, -3f)
            )

            Line(coords, MaterialTheme.colorScheme.primary, 2.dp, { transformation })
        }
    }
}

@Preview(widthDp = 200, heightDp = 200, showBackground = true)
@Composable
private fun LinePreview2() {
    TunerTheme {
        val numValues = 500
        val x = remember { FloatArray(numValues) { it.toFloat() } }
        var y by remember { mutableStateOf(FloatArray(numValues) { Random.nextFloat() }) }
        Column {
            Button(onClick = {
                y = FloatArray(numValues) { Random.nextFloat() }
            }) {
                Text("New values")
            }
            BoxWithConstraints {


                val coords = remember {
                    LineCoordinates(
                        size = numValues, x = { x[it] }, y = { y[it] }
                    )
                }
                val transformation = rememberTransformation(
                    screenWidth = maxWidth,
                    screenHeight = maxHeight,
                    viewPortRaw = Rect(0f, 1f, 0.01f*numValues.toFloat(), 0f)
                )

                Line(coords,
                    MaterialTheme.colorScheme.primary,
                    2.dp,
                    { transformation }
                )
            }
        }
    }
}


