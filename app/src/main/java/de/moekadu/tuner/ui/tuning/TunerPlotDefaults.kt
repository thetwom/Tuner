package de.moekadu.tuner.ui.tuning

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import de.moekadu.tuner.ui.plot.PlotWindowOutline

class TunerPlotDefaults(
    val tickFontStyle: TextStyle,
    val tickLineWidth: Dp,
    val tickLineColor: Color,
    val toleranceTickFontStyle: TextStyle,
    val plotPointSize: Dp,
    val plotPointSizeInactive: Dp,
    val plotLineWidth: Dp,
    val plotLineWidthInactive: Dp,
    val negativeColor: Color,
    val positiveColor: Color,
    val inactiveColor: Color,
    val plotWindowOutline: PlotWindowOutline
)