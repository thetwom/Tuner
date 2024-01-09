package de.moekadu.tuner.preferences

import android.content.Context
import android.content.SharedPreferences
import de.moekadu.tuner.fragments.indexToTolerance
import de.moekadu.tuner.fragments.indexToWindowSize
import de.moekadu.tuner.fragments.nightModeStringToID
import de.moekadu.tuner.fragments.percentToPitchHistoryDuration
import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.notedetection.WindowingFunction
import de.moekadu.tuner.temperaments.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class PreferenceResources(
    private val context: Context,
    private val sharedPreferences:
    SharedPreferences, scope: CoroutineScope
) {

    private val _appearance = MutableStateFlow(obtainAppearance())
    val appearance = _appearance.asStateFlow()
    private val _screenAlwaysOn = MutableStateFlow(obtainScreenAlwaysOn())
    val screenAlwaysOn = _screenAlwaysOn.asStateFlow()
    private val _windowing = MutableStateFlow(obtainWindowing())
    val windowing = _windowing.asStateFlow()
    private val _overlap = MutableStateFlow(obtainOverlap())
    val overlap = _overlap.asStateFlow()
    private val _windowSize = MutableStateFlow(obtainWindowSize())
    val windowSize = _windowSize.asStateFlow()
    private val _pitchHistoryDuration = MutableStateFlow(obtainPitchHistoryDuration())
    val pitchHistoryDuration = _pitchHistoryDuration.asStateFlow()
    private val _numMovingAverage = MutableStateFlow(obtainNumMovingAverage())
    val numMovingAverage = _numMovingAverage.asStateFlow()
    private val _maxNoise = MutableStateFlow(obtainMaxNoise())
    val maxNoise = _maxNoise.asStateFlow()
    private val _minHarmonicEnergyContent = MutableStateFlow(obtainMinHarmonicEnergyContent())
    val minHarmonicEnergyContent = _minHarmonicEnergyContent.asStateFlow()
    private val _sensitivity = MutableStateFlow(obtainSensitivity())
    val sensitivity = _sensitivity.asStateFlow()
    private val _pitchHistoryMaxNumFaultyValues = MutableStateFlow(obtainPitchHistoryNumFaultyValues())
    val pitchHistoryMaxNumFaultyValues = _pitchHistoryMaxNumFaultyValues.asStateFlow()
    private val _toleranceInCents = MutableStateFlow(obtainToleranceInCents())
    val toleranceInCents = _toleranceInCents.asStateFlow()
    private val _waveWriterDurationInSeconds = MutableStateFlow(obtainWaveWriterDurationInSeconds())
    val waveWriterDurationInSeconds = _waveWriterDurationInSeconds.asStateFlow()
    private val _noteNamePrinter = MutableStateFlow(obtainNoteNamePrinter())
    val noteNamePrinter = _noteNamePrinter.asStateFlow()

    private val _musicalScale = MutableStateFlow(musicalScaleFromPreference(obtainTemperamentAndReferenceNote()))
    val musicalScale = _musicalScale.asStateFlow()
    private val _temperamentAndReferenceNote = MutableStateFlow(obtainTemperamentAndReferenceNote())
    val temperamentAndReferenceNote = _temperamentAndReferenceNote.asStateFlow()
//
//    private val _instrumentId = MutableStateFlow(obtainInstrumentId())
//    val instrumentId: StateFlow<Long> get() = _instrumentId

    // we must store this explicitly outside the callback flow since otherwise this will be
    // garbage collected, see docs:
    // https://developer.android.com/reference/android/content/SharedPreferences.html#registerOnSharedPreferenceChangeListener(android.content.SharedPreferences.OnSharedPreferenceChangeListener)

    private lateinit var onSharedPreferenceChangedListener: SharedPreferences.OnSharedPreferenceChangeListener

    init {
        runMigrations()

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
                    SCREEN_ALWAYS_ON -> _screenAlwaysOn.value = obtainScreenAlwaysOn()
                    WINDOWING_KEY -> _windowing.value = obtainWindowing()
                    OVERLAP_KEY -> _overlap.value = obtainOverlap()
                    WINDOW_SIZE_KEY -> _windowSize.value = obtainWindowSize()
                    PITCH_HISTORY_DURATION_KEY -> _pitchHistoryDuration.value =
                        obtainPitchHistoryDuration()
                    PITCH_HISTORY_NUM_FAULTY_VALUES_KEY -> _pitchHistoryMaxNumFaultyValues.value =
                        obtainPitchHistoryNumFaultyValues()
//                    USE_HINT_KEY -> _useHint.value = obtainUseHint()
                    NUM_MOVING_AVERAGE_KEY -> _numMovingAverage.value = obtainNumMovingAverage()
                    MAX_NOISE_KEY -> _maxNoise.value = obtainMaxNoise()
                    MIN_HARMONIC_ENERGY_CONTENT -> _minHarmonicEnergyContent.value = obtainMinHarmonicEnergyContent()
                    SENSITIVITY -> _sensitivity.value = obtainSensitivity()
                    TOLERANCE_IN_CENTS_KEY -> _toleranceInCents.value = obtainToleranceInCents()
                    WAVE_WRITER_DURATION_IN_SECONDS_KEY -> _waveWriterDurationInSeconds.value =
                        obtainWaveWriterDurationInSeconds()
                    PREFER_FLAT_KEY -> _noteNamePrinter.value = obtainNoteNamePrinter()
                    NOTATION_KEY -> _noteNamePrinter.value = obtainNoteNamePrinter()
                    TemperamentAndReferenceNoteValue.TEMPERAMENT_AND_REFERENCE_NOTE_PREFERENCE_KEY -> {
                        val value = obtainTemperamentAndReferenceNote()
                        _temperamentAndReferenceNote.value = value
                        _musicalScale.value = musicalScaleFromPreference(value)
                    }
//                    INSTRUMENT_ID_KEY -> _instrumentId = obtainInstrumentId()
                }
            }
        }
    }

    fun setTemperamentAndReferenceNote(value: TemperamentAndReferenceNoteValue) {
        val editor = sharedPreferences.edit()
        val newPrefsString = value.toString()
        editor.putString(
            TemperamentAndReferenceNoteValue.TEMPERAMENT_AND_REFERENCE_NOTE_PREFERENCE_KEY,
            newPrefsString
        )
        editor.apply()
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

    private fun obtainScreenAlwaysOn() = sharedPreferences.getBoolean(SCREEN_ALWAYS_ON, false)

    private fun obtainNoteNamePrinter(): NoteNamePrinter {
        val preferFlat = sharedPreferences.getBoolean(PREFER_FLAT_KEY, false)

        val notationValue = NotationPreference.Value("standard", false)
        notationValue.fromString(sharedPreferences.getString(NOTATION_KEY, ""))

        val notationType = when(notationValue.notation) {
            "international" -> NotationType.International
            "solfege" -> NotationType.Solfege
            "carnatic" -> NotationType.Carnatic
            "hindustani" -> NotationType.Hindustani
            else -> NotationType.Standard
        }

        val sharpFlatPreference = if (preferFlat)
            NoteNamePrinter.SharpFlatPreference.Flat
        else
            NoteNamePrinter.SharpFlatPreference.Sharp
        return createNoteNamePrinter(
            context,
            notationType,
            sharpFlatPreference,
            helmholtzNotation = notationValue.helmholtzEnabled
        )
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
//    private fun obtainUseHint() = sharedPreferences.getBoolean(USE_HINT_KEY, true)
    private fun obtainNumMovingAverage() = sharedPreferences.getInt(NUM_MOVING_AVERAGE_KEY, 5)

    // disable this setting for now by hardcoding a good value
    private fun obtainMaxNoise() = 0.1f// sharedPreferences.getInt(MAX_NOISE_KEY, 10) / 100f

    // disable this setting for now by hardcoding a good value
    private fun obtainMinHarmonicEnergyContent() = 0.1f // sharedPreferences.getInt(MIN_HARMONIC_ENERGY_CONTENT, 20) / 100f
    private fun obtainSensitivity() = sharedPreferences.getInt(SENSITIVITY, 90)
    private fun obtainToleranceInCents() = indexToTolerance(sharedPreferences.getInt(
        TOLERANCE_IN_CENTS_KEY, 3))
    private fun obtainWaveWriterDurationInSeconds() = sharedPreferences.getInt(
        WAVE_WRITER_DURATION_IN_SECONDS_KEY, 0)
    private fun obtainTemperamentAndReferenceNote() = TemperamentAndReferenceNoteValue.fromSharedPreferences(sharedPreferences)
    private fun musicalScaleFromPreference(value: TemperamentAndReferenceNoteValue): MusicalScale {
        return MusicalScaleFactory.create(
            value.temperamentType,
            value.referenceNote,
            value.rootNote,
            value.referenceFrequency.toFloatOrNull() ?: DefaultValues.REFERENCE_FREQUENCY
        )
    }

//    private fun obtainInstrumentId() = sharedPreferences.getLong(INSTRUMENT_ID_KEY, 0L)

    private fun runMigrations() {
        if (sharedPreferences.contains(SOLFEGE_KEY)) {
            val isSolfege = sharedPreferences.getBoolean(SOLFEGE_KEY, false)
            if (isSolfege) {
                if (noteNamePrinter.value.sharpFlatPreference == NoteNamePrinter.SharpFlatPreference.Flat)
                    _noteNamePrinter.value = createNoteNamePrinter(
                        context, NotationType.Solfege, NoteNamePrinter.SharpFlatPreference.Flat,
                        helmholtzNotation = false)
                else
                    _noteNamePrinter.value = createNoteNamePrinter(
                        context, NotationType.Solfege, NoteNamePrinter.SharpFlatPreference.Sharp,
                        helmholtzNotation = false
                    )
                sharedPreferences.edit().putString(NOTATION_KEY, "solfege").apply()
            }
            sharedPreferences.edit().remove(SOLFEGE_KEY).apply()
        }
    }

    companion object {
        const val APPEARANCE_KEY = "appearance"
        const val SCREEN_ALWAYS_ON = "screenon"
        const val PREFER_FLAT_KEY = "prefer_flat"
        const val SOLFEGE_KEY = "solfege"
        const val NOTATION_KEY = "notation"
        const val WINDOWING_KEY = "windowing"
        const val OVERLAP_KEY = "overlap"
        const val WINDOW_SIZE_KEY = "window_size"
        const val PITCH_HISTORY_DURATION_KEY = "pitch_history_duration"
        const val PITCH_HISTORY_NUM_FAULTY_VALUES_KEY = "pitch_history_num_faulty_values"
//        const val USE_HINT_KEY = "use_hint"
        const val NUM_MOVING_AVERAGE_KEY = "num_moving_average"
        const val MAX_NOISE_KEY = "max_noise"
        const val MIN_HARMONIC_ENERGY_CONTENT = "min_harmonic_energy_content"
        const val SENSITIVITY = "sensitivity"
        const val TOLERANCE_IN_CENTS_KEY = "tolerance_in_cents"
        const val WAVE_WRITER_DURATION_IN_SECONDS_KEY = "wave_writer_duration_in_seconds"
//        const val INSTRUMENT_ID_KEY = "instrument_id"
    }
}
