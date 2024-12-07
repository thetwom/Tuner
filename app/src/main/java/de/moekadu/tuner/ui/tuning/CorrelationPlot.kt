/*
* Copyright 2024 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.moekadu.tuner.ui.tuning

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments2.MusicalScale2
import de.moekadu.tuner.temperaments2.MusicalScale2Factory
import de.moekadu.tuner.ui.common.Label
import de.moekadu.tuner.ui.plot.Anchor
import de.moekadu.tuner.ui.plot.GestureBasedViewPort
import de.moekadu.tuner.ui.plot.HorizontalLinesPositions
import de.moekadu.tuner.ui.plot.LineCoordinates
import de.moekadu.tuner.ui.plot.Plot
import de.moekadu.tuner.ui.plot.PlotWindowOutline
import de.moekadu.tuner.ui.plot.TickLevelDeltaBased
import de.moekadu.tuner.ui.plot.VerticalMark
import de.moekadu.tuner.ui.plot.rememberTextLabelWidth
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.pow

@Composable
fun CorrelationPlot(
    correlationPlotData: LineCoordinates, // TODO: pass zero position
    correlationPlotDataYZeroPosition: Float,
    targetNote: MusicalNote,
    musicalScale: MusicalScale2,
    modifier: Modifier = Modifier,
    gestureBasedViewPort: GestureBasedViewPort = remember { GestureBasedViewPort() },
    plotWindowPadding: DpRect = DpRect(0.dp, 0.dp, 0.dp, 0.dp),
    // line properties
    lineWidth: Dp = 2.dp,
    lineColor: Color = MaterialTheme.colorScheme.onSurface,
    // tick properties
    tickLineWidth: Dp = 1.dp,
    tickLineColor: Color = MaterialTheme.colorScheme.outline,
    tickLabelStyle: TextStyle = MaterialTheme.typography.labelMedium,
    tickLabelColor: Color = MaterialTheme.colorScheme.onSurface,
    // current frequency
    currentFrequency: Float? = null,
    frequencyMarkLineWidth: Dp = 1.dp,
    frequencyMarkLineColor: Color = MaterialTheme.colorScheme.secondary,
    frequencyMarkTextStyle: TextStyle = MaterialTheme.typography.labelMedium,
    // outline
    plotWindowOutline: PlotWindowOutline = PlotWindowOutline()
) {
    val viewPort = remember(targetNote, musicalScale) {
        val noteIndex = musicalScale.getNoteIndex(targetNote)
        val frequency = musicalScale.getNoteFrequency(noteIndex)
        Rect(
            left = 0f,
            top = 1f,
            right = 2.5f / frequency,
            bottom = 0f
        )
    }
    val maximumTimeShift = remember(correlationPlotData) {
        //correlationPlotData.timeShift(correlationPlotData.size - 1)
        correlationPlotData.coordinates.lastOrNull()?.x ?: 100f
    }

    val viewPortLimits = remember(maximumTimeShift) {
        Rect(
            left = 0f,
            top = 1f,
            right = maximumTimeShift,
            bottom = 0f
        )
    }
//    Log.v("Tuner", "CorrelationPlot: viewPort=$viewPort")
    Plot(
        modifier = modifier,
        viewPort = viewPort,
        viewPortGestureLimits = viewPortLimits,
        gestureBasedViewPort = gestureBasedViewPort,
        plotWindowPadding = plotWindowPadding,
        plotWindowOutline = plotWindowOutline,
        lockX = false,
        lockY = true
    ) {
        val resolutions = remember {
           //floatArrayOf(0.0001f, 0.0002f, 0.0004f, 0.0008f, 0.0016f, 0.0032f, 0.0064f, 0.0128f, 0.0256f)
            //floatArrayOf(0.0256f, 0.0128f, 0.0064f, 0.0032f, 0.0016f, 0.0008f, 0.0004f, 0.0002f, 0.0001f)
            //floatArrayOf(0.0016f, 0.0008f, 0.0004f, 0.0002f, 0.0001f) // TODO: get this right,
            FloatArray(100) { 0.0001f * 2f.pow(it / 8f) }.toList().toImmutableList()
        }
        val tickLevel = remember {
//            val maxValue = 0.1f // freq shift of 10Hz
//            val levelList = resolutions.map { resolution ->
//                val numValues = (maxValue / resolution).toInt()
//                FloatArray(numValues) {
//                    //val proposedTick = (it + 1) * resolution // we can't use 0 * resolution, otherwise we get division by zero later ...
//                    //val closestIntegerFrequency = (1.0f / proposedTick).roundToInt()
//                    //1.0f / closestIntegerFrequency
//                    (it + 1) * resolution
//                }
//            }.toImmutableList()
//            Log.v("Tuner", "Creating tick level")
//            TickLevelExplicitRanges(levelList)
            TickLevelDeltaBased(resolutions)
        }
        val resources = LocalContext.current.resources
        val longestTick = remember(resources) {
            resources.getString(R.string.hertz_1f, 1.0f / resolutions[1])
        }
        //Log.v("Tuner", "Longest tick: $longestTick")
        XTicks(
            tickLevel = tickLevel,
            maxLabelWidth = rememberTextLabelWidth(
                testString = longestTick,
                style = tickLabelStyle
            ),
            verticalLabelPosition = 0f,
            anchor = Anchor.North,
            lineColor = tickLineColor,
            lineWidth = tickLineWidth,
            maxNumLabels = -1,
            clipLabelToPlotWindow = false
        ) { m, _, _, x ->
            val w = rememberTextLabelWidth(testString = longestTick)
            //val widthDp = with(LocalDensity.current) { w.toDp() }
            val text = if (x == 0f) "" else stringResource(id = R.string.hertz_1f, 1 / x)
            //val text = longestTick
            Text(
                text,
                modifier = m, //.width(widthDp).background(MaterialTheme.colorScheme.error),
                textAlign = TextAlign.Center,
                style = tickLabelStyle,
                color = tickLabelColor
            )
        }
        val horizontalLinePositions = remember(correlationPlotDataYZeroPosition) {
            HorizontalLinesPositions.create(floatArrayOf(correlationPlotDataYZeroPosition))
        }
        HorizontalLines(
            positions = horizontalLinePositions,
            color = tickLineColor,
            lineWidth = tickLineWidth
        )

        Line(
            correlationPlotData,
            lineColor = lineColor,
            lineWidth = lineWidth
        )

        if (currentFrequency != null) {
            VerticalMarks(
                marks = persistentListOf(
                    VerticalMark(
                        1.0f / currentFrequency,
                        VerticalMark.Settings(
                            anchor = Anchor.SouthWest,
                            labelPosition = 0f,
                            lineWidth = frequencyMarkLineWidth,
                            lineColor = frequencyMarkLineColor
                        )
                    ) {markModifier ->
                        Label(
                            content = {
                                Text(
                                    stringResource(id = R.string.hertz_1f, currentFrequency),
                                    style = frequencyMarkTextStyle,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            },
                            modifier = markModifier,
                            color = frequencyMarkLineColor
                        )
                    }
                )
            )
        }
    }
}

@Preview(widthDp = 400, heightDp = 200, showBackground = true)
@Composable
private fun CorrelationPlotPreview() {
    TunerTheme {
        val timeShifts = remember { FloatArray(20) { 0.0004f * it } }
        // TODO: we now expect normalized data here.
        val correlations = remember { floatArrayOf(
            10f, 7f, 4f, 2f, 1f, -1f, -3f, -3f, -2f, 2f,
            3f, 4f, 3.8f, 3f, 2f, -1f, 0f, 1f, 0f, 0.3f,
        ) }
        val correlationPlotDataYZeroPosition = 0f
        val data = remember {
            LineCoordinates.create(timeShifts, correlations)
            //CorrelationPlotData(timeShifts.size, { timeShifts[it] }, { correlations[it] })
        }

        val musicalScale = remember { MusicalScale2Factory.createTestEdo12() }
        val targetNote = musicalScale.referenceNote

        CorrelationPlot(
            correlationPlotData = data,
            correlationPlotDataYZeroPosition,
            targetNote = targetNote,
            musicalScale = musicalScale,
            plotWindowPadding = DpRect(left = 3.dp, top = 4.dp, right = 6.dp, bottom = 20.dp),
            currentFrequency = 500f,
        )
    }
}