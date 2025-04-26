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
package de.moekadu.tuner.temperaments

import de.moekadu.tuner.notenames.MusicalNote

object TemperamentValidityChecks {
    /** Possible errors for the different cent/ratio values of the temperament table. */
    enum class ValueOrdering {
        Increasing,
        Unordered,
        Undefined,
    }

    /** Check cent/ratio values for correctness
     * @param numberOfNotes For number of notes, we expect, that the first and the octave value
     *   is given, so this should be numberOfNotesPerOctave + 1
     * @param obtainCent Obtain cent at a given index, this must take indices 0 .. numberOfNotes-1.
     *   Normally this would take an array like { centArray[it] }
     * @param decreasingValueCallback Callback with info if a cent value is decreasing or not. This
     *   will be called for each cent value. Set this to null to ignore this.
     * @return Summary about if there were errors or not. Undefined overrules Unordered.
     */
    fun checkValueOrderingErrors(
        numberOfNotes: Int,
        obtainCent: (index: Int) -> Double?,
        decreasingValueCallback: ((index: Int, isValueDecreasing: Boolean) -> Unit)?
    ) : ValueOrdering {
        if (numberOfNotes < 2) {
            if (decreasingValueCallback != null) {
                for (i in 0 until numberOfNotes)
                    decreasingValueCallback(i, false)
            }
            return ValueOrdering.Increasing
        }
        if (decreasingValueCallback != null)
            decreasingValueCallback(0, false)

        var error = ValueOrdering.Increasing

        for (i in 1 until numberOfNotes) {
            val centPrevious = obtainCent(i - 1)
            val cent = obtainCent(i)
//        Log.v("Tuner", "checkAndSetValueOrderingErrors: $i : centPrev=$centPrevious, cent=$cent")
            if (centPrevious == null || cent == null) {
                if (decreasingValueCallback != null)
                    decreasingValueCallback(i, false)
                // undefined outrules other errors
                error = ValueOrdering.Undefined
            } else if (cent <= centPrevious) {
                if (decreasingValueCallback != null)
                    decreasingValueCallback(i, true)
                // do not overwrite an undefined-error
                if (error != ValueOrdering.Undefined)
                    error = ValueOrdering.Unordered
            } else {
                if (decreasingValueCallback != null)
                    decreasingValueCallback(i, false)
            }
        }
        return error
    }

    /** Errors with note names. */
    enum class NoteNameError {
        None, /**< No error */
        Duplicates, /**< Note name appears more than once */
        Undefined /**< Note name is not defined. */
    }

    /** Check for problems with note names.
     * @param numberOfNotes For number of notes, we expect, that the first and the octave value
     *   is given, so this should be numberOfNotesPerOctave + 1
     * @param obtainNote Obtain note at a given index, this must take indices 0 .. numberOfNotes-1.
     *   Normally this would take an array like { noteArray[it] }
     * @param duplicateNoteCallback Callback with info if a note is duplicate or not. This will
     *   be called for each note. Set this to null to ignore this.
     * @return Error, where the NoteNameError.Undefined overrules NoteNameError.Duplicates
     */
    fun checkNoteNameErrors(
        numberOfNotes: Int,
        obtainNote: (index: Int) -> MusicalNote?,
        duplicateNoteCallback: ((index: Int, isDuplicate: Boolean) -> Unit)?
    ): NoteNameError {
        var error = NoteNameError.None
        val duplicateNoteErrors = Array(numberOfNotes) { false }
        // don't check last note, since this is the next octave
        for (i in 0 until numberOfNotes - 1) {
            val note = obtainNote(i)
            if (note == null) {
                error = NoteNameError.Undefined
            } else {
                for (j in i + 1 until numberOfNotes - 1) {
                    val noteNext = obtainNote(j)
                    if (noteNext != null && MusicalNote.notesEqualIgnoreOctave(note, noteNext)) {
                        duplicateNoteErrors[i] = true
                        duplicateNoteErrors[j] = true
                        if (error != NoteNameError.Undefined)
                            error = NoteNameError.Duplicates
                    }
                }
            }
        }
        if (duplicateNoteCallback != null) {
            for (i in 0 until numberOfNotes - 1)
                duplicateNoteCallback(i, duplicateNoteErrors[i])
        }

        return error
    }
}