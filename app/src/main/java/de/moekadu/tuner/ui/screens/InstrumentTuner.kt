package de.moekadu.tuner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moekadu.tuner.notedetection.TuningState
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.ui.instruments.Strings
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.rememberMaxNoteSize
import de.moekadu.tuner.ui.plot.GestureBasedViewPort
import de.moekadu.tuner.ui.plot.rememberTextLabelHeight
import de.moekadu.tuner.ui.theme.TunerTheme
import de.moekadu.tuner.ui.tuning.PitchHistory
import de.moekadu.tuner.ui.tuning.PitchHistoryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface InstrumentTunerData {
    val musicalScale: StateFlow<MusicalScale>
    val notePrintOptions: StateFlow<NotePrintOptions>
    val toleranceInCents: StateFlow<Int>


    // Data specific to history plot
    val pitchHistoryState: PitchHistoryState

    val pitchHistoryGestureBasedViewPort: GestureBasedViewPort
    val tuningState: TuningState

    // Data shared over different plots
    val currentFrequency: Float?
    val targetNote: MusicalNote
}

@Composable
fun InstrumentTuner(
    data: InstrumentTunerData,
    modifier: Modifier = Modifier,
    tunerPlotStyle: TunerPlotStyle = TunerPlotStyle.create()
) {
    Column(
        modifier = modifier
    ) {
        val musicalScaleAsState by data.musicalScale.collectAsStateWithLifecycle()
        val notePrintOptionsAsState by data.notePrintOptions.collectAsStateWithLifecycle()
        val toleranceInCentsAsState by data.toleranceInCents.collectAsStateWithLifecycle()
        val tickHeightPx = rememberTextLabelHeight(tunerPlotStyle.tickFontStyle)
        val tickHeightDp = with(LocalDensity.current) { tickHeightPx.toDp() }
        val noteWidthDp = rememberMaxNoteSize(
            noteNameScale = musicalScaleAsState.noteNameScale,
            notePrintOptions = notePrintOptionsAsState,
            fontSize = tunerPlotStyle.tickFontStyle.fontSize,
            octaveRange = musicalScaleAsState.getNote(
                musicalScaleAsState.noteIndexBegin
            ).octave .. musicalScaleAsState.getNote(
                musicalScaleAsState.noteIndexEnd - 1
            ).octave
        ).width + 8.dp
        val scope = rememberCoroutineScope()
//        Strings(
//
//        )
        PitchHistory(
            state = data.pitchHistoryState,
            musicalScale = musicalScaleAsState,
            notePrintOptions = notePrintOptionsAsState,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    scope.launch { data.pitchHistoryGestureBasedViewPort.finish() }
                },
            gestureBasedViewPort = data.pitchHistoryGestureBasedViewPort,
            tuningState = data.tuningState,
            targetNote = data.targetNote,
            toleranceInCents = toleranceInCentsAsState,
            plotWindowPadding = DpRect(
                bottom = tunerPlotStyle.margin, top = 0.dp, left = tunerPlotStyle.margin, right = noteWidthDp
            ),
            lineWidth = tunerPlotStyle.plotLineWidth,
            lineColor = tunerPlotStyle.plotLineColor,
            lineWidthInactive = tunerPlotStyle.plotLineWidthInactive,
            lineColorInactive = tunerPlotStyle.inactiveColor,
            pointSize = tunerPlotStyle.plotPointSize,
            pointSizeInactive = tunerPlotStyle.plotPointSizeInactive,
            colorInTune = tunerPlotStyle.positiveColor,
            colorOutOfTune = tunerPlotStyle.negativeColor,
            colorInactive = tunerPlotStyle.inactiveColor,
            targetNoteLineWidth = tunerPlotStyle.targetNoteLineWith,
            colorOnInTune = tunerPlotStyle.onPositiveColor,
            colorOnOutOfTune = tunerPlotStyle.onNegativeColor,
            colorOnInactive = tunerPlotStyle.onInactiveColor,
            toleranceLineColor = tunerPlotStyle.toleranceColor,
            toleranceLabelStyle = tunerPlotStyle.toleranceTickFontStyle,
            toleranceLabelColor = tunerPlotStyle.toleranceColor,
            tickLineWidth = tunerPlotStyle.tickLineWidth,
            tickLineColor = tunerPlotStyle.tickLineColor,
            tickLabelStyle = tunerPlotStyle.tickFontStyle,
            plotWindowOutline = if (data.pitchHistoryGestureBasedViewPort.isActive)
                tunerPlotStyle.plotWindowOutlineDuringGesture
            else
                tunerPlotStyle.plotWindowOutline
        )
    }
}

class TestInstrumentTunerData : InstrumentTunerData {
    override val musicalScale: StateFlow<MusicalScale>
            = MutableStateFlow(MusicalScaleFactory.create(TemperamentType.EDO12))

    override val notePrintOptions: StateFlow<NotePrintOptions>
            = MutableStateFlow(NotePrintOptions())
    override val toleranceInCents: StateFlow<Int>
            = MutableStateFlow(10)

    override var pitchHistoryState: PitchHistoryState = PitchHistoryState(
        5
    ).apply {
        addFrequency(442f)
        addFrequency(441f)
        addFrequency(450f)
        addFrequency(442f)
        addFrequency(440f)
        addFrequency(445f)
        addFrequency(438f)
        addFrequency(437f)
        addFrequency(435f)
    }

    override val pitchHistoryGestureBasedViewPort: GestureBasedViewPort
            = GestureBasedViewPort()
    override var tuningState: TuningState
            by mutableStateOf(TuningState.TooLow)
    override val currentFrequency: Float?
            by mutableStateOf(412f)
    override var targetNote: MusicalNote
            by mutableStateOf(musicalScale.value.referenceNote)
}

@Preview(widthDp = 300, heightDp = 600, showBackground = true)
@Composable
private fun ScientificTunerPreview() {
    TunerTheme {
        val data = remember { TestInstrumentTunerData() }
        InstrumentTuner(data = data)
    }
}
