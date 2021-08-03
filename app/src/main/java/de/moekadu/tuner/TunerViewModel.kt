/*
 * Copyright 2020 Michael Moessner
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

package de.moekadu.tuner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin
import kotlin.random.Random

class TunerViewModel : ViewModel() {

    /// Source which conducts the audio recording.
    private val sampleSource = SoundSource(viewModelScope)

    private val _tunerResults = MutableLiveData<TunerResults>()
    val tunerResults : LiveData<TunerResults>
        get() = _tunerResults

    var pitchHistoryDuration = 3.0f
        set(value) {
            if (value != field) {
                field = value
//                Log.v("Tuner", "TunerViewModel.pitchH istoryDuration: new duration = $value, new size = $pitchHistorySize")
                pitchHistory.size = pitchHistorySize
            }
        }

    /// Compute number of samples to be stored in pitch history.
    private val pitchHistorySize
        get() = pitchHistoryDurationToPitchSamples(
            pitchHistoryDuration, sampleSource.sampleRate, windowSize, overlap)

    /// Duration in seconds between two updates for the pitch history
    val pitchHistoryUpdateInterval
        get() = windowSize.toFloat() * (1f - overlap) / sampleSource.sampleRate

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
                pitchHistory.size = pitchHistorySize
            }
        }

    var overlap = 0.25f
        set(value) {
            if (field != value) {
                field = value
                sampleSource.overlap = value
                pitchHistory.size = pitchHistorySize
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

    var useHint = true

    init {
        //Log.v("TestRecordFlow", "TunerViewModel.init: application: $application")

//        sampleSource.testFunction = { t ->
//            val freq = 400f + 2*t
//           //Log.v("TestRecordFlow", "TunerViewModel.testfunction: f=$freq")
//            sin(t * 2 * kotlin.math.PI.toFloat() * freq)
//        }
//        sampleSource.testFunction = { t ->
//            800f * Random.nextFloat()
//            //1f
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
                    withContext(Dispatchers.Default) {
                        it.correlationMaximaIndices =
                            determineCorrelationMaxima(it.correlation, 25f, 5000f, it.dt)
                    }
                    emit(it)
                }
                .transform {
                    withContext(Dispatchers.Default) {
                        it.specMaximaIndices =
                            determineSpectrumMaxima(it.ampSqrSpec, 25f, 5000f, it.dt, 10f)
                    }
                    emit(it)
                }
                .buffer()
                .transform {
                    it.pitchFrequency = pitchChooserAndAccuracyIncreaser.run(it, if (useHint) pitchHistory.history.value?.lastOrNull() else null)
                    emit(it)
                }
                .buffer()
                .collect {
                    it.pitchFrequency?.let {pitchFrequency ->
                        _tunerResults.value = it
                        if (pitchFrequency > 0.0f)
                            pitchHistory.appendValue(pitchFrequency)
                    }
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
