package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlin.math.roundToInt

class VerticalMark(
    initialPosition: Float,
    initialAnchor: Anchor,
    initialVerticalLabelPosition: Float = 0.5f,
    initialLineWidth: Dp = 1.dp,
    initialLineColor: @Composable () -> Color = {  Color.Unspecified },
    initialScreenOffset: DpOffset = DpOffset.Zero,
    initialContent: @Composable (modifier: Modifier) -> Unit,
) {
    data class LayoutData(
        val position: Float,
        val screenOffset: DpOffset,
        val anchor: Anchor,
        val verticalLabelPosition: Float,
        val lineWidth: Dp,
    ): ParentDataModifier {
        override fun Density.modifyParentData(parentData: Any?) = this@LayoutData
    }
    var layoutData by mutableStateOf(LayoutData(
        initialPosition,
        initialScreenOffset,
        initialAnchor,
        initialVerticalLabelPosition,
        initialLineWidth,
    ))
        private set

    var lineColor by mutableStateOf(initialLineColor)
        private set
    var content by mutableStateOf(initialContent)
        private set

    fun setMark(
        position: Float? = null,
        anchor: Anchor? = null,
        screenOffset: DpOffset? = null,
        verticalLabelPosition: Float? = null,
        lineWidth: Dp? = null,
        lineColor: (@Composable () -> Color)? = null,
        content: (@Composable (modifier: Modifier) -> Unit)? = null,
    ) {
        this.layoutData = LayoutData(
            position ?: this.layoutData.position,
            screenOffset ?: this.layoutData.screenOffset,
            anchor ?: this.layoutData.anchor,
            verticalLabelPosition ?: this.layoutData.verticalLabelPosition,
            lineWidth ?: this.layoutData.lineWidth,
        )

        if (lineColor != null)
            this.lineColor = lineColor

        if (content != null)
            this.content = content
    }
}

private data class MeasuredVerticalMark(val layoutData: VerticalMark.LayoutData, val placeable: Placeable)
class VerticalMarkGroup(
    private val clipLabel: Boolean = false,
    private val sameSizeLabels: Boolean = false
) : PlotGroup {
    private val marks = mutableMapOf<Int, VerticalMark>()

    fun setMark(
        key: Int = 0,
        position: Float? = null,
        anchor: Anchor? = null,
        screenOffset: DpOffset? = null,
        verticalLabelPosition: Float? = null,
        lineWidth: Dp? = null,
        lineColor: (@Composable () -> Color)? = null,
        content: (@Composable (modifier: Modifier) -> Unit)? = null,
    ) {
        marks[key]?.setMark(
            position,
            anchor,
            screenOffset,
            verticalLabelPosition,
            lineWidth,
            lineColor,
            content
        )

        if (key !in marks) {
            marks[key] = VerticalMark(
                position ?: 0f,
                anchor ?: Anchor.Center,
                verticalLabelPosition ?: 0.5f,
                lineWidth ?: 1.dp,
                lineColor ?: { Color.Black },
                screenOffset ?: DpOffset.Zero,
                content ?: { Text("x") }
            )
        }
    }

    @Composable
    override fun Draw(transformation: Transformation) {

        Box(Modifier.fillMaxSize()) {
            val clipShape = transformation.rememberClipShape()

            // layout to draw the lines
            Layout(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(clipShape),
                content = {
                    marks.forEach {
                        Spacer(
                            modifier = it.value.layoutData
                                .fillMaxHeight()
                                .width(it.value.layoutData.lineWidth)
                                .background(it.value.lineColor())
                        )
                    }
                }
            ) { measureables, constraints ->
                val c = constraints.copy(minWidth = 0, minHeight = 0)
                val placeables = measureables.map {
                    MeasuredVerticalMark(
                        it.parentData as VerticalMark.LayoutData,
                        it.measure(c)
                    )
                }

                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeables.forEach {
                        val l = it.layoutData
                        val xOffset = Offset(l.position, 0f)
                        val xTransformed = transformation.toScreen(xOffset).x
                        it.placeable.place(
                            (xTransformed - 0.5f * l.lineWidth.toPx()).roundToInt(),
                            transformation.viewPortScreen.top,
                        )
                    }
                }
            }

            // now draw the labels
            Layout(
                modifier = (if (clipLabel) Modifier.clip(clipShape) else Modifier)
                    .fillMaxSize(),
                content = {
                    marks.forEach { it.value.content(it.value.layoutData) }
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
                    MeasuredVerticalMark(
                        it.parentData as VerticalMark.LayoutData,
                        it.measure(c)
                    )
                }

                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeables.forEach {
                        val p = it.placeable
                        val l = it.layoutData
                        val xOffset = Offset(l.position, 0f)
                        val xTransformed = transformation.toScreen(xOffset).x
                        val vp = transformation.viewPortScreen

                        p.place(
                            it.layoutData.anchor.place(
                                xTransformed + l.screenOffset.x.toPx(),
                                vp.top + (1f - l.verticalLabelPosition) * vp.height+ l.screenOffset.y.toPx(),
                                p.width.toFloat(),
                                p.height.toFloat(),
                                0f,
                                l.lineWidth.toPx()
                            ).round()
                        )
                    }
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

@Preview(widthDp = 200, heightDp = 100, showBackground = true)
@Composable
private fun VerticalMarkGroupPreview() {
    TunerTheme {
        BoxWithConstraints {
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-10f, 5f, 10f, -5f)
            )

            val markGroup = remember {
                VerticalMarkGroup(sameSizeLabels = true).also {
                    it.setMark(
                        key = 0,
                        position = 0f,
                        anchor = Anchor.NorthEast,
                        verticalLabelPosition = 0.5f,
                        lineWidth = 3.dp,
                        content = { modifier -> Text("0NE", modifier = modifier.background(Color.Cyan))}
                    )
                    it.setMark(
                        key = 1,
                        position = -8f,
                        anchor = Anchor.SouthWest,
                        verticalLabelPosition = 0.1f,
                        content = { modifier -> Text("-8SW", modifier = modifier.background(Color.Magenta))}
                    )
                    it.setMark(
                        key = 2,
                        position = 9f,
                        anchor = Anchor.NorthEast,
                        verticalLabelPosition = 0.9f,
                        content = { modifier -> Text("9SE", modifier = modifier.background(Color.Green))}
                    )
                }
            }
            markGroup.Draw(transformation = transformation)
        }
    }
}