package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlin.math.roundToInt

class HorizontalMark(
    initialPosition: Float,
    initialAnchor: Anchor,
    initialHorizontalLabelPosition: Float = 0.5f,
    initialLineWidth: Dp = 1.dp,
    initialLineColor: @Composable () -> Color = {  Color.Unspecified },
    initialScreenOffset: DpOffset = DpOffset.Zero,
    initialContent: @Composable (modifier: Modifier) -> Unit,
) {
    data class LayoutData(
        val position: Float,
        val screenOffset: DpOffset,
        val anchor: Anchor,
        val horizontalLabelPosition: Float,
        val lineWidth: Dp,
    ): ParentDataModifier {
        override fun Density.modifyParentData(parentData: Any?) = this@LayoutData
    }
    var layoutData by mutableStateOf(LayoutData(
        initialPosition,
        initialScreenOffset,
        initialAnchor,
        initialHorizontalLabelPosition,
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
        horizontalLabelPosition: Float? = null,
        lineWidth: Dp? = null,
        lineColor: (@Composable () -> Color)? = null,
        content: (@Composable (modifier: Modifier) -> Unit)? = null,
    ) {
        this.layoutData = LayoutData(
            position ?: this.layoutData.position,
            screenOffset ?: this.layoutData.screenOffset,
            anchor ?: this.layoutData.anchor,
            horizontalLabelPosition ?: this.layoutData.horizontalLabelPosition,
            lineWidth ?: this.layoutData.lineWidth,
        )

        if (lineColor != null)
            this.lineColor = lineColor

        if (content != null)
            this.content = content
    }
}

private data class MeasuredHorizontalMark(val layoutData: HorizontalMark.LayoutData, val placeable: Placeable)
class HorizontalMarkGroup(
    private val clipLabel: Boolean = false,
    private val sameSizeLabels: Boolean = false
) : PlotGroup {
    private val marks = mutableMapOf<Int, HorizontalMark>()

    fun setMark(
        key: Int = 0,
        position: Float? = null,
        anchor: Anchor? = null,
        screenOffset: DpOffset? = null,
        horizontalLabelPosition: Float? = null,
        lineWidth: Dp? = null,
        lineColor: (@Composable () -> Color)? = null,
        content: (@Composable (modifier: Modifier) -> Unit)? = null,
    ) {
        marks[key]?.setMark(
            position,
            anchor,
            screenOffset,
            horizontalLabelPosition,
            lineWidth,
            lineColor,
            content
        )

        if (key !in marks) {
            marks[key] = HorizontalMark(
                position ?: 0f,
                anchor ?: Anchor.Center,
                horizontalLabelPosition ?: 0.5f,
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
                                .fillMaxWidth()
                                .height(it.value.layoutData.lineWidth)
                                .background(it.value.lineColor())
                        )
                    }
                }
            ) { measureables, constraints ->
                val c = constraints.copy(minWidth = 0, minHeight = 0)
                val placeables = measureables.map {
                    MeasuredHorizontalMark(
                        it.parentData as HorizontalMark.LayoutData,
                        it.measure(c)
                    )
                }

                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeables.forEach {
                        val l = it.layoutData
                        val yOffset = Offset(0f, l.position)
                        val yTransformed = transformation.toScreen(yOffset).y
                        it.placeable.place(
                            transformation.viewPortScreen.left,
                            (yTransformed - 0.5f * l.lineWidth.toPx()).roundToInt()
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
                    MeasuredHorizontalMark(
                        it.parentData as HorizontalMark.LayoutData,
                        it.measure(c)
                    )
                }

                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeables.forEach {
                        val p = it.placeable
                        val l = it.layoutData
                        val yOffset = Offset(0f, l.position)
                        val yTransformed = transformation.toScreen(yOffset).y
                        val vp = transformation.viewPortScreen

                        p.place(
                            it.layoutData.anchor.place(
                                vp.left + l.horizontalLabelPosition * vp.width + l.screenOffset.x.toPx(),
                                yTransformed + l.screenOffset.y.toPx(),
                                p.width.toFloat(),
                                p.height.toFloat(),
                                l.lineWidth.toPx(),
                                0f
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

@Preview(widthDp = 200, heightDp = 150, showBackground = true)
@Composable
private fun HorizontalMarkGroupPreview() {
    TunerTheme {
        BoxWithConstraints {
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-10f, 5f, 10f, -5f)
            )

            val markGroup = remember {
                HorizontalMarkGroup(sameSizeLabels = true).also {
                    it.setMark(
                        key = 0,
                        position = 0f,
                        anchor = Anchor.NorthEast,
                        horizontalLabelPosition = 0.5f,
                        lineWidth = 3.dp,
                        content = { modifier -> Text("0NE....", modifier = modifier.background(Color.Cyan))}
                    )
                    it.setMark(
                        key = 1,
                        position = -4f,
                        anchor = Anchor.SouthWest,
                        horizontalLabelPosition = 0.1f,
                        content = { modifier -> Text("-4SW", modifier = modifier.background(Color.Magenta))}
                    )
                    it.setMark(
                        key = 2,
                        position = 3f,
                        anchor = Anchor.SouthEast,
                        horizontalLabelPosition = 0.9f,
                        content = { modifier -> Text(
                            "3SE", modifier = modifier.background(Color.Green),
                            textAlign = TextAlign.Center
                        )}
                    )
                }
            }
            markGroup.Draw(transformation = transformation)
        }
    }
}