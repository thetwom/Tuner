package de.moekadu.tuner.ui.plot3

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import de.moekadu.tuner.ui.plot.Anchor
import de.moekadu.tuner.ui.plot.Transformation
import de.moekadu.tuner.ui.plot.place
import kotlinx.collections.immutable.ImmutableList


data class PointMark3(
    val position: Offset,
    val settings: Settings,
    val content: @Composable (modifier: Modifier) -> Unit,
) : ParentDataModifier {
    data class Settings(
        val anchor: Anchor = Anchor.Center,
        val screenOffset: DpOffset = DpOffset.Zero
    )
    override fun Density.modifyParentData(parentData: Any?) = this@PointMark3
}

private data class MeasuredPointMark(
    val placeable: Placeable,
    val mark: PointMark3
)

@Composable
private fun PointMark3Labels(
    marks: ImmutableList<PointMark3>,
    sameSizeLabels: Boolean,
    transformation: () -> Transformation
) {
    Layout(modifier = Modifier.fillMaxSize(),
        content = {
            marks.forEach { it.content(it) }
        }
    ) { measureables, constraints ->
        val c = if (sameSizeLabels) {
            val maxHeight = measureables.maxOf { it.minIntrinsicHeight(Int.MAX_VALUE) }
            val maxWidth = measureables.maxOf { it.maxIntrinsicWidth(maxHeight) }
            constraints.copy(
                minWidth = maxWidth, minHeight = maxHeight,
                maxWidth = maxWidth, maxHeight = maxHeight
            )
        } else {
            constraints.copy(minWidth = 0, minHeight = 0)
        }

        val placeables = measureables.map {
            MeasuredPointMark(it.measure(c), it.parentData as PointMark3)
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            val transform = transformation()
            placeables.forEach {measuredMark ->
                val p = measuredMark.placeable
                val mark = measuredMark.mark
                val positionScreen = transform.toScreen(mark.position)
                val settings = mark.settings
                p.place(
                    settings.anchor.place(
                        positionScreen.x + settings.screenOffset.x.toPx(),
                        positionScreen.y + settings.screenOffset.y.toPx(),
                        p.width.toFloat(),
                        p.height.toFloat()
                    ).round()
                )
            }
        }
    }
}

@Composable
fun PointMarks3(
    marks: ImmutableList<PointMark3>,
    clipLabelsToWindow: Boolean = false,
    sameSizeLabels: Boolean = false,
    clipped: Boolean,
    transformation: () -> Transformation
) {
    if (clipLabelsToWindow == clipped) {
        PointMark3Labels(
            marks = marks,
            sameSizeLabels = sameSizeLabels,
            transformation = transformation
        )
    }
}