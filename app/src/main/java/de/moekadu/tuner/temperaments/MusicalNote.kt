package de.moekadu.tuner.temperaments

// user input:
//  - reference-note + frequency, z.B. A4 + 440Hz
//  - root note z.B. C
// Summary:
//  - Class must contain something like:
//    -> get_notes() -> we get a list of notes, z.B. C, C#, ...
//  - Maybe have different classes for using flat and sharps
//
// We must tightly couple this with the temperament
//  As temperament input, we use:
//    -> note name + octave of reference note
//    -> frequency of reference note
//    -> root note name
//    -> minimum and maximum frequency

enum class BaseNote {
    C, D, E, F, G, A, B, None
}

enum class NoteModifier {
    Sharp, Flat, None
}

/** Representation of a musical note.
 * @param base Base of note (C, D, E, ...).
 * @param modifier Modifier of note (none, sharp, flat, ...).
 * @param octave Octave index if the note. Note, modifiers are not able to repesent another octave.
 *   E.g. if you want to modify C4 with a flat, it will become Cb3 (and not Cb4), since Cb3 is part
 *   of the lower octave. So if you really want to decrease a note a half tone, you must always
 *   check if you have to adapt the octave.
 * @param enharmonicBase base of enharmonic note, which represents the same note. If no enharmonic
 *   should is available, BaseNote.None must be used.
 * @param enharmonicModifier modifier of enharmonic note, which represents the same note. This value
 *   has no meaning if enharmonicBase is BaseNote.None
 */
data class MusicalNote(val base: BaseNote, val modifier: NoteModifier, val octave: Int = Int.MAX_VALUE,
                       val enharmonicBase: BaseNote = BaseNote.None, val enharmonicModifier: NoteModifier = NoteModifier.None) {

    fun switchEnharmonic(): MusicalNote {
        if (enharmonicBase == BaseNote.None)
            return this
        return this.copy(base = enharmonicBase, modifier = enharmonicModifier, enharmonicBase = base, enharmonicModifier = modifier)
    }
    companion object {
        /** Check if two notes are equal, where we also take enharmonics into account.
         * E.g. if the base values of one note are the same as the enharmonic of the other note
         *   we also return true.
         * @param first First note to compare.
         * @param second Second note to compare.
         * @return True for the following cases:
         *  - first and second are both null
         *  - base, modifier and octave are the same for both notes
         *  - base, modifier, octave of one note are the same as enharmonicBase, enharmonicModifier, octave
         *     of the other note are the same.
         *  - enharmonicBase and enharmonic modifier and octave are the same for both notes
         *  else false.
         */
        fun notesEqual(first: MusicalNote?, second: MusicalNote?): Boolean {
            if (first == null && second == null)
                return true
            else if (notesEqualIgnoreOctave(first, second) && first?.octave == second?.octave)
                return true
            return false
        }

        /** Check if two notes are the same, while ignoring the octave.
         * We will also return true, if the enharmonic matches the note.
         * @param first First note to compare.
         * @param second Second note to compare.
         * @return True if base and modifier are the same or if base and modifier are the same
         *   as the base of modifier of the enharmonic of the other note. If both notes are null
         *   we also return false.
         */
        fun notesEqualIgnoreOctave(first: MusicalNote?, second: MusicalNote?): Boolean {
            if (first == null || second == null)
                return false
            else if (first.base == second.base && first.modifier == second.modifier)
                return true
            else if (first.base == second.enharmonicBase && first.modifier == second.enharmonicModifier)
                return true
            else if (first.enharmonicBase == second.base && first.enharmonicModifier == second.modifier)
                return true
            else if (first.enharmonicBase == second.enharmonicBase && first.enharmonicModifier == second.enharmonicModifier)
                return true
            return false
        }
    }
}

class NoteNameScale(
    val referenceNote: MusicalNote,
    val notes: Array<MusicalNote>
) {
    // TODO: test getNoteOfIndex and getIndexOfNote
    private val referenceNoteIndexWithinOctave = notes.indexOfFirst { MusicalNote.notesEqualIgnoreOctave(it, referenceNote) }

    fun getNoteOfIndex(noteIndex: Int): MusicalNote {
        val octave = (noteIndex + referenceNoteIndexWithinOctave) / notes.size
        val localNoteIndex = (noteIndex + referenceNoteIndexWithinOctave) % notes.size
        return notes[localNoteIndex].copy(octave = octave)
    }

    fun getIndexOfNote(musicalNote: MusicalNote): Int {
        val localNoteIndex = notes.indexOfFirst { MusicalNote.notesEqualIgnoreOctave(it, musicalNote) }
        return (musicalNote.octave - referenceNote.octave) * notes.size + localNoteIndex - referenceNoteIndexWithinOctave
    }

    fun switchEnharmonic(): NoteNameScale {
        return NoteNameScale(referenceNote.switchEnharmonic(), notes.map {it.switchEnharmonic()}.toTypedArray())
    }
}

val noteNameScale12ToneSharp = NoteNameScale(
    MusicalNote(BaseNote.A, NoteModifier.None, 4),
    arrayOf(
        MusicalNote(base = BaseNote.C, modifier = NoteModifier.None),
        MusicalNote(base = BaseNote.C, modifier = NoteModifier.Sharp, enharmonicBase = BaseNote.D, enharmonicModifier = NoteModifier.Flat),
        MusicalNote(base = BaseNote.D, modifier = NoteModifier.None),
        MusicalNote(base = BaseNote.D, modifier = NoteModifier.Sharp, enharmonicBase = BaseNote.E, enharmonicModifier = NoteModifier.Flat),
        MusicalNote(base = BaseNote.E, modifier = NoteModifier.None),
        MusicalNote(base = BaseNote.F, modifier = NoteModifier.None),
        MusicalNote(base = BaseNote.F, modifier = NoteModifier.Sharp, enharmonicBase = BaseNote.G, enharmonicModifier = NoteModifier.Flat),
        MusicalNote(base = BaseNote.G, modifier = NoteModifier.None),
        MusicalNote(base = BaseNote.G, modifier = NoteModifier.Sharp, enharmonicBase = BaseNote.A, enharmonicModifier = NoteModifier.Flat),
        MusicalNote(base = BaseNote.A, modifier = NoteModifier.None),
        MusicalNote(base = BaseNote.A, modifier = NoteModifier.Sharp, enharmonicBase = BaseNote.B, enharmonicModifier = NoteModifier.Flat),
        MusicalNote(base = BaseNote.B, modifier = NoteModifier.None)
    )
)

val noteNameScale12ToneFlat = noteNameScale12ToneSharp.switchEnharmonic()
