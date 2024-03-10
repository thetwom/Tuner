package de.moekadu.tuner.ui.plot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntOffset

interface PlotItem {
    val boundingBox: State<Rect>

    fun getExtraExtentsScreen(density: Density): Rect

    @Composable
    fun Item(transformation: Transformation)
    //fun Item(transformToScreen: Matrix)
}

fun PlotItem.computeExtendedBoundingBoxScreen(
    transformation: Transformation,
    density: Density
): Rect {
    val bb = this.boundingBox.value
    val boundingBoxRaw = Rect(
        if (bb.left == Float.NEGATIVE_INFINITY) 0f else bb.left,
        if (bb.top == Float.POSITIVE_INFINITY) 0f else bb.top,
        if (bb.right == Float.POSITIVE_INFINITY) 0f else bb.right,
        if (bb.bottom == Float.NEGATIVE_INFINITY) 0f else bb.bottom,
    )
    val bbS = transformation.toScreen(boundingBoxRaw)
    val extraExtents = this.getExtraExtentsScreen(density)
    return Rect(
            if (bb.left == Float.NEGATIVE_INFINITY) Float.NEGATIVE_INFINITY else (bbS.left - extraExtents.left),
            if (bb.top == Float.POSITIVE_INFINITY) Float.NEGATIVE_INFINITY else (bbS.top - extraExtents.top),
            if (bb.right == Float.POSITIVE_INFINITY) Float.POSITIVE_INFINITY else (bbS.right + extraExtents.right),
            if (bb.bottom == Float.NEGATIVE_INFINITY) Float.POSITIVE_INFINITY else (bbS.bottom + extraExtents.bottom),
        )
}