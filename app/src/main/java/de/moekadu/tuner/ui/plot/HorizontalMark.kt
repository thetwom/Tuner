package de.moekadu.tuner.ui.plot

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toRect
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.roundToInt

class HorizontalMarksGroup(
    private val label: (@Composable (index: Int, y: Float) -> Unit)?,
    initialYValues: FloatArray = floatArrayOf(),
    initialMaxLabelHeight: (density: Density) -> Float = { 0f },
    private val defaultAnchor: Anchor = Anchor.Center,
    private val defaultHorizontalLabelPosition: Float = 0.5f,
    private val lineWidth: Dp = 1.dp,
    private val lineColor: @Composable (index: Int, y: Float) -> Color = {_,_ -> Color.Unspecified},
    private val clipLabelToPlotWindow: Boolean = false,
    private val onDataUpdated: (oldGroup: HorizontalMarksGroupImpl, newGroup: HorizontalMarksGroupImpl) -> Unit = { _, _ -> }
) {
    var group: HorizontalMarksGroupImpl
        private set
    var maxLabelHeight = initialMaxLabelHeight
        private set

    init {
        group = HorizontalMarksGroupImpl(createMarks(initialYValues), maxLabelHeight)
    }
    fun setMarks(yValues: FloatArray) {
        val oldGroup = group
        group = group.copy(items = createMarks(yValues))
        onDataUpdated(oldGroup, group)
    }

    private fun createMarks(
        yValues: FloatArray,
        horizontalLabelPosition: Float = defaultHorizontalLabelPosition,
        anchor: Anchor = defaultAnchor
        ): PersistentList<HorizontalMark> {
        return yValues.mapIndexed { index, yValue ->
            HorizontalMark(horizontalLabelPosition, anchor, maxLabelHeight, lineWidth, yValue,
                clipLabelToPlotWindow  = clipLabelToPlotWindow,
                lineColor = { lineColor(index, yValue) },
                label = label?.let{ { it(index, yValue) } }
            )
        }.toPersistentList()
    }
}

data class HorizontalMarksGroupImpl(
    val items: PersistentList<HorizontalMark> = persistentListOf(),
    val maxLabelHeight: (density: Density) -> Float
) : ItemGroup2 {
    private var cachedDensity: Density? = null
    private var maxLabelHeightRaw: Float = -1f

    override val size = items.size

    override fun getVisibleItems(
        transformation: Transformation,
        density: Density
    ): Sequence<PlotItemPositioned> {
        if (maxLabelHeightRaw < 0f || density != cachedDensity) {
            val maxLabelHeightScreen = maxLabelHeight(density)
            val rectScreen = Rect(0f, 0f, 1f, maxLabelHeightScreen)
            val rectRaw = transformation.toRaw(rectScreen)
            maxLabelHeightRaw = rectRaw.height.absoluteValue
        }
        //
        // 0 1 2 3
        val firstItemSearchResult = items.binarySearchBy(transformation.viewPortRaw.bottom) {
            it.boundingBox.value.top + maxLabelHeightRaw
        }
        val itemBegin = if (firstItemSearchResult >= 0)
            firstItemSearchResult
        else
            -firstItemSearchResult - 1
        val lastItemSearchResult = items.binarySearchBy(transformation.viewPortRaw.top) {
            it.boundingBox.value.bottom - maxLabelHeightRaw
        }
        val itemEnd = if (lastItemSearchResult >= 0)
            lastItemSearchResult
        else
            -lastItemSearchResult - 1
        return items.subList(itemBegin, itemEnd).asSequence()
            .mapIndexed { index, item ->
                val extendedBoundingBox = item.computeExtendedBoundingBoxScreen(transformation, density)
                PlotItemPositioned(
                    item,
                    extendedBoundingBox,
                    index + itemBegin
                )
            }
    }
    override operator fun get(index: Int) = items[index]
}

class HorizontalMark(
    val horizontalLabelPosition: Float, // relative: from 0 to 1
    val anchor: Anchor,
    val maxLabelHeight: (density: Density) -> Float,
    val lineWidth: Dp,
    initialYPosition: Float = 0f,
    val clipLabelToPlotWindow: Boolean = false,
    var lineColor: @Composable () -> Color = { Color.Unspecified },
    var label: (@Composable () -> Unit)? = null
) : PlotItem {

    private var yPosition by mutableStateOf(initialYPosition)

    override val boundingBox = mutableStateOf(
        Rect(//0f, 30f, 15f, 15f
            Float.NEGATIVE_INFINITY, initialYPosition,
            Float.POSITIVE_INFINITY, initialYPosition
        )
    )

    override fun getExtraExtentsScreen(density: Density): Rect {
        val h = maxLabelHeight(density)
        val l2 = with(density) {0.5f * lineWidth.toPx() }
        return Rect(
            left = 0f,
            top = when (anchor) {
                Anchor.NorthWest, Anchor.North, Anchor.NorthEast -> l2
                Anchor.West, Anchor.Center, Anchor.East -> max(0.5f * h, l2)
                Anchor.SouthWest, Anchor.South, Anchor.SouthEast -> h + l2
            },
            right = 0f,
            bottom = when (anchor) {
                Anchor.NorthWest, Anchor.North, Anchor.NorthEast -> h + l2
                Anchor.West, Anchor.Center, Anchor.East -> max(0.5f * h, l2)
                Anchor.SouthWest, Anchor.South, Anchor.SouthEast -> l2
            }
        )
    }

    @Composable
    override fun Item(transformation: Transformation) {
        val yTransformed = remember(transformation, yPosition) {
            val yOffset = Offset(0f, yPosition)
            val yOffsetScreen = transformation.toScreen(yOffset)
            yOffsetScreen.y
        }
//        Log.v("Tuner", "HorizontalMark, draw $yPosition")
        val lineColor = this.lineColor().takeOrElse { MaterialTheme.colorScheme.outline }

        val isLabelVisible = (clipLabelToPlotWindow ||
                (yTransformed < transformation.viewPortScreen.bottom &&
                        yTransformed > transformation.viewPortScreen.top))
        val clipShape = remember(
            transformation.viewPortScreen, transformation.viewPortCornerRadius) {
            GenericShape { _, _ ->
                val r = CornerRadius(transformation.viewPortCornerRadius)
                addRoundRect(RoundRect(transformation.viewPortScreen.toRect(), r, r, r, r))
            }
        }

        Box(modifier = Modifier
            .fillMaxSize()
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize().clip(clipShape)
            ) {
                drawLine(
                    lineColor,
                    Offset(transformation.viewPortScreen.left.toFloat(), yTransformed),
                    Offset(transformation.viewPortScreen.right.toFloat(), yTransformed),
                    strokeWidth = lineWidth.toPx()
                )
            }
            val l = label
            if (isLabelVisible && l != null) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .then(if (clipLabelToPlotWindow) Modifier.clip(clipShape) else Modifier)
                    .layout { measurable, constraints ->
                        val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))

                        layout(constraints.maxWidth, constraints.maxHeight) {
                            val vp = transformation.viewPortScreen
                            val x = vp.left + horizontalLabelPosition * (vp.width)
                            val y = yTransformed
                            val w = p.width
                            val h = p.height
                            val l2 = 0.5f * lineWidth.toPx()

                            when (anchor) {
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
                ) {
                    l()
                }
            }
        }
//        Box(modifier = Modifier
//            .fillMaxSize()
//            .then(if (visible) Modifier.drawBehind {
//                drawLine(
//                    lineColor,
//                    Offset(transformation.viewPortScreen.left.toFloat(), yTransformed),
//                    Offset(transformation.viewPortScreen.right.toFloat(), yTransformed),
//                    strokeWidth = lineWidth.toPx()
//                )
//            } else {
//                Modifier
//            })
//            .layout { measurable, constraints ->
//                val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
//
//                layout(constraints.maxWidth, constraints.maxHeight) {
//
//                    if (visible) {
//                        val vp = transformation.viewPortScreen
//                        val x = vp.left + horizontalLabelPosition * (vp.width)
//                        val y = yTransformed
//                        val w = p.width
//                        val h = p.height
//                        val l2 = 0.5f * lineWidth.toPx()
//
//                        // TODO: line width must taken into account for North..., South... nachors
//                        when (anchor) {
//                            Anchor.NorthWest -> p.place(x.roundToInt(), (y + l2).roundToInt())
//                            Anchor.North -> p.place(
//                                (x - 0.5 * w).roundToInt(),
//                                (y + l2).roundToInt()
//                            )
//
//                            Anchor.NorthEast -> p.place(
//                                (x - w).roundToInt(),
//                                (y + l2).roundToInt()
//                            )
//
//                            Anchor.West -> p.place(x.roundToInt(), (y - 0.5f * h).roundToInt())
//                            Anchor.Center -> p.place(
//                                (x - 0.5f * w).roundToInt(),
//                                (y - 0.5f * h).roundToInt()
//                            )
//
//                            Anchor.East -> p.place(
//                                (x - w).roundToInt(),
//                                (y - 0.5f * h).roundToInt()
//                            )
//
//                            Anchor.SouthWest -> p.place(
//                                x.roundToInt(),
//                                (y - h - l2).roundToInt()
//                            )
//
//                            Anchor.South -> p.place(
//                                (x - 0.5 * w).roundToInt(),
//                                (y - h - l2).roundToInt()
//                            )
//
//                            Anchor.SouthEast -> p.place(
//                                (x - w).roundToInt(),
//                                (y - h - l2).roundToInt()
//                            )
//                        }
//                    }
//                }
//            }
//        ) {
//            label?.let { it() }
//        }
    }
}
