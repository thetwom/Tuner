package de.moekadu.tuner.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
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
import de.moekadu.tuner.ui.instruments.InstrumentButton
import de.moekadu.tuner.ui.instruments.StringWithInfo
import de.moekadu.tuner.ui.instruments.Strings
import de.moekadu.tuner.ui.instruments.StringsScrollMode
import de.moekadu.tuner.ui.instruments.StringsSidebarPosition
import de.moekadu.tuner.ui.instruments.StringsState
import de.moekadu.tuner.ui.misc.QuickSettingsBar
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.rememberMaxNoteSize
import de.moekadu.tuner.ui.plot.GestureBasedViewPort
import de.moekadu.tuner.ui.plot.rememberTextLabelHeight
import de.moekadu.tuner.ui.theme.TunerTheme
import de.moekadu.tuner.ui.tuning.PitchHistory
import de.moekadu.tuner.ui.tuning.PitchHistoryState
import de.moekadu.tuner.views.StringView
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface InstrumentTunerData {
    val musicalScale: StateFlow<MusicalScale>
    val notePrintOptions: StateFlow<NotePrintOptions>
    val toleranceInCents: StateFlow<Int>

    val instrumentIconId: Int
    val instrumentResourceId: Int?
    val instrumentName: String?

    // Data specific to instruments
    val strings: ImmutableList<StringWithInfo>?
    val selectedNoteKey: Int?
    val stringsState: StringsState
    val onStringClicked: (key: Int, note: MusicalNote) -> (Unit)

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
fun InstrumentTuner(
    data: InstrumentTunerData,
    modifier: Modifier = Modifier,
    tunerPlotStyle: TunerPlotStyle = TunerPlotStyle.create()
) {
    BoxWithConstraints(modifier = modifier) {
        val stringsHeight = 0.4f * maxHeight
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            val musicalScaleAsState by data.musicalScale.collectAsStateWithLifecycle()
            val notePrintOptionsAsState by data.notePrintOptions.collectAsStateWithLifecycle()
            val toleranceInCentsAsState by data.toleranceInCents.collectAsStateWithLifecycle()
//        val tickHeightPx = rememberTextLabelHeight(tunerPlotStyle.tickFontStyle)
//        val tickHeightDp = with(LocalDensity.current) { tickHeightPx.toDp() }
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

            InstrumentButton(
                iconResourceId = data.instrumentIconId,
                name = data.instrumentResourceId?.let { stringResource(id = it) }
                    ?: data.instrumentName ?: "Unknown",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = tunerPlotStyle.margin,
                        top = tunerPlotStyle.margin - 4.dp,
                        end = noteWidthDp,
                        bottom = 0.dp
                    ),
                outline = tunerPlotStyle.plotWindowOutline,
                onClick = {} // TODO: do something
            )

            Strings(
                strings = data.strings,
                musicalScale = musicalScaleAsState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(0.dp, stringsHeight)
                    .padding(start = tunerPlotStyle.margin, top = tunerPlotStyle.margin - 4.dp),
                tuningState = data.tuningState,
                highlightedNoteKey = data.selectedNoteKey,
                highlightedNote = data.targetNote,
                notePrintOptions = notePrintOptionsAsState,
                defaultColor = tunerPlotStyle.stringColor,
                onDefaultColor = tunerPlotStyle.onStringColor,
                inTuneColor = tunerPlotStyle.positiveColor,
                onInTuneColor = tunerPlotStyle.onPositiveColor,
                outOfTuneColor = tunerPlotStyle.negativeColor,
                onOutOfTuneColor = tunerPlotStyle.onNegativeColor,
                fontSize = tunerPlotStyle.stringFontStyle.fontSize,
                sidebarPosition = StringsSidebarPosition.End,
                sidebarWidth = noteWidthDp,
                outline = if (data.stringsState.scrollMode == StringsScrollMode.Manual)
                    tunerPlotStyle.plotWindowOutlineDuringGesture
                else
                    tunerPlotStyle.plotWindowOutline,
                state = data.stringsState,
                onStringClicked = data.onStringClicked
            )
            Spacer(modifier = Modifier.height(tunerPlotStyle.margin))

            PitchHistory(
                state = data.pitchHistoryState,
                musicalScale = musicalScaleAsState,
                notePrintOptions = notePrintOptionsAsState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
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
                    bottom = tunerPlotStyle.margin,
                    top = 0.dp,
                    left = tunerPlotStyle.margin,
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

            val temperamentAndReferenceNote by data.temperamentAndReferenceNote.collectAsStateWithLifecycle()

            QuickSettingsBar(
                temperamentAndReferenceNote,
                notePrintOptions = notePrintOptionsAsState
            )
        }
    }
}

@Composable
fun InstrumentTunerLandscape(
    data: InstrumentTunerData,
    modifier: Modifier = Modifier,
    tunerPlotStyle: TunerPlotStyle = TunerPlotStyle.create()
) {
    Column(modifier = modifier.fillMaxSize()) {
        val notePrintOptionsAsState by data.notePrintOptions.collectAsStateWithLifecycle()
        Row(
            modifier = modifier.weight(1f)
        ) {
            val musicalScaleAsState by data.musicalScale.collectAsStateWithLifecycle()
            val toleranceInCentsAsState by data.toleranceInCents.collectAsStateWithLifecycle()
//        val tickHeightPx = rememberTextLabelHeight(tunerPlotStyle.tickFontStyle)
//        val tickHeightDp = with(LocalDensity.current) { tickHeightPx.toDp() }
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

            Column(modifier = Modifier.weight(0.5f)) {
                InstrumentButton(
                    iconResourceId = data.instrumentIconId,
                    name = data.instrumentResourceId?.let { stringResource(id = it) }
                        ?: data.instrumentName ?: "Unknown",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = noteWidthDp,
                            top = tunerPlotStyle.margin - 4.dp,
                            end = 0.dp,
                            bottom = 0.dp
                        ),
                    outline = tunerPlotStyle.plotWindowOutline,
                    onClick = {} // TODO: do something
                )

                Strings(
                    strings = data.strings,
                    musicalScale = musicalScaleAsState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = tunerPlotStyle.margin - 4.dp,
                            bottom = tunerPlotStyle.margin
                        ),
                    tuningState = data.tuningState,
                    highlightedNoteKey = data.selectedNoteKey,
                    highlightedNote = data.targetNote,
                    notePrintOptions = notePrintOptionsAsState,
                    defaultColor = tunerPlotStyle.stringColor,
                    onDefaultColor = tunerPlotStyle.onStringColor,
                    inTuneColor = tunerPlotStyle.positiveColor,
                    onInTuneColor = tunerPlotStyle.onPositiveColor,
                    outOfTuneColor = tunerPlotStyle.negativeColor,
                    onOutOfTuneColor = tunerPlotStyle.onNegativeColor,
                    fontSize = tunerPlotStyle.stringFontStyle.fontSize,
                    sidebarPosition = StringsSidebarPosition.Start,
                    sidebarWidth = noteWidthDp,
                    outline = if (data.stringsState.scrollMode == StringsScrollMode.Manual)
                        tunerPlotStyle.plotWindowOutlineDuringGesture
                    else
                        tunerPlotStyle.plotWindowOutline,
                    state = data.stringsState,
                    onStringClicked = data.onStringClicked
                )
            }
            Spacer(modifier = Modifier.width(tunerPlotStyle.margin))

            PitchHistory(
                state = data.pitchHistoryState,
                musicalScale = musicalScaleAsState,
                notePrintOptions = notePrintOptionsAsState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
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
                    bottom = tunerPlotStyle.margin,
                    top = tunerPlotStyle.margin,
                    left = 0.dp,
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
        val temperamentAndReferenceNote by data.temperamentAndReferenceNote.collectAsStateWithLifecycle()
        QuickSettingsBar(
            temperamentAndReferenceNote,
            notePrintOptions = notePrintOptionsAsState
        )
    }
}

class TestInstrumentTunerData : InstrumentTunerData {
    override val instrumentIconId by mutableStateOf(R.drawable.ic_piano)

    override val instrumentResourceId by mutableStateOf<Int?>(null)
    override val instrumentName by mutableStateOf<String?>("Test instrument")


    override val musicalScale: StateFlow<MusicalScale>
            = MutableStateFlow(MusicalScaleFactory.create(TemperamentType.EDO12))

    override val notePrintOptions: StateFlow<NotePrintOptions>
            = MutableStateFlow(NotePrintOptions())
    override val toleranceInCents: StateFlow<Int>
            = MutableStateFlow(10)

    private val noteNameScale = musicalScale.value.noteNameScale
    override var strings by mutableStateOf(
        listOf(
            noteNameScale.notes[0].copy(octave = 2),
            noteNameScale.notes[1].copy(octave = 3),
            noteNameScale.notes[4].copy(octave = 3),
            noteNameScale.notes[2].copy(octave = 3),
            noteNameScale.notes[4].copy(octave = 3),
            noteNameScale.notes[3].copy(octave = 4),
            noteNameScale.notes[5].copy(octave = 4),
            noteNameScale.notes[7].copy(octave = 4),
            noteNameScale.notes[4].copy(octave = 3),
            noteNameScale.notes[9].copy(octave = 4),
            noteNameScale.notes[11].copy(octave = 5),
            noteNameScale.notes[6].copy(octave = 5),
            noteNameScale.notes[4].copy(octave = 6),
            noteNameScale.notes[5].copy(octave = 7),
            noteNameScale.notes[3].copy(octave = 8),
        ).mapIndexed { index, note ->
            StringWithInfo(note, index, musicalScale.value.getNoteIndex(note))
        }.toPersistentList()
    )
    override var selectedNoteKey by mutableStateOf<Int?>(null)

    override val stringsState: StringsState = StringsState(0)

    override val onStringClicked: (key: Int, note: MusicalNote) -> Unit = { key, note ->
        selectedNoteKey = key
    }

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
        val data = remember { TestInstrumentTunerData() }
        InstrumentTuner(data = data)
    }
}

@Preview(widthDp = 600, heightDp = 300, showBackground = true)
@Composable
private fun ScientificTunerLandscapePreview() {
    TunerTheme {
        val data = remember { TestInstrumentTunerData() }
        InstrumentTunerLandscape(data = data)
    }
}
