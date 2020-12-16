package de.moekadu.tuner

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlin.math.sin

class TunerViewModel : ViewModel() {

    /// Source which conducts the audio recording.
    private val sampleSource = SoundSource(viewModelScope)

    private val _tunerResults = MutableLiveData<TunerResults>()
    val tunerResults : LiveData<TunerResults>
        get() = _tunerResults

    private var pitchHistorySize = 100
        set(value) {
            field = value
            pitchHistory.size = value
        }

    var a4Frequency = 440f
        set(value) {
            if (field != value) {
                field = value
                tuningFrequencyValues = TuningEqualTemperament(value)
            }
        }

    var windowSize = 4096
        set(value) {
            if (field != value) {
                field = value
                sampleSource.windowSize = value
            }
        }

    var overlap = 0.25f
        set(value) {
            if (field != value) {
                field = value
                sampleSource.overlap = value
            }
        }
    private var tuningFrequencyValues = TuningEqualTemperament(a4Frequency)
        set(value) {
            field = value
            pitchHistory.tuningFrequencies = value
            _tuningFrequencies.value = value
        }

    private val _tuningFrequencies = MutableLiveData<TuningFrequencies>().apply { value = tuningFrequencyValues }
    val tuningFrequencies: LiveData<TuningFrequencies>
        get() = _tuningFrequencies

    val pitchHistory = PitchHistory(pitchHistorySize, tuningFrequencyValues)

    private val correlationAndSpectrumComputer = CorrelationAndSpectrumComputer()
    private val pitchChooserAndAccuracyIncreaser = PitchChooserAndAccuracyIncreaser()

    var windowingFunction = WindowingFunction.Hamming

    init {
        //Log.v("TestRecordFlow", "TunerViewModel.init: application: $application")
//        sampleSource.testFunction = { t ->
//            val freq = 400f + 10*t
//            //Log.v("TestRecordFlow", "TunerViewModel.testfunction: f=$freq")
//            sin(t * 2 * kotlin.math.PI.toFloat() * freq)
//        }

        viewModelScope.launch {
            sampleSource.flow
                .buffer()
                .transform {
                    val result = correlationAndSpectrumComputer.run(it, windowingFunction)
                    emit(result)
                }
                .buffer()
                .transform {
                    it.correlationMaximaIndices = determineCorrelationMaxima(it.correlation, 25f, 5000f, it.dt)
                    emit(it)
                }
                .transform {
                    it.specMaximaIndices = determineSpectrumMaxima(it.ampSqrSpec, 25f, 5000f, it.dt, 10f)
                    emit(it)
                }
                .buffer()
                .transform {
                    it.pitchFrequency = pitchChooserAndAccuracyIncreaser.run(it)
                    emit(it)
                }
                .buffer()
                .collect {
                    _tunerResults.value = it
                    pitchHistory.appendValue(it.pitchFrequency)
                }
        }


    }

    fun startSampling() {
        sampleSource.restartSampling()
    }

    fun stopSampling() {
        sampleSource.stopSampling()
    }

    override fun onCleared() {
        stopSampling()
        super.onCleared()
    }
}
