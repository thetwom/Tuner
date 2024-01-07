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

import androidx.lifecycle.*
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentResources
import de.moekadu.tuner.instruments.instrumentChromatic
import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.misc.MemoryPool
import de.moekadu.tuner.misc.RestartableJob
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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Class for tuner view model.
 * @param pref Preferences.
 * @param instrumentResources Instruments or null to use a chromatic instrument.
 *   Also, if this is null, we assume that we are in simple mode with a string view and no
 *   correlation and spectrum plots.
 */
class TunerViewModel(
    private val pref: PreferenceResources,
    private val instrumentResources: InstrumentResources?
) : ViewModel() {
    data class UserSelectedString(val stringIndex: Int, val note: MusicalNote)
    data class FrequencyWithFramePosition(val frequency: Float, val framePosition: Int)
    /** Simple mode (with string view) or no simple mode (with spectrum and correlation plots). */
    private val simpleMode = (instrumentResources != null)

    /** Job for finding frequencies */
    private var frequencyDetectionJob: RestartableJob? = null
    /** Job for translating frequencies to a target note. */
    private var frequencyEvaluationJob: RestartableJob? = null

    /** Instrument which is used if there are no instrument resources. */
    private val chromaticInstrumentStateFlow = MutableStateFlow(
        InstrumentResources.InstrumentAndSection(instrumentChromatic, InstrumentResources.Section.Undefined)
    )

    /** Current instrument with instrument section. */
    val instrument get() = instrumentResources?.instrument ?: chromaticInstrumentStateFlow.asStateFlow()

    /** Sample rate. */
    val sampleRate = DefaultValues.SAMPLE_RATE
    /** Writer class which allows dumping recorded data into wav-files. */
    val waveWriter = WaveWriter()
    /** Collected results from the frequency detection. */
    private val _frequencyDetectionResults = MutableStateFlow<MemoryPool<FrequencyDetectionCollectedResults>.RefCountedMemory?>(null)
    private val frequencyDetectionResults = _frequencyDetectionResults.asStateFlow()

    /** Current tuning target. */
    private val _tuningTarget = MutableStateFlow(
        TuningTarget(
            pref.musicalScale.value.referenceNote,
            pref.musicalScale.value.referenceFrequency,
            isPartOfInstrument = false,
            instrumentHasNoStrings = !instrument.value.instrument.isChromatic && instrument.value.instrument.strings.isEmpty()
        )
    )
    private val tuningTarget = _tuningTarget.asStateFlow()

    /** Model class for the pitch history. */
    private val _pitchHistoryModel = MutableLiveData(PitchHistoryModel().apply { changeSettings(maxNumHistoryValues = computePitchHistorySize()) })
    val pitchHistoryModel: LiveData<PitchHistoryModel> get() = _pitchHistoryModel

    /** Model class for strings (only in simple mode). */
    private val _stringsModel = MutableLiveData(StringsModel())
    val stringsModel: LiveData<StringsModel> get() = _stringsModel

    /** Model class for showing the spectrum (only if simple mode is disabled). */
    private val _spectrumPlotModel = MutableLiveData(SpectrumPlotModel())
    val spectrumPlotModel: LiveData<SpectrumPlotModel> get() = _spectrumPlotModel

    /** Model class for showing the autocorrelation (only if simple mode is disabled). */
    private val _correlationPlotModel = MutableLiveData(CorrelationPlotModel())
    val correlationPlotModel: LiveData<CorrelationPlotModel> get() = _correlationPlotModel

    /** Info if the wave-writer fab button should be shown. */
    private val _showWaveWriterFab = MutableStateFlow(false)
    val showWaveWriterFab = _showWaveWriterFab.asStateFlow()

    /** User defined target note or null, if target note is auto-detected. */
    private val _userDefinedTargetNote = MutableStateFlow<UserSelectedString?>(null)
    private val userDefinedTargetNote = _userDefinedTargetNote.asStateFlow()

    /** Currently detected base frequency. */
    private val _currentFrequency = MutableStateFlow(FrequencyWithFramePosition(0f, 0))
    private val currentFrequency = _currentFrequency.asStateFlow()

    /** Time duration since we didn't get any new frequencies. */
    private val _timeSinceThereIsNoFrequencyDetectionResult = MutableStateFlow(0f)
    private val timeSinceThereIsNoFrequencyDetectionResult = _timeSinceThereIsNoFrequencyDetectionResult.asStateFlow()

    init {
        frequencyDetectionJob = RestartableJob(viewModelScope) {
            viewModelScope.launch(Dispatchers.Default) {
                frequencyDetectionFlow(pref, waveWriter)
                    .collect {
                        ensureActive()
                        frequencyDetectionResults.value?.decRef()
                        _frequencyDetectionResults.value = it
                    }
            }
        }

        frequencyEvaluationJob = RestartableJob(viewModelScope) {
            viewModelScope.launch(Dispatchers.Default) {
//            Log.v("Tuner", "TunerViewModel.restartFrequencyEvaluationJob: restarting")
                val freqEvaluator = FrequencyEvaluator(
                    pref.numMovingAverage.value,
                    pref.toleranceInCents.value.toFloat(),
                    pref.pitchHistoryMaxNumFaultyValues.value,
                    pref.maxNoise.value,
                    pref.minHarmonicEnergyContent.value,
                    pref.musicalScale.value,
                    instrument.value.instrument,
                )

                frequencyDetectionResults.combine(userDefinedTargetNote) { noteDetectionRes, userDefinedNote ->
                    noteDetectionRes?.with {
//                    Log.v("Tuner", "TunerViewModel: collecting noteDetectionResults, framePosition = ${it.timeSeries.framePosition}")
                        freqEvaluator.evaluate(it, userDefinedNote?.note)
                    } ?: freqEvaluator.evaluate(null, userDefinedNote?.note)
                }.collect {
                    ensureActive()
//                Log.v("Tuner", "TunerViewModel: collecting frequencyEvaluationResults, framePosition = ${it.framePosition}")
//                Log.v("Tuner", "TunerViewModel: evaluating target: $it, $coroutineContext")
                    it.target?.let{ tuningTarget ->
                        _tuningTarget.value = tuningTarget
                    }
                    _timeSinceThereIsNoFrequencyDetectionResult.value = it.timeSinceThereIsNoFrequencyDetectionResult
                    if (it.smoothedFrequency > 0f)
                        _currentFrequency.value = FrequencyWithFramePosition(it.smoothedFrequency, it.framePosition)
                }
            }
        }
        frequencyEvaluationJob?.restart()

        viewModelScope.launch { pref.overlap.collect { overlap ->
            _pitchHistoryModel.value = pitchHistoryModel.value?.apply { changeSettings(maxNumHistoryValues = computePitchHistorySize(overlap = overlap)) }
            frequencyDetectionJob?.restartIfRunning()
        } }

        viewModelScope.launch { pref.windowSize.collect { windowSize ->
            _pitchHistoryModel.value = pitchHistoryModel.value?.apply { changeSettings(maxNumHistoryValues = computePitchHistorySize(windowSize = windowSize)) }
            frequencyDetectionJob?.restartIfRunning()
        } }

        viewModelScope.launch { pref.musicalScale.collect {
            _pitchHistoryModel.value = pitchHistoryModel.value?.apply { changeSettings(musicalScale = it) }
            if (simpleMode)
                _stringsModel.value = stringsModel.value?.apply { changeSettings(musicalScale = it) }
            frequencyEvaluationJob?.restartIfRunning()
            frequencyDetectionJob?.restartIfRunning()
        }}

        viewModelScope.launch { pref.toleranceInCents.collect {
            _pitchHistoryModel.value = pitchHistoryModel.value?.apply { changeSettings(toleranceInCents = it) }
            if (simpleMode) {
                _stringsModel.value = stringsModel.value?.apply {
                    changeSettings(tuningState = computeTuningState(toleranceInCents = it))
                }
            }
            frequencyEvaluationJob?.restartIfRunning()
            frequencyDetectionJob?.restartIfRunning()
        }}

        viewModelScope.launch { pref.pitchHistoryMaxNumFaultyValues.collect {
            frequencyEvaluationJob?.restartIfRunning()
        }}

        viewModelScope.launch { pref.maxNoise.collect {
            frequencyEvaluationJob?.restartIfRunning()
        }}

        viewModelScope.launch { pref.minHarmonicEnergyContent.collect {
            frequencyEvaluationJob?.restartIfRunning()
        }}

        viewModelScope.launch { pref.numMovingAverage.collect {
            frequencyEvaluationJob?.restartIfRunning()
        }}

        viewModelScope.launch { pref.waveWriterDurationInSeconds.collect { durationInSeconds ->
            if (!simpleMode) {
                val size = sampleRate * durationInSeconds
                waveWriter.setBufferSize(size)
                _showWaveWriterFab.value = (size > 0)
                frequencyDetectionJob?.restartIfRunning()
            }
        }}

        viewModelScope.launch { pref.noteNamePrinter.collect { printer ->
            if (simpleMode) {
                _stringsModel.value =
                    stringsModel.value?.apply { changeSettings(noteNamePrinter = printer) }
            } else {
                _spectrumPlotModel.value =
                    spectrumPlotModel.value?.apply { changeSettings(noteNamePrinter = printer) }
                _correlationPlotModel.value =
                    correlationPlotModel.value?.apply { changeSettings(noteNamePrinter = printer) }
            }
            _pitchHistoryModel.value = pitchHistoryModel.value?.apply{ changeSettings(noteNamePrinter = printer)}
        }}

        if (simpleMode) {
            viewModelScope.launch { instrument.collect { instrumentAndSection ->
                _stringsModel.value =
                    stringsModel.value?.apply { changeSettings(instrument = instrumentAndSection.instrument) }
                frequencyEvaluationJob?.restartIfRunning()
                frequencyDetectionJob?.restartIfRunning()
            } }
        }

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

        viewModelScope.launch {
            var framePositionOfLastUpdate = 0
            val minSampleDiffToUpdateView = sampleRate / MAXIMUM_REFRESH_RATE
            currentFrequency.collect {
//                Log.v("Tuner", "TunerViewModel: collecting currentFrequency, framePosition = ${it.framePosition}")
                if (it.frequency > 0f) {
                    val currentFramePosition = it.framePosition
                    val diff = currentFramePosition - framePositionOfLastUpdate

                    if (diff > minSampleDiffToUpdateView || diff < 0) {
                        framePositionOfLastUpdate = currentFramePosition
                        _pitchHistoryModel.value = pitchHistoryModel.value?.apply { addFrequency(it.frequency, cacheOnly = false) }
                    } else {
                        _pitchHistoryModel.value?.addFrequency(it.frequency, cacheOnly = true)
                    }

                    if (simpleMode) {
                        _stringsModel.value = stringsModel.value?.apply {
                            changeSettings(tuningState = computeTuningState(currentFrequency = it.frequency))
                        }
                    }
                }
            }
        }

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
                var framePositionOfLastUpdate = 0
                val minSampleDiffToUpdateSpectrumAndCorrelation = sampleRate / MAXIMUM_REFRESH_RATE

                frequencyDetectionResults.collect {
//            Log.v("Tuner", "TunerViewModel: collecting noteDetectionResults: resultUpdateRate = ${computeResultUpdateRate()}")

                it?.with { results ->
                    val framePositionNumber = results.timeSeries.framePosition
                    val diff = framePositionNumber - framePositionOfLastUpdate

                    if (diff > minSampleDiffToUpdateSpectrumAndCorrelation || diff < 0) {
                        framePositionOfLastUpdate = framePositionNumber

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

    /** Start sampling and frequency detection.
     * This will use the current preference (pref) to get the current settings.
     */
    fun startSampling() {
        frequencyDetectionJob?.restart()
    }

    /** Stop sampling. */
    fun stopSampling() {
        // Log.v("Tuner", "TunerViewModel.stopSampling")
        frequencyDetectionJob?.stop()
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
//        Log.v("Tuner", "TunerViewModel.clickString: stringIndex=$stringIndex, ${instrument.value}")
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

    /** Compute current tuning state. */
    private fun computeTuningState(
        currentFrequency: Float = this.currentFrequency.value.frequency,
        targetFrequency: Float = tuningTarget.value.frequency,
        toleranceInCents: Int = pref.toleranceInCents.value
    ) = checkTuning(currentFrequency, targetFrequency, toleranceInCents.toFloat())

    /** Compute update rate with which we get new frequency results. */
    private fun computeResultUpdateRate(
        sampleRate: Int = this.sampleRate,
        windowSize: Int = pref.windowSize.value,
        overlap: Float = pref.overlap.value
    ) = sampleRate.toFloat() / (windowSize * (1.0f - overlap))

    companion object {
        const val DURATION_FOR_MARKING_NOTEDETECTION_AS_INACTIVE = 0.5f // in seconds
        const val MAXIMUM_REFRESH_RATE = 60 // Hz
    }

    /** Factory for creating the view model.
     * @param pref Preferences.
     * @param instrumentResources Instruments or null to use a chromatic instrument.
     *   Also, if this is null, we assume that we are in simple mode with a string view and no
     *   correlation and spectrum plots.
     */
    class Factory(
        private val pref: PreferenceResources,
        private val instrumentResources: InstrumentResources?
    ) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TunerViewModel(pref, instrumentResources) as T
        }
    }
}
