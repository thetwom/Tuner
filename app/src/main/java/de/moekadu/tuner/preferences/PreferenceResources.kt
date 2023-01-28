package de.moekadu.tuner.preferences

import android.content.SharedPreferences
import de.moekadu.tuner.fragments.indexToTolerance
import de.moekadu.tuner.fragments.indexToWindowSize
import de.moekadu.tuner.fragments.nightModeStringToID
import de.moekadu.tuner.notedetection.WindowingFunction
import de.moekadu.tuner.notedetection.percentToPitchHistoryDuration
import de.moekadu.tuner.temperaments.MusicalNotePrintOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PreferenceResources(private val sharedPreferences: SharedPreferences, scope: CoroutineScope) {

    private val _appearance = MutableStateFlow(obtainAppearance())
    val appearance: StateFlow<AppearancePreference.Value> get() = _appearance
    private val _language = MutableStateFlow(obtainLanguage())
    val language = _language.asStateFlow()
    private val _windowing = MutableStateFlow(obtainWindowing())
    val windowing: StateFlow<WindowingFunction> get() = _windowing
    private val _overlap = MutableStateFlow(obtainOverlap())
    val overlap: StateFlow<Float> get() = _overlap
    private val _windowSize = MutableStateFlow(obtainWindowSize())
    val windowSize: StateFlow<Int> get() = _windowSize
    private val _pitchHistoryDuration = MutableStateFlow(obtainPitchHistoryDuration())
    val pitchHistoryDuration: StateFlow<Float> get() = _pitchHistoryDuration
    private val _pitchHistoryMaxNumFaultyValues = MutableStateFlow(obtainPitchHistoryNumFaultyValues())
    val pitchHistoryMaxNumFaultyValues: StateFlow<Int> get() = _pitchHistoryMaxNumFaultyValues
    private val _useHint = MutableStateFlow(obtainUseHint())
    val useHint: StateFlow<Boolean> get() = _useHint
    private val _numMovingAverage = MutableStateFlow(obtainNumMovingAverage())
    val numMovingAverage: StateFlow<Int> get() = _numMovingAverage
    private val _maxNoise = MutableStateFlow(obtainMaxNoise())
    val maxNoise: StateFlow<Float> get() = _maxNoise
    private val _toleranceInCents = MutableStateFlow(obtainToleranceInCents())
    val toleranceInCents: StateFlow<Int> get() = _toleranceInCents
    private val _waveWriterDurationInSeconds = MutableStateFlow(obtainWaveWriterDurationInSeconds())
    val waveWriterDurationInSeconds: StateFlow<Int> get() = _waveWriterDurationInSeconds
    private val _notePrintOptions = MutableStateFlow(obtainNotePrintOptions())
    val notePrintOptions: StateFlow<MusicalNotePrintOptions> get() = _notePrintOptions
    private val _temperamentAndReferenceNote = MutableStateFlow(obtainTemperamentAndReferenceNote())
    val temperamentAndReferenceNote: StateFlow<TemperamentAndReferenceNoteValue> get() = _temperamentAndReferenceNote

    // we must store this explicitly outside the callback flow since otherwise this will be
    // garbage collection, see docs:
    // https://developer.android.com/reference/android/content/SharedPreferences.html#registerOnSharedPreferenceChangeListener(android.content.SharedPreferences.OnSharedPreferenceChangeListener)
    lateinit var onSharedPreferenceChangedListener: SharedPreferences.OnSharedPreferenceChangeListener
    init {

        val sharedPrefFlow = callbackFlow {
            onSharedPreferenceChangedListener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
//                    Log.v("Tuner", "PreferenceResources : callbackFlow: key changed: $key")
                    if (key != null)
                        trySend(key)
                }
            sharedPreferences.registerOnSharedPreferenceChangeListener(
                onSharedPreferenceChangedListener
            )
            awaitClose {
//                Log.v("Tuner", "PreferenceResources sharedPrefFlow closed")
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(
                    onSharedPreferenceChangedListener
                )
            }
        }

        scope.launch {
            sharedPrefFlow.buffer(Channel.CONFLATED).collect { key ->
                when (key) {
                    APPEARANCE_KEY -> _appearance.value = obtainAppearance()
                    LANGUAGE_KEY -> _language.value = obtainLanguage()
                    WINDOWING_KEY -> _windowing.value = obtainWindowing()
                    OVERLAP_KEY -> _overlap.value = obtainOverlap()
                    PITCH_HISTORY_DURATION_KEY -> _pitchHistoryDuration.value = obtainPitchHistoryDuration()
                    PITCH_HISTORY_NUM_FAULTY_VALUES_KEY -> _pitchHistoryMaxNumFaultyValues.value = obtainPitchHistoryNumFaultyValues()
                    USE_HINT_KEY -> _useHint.value = obtainUseHint()
                    NUM_MOVING_AVERAGE_KEY -> _numMovingAverage.value = obtainNumMovingAverage()
                    MAX_NOISE_KEY -> _maxNoise.value = obtainMaxNoise()
                    TOLERANCE_IN_CENTS_KEY -> _toleranceInCents.value = obtainToleranceInCents()
                    WAVE_WRITER_DURATION_IN_SECONDS -> _waveWriterDurationInSeconds.value = obtainWaveWriterDurationInSeconds()
                    PREFER_FLAT_KEY -> _notePrintOptions.value = obtainNotePrintOptions()
                    SOLFEGE_KEY -> _notePrintOptions.value = obtainNotePrintOptions()
                    TemperamentAndReferenceNoteValue.TEMPERAMENT_AND_REFERENCE_NOTE_PREFERENCE_KEY ->
                        _temperamentAndReferenceNote.value = obtainTemperamentAndReferenceNote()
                }
            }
        }
    }

    private fun obtainAppearance(): AppearancePreference.Value {
        val value = AppearancePreference.Value(nightModeStringToID("auto"),
            blackNightEnabled = false,
            useSystemColorAccents = true
        )
        value.fromString(sharedPreferences.getString(APPEARANCE_KEY, ""))
        //Log.v("Tuner", "PreferenceResources.obtainAppearance: value: ${sharedPreferences.getString(APPEARANCE_KEY, "")}, $value")
        return value
    }

    private fun obtainLanguage(): String {
        return sharedPreferences.getString(LANGUAGE_KEY, null) ?: "en"
    }

    private fun obtainNotePrintOptions(): MusicalNotePrintOptions {
        val preferFlat = sharedPreferences.getBoolean(PREFER_FLAT_KEY, false)
        val useSolfege = sharedPreferences.getBoolean(SOLFEGE_KEY, false)

        return if (preferFlat) {
            if (useSolfege)
                MusicalNotePrintOptions.SolfegePreferFlat
            else
                MusicalNotePrintOptions.PreferFlat
        } else {
            if (useSolfege)
                MusicalNotePrintOptions.SolfegePreferSharp
            else
                MusicalNotePrintOptions.PreferSharp
        }
    }

    private fun obtainWindowing(): WindowingFunction {
        return when (sharedPreferences.getString(WINDOWING_KEY, null)) {
            "no_window" -> WindowingFunction.Tophat
            null, "window_hamming" -> WindowingFunction.Hamming
            "window_hann" -> WindowingFunction.Hann
            else -> throw RuntimeException("Unknown window")
        }
    }
    private fun obtainOverlap() = sharedPreferences.getInt(OVERLAP_KEY, 25) / 100f
    private fun obtainWindowSize() = indexToWindowSize(sharedPreferences.getInt(WINDOW_SIZE_KEY, 5))
    private fun obtainPitchHistoryDuration() = percentToPitchHistoryDuration(sharedPreferences.getInt(
        PITCH_HISTORY_DURATION_KEY, 50))
    private fun obtainPitchHistoryNumFaultyValues() = sharedPreferences.getInt(
        PITCH_HISTORY_NUM_FAULTY_VALUES_KEY, 3)
    private fun obtainUseHint() = sharedPreferences.getBoolean(USE_HINT_KEY, true)
    private fun obtainNumMovingAverage() = sharedPreferences.getInt(NUM_MOVING_AVERAGE_KEY, 5)
    private fun obtainMaxNoise() = sharedPreferences.getInt(MAX_NOISE_KEY, 10) / 100f
    private fun obtainToleranceInCents() = indexToTolerance(sharedPreferences.getInt(
        TOLERANCE_IN_CENTS_KEY, 3))
    private fun obtainWaveWriterDurationInSeconds() = sharedPreferences.getInt(
        WAVE_WRITER_DURATION_IN_SECONDS, 0)
    private fun obtainTemperamentAndReferenceNote() = TemperamentAndReferenceNoteValue.fromSharedPreferences(sharedPreferences)

    companion object {
        const val APPEARANCE_KEY = "appearance"
        const val LANGUAGE_KEY = "language"
        const val PREFER_FLAT_KEY = "prefer_flat"
        const val SOLFEGE_KEY = "solfege"
        const val WINDOWING_KEY = "windowing"
        const val OVERLAP_KEY = "overlap"
        const val WINDOW_SIZE_KEY = "window_size"
        const val PITCH_HISTORY_DURATION_KEY = "pitch_history_duration"
        const val PITCH_HISTORY_NUM_FAULTY_VALUES_KEY = "pitch_history_num_faulty_values"
        const val USE_HINT_KEY = "use_hint"
        const val NUM_MOVING_AVERAGE_KEY = "num_moving_average"
        const val MAX_NOISE_KEY = "max_noise"
        const val TOLERANCE_IN_CENTS_KEY = "tolerance_in_cents"
        const val WAVE_WRITER_DURATION_IN_SECONDS = "wave_writer_duration_in_seconds"
    }
}