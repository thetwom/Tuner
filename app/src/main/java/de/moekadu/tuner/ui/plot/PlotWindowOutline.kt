package de.moekadu.tuner.ui.plot

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp

data class PlotWindowOutline(
    val lineWidth: Dp = 1.dp,
    val cornerRadius: Dp = 8.dp,
    val color: Color = Color.Unspecified
)

@Composable
fun Modifier.createPlotWindowOutline(outline: PlotWindowOutline, viewPort: () -> IntRect): Modifier {
    val color = outline.color.takeOrElse { MaterialTheme.colorScheme.onSurface }
    return this.drawBehind {
        val vP = viewPort()
        val lineWidthPx = outline.lineWidth.toPx()
        val lineWidthPxHalf = 0.5f * lineWidthPx
        val topLeft = Offset(
            vP.left + lineWidthPxHalf,
            vP.top + lineWidthPxHalf
        )
        val bottomRight = Offset(
            vP.right - lineWidthPxHalf,
            vP.bottom - lineWidthPxHalf,
        )
        val size = Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y)

        drawRoundRect(
            color = color,
            topLeft = topLeft,
            size = size,
            cornerRadius = CornerRadius(outline.cornerRadius.toPx()),
            style = Stroke(outline.lineWidth.toPx())
        )
    }
}
