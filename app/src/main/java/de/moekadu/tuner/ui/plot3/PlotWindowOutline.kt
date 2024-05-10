package de.moekadu.tuner.ui.plot3

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
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize

data class Plot3WindowOutline(
    val lineWidth: Dp,
    val cornerRadius: Dp,
    val color: Color
)

@Composable
fun Modifier.createPlotWindowOutline(outline: Plot3WindowOutline, viewPort: () -> IntRect): Modifier {
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
