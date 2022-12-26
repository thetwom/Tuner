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

package de.moekadu.tuner.viewmodels

import android.util.Log
import androidx.lifecycle.*
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentResources
import de.moekadu.tuner.instruments.instrumentChromatic
import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.misc.MemoryPool
import de.moekadu.tuner.misc.WaveWriter
import de.moekadu.tuner.models.CorrelationPlotModel
import de.moekadu.tuner.models.PitchHistoryModel
import de.moekadu.tuner.models.SpectrumPlotModel
import de.moekadu.tuner.models.StringsModel
import de.moekadu.tuner.notedetection.*
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class TunerViewModel(
    private val pref: PreferenceResources,
    private val instrumentResources: InstrumentResources,
    private val simpleMode: Boolean
) : ViewModel() {
    data class UserSelectedString(val stringIndex: Int, val note: MusicalNote)

    private var frequencyDetectionJob: Job? = null
    private var frequencyEvaluationJob: Job? = null

    private val chromaticInstrumentStateFlow = MutableStateFlow(
        InstrumentResources.InstrumentAndSection(instrumentChromatic, InstrumentResources.Section.Undefined)
    )

    val instrument get() = if (simpleMode)
        instrumentResources.instrument
    else
        chromaticInstrumentStateFlow.asStateFlow()

    val sampleRate = DefaultValues.SAMPLE_RATE
    val waveWriter = WaveWriter()

    private val _noteDetectionResults = MutableStateFlow<MemoryPool<FrequencyDetectionCollectedResults>.RefCountedMemory?>(null)
    private val noteDetectionResults = _noteDetectionResults.asStateFlow()

    private val _tuningTarget = MutableStateFlow(
        TuningTarget(pref.musicalScale.value.referenceNote, pref.musicalScale.value.referenceFrequency, false)
    )
    private val tuningTarget = _tuningTarget.asStateFlow()

    private val _pitchHistoryModel = MutableLiveData(PitchHistoryModel().apply { changeSettings(maxNumHistoryValues = computePitchHistorySize()) })
    val pitchHistoryModel: LiveData<PitchHistoryModel> get() = _pitchHistoryModel

    private val _stringsModel = MutableLiveData(StringsModel())
    val stringsModel: LiveData<StringsModel> get() = _stringsModel

    private val _spectrumPlotModel = MutableLiveData(SpectrumPlotModel())
    val spectrumPlotModel: LiveData<SpectrumPlotModel> get() = _spectrumPlotModel

    private val _correlationPlotModel = MutableLiveData(CorrelationPlotModel())
    val correlationPlotModel: LiveData<CorrelationPlotModel> get() = _correlationPlotModel

    private val _showWaveWriterFab = MutableStateFlow(false)
    val showWaveWriterFab = _showWaveWriterFab.asStateFlow()

    /** User defined target note or null, if target note is autodetected. */
    private val _userDefinedTargetNote = MutableStateFlow<UserSelectedString?>(null)
    private val userDefinedTargetNote = _userDefinedTargetNote.asStateFlow()

    private val _currentFrequency = MutableStateFlow(0f)
    private val currentFrequency = _currentFrequency.asStateFlow()

    private val _timeSinceThereIsNoFrequencyDetectionResult = MutableStateFlow(0f)
    private val timeSinceThereIsNoFrequencyDetectionResult = _timeSinceThereIsNoFrequencyDetectionResult.asStateFlow()


    init {
        viewModelScope.launch { pref.overlap.collect { overlap ->
            _pitchHistoryModel.value = pitchHistoryModel.value?.apply { changeSettings(maxNumHistoryValues = computePitchHistorySize(overlap = overlap)) }
            restartSamplingIfRunning()
        } }

        viewModelScope.launch { pref.windowSize.collect { windowSize ->
            _pitchHistoryModel.value = pitchHistoryModel.value?.apply { changeSettings(maxNumHistoryValues = computePitchHistorySize(windowSize = windowSize)) }
            restartSamplingIfRunning()
        } }

        viewModelScope.launch { pref.musicalScale.collect {
            _pitchHistoryModel.value = pitchHistoryModel.value?.apply { changeSettings(musicalScale = it) }
            if (simpleMode)
                _stringsModel.value = stringsModel.value?.apply { changeSettings(musicalScale = it) }
            restartFrequencyEvaluationJob(musicalScale = it)
            restartSamplingIfRunning()
        }}

        viewModelScope.launch { pref.toleranceInCents.collect {
            _pitchHistoryModel.value = pitchHistoryModel.value?.apply { changeSettings(toleranceInCents = it) }
            if (simpleMode) {
                _stringsModel.value = stringsModel.value?.apply {
                    changeSettings(tuningState = computeTuningState(toleranceInCents = it))
                }
            }
            restartFrequencyEvaluationJob(toleranceInCents = it.toFloat())
            restartSamplingIfRunning()
        }}

        viewModelScope.launch { pref.pitchHistoryMaxNumFaultyValues.collect {
            restartFrequencyEvaluationJob(maxNumFaultyValues = it)
        }}

        viewModelScope.launch { pref.maxNoise.collect {
            restartFrequencyEvaluationJob(maxNoise = it)
        }}

        viewModelScope.launch { pref.numMovingAverage.collect {
            restartFrequencyEvaluationJob(numMovingAverage = it)
        }}

        viewModelScope.launch { pref.waveWriterDurationInSeconds.collect { durationInSeconds ->
            if (!simpleMode) {
                val size = sampleRate * durationInSeconds
                waveWriter.setBufferSize(size)
                _showWaveWriterFab.value = (size > 0)
                restartSamplingIfRunning()
            }
        }}

        viewModelScope.launch { pref.notePrintOptions.collect { printOptions ->
            if (simpleMode) {
                _stringsModel.value =
                    stringsModel.value?.apply { changeSettings(notePrintOptions = printOptions) }
            } else {
                _spectrumPlotModel.value =
                    spectrumPlotModel.value?.apply { changeSettings(notePrintOptions = printOptions) }
                _correlationPlotModel.value =
                    correlationPlotModel.value?.apply { changeSettings(notePrintOptions = printOptions) }
            }
            _pitchHistoryModel.value = pitchHistoryModel.value?.apply{ changeSettings(notePrintOptions = printOptions)}
        }}

        viewModelScope.launch { instrument.collect { instrumentAndSection ->
            if (simpleMode) {
                _stringsModel.value =
                    stringsModel.value?.apply { changeSettings(instrument = instrumentAndSection.instrument) }
            }
            restartFrequencyEvaluationJob(instrument = instrumentAndSection.instrument)
            restartSamplingIfRunning()
        }}

        viewModelScope.launch { tuningTarget.collect {
            if (simpleMode) {
                _stringsModel.value = stringsModel.value?.apply {
                    changeSettings(
                        highlightedNote = tuningTarget.value.note,
                        tuningState = computeTuningState(targetFrequency = it.frequency)
                    )
                }
            } else {
                _spectrumPlotModel.value = spectrumPlotModel.value?.apply {
                    changeSettings(targetFrequency = it.frequency)
                }
                _correlationPlotModel.value = correlationPlotModel.value?.apply {
                    changeSettings(targetFrequency = it.frequency)
                }
            }
            _pitchHistoryModel.value = pitchHistoryModel.value?.apply { changeSettings(tuningTarget = it) }
        }}

        viewModelScope.launch { timeSinceThereIsNoFrequencyDetectionResult.collect {
            _pitchHistoryModel.value = pitchHistoryModel.value?.apply {
                changeSettings(isCurrentlyDetectingNotes = (it <= DURATION_FOR_MARKING_NOTEDETECTION_AS_INACTIVE))
            }
        }}

        viewModelScope.launch { currentFrequency.collect {
            if (currentFrequency.value > 0f) {
                _pitchHistoryModel.value = pitchHistoryModel.value?.apply { addFrequency(it) }
                if (simpleMode) {
                    _stringsModel.value = stringsModel.value?.apply {
                        changeSettings(tuningState = computeTuningState(currentFrequency = it))
                    }
                }
            }
        }}

        viewModelScope.launch { userDefinedTargetNote.collect {
//            Log.v("Tuner", "TunerViewModel: collecting userDefinedTargetNote = $it")
            if (simpleMode) {
                if (it == null)
                    _stringsModel.value?.changeSettings(highlightedStringIndex = -1)
                else
                    _stringsModel.value?.changeSettings(highlightedStringIndex = it.stringIndex)
            }
        }}

        if (!simpleMode) {
            viewModelScope.launch {
                var sampleNumberOfLastUpdate = 0
                val minSampleDiffToUpdateSpectrumAndCorrelation = sampleRate / MAXIMUM_REFRESH_RATE

                noteDetectionResults.collect {
//            Log.v("Tuner", "TunerViewModel: collecting noteDetectionResults: resultUpdateRate = ${computeResultUpdateRate()}")

                it?.with { results ->
                    val currentSampleNumber = results.timeSeries.framePosition
                    val diff = currentSampleNumber - sampleNumberOfLastUpdate

                    if (diff > minSampleDiffToUpdateSpectrumAndCorrelation || diff < 0) {
                        sampleNumberOfLastUpdate = currentSampleNumber

//              Log.v("Tuner", "TunerViewModel: collecting noteDetectionResults: time = ${results.timeSeries.framePosition}, dt=${results.timeSeries.dt}")
                        _spectrumPlotModel.value = spectrumPlotModel.value?.apply {
                            changeSettings(
                                frequencySpectrum = results.frequencySpectrum,
                                harmonics = results.harmonics,
                                detectedFrequency = results.harmonicStatistics.frequency
                            )
                        }
                        _correlationPlotModel.value = correlationPlotModel.value?.apply {
                            changeSettings(
                                autoCorrelation = results.autoCorrelation,
                                detectedFrequency = results.harmonicStatistics.frequency
                            )
                        }
                    }
                } }
            }
        }
    }

    private fun restartFrequencyEvaluationJob(
        numMovingAverage: Int = pref.numMovingAverage.value,
        toleranceInCents: Float = pref.toleranceInCents.value.toFloat(),
        maxNumFaultyValues: Int = pref.pitchHistoryMaxNumFaultyValues.value,
        maxNoise: Float = pref.maxNoise.value,
        musicalScale: MusicalScale = pref.musicalScale.value,
        instrument: Instrument = instrumentResources.instrument.value.instrument,
    ) {
        frequencyEvaluationJob?.cancel()
        frequencyEvaluationJob = viewModelScope.launch(Dispatchers.Main) {
//            Log.v("Tuner", "TunerViewModel.restartFrequencyEvaluationJob: restarting")
            val freqEvaluator = FrequencyEvaluator(
                numMovingAverage,
                toleranceInCents,
                maxNumFaultyValues,
                maxNoise,
                musicalScale,
                instrument
            )

            noteDetectionResults.combine(userDefinedTargetNote) { noteDetectionRes, userDefinedNote ->
                noteDetectionRes?.with {
                    freqEvaluator.evaluate(it, userDefinedNote?.note)
                } ?: freqEvaluator.evaluate(null, userDefinedNote?.note)
            }.collect {
                ensureActive()
//                Log.v("Tuner", "TunerViewModel: evaluating target: $it, $coroutineContext")
                it.target?.let{ tuningTarget ->
                    _tuningTarget.value = tuningTarget
                }
                _timeSinceThereIsNoFrequencyDetectionResult.value = it.timeSinceThereIsNoFrequencyDetectionResult
                if (it.smoothedFrequency > 0f)
                    _currentFrequency.value = it.smoothedFrequency
            }
        }
    }

    private fun restartSamplingIfRunning() {
        if (frequencyDetectionJob != null) {
            stopSampling()
            startSampling()
        }
    }

    fun startSampling() {
        frequencyDetectionJob?.cancel()
        frequencyDetectionJob = viewModelScope.launch(Dispatchers.Main) {
            frequencyDetectionFlow(pref, waveWriter).collect {
                ensureActive()
                noteDetectionResults.value?.decRef()
                _noteDetectionResults.value = it
//                Log.v("TunerViewModel", "collecting frequencyDetectionFlow")
            }
        }
    }

    fun stopSampling() {
//        Log.v("Tuner", "TunerViewModel.stopSampling")
        frequencyDetectionJob?.cancel()
        frequencyDetectionJob = null
    }

    /** Set a new target note and string index.
     *  @param stringIndex Index of string to highlight in string view or -1 for no string-based
     *    highlighting.
     *  @param note Target note, or note == null for automatic note detection.
     * */
    fun setTargetNote(stringIndex: Int, note: MusicalNote?) {
        _userDefinedTargetNote.value = if (note == null) null else UserSelectedString(stringIndex, note)
    }

    /** Click a string to select/deselect.
     * @param stringIndex Index of string.
     * @return true, if a string was selected, false if a string was deselected.
     */
    fun clickString(stringIndex: Int, note: MusicalNote): Boolean {
        Log.v("Tuner", "TunerViewModel.clickString: stringIndex=$stringIndex, ${instrument.value}")
        val userDefNote = userDefinedTargetNote.value
        return if (userDefNote?.stringIndex == stringIndex) {
            _userDefinedTargetNote.value = null
            false
        } else {
            _userDefinedTargetNote.value = UserSelectedString(stringIndex, note)
            true
        }
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
        sampleRate: Int = this.sampleRate,
        windowSize: Int = pref.windowSize.value,
        overlap: Float = pref.overlap.value
    ) = (duration / (windowSize.toFloat() / sampleRate.toFloat() * (1.0f - overlap))).roundToInt()

    private fun computeTuningState(
        currentFrequency: Float = this.currentFrequency.value,
        targetFrequency: Float = tuningTarget.value.frequency,
        toleranceInCents: Int = pref.toleranceInCents.value
    ) = checkTuning(currentFrequency, targetFrequency, toleranceInCents.toFloat())

    private fun computeResultUpdateRate(
        sampleRate: Int = this.sampleRate,
        windowSize: Int = pref.windowSize.value,
        overlap: Float = pref.overlap.value
    ) = sampleRate.toFloat() / (windowSize * (1.0f - overlap))

    companion object {
        const val DURATION_FOR_MARKING_NOTEDETECTION_AS_INACTIVE = 0.5f // in seconds
        const val MAXIMUM_REFRESH_RATE = 60 // Hz
    }

    class Factory(
        private val pref: PreferenceResources,
        private val instrumentResources: InstrumentResources,
        private val simpleMode: Boolean
    ) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TunerViewModel(pref, instrumentResources, simpleMode) as T
        }
    }
}
