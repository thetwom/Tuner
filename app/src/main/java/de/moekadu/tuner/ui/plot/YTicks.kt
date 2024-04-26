package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


/** Helper function to compute height of text labels.
 * @param style Text style.
 * @param density Density of measurement environment.
 * @param paddingTop Padding above text.
 * @param paddingBottom Padding below text.
 * @param textMeasurer Text measurer which is used to measure the text.
 * @return Label height in px.
 */
@Composable
fun rememberTextLabelHeight(
    style: TextStyle = LocalTextStyle.current,
    density: Density = LocalDensity.current,
    paddingTop: Dp = 0.dp,
    paddingBottom: Dp = 0.dp,
    textMeasurer: TextMeasurer = rememberTextMeasurer()
): Float {
    return remember(textMeasurer, density, paddingTop, paddingBottom, style) {
        with(density) {
            (textMeasurer.measure("X", style = style, density = density).size.height
                    + paddingTop.toPx()
                    + paddingBottom.toPx())
        }
    }
}

class YTicks(
    private val label: (@Composable (modifier: Modifier, level: Int, index: Int, y: Float) -> Unit)?,
    private val tickLevel: TickLevel,
    private val maxLabelHeight: @Composable Density.() -> Float,
    private val anchor: Anchor = Anchor.Center,
    private val horizontalLabelPosition: Float = 0.5f,
    private val lineWidth: Dp = 1.dp,
    private val lineColor: @Composable () -> Color = {  Color.Unspecified },
    private val clipLabelToPlotWindow: Boolean = false,
    private val maxNumLabels: Int = -1,
    private val screenOffset: DpOffset = DpOffset.Zero
): PlotItem {
    override val hasClippedDraw = true
    override val hasUnclippedDraw = !clipLabelToPlotWindow

    data class TickLayoutData(val position: Float):
        ParentDataModifier {
        override fun Density.modifyParentData(parentData: Any?) = this@TickLayoutData
    }

    private data class MeasuredTick(
        val position: TickLayoutData,
        val placeable: Placeable
    )

    @Composable
    private fun DrawLabels(transformation: Transformation, range: TicksRange) {
        Layout(
            content = {
                label?.let { l ->
                    for (i in range.indexBegin until range.indexEnd) {
                        val y = tickLevel.getTickValue(range.level, i)
                        l(TickLayoutData(y), range.level, i, y)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { measureables, constraints ->
            val c = constraints.copy(minWidth = 0, minHeight = 0)
            val placeables = measureables.map {
                MeasuredTick(
                    it.parentData as TickLayoutData,
                    it.measure(c)
                )
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEach {
                    val p = it.placeable
                    val yOffset = Offset(0f, it.position.position)
                    val yTransformed = transformation.toScreen(yOffset).y
                    val vp = transformation.viewPortScreen

                    p.place(
                        anchor.place(
                            vp.left + horizontalLabelPosition * vp.width + screenOffset.x.toPx(),
                            yTransformed + screenOffset.y.toPx(),
                            p.width.toFloat(),
                            p.height.toFloat(),
                            lineWidth.toPx(),
                            0f
                        ).round()
                    )

                }
            }
        }
    }

    @Composable
    private fun rememberRange(transformation: Transformation): TicksRange {
        val density = LocalDensity.current
        val maxLabelHeightPx = density.maxLabelHeight()

        return remember(density, transformation, screenOffset, lineWidth, maxNumLabels) {
            val screenOffsetPx = with(density) { screenOffset.y.toPx() }
            val lineWidthPx = with(density) { lineWidth.toPx() }
            val maxNumLabelsResolved = if (maxNumLabels <= 0)
                (transformation.viewPortScreen.height / maxLabelHeightPx / 2f).roundToInt()
            else
                maxNumLabels
            val labelHeightScreen = Rect(
                0f,
                0f,
                1f,
                maxLabelHeightPx + 0.5f * lineWidthPx + screenOffsetPx.absoluteValue
            )

            val labelHeightRaw = transformation.toRaw(labelHeightScreen).height
            tickLevel.getTicksRange(
                transformation.viewPortRaw.bottom - labelHeightRaw,
                transformation.viewPortRaw.top + labelHeightRaw,
                maxNumLabelsResolved,
                labelHeightRaw
            )
        }
    }

    @Composable
    override fun DrawClipped(transformation: Transformation) {

        Box(Modifier.fillMaxSize()) {
            val lineColor = lineColor().takeOrElse { MaterialTheme.colorScheme.outline }
            val range = rememberRange(transformation = transformation)

            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in range.indexBegin until range.indexEnd) {
                    val yOffset = Offset(0f, tickLevel.getTickValue(range.level, i))
                    val yTransformed = transformation.toScreen(yOffset).y
                    drawLine(
                        lineColor,
                        Offset(transformation.viewPortScreen.left.toFloat(), yTransformed),
                        Offset(transformation.viewPortScreen.right.toFloat(), yTransformed),
                        strokeWidth = lineWidth.toPx()
                    )
                }
            }

            if (clipLabelToPlotWindow)
                DrawLabels(transformation = transformation, range = range)
        }
    }

    @Composable
    override fun DrawUnclipped(transformation: Transformation) {
        if (!clipLabelToPlotWindow) {
            val range = rememberRange(transformation = transformation)
            DrawLabels(transformation = transformation, range = range)
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

@Preview(widthDp = 200, heightDp = 400, showBackground = true)
@Composable
private fun YTicksPreview() {
    TunerTheme {
        BoxWithConstraints {
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-10f, 5f, 10f, -5f)
            )
            val textLabelHeight = rememberTextLabelHeight()

            val ticks = YTicks(
                label = { m, l, i, y -> Text("$l, $i, $y", modifier = m.background(Color.Magenta))},
                anchor = Anchor.South,
                tickLevel = TickLevelExplicitRanges(
                    listOf(
                        floatArrayOf(-3f, -2f, 0f, 4f),
                        floatArrayOf(-3f, -2f, -1f, 0f, 2f, 4f),
                        floatArrayOf(-3f, -2.5f, -2f, -1.5f, -1f, 0f, 1f, 2f, 3f, 4f),
                    ).toImmutableList()
                ),
                maxLabelHeight = { textLabelHeight },
                screenOffset = DpOffset(0.dp, (-1).dp)
            )

            ticks.DrawClipped(transformation = transformation)
            ticks.DrawUnclipped(transformation = transformation)
        }
    }
}