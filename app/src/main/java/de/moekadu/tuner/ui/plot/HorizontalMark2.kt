package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

interface MarkLevel {
    fun getMarksRange(startValue: Float, endValue: Float, maxNumMarks: Int): MarksRange

    fun getMarkValue(level: Int, index: Int): Float
}

data class MarksRange(
    val level: Int,
    val indexBegin: Int,
    val indexEnd: Int
)

class MarkLevelExplicitRanges(
   private val marks: ImmutableList<FloatArray>
) : MarkLevel {
    override fun getMarksRange(startValue: Float, endValue: Float, maxNumMarks: Int): MarksRange {
        var level = 0
        var iBegin = 0
        var iEnd = marks[0].size

        for (l in marks.size-1 downTo 0) {
            level = l
            val m = marks[l]
            val i0 = m.binarySearch(startValue)
            iBegin = if (i0 >= 0) i0 else -i0 - 1
            val i1 = m.binarySearch(endValue)
            iEnd = if (i1 >= 0) i1 else -i1 - 1

            if (iEnd - iBegin >= maxNumMarks)
                break
        }
        return MarksRange(level, iBegin, iEnd)
    }

    override fun getMarkValue(level: Int, index: Int) = marks[level][index]
}

class HorizontalMarks2Group(private val marks: HorizontalMarks2) : ItemGroup2 {
    override val size: Int = 1
    override fun getVisibleItems(
        transformation: Transformation,
        density: Density
    ): Sequence<PlotItemPositioned> {
        return sequence {
            yield(
                PlotItemPositioned(
                    marks,
                    marks.computeExtendedBoundingBoxScreen(transformation, density),
                    0
                )
            )
        }
    }

    override fun get(index: Int): PlotItem = marks
}
class HorizontalMarks2(
    private val label: (@Composable (level: Int, index: Int, y: Float) -> Unit)?,
    private val markLevel: MarkLevel,
    private val defaultAnchor: Anchor = Anchor.Center,
    private val defaultHorizontalLabelPosition: Float = 0.5f,
    private val lineWidth: Dp = 1.dp,
    private val lineColor: @Composable () -> Color = {  Color.Unspecified },
    private val maxLabelHeight: (density: Density) -> Float,
    private val clipLabelToPlotWindow: Boolean = false,
    private val maxNumLabels: Int = 7
): PlotItem {
    override val boundingBox: State<Rect> = mutableStateOf(Rect(
        Float.NEGATIVE_INFINITY,
        Float.POSITIVE_INFINITY,
        Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY
    ))
    private val extraExtents = Rect(0f, 0f, 0f, 0f)


    override fun getExtraExtentsScreen(density: Density): Rect {
        return extraExtents
    }

    @Composable
    override fun Item(transformation: Transformation) {
        val density = LocalDensity.current
        // TODO: when computing range, we must take the label height into account
        val range = remember(transformation, maxNumLabels, markLevel, density) {
            // TODO: this seems not really to correct, I guess, we have to compute a difference
            val labelHeightScreen = Offset(0f, maxLabelHeight(density))
            val labelHeightRaw = transformation.toRaw(labelHeightScreen).y
            markLevel.getMarksRange(
                transformation.viewPortRaw.bottom - labelHeightRaw,
                transformation.viewPortRaw.top + labelHeightRaw,
                maxNumLabels
            )
        }
        val lineColor = lineColor().takeOrElse { MaterialTheme.colorScheme.outline }

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
                .drawBehind {
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
        ) { measureables, constraints ->
            val w = constraints.maxWidth
            val h = constraints.maxHeight
            val c = constraints.copy(minWidth = 0, minHeight = 0)
            val placeables = measureables.map { it.measure(c) }

            layout(w, h){
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
    // given min/max value, I need the ticks inbetween
    // I need a number of ticks, which should not be exceeded
    // I need an option to change the levels, such when the tick number is exceeded,
    //   we can switch to the next coarser level
}