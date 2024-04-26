package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.ui.theme.TunerTheme


interface PointScope {
    fun drawShape(draw: DrawScope.() -> Unit)
}

class PointScopeImpl : PointScope {
    var draw: (DrawScope.() -> Unit)? = null
    override fun drawShape(draw: DrawScope.() -> Unit) {
        this.draw = draw
    }
}

class Point(
    position: Offset? = null,
    content: (@Composable PointScope.() -> Unit)? = null,
) : PlotItem {
    override val hasClippedDraw = true
    override val hasUnclippedDraw = false

    var position by mutableStateOf(position)
        private set
    var content by mutableStateOf(content ?: drawCircle(5.dp, { Color.Unspecified }))
        private set

    fun modify(
        position: Offset? = null,
        content: (@Composable PointScope.() -> Unit)? = null,
    ) {
        if (position != null)
            this.position = position
        if (content != null)
            this.content = content
    }

    @Composable
    override fun DrawClipped(transformation: Transformation) {
        val pointTransformed = remember(transformation, position) {
            position?.let { transformation.toScreen(it) }
        }
        val scope = remember { PointScopeImpl() }
        scope.content()
        Canvas(modifier = Modifier.fillMaxSize()) {
            pointTransformed?.let { p ->
                translate(p.x, p.y) { scope.draw?.let { it() } }
            }
        }
    }

    @Composable
    override fun DrawUnclipped(transformation: Transformation) { }

    companion object {
        fun drawCircle(size: Dp, color: @Composable () -> Color)
                : (@Composable PointScope.() -> Unit) {
            return {
                val c = color()
                drawShape {
                    drawCircle(c, size.toPx() / 2, Offset.Zero)
                }
            }
        }

        fun drawUpwardTriangle(
            size: Dp,
            color: @Composable () -> Color,
            offset: DpOffset = DpOffset.Zero
        )
                : (@Composable PointScope.() -> Unit) {
            return {
                val c = color()
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
                drawShape {
                    drawPath(path, c)
                }
            }
        }

        fun drawDownwardTriangle(
            size: Dp,
            color: @Composable () -> Color,
            offset: DpOffset = DpOffset.Zero
        )
                : (@Composable PointScope.() -> Unit) {
            return {
                val c = color()
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
                drawShape {
                    drawPath(path, c)
                }
            }
        }

        fun drawCircleWithUpwardTriangle(
            size: Dp,
            color: @Composable () -> Color,
        )
                : (@Composable PointScope.() -> Unit) {
            return {
                val c = color()
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
                drawShape {
                    drawPath(path, c)
                    drawCircle(c, r, Offset.Zero)
                }
            }
        }

        fun drawCircleWithDownwardTriangle(
            size: Dp,
            color: @Composable () -> Color,
        )
                : (@Composable PointScope.() -> Unit) {
            return {
                val c = color()
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
                drawShape {
                    drawPath(path, c)
                    drawCircle(c, r, Offset.Zero)
                }
            }
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
private fun PointPreview() {
    TunerTheme {
        BoxWithConstraints {
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-10f, 5f, 10f, -5f)
            )

            val point1 = Point(
                position = Offset(-5f, -2f),
                content = Point.drawCircle(6.dp) { MaterialTheme.colorScheme.primary }
            )
            val point2 = Point(
                position = Offset(-5f, -2f),
                content = Point.drawUpwardTriangle(
                    8.dp,
                    { MaterialTheme.colorScheme.primary },
                    DpOffset(0.dp, (-5).dp)
                )
            )
            val point3 = Point(
                position = Offset(-5f, -2f),
                content = Point.drawDownwardTriangle(
                    8.dp,
                    { MaterialTheme.colorScheme.primary },
                    DpOffset(0.dp, 5.dp)
                )
            )
            val point4 = Point(
                position = Offset(5f, 2f),
                content = Point.drawCircle(6.dp) { MaterialTheme.colorScheme.primary }
            )

            val point5 = Point(
                position = Offset(2f, 0.3f),
                content = Point.drawCircleWithUpwardTriangle(6.dp) { MaterialTheme.colorScheme.primary }
            )
            val point6 = Point(
                position = Offset(-0.5f, 0.3f),
                content = Point.drawCircleWithDownwardTriangle(6.dp) { MaterialTheme.colorScheme.primary }
            )
            val point7 = Point()

            point1.DrawClipped(transformation = transformation)
            point2.DrawClipped(transformation = transformation)
            point3.DrawClipped(transformation = transformation)
            point4.DrawClipped(transformation = transformation)
            point5.DrawClipped(transformation = transformation)
            point6.DrawClipped(transformation = transformation)
            point7.DrawClipped(transformation = transformation)
        }
    }
}