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
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.ui.common.Label
import de.moekadu.tuner.ui.plot.Anchor
import de.moekadu.tuner.ui.plot.GestureBasedViewPort
import de.moekadu.tuner.ui.plot.HorizontalLinesPositions
import de.moekadu.tuner.ui.plot.LineCoordinates
import de.moekadu.tuner.ui.plot.Plot
import de.moekadu.tuner.ui.plot.PlotWindowOutline
import de.moekadu.tuner.ui.plot.TickLevel
import de.moekadu.tuner.ui.plot.TickLevelDeltaBased
import de.moekadu.tuner.ui.plot.TickLevelExplicitRanges
import de.moekadu.tuner.ui.plot.VerticalLinesPositions
import de.moekadu.tuner.ui.plot.VerticalMark
import de.moekadu.tuner.ui.plot.rememberTextLabelWidth
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

data class CorrelationPlotData(
    val size: Int,
    val timeShift: (i: Int) -> Float,
    val correlation: (i: Int) -> Float,
)
@Composable
fun CorrelationPlot(
    correlationPlotData: CorrelationPlotData,
    targetNote: MusicalNote,
    musicalScale: MusicalScale,
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
    val maximumCorrelation = remember(correlationPlotData) {
        var maxValue = 0f
        for (i in 0 until correlationPlotData.size)
            maxValue = max(maxValue, correlationPlotData.correlation(i))
        maxValue
    }

    val viewPort = remember(targetNote, musicalScale) {
        val noteIndex = musicalScale.getNoteIndex(targetNote)
        val frequency = musicalScale.getNoteFrequency(noteIndex)
        Rect(
            left = 0f,
            top = 1f,
            right = 2.5f / frequency,
            bottom = -1f
        )
    }
    val maximumTimeShift = remember(correlationPlotData) {
        correlationPlotData.timeShift(correlationPlotData.size - 1)
    }

    val viewPortLimits = remember(maximumTimeShift) {
        Rect(
            left = 0f,
            top = 1f,
            right = maximumTimeShift,
            bottom = -1f
        )
    }

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
        Log.v("Tuner", "Longest tick: $longestTick")
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
            val widthDp = with(LocalDensity.current) { w.toDp() }
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
        val horizontalLinePositions = remember {
            HorizontalLinesPositions(1, { 0f })
        }
        HorizontalLines(
            positions = horizontalLinePositions,
            color = tickLineColor,
            lineWidth = tickLineWidth
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

        val maximumCorrelationInverse = 1.0f / maximumCorrelation
        Line(
            LineCoordinates(
                correlationPlotData.size,
                correlationPlotData.timeShift,
                { correlationPlotData.correlation(it) * maximumCorrelationInverse },
            ),
            lineColor = lineColor,
            lineWidth = lineWidth
        )
    }
}

@Preview(widthDp = 400, heightDp = 200, showBackground = true)
@Composable
private fun CorrelationPlotPreview() {
    TunerTheme {
        val timeShifts = remember { FloatArray(20) { 0.0004f * it } }
        val correlations = remember { floatArrayOf(
            10f, 7f, 4f, 2f, 1f, -1f, -3f, -3f, -2f, 2f,
            3f, 4f, 3.8f, 3f, 2f, -1f, 0f, 1f, 0f, 0.3f,
        ) }
        val data = remember {
            CorrelationPlotData(timeShifts.size, { timeShifts[it] }, { correlations[it] })
        }

        val musicalScale = remember { MusicalScaleFactory.create(TemperamentType.EDO12) }
        val targetNote = musicalScale.referenceNote

        CorrelationPlot(
            correlationPlotData = data,
            targetNote = targetNote,
            musicalScale = musicalScale,
            plotWindowPadding = DpRect(left = 3.dp, top = 4.dp, right = 6.dp, bottom = 20.dp),
            currentFrequency = 500f,
        )
    }
}