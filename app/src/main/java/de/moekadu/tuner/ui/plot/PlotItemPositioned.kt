package de.moekadu.tuner.ui.plot

import androidx.compose.ui.geometry.Rect

data class PlotItemPositioned(
    val plotItem: PlotItem,
    val boundingBox: Rect,
    val indexInGroup: Int,
    var globalIndex: Int = -1
)