package de.moekadu.tuner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class TuningEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _instrumentName = MutableLiveData<CharSequence>("")
    val instrumentName: LiveData<CharSequence> get() = _instrumentName
    private val _iconResourceId = MutableLiveData(R.drawable.ic_guitar)
    val iconResourceId: LiveData<Int> get() = _iconResourceId
    private val _strings = MutableLiveData<IntArray>()
    val strings: LiveData<IntArray> get() = _strings
    private val _selectedStringIndex = MutableLiveData(0)
    val selectedStringIndex: LiveData<Int> = _selectedStringIndex

    fun getInstrument(): Instrument {
        return Instrument(
            instrumentName.value.toString(), // toString removes underlines and stuff ...
            null,
            strings.value ?: intArrayOf(),
            iconResourceId.value ?: R.drawable.ic_guitar,
            Instrument.NO_STABLE_ID
        )
    }

    fun clear(singleStringToneIndex: Int = Int.MAX_VALUE) {
        _instrumentName.value = ""
        _iconResourceId.value = R.drawable.ic_guitar
        _strings.value = if (singleStringToneIndex != Int.MAX_VALUE)
            intArrayOf(singleStringToneIndex)
        else
            intArrayOf()
        _selectedStringIndex.value = 0
    }

    fun setInstrumentName(name: CharSequence?) {
        //Log.v("Tuner", "TuningEditorViewModel: Set instrument name: |$name|, current: |${instrumentName.value}|")
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

    fun setStrings(toneIndices: IntArray) {
        if (!toneIndices.contentEquals(_strings.value)) {
            _strings.value = toneIndices.copyOf()
            if (selectedStringIndex.value ?: 0 >= toneIndices.size)
            _selectedStringIndex.value = toneIndices.size - 1
        }
    }

    fun addStringBelowSelectedAndSelectNewString(toneIndex: Int = Int.MAX_VALUE) {
        val numOldStrings = strings.value?.size ?: 0
        val newStrings = IntArray(numOldStrings + 1)
        val currentSelectedIndex = selectedStringIndex.value ?: 0

        val newToneIndex = if (toneIndex != Int.MAX_VALUE)
            toneIndex
        else
            strings.value?.getOrNull(currentSelectedIndex) ?: 0

        if (numOldStrings > 0)
            strings.value?.copyInto(newStrings, 0,0, currentSelectedIndex + 1)

        if (currentSelectedIndex + 1 < numOldStrings)
            strings.value?.copyInto(newStrings, currentSelectedIndex + 2, currentSelectedIndex + 1, numOldStrings)
        val newSelectedIndex = if (currentSelectedIndex + 1 >= newStrings.size)
            newStrings.size - 1
        else
            currentSelectedIndex + 1

        newStrings[newSelectedIndex] = newToneIndex
        _strings.value = newStrings
        _selectedStringIndex.value = newSelectedIndex
    }

    fun setSelectedStringTo(toneIndex: Int) {
        val stringArray = strings.value ?: return
        if (stringArray.isEmpty())
            return
        val currentSelectedIndex = selectedStringIndex.value ?: 0

        if (currentSelectedIndex < stringArray.size && stringArray[currentSelectedIndex] != toneIndex) {
            stringArray[currentSelectedIndex] = toneIndex
            _strings.value = stringArray
        }
    }

    fun deleteSelectedString() {
        val stringArray = strings.value ?: return
        if (stringArray.isEmpty())
            return
        val currentSelectedIndex = selectedStringIndex.value ?: 0

        if (currentSelectedIndex < stringArray.size) {
            val newStringArray = IntArray(stringArray.size - 1)
            stringArray.copyInto(newStringArray, 0, 0, currentSelectedIndex)
            if (currentSelectedIndex + 1 < stringArray.size)
                stringArray.copyInto(newStringArray, currentSelectedIndex, currentSelectedIndex + 1, stringArray.size)
            _strings.value = newStringArray
            if (currentSelectedIndex >= newStringArray.size)
                _selectedStringIndex.value = newStringArray.size - 1
        }
    }
}