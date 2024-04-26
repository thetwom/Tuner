package de.moekadu.tuner.ui.plot

import androidx.compose.runtime.Composable

interface PlotItem {
    val hasClippedDraw: Boolean
    val hasUnclippedDraw: Boolean

    @Composable
    fun DrawClipped(transformation: Transformation)

    @Composable
    fun DrawUnclipped(transformation: Transformation)
}