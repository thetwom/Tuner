package de.moekadu.tuner.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.notedetection.WindowingFunction
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.ui.notes.NotationType
import de.moekadu.tuner.ui.notes.NotePrintOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.roundToInt

@Singleton
class PreferenceResources @Inject constructor (
    @ApplicationContext private val context: Context,
    // private val sharedPreferences: SharedPreferences,
    //scope: CoroutineScope
) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    data class Appearance(
        var mode: NightMode = NightMode.Auto,  // Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        var blackNightEnabled: Boolean = true,
        var useSystemColorAccents: Boolean = true
    ) {
        private fun nightModeStringToID2(string: String) = when(string) {
            "dark" -> NightMode.On // ppCompatDelegate.MODE_NIGHT_YES
            "light" -> NightMode.Off // AppCompatDelegate.MODE_NIGHT_NO
            else -> NightMode.Auto // AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        fun fromString(string: String?) {
            if (string == null)
                return
            val values = string.split(" ")
            blackNightEnabled = values.contains("blackNightEnabled")
            useSystemColorAccents = !(values.contains("noSystemColorAccents"))
            val modeString = if (values.contains("dark"))
                "dark"
            else if (values.contains("light"))
                "light"
            else
                "auto"
            mode = nightModeStringToID2(modeString)
        }
    }

    data class NotationPreferenceValue(var notation: String, var helmholtzEnabled: Boolean) {
        override fun toString(): String {
            var result = notation
            if (helmholtzEnabled)
                result += " helmholtzEnabled"
            return result
        }

        fun fromString(string: String?) {
            if (string == null)
                return
            val values = string.split(" ")
            helmholtzEnabled = values.contains("helmholtzEnabled")
            notation = when {
                values.contains("solfege") -> "solfege"
                values.contains("international") -> "international"
                values.contains("carnatic") -> "carnatic"
                values.contains("hindustani") -> "hindustani"
                else -> "standard"
            }
        }
    }
//    val sampleRate = 44100
//    private val _appearance = MutableStateFlow(obtainAppearance())
//    val appearance = _appearance.asStateFlow()
//    private val _screenAlwaysOn = MutableStateFlow(obtainScreenAlwaysOn())
//    val screenAlwaysOn = _screenAlwaysOn.asStateFlow()
//    private val _windowing = MutableStateFlow(obtainWindowing())
//    val windowing = _windowing.asStateFlow()
//    private val _overlap = MutableStateFlow(obtainOverlap())
//    val overlap = _overlap.asStateFlow()
//    private val _windowSize = MutableStateFlow(obtainWindowSize())
//    val windowSize = _windowSize.asStateFlow()
//    private val _pitchHistoryDuration = MutableStateFlow(obtainPitchHistoryDuration())
//    val pitchHistoryDuration = _pitchHistoryDuration.asStateFlow()
//    private val _numMovingAverage = MutableStateFlow(obtainNumMovingAverage())
//    val numMovingAverage = _numMovingAverage.asStateFlow()
//    private val _maxNoise = MutableStateFlow(obtainMaxNoise())
//    val maxNoise = _maxNoise.asStateFlow()
//    private val _minHarmonicEnergyContent = MutableStateFlow(obtainMinHarmonicEnergyContent())
//    val minHarmonicEnergyContent = _minHarmonicEnergyContent.asStateFlow()
//    private val _sensitivity = MutableStateFlow(obtainSensitivity())
//    val sensitivity = _sensitivity.asStateFlow()
//    private val _pitchHistoryMaxNumFaultyValues = MutableStateFlow(obtainPitchHistoryNumFaultyValues())
//    val pitchHistoryMaxNumFaultyValues = _pitchHistoryMaxNumFaultyValues.asStateFlow()
//    private val _toleranceInCents = MutableStateFlow(obtainToleranceInCents())
//    val toleranceInCents = _toleranceInCents.asStateFlow()
//    private val _waveWriterDurationInSeconds = MutableStateFlow(obtainWaveWriterDurationInSeconds())
//    val waveWriterDurationInSeconds = _waveWriterDurationInSeconds.asStateFlow()
//    // TODO: delete noteNamePrinter
//    private val _noteNamePrinter = MutableStateFlow(obtainNoteNamePrinter())
//    val noteNamePrinter = _noteNamePrinter.asStateFlow()
//    private val _notePrintOptions = MutableStateFlow(obtainNotePrintOptions())
//    val notePrintOptions = _notePrintOptions.asStateFlow()
//
//    private val _musicalScale = MutableStateFlow(musicalScaleFromPreference(obtainTemperamentAndReferenceNote()))
//    val musicalScale = _musicalScale.asStateFlow()
//    private val _temperamentAndReferenceNote = MutableStateFlow(obtainTemperamentAndReferenceNote())
//    val temperamentAndReferenceNote = _temperamentAndReferenceNote.asStateFlow()
//
//    private val _instrumentId = MutableStateFlow(obtainInstrumentId())
//    val instrumentId: StateFlow<Long> get() = _instrumentId

    // we must store this explicitly outside the callback flow since otherwise this will be
    // garbage collected, see docs:
    // https://developer.android.com/reference/android/content/SharedPreferences.html#registerOnSharedPreferenceChangeListener(android.content.SharedPreferences.OnSharedPreferenceChangeListener)

//    private lateinit var onSharedPreferenceChangedListener: SharedPreferences.OnSharedPreferenceChangeListener

    init {
//        runMigrations()

//        val sharedPrefFlow = callbackFlow {
//            onSharedPreferenceChangedListener =
//                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
////                    Log.v("Tuner", "PreferenceResources : callbackFlow: key changed: $key")
//                    if (key != null)
//                        trySend(key)
//                }
//            sharedPreferences.registerOnSharedPreferenceChangeListener(
//                onSharedPreferenceChangedListener
//            )
//            awaitClose {
////                Log.v("Tuner", "PreferenceResources sharedPrefFlow closed")
//                sharedPreferences.unregisterOnSharedPreferenceChangeListener(
//                    onSharedPreferenceChangedListener
//                )
//            }
//        }
//
//        MainScope().launch {
//            sharedPrefFlow.buffer(Channel.CONFLATED).collect { key ->
//                when (key) {
//                    APPEARANCE_KEY -> _appearance.value = obtainAppearance()
//                    SCREEN_ALWAYS_ON -> _screenAlwaysOn.value = obtainScreenAlwaysOn()
//                    WINDOWING_KEY -> _windowing.value = obtainWindowing()
//                    OVERLAP_KEY -> _overlap.value = obtainOverlap()
//                    WINDOW_SIZE_KEY -> _windowSize.value = obtainWindowSize()
//                    PITCH_HISTORY_DURATION_KEY -> _pitchHistoryDuration.value =
//                        obtainPitchHistoryDuration()
//                    PITCH_HISTORY_NUM_FAULTY_VALUES_KEY -> _pitchHistoryMaxNumFaultyValues.value =
//                        obtainPitchHistoryNumFaultyValues()
////                    USE_HINT_KEY -> _useHint.value = obtainUseHint()
//                    NUM_MOVING_AVERAGE_KEY -> _numMovingAverage.value = obtainNumMovingAverage()
//                    MAX_NOISE_KEY -> _maxNoise.value = obtainMaxNoise()
//                    MIN_HARMONIC_ENERGY_CONTENT -> _minHarmonicEnergyContent.value = obtainMinHarmonicEnergyContent()
//                    SENSITIVITY -> _sensitivity.value = obtainSensitivity()
//                    TOLERANCE_IN_CENTS_KEY -> _toleranceInCents.value = obtainToleranceInCents()
//                    WAVE_WRITER_DURATION_IN_SECONDS_KEY -> _waveWriterDurationInSeconds.value =
//                        obtainWaveWriterDurationInSeconds()
//                    PREFER_FLAT_KEY -> {
//                        _noteNamePrinter.value = obtainNoteNamePrinter()
//                        _notePrintOptions.value = obtainNotePrintOptions()
//                    }
//                    NOTATION_KEY -> {
//                        _noteNamePrinter.value = obtainNoteNamePrinter()
//                        _notePrintOptions.value = obtainNotePrintOptions()
//                    }
//                    TemperamentAndReferenceNoteValue.TEMPERAMENT_AND_REFERENCE_NOTE_PREFERENCE_KEY -> {
//                        val value = obtainTemperamentAndReferenceNote()
//                        _temperamentAndReferenceNote.value = value
//                        _musicalScale.value = musicalScaleFromPreference(value)
//                    }
////                    INSTRUMENT_ID_KEY -> _instrumentId = obtainInstrumentId()
//                }
//            }
//        }
    }

//    fun setTemperamentAndReferenceNote(value: TemperamentAndReferenceNoteValue) {
//        val editor = sharedPreferences.edit()
//        val newPrefsString = value.toString()
//        editor.putString(
//            TemperamentAndReferenceNoteValue.TEMPERAMENT_AND_REFERENCE_NOTE_PREFERENCE_KEY,
//            newPrefsString
//        )
//        editor.apply()
//    }

    val appearance get() = getString(APPEARANCE_KEY)?.let{
        val value = Appearance()
        value.fromString(sharedPreferences.getString(APPEARANCE_KEY, ""))
        value
    }

    val scientificMode get() = getBoolean(SCIENTIFIC_KEY)

    val screenAlwaysOn get() = getBoolean(SCREEN_ALWAYS_ON)
    private val preferFlat get() = getBoolean(PREFER_FLAT_KEY)
    private val notationPreference get() =  getString(NOTATION_KEY)?.let {
        val notationValue = NotationPreferenceValue("standard", false)
        notationValue.fromString(sharedPreferences.getString(NOTATION_KEY, ""))
        notationValue
    }
    val notePrintOptions: NotePrintOptions? get() {
        if (preferFlat == null && notationPreference == null)
            return null
        val pF = preferFlat
        val nP = notationPreference
        val def = NotePrintOptions()
        val nT = when(nP?.notation) {
            "international" -> NotationType.International
            "solfege" -> NotationType.Solfege
            "carnatic" -> NotationType.Carnatic
            "hindustani" -> NotationType.Hindustani
            else -> NotationType.Standard
        }
        val hH = nP?.helmholtzEnabled ?: def.helmholtzNotation
        val sharpFlatPreference = if (pF == null)
            def.sharpFlatPreference
        else if (pF == true)
            NotePrintOptions.SharpFlatPreference.Flat
        else
            NotePrintOptions.SharpFlatPreference.Sharp
        return NotePrintOptions(sharpFlatPreference, hH, nT)
    }

    val windowing get() = getString(WINDOWING_KEY)?.let {
        when (sharedPreferences.getString(WINDOWING_KEY, null)) {
            "no_window" -> WindowingFunction.Tophat
            "window_hamming" -> WindowingFunction.Hamming
            "window_hann" -> WindowingFunction.Hann
            else -> null
        }
    }
    val overlap get() = getInt(OVERLAP_KEY)

    val windowSizeExponent get() = getInt(WINDOW_SIZE_KEY)?.let {
        it + 7
        //indexToWindowSize2(it)
    }
    val pitchHistoryDuration get() = getInt(PITCH_HISTORY_DURATION_KEY)?.let{
        percentToPitchHistoryDuration2(it)
    }
    val pitchHistoryNumFaultyValues get() = getInt(PITCH_HISTORY_NUM_FAULTY_VALUES_KEY)
    val numMovingAverage get() = getInt(NUM_MOVING_AVERAGE_KEY)

    // disable this setting for now by hardcoding a good value
    // fun obtainMaxNoise() = 0.1f// sharedPreferences.getInt(MAX_NOISE_KEY, 10) / 100f

    // disable this setting for now by hardcoding a good value
    // fun obtainMinHarmonicEnergyContent() = 0.1f // sharedPreferences.getInt(MIN_HARMONIC_ENERGY_CONTENT, 20) / 100f
    val sensitivity get() = getInt(SENSITIVITY)

    val toleranceInCents get() = getInt(TOLERANCE_IN_CENTS_KEY)?.let {
        indexToTolerance2(it)
    }

    val waveWriterDurationInSeconds get() = getInt(WAVE_WRITER_DURATION_IN_SECONDS_KEY)

    private val temperamentAndReferenceNote get() = TemperamentAndReferenceNoteFromPreference.fromSharedPreferences(sharedPreferences)
    val musicalScale: MusicalScale? get() = temperamentAndReferenceNote?.let {
            MusicalScaleFactory.create(
                it.temperamentType,
                it.referenceNote,
                it.rootNote,
                it.referenceFrequency.toFloatOrNull() ?: DefaultValues.REFERENCE_FREQUENCY
            )
        }

    private fun getBoolean(key: String) = if(sharedPreferences.contains(key))
        sharedPreferences.getBoolean(key, false)
    else
        null
    private fun getString(key: String) = if(sharedPreferences.contains(key))
        sharedPreferences.getString(key, null)
    else
        null
    private fun getInt(key: String) = if(sharedPreferences.contains(key))
        sharedPreferences.getInt(key, 0)
    else
        null

    companion object {
        const val APPEARANCE_KEY = "appearance"
        const val SCREEN_ALWAYS_ON = "screenon"
        const val PREFER_FLAT_KEY = "prefer_flat"
        const val SOLFEGE_KEY = "solfege"
        const val NOTATION_KEY = "notation"
        const val WINDOWING_KEY = "windowing"
        const val OVERLAP_KEY = "overlap"
        const val WINDOW_SIZE_KEY = "window_size"
        const val SCIENTIFIC_KEY = "scientific"
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

private data class TemperamentAndReferenceNoteFromPreference (
    val temperamentType: TemperamentType,
    val rootNote: MusicalNote,
    val referenceNote: MusicalNote,
    val referenceFrequency: String) {
    override fun toString(): String {
        return "$temperamentType ${rootNote.asString()} ${referenceNote.asString()} $referenceFrequency"
    }

    companion object {
        const val TEMPERAMENT_AND_REFERENCE_NOTE_PREFERENCE_KEY = "temperament_and_reference_note.key"

        fun fromSharedPreferences(pref: SharedPreferences): TemperamentAndReferenceNoteFromPreference? {
            if (!pref.contains(TEMPERAMENT_AND_REFERENCE_NOTE_PREFERENCE_KEY))
                return null
            return fromString(pref.getString(TEMPERAMENT_AND_REFERENCE_NOTE_PREFERENCE_KEY, null))
        }

        fun fromString(string: String?): TemperamentAndReferenceNoteFromPreference? {
            if (string == null)
                return null
            val values = string.split(" ")
            if (values.size != 4)
                return null
            val temperamentType = try {
                TemperamentType.valueOf(values[0])
            } catch (ex: IllegalArgumentException) {
                TemperamentType.EDO12
            }
            val rootNote = try {
                MusicalNote.fromString(values[1])
            } catch (ex: RuntimeException) {
                return null
            }
            val referenceNote = try {
                MusicalNote.fromString(values[2])
            } catch (ex: RuntimeException) {
                return null
            }
            val referenceFrequency = if (values[3].toFloatOrNull() == null)
                return null
            else
                values[3]
            return TemperamentAndReferenceNoteFromPreference(temperamentType, rootNote, referenceNote, referenceFrequency)
        }
    }
}

private fun indexToWindowSize2(index: Int): Int {
    return 2f.pow(7 + index).roundToInt()
}

private fun indexToTolerance2(index: Int): Int {
    return when (index) {
        0 -> 1
        1 -> 2
        2 -> 3
        3 -> 5
        4 -> 7
        5 -> 10
        6 -> 15
        7 -> 20
        else -> throw RuntimeException("Invalid index for tolerance")
    }
}

/** Compute pitch history duration in seconds based on a percent value.
 * This uses a exponential scale for setting the duration.
 * @param percent Percentage value where 0 stands for the minimum duration and 100 for the maximum.
 * @param durationAtFiftyPercent Duration in seconds at fifty percent.
 * @return Pitch history duration in seconds.
 */
private fun percentToPitchHistoryDuration2(percent: Int, durationAtFiftyPercent: Float = 3.0f) : Float {
    return durationAtFiftyPercent * 2.0f.pow(0.05f * (percent - 50))
}
