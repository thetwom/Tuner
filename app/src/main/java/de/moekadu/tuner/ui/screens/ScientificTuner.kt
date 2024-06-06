package de.moekadu.tuner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moekadu.tuner.R
import de.moekadu.tuner.notedetection.TuningState
import de.moekadu.tuner.preferences.TemperamentAndReferenceNoteValue
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.ui.misc.QuickSettingsBar
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.rememberMaxNoteSize
import de.moekadu.tuner.ui.plot.GestureBasedViewPort
import de.moekadu.tuner.ui.plot.VerticalLinesPositions
import de.moekadu.tuner.ui.plot.rememberTextLabelHeight
import de.moekadu.tuner.ui.theme.TunerTheme
import de.moekadu.tuner.ui.tuning.CorrelationPlot
import de.moekadu.tuner.ui.tuning.CorrelationPlotData
import de.moekadu.tuner.ui.tuning.FrequencyPlot
import de.moekadu.tuner.ui.tuning.FrequencyPlotData
import de.moekadu.tuner.ui.tuning.PitchHistory
import de.moekadu.tuner.ui.tuning.PitchHistoryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface ScientificTunerData {
    val musicalScale: StateFlow<MusicalScale>
    val notePrintOptions: StateFlow<NotePrintOptions>
    val toleranceInCents: StateFlow<Int>

    // Data specific to frequency plot
    val frequencyPlotData: FrequencyPlotData
    val harmonicFrequencies: VerticalLinesPositions?
    val frequencyPlotGestureBasedViewPort: GestureBasedViewPort

    // Data specific to correlation plot
    val correlationPlotData: CorrelationPlotData
    val correlationPlotGestureBasedViewPort: GestureBasedViewPort

    // Data specific to history plot
    val pitchHistoryState: PitchHistoryState

    val pitchHistoryGestureBasedViewPort: GestureBasedViewPort
    val tuningState: TuningState

    // Data shared over different plots
    val currentFrequency: Float?
    val targetNote: MusicalNote

    // Bottom bar
    val temperamentAndReferenceNote: StateFlow<TemperamentAndReferenceNoteValue>
}

@Composable
fun ScientificTuner(
    data: ScientificTunerData,
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
            fontSize = tunerPlotStyle.stringFontStyle.fontSize,
            octaveRange = musicalScaleAsState.getNote(
                musicalScaleAsState.noteIndexBegin
            ).octave .. musicalScaleAsState.getNote(
                        musicalScaleAsState.noteIndexEnd - 1
                    ).octave
        ).width + 8.dp
        val scope = rememberCoroutineScope()

        Column(modifier = Modifier.weight(0.23f)) {
            Spacer(modifier = Modifier.height(tunerPlotStyle.margin))
            Text(
                stringResource(id = R.string.spectrum),
                Modifier
                    .fillMaxWidth()
                    .padding(start = tunerPlotStyle.margin, end = noteWidthDp),
                textAlign = TextAlign.Center,
                style = tunerPlotStyle.plotHeadlineStyle
            )
            FrequencyPlot(
                frequencyPlotData = data.frequencyPlotData,
                targetNote = data.targetNote,
                musicalScale = musicalScaleAsState,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        scope.launch { data.frequencyPlotGestureBasedViewPort.finish() }
                    },
                gestureBasedViewPort = data.frequencyPlotGestureBasedViewPort,
                currentFrequency = data.currentFrequency,
                harmonicFrequencies =  data.harmonicFrequencies,
                plotWindowPadding = DpRect(
                    bottom = tickHeightDp, top = 0.dp, left = tunerPlotStyle.margin, right = noteWidthDp
                ),
                lineWidth = if (data.tuningState == TuningState.Unknown)
                    tunerPlotStyle.plotLineWidthInactive
                else
                    tunerPlotStyle.plotLineWidth,
                lineColor = if (data.tuningState == TuningState.Unknown)
                    tunerPlotStyle.inactiveColor
                else
                    tunerPlotStyle.plotLineColor,
                tickLineWidth = tunerPlotStyle.tickLineWidth,
                tickLineColor = tunerPlotStyle.tickLineColor,
                tickLabelStyle = tunerPlotStyle.tickFontStyle,
                frequencyMarkLineWidth = tunerPlotStyle.extraMarkLineWidth,
                frequencyMarkLineColor = tunerPlotStyle.extraMarkLineColor,
                frequencyMarkTextStyle = tunerPlotStyle.extraMarkTextStyle,
                harmonicLineWidth = tunerPlotStyle.extraMarkLineWidth,
                harmonicLineColor = tunerPlotStyle.extraMarkLineColor,
                plotWindowOutline = if (data.frequencyPlotGestureBasedViewPort.isActive)
                    tunerPlotStyle.plotWindowOutlineDuringGesture
                else
                    tunerPlotStyle.plotWindowOutline
            )
        }

        Column(modifier = Modifier.weight(0.23f)) {
            Text(
                stringResource(id = R.string.autocorrelation),
                Modifier
                    .fillMaxWidth()
                    .padding(start = tunerPlotStyle.margin, end = noteWidthDp),
                textAlign = TextAlign.Center,
                style = tunerPlotStyle.plotHeadlineStyle
            )
            CorrelationPlot(
                correlationPlotData = data.correlationPlotData,
                targetNote = data.targetNote,
                musicalScale = musicalScaleAsState,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        scope.launch { data.correlationPlotGestureBasedViewPort.finish() }
                    },
                currentFrequency = data.currentFrequency,
                gestureBasedViewPort = data.correlationPlotGestureBasedViewPort,
                plotWindowPadding = DpRect(
                    bottom = tickHeightDp, top = 0.dp, left = tunerPlotStyle.margin, right = noteWidthDp
                ),
                lineWidth = if (data.tuningState == TuningState.Unknown)
                    tunerPlotStyle.plotLineWidthInactive
                else
                    tunerPlotStyle.plotLineWidth,
                lineColor = if (data.tuningState == TuningState.Unknown)
                    tunerPlotStyle.inactiveColor
                else
                    tunerPlotStyle.plotLineColor,
                tickLineWidth = tunerPlotStyle.tickLineWidth,
                tickLineColor = tunerPlotStyle.tickLineColor,
                tickLabelStyle = tunerPlotStyle.tickFontStyle,
                frequencyMarkLineWidth = tunerPlotStyle.extraMarkLineWidth,
                frequencyMarkLineColor = tunerPlotStyle.extraMarkLineColor,
                frequencyMarkTextStyle = tunerPlotStyle.extraMarkTextStyle,
                plotWindowOutline = if (data.correlationPlotGestureBasedViewPort.isActive)
                    tunerPlotStyle.plotWindowOutlineDuringGesture
                else
                    tunerPlotStyle.plotWindowOutline
            )
        }

        Column(modifier = Modifier.weight(0.54f)) {
            Text(
                stringResource(id = R.string.pitch_history),
                Modifier
                    .fillMaxWidth()
                    .padding(start = tunerPlotStyle.margin, end = noteWidthDp),
                textAlign = TextAlign.Center,
                style = tunerPlotStyle.plotHeadlineStyle
            )
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
                tickLabelStyle = tunerPlotStyle.stringFontStyle,
                plotWindowOutline = if (data.pitchHistoryGestureBasedViewPort.isActive)
                    tunerPlotStyle.plotWindowOutlineDuringGesture
                else
                    tunerPlotStyle.plotWindowOutline
            )
        }

        val temperamentAndReferenceNote by data.temperamentAndReferenceNote.collectAsStateWithLifecycle()
        QuickSettingsBar(
            temperamentAndReferenceNote,
            notePrintOptions = notePrintOptionsAsState
        )
    }
}

@Composable
fun ScientificTunerLandscape(
    data: ScientificTunerData,
    modifier: Modifier = Modifier,
    tunerPlotStyle: TunerPlotStyle = TunerPlotStyle.create()
) {
    Column(
        modifier = modifier
    ) {
        val notePrintOptionsAsState by data.notePrintOptions.collectAsStateWithLifecycle()

        Row(
            modifier = Modifier.weight(1f)
        ) {
            val musicalScaleAsState by data.musicalScale.collectAsStateWithLifecycle()
            val toleranceInCentsAsState by data.toleranceInCents.collectAsStateWithLifecycle()
            val tickHeightPx = rememberTextLabelHeight(tunerPlotStyle.tickFontStyle)
            val tickHeightDp = with(LocalDensity.current) { tickHeightPx.toDp() }
            val noteWidthDp = rememberMaxNoteSize(
                noteNameScale = musicalScaleAsState.noteNameScale,
                notePrintOptions = notePrintOptionsAsState,
                fontSize = tunerPlotStyle.stringFontStyle.fontSize,
                octaveRange = musicalScaleAsState.getNote(
                    musicalScaleAsState.noteIndexBegin
                ).octave..musicalScaleAsState.getNote(
                    musicalScaleAsState.noteIndexEnd - 1
                ).octave
            ).width + 8.dp
            val scope = rememberCoroutineScope()

            Column(
                modifier = Modifier
                    .weight(0.48f)
                    .clip(shape = RectangleShape)
            ) {
                Column(modifier = Modifier.weight(0.5f)) {
                    Spacer(modifier = Modifier.height(tunerPlotStyle.margin))
                    Text(
                        stringResource(id = R.string.spectrum),
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = tunerPlotStyle.plotHeadlineStyle
                    )
                    FrequencyPlot(
                        frequencyPlotData = data.frequencyPlotData,
                        targetNote = data.targetNote,
                        musicalScale = musicalScaleAsState,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                scope.launch { data.frequencyPlotGestureBasedViewPort.finish() }
                            },
                        gestureBasedViewPort = data.frequencyPlotGestureBasedViewPort,
                        currentFrequency = data.currentFrequency,
                        harmonicFrequencies = data.harmonicFrequencies,
                        plotWindowPadding = DpRect(
                            bottom = tickHeightDp,
                            top = 0.dp,
                            left = tunerPlotStyle.margin,
                            right = tunerPlotStyle.margin / 2
                        ),
                        lineWidth = if (data.tuningState == TuningState.Unknown)
                            tunerPlotStyle.plotLineWidthInactive
                        else
                            tunerPlotStyle.plotLineWidth,
                        lineColor = if (data.tuningState == TuningState.Unknown)
                            tunerPlotStyle.inactiveColor
                        else
                            tunerPlotStyle.plotLineColor,
                        tickLineWidth = tunerPlotStyle.tickLineWidth,
                        tickLineColor = tunerPlotStyle.tickLineColor,
                        tickLabelStyle = tunerPlotStyle.tickFontStyle,
                        frequencyMarkLineWidth = tunerPlotStyle.extraMarkLineWidth,
                        frequencyMarkLineColor = tunerPlotStyle.extraMarkLineColor,
                        frequencyMarkTextStyle = tunerPlotStyle.extraMarkTextStyle,
                        harmonicLineWidth = tunerPlotStyle.extraMarkLineWidth,
                        harmonicLineColor = tunerPlotStyle.extraMarkLineColor,
                        plotWindowOutline = if (data.frequencyPlotGestureBasedViewPort.isActive)
                            tunerPlotStyle.plotWindowOutlineDuringGesture
                        else
                            tunerPlotStyle.plotWindowOutline
                    )
                }

                Column(modifier = Modifier.weight(0.52f)) {
                    Text(
                        stringResource(id = R.string.autocorrelation),
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = tunerPlotStyle.plotHeadlineStyle
                    )
                    CorrelationPlot(
                        correlationPlotData = data.correlationPlotData,
                        targetNote = data.targetNote,
                        musicalScale = musicalScaleAsState,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                scope.launch { data.correlationPlotGestureBasedViewPort.finish() }
                            },
                        currentFrequency = data.currentFrequency,
                        gestureBasedViewPort = data.correlationPlotGestureBasedViewPort,
                        plotWindowPadding = DpRect(
                            bottom = tickHeightDp + 4.dp,
                            top = 0.dp,
                            left = tunerPlotStyle.margin,
                            right = tunerPlotStyle.margin / 2
                        ),
                        lineWidth = if (data.tuningState == TuningState.Unknown)
                            tunerPlotStyle.plotLineWidthInactive
                        else
                            tunerPlotStyle.plotLineWidth,
                        lineColor = if (data.tuningState == TuningState.Unknown)
                            tunerPlotStyle.inactiveColor
                        else
                            tunerPlotStyle.plotLineColor,
                        tickLineWidth = tunerPlotStyle.tickLineWidth,
                        tickLineColor = tunerPlotStyle.tickLineColor,
                        tickLabelStyle = tunerPlotStyle.tickFontStyle,
                        frequencyMarkLineWidth = tunerPlotStyle.extraMarkLineWidth,
                        frequencyMarkLineColor = tunerPlotStyle.extraMarkLineColor,
                        frequencyMarkTextStyle = tunerPlotStyle.extraMarkTextStyle,
                        plotWindowOutline = if (data.correlationPlotGestureBasedViewPort.isActive)
                            tunerPlotStyle.plotWindowOutlineDuringGesture
                        else
                            tunerPlotStyle.plotWindowOutline
                    )
                }

            }

            Column(modifier = Modifier.weight(0.5f)) {
                Text(
                    stringResource(id = R.string.pitch_history),
                    Modifier
                        .fillMaxWidth()
                        .padding(top = tunerPlotStyle.margin, end = noteWidthDp),
                    textAlign = TextAlign.Center,
                    style = tunerPlotStyle.plotHeadlineStyle
                )
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
                        bottom = tickHeightDp + 4.dp,
                        top = 0.dp,
                        left = tunerPlotStyle.margin / 2,
                        right = noteWidthDp
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
                    tickLabelStyle = tunerPlotStyle.stringFontStyle,
                    plotWindowOutline = if (data.pitchHistoryGestureBasedViewPort.isActive)
                        tunerPlotStyle.plotWindowOutlineDuringGesture
                    else
                        tunerPlotStyle.plotWindowOutline
                )
            }
        }

        val temperamentAndReferenceNote by data.temperamentAndReferenceNote.collectAsStateWithLifecycle()
        QuickSettingsBar(
            temperamentAndReferenceNote,
            notePrintOptions = notePrintOptionsAsState
        )
    }
}

class TestScientificTunerData : ScientificTunerData {
    override val musicalScale: StateFlow<MusicalScale>
            = MutableStateFlow(MusicalScaleFactory.create(TemperamentType.EDO12))

    override val notePrintOptions: StateFlow<NotePrintOptions>
        = MutableStateFlow(NotePrintOptions())
    override val toleranceInCents: StateFlow<Int>
        = MutableStateFlow(10)
    override val frequencyPlotData: FrequencyPlotData = FrequencyPlotData(
        11,
        { floatArrayOf(0f, 200f, 400f, 600f, 800f, 1000f, 1200f, 1400f, 1600f, 1800f, 2000f)[it] },
        { floatArrayOf(1f, 2f, 4f, 4f, 1f, 2f, 0f, 6f, 1f, 0f, 0.4f)[it] }
    )
    override val harmonicFrequencies: VerticalLinesPositions?
        = VerticalLinesPositions(2, { floatArrayOf(405f, 432f)[it] })
    override val frequencyPlotGestureBasedViewPort: GestureBasedViewPort
            = GestureBasedViewPort()
    override val correlationPlotData: CorrelationPlotData = CorrelationPlotData(
        8,
        { floatArrayOf(0f, 0.001f, 0.002f, 0.003f, 0.004f, 0.005f, 0.006f, 0.007f)[it] },
        { floatArrayOf(10f, 9f, 6f, -2f, -2f, 0f, 0.5f, 0.4f)[it] }
    )

    override val correlationPlotGestureBasedViewPort: GestureBasedViewPort
            = GestureBasedViewPort()

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

    override val temperamentAndReferenceNote: StateFlow<TemperamentAndReferenceNoteValue>
            = MutableStateFlow(TemperamentAndReferenceNoteValue(
        temperamentType = TemperamentType.EDO12,
        rootNote = MusicalNote(BaseNote.A, NoteModifier.None, octave = 4),
        referenceNote = MusicalNote(BaseNote.A, NoteModifier.None, octave = 4),
        referenceFrequency = "440"
    ))
}

@Preview(widthDp = 300, heightDp = 600, showBackground = true)
@Composable
private fun ScientificTunerPreview() {
    TunerTheme {
        val data = remember { TestScientificTunerData() }
        ScientificTuner(data = data)
    }
}

@Preview(widthDp = 600, heightDp = 300, showBackground = true)
@Composable
private fun ScientificTunerLandscapePreview() {
    TunerTheme {
        val data = remember { TestScientificTunerData() }
        ScientificTunerLandscape(data = data)
    }
}