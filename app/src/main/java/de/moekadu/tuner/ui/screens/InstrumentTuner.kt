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
package de.moekadu.tuner.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentIcon
import de.moekadu.tuner.notedetection.SortedAndDistinctInstrumentStrings
import de.moekadu.tuner.notedetection.TuningState
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.musicalscale.MusicalScale2
import de.moekadu.tuner.ui.instruments.InstrumentButton
import de.moekadu.tuner.ui.instruments.StringWithInfo
import de.moekadu.tuner.ui.instruments.Strings
import de.moekadu.tuner.ui.instruments.StringsScrollMode
import de.moekadu.tuner.ui.instruments.StringsSidebarPosition
import de.moekadu.tuner.ui.instruments.StringsState
import de.moekadu.tuner.ui.misc.TunerScaffold
import de.moekadu.tuner.ui.misc.rememberTunerAudioPermission
import de.moekadu.tuner.ui.notes.NoteLockedButton
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.rememberMaxNoteSize
import de.moekadu.tuner.ui.plot.GestureBasedViewPort
import de.moekadu.tuner.ui.theme.TunerTheme
import de.moekadu.tuner.ui.tuning.PitchHistory
import de.moekadu.tuner.ui.tuning.PitchHistoryState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface InstrumentTunerData {
    val musicalScale: StateFlow<MusicalScale2>
    val notePrintOptions: StateFlow<NotePrintOptions>
    val toleranceInCents: StateFlow<Int>

    val instrument: StateFlow<Instrument>

    // Data specific to instruments
    val strings: ImmutableList<StringWithInfo>?
    val selectedNoteKey: Int?
    val stringsState: StringsState
    fun onStringClicked(key: Int, note: MusicalNote)
    //val onStringClicked: (key: Int, note: MusicalNote) -> (Unit)

    // Data specific to history plot
    val pitchHistoryState: PitchHistoryState

    val pitchHistoryGestureBasedViewPort: GestureBasedViewPort
    val tuningState: TuningState

    // Data shared over different plots
    val targetNote: MusicalNote

    /** Same as target note but does not change, when note is unselected. */
    val targetNoteForLockButton: MusicalNote

    // others
    fun onClearFixedTargetClicked()

    fun startTuner()
    fun stopTuner()
}

private fun checkInstrumentCompatibility(
    musicalScale: MusicalScale2,
    instrument: Instrument): Boolean {
    if (instrument.isChromatic)
        return true
    val sortedStrings = SortedAndDistinctInstrumentStrings(instrument, musicalScale)
    return when {
        sortedStrings.sortedAndDistinctNoteIndices.isEmpty() -> true
        sortedStrings.sortedAndDistinctNoteIndices.last() == Int.MAX_VALUE -> false
        else -> true
    }
}

@Composable
fun InstrumentTuner(
    data: InstrumentTunerData,
    canNavigateUp: Boolean,
    onNavigateUpClicked: () -> Unit,
    onPreferenceButtonClicked: () -> Unit,
    onSharpFlatClicked: () -> Unit,
    onReferenceNoteClicked: () -> Unit,
    onTemperamentClicked: () -> Unit,
    onInstrumentButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    tunerPlotStyle: TunerPlotStyle = TunerPlotStyle.create()
) {
    val instrumentAsState by data.instrument.collectAsStateWithLifecycle()
    val isEmptyInstrument = !instrumentAsState.isChromatic && instrumentAsState.strings.isEmpty()

    val musicalScale by data.musicalScale.collectAsStateWithLifecycle()
    val notePrintOptions by data.notePrintOptions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val permissionGranted = rememberTunerAudioPermission(snackbarHostState)

    LifecycleResumeEffect(permissionGranted) {
        if (permissionGranted)
            data.startTuner()
        onPauseOrDispose { data.stopTuner() }
    }
    TunerScaffold(
        canNavigateUp = canNavigateUp,
        onNavigateUpClicked = onNavigateUpClicked,
        showPreferenceButton = true,
        onPreferenceButtonClicked = onPreferenceButtonClicked,
        showBottomBar = true,
        onSharpFlatClicked = onSharpFlatClicked,
        onReferenceNoteClicked = onReferenceNoteClicked,
        onTemperamentClicked = onTemperamentClicked,
        musicalScale = musicalScale,
        notePrintOptions = notePrintOptions,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = modifier
    ) { paddingValues ->
        val configuration = LocalConfiguration.current
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && !isEmptyInstrument) {
            InstrumentTunerLandscape(
                data = data,
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                onInstrumentButtonClicked = onInstrumentButtonClicked,
                tunerPlotStyle = tunerPlotStyle
            )
        } else {
            InstrumentTunerPortrait(
                data = data,
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                onInstrumentButtonClicked = onInstrumentButtonClicked,
                tunerPlotStyle = tunerPlotStyle
            )
        }
    }
}

@Composable
fun InstrumentTunerPortrait(
    data: InstrumentTunerData,
    modifier: Modifier = Modifier,
    onInstrumentButtonClicked: () -> Unit = {},
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
            val instrumentAsState by data.instrument.collectAsStateWithLifecycle()
            val noteNames by remember { derivedStateOf {
                musicalScaleAsState.temperament.noteNames(musicalScaleAsState.rootNote)
            }}
//        val tickHeightPx = rememberTextLabelHeight(tunerPlotStyle.tickFontStyle)
//        val tickHeightDp = with(LocalDensity.current) { tickHeightPx.toDp() }
            val noteWidthDp = rememberMaxNoteSize(
                notes = noteNames.notes,
                notePrintOptions = notePrintOptionsAsState,
                fontSize = tunerPlotStyle.stringFontStyle.fontSize,
                octaveRange = musicalScaleAsState.getNote(
                    musicalScaleAsState.noteIndexBegin
                ).octave..musicalScaleAsState.getNote(
                    musicalScaleAsState.noteIndexEnd - 1
                ).octave
            ).width + 8.dp + 4.dp
            val scope = rememberCoroutineScope()
            val scaleInvalid= remember(musicalScaleAsState, instrumentAsState) {
                !checkInstrumentCompatibility(musicalScaleAsState, instrumentAsState)
            }
            //Log.v("Tuner", "InstrumentTuner: instrument as state: $instrumentAsState")
            InstrumentButton(
                iconResourceId = instrumentAsState.icon.resourceId,
                name = instrumentAsState.getNameString(LocalContext.current),
//                name = data.instrumentResourceId?.let { stringResource(id = it) }
//                    ?: data.instrumentName ?: "Unknown",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = tunerPlotStyle.margin,
                        top = tunerPlotStyle.margin,
                        end = noteWidthDp,
                        bottom = 0.dp
                    ),
                outline = tunerPlotStyle.plotWindowOutline,
                errorMessage = if (scaleInvalid)
                    stringResource(id = R.string.incorrect_temperament)
                else
                    null,
                onClick = onInstrumentButtonClicked
            )

            if (instrumentAsState.strings.isNotEmpty() || instrumentAsState.isChromatic) {
                Strings(
                    strings = if (instrumentAsState.isChromatic) null else data.strings,
                    musicalScale = musicalScaleAsState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(0.dp, stringsHeight)
                        .padding(start = tunerPlotStyle.margin, top = tunerPlotStyle.margin),
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
                    unknownTuningColor = tunerPlotStyle.inactiveStringColor,
                    onUnknownTuningColor = tunerPlotStyle.onInactiveStringColor,
                    fontSize = tunerPlotStyle.stringFontStyle.fontSize,
                    sidebarPosition = StringsSidebarPosition.End,
                    sidebarWidth = noteWidthDp,
                    outline = if (data.stringsState.scrollMode == StringsScrollMode.Manual)
                        tunerPlotStyle.plotWindowOutlineDuringGesture
                    else
                        tunerPlotStyle.plotWindowOutline,
                    state = data.stringsState,
                    onStringClicked = { key, note -> data.onStringClicked(key, note) }
                )
            }
            AnimatedVisibility(data.selectedNoteKey != null) {
                NoteLockedButton(
                    note = data.targetNoteForLockButton,
                    modifier = Modifier
                        .padding(
                            start = tunerPlotStyle.margin,
                            end = noteWidthDp,
                            top = tunerPlotStyle.margin - 4.dp,
                            bottom = tunerPlotStyle.margin - 4.dp
                        )
                        .fillMaxWidth(),
                    notePrintOptions = notePrintOptionsAsState,
                    onClick = { data.onClearFixedTargetClicked() }
                )
            }
            AnimatedVisibility(data.selectedNoteKey == null) {
                Spacer(Modifier.height(tunerPlotStyle.margin))
            }

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
                centDeviationStyle = tunerPlotStyle.toleranceTickFontStyle,
                //centDeviationColor = ,
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
}

@Composable
fun InstrumentTunerLandscape(
    data: InstrumentTunerData,
    modifier: Modifier = Modifier,
    onInstrumentButtonClicked: () -> Unit = {},
    tunerPlotStyle: TunerPlotStyle = TunerPlotStyle.create()
) {
    Row(
        modifier = modifier.fillMaxSize()
    ) {
        val notePrintOptionsAsState by data.notePrintOptions.collectAsStateWithLifecycle()
        val musicalScaleAsState by data.musicalScale.collectAsStateWithLifecycle()
        val toleranceInCentsAsState by data.toleranceInCents.collectAsStateWithLifecycle()
        val instrumentAsState by data.instrument.collectAsStateWithLifecycle()
        val noteNames by remember { derivedStateOf {
            musicalScaleAsState.temperament.noteNames(musicalScaleAsState.rootNote)
        }}
//        val tickHeightPx = rememberTextLabelHeight(tunerPlotStyle.tickFontStyle)
//        val tickHeightDp = with(LocalDensity.current) { tickHeightPx.toDp() }
        val noteWidthDp = rememberMaxNoteSize(
            notes = noteNames.notes,
            notePrintOptions = notePrintOptionsAsState,
            fontSize = tunerPlotStyle.stringFontStyle.fontSize,
            octaveRange = musicalScaleAsState.getNote(
                musicalScaleAsState.noteIndexBegin
            ).octave..musicalScaleAsState.getNote(
                musicalScaleAsState.noteIndexEnd - 1
            ).octave
        ).width + 8.dp + 4.dp
        val scope = rememberCoroutineScope()
        val scaleInvalid= remember(musicalScaleAsState, instrumentAsState) {
            !checkInstrumentCompatibility(musicalScaleAsState, instrumentAsState)
        }

        Column(modifier = Modifier.weight(0.5f)) {
            InstrumentButton(
                iconResourceId = instrumentAsState.icon.resourceId,
                name = instrumentAsState.getNameString(LocalContext.current),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = noteWidthDp,
                        top = tunerPlotStyle.margin,
                        end = 0.dp,
                        bottom = 0.dp
                    ),
                outline = tunerPlotStyle.plotWindowOutline,
                errorMessage = if (scaleInvalid)
                    stringResource(id = R.string.incorrect_temperament)
                else
                    null,
                onClick = onInstrumentButtonClicked

            )

            Strings(
                strings = if (instrumentAsState.isChromatic) null else data.strings,
                musicalScale = musicalScaleAsState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = tunerPlotStyle.margin),
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
                unknownTuningColor = tunerPlotStyle.inactiveStringColor,
                onUnknownTuningColor = tunerPlotStyle.onInactiveStringColor,
                fontSize = tunerPlotStyle.stringFontStyle.fontSize,
                sidebarPosition = StringsSidebarPosition.Start,
                sidebarWidth = noteWidthDp,
                outline = if (data.stringsState.scrollMode == StringsScrollMode.Manual)
                    tunerPlotStyle.plotWindowOutlineDuringGesture
                else
                    tunerPlotStyle.plotWindowOutline,
                state = data.stringsState,
                onStringClicked = { key, note -> data.onStringClicked(key, note) }
            )

            AnimatedVisibility(data.selectedNoteKey != null) {
                NoteLockedButton(
                    note = data.targetNoteForLockButton,
                    modifier = Modifier
                        .padding(
                            start = noteWidthDp,
                            top = tunerPlotStyle.margin - 4.dp,
                            bottom = tunerPlotStyle.margin - 4.dp
                        )
                        .fillMaxWidth(),
                    notePrintOptions = notePrintOptionsAsState,
                    onClick = { data.onClearFixedTargetClicked() }
                )
            }
            AnimatedVisibility(data.selectedNoteKey == null) {
                Spacer(Modifier.height(tunerPlotStyle.margin))
            }
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
            centDeviationStyle = tunerPlotStyle.toleranceTickFontStyle,
            //centDeviationColor = ,
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

class TestInstrumentTunerData : InstrumentTunerData {
    override val musicalScale: StateFlow<MusicalScale2> = MutableStateFlow(
        MusicalScale2.createTestEdo12()
    )

    override val notePrintOptions: StateFlow<NotePrintOptions>
            = MutableStateFlow(NotePrintOptions())
    override val toleranceInCents: StateFlow<Int>
            = MutableStateFlow(10)

    private val noteNameScale = musicalScale.value.temperament.noteNames(
        musicalScale.value.rootNote
    )

    override val instrument: StateFlow<Instrument> = MutableStateFlow(
        Instrument(
            name = "Test instrument",
            nameResource = null,
            strings = arrayOf(
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
            ),
            icon = InstrumentIcon.piano,
            stableId = 1L,
            isChromatic = false
        )
    )

    override var strings by mutableStateOf(
        instrument.value.strings.mapIndexed { index, note ->
            StringWithInfo(note, index) //, musicalScale.value.getNoteIndex(note))
        }.toPersistentList()
    )
    override var selectedNoteKey by mutableStateOf<Int?>(null)

    override val stringsState: StringsState = StringsState(0)

    override fun onStringClicked(key: Int, note: MusicalNote) {
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
    override var targetNote: MusicalNote
            by mutableStateOf(musicalScale.value.referenceNote)
    override val targetNoteForLockButton get() = targetNote

    override fun onClearFixedTargetClicked() {
        selectedNoteKey = null
    }

    override fun startTuner() {}
    override fun stopTuner() {}
}

@Preview(widthDp = 300, heightDp = 600, showBackground = true)
@Composable
private fun InstrumentTunerPreview() {
    TunerTheme {
        val data = remember { TestInstrumentTunerData() }
        InstrumentTunerPortrait(data = data)
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
