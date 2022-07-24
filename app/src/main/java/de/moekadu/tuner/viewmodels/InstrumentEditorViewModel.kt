package de.moekadu.tuner.viewmodels

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.preferences.TemperamentAndReferenceNoteValue
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.temperaments.NoteNameScaleFactory

class InstrumentEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _instrumentName = MutableLiveData<CharSequence>("")
    val instrumentName: LiveData<CharSequence> get() = _instrumentName
    private val _iconResourceId = MutableLiveData(R.drawable.ic_guitar)
    val iconResourceId: LiveData<Int> get() = _iconResourceId
    private val _strings = MutableLiveData<Array<MusicalNote> >()
    val strings: LiveData<Array<MusicalNote> > get() = _strings
    private val _selectedStringIndex = MutableLiveData(0)
    val selectedStringIndex: LiveData<Int> = _selectedStringIndex

    var stableId = Instrument.NO_STABLE_ID
    private var defaultNote = MusicalNote(BaseNote.A, NoteModifier.None, 4)

    private val pref = PreferenceManager.getDefaultSharedPreferences(application)

    private val onPreferenceChangedListener = object : SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (sharedPreferences == null)
                return
            when (key) {
                TemperamentAndReferenceNoteValue.TEMPERAMENT_AND_REFERENCE_NOTE_PREFERENCE_KEY -> {
                    val valueString = sharedPreferences.getString(
                        TemperamentAndReferenceNoteValue.TEMPERAMENT_AND_REFERENCE_NOTE_PREFERENCE_KEY, null)
                    val value = TemperamentAndReferenceNoteValue.fromString(valueString)
                    defaultNote = NoteNameScaleFactory.getDefaultReferenceNote(value!!.temperamentType)
                }
            }
        }
    }

    init {
        pref.registerOnSharedPreferenceChangeListener(onPreferenceChangedListener)
        loadSettingsFromSharedPreferences()
    }

    override fun onCleared() {
        pref.unregisterOnSharedPreferenceChangeListener(onPreferenceChangedListener)
        super.onCleared()
    }

    fun getInstrument(): Instrument {
        return Instrument(
            instrumentName.value.toString(), // toString removes underlines and stuff ...
            null,
            strings.value ?: arrayOf(),
            iconResourceId.value ?: R.drawable.ic_guitar,
            stableId
        )
    }

    fun setInstrument(instrument: Instrument) {
        _instrumentName.value = instrument.getNameString(getApplication())
        _iconResourceId.value = instrument.iconResource
        _strings.value = instrument.strings
        _selectedStringIndex.value = instrument.strings.size - 1
        stableId = instrument.stableId
    }

    fun clear(singleString: MusicalNote? = null) {
        _instrumentName.value = ""
        _iconResourceId.value = R.drawable.ic_guitar
        _strings.value = if (singleString == null)
            arrayOf()
        else
            arrayOf(singleString)
        _selectedStringIndex.value = 0
        stableId = Instrument.NO_STABLE_ID
    }

    fun setInstrumentName(name: CharSequence?) {
        //Log.v("Tuner", "InstrumentEditorViewModel: Set instrument name: |$name|, current: |${instrumentName.value}|")
        if (name?.contentEquals(instrumentName.value) == false)
            _instrumentName.value = name ?: ""
    }

    fun setInstrumentIcon(resourceId: Int) {
        if (resourceId != iconResourceId.value)
            _iconResourceId.value = resourceId
    }

    fun selectString(stringIndex: Int) {
        if (stringIndex != -1)
            _selectedStringIndex.value = stringIndex
    }

    fun setStrings(strings: Array<MusicalNote>) {
        if (!strings.contentEquals(_strings.value)) {
            _strings.value = strings.copyOf()
            if ((selectedStringIndex.value ?: 0) >= strings.size)
                _selectedStringIndex.value = strings.size - 1
        }
    }

    fun addStringBelowSelectedAndSelectNewString(string: MusicalNote? = null) {
        val numOldStrings = strings.value?.size ?: 0
        val currentSelectedIndex = selectedStringIndex.value ?: 0

        val newStrings = Array<MusicalNote?>(numOldStrings + 1) {null}

        val newString = string ?: strings.value?.getOrNull(currentSelectedIndex) ?: defaultNote

        if (numOldStrings > 0)
            strings.value?.copyInto(newStrings, 0,0, currentSelectedIndex + 1)

        if (currentSelectedIndex + 1 < numOldStrings)
            strings.value?.copyInto(newStrings, currentSelectedIndex + 2, currentSelectedIndex + 1, numOldStrings)
        val newSelectedIndex = if (currentSelectedIndex + 1 >= newStrings.size)
            newStrings.size - 1
        else
            currentSelectedIndex + 1

        newStrings[newSelectedIndex] = newString
        _strings.value = newStrings.filterNotNull().toTypedArray()
        _selectedStringIndex.value = newSelectedIndex
    }

    /** Change the note of the currently selected string.
     * @param note New note for the selectd string
     */
    fun setSelectedStringTo(note: MusicalNote) {
        val stringArray = strings.value ?: return
        if (stringArray.isEmpty())
            return
        val currentSelectedIndex = selectedStringIndex.value ?: 0

        if (currentSelectedIndex < stringArray.size && stringArray[currentSelectedIndex] != note) {
            stringArray[currentSelectedIndex] = note
            _strings.value = stringArray
        }
    }

    fun deleteSelectedString() {
        val stringArray = strings.value ?: return
        if (stringArray.isEmpty())
            return
        val currentSelectedIndex = selectedStringIndex.value ?: 0

        if (currentSelectedIndex < stringArray.size) {
            val newStringArray = Array<MusicalNote?>(stringArray.size - 1) {null}
            stringArray.copyInto(newStringArray, 0, 0, currentSelectedIndex)
            if (currentSelectedIndex + 1 < stringArray.size)
                stringArray.copyInto(newStringArray, currentSelectedIndex, currentSelectedIndex + 1, stringArray.size)
            _strings.value = newStringArray.filterNotNull().toTypedArray()
            if (currentSelectedIndex >= newStringArray.size)
                _selectedStringIndex.value = newStringArray.size - 1
        }
    }

    private fun loadSettingsFromSharedPreferences() {
        val values = TemperamentAndReferenceNoteValue.fromSharedPreferences(pref)
        defaultNote = NoteNameScaleFactory.getDefaultReferenceNote(values.temperamentType)
    }
}