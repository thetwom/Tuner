package de.moekadu.tuner.ui.tuning

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
import de.moekadu.tuner.ui.plot.LineCoordinates
import de.moekadu.tuner.ui.plot.Plot
import de.moekadu.tuner.ui.plot.PlotWindowOutline
import de.moekadu.tuner.ui.plot.TickLevelDeltaBased
import de.moekadu.tuner.ui.plot.VerticalLinesPositions
import de.moekadu.tuner.ui.plot.VerticalMark
import de.moekadu.tuner.ui.plot.rememberTextLabelWidth
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.max

//class FrequencyPlotModel(musicalScale: MusicalScale) {
//    var frequencyPlotData by mutableStateOf(FrequencyPlotData(0, { 0f }, { 0f }))
//    var targetNote by mutableStateOf(musicalScale.referenceNote)
//    var currentFrequency by mutableStateOf<Float?>(null)
//    var harmonicFrequencies by mutableStateOf<VerticalLinesPositions?>(null)
//    val gestureBasedViewPort = GestureBasedViewPort()
//}

data class FrequencyPlotData(
    val size: Int,
    val frequency: (i: Int) -> Float,
    val amplitude: (i: Int) -> Float,
)
@Composable
fun FrequencyPlot(
    frequencyPlotData: FrequencyPlotData,
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
    // harmonics
    harmonicFrequencies: VerticalLinesPositions? = null,
    harmonicLineWidth: Dp = 1.dp,
    harmonicLineColor: Color = MaterialTheme.colorScheme.secondary,
    // outline
    plotWindowOutline: PlotWindowOutline = PlotWindowOutline()
) {
    val maximumAmplitude = remember(frequencyPlotData) {
        var maxValue = 0f
        for (i in 0 until frequencyPlotData.size)
            maxValue = max(maxValue, frequencyPlotData.amplitude(i))
        maxValue
    }
    val viewPort = remember(targetNote, musicalScale) {
        val noteIndex = musicalScale.getNoteIndex(targetNote)
        val frequency = musicalScale.getNoteFrequency(noteIndex)
        Rect(
            left = 0f,
            top = 1f,
            right = 3.5f * frequency,
            bottom = 0f
        )
    }
    val maximumFrequency = remember(frequencyPlotData) {
        frequencyPlotData.frequency(frequencyPlotData.size - 1)
    }

    val viewPortLimits = remember(maximumFrequency) {
        Rect(
            left = 0f,
            top = 1f,
            right = maximumFrequency,
            bottom = 0f
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
        val tickLevel = remember(maximumFrequency) {
            TickLevelDeltaBased(persistentListOf(
                10f, 20f, 25f, 50f, 100f, 200f, 250f, 500f, 1000f, 2000f, 2500f, 5000f, 10000f
            ))
        }
        XTicks(
            tickLevel = tickLevel,
            maxLabelWidth = rememberTextLabelWidth(testString = "99999XX"),
            verticalLabelPosition = 0f,
            anchor = Anchor.North,
            lineColor = tickLineColor,
            lineWidth = tickLineWidth,
            maxNumLabels = -1,
            clipLabelToPlotWindow = false
        ) { m, _, _, x ->
            Text(
                if (x == 0f) "" else stringResource(id = R.string.hertz, x),
                modifier = m,
                style = tickLabelStyle,
                color = tickLabelColor
            )
        }

        if (harmonicFrequencies != null) {
            VerticalLines(
                positions = harmonicFrequencies,
                color = harmonicLineColor,
                lineWidth = harmonicLineWidth
            )
        }

        if (currentFrequency != null) {
            VerticalMarks(
                marks = persistentListOf(
                    VerticalMark(
                        currentFrequency,
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
                                    style = frequencyMarkTextStyle
                                )
                            },
                            modifier = markModifier,
                            color = frequencyMarkLineColor
                        )
                    }
                )
            )
        }

        val maximumAmplitudeInverse = 1.0f / maximumAmplitude
        Line(
            LineCoordinates(
                frequencyPlotData.size,
                frequencyPlotData.frequency,
                { frequencyPlotData.amplitude(it) * maximumAmplitudeInverse },
            ),
            lineColor = lineColor,
            lineWidth = lineWidth
        )
    }
}

@Preview(widthDp = 400, heightDp = 200, showBackground = true)
@Composable
private fun FrequencyPlotPreview() {
    TunerTheme {
        val frequencies = remember { FloatArray(20) { 200f * it } }
        val amplitudes = remember { floatArrayOf(
            0f, 1f, 2f, 2f, 1f, 7f, 5f, 2f, 1f, 0.3f,
            1f, 2f, 3f, 3f, 5f, 2f, 1f, 2f, 0f, 0.3f,
        ) }
        val data = remember {
            FrequencyPlotData(frequencies.size, { frequencies[it] }, { amplitudes[it] })
        }
        val harmonicFrequencies = remember { floatArrayOf(300f, 510f, 800f) }
        val harmonicFrequencyData = remember {
            VerticalLinesPositions(harmonicFrequencies.size, { harmonicFrequencies[it] })
        }
        val musicalScale = remember { MusicalScaleFactory.create(TemperamentType.EDO12) }
        val targetNote = musicalScale.referenceNote

        FrequencyPlot(
            frequencyPlotData = data,
            targetNote = targetNote,
            musicalScale = musicalScale,
            plotWindowPadding = DpRect(left = 3.dp, top = 4.dp, right = 6.dp, bottom = 20.dp),
            currentFrequency = 1200f,
            harmonicFrequencies = harmonicFrequencyData,
            harmonicLineColor = MaterialTheme.colorScheme.error
        )
    }
}