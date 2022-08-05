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
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import de.moekadu.tuner.fragments.indexToTolerance
import de.moekadu.tuner.fragments.indexToWindowSize
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.instrumentDatabase
import de.moekadu.tuner.misc.SoundSource
import de.moekadu.tuner.notedetection.*
import de.moekadu.tuner.preferences.TemperamentAndReferenceNoteValue
import de.moekadu.tuner.temperaments.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

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
//                Log.v("Tuner", "TunerViewModel.pitchHistoryDuration: new duration = $value, new size = $pitchHistorySize")
                pitchHistory.size = pitchHistorySize
            }
        }

    /// Compute number of samples to be stored in pitch history.
    private val pitchHistorySize
        get() = pitchHistoryDurationToPitchSamples(
            pitchHistoryDuration, sampleSource.sampleRate, windowSize, overlap)

//    var a4Frequency = 440f
//        set(value) {
//            if (field != value) {
//                field = value
//                temperamentFrequencyValues = TemperamentEqualTemperament(numNotesPerOctave = 12, noteIndexAtReferenceFrequency = 0, referenceFrequency = value)
//            }
//        }

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

    /// Duration in seconds between two updates for the pitch history
    private val _pitchHistoryUpdateInterval = MutableLiveData(windowSize.toFloat() * (1f - overlap) / sampleSource.sampleRate)
    val pitchHistoryUpdateInterval: LiveData<Float> = _pitchHistoryUpdateInterval

    private val _preferFlat = MutableLiveData(false)
    val preferFlat: LiveData<Boolean> get() = _preferFlat
    val notePrintOptions get() = if (preferFlat.value == true) MusicalNotePrintOptions.PreferFlat else MusicalNotePrintOptions.PreferSharp

    private var musicalScaleValue: MusicalScale =
        MusicalScaleFactory.create(TemperamentType.EDO12, null, null, 440f)
        set(value) {
            field = value
            pitchHistory.musicalScale = value
            changeTargetNoteSettings(musicalScale = value)
            _musicalScale.value = value
        }

//    private var _instrument = MutableLiveData<Instrument>().apply { value = instrumentDatabase[1] }
//    val instrument: LiveData<Instrument>
//        get() = _instrument

    private val _musicalScale = MutableLiveData<MusicalScale>().apply { value = musicalScaleValue }
    val musicalScale: LiveData<MusicalScale>
        get() = _musicalScale

//    private val _noteNames = MutableLiveData<NoteNames>().apply { value = noteNames12Tone }
//    val noteNames: LiveData<NoteNames> get() = _noteNames

    private val _standardDeviation = MutableLiveData(0f)
    val standardDeviation: LiveData<Float> get() = _standardDeviation

    val pitchHistory = PitchHistory(pitchHistorySize, musicalScaleValue)

    private val correlationAndSpectrumComputer = CorrelationAndSpectrumComputer()
    private val pitchChooserAndAccuracyIncreaser = PitchChooserAndAccuracyIncreaser()

    var windowingFunction = WindowingFunction.Hamming

    var useHint = true

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

    private val pref = PreferenceManager.getDefaultSharedPreferences(application)
    
    private val onPreferenceChangedListener = object : SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (sharedPreferences == null)
                return
//            Log.v("Tuner", "TunerViewModel.setupPreferenceListener: key=$key")
            when (key) {
                TemperamentAndReferenceNoteValue.TEMPERAMENT_AND_REFERENCE_NOTE_PREFERENCE_KEY -> {
                    val valueString = sharedPreferences.getString(
                        TemperamentAndReferenceNoteValue.TEMPERAMENT_AND_REFERENCE_NOTE_PREFERENCE_KEY, null)
                    val value = TemperamentAndReferenceNoteValue.fromString(valueString)
                    changeMusicalScale(
                        rootNote = value?.rootNote,
                        referenceNote = value?.referenceNote,
                        referenceFrequency = value?.referenceFrequency?.toFloatOrNull() ?: 440f, // TODO: should we use a globally defined default value?
                        temperamentType = value?.temperamentType
                    )
//                    Log.v("Tuner", "TunerViewModel.setupPreferenceListener: temperament and root note changed: $valueString")
                }
                "prefer_flat" -> {
                    _preferFlat.value = sharedPreferences.getBoolean(key, false)
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
//        Log.v("TestRecordFlow", "TunerViewModel.init: application: $application")

//        sampleSource.testFunction = { t ->
//            //val freq = 400 + 2*t
//            val freq = 200 + 0.6f*t
//            //val freq = 440
//           //Log.v("TestRecordFlow", "TunerViewModel.testfunction: f=$freq")
//            sin(t * 2 * kotlin.math.PI.toFloat() * freq)
//        }
//        sampleSource.testFunction = { t ->
//            val freq = 440f
//            sin(t * 2 * kotlin.math.PI.toFloat() * freq)
//            //800f * Random.nextFloat()
//            //1f
//        }

        pref.registerOnSharedPreferenceChangeListener(onPreferenceChangedListener)
        loadSettingsFromSharedPreferences()

        sampleSource.settingsChangedListener = SoundSource.SettingsChangedListener { sampleRate, windowSize, overlap ->
            _pitchHistoryUpdateInterval.value = windowSize.toFloat() * (1f - overlap) / sampleRate
        }

        changeTargetNoteSettings(musicalScale = musicalScaleValue)

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
                    }
                    correlationAndSpectrumComputer.recycle(it)
                }
        }
    }

    fun startSampling() {
        //Log.v("Tuner", "TunerViewModel.startSampling")
        sampleSource.restartSampling()
    }

    fun stopSampling() {
//        Log.v("Tuner", "TunerViewModel.stopSampling")
        sampleSource.stopSampling()
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
        pref.unregisterOnSharedPreferenceChangeListener(onPreferenceChangedListener)

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

    private fun loadSettingsFromSharedPreferences() {
        val temperamentAndReferenceNote = TemperamentAndReferenceNoteValue.fromSharedPreferences(pref)
        _preferFlat.value = pref.getBoolean("prefer_flat", false)
        changeMusicalScale(
            temperamentType = temperamentAndReferenceNote.temperamentType,
            rootNote = temperamentAndReferenceNote.rootNote,
            referenceNote = temperamentAndReferenceNote.referenceNote,
            referenceFrequency = temperamentAndReferenceNote.referenceFrequency.toFloatOrNull() ?: 440f
        )

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
        const val NO_NEW_TOLERANCE = Int.MAX_VALUE
    }
}
