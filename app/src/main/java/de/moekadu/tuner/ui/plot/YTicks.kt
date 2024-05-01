package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
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
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
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
    private val styleKey: Int?,
    private val label: (@Composable (modifier: Modifier, level: Int, index: Int, y: Float) -> Unit)?,
    private val tickLevel: TickLevel,
    private val maxLabelHeight: @Composable Density.() -> Float,
    private val clipLabelToPlotWindow: Boolean = false,
    private val maxNumLabels: Int = -1,
): PlotItem {
    override val hasClippedDraw = true
    override val hasUnclippedDraw = !clipLabelToPlotWindow

    data class Style(
        val anchor: Anchor = Anchor.Center,
        val horizontalLabelPosition: Float = 0.5f,
        val lineWidth: Dp = 1.dp,
        val lineColor: Color = Color.Unspecified,
        val screenOffset: DpOffset = DpOffset.Zero
    ) : PlotStyle

    data class TickLayoutData(val position: Float):
        ParentDataModifier {
        override fun Density.modifyParentData(parentData: Any?) = this@TickLayoutData
    }

    private data class MeasuredTick(
        val position: TickLayoutData,
        val placeable: Placeable
    )

    @Composable
    private fun DrawLabels(transformation: Transformation, range: TicksRange, plotStyle: Style) {
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
                        plotStyle.anchor.place(
                            vp.left + plotStyle.horizontalLabelPosition * vp.width + plotStyle.screenOffset.x.toPx(),
                            yTransformed + plotStyle.screenOffset.y.toPx(),
                            p.width.toFloat(),
                            p.height.toFloat(),
                            plotStyle.lineWidth.toPx(),
                            0f
                        ).round()
                    )

                }
            }
        }
    }

    @Composable
    private fun rememberRange(transformation: Transformation, plotStyle: Style): TicksRange {
        val density = LocalDensity.current
        val maxLabelHeightPx = density.maxLabelHeight()

        return remember(density, transformation, plotStyle, maxNumLabels) {
            val screenOffsetPx = with(density) { plotStyle.screenOffset.y.toPx() }
            val lineWidthPx = with(density) { plotStyle.lineWidth.toPx() }
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
    override fun DrawClipped(
        transformation: Transformation,
        plotStyles: ImmutableMap<Int, PlotStyle>
    ) {
        Box(Modifier.fillMaxSize()) {
            val plotStyle = remember(plotStyles, styleKey) {
                (plotStyles[styleKey] as? Style) ?: defaultStyle
            }
            val lineColor = plotStyle.lineColor.takeOrElse {
                LocalContentColor.current.takeOrElse {
                    MaterialTheme.colorScheme.outline
                }
            }
            val range = rememberRange(transformation = transformation, plotStyle = plotStyle)

            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in range.indexBegin until range.indexEnd) {
                    val yOffset = Offset(0f, tickLevel.getTickValue(range.level, i))
                    val yTransformed = transformation.toScreen(yOffset).y
                    drawLine(
                        lineColor,
                        Offset(transformation.viewPortScreen.left.toFloat(), yTransformed),
                        Offset(transformation.viewPortScreen.right.toFloat(), yTransformed),
                        strokeWidth = plotStyle.lineWidth.toPx()
                    )
                }
            }

            if (clipLabelToPlotWindow)
                DrawLabels(transformation = transformation, range = range, plotStyle)
        }
    }

    @Composable
    override fun DrawUnclipped(
        transformation: Transformation,
        plotStyles: ImmutableMap<Int, PlotStyle>
    ) {
        if (!clipLabelToPlotWindow) {
            val plotStyle = remember(plotStyles, styleKey) {
                (plotStyles[styleKey] as? Style) ?: defaultStyle
            }
            val range = rememberRange(transformation = transformation, plotStyle = plotStyle)
            DrawLabels(transformation = transformation, range = range, plotStyle = plotStyle)
        }
    }

    companion object {
        private val defaultStyle = Style()
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
            val plotStyles = persistentMapOf<Int, PlotStyle>(
                0 to YTicks.Style(
                    anchor = Anchor.South,
                    screenOffset = DpOffset(0.dp, (-1).dp)
                )
            )
            val textLabelHeight = rememberTextLabelHeight()

            val ticks = YTicks(
                label = { m, l, i, y -> Text("$l, $i, $y", modifier = m.background(Color.Magenta))},
                styleKey = 0,
                tickLevel = TickLevelExplicitRanges(
                    listOf(
                        floatArrayOf(-3f, -2f, 0f, 4f),
                        floatArrayOf(-3f, -2f, -1f, 0f, 2f, 4f),
                        floatArrayOf(-3f, -2.5f, -2f, -1.5f, -1f, 0f, 1f, 2f, 3f, 4f),
                    ).toImmutableList()
                ),
                maxLabelHeight = { textLabelHeight }
            )

            ticks.DrawClipped(transformation = transformation, plotStyles = plotStyles)
            ticks.DrawUnclipped(transformation = transformation, plotStyles = plotStyles)
        }
    }
}