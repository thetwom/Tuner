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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.instrumentDatabase
import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.misc.MemoryPool
import de.moekadu.tuner.misc.WaveWriter
import de.moekadu.tuner.notedetection.PitchHistory
import de.moekadu.tuner.notedetection.pitchHistoryDurationToPitchSamples
import de.moekadu.tuner.notedetection2.AcousticZeroWeighting
import de.moekadu.tuner.notedetection2.CollectedResults
import de.moekadu.tuner.notedetection2.noteDetectionFlow
import de.moekadu.tuner.preferenceResources
import de.moekadu.tuner.temperaments.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class TunerViewModel(application: Application) : AndroidViewModel(application) {

    private val pref = application.preferenceResources

    private var noteDetectionJob: Job? = null

//    /// Source which conducts the audio recording.
//    private val sampleSource = SoundSource(viewModelScope)
    val sampleRate = DefaultValues.SAMPLE_RATE
        // get() = sampleSource.sampleRate

    val waveWriter = WaveWriter()
        //get() = sampleSource.waveWriter

//    private val _tunerResults = MutableLiveData<TunerResults>()
//    val tunerResults : LiveData<TunerResults>
//        get() = _tunerResults

    private val _noteDetectionResults = MutableLiveData<MemoryPool<CollectedResults>.RefCountedMemory>()
    val noteDetectionResults: LiveData<MemoryPool<CollectedResults>.RefCountedMemory>
        get() = _noteDetectionResults

//    /** Compute number of samples to be stored in pitch history. */
//    private val pitchHistorySize
//        get() = pitchHistoryDurationToPitchSamples(
//            pref.pitchHistoryDuration.value, sampleRate, pref.windowSize.value, pref.overlap.value)

    /** Duration in seconds between two updates for the pitch history. */
    private val _pitchHistoryUpdateInterval = MutableLiveData(computePitchHistoryUpdateInterval())
    val pitchHistoryUpdateInterval: LiveData<Float> = _pitchHistoryUpdateInterval

//    private val _preferFlat = MutableLiveData(false)
//    val preferFlat: LiveData<Boolean> get() = _preferFlat
//    val notePrintOptions get() = if (preferFlat.value == true) MusicalNotePrintOptions.PreferFlat else MusicalNotePrintOptions.PreferSharp

    private var musicalScaleValue: MusicalScale =
        MusicalScaleFactory.create(DefaultValues.TEMPERAMENT, null, null, DefaultValues.REFERENCE_FREQUENCY)
        set(value) {
            field = value
            pitchHistory.musicalScale = value
            changeTargetNoteSettings(musicalScale = value)
            _musicalScale.value = value
        }

    private val _musicalScale = MutableLiveData<MusicalScale>().apply { value = musicalScaleValue }
    val musicalScale: LiveData<MusicalScale>
        get() = _musicalScale

//    private val _standardDeviation = MutableLiveData(0f)
//    val standardDeviation: LiveData<Float> get() = _standardDeviation

    val pitchHistory = PitchHistory(computePitchHistorySize(), musicalScaleValue)

//    private val correlationAndSpectrumComputer = CorrelationAndSpectrumComputer()
//    private val pitchChooserAndAccuracyIncreaser = PitchChooserAndAccuracyIncreaser()

    private val targetNoteValue = TargetNote().apply { instrument = instrumentDatabase[0] }
    private val _targetNote = MutableLiveData(targetNoteValue)
    val targetNote: LiveData<TargetNote>
            get() = _targetNote

    /** The selected string index in the string view, this is no live data since it is always changed togehter with the target note. */
    var selectedStringIndex = -1
        private set

    private var userDefinedTargetNote: MusicalNote? = null
        set(value) {
            field = value
            _isTargetNoteUserDefined.value = (field != null) // null -> AUTOMATIC_TARGET_NOTE_DETECTION
        }
    private val _isTargetNoteUserDefined = MutableLiveData(false)
    val isTargetNoteUserDefined: LiveData<Boolean>
        get() = _isTargetNoteUserDefined

    private val frequencyPlotRangeValues = floatArrayOf(400f, 500f)
    private val _frequencyPlotRange = MutableLiveData(frequencyPlotRangeValues)
    val frequencyPlotRange: LiveData<FloatArray>
        get() = _frequencyPlotRange

//    // no test function
//    private val testFunction = null
//    // test function, which avoids excessive large times
//    private val testFunction = { frame: Int, dt: Float ->
//        val freqApprox = 660f
//        val numSteps = (1 / freqApprox / dt).roundToInt()
//        val freq = 1 / (numSteps * dt)
//        val frameMod = frame - (frame / numSteps) * numSteps
//        sin(frameMod * dt * 2 * kotlin.math.PI.toFloat() * freq)
//    }
//
//    // constant frequency test function, but suffers from inaccuracies at large times
//    private val testFunction = { frame: Int, dt: Float ->
//        val freq = 660f
//        sin(frame * dt * 2 * kotlin.math.PI.toFloat() * freq)
//    }

    // test function with increasing frequency
    private val testFunction = { frame: Int, dt: Float ->
        val freq = 200f + 2 * frame * dt
        sin(frame * dt * 2 * kotlin.math.PI.toFloat() * freq)
    }

//    private val testFunction = { t: Float ->
//        val freq = 440f
//        sin(t * 2 * kotlin.math.PI.toFloat() * freq)
//        //800f * Random.nextFloat()
//        // 1f
//    }

    init {
//        Log.v("TestRecordFlow", "TunerViewModel.init: application: $application")

//        sampleSource.testFunction = { t ->
//            val freq = 400 + 2*t
//            //val freq = 200 + 0.6f*t
//            //val freq = 496.68f
//           //Log.v("TestRecordFlow", "TunerViewModel.testfunction: f=$freq")
//            sin(t * 2 * kotlin.math.PI.toFloat() * freq)
//        }
//        sampleSource.testFunction = { t ->
//            val freq = 440f
//            sin(t * 2 * kotlin.math.PI.toFloat() * freq)
//            //800f * Random.nextFloat()
//            //1f
//        }

//        pref.registerOnSharedPreferenceChangeListener(onPreferenceChangedListener)
//        loadSettingsFromSharedPreferences()

//        sampleSource.settingsChangedListener = SoundSource.SettingsChangedListener { sampleRate, windowSize, overlap ->
//            _pitchHistoryUpdateInterval.value = windowSize.toFloat() * (1f - overlap) / sampleRate
//        }

        changeTargetNoteSettings(musicalScale = musicalScaleValue)

        viewModelScope.launch { pref.overlap.collect { overlap ->
            _pitchHistoryUpdateInterval.value = computePitchHistoryUpdateInterval(overlap = overlap)
            pitchHistory.size = computePitchHistorySize(overlap = overlap)
            restartSamplingIfRunning()
        } }

        viewModelScope.launch { pref.windowSize.collect { windowSize ->
            pitchHistory.size = computePitchHistorySize(windowSize = windowSize)
            restartSamplingIfRunning()
        } }

        viewModelScope.launch { pref.temperamentAndReferenceNote.collect {
            changeMusicalScale(
                it.rootNote,
                it.referenceNote,
                it.referenceFrequency.toFloatOrNull() ?: DefaultValues.REFERENCE_FREQUENCY,
                it.temperamentType
            )
        }}

        viewModelScope.launch {
            pref.pitchHistoryMaxNumFaultyValues.collect { pitchHistory.maxNumFaultyValues = it
        }}

        viewModelScope.launch {
            pref.toleranceInCents.collect { changeTargetNoteSettings(tolerance = it)
        }}

        viewModelScope.launch { pref.waveWriterDurationInSeconds.collect { durationInSeconds ->
            val size = sampleRate * durationInSeconds
            waveWriter.setBufferSize(size)
            restartSamplingIfRunning()
        }}

//        viewModelScope.launch {
//            sampleSource.flow
//                .buffer()
//                .transform {
//                    val result = correlationAndSpectrumComputer.run(it, pref.windowing.value)
//                    result.noise = if (result.correlation.size > 1) 1f - result.correlation[1] / result.correlation[0] else 1f
//
//                    _standardDeviation.value = withContext(Dispatchers.Default) {
//                        val average = it.data.average().toFloat()
//                        sqrt(it.data.fold(0f) {sum, element -> sum + (element - average).pow(2)}/ it.data.size)
//                    }
//                    sampleSource.recycle(it)
//                    emit(result)
//                }
//                .buffer()
//                .transform {
//                    withContext(Dispatchers.Default) {
//                        it.correlationMaximaIndices =
//                            determineCorrelationMaxima(it.correlation, 25f, 5000f, it.dt)
//                    }
//                    emit(it)
//                }
//                .transform {
//                    withContext(Dispatchers.Default) {
//                        it.specMaximaIndices =
//                            determineSpectrumMaxima(it.ampSqrSpec, 25f, 5000f, it.dt, 10f)
//                    }
//                    emit(it)
//                }
//                .buffer()
//                .transform {
//                    it.pitchFrequency = pitchChooserAndAccuracyIncreaser.run(it, if (pref.useHint.value) pitchHistory.history.value?.lastOrNull() else null)
//                    emit(it)
//                }
//                .buffer()
//                .collect {
//                    it.pitchFrequency?.let {pitchFrequency ->
//                        val resultsFromLiveData = _tunerResults.value
//                        val results = if (resultsFromLiveData != null && resultsFromLiveData.size == it.size && resultsFromLiveData.sampleRate == it.sampleRate)
//                            resultsFromLiveData
//                        else
//                            TunerResults(it.size, it.sampleRate)
//                        results.set(it)
//                        _tunerResults.value = results
//                        if (pitchFrequency > 0.0f) {
//                            pitchHistory.appendValue(pitchFrequency, it.noise)
//                            if (userDefinedTargetNote == null) { // null -> AUTOMATIC_TARGET_NOTE_DETECTION
//                                pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
//                                    val oldTargetNote = targetNoteValue.note
//                                    targetNoteValue.setTargetNoteBasedOnFrequency(frequency)
//                                    if (targetNoteValue.note != oldTargetNote)
//                                        _targetNote.value = targetNoteValue
//                                }
//                                //changeTargetNoteSettings(toneIndex = pitchHistory.currentEstimatedToneIndex)
//                            }
//
//                            pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
//                                updateFrequencyPlotRange(targetNoteValue.note, frequency)
//                            }
//                        }
//                    }
//                    correlationAndSpectrumComputer.recycle(it)
//                }
//        }
    }

    fun collectNoteDetectionResults(resultMemory: MemoryPool<CollectedResults>.RefCountedMemory) {
        val results = resultMemory.memory
        if (results.frequency > 0.0f) {
            pitchHistory.appendValue(results.frequency, results.noise)
            if (userDefinedTargetNote == null) { // null -> AUTOMATIC_TARGET_NOTE_DETECTION
                pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
                    val oldTargetNote = targetNoteValue.note
                    targetNoteValue.setTargetNoteBasedOnFrequency(frequency)
                    if (targetNoteValue.note != oldTargetNote)
                        _targetNote.value = targetNoteValue
                }
                //changeTargetNoteSettings(toneIndex = pitchHistory.currentEstimatedToneIndex)
            }

            pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
                updateFrequencyPlotRange(targetNoteValue.note, frequency)
            }
        }
        noteDetectionResults.value?.decRef()
        _noteDetectionResults.value = resultMemory
    }

    fun restartSamplingIfRunning() {
        if (noteDetectionJob != null) {
            stopSampling()
            startSampling()
        }
    }

    fun startSampling() {
        noteDetectionJob?.cancel()
        noteDetectionJob = viewModelScope.launch(Dispatchers.Main) {
            noteDetectionFlow(
                overlap = pref.overlap.value,
                windowSize = pref.windowSize.value,
                sampleRate = DefaultValues.SAMPLE_RATE,
                testFunction = testFunction,
                waveWriter = if (pref.waveWriterDurationInSeconds.value == 0) null else waveWriter,
                frequencyMin = DefaultValues.FREQUENCY_MIN,
                frequencyMax = DefaultValues.FREQUENCY_MAX,
                subharmonicsTolerance = 0.05f,
                subharmonicsPeakRatio = 0.9f,
                harmonicTolerance = 0.1f,
                minimumFactorOverLocalMean = 5f,
                maxGapBetweenHarmonics = 10,
                windowType = pref.windowing.value,
                acousticWeighting = AcousticZeroWeighting() //AcousticCWeighting()
            ).collect {
                collectNoteDetectionResults(it)
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
        noteDetectionJob?.cancel()
        noteDetectionJob = null
    }

    /** Set a new target note and string index.
     *  @param stringIndex Index of string to highlight in string view or -1 for no string-based
     *    highlighting.
     *  @param note Target note, or note == null for automatic note detection.
     * */
    fun setTargetNote(stringIndex: Int, note: MusicalNote?) {
//        Log.v("Tuner", "TunerViewModel.setTargetNote: stringIndex=$stringIndex")
        val oldTargetNote = targetNoteValue.note
        val oldStringIndex = selectedStringIndex
        selectedStringIndex = stringIndex

        if (note == null) { // -> AUTOMATIC_TARGET_NOTE_DETECTION
            userDefinedTargetNote = null
            val frequency = pitchHistory.historyAveraged.value?.lastOrNull()
            targetNoteValue.setTargetNoteBasedOnFrequency(frequency, true)
        } else {
            userDefinedTargetNote = note
            targetNoteValue.setNoteExplicitly(note)
            //changeTargetNoteSettings(toneIndex = toneIndex)
        }

        if (targetNoteValue.note != oldTargetNote) {
            pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
                updateFrequencyPlotRange(targetNoteValue.note, frequency)
            }
        }

        if (targetNoteValue.note != oldTargetNote || stringIndex != oldStringIndex)
            _targetNote.value = targetNoteValue
    }

    fun setInstrument(instrument: Instrument) {
//        Log.v("Tuner", "TunerViewModel.setInstrument $instrument, before: ${targetNoteValue.instrument}")
        // val oldTargetNote = targetNoteValue.toneIndex
        if (targetNoteValue.instrument.stableId != instrument.stableId) {
            //Log.v("Tuner", "TunerViewModel.setInstrument ...")
            targetNoteValue.instrument = instrument
            setTargetNote(-1, null)
        }
//        userDefinedTargetNoteIndex = AUTOMATIC_TARGET_NOTE_DETECTION
//        pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
//            updateFrequencyPlotRange(targetNoteValue.toneIndex, frequency)
//        }
//        _targetNote.value = targetNoteValue
//        if (oldTargetNote != targetNoteValue.toneIndex) { // changing instrument can change target note
//            pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
//                updateFrequencyPlotRange(targetNoteValue.toneIndex, frequency)
//            }
//            _targetNote.value = targetNoteValue
//        }
    }

//    fun setNoteNames(noteNames: NoteNames) {
//        _noteNames.value = noteNames
//    }

    private fun computePitchHistoryUpdateInterval(
        windowSize: Int = pref.windowSize.value,
        overlap: Float = pref.overlap.value,
        sampleRate: Int = this.sampleRate
    ) = windowSize * (1f - overlap) / sampleRate

    /** Compute number of samples to be stored in pitch history. */
    private fun computePitchHistorySize(
        duration: Float = pref.pitchHistoryDuration.value,
        sampleRate: Int = this.sampleRate,
        windowSize: Int = pref.windowSize.value,
        overlap: Float = pref.overlap.value
    ) = pitchHistoryDurationToPitchSamples(duration, sampleRate, windowSize, overlap)

    private fun updateFrequencyPlotRange(targetNote: MusicalNote, currentFrequency: Float) {
        val minOld = frequencyPlotRangeValues[0]
        val maxOld = frequencyPlotRangeValues[1]

        val targetNoteIndex = musicalScaleValue.getNoteIndex(targetNote)
        val frequencyToneIndex = musicalScaleValue.getClosestNoteIndex(currentFrequency)
        val minIndex = min(frequencyToneIndex - 0.55f, targetNoteIndex - 1.55f)
        val maxIndex = max(frequencyToneIndex + 0.55f, targetNoteIndex + 1.55f)
        frequencyPlotRangeValues[0] = musicalScaleValue.getNoteFrequency(minIndex)
        frequencyPlotRangeValues[1] = musicalScaleValue.getNoteFrequency(maxIndex)
        if (frequencyPlotRangeValues[0] != minOld || frequencyPlotRangeValues[1] != maxOld)
            _frequencyPlotRange.value = frequencyPlotRangeValues
    }

    override fun onCleared() {
//        Log.v("Tuner", "TunerViewModel.onCleared")
        stopSampling()
//        pref.unregisterOnSharedPreferenceChangeListener(onPreferenceChangedListener)

        super.onCleared()
    }

    private fun changeTargetNoteSettings(tolerance: Int = NO_NEW_TOLERANCE,
                                         musicalScale: MusicalScale? = null
    ) {
        var changed = false
        if (tolerance != NO_NEW_TOLERANCE && tolerance != targetNoteValue.toleranceInCents) {
            targetNoteValue.toleranceInCents = tolerance
            changed = true
        }
        if (musicalScale != null) {
            targetNoteValue.musicalScale = musicalScale
            changed = true
        }

        if (changed) {
            _targetNote.value = targetNoteValue
            pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
                updateFrequencyPlotRange(targetNoteValue.note, frequency)
            }
        }
    }

    private fun changeMusicalScale(rootNote: MusicalNote? = null, referenceNote: MusicalNote? = null,
                                   referenceFrequency: Float? = null, temperamentType: TemperamentType? = null) {
//        Log.v("Tuner", "TunerViewModel.changeMusicalScale")
        val temperamentTypeResolved = temperamentType ?: musicalScaleValue.temperamentType
        val rootNoteResolved = rootNote ?: musicalScaleValue.rootNote
        val referenceNoteResolved = referenceNote ?: musicalScaleValue.referenceNote
        val referenceFrequencyResolved = referenceFrequency ?: musicalScaleValue.referenceFrequency

        val newNoteNameScale = NoteNameScaleFactory.create(temperamentTypeResolved)
        // get a reference note, which fits the new note name scale
        // WARNING: we can get out of sync with the reference note preference here, so this is only
        //   to avoid undefined states. We expect, that in case we change the scale and a reference
        //   note change is needed, this will be handled by explicitly setting a new reference note.
        val referenceNoteInNewNoteNameScale = newNoteNameScale.getClosestNote(referenceNoteResolved, musicalScaleValue.noteNameScale)

        musicalScaleValue = MusicalScaleFactory.create(
            temperamentTypeResolved,
            newNoteNameScale,
            referenceNoteInNewNoteNameScale,
            rootNoteResolved,
            referenceFrequencyResolved
        )
        _musicalScale.value = musicalScaleValue
    }

    companion object {
        const val NO_NEW_TOLERANCE = Int.MAX_VALUE
    }
}
