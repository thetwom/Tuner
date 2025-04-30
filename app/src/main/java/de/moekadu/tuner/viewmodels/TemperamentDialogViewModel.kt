package de.moekadu.tuner.viewmodels

import androidx.compose.runtime.IntState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.hilt.ApplicationScope
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.temperaments.TemperamentResources
import de.moekadu.tuner.notenames.generateNoteNames
import de.moekadu.tuner.temperaments.Temperament3
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
    override val temperament: State<Temperament3> get() = _temperament

//    private val _noteNames = mutableStateOf(initialMusicalScale.noteNames)
//    override val noteNames: State<NoteNames> get() = _noteNames

    override val defaultTemperament: Temperament3 get() = pref.defaultTemperament

    override val rootNotes = derivedStateOf { temperament.value.possibleRootNotes() }

    private val _selectedRootNoteIndex = mutableIntStateOf(
        rootNotes.value
            .indexOfFirst { MusicalNote.notesEqualIgnoreOctave(it, initialMusicalScale.rootNote) }
            .coerceAtLeast(0)
    )
    override val selectedRootNoteIndex: IntState get() = _selectedRootNoteIndex

    override fun setNewTemperament(temperament: Temperament3) {
        val oldRootNoteIndex = selectedRootNoteIndex.intValue
        val oldRootNote = rootNotes.value[oldRootNoteIndex]
        val newNoteNames = temperament.possibleRootNotes()

        _selectedRootNoteIndex.intValue = newNoteNames
            .indexOfFirst { MusicalNote.notesEqualIgnoreOctave(it, oldRootNote) }
            .coerceAtLeast(0)

        _temperament.value = temperament
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