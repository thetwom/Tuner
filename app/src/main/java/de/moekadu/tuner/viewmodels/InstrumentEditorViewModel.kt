/*
* Copyright 2024 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.moekadu.tuner.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.instrumentChromatic
import de.moekadu.tuner.notedetection.FrequencyDetectionCollectedResults
import de.moekadu.tuner.notedetection.FrequencyEvaluationResult
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.tuner.Tuner
import de.moekadu.tuner.ui.instruments.StringWithInfo
import de.moekadu.tuner.ui.notes.NoteDetectorState
import de.moekadu.tuner.ui.screens.InstrumentEditorData
import de.moekadu.tuner.ui.instruments.StringsState
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.min

@HiltViewModel(assistedFactory = InstrumentEditorViewModel.Factory::class)
class InstrumentEditorViewModel @AssistedInject constructor(
    @Assisted instrument: Instrument,
    private val pref: PreferenceResources
) : ViewModel(), InstrumentEditorData {
     @AssistedFactory
     interface Factory {
         fun create(instrument: Instrument): InstrumentEditorViewModel
     }

    private val tuner = Tuner(
        pref,
        instrumentChromatic,
        viewModelScope,
        onResultAvailableListener = object : Tuner.OnResultAvailableListener {
            override fun onFrequencyDetected(result: FrequencyDetectionCollectedResults) {}

            override fun onFrequencyEvaluated(result: FrequencyEvaluationResult) {
                result.target?.let { tuningTarget ->
                    noteDetectorState.hitNote(tuningTarget.note)
                }
            }
        }
    )

    val musicalScale get() = pref.musicalScale
    private var stableId = instrument.stableId

    override val icon = MutableStateFlow(instrument.iconResource)
    override val name = MutableStateFlow(instrument.getNameString2(null))

    override val strings = MutableStateFlow(
        instrument.strings.mapIndexed { index, string ->
            StringWithInfo(string, index)
        }.toPersistentList()
    )
    override val stringsState = StringsState(strings.value.size - 1)

    override var selectedStringIndex = MutableStateFlow(strings.value.size - 1)

    override val noteDetectorState = NoteDetectorState()

    override val initializerNote = MutableStateFlow(musicalScale.value.referenceNote)

    override fun setIcon(icon: Int) {
        this.icon.value = icon
    }

    override fun setName(name: String) {
        this.name.value = name
    }

    override fun selectString(key: Int) {
        val index = strings.value.indexOfFirst { it.key == key }
        selectedStringIndex.value = index
    }

    override fun modifySelectedString(note: MusicalNote) {
        val stringsValue = strings.value
        val index = selectedStringIndex.value
        if (index in stringsValue.indices) {
            strings.value = stringsValue.mutate {
                it[index] = stringsValue[index].copy(note = note)
            }
        } else {
            initializerNote.value = note
        }
    }

    override fun addNote() {
        val stringsValue = strings.value
        if (stringsValue.size == 0) {
            strings.value = persistentListOf(StringWithInfo(initializerNote.value, 1))
            selectedStringIndex.value = 0
        } else {
            val newKey = StringWithInfo.generateKey(existingList = stringsValue)
            val note = stringsValue.getOrNull(selectedStringIndex.value)?.note ?: initializerNote.value
            val insertionPosition = min(selectedStringIndex.value + 1, stringsValue.size)
            strings.value = stringsValue.mutate {
                it.add(insertionPosition, StringWithInfo(note, newKey))
            }
            selectedStringIndex.value = insertionPosition
        }
    }

    override fun deleteNote() {
        val stringsValue = strings.value
        val index = selectedStringIndex.value
        if (index in stringsValue.indices) {
            strings.value = stringsValue.mutate {
                it.removeAt(index)
            }
            selectedStringIndex.value = if (stringsValue.size <= 1)
                0
            else
                index.coerceIn(0, stringsValue.size - 2)

            if (stringsValue.size == 1)
                initializerNote.value = stringsValue[0].note
        }
    }

//    private fun setInstrument(instrument: Instrument) {
//        setIcon(instrument.iconResource)
//        setName(instrument.getNameString2(null))
//        strings.value = instrument.strings.mapIndexed { index, string ->
//            StringWithInfo(string, index)
//        }.toPersistentList()
//        selectString(instrument.strings.size - 1) // key of last string
//        stableId = instrument.stableId
//    }

    fun getInstrument(): Instrument {
        return Instrument(
            name = this.name.value,
            nameResource = null,
            strings = strings.value.map{ it.note }.toTypedArray(),
            iconResource = icon.value,
            stableId = stableId,
            isChromatic = false
        )
    }

    fun startTuner() {
        tuner.connect()
    }
    fun stopTuner() {
        tuner.disconnect()
    }
}