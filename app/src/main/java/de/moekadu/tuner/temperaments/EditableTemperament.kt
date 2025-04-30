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

import android.content.Context
import de.moekadu.tuner.misc.GetTextFromString
import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteModifier
import de.moekadu.tuner.notenames.NoteNames
import de.moekadu.tuner.notenames.NoteNamesEDOGenerator
import de.moekadu.tuner.notenames.generateNoteNames
import kotlinx.serialization.Serializable

/** Temperament with note names, which can be incomplete, but is allowed for editing. */
@Serializable
class EditableTemperament(
    val name: String,
    val abbreviation: String,
    val description: String,
    val noteLines: Array<NoteLineContents?>,
    val stableId: Long
) {
    @Serializable
    data class NoteLineContents(
        val note: MusicalNote?,
        val cent: Double?,
        val ratio: RationalNumber?
    ) {
        /** Obtain cent value from cent or ratio, what is available.
         * @return Cent value, or null, if neither cent nor ratio is available.
         */
        fun resolveCentValue(): Double? {
            return when {
                ratio != null -> ratioToCents(ratio.toDouble())
                cent != null -> cent
                else -> null
            }
        }
    }
}

/** Convert temperament with note names to editable temperament.
 * @param context Context required to resolve potential string resource ids.
 * @param name New temperament or null to use the name from the input.
 * @param stableId New stable or null to use the name from the input.
 * @return Editable temperament.
 */
fun Temperament3.toEditableTemperament(
    context: Context,
    name: String? = null,
    stableId: Long? = null
    ): EditableTemperament {

    val rootNoteResolved = possibleRootNotes()[0]
    val noteNamesResolved = noteNames(rootNoteResolved)
    val _cents = cents()
    val _ratios = rationalNumbers()
    val noteLines = Array<EditableTemperament.NoteLineContents?>(size + 1) {
        val octave = 4 + it / size
        val noteIndex = it % size
        EditableTemperament.NoteLineContents(
            noteNamesResolved[noteIndex].copy(octave = octave),
            _cents[it],
            _ratios?.getOrNull(it)
        )
    }
    return EditableTemperament(
        name = name ?: this.name.value(context),
        abbreviation = this.abbreviation.value(context),
        description = this.description.value(context),
        noteLines = noteLines,
        stableId = stableId ?: this.stableId
    )
}

fun EditableTemperament.hasErrors(): Boolean {
    if (noteLines.isEmpty())
        return true
    // check that all lines could be successfully parsed
    if (noteLines.contains(null))
        return true
    // check that there is a valid cent or ratio value
    if (noteLines.firstOrNull { it?.cent == null && it?.ratio == null  } != null)
        return true
    // check that values are increasing
    val orderingError = TemperamentValidityChecks.checkValueOrderingErrors(
        noteLines.size,
        {
            byteArrayOf()
            val ratio = noteLines[it]?.ratio?.toDouble()
            if (ratio != null) ratioToCents(ratio.toDouble()) else noteLines[it]?.cent
        },
        null
    )
    if (orderingError != TemperamentValidityChecks.ValueOrdering.Increasing)
        return true
    // check if we can use predefined notes
    val predefinedNotes = generateNoteNames(noteLines.size - 1)

    if (predefinedNotes != null)
        return false
    // if predefined notes are not possible, check the validity of the user defined notes
    val noteNameError = TemperamentValidityChecks.checkNoteNameErrors(
        noteLines.size,
        { noteLines[it]?.note },
        null
    )
    if (noteNameError != TemperamentValidityChecks.NoteNameError.None)
        return true
    return false
}

fun EditableTemperament.toTemperament3Custom(): Temperament3Custom? {
    if (noteLines.isEmpty())
        return null

    // fill different arrays
    val numberOfNotesPerOctave = noteLines.size - 1
    val ratios = ArrayList<RationalNumber?>()
    val cents = ArrayList<Double>()
    val notes = ArrayList<MusicalNote?>()

    for (line in noteLines) {
        ratios.add(line?.ratio)
        val centFromRatio = if (line?.ratio != null)
            ratioToCents(line.ratio.toDouble())
        else
            null
        val cent = line?.cent ?: centFromRatio ?: return null
        cents.add(cent)
        notes.add(line?.note)
    }

    // resolve note names, we try to use predefined note names
    val predefinedNotes = NoteNamesEDOGenerator.getNoteNames(numberOfNotesPerOctave, null)
    if (notes.contains(null) && predefinedNotes == null)
        return null

    val resolvedNotes = if (notes.contains(null)) {
        null // this branch uses the predefined notes, which happens by specifying null
    } else if (predefinedNotes == null) {
        notes
    } else {
        var usePredefined = true
        for (i in 0 until numberOfNotesPerOctave) {
            if (!notesEqualCheck(predefinedNotes.notes[i], notes[i])) {
                usePredefined = false
                break
            }
        }
        if (usePredefined) null else notes
    }

    return Temperament3Custom(
        _name = name,
        _abbreviation = abbreviation,
        _description = description,
        cents = cents.toDoubleArray(),
        _rationalNumbers = ratios.toTypedArray(),
        _noteNames = resolvedNotes?.toTypedArray()?.requireNoNulls(),
        stableId = Temperament3.NO_STABLE_ID
    )
//    // construct the temperament
//    return if (!ratios.contains(null)) {
//        TemperamentWithNoteNames2(
//            Temperament2(
//                name = GetTextFromString(name),
//                abbreviation = GetTextFromString(abbreviation),
//                description = GetTextFromString(description),
//                rationalNumbers = ratios.map { it!! }.toTypedArray(),
//                stableId = Temperament2.NO_STABLE_ID
//            ),
//            noteNames = noteNames
//        )
//    } else {
//        TemperamentWithNoteNames2(
//            Temperament2(
//                name = GetTextFromString(name),
//                abbreviation = GetTextFromString(abbreviation),
//                description = GetTextFromString(description),
//                cents = cents.toDoubleArray(),
//                stableId = Temperament2.NO_STABLE_ID
//            ),
//            noteNames = noteNames
//        )
//    }
}

// TODO: this should be be somehow be part of the MusicalNote class itself!
private fun notesEqualCheck(note: MusicalNote?, other: MusicalNote?): Boolean {
    return (note?.base == other?.base &&
            note?.modifier == other?.modifier &&
            note?.octaveOffset == other?.octaveOffset &&
            note?.enharmonicBase == other?.enharmonicBase &&
            note?.enharmonicModifier == other?.enharmonicModifier &&
            note?.enharmonicOctaveOffset == other?.enharmonicOctaveOffset
            )
}

//private fun detectReferenceNote(notes: List<MusicalNote?>): MusicalNote? {
//    return notes.firstOrNull{
//        (it?.base == BaseNote.A && it.modifier == NoteModifier.None) ||
//                (it?.enharmonicBase == BaseNote.A && it.enharmonicModifier == NoteModifier.None)
//    } ?: notes.firstOrNull { it != null }
//}