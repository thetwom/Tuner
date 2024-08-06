package de.moekadu.tuner.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.moekadu.tuner.hilt.ApplicationScope
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentResources
import de.moekadu.tuner.instruments.instrumentChromatic
import de.moekadu.tuner.instruments.instrumentDatabase
import de.moekadu.tuner.notedetection.FrequencyDetectionCollectedResults
import de.moekadu.tuner.notedetection.FrequencyEvaluationResult
import de.moekadu.tuner.notedetection.TuningState
import de.moekadu.tuner.notedetection.checkTuning
import de.moekadu.tuner.preferences.PreferenceResources2
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.tuner.Tuner
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.plot.GestureBasedViewPort
import de.moekadu.tuner.ui.plot.LineCoordinates
import de.moekadu.tuner.ui.plot.VerticalLinesPositions
import de.moekadu.tuner.ui.screens.ScientificTunerData
import de.moekadu.tuner.ui.tuning.PitchHistoryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScientificTunerViewModel @Inject constructor (
    val pref: PreferenceResources2,
    @ApplicationScope val applicationScope: CoroutineScope
) : ViewModel(), ScientificTunerData {
    private val tuner = Tuner(
        pref,
        instrumentChromatic,
        viewModelScope,
        onResultAvailableListener = object : Tuner.OnResultAvailableListener {
            override fun onFrequencyDetected(result: FrequencyDetectionCollectedResults) {
                // Set frequency plot
                frequencyPlotData = frequencyPlotData.mutate(
                    result.frequencySpectrum.frequencies,
                    result.frequencySpectrum.plottingSpectrumNormalized
                )
                harmonicFrequencies = harmonicFrequencies.mutate(
                    result.harmonics.size, { result.harmonics[it].frequency }
                )

                // Set correlation plot
                correlationPlotData = correlationPlotData.mutate(
                    result.autoCorrelation.times,
                    result.autoCorrelation.plotValuesNormalized
                )
                correlationPlotDataYZeroPosition = result.autoCorrelation.plotValuesNormalizedZero

                currentFrequency = result.frequency
            }

            override fun onFrequencyEvaluated(result: FrequencyEvaluationResult) {
                if (result.smoothedFrequency > 0f) {
                    pitchHistoryState.addFrequency(result.smoothedFrequency)
                    result.target?.let { tuningTarget ->
                        targetNote = tuningTarget.note
                        tuningState = checkTuning(
                            result.smoothedFrequency,
                            tuningTarget.frequency,
                            toleranceInCents.value.toFloat()
                        )
                    }
                }
                //Log.v("Tuner", "ScientificTunerViewModel: dt = ${result.timeSinceThereIsNoFrequencyDetectionResult}")
                if (result.timeSinceThereIsNoFrequencyDetectionResult > DURATION_FOR_MARKING_NOTEDETECTION_AS_INACTIVE)
                    tuningState = TuningState.Unknown
            }

        }
    )

    override val musicalScale: StateFlow<MusicalScale> get() = pref.musicalScale
    override val notePrintOptions: StateFlow<NotePrintOptions> get() = pref.notePrintOptions
    override val sampleRate: Int get() = pref.sampleRate
    override val toleranceInCents: StateFlow<Int> get() = pref.toleranceInCents

    override var frequencyPlotData by mutableStateOf(LineCoordinates())
    override var harmonicFrequencies by mutableStateOf(VerticalLinesPositions())
    override val frequencyPlotGestureBasedViewPort: GestureBasedViewPort
            = GestureBasedViewPort()

    override var correlationPlotData by mutableStateOf(LineCoordinates())
    override var correlationPlotDataYZeroPosition by mutableStateOf(0f)
    override val correlationPlotGestureBasedViewPort: GestureBasedViewPort
            = GestureBasedViewPort()

    override var pitchHistoryState: PitchHistoryState = PitchHistoryState(
        computePitchHistorySize()
    )

    override val pitchHistoryGestureBasedViewPort: GestureBasedViewPort
            = GestureBasedViewPort()
    override var tuningState by mutableStateOf(TuningState.Unknown)
    override var currentFrequency: Float?
            by mutableStateOf(null)
    override var targetNote: MusicalNote
            by mutableStateOf(musicalScale.value.referenceNote)

    init {
        viewModelScope.launch {
            pref.pitchHistoryDuration.collect {
                pitchHistoryState.resize(computePitchHistorySize())
            }
        }
        viewModelScope.launch {
            pref.windowSize.collect {
                pitchHistoryState.resize(computePitchHistorySize())
            }
        }
        viewModelScope.launch {
            pref.overlap.collect {
                pitchHistoryState.resize(computePitchHistorySize())
            }
        }
    }

    override val waveWriterDuration: StateFlow<Int> get() = pref.waveWriterDurationInSeconds

    override fun startTuner() {
        tuner.connect()
    }
    override fun stopTuner() {
        tuner.disconnect()
    }

    override fun storeCurrentWaveWriterSnapshot() {
        applicationScope.launch {
            tuner.storeCurrentWaveWriterSnapshot()
        }
    }

    override fun writeStoredWaveWriterSnapshot(context: Context, uri: Uri, sampleRate: Int) {
        applicationScope.launch {
            tuner.writeStoredWaveWriterSnapshot(context, uri, sampleRate)
        }
    }

    /** Compute number of samples to be stored in pitch history. */
    private fun computePitchHistorySize() = PitchHistoryState.computePitchHistorySize(
        pref.pitchHistoryDuration.value,
        pref.sampleRate,
        pref.windowSize.value,
        pref.overlap.value
    )

    companion object {
        const val DURATION_FOR_MARKING_NOTEDETECTION_AS_INACTIVE = 0.5f // in seconds
    }

}