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

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*

class TunerViewModel(application: Application) : AndroidViewModel(application) {

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
            changeTargetNoteSettings(tuningFrequencies = value)
            _tuningFrequencies.value = value
        }

    private var _instrument = MutableLiveData<Instrument>().apply { value = instrumentDatabase[1] }
    val instrument: LiveData<Instrument>
        get() = _instrument

    private val _tuningFrequencies = MutableLiveData<TuningFrequencies>().apply { value = tuningFrequencyValues }
    val tuningFrequencies: LiveData<TuningFrequencies>
        get() = _tuningFrequencies

    private val _standardDeviation = MutableLiveData(0f)
    val standardDeviation: LiveData<Float> get() = _standardDeviation

    val pitchHistory = PitchHistory(pitchHistorySize, tuningFrequencyValues)

    private val correlationAndSpectrumComputer = CorrelationAndSpectrumComputer()
    private val pitchChooserAndAccuracyIncreaser = PitchChooserAndAccuracyIncreaser()

    var windowingFunction = WindowingFunction.Hamming

    var useHint = true

    private val targetNoteValue = TargetNote().apply { instrument = _instrument.value!! }
    private val _targetNote = MutableLiveData(targetNoteValue)
    val targetNote: LiveData<TargetNote>
            get() = _targetNote

    private var userDefinedTargetNoteIndex = AUTOMATIC_TARGET_NOTE_DETECTION
        set(value) {
            field = value
            _isTargetNoteUserDefined.value = (field != AUTOMATIC_TARGET_NOTE_DETECTION)
        }
    private val _isTargetNoteUserDefined = MutableLiveData(false)
    val isTargetNoteUserDefined: LiveData<Boolean>
        get() = _isTargetNoteUserDefined

    private val frequencyPlotRangeValues = floatArrayOf(400f, 500f)
    private val _frequencyPlotRange = MutableLiveData(frequencyPlotRangeValues)
    val frequencyPlotRange: LiveData<FloatArray>
        get() = _frequencyPlotRange

    private val pref = PreferenceManager.getDefaultSharedPreferences(application)
    
    private val onPreferenceChangedListener = object : SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (sharedPreferences == null)
                return
//            Log.v("Tuner", "TunerFragment.setupPreferenceListener: key=$key")
            when (key) {
                "a4_frequency" -> {
//                    Log.v("Tuner", "TunerFragment.setupPreferenceListener: a4_frequency changed")
                    a4Frequency = sharedPreferences.getString("a4_frequency", "440")?.toFloat() ?: 440f
                }
                "windowing" -> {
                    val value = sharedPreferences.getString(key, null)
                    windowingFunction =
                        when (value) {
                            "no_window" -> WindowingFunction.Tophat
                            "window_hamming" -> WindowingFunction.Hamming
                            "window_hann" -> WindowingFunction.Hann
                            else -> throw RuntimeException("Unknown window")
                        }
                }
                "window_size" -> {
                    windowSize = indexToWindowSize(sharedPreferences.getInt(key, 5))
                }
                "overlap" -> {
                    overlap = sharedPreferences.getInt(key, 25) / 100f
                }
                "pitch_history_duration" -> {
                    pitchHistoryDuration = percentToPitchHistoryDuration(sharedPreferences.getInt(key, 50))
                }
                "pitch_history_num_faulty_values" -> {
                    pitchHistory.maxNumFaultyValues = sharedPreferences.getInt(key, 3)
                }
                "use_hint" -> {
                    useHint = sharedPreferences.getBoolean(key, true)
                }
                "num_moving_average" -> {
                    pitchHistory.numMovingAverage = sharedPreferences.getInt(key, 5)
                }
                "max_noise" -> {
                    pitchHistory.maxNoise = sharedPreferences.getInt(key, 10) / 100f
                }
                "tolerance_in_cents" -> {
                    changeTargetNoteSettings(tolerance = indexToTolerance(sharedPreferences.getInt(key, 3)))
                }
            }
        }
    }

    init {
        Log.v("TestRecordFlow", "TunerViewModel.init: application: $application")

        sampleSource.testFunction = { t ->
            val freq = 400 + 2*t
           //Log.v("TestRecordFlow", "TunerViewModel.testfunction: f=$freq")
            sin(t * 2 * kotlin.math.PI.toFloat() * freq)
        }
//        sampleSource.testFunction = { t ->
//            800f * Random.nextFloat()
//            //1f
//        }

        pref.registerOnSharedPreferenceChangeListener(onPreferenceChangedListener)
        loadSettingsFromSharedPreferences()

        changeTargetNoteSettings(tuningFrequencies = tuningFrequencyValues)

        viewModelScope.launch {
            sampleSource.flow
                .buffer()
                .transform {
                    val result = correlationAndSpectrumComputer.run(it, windowingFunction)
                    result.noise = if (result.correlation.size > 1) 1f - result.correlation[1] / result.correlation[0] else 1f

                    _standardDeviation.value = withContext(Dispatchers.Default) {
                        val average = it.data.average().toFloat()
                        sqrt(it.data.fold(0f) {sum, element -> sum + (element - average).pow(2)}/ it.data.size)
                    }
                    sampleSource.recycle(it)
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
                        val resultsFromLiveData = _tunerResults.value
                        val results = if (resultsFromLiveData != null && resultsFromLiveData.size == it.size && resultsFromLiveData.sampleRate == it.sampleRate)
                            resultsFromLiveData
                        else
                            TunerResults(it.size, it.sampleRate)
                        results.set(it)
                        _tunerResults.value = results
                        if (pitchFrequency > 0.0f) {
                            pitchHistory.appendValue(pitchFrequency, it.noise)
                            if (userDefinedTargetNoteIndex == AUTOMATIC_TARGET_NOTE_DETECTION) {
                                pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
                                    val oldTargetNoteIndex = targetNoteValue.toneIndex
                                    targetNoteValue.setTargetNoteBasedOnFrequency(frequency)
                                    if (targetNoteValue.toneIndex != oldTargetNoteIndex)
                                        _targetNote.value = targetNoteValue
                                }
                                //changeTargetNoteSettings(toneIndex = pitchHistory.currentEstimatedToneIndex)
                            }

                            pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
                                updateFrequencyPlotRange(targetNoteValue.toneIndex, frequency)
                            }
                        }
                    }
                    correlationAndSpectrumComputer.recycle(it)
                }
        }
    }

    fun startSampling() {
        Log.v("Tuner", "TunerViewModel.startSampling")
        sampleSource.restartSampling()
    }

    fun stopSampling() {
        Log.v("Tuner", "TunerViewModel.stopSampling")
        sampleSource.stopSampling()
    }

    fun setTargetNote(toneIndex: Int = AUTOMATIC_TARGET_NOTE_DETECTION) {
        Log.v("Tuner", "TunerViewModel.setTargetNote: toneIndex=$toneIndex")
        val oldTargetNote = targetNoteValue.toneIndex

        if (toneIndex == AUTOMATIC_TARGET_NOTE_DETECTION) {
            userDefinedTargetNoteIndex = AUTOMATIC_TARGET_NOTE_DETECTION
            pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
                targetNoteValue.setTargetNoteBasedOnFrequency(frequency)
            }
        } else {
            userDefinedTargetNoteIndex = toneIndex
            targetNoteValue.setToneIndexExplicitely(toneIndex)
            //changeTargetNoteSettings(toneIndex = toneIndex)
        }

        if (targetNoteValue.toneIndex != oldTargetNote) {
            pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
                updateFrequencyPlotRange(targetNoteValue.toneIndex, frequency)
            }
            _targetNote.value = targetNoteValue
        }
    }

    fun setInstrument(instrument: Instrument) {
        val currentInstrument = _instrument.value
        if (currentInstrument == null || currentInstrument.id == instrument.id) {
            _instrument.value = instrument
            val oldTargetNote = targetNoteValue.toneIndex
            targetNoteValue.instrument = instrument

            if (oldTargetNote != targetNoteValue.toneIndex) { // changing instrument can change target note
                pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
                    updateFrequencyPlotRange(targetNoteValue.toneIndex, frequency)
                }
                _targetNote.value = targetNoteValue
            }
        }
    }

    private fun updateFrequencyPlotRange(targetNoteIndex: Int, currentFrequency: Float) {
        val minOld = frequencyPlotRangeValues[0]
        val maxOld = frequencyPlotRangeValues[1]

        val frequencyToneIndex = tuningFrequencyValues.getClosestToneIndex(currentFrequency)
        val minIndex = min(frequencyToneIndex - 0.55f, targetNoteIndex - 1.55f)
        val maxIndex = max(frequencyToneIndex + 0.55f, targetNoteIndex + 1.55f)
        frequencyPlotRangeValues[0] = tuningFrequencyValues.getNoteFrequency(minIndex)
        frequencyPlotRangeValues[1] = tuningFrequencyValues.getNoteFrequency(maxIndex)
        if (frequencyPlotRangeValues[0] != minOld || frequencyPlotRangeValues[1] != maxOld)
            _frequencyPlotRange.value = frequencyPlotRangeValues
    }

    override fun onCleared() {
        Log.v("Tuner", "TunerViewModel.onCleared")
        stopSampling()
        pref.unregisterOnSharedPreferenceChangeListener(onPreferenceChangedListener)

        super.onCleared()
    }

    private fun changeTargetNoteSettings(tolerance: Int = NO_NEW_TOLERANCE,
                                         tuningFrequencies: TuningFrequencies? = null) {
        var changed = false
        if (tolerance != NO_NEW_TOLERANCE && tolerance != targetNoteValue.toleranceInCents) {
            targetNoteValue.toleranceInCents = tolerance
            changed = true
        }
        if (tuningFrequencies != null) {
            targetNoteValue.tuningFrequencies = tuningFrequencies
            changed = true
        }
        if (changed) {
            _targetNote.value = targetNoteValue
            pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
                updateFrequencyPlotRange(targetNoteValue.toneIndex, frequency)
            }
        }
    }

    private fun loadSettingsFromSharedPreferences() {
        a4Frequency = pref.getString("a4_frequency", "440")?.toFloat() ?: 440f
        windowingFunction = when (pref.getString("windowing", "no_window")) {
            "no_window" -> WindowingFunction.Tophat
            "window_hamming" -> WindowingFunction.Hamming
            "window_hann" -> WindowingFunction.Hann
            else -> throw RuntimeException("Unknown window")
        }
        windowSize = indexToWindowSize(pref.getInt("window_size", 5))
        overlap = pref.getInt("overlap", 25) / 100f
        pitchHistoryDuration = percentToPitchHistoryDuration(pref.getInt("pitch_history_duration", 50))
        pitchHistory.maxNumFaultyValues = pref.getInt("pitch_history_num_faulty_values", 3)
        pitchHistory.numMovingAverage = pref.getInt("num_moving_average", 5)
        useHint = pref.getBoolean("use_hint", true)
        pitchHistory.maxNoise = pref.getInt("max_noise", 10) / 100f
        changeTargetNoteSettings(tolerance = indexToTolerance(pref.getInt("tolerance_in_cents", 3)))
    }

    companion object {
        const val NO_NEW_TONE_INDEX = Int.MAX_VALUE
        const val NO_NEW_TOLERANCE = Int.MAX_VALUE
        const val AUTOMATIC_TARGET_NOTE_DETECTION = Int.MAX_VALUE
    }
}
