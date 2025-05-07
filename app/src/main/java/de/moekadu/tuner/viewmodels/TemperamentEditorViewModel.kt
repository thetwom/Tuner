package de.moekadu.tuner.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteNamesEDOGenerator
import de.moekadu.tuner.temperaments.EditableTemperament
import de.moekadu.tuner.temperaments.Temperament3Custom
import de.moekadu.tuner.temperaments.TemperamentResources
import de.moekadu.tuner.temperaments.TemperamentValidityChecks
import de.moekadu.tuner.ui.temperaments.TemperamentEditorState
import de.moekadu.tuner.ui.temperaments.TemperamentTableLineState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentList

/** Create the table of temperament values and note names, based on a temperament.
 * @param temperament Temperament, based on which the table will be created.
 * @return List with fits the given temperament.
 */
private fun temperamentToTable(temperament: EditableTemperament)
        : PersistentList<TemperamentTableLineState>
{
    val numberOfNotesPerOctave = temperament.noteLines.size - 1
    // use default note names if there is no note name given for any line (and if default notes are available)
    val defaultNoteNames = if (temperament.noteLines.firstOrNull { it?.note != null } == null)
        NoteNamesEDOGenerator.getNoteNames(numberOfNotesPerOctave, null)
    else
        null

    return temperament.noteLines.mapIndexed { index, noteLine ->
        val note = defaultNoteNames?.getOrNull(index % numberOfNotesPerOctave)
            ?:noteLine?.note
        val octave = when {
            note == null -> 4 + (index / numberOfNotesPerOctave)
            note.octave == Int.MAX_VALUE -> 4 + (index / numberOfNotesPerOctave)
            else -> note.octave
        }
        val cent = noteLine?.resolveCentValue()
        val centPrevious = temperament.noteLines.getOrNull(index - 1)?.resolveCentValue()
        TemperamentTableLineState(
            note?.copy(octave = octave),
            noteLine?.cent,
            noteLine?.ratio,
            isFirstLine = index == 0,
            isOctaveLine = index == temperament.noteLines.size - 1,
            decreasingValueError = if (index == 0)
                false
            else if (cent != null && centPrevious != null)
                cent <= centPrevious
            else
                false,
            duplicateNoteError = false // this will be set later inside the view model
        )
    }.toPersistentList()
}

/** Check cent/ratio values for correctness
 * @warning This will (un)set the decreasingValueError within the input list.
 * @param values List of temperament values. The function will set the decreasingValueError
 *   within this list.
 * @return Summary about if there were errors or not.
 */
private fun checkAndSetValueOrderingErrors(values: PersistentList<TemperamentTableLineState>)
: TemperamentValidityChecks.ValueOrdering {
    return TemperamentValidityChecks.checkValueOrderingErrors(
        values.size,
        { values[it].obtainCent() },
        { i, e -> values[i].changeDecreasingValueError(e) }
    )
}

private fun checkAndSetNoteNameErrors(values: PersistentList<TemperamentTableLineState>): TemperamentValidityChecks.NoteNameError {
    return TemperamentValidityChecks.checkNoteNameErrors(
        values.size, {values[it].note}, { i, e ->values[i].changeDuplicateNoteError(e) }
    )
}

/** Check if list of notes is the same as the default note names.
 * @param noteNameList List of notes to be checked.
 * @param defaultNames Default names against which we do the check.
 * @return True if the list is the same as the default note names, else false
 */
private fun checkIfDefaultNoteNames(
    noteNameList: List<MusicalNote>, defaultNames: Array<MusicalNote>?
): Boolean {
    var useDefaultNoteNames = true

    if (defaultNames == null || defaultNames.size != noteNameList.size) {
        useDefaultNoteNames = false
    } else {
        for (i in noteNameList.indices) {
            if (!noteNameList[i].equalsIgnoreOctave(defaultNames[i])) {
                useDefaultNoteNames = false
                break
            }
        }
    }
    return useDefaultNoteNames
}

@HiltViewModel (assistedFactory = TemperamentEditorViewModel.Factory::class)
class TemperamentEditorViewModel @AssistedInject constructor(
    @Assisted temperament: EditableTemperament,
    val pref: TemperamentResources
) : ViewModel(), TemperamentEditorState {
    @AssistedFactory
    interface Factory {
        fun create(temperament: EditableTemperament): TemperamentEditorViewModel
    }

    private val stableId = temperament.stableId

    private val _name = mutableStateOf(temperament.name)
    override val name: State<String> get() = _name

    private val _abbreviation = mutableStateOf(temperament.abbreviation)
    override val abbreviation: State<String> get() = _abbreviation

    private val _description = mutableStateOf(temperament.description)
    override val description: State<String> get() = _description

    private val _numberOfValues = mutableIntStateOf(temperament.noteLines.size - 1)
    override val numberOfValues: State<Int> get() = _numberOfValues

    private val _temperamentValues = mutableStateOf(temperamentToTable(temperament))
    override val temperamentValues: State<PersistentList<TemperamentTableLineState>>
        get() = _temperamentValues

    // Warning: The checkAndSetValueOrderingFunction also sets the increasingValueInfo
    //  within the temperamentValues.
    private var valueOrderingError = checkAndSetValueOrderingErrors(temperamentValues.value)
    private var noteNameError = checkAndSetNoteNameErrors(temperamentValues.value)
    private var _hasErrors = mutableStateOf(
        valueOrderingError != TemperamentValidityChecks.ValueOrdering.Increasing
                || noteNameError != TemperamentValidityChecks.NoteNameError.None
    )
    override val hasErrors: State<Boolean>
        get() = _hasErrors

    override fun onCentValueChanged(index: Int, value: String) {
        temperamentValues.value.getOrNull(index)?.changeCentOrRatio(value)
        valueOrderingError = checkAndSetValueOrderingErrors(temperamentValues.value)
//        Log.v("Tuner", "TemperamentEditorViewModel.modifyCentOrRatioValue: valueOrdering=$valueOrdering")
        checkAndSetError()
    }

    override fun onNoteNameClicked(index: Int, enharmonic: Boolean) {
        temperamentValues.value.forEachIndexed { i, value ->
            value.setNoteEditor(
                if (i != index
                    || (!enharmonic && value.noteEditorState == TemperamentTableLineState.NoteEditorState.Standard)
                    || (enharmonic && value.noteEditorState == TemperamentTableLineState.NoteEditorState.Enharmonic)
                ) {
                    TemperamentTableLineState.NoteEditorState.Off
                } else if (enharmonic) {
                    TemperamentTableLineState.NoteEditorState.Enharmonic
                } else {
                    TemperamentTableLineState.NoteEditorState.Standard
                }
            )
        }
    }

    override fun onCloseNoteEditorClicked(index: Int) {
        temperamentValues.value.getOrNull(index)?.setNoteEditor(
            TemperamentTableLineState.NoteEditorState.Off
        )
    }

    private fun checkAndSetError() {
        _hasErrors.value = (
                valueOrderingError != TemperamentValidityChecks.ValueOrdering.Increasing
                        || noteNameError != TemperamentValidityChecks.NoteNameError.None
                )
    }

    fun changeNumberOfValues(numberOfValues: Int) {
        val oldTemperamentValues  = temperamentValues.value
        val oldNumValues = oldTemperamentValues.size - 1 // num notes per octave + octave note

        if (temperamentValues.value.size == oldNumValues)
            return

        val newNoteNames = NoteNamesEDOGenerator.getNoteNames(numberOfValues, null)
        _temperamentValues.value = List(numberOfValues + 1) {
            TemperamentTableLineState(
                note = if (it == numberOfValues)
                    newNoteNames?.getOrNull(0)?.copy(octave = 5)
                else
                    newNoteNames?.getOrNull(it)?.copy(octave = 4),
                cent = if (it == numberOfValues)
                    oldTemperamentValues.lastOrNull()?.cent
                else
                    oldTemperamentValues.getOrNull(it)?.cent,
                ratio = if (it == numberOfValues)
                    oldTemperamentValues.lastOrNull()?.ratio
                else
                    oldTemperamentValues.getOrNull(it)?.ratio,
                isFirstLine = it == 0,
                isOctaveLine = it == numberOfValues,
                decreasingValueError = false,
                duplicateNoteError = false
            )
        }.toPersistentList()

        _numberOfValues.intValue = numberOfValues

        valueOrderingError = checkAndSetValueOrderingErrors(temperamentValues.value)
        noteNameError = checkAndSetNoteNameErrors(temperamentValues.value)
        checkAndSetError()
    }

    override fun modifyName(value: String) {
        _name.value = value
    }

    override fun modifyAbbreviation(value: String) {
        _abbreviation.value = value
    }

    override fun modifyDescription(value: String) {
        _description.value = value
    }

    override fun modifyNote(index: Int, note: MusicalNote) { // }, isReferenceNote: Boolean) {
//        Log.v("Tuner", "TemperamentEditorViewModel.modifyNote: index = $index, note = $note")
        temperamentValues.value.getOrNull(index)?.let {
            it.note = note
            // it.isReferenceNote = isReferenceNote
        }
        // also set octave value, if first value ist set
        if (index == 0) {
            temperamentValues.value.lastOrNull()?.let {
                it.note = note.copy(octave = (it.note?.octave ?: 4) + 1)
            }
        }
        noteNameError = checkAndSetNoteNameErrors(temperamentValues.value)
        checkAndSetError()
    }

    fun saveTemperament(): Boolean {
        val values = temperamentValues.value
        val noteNameList = values.dropLast(1).map {
            it.note ?: return false
        }
        val predefinedNoteNames = NoteNamesEDOGenerator.getNoteNames(
            values.size - 1, null
        )
        val useDefaultNoteNames = checkIfDefaultNoteNames(
            noteNameList, predefinedNoteNames?.notes
        )

//        val defaultReferenceNote = values.firstOrNull {
//            (it.note?.base == BaseNote.A && it.note?.modifier == NoteModifier.None)
//                    || (it.note?.enharmonicBase == BaseNote.A && it.note?.enharmonicModifier == NoteModifier.None)
//        }?.note ?: values.firstOrNull{ it.note != null }?.note ?: return false

        val noteNames = if (useDefaultNoteNames) {
            predefinedNoteNames!!.notes
        } else {
            noteNameList.toTypedArray()
            //NoteNames2(
//                notes = noteNameList.toTypedArray(),
//                defaultReferenceNote =  defaultReferenceNote,
//                firstNoteOfOctave = noteNameList[0]
//            )
        }

        val ratios = values.map { it.obtainRatio() }
//        val hasRatios = !possibleRatios.contains(null)
//        val ratios = if (hasRatios) {
//            possibleRatios.map { it ?: RationalNumber(1,1) }
//        } else {
//            null
//        }
        val cents = values.map { it.obtainCent() ?: return false }

//        var hasEqualDivisions = true
//        for (i in 0 until cents.size - 2)
//            if (2 * cents[i+1] - cents[i] - cents[i+2] > 0.01)
//                hasEqualDivisions = false

        val temperament = Temperament3Custom(
            _name = name.value,
            _abbreviation = abbreviation.value,
            _description = description.value,
            cents = cents.toDoubleArray(),
            _rationalNumbers = ratios.toTypedArray(),
            //equalOctaveDivision = if (hasEqualDivisions) cents.size - 1 else null,
            _noteNames = noteNames,
            stableId = stableId
        )

        pref.addNewOrReplaceTemperament(temperament)

        return true
    }

}
