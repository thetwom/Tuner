package de.moekadu.tuner.viewmodels

import android.content.Context
import androidx.lifecycle.*
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.models.DetectedNoteViewModel
import de.moekadu.tuner.models.InstrumentEditorNameModel
import de.moekadu.tuner.models.NoteSelectorForEditorModel
import de.moekadu.tuner.models.StringViewEditorModel
import de.moekadu.tuner.notedetection.FrequencyEvaluator
import de.moekadu.tuner.notedetection.frequencyDetectionFlow
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.temperaments.MusicalNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/** Class to store a set of strings and a index of the selected string.
 * @param strings List of strings.
 * @param selectedIndex Index of selected string in the strings-list.
 */
private class StringsAndSelection(val strings: Array<MusicalNote>, val selectedIndex: Int) {
    val note get() = strings.getOrNull(selectedIndex)

    fun copy(strings: Array<MusicalNote> = this.strings, selectedIndex: Int = this.selectedIndex): StringsAndSelection {
        return StringsAndSelection(strings, selectedIndex)
    }
}

/** View model for instrument editor.
 * @param pref App preference resources.
 */
class InstrumentEditorViewModel(private val pref: PreferenceResources) : ViewModel() {
    /** Job for detecting notes. This is used to show currently detected notes. */
    private var noteDetectionJob: Job? = null

    /** Sample rate for the frequency detection. */
    val sampleRate = DefaultValues.SAMPLE_RATE

    /** Model for the instrument name. */
    private val _instrumentNameModel = MutableLiveData(InstrumentEditorNameModel("", R.drawable.ic_guitar))
    val instrumentNameModel: LiveData<InstrumentEditorNameModel> get() = _instrumentNameModel

    /** List of strings in the editor. */
    private val _strings = MutableStateFlow(StringsAndSelection(arrayOf(), 0))
    private val strings = _strings.asStateFlow()

    /** Model for the instrument strings. */
    private val _stringViewModel = MutableLiveData(StringViewEditorModel())
    val stringViewModel: LiveData<StringViewEditorModel> get() = _stringViewModel

    /** Model for the note selector. */
    private val _noteSelectorModel = MutableLiveData(NoteSelectorForEditorModel())
    val noteSelectorModel: LiveData<NoteSelectorForEditorModel> get() = _noteSelectorModel

    /** Stable id of the instrument which is modified.
     * Use Instrument.NO_STABLE_ID, if a new instrument is defined.
     */
    var stableId = Instrument.NO_STABLE_ID
    /** Default note, which is used, for new notes if we have no other good choice. */
    private val defaultNote get() = pref.musicalScale.value.referenceNote

    /** Model for showing the lately detected notes. */
    private val _detectedNoteModel = MutableLiveData(DetectedNoteViewModel())
    val detectedNoteModel: LiveData<DetectedNoteViewModel> get() = _detectedNoteModel

    init {
        viewModelScope.launch { pref.overlap.collect { overlap ->
            _detectedNoteModel.value = detectedNoteModel.value?.apply {
                changeSettings(noteUpdateInterval = computePitchHistoryUpdateInterval(overlap = overlap))
            }
            restartSamplingIfRunning()
        } }
        viewModelScope.launch { pref.windowSize.collect { windowSize ->
            _detectedNoteModel.value = detectedNoteModel.value?.apply {
                changeSettings(noteUpdateInterval = computePitchHistoryUpdateInterval(windowSize = windowSize))
            }
            restartSamplingIfRunning()
        } }

        viewModelScope.launch { pref.maxNoise.collect {
            restartSamplingIfRunning()
        }}

        viewModelScope.launch { pref.minHarmonicEnergyContent.collect {
            restartSamplingIfRunning()
        }}

        viewModelScope.launch { pref.numMovingAverage.collect {
            restartSamplingIfRunning()
        }}

        viewModelScope.launch { pref.toleranceInCents.collect {
            restartSamplingIfRunning()
        }}

        viewModelScope.launch { pref.pitchHistoryMaxNumFaultyValues.collect {
            restartSamplingIfRunning()
        }}

        viewModelScope.launch { pref.musicalScale.collect {
            _stringViewModel.value = stringViewModel.value?.apply {
                changeSettings(musicalScale = it)
            }
            _noteSelectorModel.value = noteSelectorModel.value?.apply {
                changeSettings(musicalScale = it)
            }
            _detectedNoteModel.value = detectedNoteModel.value?.apply {
                changeSettings(musicalScale = it)
            }
            restartSamplingIfRunning()
        }}

        viewModelScope.launch { pref.noteNamePrinter.collect {
            _stringViewModel.value = stringViewModel.value?.apply {
                changeSettings(noteNamePrinter = it)
            }
            _noteSelectorModel.value = noteSelectorModel.value?.apply {
                changeSettings(noteNamePrinter = it)
            }
            _detectedNoteModel.value = detectedNoteModel.value?.apply {
                changeSettings(noteNamePrinter = it)
            }
        }}

        viewModelScope.launch { strings.collect {
            _stringViewModel.value = stringViewModel.value?.apply {
                changeSettings(strings = it.strings, selectedStringIndex = it.selectedIndex)
            }
            _noteSelectorModel.value = noteSelectorModel.value?.apply {
                it.note?.let { note ->
                    changeSettings(selectedNote = note)
                }
            }
        }}
    }

    /** Get instrument, which is defined in the instrument editor.
     * @return Instrument of the editor.
     */
    fun getInstrument(): Instrument {
//        Log.v("Tuner", "InstrumentEditorViewModel: strings = ${strings.value.strings}")
        return Instrument(
            instrumentNameModel.value?.name.toString(), // toString removes underlines and stuff ...
            null,
            strings.value.strings,
            instrumentNameModel.value?.iconResourceId ?: R.drawable.ic_guitar,
            stableId
        )
    }

    /** Set the instrument editor to a given instrument.
     * @param instrument Instrument to set.
     * @param context Context to obtain string from string resources. If null, the instrument
     *   name can only be used if it is not based on a string resource.
     */
    fun setInstrument(instrument: Instrument, context: Context?) {
        _instrumentNameModel.value = InstrumentEditorNameModel(
            instrument.getNameString(context),
            instrument.iconResource
        )
        _strings.value = StringsAndSelection(instrument.strings, instrument.strings.size -1)

        stableId = instrument.stableId
    }

    /** Set the editor the the default instrument.
     * The default instrument is just one string with the reference note.
     */
    fun setDefaultInstrument() {
        clear(pref.musicalScale.value.referenceNote)
    }

    /** Clear the editor.
     * @param singleString If this is not null, after clearing we will set one string of the given
     *   note.
     */
    fun clear(singleString: MusicalNote? = null) {
        _instrumentNameModel.value = InstrumentEditorNameModel("", R.drawable.ic_guitar)
        _strings.value = StringsAndSelection(
            if (singleString == null) arrayOf() else arrayOf(singleString),
            0
        )
        stableId = Instrument.NO_STABLE_ID
    }

    /** Set the instrument name in the editor.
     * @param name Instrument name.
     */
    fun setInstrumentName(name: CharSequence?) {
        //Log.v("Tuner", "InstrumentEditorViewModel: Set instrument name: |$name|, current: |${instrumentName.value}|")
        if (name?.contentEquals(instrumentNameModel.value?.name) == false)
            _instrumentNameModel.value = instrumentNameModel.value?.copy(name = name)
    }

    /** Set the instrument icon in the editor.
     * @param resourceId Resource id of the icon.
     */
    fun setInstrumentIcon(resourceId: Int) {
        if (resourceId != instrumentNameModel.value?.iconResourceId)
            _instrumentNameModel.value = instrumentNameModel.value?.copy(iconResourceId = resourceId)
    }

    /** Select a string, which is currently modified.
     * @param stringIndex Index of string to be selected.
     */
    fun selectString(stringIndex: Int) {
        if (stringIndex != -1) {
            _strings.value = strings.value.copy(selectedIndex = stringIndex)
        }
    }

//    fun setStrings(strings: Array<MusicalNote>) {
//        if (!strings.contentEquals(this.strings.value.strings)) {
//            _strings.value = StringsAndSelection(
//                this.strings.value.strings.copyOf(),
//                if (this.strings.value.selectedIndex >= strings.size)
//                    strings.size - 1
//                else
//                    this.strings.value.selectedIndex
//            )
//        }
//    }

    /** Add string below the selected string and select it.
     * @param string Note of new string to insert. If null we will use the note of the selected
     *   string or the default note, if no string is selected.
     */
    fun addStringBelowSelectedAndSelectNewString(string: MusicalNote? = null) {
        val numOldStrings = strings.value.strings.size
        val currentSelectedIndex = strings.value.selectedIndex

        val newStrings = Array<MusicalNote?>(numOldStrings + 1) {null}

        val newString = string ?: strings.value.strings.getOrNull(currentSelectedIndex) ?: defaultNote

        if (numOldStrings > 0)
            strings.value.strings.copyInto(newStrings, 0,0, currentSelectedIndex + 1)

        if (currentSelectedIndex + 1 < numOldStrings)
            strings.value.strings.copyInto(newStrings, currentSelectedIndex + 2, currentSelectedIndex + 1, numOldStrings)
        val newSelectedIndex = if (currentSelectedIndex + 1 >= newStrings.size)
            newStrings.size - 1
        else
            currentSelectedIndex + 1

        newStrings[newSelectedIndex] = newString
        _strings.value = StringsAndSelection(
            newStrings.filterNotNull().toTypedArray(),
            newSelectedIndex
        )
    }

    /** Change the note of the currently selected string.
     * @param note New note for the selected string
     */
    fun setSelectedStringTo(note: MusicalNote) {
        if (strings.value.strings.isEmpty())
            return
        val currentSelectedIndex = strings.value.selectedIndex

        if (currentSelectedIndex < strings.value.strings.size && strings.value.strings[currentSelectedIndex] != note) {
            strings.value.strings[currentSelectedIndex] = note
            _strings.value = strings.value.copy(strings = strings.value.strings)
            //_strings.value = stringArray
        }
    }

    /** Delete the currently selected string. */
    fun deleteSelectedString() {
        val stringArray = strings.value.strings
        if (stringArray.isEmpty())
            return
        val currentSelectedIndex = strings.value.selectedIndex

        if (currentSelectedIndex < stringArray.size) {
            val newStringArray = Array<MusicalNote?>(stringArray.size - 1) {null}
            stringArray.copyInto(newStringArray, 0, 0, currentSelectedIndex)
            if (currentSelectedIndex + 1 < stringArray.size)
                stringArray.copyInto(newStringArray, currentSelectedIndex, currentSelectedIndex + 1, stringArray.size)
            _strings.value = strings.value.copy(
                strings = newStringArray.filterNotNull().toTypedArray(),
                selectedIndex = if (currentSelectedIndex >= newStringArray.size)
                    newStringArray.size - 1
                else
                    currentSelectedIndex
            )
        }
    }

    /** Restart sampling for note detection if it is running.
     * The app preferences will be used to configure the detection.
     */
    private fun restartSamplingIfRunning() {
        if (noteDetectionJob != null)
            startSampling()
    }

    /** Start sampling for note detection.
     * The app preferences will be used to configure the detection.
     */
    fun startSampling() {
        noteDetectionJob?.cancel()
        noteDetectionJob = viewModelScope.launch(Dispatchers.Main) {
            val frequencyEvaluator = FrequencyEvaluator(
                pref.numMovingAverage.value,
                pref.toleranceInCents.value.toFloat(),
                pref.pitchHistoryMaxNumFaultyValues.value,
                pref.maxNoise.value,
                pref.minHarmonicEnergyContent.value,
                pref.musicalScale.value,
                Instrument(null, null, arrayOf(), 0, 0, true)
            )
            frequencyDetectionFlow(pref, null)
                .flowOn(Dispatchers.Default)
                .collect {
                    ensureActive()
                    frequencyEvaluator.evaluate(it.memory, null).target?.note?.let {
                        _detectedNoteModel.value = detectedNoteModel.value?.apply {
                            changeSettings(note = it)
                        }
                    }

                    it.decRef()
//                Log.v("TunerViewModel", "collecting frequencyDetectionFlow")
                }
        }
    }

    /** Stop the note detection job. */
    fun stopSampling() {
        noteDetectionJob?.cancel()
    }

    /** Compute duration in seconds, between two new note detection results.
     * @param windowSize Number of samples for one frequency evaluation.
     * @param overlap Overlap between two succeeding windows. (where 0.0 is no overlap;
     *   1.0 would be full overlap, which is not allowed ...)
     * @param sampleRate Sample rate.
     */
    private fun computePitchHistoryUpdateInterval(
        windowSize: Int = pref.windowSize.value,
        overlap: Float = pref.overlap.value,
        sampleRate: Int = this.sampleRate
    ) = windowSize * (1f - overlap) / sampleRate

    /** Factory for creating the view model.
     * @param pref App preferences.
     */
    class Factory(private val pref: PreferenceResources) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InstrumentEditorViewModel(pref) as T
        }
    }
}