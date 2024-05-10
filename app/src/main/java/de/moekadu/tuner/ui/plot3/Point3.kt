package de.moekadu.tuner.ui.plot3

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
import de.moekadu.tuner.ui.plot.Transformation

class Point3Shape {
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

//interface Point3Scope {
//    fun drawShape(draw: DrawScope.() -> Unit)
//}
//
//private class Point3ScopeImpl : Point3Scope {
//    var draw: (DrawScope.() -> Unit)? = null
//    override fun drawShape(draw: DrawScope.() -> Unit) {
//        this.draw = draw
//    }
//}

@Composable
fun Point3(
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