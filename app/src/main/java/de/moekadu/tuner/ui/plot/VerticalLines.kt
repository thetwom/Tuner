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

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
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

class VerticalLinesPositions(
    val x: MutableList<Float> = mutableListOf()
) {
    fun mutate(size: Int, x: (i: Int) -> Float): VerticalLinesPositions {
        if (this.x.size == size) {
            for (i in 0 until size)
                this.x[i] = x(i)
        } else {
            this.x.clear()
            for (i in 0 until size)
                this.x.add(x(i))
        }
        return VerticalLinesPositions(this.x)
    }

    fun mutate(x: FloatArray, from: Int = 0, to: Int = x.size): VerticalLinesPositions {
        val size = to - from
        if (this.x.size == size) {
            for (i in 0 until size)
                this.x[i] = x[i+from]
        } else {
            this.x.clear()
            for (i in 0 until size)
                this.x.add(x[i+from])
        }
        return VerticalLinesPositions(this.x)
    }
    companion object {
        fun create(size: Int, x: (i: Int) -> Float): VerticalLinesPositions {
            return VerticalLinesPositions(MutableList(size){ x(it) })
        }
        fun create(x: FloatArray): VerticalLinesPositions {
            return VerticalLinesPositions(x.toMutableList())
        }
    }
}

//data class VerticalLinesPositions(
//    val size: Int,
//    val x: (i: Int) -> Float
//)

//private data class VerticalLineCache(
//    private var positions: VerticalLinesPositions,
//    private var transformation: Transformation
//) {
//    val cachedPositions
//
//    init {
//        _update(coordinates, transformation, init = true)
//    }
//
//    fun update(coordinates: LineCoordinates, transformation: Transformation) {
//        _update(coordinates, transformation, init = false)
//    }
//    private fun _update(coordinates: LineCoordinates, transformation: Transformation, init: Boolean) {
//        if (coordinates == this.coordinates && transformation == this.transformation && !init)
//            return
//        this.coordinates = coordinates
//        this.transformation = transformation
//        path.rewind()
//
//        path.rewind()
//        if (coordinates.size > 0) {
//            path.moveTo(coordinates.x(0), coordinates.y(0))
//        }
//
//        for (i in 1 until coordinates.size) {
//            path.lineTo(coordinates.x(i), coordinates.y(i))
//        }
//        path.transform(transformation.matrixRawToScreen)
//    }
//}

@Composable
fun VerticalLines(
    data: VerticalLinesPositions,
    color: Color,
    width: Dp,
    transformation: () -> Transformation
) {
    val c = color.takeOrElse { MaterialTheme.colorScheme.onSurface }

    Spacer(modifier = Modifier
        .fillMaxSize()
        .drawBehind {
            for (i in 0 until data.x.size) {
                val transform = transformation()
                val x = transform.toScreen(Offset(data.x[i], 0f)).x
                drawLine(
                    c,
                    Offset(x, transform.viewPortScreen.top.toFloat()),
                    Offset(x, transform.viewPortScreen.bottom.toFloat()),
                    strokeWidth = width.toPx()
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
private fun VerticalLinePreview() {
    TunerTheme {
        BoxWithConstraints {
            val x = remember { floatArrayOf(0f, 1f, 2f, 3f, 4f) }
            val positions = remember {
                VerticalLinesPositions.create(x)//(
                    //size = 5, x = { x[it] }
                //)
            }
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-1f, 5f, 5f, -3f)
            )

            VerticalLines(positions, MaterialTheme.colorScheme.primary, 2.dp, { transformation })
        }
    }
}


