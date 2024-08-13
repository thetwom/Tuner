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

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.toRect

data class Transformation(
    val viewPortScreen: IntRect,
    val viewPortRaw: Rect,
    val viewPortCornerRadius: Float = 0f
) {
    val matrixRawToScreen = Matrix().apply {
        translate(viewPortScreen.left.toFloat(), viewPortScreen.center.y.toFloat())
        scale(viewPortScreen.width / viewPortRaw.width, viewPortScreen.height / viewPortRaw.height)
        translate(-viewPortRaw.left, -viewPortRaw.center.y)
//        Log.v("Tuner", "Transformation: create, from ${viewPortRaw} to $viewPortScreen")
//        Log.v("Tuner", "Transformation: create, translate: ${-viewPortRaw.left}, ${-viewPortRaw.center.y}")
//
//        Log.v("Tuner", "Transformation: create, scale: ${viewPortScreen.width / viewPortRaw.width}, ${viewPortScreen.height / viewPortRaw.height}")
//
//        Log.v("Tuner", "Transformation: create, translate: ${viewPortScreen.left}, ${viewPortScreen.center.y}")
    }
//    private val matrixRawToScreen = Matrix().apply {
//        setRectToRect(viewPortRaw, viewPortScreenFloat, Matrix.ScaleToFit.FILL)
//        postScale(1f, -1f, 0f, viewPortScreenFloat.centerY())
//    }
    val matrixScreenToRaw = Matrix().apply {
        setFrom(matrixRawToScreen)
        invert()
    }

    fun toScreen(rect: Rect) = matrixRawToScreen.map(rect)
    fun toScreen(point: Offset) = matrixRawToScreen.map(point)

    fun toRaw(rect: Rect) = matrixScreenToRaw.map(rect)
    fun toRaw(point: Offset) = matrixScreenToRaw.map(point)

    fun toRaw(velocity: Velocity): Velocity {
        val result = (toRaw(Offset.Zero) - toRaw(Offset(velocity.x, velocity.y)))
        return Velocity(result.x, result.y)
    }

    @Composable
    fun rememberClipShape() = remember(viewPortScreen, viewPortCornerRadius) {
        GenericShape { _, _ ->
            val r = CornerRadius(viewPortCornerRadius)
            addRoundRect(RoundRect(viewPortScreen.toRect(), r, r, r, r))
        }
    }
}