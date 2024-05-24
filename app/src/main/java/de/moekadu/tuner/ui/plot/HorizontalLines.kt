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

data class HorizontalLinesPositions(
    val size: Int,
    val y: (i: Int) -> Float
)

@Composable
fun HorizontalLines(
    data: HorizontalLinesPositions,
    color: Color,
    width: Dp,
    transformation: () -> Transformation
) {
    val c = color.takeOrElse { MaterialTheme.colorScheme.onSurface }

    Spacer(modifier = Modifier
        .fillMaxSize()
        .drawBehind {
            for (i in 0 until data.size) {
                val transform = transformation()
                val y = transform.toScreen(Offset(0f, data.y(i))).y
                drawLine(
                    c,
                    Offset(transform.viewPortScreen.left.toFloat(), y),
                    Offset(transform.viewPortScreen.right.toFloat(), y),
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
private fun HorizontalLinePreview() {
    TunerTheme {
        BoxWithConstraints {
            val y = remember { floatArrayOf(-1f, 1f, 2f, 3f, 4f) }
            val positions = remember {
                HorizontalLinesPositions(
                    size = 5, y = { y[it] }
                )
            }
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-1f, 5f, 5f, -3f)
            )

            HorizontalLines(positions, MaterialTheme.colorScheme.primary, 2.dp, { transformation })
        }
    }
}


