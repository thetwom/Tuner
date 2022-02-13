package de.moekadu.tuner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class TuningEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _instrumentName = MutableLiveData("")
    val instrumentName: LiveData<String> get() = _instrumentName
    private val _iconResourceId = MutableLiveData(R.drawable.ic_guitar)
    val iconResourceId: LiveData<Int> get() = _iconResourceId
    private val _strings = MutableLiveData(intArrayOf())
    val strings: LiveData<IntArray> get() = _strings
    private val _selectedStringIndex = MutableLiveData(0)
    val selectedStringIndex: LiveData<Int> = _selectedStringIndex

    fun setInstrumentName(name: String) {
        if (name != instrumentName.value)
            _instrumentName.value = name
    }

    fun setInstrumentIcon(resourceId: Int) {
        if (resourceId != iconResourceId.value)
            _iconResourceId.value = resourceId
    }

    fun selectString(toneIndex: Int) {
        // TODO: string selecting should be better done by array index since we could have two times the same tone
        val arrayIndex = strings.value?.indexOfLast { it == toneIndex }
        if (arrayIndex != -1)
            _selectedStringIndex.value = arrayIndex
    }

    fun setStrings(toneIndices: IntArray) {
        if (!toneIndices.contentEquals(_strings.value)) {
            val currentToneIndex = if (selectedStringIndex.value ?: 0 < strings.value?.size ?: 0)
                strings.value?.get(selectedStringIndex.value ?: 0) ?: 0
            else
                Int.MAX_VALUE
            // TODO: don't keep toneIndex constant, but better the arrayIndex!
            var newIndexOfTone = toneIndices.indexOfLast { it == currentToneIndex }
            if (newIndexOfTone == -1)
                newIndexOfTone = toneIndices.size - 1
            _strings.value = toneIndices.copyOf()
            _selectedStringIndex.value = newIndexOfTone
        }
    }

    fun addStringBelowSelectedAndSelectNewString(toneIndex: Int = Int.MAX_VALUE) {
        val numOldStrings = strings.value?.size ?: 0
        val newStrings = IntArray(numOldStrings + 1)
        val currentSelectedIndex = selectedStringIndex.value ?: 0

        val newToneIndex = when {
            toneIndex != Int.MAX_VALUE -> toneIndex
            currentSelectedIndex < numOldStrings -> strings.value?.get(currentSelectedIndex) ?: 0
            else -> 0
        }

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
        }
    }
}