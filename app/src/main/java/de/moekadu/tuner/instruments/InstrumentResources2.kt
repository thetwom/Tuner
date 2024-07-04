package de.moekadu.tuner.instruments

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import de.moekadu.tuner.misc.ResourcesDataStoreBase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstrumentResources2 @Inject constructor(
    @ApplicationContext context: Context
) {
    val store = ResourcesDataStoreBase(context, "instruments")

    val predefinedInstruments = instrumentDatabase.toImmutableList()

    val currentInstrument = store.getSerializablePreferenceFlow(
        CURRENT_INSTRUMENT_KEY, predefinedInstruments[0]
    )
    suspend fun writeCurrentInstrument(instrument: Instrument) {
        store.writeSerializablePreference(CUSTOM_INSTRUMENTS_KEY, instrument)
    }

    val customInstruments = store.getSerializablePreferenceFlow(
        CUSTOM_INSTRUMENTS_KEY, CustomInstrumentsDefault
    )
    suspend fun writeCustomInstruments(instruments: ImmutableList<Instrument>) {
        store.writeSerializablePreference(CUSTOM_INSTRUMENTS_KEY, instruments)
    }

    val customInstrumentsExpanded = store.getPreferenceFlow(
        CUSTOM_INSTRUMENTS_EXPANDED_KEY, CustomInstrumentExpandedDefault
    )
    suspend fun writeCustomInstrumentsExpanded(expanded: Boolean) {
        store.writePreference(CUSTOM_INSTRUMENTS_EXPANDED_KEY, expanded)
    }

    val predefinedInstrumentsExpanded = store.getPreferenceFlow(
        PREDEFINED_INSTRUMENTS_EXPANDED_KEY, PredefinedInstrumentExpandedDefault
    )
    suspend fun writePredefinedInstrumentsExpanded(expanded: Boolean) {
        store.writePreference(PREDEFINED_INSTRUMENTS_EXPANDED_KEY, expanded)
    }

    companion object {
        private val CustomInstrumentsDefault = persistentListOf<Instrument>()
        private const val CustomInstrumentExpandedDefault = true
        private const val PredefinedInstrumentExpandedDefault = true

        private val CURRENT_INSTRUMENT_KEY = stringPreferencesKey("current instrument")
        private val CUSTOM_INSTRUMENTS_KEY = stringPreferencesKey("custom instruments")
        private val CUSTOM_INSTRUMENTS_EXPANDED_KEY = booleanPreferencesKey(
            "custom instruments expanded"
        )

        private val PREDEFINED_INSTRUMENTS_EXPANDED_KEY = booleanPreferencesKey(
            "predefined instruments expanded"
        )
    }
    //private val customInstruments = ...



//    enum class Section {Predefined, Custom, Undefined }
//    data class InstrumentAndSection(val instrument: Instrument, val section: Section)

//    val customInstrumentDatabase = readCustomInstrumentsDatabaseFromPreferences(sharedPreferences)
//
//    /** Predefined instruments, this is simply and no database, since it never changes. */
//    val predefinedInstruments: List<Instrument> = instrumentDatabase
//    private val _predefinedDatabaseExpanded
//            = MutableStateFlow(readPredefinedInstrumentsExpandedFromPreferences(sharedPreferences))
//    val predefinedInstrumentsExpanded: StateFlow<Boolean> get() = _predefinedDatabaseExpanded
//
//    /** Custom instruments list, derived from database. */
//    private val _customInstruments
//            = MutableStateFlow(customInstrumentDatabase.getCopyOfInstrumentsList())
//    val customInstruments: StateFlow<List<Instrument> > get() = _customInstruments
//
//    private val _customInstrumentsExpanded
//            = MutableStateFlow(readCustomInstrumentsExpandedFromPreferences(sharedPreferences))
//    val customInstrumentsExpanded: StateFlow<Boolean> get() = _customInstrumentsExpanded
//
//    /** Instrument which is currently active together with the section where it is stored.
//     * This depends on the instrument lists, so this must come after them!
//     */
//    private val _instrument = MutableStateFlow(readInstrumentAndSectionFromPreferences(
//        sharedPreferences, predefinedInstruments, customInstruments.value
//    ))
//    val instrument: StateFlow<InstrumentAndSection> get() = _instrument
//
//    private fun readInstrumentAndSectionFromPreferences(
//        prefs: SharedPreferences,
//        predefinedInstruments: List<Instrument>,
//        customInstruments: List<Instrument>
//    ): InstrumentAndSection {
//        val instrumentId = prefs.getLong(CURRENT_INSTRUMENT_ID_KEY, 0)
//        val sectionString = prefs.getString(SECTION_OF_CURRENT_INSTRUMENT_KEY, null)
//        val section = if (sectionString == null)
//            Section.Predefined
//        else
//            Section.valueOf(sectionString)
//
//        return when (section) {
//            Section.Predefined -> {
//                val i = predefinedInstruments.find { it.stableId == instrumentId } ?: predefinedInstruments[0]
//                InstrumentAndSection(i, Section.Predefined)
//            }
//            Section.Custom -> {
//                val i = customInstruments.find { it.stableId == instrumentId }
//                if (i == null)
//                    InstrumentAndSection(predefinedInstruments[0], Section.Predefined)
//                else
//                    InstrumentAndSection(i, Section.Custom)
//            }
//            Section.Undefined -> {
//                throw RuntimeException("Instruments in undefined sections must not be stored in preferences")
//            }
//        }
//    }
//
//    private fun readPredefinedInstrumentsExpandedFromPreferences(prefs: SharedPreferences)
//            = prefs.getBoolean(PREDEFINED_SECTION_EXPANDED_KEY, true)
//
//    private fun readCustomInstrumentsDatabaseFromPreferences(prefs: SharedPreferences): InstrumentDatabase {
//        val instrumentsString = prefs.getString(CUSTOM_INSTRUMENTS_KEY, "") ?: ""
//        val instruments = InstrumentDatabase.stringToInstruments(instrumentsString)
//        val database = InstrumentDatabase()
//
//        database.loadInstruments(instruments.instruments, InstrumentDatabase.InsertMode.Replace)
//        database.databaseChangedListener = InstrumentDatabase.DatabaseChangedListener { db ->
//            _customInstruments.value = db.getCopyOfInstrumentsList()
//
//            // update instrument
//            db.getInstrument(instrument.value.instrument.stableId)?.let {
//                _instrument.value = InstrumentAndSection(it, Section.Custom)
//            }
//        }
//        return database
//    }
//
//    private fun readCustomInstrumentsExpandedFromPreferences(prefs: SharedPreferences)
//            = prefs.getBoolean(CUSTOM_SECTION_EXPANDED_KEY, true)
//
//    fun moveCustomInstrument(fromIndex: Int, toIndex: Int) {
//        customInstrumentDatabase.move(fromIndex, toIndex)
//        writeToSharedPreferences()
//    }
//
//    fun removeCustomInstrument(position: Int): Instrument {
//        val instrument = customInstrumentDatabase.remove(position)
//        writeToSharedPreferences()
//        return instrument
//    }
//
//    fun addCustomInstrument(position: Int, instrument: Instrument) {
////        Log.v("Tuner", "InstrumentResources.addCustomInstrument")
//        customInstrumentDatabase.add(position, instrument)
//        writeToSharedPreferences()
//    }
//
//    fun replaceOrAddCustomInstrument(instrument: Instrument) {
//        customInstrumentDatabase.replaceOrAdd(instrument)
//        writeToSharedPreferences()
//    }
//
//    fun setCustomInstrumentsExpanded(isExpanded: Boolean) {
//        _customInstrumentsExpanded.value = isExpanded
//        writeToSharedPreferences()
//    }
//
//    fun setPredefinedInstrumentsExpanded(isExpanded: Boolean) {
//        _predefinedDatabaseExpanded.value = isExpanded
//        writeToSharedPreferences()
//    }
//
//    fun selectInstrument(instrument: Instrument) {
//        val section = if (predefinedInstruments.firstOrNull { it.stableId == instrument.stableId } != null)
//            Section.Predefined
//        else if (customInstruments.value.firstOrNull { it.stableId == instrument.stableId } != null)
//            Section.Custom
//        else
//            Section.Undefined
//
//        _instrument.value = InstrumentAndSection(instrument, section)
//        writeToSharedPreferences()
//    }
//
//    private fun writeToSharedPreferences() {
////        Log.v("Tuner", "InstrumentResources.writeToSharedPreferences")
//        sharedPreferences.edit {
//            putBoolean(CUSTOM_SECTION_EXPANDED_KEY, customInstrumentsExpanded.value)
//            putBoolean(PREDEFINED_SECTION_EXPANDED_KEY, predefinedInstrumentsExpanded.value)
//
//            putLong(CURRENT_INSTRUMENT_ID_KEY, instrument.value.instrument.stableId)
//            putString(SECTION_OF_CURRENT_INSTRUMENT_KEY, instrument.value.section.name)
//
//            // we don't need a context here, since custom instruments do not use string resources
//            // and the context is only needed for instruments, which have a string resource as name.
//            putString(CUSTOM_INSTRUMENTS_KEY, customInstrumentDatabase.getInstrumentsString(null))
//        }
//    }
//
//    // TODO: call this in activity with getPreferences(MODE_PRIVATE) and afterwards delete the activity preferences and don't call this again
//    fun migrateFromOtherSharedPreferences(otherSharedPreferences: SharedPreferences) {
//        val instrumentsString = otherSharedPreferences.getString(CUSTOM_INSTRUMENTS_KEY, "") ?: ""
//        val instruments = InstrumentDatabase.stringToInstruments(instrumentsString)
//        customInstrumentDatabase.loadInstruments(instruments.instruments)
//        _predefinedDatabaseExpanded.value = readPredefinedInstrumentsExpandedFromPreferences(otherSharedPreferences)
//        _customInstrumentsExpanded.value = readCustomInstrumentsExpandedFromPreferences(otherSharedPreferences)
//        _instrument.value = readInstrumentAndSectionFromPreferences(otherSharedPreferences, predefinedInstruments, customInstruments.value)
//        writeToSharedPreferences()
//    }
//
//    companion object {
//        const val CUSTOM_SECTION_EXPANDED_KEY = "custom_section_expanded"
//        const val PREDEFINED_SECTION_EXPANDED_KEY = "predefined_section_expanded"
//
//        const val CURRENT_INSTRUMENT_ID_KEY = "instrument_id"
//        const val SECTION_OF_CURRENT_INSTRUMENT_KEY = "instrument_section"
//
//        const val CUSTOM_INSTRUMENTS_KEY = "custom_instruments"
//    }
}