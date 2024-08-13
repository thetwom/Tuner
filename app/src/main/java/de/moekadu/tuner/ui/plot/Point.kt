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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset

class PointShape {
    companion object {
        @Composable
        fun circle(size: Dp, color: Color = Color.Unspecified): (DrawScope.() -> Unit) {
            val c = color.takeOrElse { MaterialTheme.colorScheme.onSurface }
            return  { drawCircle(c, size.toPx() / 2, Offset.Zero) }
        }

        @Composable
        fun upwardTriangleShape(size: Dp, color: Color, offset: DpOffset = DpOffset.Zero)
                : (DrawScope.() -> Unit) {
            val c = color.takeOrElse { MaterialTheme.colorScheme.onSurface }
            val r = with(LocalDensity.current) { size.toPx() / 2 }
            val dx = with(LocalDensity.current) { offset.x.toPx() }
            val dy = with(LocalDensity.current) { offset.y.toPx() }

            val path = remember {
                Path().also {
                    it.moveTo(-r + dx, dy)
                    it.lineTo(r + dx, dy)
                    it.lineTo(dx, -r + dy)
                }
            }
            return { drawPath(path, c) }
        }

        @Composable
        fun downwardTriangleShape(size: Dp, color: Color, offset: DpOffset = DpOffset.Zero)
                : (DrawScope.() -> Unit) {
            val c = color.takeOrElse { MaterialTheme.colorScheme.onSurface }
            val r = with(LocalDensity.current) { size.toPx() / 2 }
            val dx = with(LocalDensity.current) { offset.x.toPx() }
            val dy = with(LocalDensity.current) { offset.y.toPx() }

            val path = remember {
                Path().also {
                    it.moveTo(-r + dx, dy)
                    it.lineTo(r + dx, dy)
                    it.lineTo(dx, r + dy)
                }
            }
            return { drawPath(path, c) }
        }

        @Composable
        fun circleWithUpwardTriangleShape(size: Dp, color: Color = Color.Unspecified)
                : (DrawScope.() -> Unit) {
            val c = color.takeOrElse { MaterialTheme.colorScheme.onSurface }
            val r = with(LocalDensity.current) { size.toPx() / 2 }
            val rTri = 1.4f * r
            val dy = -1.6f * r
            val path = remember {
                Path().also {
                    it.moveTo(-rTri, dy)
                    it.lineTo(rTri, dy)
                    it.lineTo(0f, -rTri + dy)
                }
            }
            return {
                drawPath(path, c)
                drawCircle(c, r, Offset.Zero)
            }
        }

        @Composable
        fun circleWithDownwardTriangleShape(size: Dp, color: Color = Color.Unspecified)
                : (DrawScope.() -> Unit) {
            val c = color.takeOrElse { MaterialTheme.colorScheme.onSurface }
            val r = with(LocalDensity.current) { size.toPx() / 2 }
            val rTri = 1.4f * r
            val dy = 1.6f * r

            val path = remember {
                Path().also {
                    it.moveTo(-rTri, dy)
                    it.lineTo(rTri, dy)
                    it.lineTo(0f, rTri + dy)
                }
            }
            return {
                drawPath(path, c)
                drawCircle(c, r, Offset.Zero)
            }
        }
    }
}

@Composable
fun Point(
    position: Offset,
    shape: DrawScope.() -> Unit,
    transformation: () -> Transformation
) {
    Spacer(modifier = Modifier
        .fillMaxSize()
        .drawWithCache {
            onDrawBehind {
                val p = transformation().toScreen(position)
                translate(p.x, p.y) {
                    this.shape()
                }
            }
        }
    )
}