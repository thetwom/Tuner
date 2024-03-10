package de.moekadu.tuner.ui.plot

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.toRect
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlin.math.absoluteValue

interface ItemGroup2 {
    val size: Int
    fun getVisibleItems(
        transformation: Transformation,
        density: Density
    ): Sequence<PlotItemPositioned>

    operator fun get(index: Int): PlotItem
}

data class ItemGroupLines(
    val items: PersistentList<Line> = persistentListOf()
): ItemGroup2 {
    override val size = items.size

    fun add(plotItem: Line): ItemGroupLines {
        val modified = items + plotItem
        return this.copy(items = modified)
    }

    override fun getVisibleItems(transformation: Transformation, density: Density)
            : Sequence<PlotItemPositioned> {
        return items.asSequence()
            .mapIndexed { index, item ->
                val extendedBoundingBox = item.computeExtendedBoundingBoxScreen(
                    transformation, density
                )
                PlotItemPositioned(
                    plotItem = item,
                    boundingBox = extendedBoundingBox,
                    indexInGroup = index
                )
            }
            .filter { transformation.viewPortScreen.toRect().overlaps(it.boundingBox) }
    }

    override operator fun get(index: Int) = items[index]
    // Addtionally needed:
    // - max size
    // -

}