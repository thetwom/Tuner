package de.moekadu.tuner

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import androidx.preference.PreferenceManager


class InstrumentsViewModel(
    initialInstrumentId: Long,
    initialInstrumentSection: String?,
    initialCustomInstrumentsString: String,
    initialPredefinedExpanded: Boolean,
    initialCustomExpanded: Boolean,
    application: Application) : AndroidViewModel(application) {

    enum class Section {Predefined, Custom}
    class InstrumentAndSection(val instrument: Instrument, val section: Section)

    private var _instrument = MutableLiveData<InstrumentAndSection>()

    val instrument: LiveData<InstrumentAndSection>
        get() = _instrument

    private val _predefinedDatabaseExpanded = MutableLiveData(initialPredefinedExpanded)
    val predefinedDatabaseExpanded: LiveData<Boolean> get() = _predefinedDatabaseExpanded

    private val _customInstrumentDatabase = MutableLiveData<InstrumentDatabase>()

    val customInstrumentDatabase: LiveData<InstrumentDatabase>
        get() = _customInstrumentDatabase

    private val _customDatabaseExpanded = MutableLiveData(initialCustomExpanded)
    val customDatabaseExpanded: LiveData<Boolean> get() = _customDatabaseExpanded

    init {
        Log.v("Tuner", "InstrumentsViewModel.init: loading custom instruments $initialCustomInstrumentsString")
        val instruments = InstrumentDatabase.stringToInstruments(initialCustomInstrumentsString)
        val database = InstrumentDatabase()
        database.loadInstruments(instruments.instruments, InstrumentDatabase.InsertMode.Replace)
        _customInstrumentDatabase.value = database

        val section = try {
            if (initialInstrumentSection == null) Section.Predefined else Section.valueOf(initialInstrumentSection)
        } catch (e: IllegalArgumentException) {
            Section.Predefined
        }
        _instrument.value = when (section) {
            Section.Predefined -> {
                val i = instrumentDatabase.find { it.stableId == initialInstrumentId } ?: instrumentDatabase[0]
                InstrumentAndSection(i, Section.Predefined)
            }
            Section.Custom -> {
                val i = customInstrumentDatabase.value?.instruments?.find { it.stableId == initialInstrumentId }
                if (i == null)
                    InstrumentAndSection(instrumentDatabase[0], Section.Predefined)
                else
                    InstrumentAndSection(i, Section.Custom)
            }
        }
    }

    fun setInstrument(instrument: Instrument, section: Section) {
        val currentInstrument = _instrument.value
        if (currentInstrument == null || currentInstrument.instrument.stableId != instrument.stableId || currentInstrument.section != section) {
//            Log.v("Tuner", "InstrumentsViewModel.setInstrument: $instrument")
            _instrument.value = InstrumentAndSection(instrument, section)
        }
    }

    fun expandPredefinedDatabase(value: Boolean = true) {
        if (value != _predefinedDatabaseExpanded.value)
            _predefinedDatabaseExpanded.value = value
    }
    fun expandCustomDatabase(value: Boolean = true) {
        if (value != _customDatabaseExpanded.value)
            _customDatabaseExpanded.value = value
    }
    fun addInstrument(instrument: Instrument) {
        // TODO: maybe we must copy the database to make the adapter work correctly
        val database = customInstrumentDatabase.value ?: InstrumentDatabase()
        database.add(instrument)
        _customInstrumentDatabase.value = database
    }

    class Factory(
        private val initialInstrumentId: Long,
        private val initialInstrumentSection: String?,
        private val initialCustomInstrumentsString: String,
        private val initialPredefinedExpanded: Boolean,
        private val initialCustomExpanded: Boolean,
        private val application: Application) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
//            Log.v("Metronome", "ScenesViewModel.factory.create")
            return InstrumentsViewModel(
                initialInstrumentId,
                initialInstrumentSection,
                initialCustomInstrumentsString,
                initialPredefinedExpanded,
                initialCustomExpanded,
                application) as T
        }
    }
}