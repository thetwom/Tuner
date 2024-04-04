package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.roundToInt

/** Helper function to compute height of text labels.
 * @param testString Test string, for which the width is measured.
 * @param style Text style.
 * @param density Density of measurement environment.
 * @param paddingLeft Padding left of text.
 * @param paddingRight Padding right of text.
 * @param textMeasurer Text measurer which is used to measure the text.
 * @return Label width in px.
 */
@Composable
fun rememberTextLabelWidth(
    testString: String,
    style: TextStyle = LocalTextStyle.current,
    density: Density = LocalDensity.current,
    paddingLeft: Dp = 0.dp,
    paddingRight: Dp = 0.dp,
    textMeasurer: TextMeasurer = rememberTextMeasurer()
): Float {
    return remember(textMeasurer, density, paddingLeft, paddingRight, style) {
        with(density) {
            (textMeasurer.measure(testString, style = style, density = density).size.width
                    + paddingLeft.toPx()
                    + paddingRight.toPx())
        }
    }
}


class VerticalMarks(
    private val label: (@Composable (level: Int, index: Int, y: Float) -> Unit)?,
    private val markLevel: MarkLevel3,
    private val maxLabelWidth: Density.() -> Float,
    private val defaultAnchor: Anchor = Anchor.Center,
    private val defaultVerticalLabelPosition: Float = 0.5f,
    private val lineWidth: Dp = 1.dp,
    private val lineColor: @Composable () -> Color = {  Color.Unspecified },
    private val clipLabelToPlotWindow: Boolean = false,
    private val maxNumLabels: Int = -1
): PlotGroup {
    @Composable
    override fun Draw(transformation: Transformation) {
        val density = LocalDensity.current
        val lineWidthPx = with(density) { lineWidth.toPx() }
        val maxLabelWidthPx = density.maxLabelWidth()
        val maxNumLabelsResolved = if (maxNumLabels <= 0)
            (transformation.viewPortScreen.width / maxLabelWidthPx / 1.1f).roundToInt()
        else
            maxNumLabels
        val range = remember(transformation, maxNumLabelsResolved, markLevel, maxLabelWidthPx) {
            val labelWidthScreen = Rect(
                0f, 0f, maxLabelWidthPx + 0.5f * lineWidthPx, 1f
            )

            val labelWidthRaw = transformation.toRaw(labelWidthScreen).width
            markLevel.getMarksRange(
                transformation.viewPortRaw.left - labelWidthRaw,
                transformation.viewPortRaw.right + labelWidthRaw,
                maxNumLabelsResolved,
                labelWidthRaw
            )
        }
        Box(Modifier.fillMaxSize()) {
            val lineColor = lineColor().takeOrElse { MaterialTheme.colorScheme.outline }
            val clipShape = transformation.rememberClipShape()
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(clipShape)
            ) {
                for (i in range.indexBegin until range.indexEnd) {
                    val xOffset = Offset(markLevel.getMarkValue(range.level, i), 0f)
                    val xTransformed = transformation.toScreen(xOffset).x
                    drawLine(
                        lineColor,
                        Offset(xTransformed, transformation.viewPortScreen.top.toFloat()),
                        Offset(xTransformed, transformation.viewPortScreen.bottom.toFloat()),
                        strokeWidth = lineWidth.toPx()
                    )
                }
            }

            Layout(
                content = {
                    label?.let { l ->
                        for (i in range.indexBegin until range.indexEnd) {
                            l(range.level, i, markLevel.getMarkValue(range.level, i))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (clipLabelToPlotWindow) Modifier.clip(clipShape) else Modifier)
            ) { measureables, constraints ->
                val c = constraints.copy(minWidth = 0, minHeight = 0)
                val placeables = measureables.map { it.measure(c) }

                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeables.forEachIndexed { index, p ->
                        val xOffset = Offset(
                            markLevel.getMarkValue(range.level, range.indexBegin + index),
                            0f
                        )
                        val xTransformed = transformation.toScreen(xOffset).x

                        val vp = transformation.viewPortScreen
                        val x = xTransformed
                        val y = vp.top + (1f - defaultVerticalLabelPosition) * (vp.height)
                        val w = p.width
                        val h = p.height
                        val l2 = 0.5f * lineWidth.toPx()

                        when (defaultAnchor) {
                            Anchor.NorthWest -> p.place((x + l2).roundToInt(), y.roundToInt())
                            Anchor.North -> p.place(
                                (x - 0.5 * w).roundToInt(),
                                y.roundToInt()
                            )

                            Anchor.NorthEast -> p.place(
                                (x - w - l2).roundToInt(),
                                y.roundToInt()
                            )

                            Anchor.West -> p.place((x + l2).roundToInt(), (y - 0.5f * h).roundToInt())
                            Anchor.Center -> p.place(
                                (x - 0.5f * w).roundToInt(),
                                (y - 0.5f * h).roundToInt()
                            )

                            Anchor.East -> p.place(
                                (x - w - l2).roundToInt(),
                                (y - 0.5f * h).roundToInt()
                            )

                            Anchor.SouthWest -> p.place(
                                (x + l2).roundToInt(),
                                (y - h).roundToInt()
                            )

                            Anchor.South -> p.place(
                                (x - 0.5 * w).roundToInt(),
                                (y - h).roundToInt()
                            )

                            Anchor.SouthEast -> p.place(
                                (x - w - l2).roundToInt(),
                                (y - h).roundToInt()
                            )
                        }
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

@Preview(widthDp = 200, heightDp = 400, showBackground = true)
@Composable
private fun VerticalMarksPreview() {
    TunerTheme {
        BoxWithConstraints {
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-10f, 5f, 10f, -5f)
            )
            val textLabelHeight = rememberTextLabelHeight()

            val marks = VerticalMarks(
                label = { l, i, y -> Text("$l, $i, $y")},
                markLevel = MarkLevelExplicitRanges3(
                    listOf(
                        floatArrayOf(-9f, 0f, 8f),
//                        floatArrayOf(-3f, -2f, 0f, 4f),
//                        floatArrayOf(-3f, -2f, -1f, 0f, 2f, 4f),
//                        floatArrayOf(-3f, -2.5f, -2f, -1.5f, -1f, 0f, 1f, 2f, 3f, 4f),
                    ).toImmutableList()
                ),
                maxLabelWidth = { textLabelHeight }
            )
            
            marks.Draw(transformation = transformation)
        }
    }
}