package de.moekadu.tuner.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.instruments.InstrumentResources
import de.moekadu.tuner.notedetection.FrequencyDetectionCollectedResults
import de.moekadu.tuner.notedetection.FrequencyEvaluationResult
import de.moekadu.tuner.notedetection.TuningState
import de.moekadu.tuner.notedetection.checkTuning
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.preferences.TemperamentAndReferenceNoteValue
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.tuner.Tuner
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.plot.GestureBasedViewPort
import de.moekadu.tuner.ui.plot.VerticalLinesPositions
import de.moekadu.tuner.ui.screens.ScientificTunerData
import de.moekadu.tuner.ui.tuning.CorrelationPlotData
import de.moekadu.tuner.ui.tuning.FrequencyPlotData
import de.moekadu.tuner.ui.tuning.PitchHistoryState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.roundToInt

private class FrequencyPlotBackingData(
    val size: Int,
) {
    var df = 1f
    val y = FloatArray(size)
    val harmonics = ArrayList<Float>()
}

private class CorrelationPlotBackingData(
    val size: Int,
) {
    var dt = 1f
    val y = FloatArray(size)
}

@HiltViewModel
class ScientificTunerViewModel @Inject constructor (
    val pref: PreferenceResources,
    val instruments: InstrumentResources
) : ViewModel(), ScientificTunerData {
    private val tuner = Tuner(
        pref,
        instruments,
        viewModelScope,
        onResultAvailableListener = object : Tuner.OnResultAvailableListener {
            override fun onFrequencyDetected(result: FrequencyDetectionCollectedResults) {
                // Set frequency plot
                if (frequencyPlotBackingData.size != result.frequencySpectrum.size)
                    frequencyPlotBackingData = FrequencyPlotBackingData(
                        result.frequencySpectrum.size
                    )
                frequencyPlotBackingData.df = result.frequencySpectrum.df
                result.frequencySpectrum.amplitudeSpectrumSquared.forEachIndexed { index, value ->
                        frequencyPlotBackingData.y[index] = value } // for log, we would have first to normalize the spectrum log2(max(value,1f-1)) }
                frequencyPlotData = FrequencyPlotData(
                    frequencyPlotBackingData.size,
                    { it * frequencyPlotBackingData.df },
                    { frequencyPlotBackingData.y[it] },
                )
                frequencyPlotBackingData.harmonics.clear()
                for (i in 0 until result.harmonics.size)
                    frequencyPlotBackingData.harmonics.add(result.harmonics[i].frequency)
                harmonicFrequencies = VerticalLinesPositions(
                    frequencyPlotBackingData.harmonics.size,
                    { frequencyPlotBackingData.harmonics[it] }
                )

                // Set correlation plot
                if (correlationPlotBackingData.size != result.autoCorrelation.size)
                    correlationPlotBackingData = CorrelationPlotBackingData(
                        result.autoCorrelation.size
                    )
                correlationPlotBackingData.dt = result.autoCorrelation.dt
                result.autoCorrelation.values.forEachIndexed { index, value ->
                    correlationPlotBackingData.y[index] = value
                }
                correlationPlotData = CorrelationPlotData(
                    correlationPlotBackingData.size,
                    { it * correlationPlotBackingData.dt },
                    { correlationPlotBackingData.y[it] },
                )

                currentFrequency = result.frequency
            }

            override fun onFrequencyEvaluated(result: FrequencyEvaluationResult) {
                if (result.smoothedFrequency > 0f)
                    pitchHistoryState.addFrequency(result.smoothedFrequency)
                result.target?.let{ tuningTarget ->
                    targetNote = tuningTarget.note
                    tuningState = checkTuning(
                        result.smoothedFrequency,
                        tuningTarget.frequency,
                        toleranceInCents.value.toFloat()
                    )
                }
                // TODO: inactive setting
                // TODO: in pitch history, we must still show the cents we are off
                //TODO("Not yet implemented")
            }

        }
    )

    private var frequencyPlotBackingData = FrequencyPlotBackingData(0)
    private var correlationPlotBackingData = CorrelationPlotBackingData(0)

    override val musicalScale: StateFlow<MusicalScale> get() = pref.musicalScale
    override val notePrintOptions: StateFlow<NotePrintOptions> get() = pref.notePrintOptions
    override val toleranceInCents: StateFlow<Int> get() = pref.toleranceInCents

    override var frequencyPlotData by mutableStateOf(
        FrequencyPlotData(0, { 0f }, { 0f })
    )
    override var harmonicFrequencies by mutableStateOf(VerticalLinesPositions(0) { 0f })
    override val frequencyPlotGestureBasedViewPort: GestureBasedViewPort
            = GestureBasedViewPort()

    override var correlationPlotData by mutableStateOf(
        CorrelationPlotData(0, {0f}, {0f})
    )

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

    override val temperamentAndReferenceNote: StateFlow<TemperamentAndReferenceNoteValue>
        get() = pref.temperamentAndReferenceNote

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
    fun startTuner() {
        tuner.connect()
    }
    fun stopTuner() {
        tuner.disconnect()
    }

    /** Compute number of samples to be stored in pitch history.
     * @param duration Duration of pitch history in seconds.
     * @param sampleRate Sample rate of audio signal in Hertz
     * @param windowSize Number of samples for one chunk of data which is used for evaluation.
     * @param overlap Overlap between to succeeding data chunks, where 0 is no overlap and 1 is
     *   100% overlap (1.0 is of course not allowed).
     * @return Number of samples, which must be stored in the pitch history, so that the we match
     *   the given duration.
     * */
    private fun computePitchHistorySize(
        duration: Float = pref.pitchHistoryDuration.value,
        sampleRate: Int = pref.sampleRate,
        windowSize: Int = pref.windowSize.value,
        overlap: Float = pref.overlap.value
    ) = (duration / (windowSize.toFloat() / sampleRate.toFloat() * (1.0f - overlap))).roundToInt()

//    // Data specific to frequency plot
//    var frequencyPlotData by mutableStateOf(FrequencyPlotData(0, { 0f }, { 0f }))
//    var harmonicFrequencies by mutableStateOf<VerticalLinesPositions?>(null)
//    val frequencyPlotGestureBasedViewPort = GestureBasedViewPort()
//
//    // Data specific to correlation plot
//    var correlationPlotData by mutableStateOf(CorrelationPlotData(0, { 0f }, { 0f }))
//    val correlationPlotGestureBasedViewPort = GestureBasedViewPort()
//
//    // Data specific to history plot
//    var pitchHistoryState = PitchHistoryState(PitchHistoryState.computePitchHistorySize(
//        pref.pitchHistoryDuration.value, pref.sampleRate, pref.windowSize.value, pref.overlap.value
//    ))
//    val pitchHistoryGestureBasedViewPort = GestureBasedViewPort()
//    var tuningState by mutableStateOf(TuningState.Unknown)
//
//    // Data shared over different plots
//    var currentFrequency by mutableStateOf<Float?>(null)
//    var targetNote by mutableStateOf(pref.musicalScale.value.referenceNote)




}