package de.moekadu.tuner.ui.plot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

private fun createIndexRanges(groups: PersistentList<ItemGroup2>): IntArray {
    val result = IntArray(groups.size + 1)
    var accumulatedSum = 0
    groups.forEachIndexed { index, itemGroup ->
        result[index] = accumulatedSum
        accumulatedSum += itemGroup.size
    }
    result[groups.size]= accumulatedSum

    return result
}

class PlotState(
    initialGroups: PersistentList<ItemGroup2>,
    initialViewPortRaw: Rect
) {
    var state by mutableStateOf(PlotStateImpl(initialGroups, initialViewPortRaw))
        private set

    fun setViewPort(viewPortRaw: Rect) {
        state = state.copy(viewPortRaw = viewPortRaw, targetRectForAnimation = null)
    }

    fun animateToViewPort(viewPortRaw: Rect) {
         state = state.copy(targetRectForAnimation = PlotStateImpl.TargetRectForAnimation(viewPortRaw))
    }

    // TODO: instead of returning a line, we should better define a line together with a key
    fun addLine(xValues: FloatArray, yValues: FloatArray, lineWidth: Dp): Line {
        val line = Line(lineWidth) // should we maybe directly pass x/y values at creation?
        line.setLine(xValues, yValues)
        val modifiedLines = (state.groups[INDEX_LINE] as ItemGroupLines).add(line)
        state = state.replaceGroup(INDEX_LINE, modifiedLines)
        return line
    }

    // TODO: instead of returning group, we should better define a group together with a key
    fun addHorizontalMarks(
        yValues: FloatArray,
        maxLabelHeight: (density: Density) -> Float,
        horizontalLabelPosition: Float,
        anchor: Anchor,
        lineWidth: Dp,
        lineColor: @Composable (index: Int, y:Float) -> Color = {_,_-> Color.Unspecified},
        label: (@Composable (index: Int, y: Float) -> Unit)? = null
    ): HorizontalMarksGroup {
        val marks = HorizontalMarksGroup(
            label,
            yValues,
            maxLabelHeight,
            anchor,
            horizontalLabelPosition,
            lineWidth,
            lineColor
        )
        state = state.addGroup(marks.group)
        return marks
    }

    companion object {
        const val INDEX_LINE = 0

        fun create(viewPartRaw: Rect): PlotState {
            return PlotState(
                persistentListOf(ItemGroupLines()),
                viewPartRaw
            )
        }
    }

}

data class PlotStateImpl(
    val groups: PersistentList<ItemGroup2>,
    val viewPortRaw: Rect,
    val targetRectForAnimation: TargetRectForAnimation? = null
) {
    class TargetRectForAnimation(val target: Rect)
    private val indexRanges = createIndexRanges(groups)

    val itemCount get() = indexRanges.last()

    fun addGroup(group: ItemGroup2): PlotStateImpl {
        val modified = groups.mutate {
            it.add(group)
        }
        return this.copy(groups = modified)
    }

    fun replaceGroup(indexOfGroup: Int, group: ItemGroup2): PlotStateImpl {
        val modified = groups.mutate {
            it[indexOfGroup] = group
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

    fun getVisibleItems(transformation: Transformation, density: Density): Sequence<PlotItemPositioned> {
        return sequence {
            groups.forEachIndexed { groupIndex, group ->
                yieldAll(group.getVisibleItems(transformation, density).map {
                    it.globalIndex = it.indexInGroup + indexRanges[groupIndex]
                    it
                })
            }
        }
    }
}