package de.moekadu.tuner.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentDatabase
import de.moekadu.tuner.instruments.instrumentDatabase


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

    private val _predefinedInstrumentList = MutableLiveData<List<Instrument> >()
    val predefinedInstrumentList: LiveData<List<Instrument> >
        get() = _predefinedInstrumentList

    private val _predefinedDatabaseExpanded = MutableLiveData(initialPredefinedExpanded)
    val predefinedDatabaseExpanded: LiveData<Boolean> get() = _predefinedDatabaseExpanded

    val customInstrumentDatabase = InstrumentDatabase()
    private val _customInstrumentDatabaseAsLiveData = MutableLiveData(customInstrumentDatabase)
    val customInstrumentDatabaseAsLiveData: LiveData<InstrumentDatabase> get() = _customInstrumentDatabaseAsLiveData

    private val _customInstrumentList = MutableLiveData<List<Instrument> >()
    val customInstrumentList: LiveData<List<Instrument> >
        get() = _customInstrumentList

    private val _customDatabaseExpanded = MutableLiveData(initialCustomExpanded)
    val customDatabaseExpanded: LiveData<Boolean> get() = _customDatabaseExpanded

    private val _uri = MutableLiveData<Uri?>()
    val uri: LiveData<Uri?> get() = _uri

    init {
//        Log.v("Tuner", "InstrumentsViewModel.init: loading custom instruments $initialCustomInstrumentsString, customExpanded=${customDatabaseExpanded.value}")
        val instruments = InstrumentDatabase.stringToInstruments(initialCustomInstrumentsString)
        customInstrumentDatabase.loadInstruments(instruments.instruments, InstrumentDatabase.InsertMode.Replace)
        customInstrumentDatabase.databaseChangedListener = InstrumentDatabase.DatabaseChangedListener { db ->
//            Log.v("Tuner", "InstrumentsViewModel.DatabaseChanged: size=${db.size}, custom expanded=${customDatabaseExpanded.value}")
            _customInstrumentDatabaseAsLiveData.value = db

            if (customDatabaseExpanded.value == true) {
                val listCopy = ArrayList<Instrument>(db.size)
                db.instruments.forEach { listCopy.add(it.copy()) }
                _customInstrumentList.value = listCopy
            }

            // update instrument if necessary
            db.getInstrument(instrument.value?.instrument?.stableId ?: Instrument.NO_STABLE_ID)?.let {
                _instrument.value = InstrumentAndSection(it, Section.Custom)
            }
        }

        if (_customDatabaseExpanded.value == true) {
            val listCopy = ArrayList<Instrument>(customInstrumentDatabase.size)
            customInstrumentDatabase.instruments.forEach { listCopy.add(it.copy()) }
            _customInstrumentList.value = listCopy
        }

        if (_predefinedDatabaseExpanded.value == true) {
            val listCopy = ArrayList<Instrument>(instrumentDatabase.size)
            instrumentDatabase.forEach {listCopy.add(it.copy())}
            _predefinedInstrumentList.value = listCopy
        }

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
                val i = customInstrumentDatabase.instruments.find { it.stableId == initialInstrumentId }
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
        if (value != _predefinedDatabaseExpanded.value) {
//            Log.v("Tuner", "InstrumentsViewModel.expandPredefinedDatabase: $value")
            _predefinedDatabaseExpanded.value = value
            _predefinedInstrumentList.value = if (value) {
                val listCopy = ArrayList<Instrument>(instrumentDatabase.size)
                instrumentDatabase.forEach { listCopy.add(it.copy()) }
                listCopy
            } else {
                ArrayList()
            }
        }
    }
    fun expandCustomDatabase(value: Boolean = true) {
        if (value != _customDatabaseExpanded.value) {
//            Log.v("Tuner", "InstrumentsViewModel.expandCustomDatabase: $value")
            _customDatabaseExpanded.value = value
            _customInstrumentList.value = if (value) {
                val listCopy = ArrayList<Instrument>(customInstrumentDatabase.size)
                customInstrumentDatabase.instruments.forEach { listCopy.add(it.copy()) }
                listCopy
            } else {
                ArrayList()
            }
        }
    }

    fun loadInstrumentsFromFile(uri: Uri) {
        _uri.value = uri
    }

    fun loadingFileCompleted() {
        _uri.value = null
    }
//    fun addInstrument(instrument: Instrument) {
//        customInstrumentDatabase.add(instrument)
//        if (customDatabaseExpanded.value == true)
//            _customInstrumentList.value = customInstrumentDatabase.instruments
//    }

    class Factory(
        private val initialInstrumentId: Long,
        private val initialInstrumentSection: String?,
        private val initialCustomInstrumentsString: String,
        private val initialPredefinedExpanded: Boolean,
        private val initialCustomExpanded: Boolean,
        private val application: Application) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
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