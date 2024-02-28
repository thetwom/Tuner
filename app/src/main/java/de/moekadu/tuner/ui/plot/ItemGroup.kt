package de.moekadu.tuner.ui.plot

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.unit.Density
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus

data class ItemGroup(
    val items: PersistentList<PlotItem> = persistentListOf()
) {
    val size get() = items.size

    fun add(plotItem: PlotItem): ItemGroup {
        val modified = items + plotItem
        return this.copy(items = modified)
    }
    fun numVisibleItems(): Int {
        // tODO: return real number
        return size
    }

    fun getVisibleItems(
        matrixRawToScreen: Matrix,
        viewPortScreen: Rect,
        indexOffset: Int,
        density: Density
    ): Sequence<PlotItemPositioned> {
        // TODO: this should better be configurable, in many cases (when elements are ordered),
        //  we can "filter" much cheaper
        return items.asSequence()
            .mapIndexed { index, plotItem ->
                val boundingBoxScreen = matrixRawToScreen.map(plotItem.boundingBox.value)
                val extendedBoundingBox = with (density){
                    Rect(
                        boundingBoxScreen.left - plotItem.extraExtentsOnScreen.left.roundToPx(),
                        boundingBoxScreen.top - plotItem.extraExtentsOnScreen.top.roundToPx(),
                        boundingBoxScreen.right + plotItem.extraExtentsOnScreen.right.roundToPx(),
                        boundingBoxScreen.bottom + plotItem.extraExtentsOnScreen.bottom.roundToPx(),
                    )
                }
                PlotItemPositioned(
                    plotItem = plotItem,
                    boundingBox = extendedBoundingBox,
                    globalIndex = indexOffset + index
                )
            }
            .filter { viewPortScreen.overlaps(it.boundingBox) }
    }

    operator fun get(index: Int) = items[index]
    // Addtionally needed:
    // - max size
    // -

}