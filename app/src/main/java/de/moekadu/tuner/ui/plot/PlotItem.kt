package de.moekadu.tuner.ui.plot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntOffset

interface PlotItem {
    val boundingBox: State<Rect>
    val extraExtentsOnScreen: DpRect

    @Composable
    fun Item(transformToScreen: Matrix)
}