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
import de.moekadu.tuner.notedetection.pitchHistoryDurationToPitchSamples
import de.moekadu.tuner.notedetection2.*
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

//private data class FrequencyEvaluationResult(
//    val smoothedFrequency: Float,
//    val target: TuningTarget?,
//    val timeSinceThereIsNoFrequencyDetectionResult: Float
//)


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

//    /// Source which conducts the audio recording.
//    private val sampleSource = SoundSource(viewModelScope)
    val sampleRate = DefaultValues.SAMPLE_RATE
        // get() = sampleSource.sampleRate

    val waveWriter = WaveWriter()
        //get() = sampleSource.waveWriter

//    private val _tunerResults = MutableLiveData<TunerResults>()
//    val tunerResults : LiveData<TunerResults>
//        get() = _tunerResults

    private val _noteDetectionResults = MutableStateFlow<MemoryPool<FrequencyDetectionCollectedResults>.RefCountedMemory?>(null)
    //private val _noteDetectionResults = MutableSharedFlow<MemoryPool<FrequencyDetectionCollectedResults>.RefCountedMemory?>(1, 0, BufferOverflow.DROP_OLDEST)
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
//    /** Compute number of samples to be stored in pitch history. */
//    private val pitchHistorySize
//        get() = pitchHistoryDurationToPitchSamples(
//            pref.pitchHistoryDuration.value, sampleRate, pref.windowSize.value, pref.overlap.value)

//    /** Duration in seconds between two updates for the pitch history. */
//    private val _pitchHistoryUpdateInterval = MutableLiveData(computePitchHistoryUpdateInterval())
//    val pitchHistoryUpdateInterval: LiveData<Float> = _pitchHistoryUpdateInterval

//    private val _preferFlat = MutableLiveData(false)
//    val preferFlat: LiveData<Boolean> get() = _preferFlat
//    val notePrintOptions get() = if (preferFlat.value == true) MusicalNotePrintOptions.PreferFlat else MusicalNotePrintOptions.PreferSharp

//    private var musicalScaleValue: MusicalScale =
//        MusicalScaleFactory.create(DefaultValues.TEMPERAMENT, null, null, DefaultValues.REFERENCE_FREQUENCY)
//        set(value) {
//            field = value
//            pitchHistory.musicalScale = value
//            changeTargetNoteSettings(musicalScale = value)
//            _musicalScale.value = value
//        }
//
//    private val _musicalScale = MutableLiveData<MusicalScale>().apply { value = musicalScaleValue }
//    val musicalScale: LiveData<MusicalScale>
//        get() = _musicalScale

//    private val _standardDeviation = MutableLiveData(0f)
//    val standardDeviation: LiveData<Float> get() = _standardDeviation

//    val pitchHistory = PitchHistory(computePitchHistorySize(), pref.musicalScale.value)

//    private val correlationAndSpectrumComputer = CorrelationAndSpectrumComputer()
//    private val pitchChooserAndAccuracyIncreaser = PitchChooserAndAccuracyIncreaser()

//    private val targetNoteValue = TargetNote().apply { instrument = instrumentDatabase[0] }
//    private val _targetNote = MutableLiveData(targetNoteValue)
//    val targetNote: LiveData<TargetNote>
//            get() = _targetNote

//    /** The selected string index in the string view, this is no live data since it is always changed togehter with the target note. */
//    var selectedStringIndex = -1
//        private set

//    private var userDefinedTargetNote: MusicalNote? = null
//        set(value) {
//            field = value
//            _isTargetNoteUserDefined.value = (field != null) // null -> AUTOMATIC_TARGET_NOTE_DETECTION
//        }
//    private val _isTargetNoteUserDefined = MutableLiveData(false)
//    val isTargetNoteUserDefined: LiveData<Boolean>
//        get() = _isTargetNoteUserDefined

    /** Currently used target note.
     * If the userDefinedTargetNote is not null, this will contain the userDefinedTargetNote.
     */
//    private val _targetNote = MutableStateFlow<MusicalNote?>(null)
//    val targetNote: StateFlow<MusicalNote?> get() = _targetNote

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

//        viewModelScope.launch { pref.pitchHistoryMaxNumFaultyValues.collect {
//            pitchHistory.maxNumFaultyValues = it
//        }}

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

        viewModelScope.launch { noteDetectionResults.collect {
            if (!simpleMode) {
                it?.incRef()
                it?.memory?.let { results ->
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
                it?.decRef()
            }
        }}
    }

    private fun restartFrequencyEvaluationJob(
        numMovingAverage: Int = pref.numMovingAverage.value,
        toleranceInCents: Float = pref.toleranceInCents.value.toFloat(),
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
                maxNoise,
                musicalScale,
                instrument
            )
//            val smoother = OutlierRemovingSmoother(
//                numMovingAverage,
//                DefaultValues.FREQUENCY_MIN,
//                DefaultValues.FREQUENCY_MAX,
//                relativeDeviationToBeAnOutlier = 0.1f,
//                maxNumSuccessiveOutliers = 2,
//                minNumValuesForValidMean = 2,
//                numBuffers = 3
//            )
//
//            val tuningTargetComputer = TuningTargetComputer(musicalScale, instrument, toleranceInCents)
//            var currentTargetNote: MusicalNote? = null
//            var timeStepOfLastSuccessfulFrequencyDetection = 0
//            var smoothedFrequency = 0f

            noteDetectionResults.combine(userDefinedTargetNote) { noteDetectionRes, userDefinedNote ->
                noteDetectionRes?.incRef()
                val result = freqEvaluator.evaluate(noteDetectionRes?.memory, userDefinedNote?.note)
//                var frequencyDetectionTimeStep = -1
//                var dt = -1f
//                val newTarget = noteDetectionRes?.let {
//                    val frequencyCollectionResults = it.memory
//                    smoothedFrequency = smoother(frequencyCollectionResults.frequency)
//                    frequencyDetectionTimeStep = frequencyCollectionResults.timeSeries.framePosition
//                    dt = frequencyCollectionResults.timeSeries.dt
//
//                    if (smoothedFrequency > 0f) {
////                        Log.v("Tuner", "TunerViewModel note evaluation smoothedFrequency = $smoothedFrequency")
//                        timeStepOfLastSuccessfulFrequencyDetection = frequencyDetectionTimeStep
//                        tuningTargetComputer(
//                            smoothedFrequency,
//                            currentTargetNote,
//                            userDefinedNote?.note
//                        )
//                    } else {
//                        null
//                    }
//                }
                noteDetectionRes?.decRef()
//                FrequencyEvaluationResult(
//                    smoothedFrequency,
//                    newTarget,
//                    (frequencyDetectionTimeStep - timeStepOfLastSuccessfulFrequencyDetection) * dt
//                )
                result
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

//    fun startSampling() {
//        //Log.v("Tuner", "TunerViewModel.startSampling")
//        //sampleSource.restartSampling()
//    }

    fun stopSampling() {
//        Log.v("Tuner", "TunerViewModel.stopSampling")
//        sampleSource.stopSampling()
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

//    private fun setInstrument(instrument: Instrument) {
//        instrumentResources.selectInstrument(instrument)

////        Log.v("Tuner", "TunerViewModel.setInstrument $instrument, before: ${targetNoteValue.instrument}")
//        // val oldTargetNote = targetNoteValue.toneIndex
//        if (targetNoteValue.instrument.stableId != instrument.stableId) {
//            //Log.v("Tuner", "TunerViewModel.setInstrument ...")
//            targetNoteValue.instrument = instrument
//            setTargetNote(-1, null)
//        }
////        userDefinedTargetNoteIndex = AUTOMATIC_TARGET_NOTE_DETECTION
////        pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
////            updateFrequencyPlotRange(targetNoteValue.toneIndex, frequency)
////        }
////        _targetNote.value = targetNoteValue
////        if (oldTargetNote != targetNoteValue.toneIndex) { // changing instrument can change target note
////            pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
////                updateFrequencyPlotRange(targetNoteValue.toneIndex, frequency)
////            }
////            _targetNote.value = targetNoteValue
////        }
//    }

//    private fun computePitchHistoryUpdateInterval(
//        windowSize: Int = pref.windowSize.value,
//        overlap: Float = pref.overlap.value,
//        sampleRate: Int = this.sampleRate
//    ) = windowSize * (1f - overlap) / sampleRate

    /** Compute number of samples to be stored in pitch history. */
    private fun computePitchHistorySize(
        duration: Float = pref.pitchHistoryDuration.value,
        sampleRate: Int = this.sampleRate,
        windowSize: Int = pref.windowSize.value,
        overlap: Float = pref.overlap.value
    ) = pitchHistoryDurationToPitchSamples(duration, sampleRate, windowSize, overlap)

    private fun computeTuningState(
        currentFrequency: Float = this.currentFrequency.value,
        targetFrequency: Float = tuningTarget.value.frequency,
        toleranceInCents: Int = pref.toleranceInCents.value
    ) = checkTuning(currentFrequency, targetFrequency, toleranceInCents.toFloat())

//    override fun onCleared() {
////        Log.v("Tuner", "TunerViewModel.onCleared")
//        stopSampling()
////        pref.unregisterOnSharedPreferenceChangeListener(onPreferenceChangedListener)
//
//        super.onCleared()
//    }

    companion object {
        const val DURATION_FOR_MARKING_NOTEDETECTION_AS_INACTIVE = 0.5f // in seconds
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
