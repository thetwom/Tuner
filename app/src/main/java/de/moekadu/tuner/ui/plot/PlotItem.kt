package de.moekadu.tuner.ui.plot

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableMap

interface PlotItem {
    val hasClippedDraw: Boolean
    val hasUnclippedDraw: Boolean

    @Composable
    fun DrawClipped(transformation: Transformation, plotStyles: ImmutableMap<Int, PlotStyle>)

    @Composable
    fun DrawUnclipped(transformation: Transformation, plotStyles: ImmutableMap<Int, PlotStyle>)
}