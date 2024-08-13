/*
* Copyright 2024 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.moekadu.tuner.ui.plot

import android.util.Log
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

//data class LineCoordinates(
//    val size: Int,
//    val x: (i: Int) -> Float,
//    val y: (i: Int) -> Float,
//)


class LineCoordinates(
    val coordinates: MutableList<Offset> = mutableListOf()
) {
    val size get() = coordinates.size

    fun mutate(size: Int, x: (i: Int) -> Float, y: (i: Int) -> Float): LineCoordinates {
        return if (this.size == size) {
            for (i in 0 until size)
                coordinates[i] = Offset(x(i), y(i))
            LineCoordinates(coordinates)
        } else {
            create(size, x, y)
        }
    }

    fun mutate(x: FloatArray, y: FloatArray): LineCoordinates {
        return if (x.size == size) {
            for (i in 0 until size)
                coordinates[i] = Offset(x[i], y[i])
            LineCoordinates(coordinates)
        } else {
            create(x, y)
        }
    }

    fun mutate(y: FloatArray): LineCoordinates {
        return if (coordinates.size == size) {
            for (i in 0 until size)
                coordinates[i] = Offset(i.toFloat(), y[i])
            LineCoordinates(coordinates)
        } else {
            create(y)
        }
    }

    companion object {
        fun create(size: Int, x: (i: Int) -> Float, y: (i: Int) -> Float): LineCoordinates {
            return LineCoordinates(MutableList(size){ Offset(x(it), y(it)) })
        }
        fun create(x: FloatArray, y: FloatArray): LineCoordinates {
            return LineCoordinates(MutableList(x.size){ Offset(x[it], y[it]) })
        }
        fun create(y: FloatArray): LineCoordinates {
            return LineCoordinates(MutableList(y.size){ Offset(it.toFloat(), y[it]) })
        }
    }
}

private fun findCoordinateIndexBegin(xMin: Float, coordinates: List<Offset>): Int {
    var iMin = coordinates.binarySearchBy(xMin) { it.x }
    if (iMin < 0)
        iMin = max(0, -iMin - 2)
    return iMin
}
private fun findCoordinateIndexEnd(xMax: Float, coordinates: List<Offset>): Int {
    var iMax = coordinates.binarySearchBy(xMax) { it.x }
    if (iMax < 0)
        iMax = -iMax-1
    return iMax
}

private data class LineCache(
    private var coordinates: LineCoordinates,
    private var transformation: Transformation
) {
    //val path = Path()
    private var numCoordinates = 0
    private var coordinatesScreen = mutableListOf<Offset>()

    init {
        _update(coordinates, transformation, init = true)
    }

    fun update(coordinates: LineCoordinates, transformation: Transformation): List<Offset> {
        return _update(coordinates, transformation, init = false)
    }

    private fun _update(coordinates: LineCoordinates, transformation: Transformation, init: Boolean)
            :List<Offset>{
        if (coordinates == this.coordinates && transformation == this.transformation && !init)
            return coordinatesScreen.subList(0, numCoordinates)
        this.coordinates = coordinates
        this.transformation = transformation

        //path.rewind()
        val rawMin = min(transformation.viewPortRaw.right, transformation.viewPortRaw.left)
        val rawMax = max(transformation.viewPortRaw.right, transformation.viewPortRaw.left)
        val iBegin = findCoordinateIndexBegin(rawMin, coordinates.coordinates)
        val iEnd = findCoordinateIndexEnd(rawMax, coordinates.coordinates)
        numCoordinates = iEnd - iBegin
        if (numCoordinates > coordinatesScreen.size) {
            coordinatesScreen = MutableList(numCoordinates) {
                transformation.toScreen(coordinates.coordinates[iBegin + it])
            }
        } else {
            for (i in 0 until numCoordinates)
                coordinatesScreen[i] = transformation.toScreen(coordinates.coordinates[iBegin + i])
        }
//        if (coordinates.size > 0) {
//            path.moveTo(coordinates.x(0), coordinates.y(0))
//        }
//
//        for (i in 1 until coordinates.size) {
//            path.lineTo(coordinates.x(i), coordinates.y(i))
//        }
//        path.transform(transformation.matrixRawToScreen)
        return coordinatesScreen.subList(0, numCoordinates)
    }
}
@Composable
fun Line(
    data: LineCoordinates,
    brush: Brush,
    width: Dp,
    transformation: () -> Transformation
) {
    Spacer(modifier = Modifier
        .fillMaxSize()
        .drawWithCache {
            val cachedData = LineCache(data, transformation())
            onDrawBehind {
                val points = cachedData.update(data, transformation())
                drawPoints(
                    points,
                    PointMode.Polygon, //PointMode.Lines,
                    brush = brush,
                    strokeWidth = width.toPx()
                )
//                drawPath(
//                    cachedData.path,
//                    color = c,
//                    style = Stroke(width = width.toPx())
//                )
            }
        }
    )
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
                val points = cachedData.update(data, transformation())
                drawPoints(
                    points,
                    PointMode.Polygon, //PointMode.Lines,
                    color = c,
                    strokeWidth = width.toPx()
                )
//                drawPath(
//                    cachedData.path,
//                    color = c,
//                    style = Stroke(width = width.toPx())
//                )
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
            val coords = remember { LineCoordinates.create(
                size = 5, x = { x[it] }, y = { y[it] }
            )
//                LineCoordinates(
//                    size = 5, x = { x[it] }, y = { y[it] }
//                )
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
private fun LineBrushPreview() {
    TunerTheme {
        BoxWithConstraints {
            val x = remember { floatArrayOf(0f, 1f, 2f, 3f, 4f) }
            val y = remember { floatArrayOf(3f, 1f, 2f, -2f, 0f) }
            val coords = remember { LineCoordinates.create(
                size = 5, x = { x[it] }, y = { y[it] }
            )
//                LineCoordinates(
//                    size = 5, x = { x[it] }, y = { y[it] }
//                )
            }
            val brush = Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.error,
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.primary,
                ),
            )
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-1f, 5f, 5f, -3f)
            )

            Line(coords, brush, 5.dp, { transformation })
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
                    LineCoordinates.create(
                        size = numValues, x = { x[it] }, y = { y[it] }
                    )
//                    LineCoordinates(
//                        size = numValues, x = { x[it] }, y = { y[it] }
//                    )
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


