package de.moekadu.tuner.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.instrumentChromatic
import de.moekadu.tuner.notedetection.FrequencyDetectionCollectedResults
import de.moekadu.tuner.notedetection.FrequencyEvaluationResult
import de.moekadu.tuner.preferences.PreferenceResources2
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.tuner.Tuner
import de.moekadu.tuner.ui.instruments.StringWithInfo
import de.moekadu.tuner.ui.notes.NoteDetectorState
import de.moekadu.tuner.ui.screens.InstrumentEditorData
import de.moekadu.tuner.ui.instruments.StringsState
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class InstrumentEditorViewModel2 @Inject constructor(
    private val pref: PreferenceResources2
) : ViewModel(), InstrumentEditorData {
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

    override val icon = MutableStateFlow(R.drawable.ic_guitar)
    override val name = MutableStateFlow("Test name")

    override var strings = MutableStateFlow(
        persistentListOf(
            StringWithInfo(musicalScale.value.getNote(0), 0)
        )
    )
    override val stringsState = StringsState(0)

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

    fun startTuner() {
        tuner.connect()
    }
    fun stopTuner() {
        tuner.disconnect()
    }
}