package de.moekadu.tuner.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.instruments.InstrumentResources
import de.moekadu.tuner.notedetection.TuningState
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.ui.plot.GestureBasedViewPort
import de.moekadu.tuner.ui.plot.VerticalLinesPositions
import de.moekadu.tuner.ui.tuning.CorrelationPlotData
import de.moekadu.tuner.ui.tuning.FrequencyPlotData
import de.moekadu.tuner.ui.tuning.PitchHistoryState
import javax.inject.Inject

@HiltViewModel
class ScientificTunerViewModel @Inject constructor (
    val pref: PreferenceResources,
   // private val instrumentResources: InstrumentResources,
) : ViewModel() {
    // Data specific to frequency plot
    var frequencyPlotData by mutableStateOf(FrequencyPlotData(0, { 0f }, { 0f }))
    var harmonicFrequencies by mutableStateOf<VerticalLinesPositions?>(null)
    val frequencyPlotGestureBasedViewPort = GestureBasedViewPort()

    // Data specific to correlation plot
    var correlationPlotData by mutableStateOf(CorrelationPlotData(0, { 0f }, { 0f }))
    val correlationPlotGestureBasedViewPort = GestureBasedViewPort()

    // Data specific to history plot
    var pitchHistoryState = PitchHistoryState(PitchHistoryState.computePitchHistorySize(
        pref.pitchHistoryDuration.value, pref.sampleRate, pref.windowSize.value, pref.overlap.value
    ))
    val pitchHistoryGestureBasedViewPort = GestureBasedViewPort()
    var tuningState by mutableStateOf(TuningState.Unknown)

    // Data shared over different plots
    var currentFrequency by mutableStateOf<Float?>(null)
    var targetNote by mutableStateOf(pref.musicalScale.value.referenceNote)




}