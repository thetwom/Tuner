package de.moekadu.tuner.ui.plot3

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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

data class VerticalMark3(
    val position: Float,
    val settings: Settings,
    val content: @Composable (modifier: Modifier) -> Unit,
) : ParentDataModifier {
    data class Settings(
        val anchor: Anchor = Anchor.Center,
        val labelPosition: Float = 0.5f,
        val lineWidth: Dp = 1.dp,
        val lineColor: Color = Color.Unspecified, // TODO: Color.Unspecified is not allowed, how to handle this?
        val screenOffset: DpOffset = DpOffset.Zero
    )
    override fun Density.modifyParentData(parentData: Any?) = this@VerticalMark3
}

private data class MeasuredVerticalMark(
    val placeable: Placeable,
    val mark: VerticalMark3
)

@Composable
private fun VerticalMark3Lines(
    marks: ImmutableList<VerticalMark3>,
    transformation: () -> Transformation
) {
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val transformationInstance = transformation()
                marks.forEach { mark ->
                    val xOffset = Offset(mark.position, 0f)
                    val xTransformed = transformationInstance.toScreen(xOffset).x
                    drawLine(
                        mark.settings.lineColor, // TODO: resolve Color.Unspecified
                        Offset(
                            xTransformed,
                            transformationInstance.viewPortScreen.top.toFloat(),
                        ),
                        Offset(
                            xTransformed,
                            transformationInstance.viewPortScreen.bottom.toFloat(),
                        ),
                        strokeWidth = mark.settings.lineWidth.toPx()
                    )
                }
            }
    )
}

@Composable
private fun VerticalMark3Labels(
    marks: ImmutableList<VerticalMark3>,
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
            MeasuredVerticalMark(it.measure(c), it.parentData as VerticalMark3)
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            val transform = transformation()
            placeables.forEach { measuredMark ->
                val p = measuredMark.placeable
                val mark = measuredMark.mark
                val xOffset = Offset(mark.position, 0f)
                val xTransformed = transform.toScreen(xOffset).x
                val vp = transform.viewPortScreen
                val settings = mark.settings
                p.place(
                    settings.anchor.place(
                        xTransformed + settings.screenOffset.x.toPx(),
                        vp.top + (1f - settings.labelPosition) * vp.height+ settings.screenOffset.y.toPx(),
                        p.width.toFloat(),
                        p.height.toFloat(),
                        0f,
                        settings.lineWidth.toPx()
                    ).round()
                )
            }
        }
    }
}


@Composable
fun VerticalMarks3(
    marks: ImmutableList<VerticalMark3>,
    clipLabelsToWindow: Boolean = false,
    sameSizeLabels: Boolean = false,
    clipped: Boolean,
    transformation: () -> Transformation
) {
    if (clipped) {
        VerticalMark3Lines(marks = marks, transformation = transformation)
    }

    if (clipLabelsToWindow == clipped) {
        VerticalMark3Labels(
            marks = marks,
            sameSizeLabels = sameSizeLabels,
            transformation = transformation
        )
    }
}