package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.takeOrElse
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt

class HorizontalMark(
    position: Float,
    anchor: Anchor = Anchor.Center,
    horizontalLabelPosition: Float = 0.5f,
    lineWidth: Dp = 1.dp,
    lineColor: @Composable () -> Color = {  Color.Unspecified },
    screenOffset: DpOffset = DpOffset.Zero,
    content: @Composable (modifier: Modifier) -> Unit,
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
        position,
        screenOffset,
        anchor,
        horizontalLabelPosition,
        lineWidth,
    ))
        private set

    var lineColor by mutableStateOf(lineColor)
        private set
    var content by mutableStateOf(content)
        private set

    fun modify(
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
    private val marks: ImmutableList<HorizontalMark>,
    private val clipLabel: Boolean = false,
    private val sameSizeLabels: Boolean = false,
) : PlotItem {
    override val hasClippedDraw = true
    override val hasUnclippedDraw = !clipLabel

    fun modify(
        markIndex: Int,
        position: Float? = null,
        anchor: Anchor? = null,
        screenOffset: DpOffset? = null,
        horizontalLabelPosition: Float? = null,
        lineWidth: Dp? = null,
        lineColor: (@Composable () -> Color)? = null,
        content: (@Composable (modifier: Modifier) -> Unit)? = null,
    ) {
        marks[markIndex].modify(
            position,
            anchor,
            screenOffset,
            horizontalLabelPosition,
            lineWidth,
            lineColor,
            content
        )
    }

    @Composable
    private fun DrawLabels(transformation: Transformation) {
        Layout(modifier = Modifier.fillMaxSize(),
            content = {
                marks.forEach { it.content(it.layoutData) }
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

    @Composable
    override fun DrawClipped(transformation: Transformation) {

        Box(modifier = Modifier.fillMaxSize()) {
            // layout to draw the lines
            Layout(
                modifier = Modifier.fillMaxSize(),
                content = {
                    marks.forEach {
                        Spacer(
                            modifier = it.layoutData
                                .fillMaxWidth()
                                .height(it.layoutData.lineWidth)
                                .background(it.lineColor().takeOrElse { MaterialTheme.colorScheme.outline })
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

            if (clipLabel)
                DrawLabels(transformation = transformation)
        }
    }

    @Composable
    override fun DrawUnclipped(transformation: Transformation) {
        if (!clipLabel)
            DrawLabels(transformation = transformation)
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
                HorizontalMarkGroup(
                    persistentListOf(
                        HorizontalMark(
                            position = 0f,
                            anchor = Anchor.NorthEast,
                            horizontalLabelPosition = 0.5f,
                            lineWidth = 3.dp,
                            content = { modifier ->
                                Text(
                                    "0NE....",
                                    modifier = modifier.background(Color.Cyan)
                                )
                            }
                        ),
                        HorizontalMark(
                            position = -4f,
                            anchor = Anchor.SouthWest,
                            horizontalLabelPosition = 0.1f,
                            content = { modifier ->
                                Text(
                                    "-4SW",
                                    modifier = modifier.background(Color.Magenta)
                                )
                            }
                        ),
                        HorizontalMark(
                            position = 3f,
                            anchor = Anchor.SouthEast,
                            horizontalLabelPosition = 0.9f,
                            content = { modifier ->
                                Text(
                                    "3SE", modifier = modifier.background(Color.Green),
                                    textAlign = TextAlign.Center
                                )
                            }
                        )
                    ),
                    clipLabel = true
                )
            }
            markGroup.DrawClipped(transformation = transformation)
            markGroup.DrawUnclipped(transformation = transformation)
        }
    }
}