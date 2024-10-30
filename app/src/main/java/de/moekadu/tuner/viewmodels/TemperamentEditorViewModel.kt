package de.moekadu.tuner.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.misc.StringOrResId
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.RationalNumber
import de.moekadu.tuner.temperaments2.NoteNames
import de.moekadu.tuner.temperaments2.Temperament
import de.moekadu.tuner.temperaments2.TemperamentResources
import de.moekadu.tuner.temperaments2.getSuitableNoteNames
import de.moekadu.tuner.ui.screens.TemperamentEditorState
import de.moekadu.tuner.ui.temperaments.TemperamentTableLineState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentList

private fun temperamentToTable(temperament: TemperamentResources.TemperamentWithNoteNames)
        : PersistentList<TemperamentTableLineState>
{
    val numberOfNotesPerOctave = temperament.temperament.numberOfNotesPerOctave
    val noteNames = temperament.noteNames ?: getSuitableNoteNames(numberOfNotesPerOctave)
    return temperament.temperament.cents.mapIndexed { index, cent ->
        val isOctaveLine = index == numberOfNotesPerOctave
        val octave = noteNames?.defaultReferenceNote?.octave ?: 4
        TemperamentTableLineState(
            note = if (isOctaveLine)
                noteNames?.getOrNull(0)?.copy(octave = octave + 1)
            else
                noteNames?.getOrNull(index)?.copy(octave = octave),
            cent = cent,
            ratio = temperament.temperament.rationalNumbers?.get(index),
            isReferenceNote = if (isOctaveLine || noteNames == null)
                false
            else
                MusicalNote.notesEqualIgnoreOctave(noteNames[index], noteNames.defaultReferenceNote),
            isOctaveLine = isOctaveLine
        )
    }.toPersistentList()
}

@HiltViewModel (assistedFactory = TemperamentEditorViewModel.Factory::class)
class TemperamentEditorViewModel @AssistedInject constructor(
    @Assisted temperament: TemperamentResources.TemperamentWithNoteNames,
    val pref: TemperamentResources
) : ViewModel(), TemperamentEditorState {
    @AssistedFactory
    interface Factory {
        fun create(temperament: TemperamentResources.TemperamentWithNoteNames): TemperamentEditorViewModel
    }

    private val stableId = temperament.stableId

    private var useDefaultNoteNames = (temperament.noteNames == null) // TODO: add condition, that it exists!
    private val _name = mutableStateOf(temperament.temperament.name)
    override val name: State<StringOrResId> get() = _name

    private val _abbreviation = mutableStateOf(temperament.temperament.abbreviation)
    override val abbreviation: State<StringOrResId> get() = _abbreviation

    private val _description = mutableStateOf(temperament.temperament.description)
    override val description: State<StringOrResId> get() = _description

    private val _numberOfValues = mutableIntStateOf(temperament.temperament.numberOfNotesPerOctave)
    override val numberOfValues: State<Int> get() = _numberOfValues

    private val _temperamentValues = mutableStateOf(temperamentToTable(temperament))
    override val temperamentValues: State<PersistentList<TemperamentTableLineState>>
        get() = _temperamentValues

    fun changeDescription(name: String, abbreviation: String, description: String) {
        _name.value = StringOrResId(name)
        _abbreviation.value = StringOrResId(abbreviation)
        _description.value = StringOrResId(description)
    }

    fun changeNumberOfValues(numberOfValues: Int) {
        val oldTemperamentValues  = temperamentValues.value
        val oldNumValues = oldTemperamentValues.size - 1 // num notes per octave + octave note
        _temperamentValues.value = oldTemperamentValues.mutate { mutated ->
            val octaveLine = oldTemperamentValues.last()
            if (numberOfValues < oldNumValues) {
                mutated.subList(numberOfValues + 1, oldNumValues + 1).clear()
                mutated[numberOfValues] = octaveLine
            } else {
                val defaultNoteNames = if (useDefaultNoteNames)
                    getSuitableNoteNames(numberOfValues)
                else
                    null
                for (i in oldNumValues until numberOfValues) {
                    mutated.add(i, TemperamentTableLineState(
                        note = defaultNoteNames?.getOrNull(i),
                        cent = null,
                        ratio = null,
                        isReferenceNote = false,
                        isOctaveLine = false
                    ))
                }
                mutated[numberOfValues] = octaveLine
            }
        }
        _numberOfValues.intValue = numberOfValues
    }

    fun modifyNote(index: Int, note: MusicalNote, isReferenceNote: Boolean) {
        useDefaultNoteNames = false
        temperamentValues.value.getOrNull(index)?.let {
            it.note = note
            it.isReferenceNote = isReferenceNote
        }

        if (isReferenceNote) {
            temperamentValues.value.forEachIndexed { i, line ->
                line.isReferenceNote = (i == index)
                line.note = line.note?.copy(octave = note.octave)
            }
        }
    }

    fun modifyCentOrRatioValue(index: Int, value: String) {
        temperamentValues.value.getOrNull(index)?.changeCentOrRatio(value)
    }

    // TODO: if reference note is unset, we must display an error
    fun saveTemperament(): Boolean {
        val values = temperamentValues.value
        val noteNameList = values.map {
            it.note ?: return false
        }
        val predefinedNoteNames = getSuitableNoteNames(values.size - 1)

        var useDefaultNoteNames = true
        if (predefinedNoteNames == null) {
            useDefaultNoteNames = false
        } else {
            for (i in 0 until values.size - 1) {
                if (!MusicalNote.notesEqualIgnoreOctave(predefinedNoteNames[i], noteNameList[i])) {
                    useDefaultNoteNames = false
                    break
                }
            }
        }
        val defaultReferenceNote = values.firstOrNull { it.isReferenceNote }?.note ?: return false
        val noteNames = if (useDefaultNoteNames) {
            predefinedNoteNames
        } else {
            NoteNames(
                name = StringOrResId(""),
                description = StringOrResId(""),
                notes = noteNameList.toTypedArray(),
                defaultReferenceNote =  defaultReferenceNote,
                stableId = stableId
            )
        }

        val possibleRatios = values.map { it.obtainRatio() }
        val hasRatios = !possibleRatios.contains(null)
        val ratios = if (hasRatios) {
            possibleRatios.map { it ?: RationalNumber(1,1) }
        } else {
            null
        }
        // TODO: we must ensure that we have valid number, otherwise saveTemperament is not allowed to be called
        val cents = ratios?.map { it.toDouble() }
            ?: values.map { it.obtainCent() ?: 0.0 }

        var hasEqualDivisions = true
        for (i in 0 until cents.size - 2)
            if (2 * cents[i+1] - cents[i] - cents[i+2] > 0.01)
                hasEqualDivisions = false

        val temperament = Temperament(
            name = name.value,
            abbreviation = abbreviation.value,
            description = description.value,
            cents = cents.toDoubleArray(),
            rationalNumbers = ratios?.toTypedArray(),
            circleOfFifths = null,
            equalOctaveDivision = if (hasEqualDivisions) cents.size - 1 else null,
            stableId = stableId
        )

        pref.addNewOrReplaceTemperament(
            TemperamentResources.TemperamentWithNoteNames(temperament, noteNames)
        )
        return true
    }

}
