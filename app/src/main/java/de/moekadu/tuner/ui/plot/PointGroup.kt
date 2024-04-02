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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.ui.theme.TunerTheme

class Point(
    initialPosition: Offset,
    initialDrawPointShape: DrawScope.(position: Offset, path: Path, color: Color) -> Unit,
    initialColor: @Composable (position: Offset) -> Color
) {
    var position by mutableStateOf(initialPosition)
        private set
    var drawPointShape by mutableStateOf(initialDrawPointShape)
        private set

    var color by mutableStateOf(initialColor)
        private set
    fun setPoint(
        position: Offset? = null,
        drawPointShape: (DrawScope.(position: Offset, path: Path, color: Color) -> Unit)? = null,
        color: (@Composable (position: Offset) -> Color)? = null
    ) {
        if (position != null)
            this.position = position
        if (drawPointShape != null)
            this.drawPointShape = drawPointShape
        if (color != null)
            this.color = color
    }
}

class PointGroup : PlotGroup {
    private val points = mutableMapOf<Int, Point>()

    fun setPoint(
        position: Offset,
        key: Int = 0,
        drawPointShape: DrawScope.(position: Offset, path: Path, color: Color) -> Unit,
        color: @Composable (position: Offset) -> Color
    ) {
        points[key]?.setPoint(position, drawPointShape, color)
        if (key !in points) {
            points[key] = Point(position, drawPointShape, color)
        }
    }

    @Composable
    override fun Draw(transformation: Transformation) {
        val path = remember { Path() }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
            //.background(Color.Gray)
        ) {
            for (point in points.values) {
                val transformed = transformation.toScreen(point.position)
                val func = point.drawPointShape
                func(transformed, path)
            }
        }
    }

    companion object {
        fun drawCircle(size: Dp, color: Color)
                : (DrawScope.(position: Offset, path: Path) -> Unit) {
            return { p, _ -> drawCircle(color, size.toPx() / 2, p) }
        }

        fun drawUpwardTriangle(size: Dp, color: Color, offset: DpOffset = DpOffset.Zero)
                : (DrawScope.(position: Offset, path: Path) -> Unit) {
            return {p, path ->
                val r = size.toPx() / 2
                val dx = offset.x.toPx()
                val dy = offset.y.toPx()
                path.rewind()
                path.moveTo(p.x - r + dx, p.y + dy)
                path.lineTo(p.x + r + dx, p.y + dy)
                path.lineTo(p.x + dx, p.y - r + dy)
                drawPath(path, color)
            }
        }

        fun drawDownwardTriangle(size: Dp, color: Color, offset: DpOffset = DpOffset.Zero)
                : (DrawScope.(position: Offset, path: Path) -> Unit) {
            return {p, path ->
                val r = size.toPx() / 2
                val dx = offset.x.toPx()
                val dy = offset.y.toPx()
                path.rewind()
                path.moveTo(p.x - r + dx, p.y + dy)
                path.lineTo(p.x + r + dx, p.y + dy)
                path.lineTo(p.x + dx, p.y + r + dy)
                drawPath(path, color)
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
private fun PointGroupPreview() {
    TunerTheme {
        BoxWithConstraints {
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-10f, 5f, 10f, -5f)
            )

            val pointGroup = remember {
                PointGroup().also {
                    it.setPoint(
                        position = Offset(-5f, -2f),
                        key = 0,
                        drawPointShape = PointGroup.drawCircle(6.dp, Color.Cyan)
                    )
                    it.setPoint(
                        position = Offset(-5f, -2f),
                        key = 2,
                        drawPointShape = PointGroup.drawUpwardTriangle(8.dp, Color.Cyan, DpOffset(0.dp, -4.dp))
                    )
                    it.setPoint(
                        position = Offset(-5f, -2f),
                        key = 3,
                        drawPointShape = PointGroup.drawDownwardTriangle(8.dp, Color.Cyan, DpOffset(0.dp, 4.dp))
                    )
                    it.setPoint(
                        position = Offset(5f, 2f),
                        key = 1,
                        drawPointShape = PointGroup.drawCircle(6.dp, Color.Cyan)
                    )
                }
            }
            pointGroup.Draw(transformation = transformation)
        }
    }
}