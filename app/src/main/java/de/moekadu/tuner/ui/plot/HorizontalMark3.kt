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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.roundToInt

interface MarkLevel3 {
    fun getMarksRange(
        startValue: Float,
        endValue: Float,
        maxNumMarks: Int,
        labelHeightRaw: Float
    ): MarksRange3

    fun getMarkValue(level: Int, index: Int): Float
}

data class MarksRange3(
    val level: Int,
    val indexBegin: Int,
    val indexEnd: Int
)

class MarkLevelExplicitRanges3(
   private val marks: ImmutableList<FloatArray>
) : MarkLevel3 {
    override fun getMarksRange(
        startValue: Float,
        endValue: Float,
        maxNumMarks: Int,
        labelHeightRaw: Float
    ): MarksRange3 {
        var level = 0

        for (l in marks.size-1 downTo 0) {
            level = l
            val m = marks[l]
            val i0 = m.binarySearch(startValue)
            val iBegin = if (i0 >= 0) i0 else -i0 - 1
            val i1 = m.binarySearch(endValue)
            val iEnd = if (i1 >= 0) i1 else -i1 - 1

            if (iEnd - iBegin <= maxNumMarks)
                break
        }

        val i0 = marks[level].binarySearch(startValue - labelHeightRaw)
        val iBegin = if (i0 >= 0) i0 else -i0 - 1
        val i1 = marks[level].binarySearch(endValue + labelHeightRaw)
        val iEnd = if (i1 >= 0) i1 else -i1 - 1

        return MarksRange3(level, iBegin, iEnd)
    }

    override fun getMarkValue(level: Int, index: Int) = marks[level][index]
}

/** Helper function to compute height of text labels.
 * @param style Text style.
 * @param density Density of measurement environment.
 * @param paddingTop Padding above text.
 * @param paddingTop Padding below text.
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

class HorizontalMarks3(
    private val label: (@Composable (level: Int, index: Int, y: Float) -> Unit)?,
    private val markLevel: MarkLevel3,
    private val maxLabelHeight: Density.() -> Float,
    private val defaultAnchor: Anchor = Anchor.Center,
    private val defaultHorizontalLabelPosition: Float = 0.5f,
    private val lineWidth: Dp = 1.dp,
    private val lineColor: @Composable () -> Color = {  Color.Unspecified },
    private val clipLabelToPlotWindow: Boolean = false,
    private val maxNumLabels: Int = -1
): PlotGroup {
    @Composable
    override fun Draw(transformation: Transformation) {
        val density = LocalDensity.current
        val lineWidthPx = with(density) { lineWidth.toPx() }
        val maxLabelHeightPx = density.maxLabelHeight()
        val maxNumLabelsResolved = if (maxNumLabels <= 0)
            (transformation.viewPortScreen.height / maxLabelHeightPx / 2f).roundToInt()
        else
            maxNumLabels
        val range = remember(transformation, maxNumLabelsResolved, markLevel, maxLabelHeightPx) {
            val labelHeightScreen = Rect(
                0f, 0f, 1f, maxLabelHeightPx + 0.5f * lineWidthPx
            )

            val labelHeightRaw = transformation.toRaw(labelHeightScreen).height
            markLevel.getMarksRange(
                transformation.viewPortRaw.bottom - labelHeightRaw,
                transformation.viewPortRaw.top + labelHeightRaw,
                maxNumLabelsResolved,
                labelHeightRaw
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
                    val yOffset = Offset(0f, markLevel.getMarkValue(range.level, i))
                    val yTransformed = transformation.toScreen(yOffset).y
                    drawLine(
                        lineColor,
                        Offset(transformation.viewPortScreen.left.toFloat(), yTransformed),
                        Offset(transformation.viewPortScreen.right.toFloat(), yTransformed),
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
                        val yOffset = Offset(
                            0f,
                            markLevel.getMarkValue(range.level, range.indexBegin + index)
                        )
                        val yTransformed = transformation.toScreen(yOffset).y

                        val vp = transformation.viewPortScreen
                        val x = vp.left + defaultHorizontalLabelPosition * (vp.width)
                        val y = yTransformed
                        val w = p.width
                        val h = p.height
                        val l2 = 0.5f * lineWidth.toPx()

                        when (defaultAnchor) {
                            Anchor.NorthWest -> p.place(x.roundToInt(), (y + l2).roundToInt())
                            Anchor.North -> p.place(
                                (x - 0.5 * w).roundToInt(),
                                (y + l2).roundToInt()
                            )

                            Anchor.NorthEast -> p.place(
                                (x - w).roundToInt(),
                                (y + l2).roundToInt()
                            )

                            Anchor.West -> p.place(x.roundToInt(), (y - 0.5f * h).roundToInt())
                            Anchor.Center -> p.place(
                                (x - 0.5f * w).roundToInt(),
                                (y - 0.5f * h).roundToInt()
                            )

                            Anchor.East -> p.place(
                                (x - w).roundToInt(),
                                (y - 0.5f * h).roundToInt()
                            )

                            Anchor.SouthWest -> p.place(
                                x.roundToInt(),
                                (y - h - l2).roundToInt()
                            )

                            Anchor.South -> p.place(
                                (x - 0.5 * w).roundToInt(),
                                (y - h - l2).roundToInt()
                            )

                            Anchor.SouthEast -> p.place(
                                (x - w).roundToInt(),
                                (y - h - l2).roundToInt()
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
private fun HorizontalMarks3Preview() {
    TunerTheme {
        BoxWithConstraints {
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-10f, 5f, 10f, -5f)
            )
            val textLabelHeight = rememberTextLabelHeight()

            val marks = HorizontalMarks3(
                label = { l, i, y -> Text("$l, $i, $y")},
                markLevel = MarkLevelExplicitRanges3(
                    listOf(
                        floatArrayOf(-3f, -2f, 0f, 4f),
                        floatArrayOf(-3f, -2f, -1f, 0f, 2f, 4f),
                        floatArrayOf(-3f, -2.5f, -2f, -1.5f, -1f, 0f, 1f, 2f, 3f, 4f),
                    ).toImmutableList()
                ),
                maxLabelHeight = { textLabelHeight }
            )
            
            marks.Draw(transformation = transformation)
        }
    }
}