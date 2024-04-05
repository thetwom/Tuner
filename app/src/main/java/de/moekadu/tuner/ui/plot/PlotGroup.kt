package de.moekadu.tuner.ui.plot

import androidx.compose.runtime.Composable

interface PlotGroup {
    @Composable
    fun Draw(transformation: Transformation)
}