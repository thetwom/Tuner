package de.moekadu.tuner.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.moekadu.tuner.ui.plot.PlotWindowOutline
import de.moekadu.tuner.ui.theme.tunerColors
import de.moekadu.tuner.ui.theme.tunerTypography

data class TunerPlotStyle(
    val tickFontStyle: TextStyle,
    val tickLineWidth: Dp,
    val tickLineColor: Color,
    val toleranceTickFontStyle: TextStyle,
    val toleranceColor: Color,
    val plotPointSize: Dp,
    val plotPointSizeInactive: Dp,
    val plotLineWidth: Dp,
    val plotLineWidthInactive: Dp,
    val plotLineColor: Color,
    val negativeColor: Color,
    val positiveColor: Color,
    val inactiveColor: Color,
    val stringColor: Color,
    val onNegativeColor: Color,
    val onPositiveColor: Color,
    val onInactiveColor: Color,
    val onStringColor: Color,
    val targetNoteLineWith: Dp,
    val extraMarkLineWidth: Dp,
    val extraMarkLineColor: Color,
    val extraMarkTextStyle: TextStyle,
    val plotHeadlineStyle: TextStyle,
    val stringFontStyle: TextStyle,
    val plotWindowOutline: PlotWindowOutline,
    val plotWindowOutlineDuringGesture: PlotWindowOutline,
    val margin: Dp
) {
    companion object {
        @Composable
        fun create(
            tickFontStyle: TextStyle = MaterialTheme.tunerTypography.plotSmall,
            tickLineWidth: Dp = 1.dp,
            tickLineColor: Color = MaterialTheme.colorScheme.outline,
            toleranceTickFontStyle: TextStyle = MaterialTheme.tunerTypography.plotMedium,
            toleranceColor: Color = MaterialTheme.colorScheme.inverseSurface,
            plotPointSize: Dp = 15.dp,
            plotPointSizeInactive: Dp = 10.dp,
            plotLineWidth: Dp = 3.dp,
            plotLineWidthInactive: Dp = 2.dp,
            plotLineColor: Color = MaterialTheme.colorScheme.onSurface,
            negativeColor: Color = MaterialTheme.tunerColors.negative,
            positiveColor: Color = MaterialTheme.tunerColors.positive,
            inactiveColor: Color = MaterialTheme.colorScheme.outline,
            stringColor: Color = MaterialTheme.colorScheme.inverseSurface,
            onNegativeColor: Color = MaterialTheme.tunerColors.onNegative,
            onPositiveColor: Color = MaterialTheme.tunerColors.onPositive,
            onInactiveColor: Color = MaterialTheme.colorScheme.onSurface,
            onStringColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
            targetNoteLineWith: Dp = 2.dp,
            extraMarkLineWidth: Dp = 1.dp,
            extraMarkLineColor: Color = MaterialTheme.colorScheme.primary,
            extraMarkTextStyle: TextStyle = MaterialTheme.tunerTypography.plotMedium,
            plotHeadlineStyle: TextStyle = MaterialTheme.typography.titleSmall,
            stringFontStyle: TextStyle = MaterialTheme.tunerTypography.plotLarge,
            plotWindowOutline: PlotWindowOutline = PlotWindowOutline(
                lineWidth = 1.5.dp,
                cornerRadius = 8.dp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            plotWindowOutlineDuringGesture: PlotWindowOutline = PlotWindowOutline(
                lineWidth = 4.dp,
                cornerRadius = 8.dp,
                color = MaterialTheme.colorScheme.primary
            ),
            margin: Dp = 8.dp
        ): TunerPlotStyle {
            return TunerPlotStyle(
                tickFontStyle,
                tickLineWidth,
                tickLineColor,
                toleranceTickFontStyle,
                toleranceColor,
                plotPointSize,
                plotPointSizeInactive,
                plotLineWidth,
                plotLineWidthInactive,
                plotLineColor,
                negativeColor,
                positiveColor,
                inactiveColor,
                stringColor,
                onNegativeColor,
                onPositiveColor,
                onInactiveColor,
                onStringColor,
                targetNoteLineWith,
                extraMarkLineWidth,
                extraMarkLineColor,
                extraMarkTextStyle,
                plotHeadlineStyle,
                stringFontStyle,
                plotWindowOutline,
                plotWindowOutlineDuringGesture,
                margin
            )
        }
    }
}