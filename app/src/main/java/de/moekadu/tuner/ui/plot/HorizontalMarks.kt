package de.moekadu.tuner.ui.plot

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
import kotlinx.collections.immutable.ImmutableList

data class HorizontalMark(
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
    override fun Density.modifyParentData(parentData: Any?) = this@HorizontalMark
}

private data class MeasuredHorizontalMark(
    val placeable: Placeable,
    val mark: HorizontalMark
)

@Composable
private fun HorizontalMarkLines(
    marks: ImmutableList<HorizontalMark>,
    transformation: () -> Transformation
) {
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val transformationInstance = transformation()
                marks.forEach { mark ->
                    val yOffset = Offset(0f, mark.position)
                    val yTransformed = transformationInstance.toScreen(yOffset).y
                    drawLine(
                        mark.settings.lineColor, // TODO: resolve Color.Unspecified
                        Offset(
                            transformationInstance.viewPortScreen.left.toFloat(),
                            yTransformed
                        ),
                        Offset(
                            transformationInstance.viewPortScreen.right.toFloat(),
                            yTransformed
                        ),
                        strokeWidth = mark.settings.lineWidth.toPx()
                    )
                }
            }
    )
}

@Composable
private fun HorizontalMarkLabels(
    marks: ImmutableList<HorizontalMark>,
    sameSizeLabels: Boolean,
    clipLabelsToWindow: Boolean,
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
            MeasuredHorizontalMark(it.measure(c), it.parentData as HorizontalMark)
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            val transform = transformation()
            placeables.forEach { measuredMark ->
                val p = measuredMark.placeable
                val mark = measuredMark.mark
                val yOffset = Offset(0f, mark.position)
                val yTransformed = transform.toScreen(yOffset).y
                val vp = transform.viewPortScreen
                val settings = mark.settings
                val visible = yTransformed in vp.top.toFloat() .. vp.bottom.toFloat()
                if (clipLabelsToWindow || visible) {
                    p.place(
                        settings.anchor.place(
                            vp.left + settings.labelPosition * vp.width + settings.screenOffset.x.toPx(),
                            yTransformed + settings.screenOffset.y.toPx(),
                            p.width.toFloat(),
                            p.height.toFloat(),
                            settings.lineWidth.toPx(),
                            0f
                        ).round()
                    )
                }
            }
        }
    }
}


@Composable
fun HorizontalMarks(
    marks: ImmutableList<HorizontalMark>,
    clipLabelsToWindow: Boolean = false,
    sameSizeLabels: Boolean = false,
    clipped: Boolean,
    transformation: () -> Transformation
) {
    if (clipped) {
        HorizontalMarkLines(marks = marks, transformation = transformation)
    }

    if (clipLabelsToWindow == clipped) {
        HorizontalMarkLabels(
            marks = marks,
            sameSizeLabels = sameSizeLabels,
            clipLabelsToWindow = clipLabelsToWindow,
            transformation = transformation
        )
    }
}