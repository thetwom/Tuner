package de.moekadu.tuner.preferences

import android.content.Context
import android.os.Parcelable
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import de.moekadu.tuner.notedetection.WindowingFunction
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.ui.notes.NotePrintOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.roundToInt

@Singleton
class PreferenceResources2 @Inject constructor (
    @ApplicationContext context: Context
) {
    private val dataStore = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() })
    ) { context.preferencesDataStoreFile("settings") }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Serializable
    data class Appearance(
        val mode: NightMode = NightMode.Auto,
        val blackNightEnabled: Boolean = false,
        val useSystemColorAccents: Boolean = true
    )

    @Serializable
    @Parcelize
    data class MusicalScaleProperties(
        val temperamentType: TemperamentType,
        val rootNote: MusicalNote,
        val referenceNote: MusicalNote,
        val referenceFrequency: Float
    ) : Parcelable {
        fun toMusicalScale() = MusicalScaleFactory.create(
            temperamentType, referenceNote, rootNote, referenceFrequency
        )
        companion object {
            fun create(musicalScale: MusicalScale) = MusicalScaleProperties(
                musicalScale.temperamentType,
                musicalScale.rootNote,
                musicalScale.referenceNote,
                musicalScale.referenceFrequency
            )
        }
    }

    val sampleRate = 44100

    // appearance
    val appearance = getSerializablePreferenceFlow(APPEARANCE_KEY, AppearanceDefault)
    suspend fun writeAppearance(appearance: Appearance) {
        writeSerializablePreference(APPEARANCE_KEY, appearance)
    }

    // keep screen on
    val screenAlwaysOn= getPreferenceFlow(SCREEN_ALWAYS_ON, ScreenAlwaysOnDefault)
    suspend fun writeScreenAlwaysOn(screenAlwaysOn: Boolean) {
        writePreference(SCREEN_ALWAYS_ON, screenAlwaysOn)
    }

    // note print options
    val notePrintOptions = getSerializablePreferenceFlow(
        NOTE_PRINT_OPTIONS_KEY, NotePrintOptionsDefault)
    suspend fun writeNotePrintOptions(notePrintOptions: NotePrintOptions) {
        writeSerializablePreference(NOTE_PRINT_OPTIONS_KEY, notePrintOptions)
    }

    // scientific mode
    val scientificMode = getPreferenceFlow(SCIENTIFIC_MODE_KEY, ScientificModeDefault)
    suspend fun writeScientificMode(scientificMode: Boolean) {
        writePreference(SCIENTIFIC_MODE_KEY, scientificMode)
    }

    val musicalScale = getTransformablePreferenceFlow(TEMPERAMENT_AND_REFERENCE_NOTE_KEY, MusicalScaleDefault) {
        try {
            Json.decodeFromString<MusicalScaleProperties>(it).toMusicalScale()
        } catch (ex: Exception) {
            MusicalScaleDefault
        }
    }

    suspend fun writeMusicalScaleProperties(properties: MusicalScaleProperties) {
        writeSerializablePreference(TEMPERAMENT_AND_REFERENCE_NOTE_KEY, properties)
    }

    // windowing
    val windowing = getTransformablePreferenceFlow(WINDOWING_KEY, WindowingDefault) {
            WindowingFunction.valueOf(it)
        }
    suspend fun writeWindowing(windowing: WindowingFunction) {
        writePreference(WINDOWING_KEY, windowing.name)
    }

    // overlap
    val overlap = getTransformablePreferenceFlow(OVERLAP_KEY, OverlapDefault) { it / 100f }
    suspend fun writeOverlap(overlapPercent: Int) {
        writePreference(OVERLAP_KEY, overlapPercent)
    }

    // window size
    val windowSize = getTransformablePreferenceFlow(WINDOW_SIZE_KEY, WindowSizeDefault) { 2f.pow(it).roundToInt() }
    val windowSizeExponent = getPreferenceFlow(WINDOW_SIZE_KEY, WindowSizeExponentDefault)

    suspend fun writeWindowSize(windowSizeExponent: Int) {
        writePreference(WINDOW_SIZE_KEY, windowSizeExponent)
    }

    val pitchHistoryDuration = getPreferenceFlow(PITCH_HISTORY_DURATION_KEY, PitchHistoryDurationDefault)
    suspend fun writePitchHistoryDuration(pitchHistoryDuration: Float) {
        writePreference(PITCH_HISTORY_DURATION_KEY, pitchHistoryDuration)
    }

    val pitchHistoryNumFaultyValues = getPreferenceFlow(PITCH_HISTORY_NUM_FAULTY_VALUES_KEY, PitchHistoryNumFaultyValuesDefault)
    suspend fun writePitchHistoryNumFaultyValues(pitchHistoryNumFaultyValues: Int) {
        writePreference(PITCH_HISTORY_NUM_FAULTY_VALUES_KEY, pitchHistoryNumFaultyValues)
    }

    val numMovingAverage = getPreferenceFlow(NUM_MOVING_AVERAGE_KEY, NumMovingAverageDefault)
    suspend fun writeNumMovingAverage(numMovingAverage: Int) {
        writePreference(NUM_MOVING_AVERAGE_KEY, numMovingAverage)
    }

    val maxNoise = getTransformablePreferenceFlow(MAX_NOISE_KEY, MaxNoiseDefault){ it / 100f }
    suspend fun writeMaxNoise(maxNoisePercent: Int) {
        writePreference(MAX_NOISE_KEY, maxNoisePercent)
    }

    val minHarmonicEnergyContent = getTransformablePreferenceFlow(MIN_HARMONIC_ENERGY_CONTENT_KEY, MinHarmonicEnergyContentDefault) { it / 100f }
    suspend fun writeMinHarmonicEnergyContent(minHarmonicEnergyContentPercent: Int) {
        writePreference(MIN_HARMONIC_ENERGY_CONTENT_KEY, minHarmonicEnergyContentPercent)
    }

    val sensitivity = getPreferenceFlow(SENSITIVITY_KEY, SensitivityDefault)
    suspend fun writeSensitivity(sensitivity: Int) {
        writePreference(SENSITIVITY_KEY, sensitivity)
    }

    val toleranceInCents = getPreferenceFlow(TOLERANCE_IN_CENTS_KEY, ToleranceInCentsDefault)
    suspend fun writeToleranceInCents(toleranceInCents: Int) {
        writePreference(TOLERANCE_IN_CENTS_KEY, toleranceInCents)
    }

    val waveWriterDurationInSeconds= getPreferenceFlow(
        WAVE_WRITER_DURATION_IN_SECONDS_KEY, WaveWriterDurationInSecondsDefault
    )
    suspend fun writeWaveWriterDurationInSeconds(waveWriterDurationInSeconds: Int) {
        writePreference(WAVE_WRITER_DURATION_IN_SECONDS_KEY, waveWriterDurationInSeconds)
    }

    suspend fun resetAllSettings() {
        dataStore.edit {
            it[APPEARANCE_KEY] = Json.encodeToString(AppearanceDefault)
            it[SCREEN_ALWAYS_ON] = ScreenAlwaysOnDefault
            it[TEMPERAMENT_AND_REFERENCE_NOTE_KEY] = Json.encodeToString(
                MusicalScaleProperties.create(MusicalScaleDefault)
            )
            it[TOLERANCE_IN_CENTS_KEY] = ToleranceInCentsDefault
            it[NOTE_PRINT_OPTIONS_KEY] = Json.encodeToString(NotePrintOptionsDefault)
            it[SENSITIVITY_KEY] = SensitivityDefault
            it[SCIENTIFIC_MODE_KEY] = ScientificModeDefault
            it[NUM_MOVING_AVERAGE_KEY] = NumMovingAverageDefault
            it[WINDOW_SIZE_KEY] = WindowSizeExponentDefault
            it[WINDOWING_KEY] = WindowingDefault.name
            it[OVERLAP_KEY] = (OverlapDefault * 100).roundToInt()
            it[PITCH_HISTORY_DURATION_KEY] = PitchHistoryDurationDefault
            it[PITCH_HISTORY_NUM_FAULTY_VALUES_KEY] = PitchHistoryNumFaultyValuesDefault
            it[WAVE_WRITER_DURATION_IN_SECONDS_KEY] = WaveWriterDurationInSecondsDefault

        }
    }

    private fun<T> getPreferenceFlow(key: Preferences.Key<T>, default: T): StateFlow<T> {
        return dataStore.data
            .catch {
//                Log.v("Tuner", "PreferenceRessources2: except: $it, $key")
                if (it is IOException) {
                    emit(emptyPreferences())
                }else {
                    throw it
                }
            }
            .map {
//                Log.v("Tuner", "PreferenceRessources2: $key ${it[key]}")
                it[key] ?: default
            }
            .stateIn(scope, SharingStarted.Eagerly, default)
    }
    private fun<K, T> getTransformablePreferenceFlow(key: Preferences.Key<K>, default: T, transform: (K) -> T): StateFlow<T> {
        return dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map {
                val s = it[key]
                if (s == null) default else transform(s)
            }
            .stateIn(scope, SharingStarted.Eagerly, default)
    }
    private inline fun<reified T> getSerializablePreferenceFlow(key: Preferences.Key<String>, default: T): StateFlow<T> {
        return dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map {
                val s = it[key]
                if (s == null) {
                    default
                } else {
                    try {
                        Json.decodeFromString<T>(s)
                    } catch(ex: Exception) {
                        default
                    }
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, default)
    }

    private suspend fun<T> writePreference(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    private suspend inline fun<reified T> writeSerializablePreference(key: Preferences.Key<String>, value: T) {
        dataStore.edit {
            it[key] = Json.encodeToString(value)
        }
    }

    init {
        // block everything until, all data is read to avoid incorrect startup behaviour
        runBlocking {
            dataStore.data.first()
        }
    }

    companion object {
        const val ReferenceFrequencyDefault = 440f

        private val AppearanceDefault = Appearance()
        private const val ScreenAlwaysOnDefault = false
        private val NotePrintOptionsDefault = NotePrintOptions()
        private const val ScientificModeDefault = false
        private val MusicalScaleDefault = MusicalScaleFactory.create(TemperamentType.EDO12)
        private val WindowingDefault = WindowingFunction.Tophat
        private val OverlapDefault = 25f / 100f
        private val WindowSizeExponentDefault = 12
        private val WindowSizeDefault = 2f.pow(WindowSizeExponentDefault).roundToInt()  // = 4096
        private val PitchHistoryDurationDefault = 1.5f
        private val PitchHistoryNumFaultyValuesDefault = 3
        private val NumMovingAverageDefault = 5
        private val MaxNoiseDefault = 0.1f
        private val MinHarmonicEnergyContentDefault = 0.1f
        private val SensitivityDefault = 90
        private val ToleranceInCentsDefault = 5
        private val WaveWriterDurationInSecondsDefault = 0

        private val APPEARANCE_KEY = stringPreferencesKey("appearance")
        private val SCREEN_ALWAYS_ON = booleanPreferencesKey("screenon")
        private val NOTE_PRINT_OPTIONS_KEY = stringPreferencesKey("note_print_options")
//        const val PREFER_FLAT_KEY = "prefer_flat"
//        const val SOLFEGE_KEY = "solfege"
        private val SCIENTIFIC_MODE_KEY = booleanPreferencesKey("scientific_mode")
//        private val NOTATION_KEY = stringPreferencesKey("notation")
        private val TEMPERAMENT_AND_REFERENCE_NOTE_KEY = stringPreferencesKey("temperament_and_reference_note")
        private val WINDOWING_KEY = stringPreferencesKey("windowing")
        private val OVERLAP_KEY = intPreferencesKey("overlap")
        private val WINDOW_SIZE_KEY = intPreferencesKey("window_size_exponent")
        private val PITCH_HISTORY_DURATION_KEY = floatPreferencesKey("pitch_history_duration")
        private val PITCH_HISTORY_NUM_FAULTY_VALUES_KEY = intPreferencesKey("pitch_history_num_faulty_values")
//        const val USE_HINT_KEY = "use_hint"
        private val NUM_MOVING_AVERAGE_KEY = intPreferencesKey("num_moving_average")
        private val MAX_NOISE_KEY = intPreferencesKey("max_noise")
        private val MIN_HARMONIC_ENERGY_CONTENT_KEY = intPreferencesKey("min_harmonic_energy_content")
        private val SENSITIVITY_KEY = intPreferencesKey("sensitivity")
        private val TOLERANCE_IN_CENTS_KEY = intPreferencesKey("tolerance_in_cents")
        private val WAVE_WRITER_DURATION_IN_SECONDS_KEY = intPreferencesKey("wave_writer_duration_in_seconds")
//        const val INSTRUMENT_ID_KEY = "instrument_id"
    }
}
