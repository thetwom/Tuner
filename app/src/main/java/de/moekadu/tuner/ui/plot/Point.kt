package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf


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
    styleKey: Int = 0
) : PlotItem {
    override val hasClippedDraw = true
    override val hasUnclippedDraw = false

    data class Style(
        val shape: @Composable PointScope.() -> Unit
    ) : PlotStyle

    private var styleKey by mutableIntStateOf(styleKey)

    var position by mutableStateOf(position)
        private set
//    var content by mutableStateOf(content ?: drawCircle(5.dp, { Color.Unspecified }))
//        private set

    fun modify(
        position: Offset? = null,
        styleKey: Int? = null
    ) {
        if (position != null)
            this.position = position
        if (styleKey != null)
            this.styleKey = styleKey
    }

    @Composable
    override fun DrawClipped(
        transformation: Transformation,
        plotStyles: ImmutableMap<Int, PlotStyle>
    ) {
        val pointTransformed = remember(transformation, position) {
            position?.let { transformation.toScreen(it) }
        }
        val plotStyle = remember(plotStyles, styleKey) {
            (plotStyles[styleKey] as? Style) ?: styleDefault
        }

        val scope = remember { PointScopeImpl() }

        val shape = plotStyle.shape
        // setup shape composable, which basically allows to initialize variables
        //  in the composable scope
        scope.shape()

        //scope.content()
        Canvas(modifier = Modifier.fillMaxSize()) {
            pointTransformed?.let { p ->
                translate(p.x, p.y) { scope.draw?.let { it() } }
            }
        }
    }

    @Composable
    override fun DrawUnclipped(
        transformation: Transformation,
        plotStyles: ImmutableMap<Int, PlotStyle>
    ) { }

    companion object {
        val styleDefault = circleShape(5.dp, Color.Unspecified)

        fun circleShape(size: Dp, color: Color = Color.Unspecified): Style {
            return Style {
                val c = color.takeOrElse { MaterialTheme.colorScheme.onSurface }
                drawShape {
                    drawCircle(c, size.toPx() / 2, Offset.Zero)
                }
            }
        }

        fun upwardTriangleShape(
            size: Dp,
            color: Color = Color.Unspecified,
            offset: DpOffset = DpOffset.Zero
        ): Style {

            return Style {
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
                drawShape {
                    drawPath(path, c)
                }
            }
        }

        fun downwardTriangleShape(
            size: Dp,
            color: Color = Color.Unspecified,
            offset: DpOffset = DpOffset.Zero
        ): Style {
            return Style {
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
                drawShape {
                    drawPath(path, c)
                }
            }
        }

        fun circleWithUpwardTriangleShape(
            size: Dp,
            color: Color = Color.Unspecified
        ): Style {
            return Style {
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
                drawShape {
                    drawPath(path, c)
                    drawCircle(c, r, Offset.Zero)
                }
            }
        }

        fun circleWithDownwardTriangleShape(
            size: Dp,
            color: Color = Color.Unspecified
        ): Style {
            return Style {
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
            val plotStyles = persistentMapOf<Int, PlotStyle>(
                0 to Point.circleShape(6.dp, MaterialTheme.colorScheme.primary),
                1 to Point.upwardTriangleShape(8.dp, MaterialTheme.colorScheme.primary, DpOffset(0.dp, (-5).dp)),
                2 to Point.downwardTriangleShape(8.dp, MaterialTheme.colorScheme.primary, DpOffset(0.dp, 5.dp)),
                3 to Point.circleWithUpwardTriangleShape(6.dp, MaterialTheme.colorScheme.primary),
                4 to Point.circleWithDownwardTriangleShape(6.dp, MaterialTheme.colorScheme.primary)
            )

            val point1 = Point(
                position = Offset(-5f, -2f),
                styleKey = 0
            )
            val point2 = Point(
                position = Offset(-5f, -2f),
                styleKey = 1
            )
            val point3 = Point(
                position = Offset(-5f, -2f),
                styleKey = 2
            )
            val point4 = Point(
                position = Offset(5f, 2f),
                styleKey = 0
            )

            val point5 = Point(
                position = Offset(2f, 0.3f),
                styleKey = 3,
            )
            val point6 = Point(
                position = Offset(-0.5f, 0.3f),
                styleKey = 4
            )
            val point7 = Point()

            point1.DrawClipped(transformation = transformation, plotStyles = plotStyles)
            point2.DrawClipped(transformation = transformation, plotStyles = plotStyles)
            point3.DrawClipped(transformation = transformation, plotStyles = plotStyles)
            point4.DrawClipped(transformation = transformation, plotStyles = plotStyles)
            point5.DrawClipped(transformation = transformation, plotStyles = plotStyles)
            point6.DrawClipped(transformation = transformation, plotStyles = plotStyles)
            point7.DrawClipped(transformation = transformation, plotStyles = plotStyles)
        }
    }
}