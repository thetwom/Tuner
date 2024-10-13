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

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moekadu.tuner.R
import de.moekadu.tuner.misc.getFilenameFromUri
import de.moekadu.tuner.notedetection.TuningState
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.ui.misc.TunerScaffold
import de.moekadu.tuner.ui.misc.rememberTunerAudioPermission
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.rememberMaxNoteSize
import de.moekadu.tuner.ui.plot.GestureBasedViewPort
import de.moekadu.tuner.ui.plot.LineCoordinates
import de.moekadu.tuner.ui.plot.VerticalLinesPositions
import de.moekadu.tuner.ui.plot.rememberTextLabelHeight
import de.moekadu.tuner.ui.theme.TunerTheme
import de.moekadu.tuner.ui.tuning.CorrelationPlot
import de.moekadu.tuner.ui.tuning.FrequencyPlot
import de.moekadu.tuner.ui.tuning.PitchHistory
import de.moekadu.tuner.ui.tuning.PitchHistoryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface ScientificTunerData {
    val musicalScale: StateFlow<MusicalScale>
    val notePrintOptions: StateFlow<NotePrintOptions>
    val toleranceInCents: StateFlow<Int>
    val sampleRate: Int

    // Data specific to frequency plot
    val frequencyPlotData: LineCoordinates
    val harmonicFrequencies: VerticalLinesPositions?
    val frequencyPlotGestureBasedViewPort: GestureBasedViewPort

    // Data specific to correlation plot
    val correlationPlotData: LineCoordinates
    val correlationPlotDataYZeroPosition: Float
    val correlationPlotGestureBasedViewPort: GestureBasedViewPort

    // Data specific to history plot
    val pitchHistoryState: PitchHistoryState

    val pitchHistoryGestureBasedViewPort: GestureBasedViewPort
    val tuningState: TuningState

    // Data shared over different plots
    val currentFrequency: Float?
    val targetNote: MusicalNote

    // Others
    val waveWriterDuration: StateFlow<Int>

    fun startTuner()
    fun stopTuner()

    fun storeCurrentWaveWriterSnapshot()
    fun writeStoredWaveWriterSnapshot(context: Context, uri: Uri, sampleRate: Int)
}

@Composable
fun ScientificTuner(
    data: ScientificTunerData,
    canNavigateUp: Boolean,
    onNavigateUpClicked: () -> Unit,
    onPreferenceButtonClicked: () -> Unit,
    onSharpFlatClicked: () -> Unit,
    onReferenceNoteClicked: () -> Unit,
    onTemperamentClicked: () -> Unit,
    modifier: Modifier = Modifier,
    tunerPlotStyle: TunerPlotStyle = TunerPlotStyle.create()
) {
    val musicalScale by data.musicalScale.collectAsStateWithLifecycle()
    val notePrintOptions by data.notePrintOptions.collectAsStateWithLifecycle()
    val waveWriterDuration by data.waveWriterDuration.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val permissionGranted = rememberTunerAudioPermission(snackbarHostState)
    val context = LocalContext.current

    val writeWaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/wav")
    ) { uri ->
        if (uri != null) {
            data.writeStoredWaveWriterSnapshot(context, uri, data.sampleRate)
            val filename = getFilenameFromUri(context, uri)
            Toast.makeText(
                context, context.getString(R.string.writing_wave_file, filename), Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context, context.getString(R.string.failed_writing_wave_file), Toast.LENGTH_LONG
            ).show()
        }
    }

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
        floatingActionButton = {
            if (waveWriterDuration > 0) {
                FloatingActionButton(
                    onClick = {
                        data.storeCurrentWaveWriterSnapshot()
                        writeWaveLauncher.launch("tuner-export.wav")
                    }
                ) {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.ic_mic),
                        contentDescription = "record"
                    )
                }
            }
        },
        floatingActionBarPosition = FabPosition.Start,
        modifier = modifier
    ) { paddingValues ->
        val configuration = LocalConfiguration.current
        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                ScientificTunerLandscape(
                    data = data,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    tunerPlotStyle = tunerPlotStyle
                )
            }

            else -> {
                ScientificTunerPortrait(
                    data = data,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    tunerPlotStyle = tunerPlotStyle
                )
            }
        }
    }
}

@Composable
fun ScientificTunerPortrait(
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
            notes = musicalScaleAsState.noteNameScale.notes,
            notePrintOptions = notePrintOptionsAsState,
            fontSize = tunerPlotStyle.stringFontStyle.fontSize,
            octaveRange = musicalScaleAsState.getNote(
                musicalScaleAsState.noteIndexBegin
            ).octave .. musicalScaleAsState.getNote(
                        musicalScaleAsState.noteIndexEnd - 1
                    ).octave
        ).width + 8.dp + 4.dp
        val scope = rememberCoroutineScope()

        Column(modifier = Modifier.weight(0.225f)) {
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
                lineWidth = tunerPlotStyle.plotLineWidth,
                lineColor = tunerPlotStyle.plotLineColor,
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

        Column(modifier = Modifier.weight(0.225f)) {
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
                correlationPlotDataYZeroPosition = data.correlationPlotDataYZeroPosition,
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
                lineWidth = tunerPlotStyle.plotLineWidth,
                lineColor = tunerPlotStyle.plotLineColor,
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

        Column(modifier = Modifier.weight(0.55f)) {
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
                //centDeviationColor = tunerPlotStyle.tickLabelColor,
                centDeviationStyle = tunerPlotStyle.toleranceTickFontStyle,
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
fun ScientificTunerLandscape(
    data: ScientificTunerData,
    modifier: Modifier = Modifier,
    tunerPlotStyle: TunerPlotStyle = TunerPlotStyle.create()
) {
    Row(
        modifier = modifier.fillMaxSize()
    ) {
        val notePrintOptionsAsState by data.notePrintOptions.collectAsStateWithLifecycle()
        val musicalScaleAsState by data.musicalScale.collectAsStateWithLifecycle()
        val toleranceInCentsAsState by data.toleranceInCents.collectAsStateWithLifecycle()
        val tickHeightPx = rememberTextLabelHeight(tunerPlotStyle.tickFontStyle)
        val tickHeightDp = with(LocalDensity.current) { tickHeightPx.toDp() }
        val noteWidthDp = rememberMaxNoteSize(
            notes = musicalScaleAsState.noteNameScale.notes,
            notePrintOptions = notePrintOptionsAsState,
            fontSize = tunerPlotStyle.stringFontStyle.fontSize,
            octaveRange = musicalScaleAsState.getNote(
                musicalScaleAsState.noteIndexBegin
            ).octave..musicalScaleAsState.getNote(
                musicalScaleAsState.noteIndexEnd - 1
            ).octave
        ).width + 8.dp + 4.dp
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
                    lineWidth = tunerPlotStyle.plotLineWidth,
                    lineColor = tunerPlotStyle.plotLineColor,
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
                    correlationPlotDataYZeroPosition = data.correlationPlotDataYZeroPosition,
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
                    lineWidth = tunerPlotStyle.plotLineWidth,
                    lineColor = tunerPlotStyle.plotLineColor,
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
                //centDeviationColor = tunerPlotStyle.tickLabelColor,
                centDeviationStyle = tunerPlotStyle.toleranceTickFontStyle,
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

class TestScientificTunerData : ScientificTunerData {
    override val musicalScale: StateFlow<MusicalScale>
            = MutableStateFlow(MusicalScaleFactory.create(TemperamentType.EDO12))

    override val notePrintOptions: StateFlow<NotePrintOptions>
        = MutableStateFlow(NotePrintOptions())

    override val sampleRate = 44100

    override val toleranceInCents: StateFlow<Int>
        = MutableStateFlow(10)
    override val frequencyPlotData =LineCoordinates.create(
        floatArrayOf(0f, 200f, 400f, 600f, 800f, 1000f, 1200f, 1400f, 1600f, 1800f, 2000f),
        floatArrayOf(0.1f, 0.2f, 1f, 0.7f, 0.1f, 0.2f, 0f, 0.6f, 0.1f, 0.0f, 0.04f)
    )
    override val harmonicFrequencies: VerticalLinesPositions
        = VerticalLinesPositions.create(2) { floatArrayOf(405f, 432f)[it] }
    override val frequencyPlotGestureBasedViewPort: GestureBasedViewPort
            = GestureBasedViewPort()

    override val correlationPlotData = LineCoordinates.create(
        floatArrayOf(0f, 0.001f, 0.002f, 0.003f, 0.004f, 0.005f, 0.006f, 0.007f),
        floatArrayOf(1f, 0.9f, 0.6f, 0.2f, 0.1f,0.6f, 0.5f, 0.2f)
    )
    override val correlationPlotDataYZeroPosition = 0.3f


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
            by mutableFloatStateOf(412f)
    override var targetNote: MusicalNote
            by mutableStateOf(musicalScale.value.referenceNote)

    override val waveWriterDuration = MutableStateFlow(1)

    override fun startTuner() {}
    override fun stopTuner() {}

    override fun storeCurrentWaveWriterSnapshot() {}
    override fun writeStoredWaveWriterSnapshot(context: Context, uri: Uri, sampleRate: Int) {}
}

@Preview(widthDp = 300, heightDp = 600, showBackground = true)
@Composable
private fun ScientificTunerPreview() {
    TunerTheme {
        val data = remember { TestScientificTunerData() }
        ScientificTunerPortrait(data = data)
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