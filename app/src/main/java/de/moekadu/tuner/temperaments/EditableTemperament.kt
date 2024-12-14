package de.moekadu.tuner.temperaments

import android.content.Context
import de.moekadu.tuner.misc.StringOrResId
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
fun TemperamentWithNoteNames.toEditableTemperament(
    context: Context,
    name: String? = null,
    stableId: Long? = null
    ): EditableTemperament {
    val numberOfNotesPerOctave = temperament.numberOfNotesPerOctave
    val noteNamesResolved = noteNames ?: getSuitableNoteNames(numberOfNotesPerOctave)
    val noteLines = Array<EditableTemperament.NoteLineContents?>(numberOfNotesPerOctave + 1) {
        val octave = 4 + it / numberOfNotesPerOctave
        val noteIndex = it % numberOfNotesPerOctave
        EditableTemperament.NoteLineContents(
            noteNamesResolved?.notes?.get(noteIndex)?.copy(octave = octave),
            temperament.cents[it],
            temperament.rationalNumbers?.get(it)
        )
    }
    return EditableTemperament(
        name = name ?: temperament.name.value(context),
        abbreviation = temperament.abbreviation.value(context),
        description = temperament.description.value(context),
        noteLines = noteLines,
        stableId = stableId ?: temperament.stableId
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
            val ratio = noteLines[it]?.ratio?.toDouble()
            if (ratio != null) ratioToCents(ratio.toDouble()) else noteLines[it]?.cent
        },
        null
    )
    if (orderingError != TemperamentValidityChecks.ValueOrdering.Increasing)
        return true
    // check if we can use predefined notes
    val predefinedNotes = getSuitableNoteNames(noteLines.size - 1)

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

fun EditableTemperament.toTemperamentWithNoteNames(): TemperamentWithNoteNames? {
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
    val predefinedNotes = getSuitableNoteNames(numberOfNotesPerOctave)
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

    val noteNames = if (resolvedNotes == null) {
        null
    } else {
        val referenceNote = detectReferenceNote(resolvedNotes) ?: return null
        NoteNames(
            resolvedNotes.map { it!! }.toTypedArray(),
            referenceNote
        )
    }

    // construct the temperament
    return if (!ratios.contains(null)) {
        TemperamentWithNoteNames(
            Temperament.create(
                name = StringOrResId(name),
                abbreviation = StringOrResId(abbreviation),
                description = StringOrResId(description),
                rationalNumbers = ratios.map { it!! }.toTypedArray(),
                stableId = Temperament.NO_STABLE_ID
            ),
            noteNames = noteNames
        )
    } else {
        TemperamentWithNoteNames(
            Temperament.create(
                name = StringOrResId(name),
                abbreviation = StringOrResId(abbreviation),
                description = StringOrResId(description),
                cents = cents.toDoubleArray(),
                stableId = Temperament.NO_STABLE_ID
            ),
            noteNames = noteNames
        )
    }
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

private fun detectReferenceNote(notes: List<MusicalNote?>): MusicalNote? {
    return notes.firstOrNull{
        (it?.base == BaseNote.A && it.modifier == NoteModifier.None) ||
                (it?.enharmonicBase == BaseNote.A && it.enharmonicModifier == NoteModifier.None)
    } ?: notes.firstOrNull { it != null }
}