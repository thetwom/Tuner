package de.moekadu.tuner.ui.plot2

import androidx.compose.runtime.Composable
import de.moekadu.tuner.ui.plot.Transformation

interface PlotItem2 {
    val hasClippedDraw: Boolean
    val hasUnclippedDraw: Boolean

    @Composable
    fun DrawClipped()

    @Composable
    fun DrawUnclipped()
}
