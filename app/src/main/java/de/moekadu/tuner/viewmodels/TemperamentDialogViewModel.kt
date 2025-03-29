package de.moekadu.tuner.viewmodels

import androidx.compose.runtime.IntState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.hilt.ApplicationScope
import de.moekadu.tuner.temperaments.NoteNames
import de.moekadu.tuner.temperaments.Temperament
import de.moekadu.tuner.temperaments.TemperamentResources
import de.moekadu.tuner.temperaments.TemperamentWithNoteNames
import de.moekadu.tuner.temperaments.generateNoteNames
import de.moekadu.tuner.ui.temperaments.TemperamentDialogState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemperamentDialogViewModel  @Inject constructor(
    val pref: TemperamentResources,
    @ApplicationScope val applicationScope: CoroutineScope
) : ViewModel(), TemperamentDialogState {
    private val initialMusicalScale = pref.musicalScale.value

    private val _temperament = mutableStateOf(initialMusicalScale.temperament)
    override val temperament: State<Temperament> get() = _temperament

    private val _noteNames = mutableStateOf(initialMusicalScale.noteNames)
    override val noteNames: State<NoteNames> get() = _noteNames

    override val defaultTemperament: TemperamentWithNoteNames get() = pref.defaultTemperament

    private val _selectedRootNoteIndex = mutableIntStateOf(
        initialMusicalScale.noteNames.getNoteIndex(initialMusicalScale.rootNote)
            .coerceAtLeast(0)
    )
    override val selectedRootNoteIndex: IntState get() = _selectedRootNoteIndex

    override fun setNewTemperament(temperamentWithNoteNames: TemperamentWithNoteNames) {
        val oldRootNoteIndex = selectedRootNoteIndex.intValue
        val oldRootNote = noteNames.value[oldRootNoteIndex]
        val newNoteNames = temperamentWithNoteNames.noteNames
            ?: generateNoteNames(temperamentWithNoteNames.temperament.numberOfNotesPerOctave)!!

        _selectedRootNoteIndex.intValue =
            newNoteNames.getNoteIndex(oldRootNote).coerceAtLeast(0)

        _temperament.value = temperamentWithNoteNames.temperament
        _noteNames.value = newNoteNames
    }

    override fun selectRootNote(index: Int) {
        _selectedRootNoteIndex.intValue = index
    }

    init {
        // make sure that the currently active item will follow changes when it was changed in
        // the temperament editor
        viewModelScope.launch {
            pref.customTemperaments.collect { custom ->
                val stableId = temperament.value.stableId
                custom.firstOrNull { it.stableId == stableId }?.let { temperament ->
                    setNewTemperament(temperament)
                }
            }
        }
    }
}