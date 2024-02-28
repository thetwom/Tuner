package de.moekadu.tuner.ui.plot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentList

private fun createIndexRanges(groups: PersistentList<ItemGroup>): IntArray {
    val result = IntArray(groups.size + 1)
    var accumulatedSum = 0
    groups.forEachIndexed { index, itemGroup ->
        result[index] = accumulatedSum
        accumulatedSum += itemGroup.size
    }
    result[groups.size]= accumulatedSum

    return result
}

data class PlotState(
    private val groups: PersistentList<ItemGroup>,
    val viewPortRaw: Rect,
    val targetRectForAnimation: TargetRectForAnimation? = null
//    val animatedViewPort: Animatable<Rect, AnimationVector4D> = Animatable(viewPortRaw, Rect.VectorConverter)
) {
    class TargetRectForAnimation(val target: Rect)
    private val indexRanges = createIndexRanges(groups)

    val itemCount get() = indexRanges.last()

    fun setViewPort(viewPortRaw: Rect): PlotState {
        return this.copy(viewPortRaw = viewPortRaw, targetRectForAnimation = null)
    }

    fun animateToViewPort(viewPortRaw: Rect): PlotState {
        return this.copy(targetRectForAnimation = TargetRectForAnimation(viewPortRaw))
    }

    fun addLine(xValues: FloatArray, yValues: FloatArray, lineWidth: Dp): PlotState {
        val line = Line(lineWidth)
        line.setLine(xValues, yValues)
        return add(line, 0)
    }

    fun addHorizontalMarks(
        yPositions: FloatArray,
        horizontalLabelPosition: HorizontalLabelPosition,
        anchor: Anchor,
        content: @Composable (index: Int, yPosition: Float) -> Unit
    ): PlotState {
        val mark = HorizontalMark(horizontalLabelPosition, anchor, DpSize.Unspecified)
        return add(mark, 1)
    }
    fun add(item: PlotItem, indexOfGroup: Int): PlotState {
        val modified = groups.mutate {
            it[indexOfGroup] = groups[indexOfGroup].add(item)
        }
        return this.copy(groups = modified)
    }
    fun get(indexOfGroup: Int, indexWithinGroup: Int) = groups[indexOfGroup][indexWithinGroup]
    operator fun get(index: Int): PlotItem {
        val groupIndex = indexRanges.binarySearch(index)
        val groupIndexResolved = if (groupIndex < 0)  -groupIndex - 2 else groupIndex
        val indexLocal = index - indexRanges[groupIndexResolved]
        return groups[groupIndexResolved][indexLocal]
    }

    fun getVisibleItems(matrixRawToScreen: Matrix, viewPortScreen: Rect, density: Density): Sequence<PlotItemPositioned> {
        return sequence {
            groups.forEachIndexed { groupIndex, group ->
                yieldAll(group.getVisibleItems(
                    matrixRawToScreen,
                    viewPortScreen,
                    groupIndex + indexRanges[groupIndex],
                    density
                ))
            }
        }
    }

    companion object {
        fun create(numGroups: Int, viewPartRaw: Rect): PlotState {
            return PlotState(
                List(numGroups){ ItemGroup() }.toPersistentList(),
                viewPartRaw
            )
        }
    }
}