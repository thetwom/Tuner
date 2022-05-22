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
    C, D, E, F, G, A, B
}

enum class NoteModifier {
    Sharp, Flat, None
}

data class MusicalNote(val base: BaseNote, val modifier: NoteModifier, val octave: Int = Int.MAX_VALUE) {
    /** Check if the note name is the same, while ignoring the octave.
     * @param other Other note against we are comparing
     * @return True if all attributes except the octave is the same. (We fully ignore the octave for the comparison)
     */
    fun noteNameEquals(other: MusicalNote?): Boolean {
        return other?.base == base && other.modifier == modifier
    }
}

class MusicalNoteScale(
    val referenceNote: MusicalNote,
    val notes: Array<MusicalNote>
) {
    // TODO: what to do for enharmonic substitutions?
    private val referenceNoteIndexWithinOctave = notes.indexOfFirst { it.noteNameEquals(referenceNote) }

    fun getNoteOfIndex(noteIndex: Int): MusicalNote {
        val octave = (noteIndex - referenceNoteIndexWithinOctave) / notes.size
        val localNoteIndex = (noteIndex - referenceNoteIndexWithinOctave) % notes.size
    }
}

val noteSet12ToneSharp = arrayOf(
    MusicalNote(BaseNote.C, NoteModifier.None),
    MusicalNote(BaseNote.C, NoteModifier.Sharp),
    MusicalNote(BaseNote.D, NoteModifier.None),
    MusicalNote(BaseNote.D, NoteModifier.Sharp),
    MusicalNote(BaseNote.E, NoteModifier.None),
    MusicalNote(BaseNote.F, NoteModifier.None),
    MusicalNote(BaseNote.F, NoteModifier.Sharp),
    MusicalNote(BaseNote.G, NoteModifier.None),
    MusicalNote(BaseNote.G, NoteModifier.Sharp),
    MusicalNote(BaseNote.A, NoteModifier.None),
    MusicalNote(BaseNote.A, NoteModifier.Sharp),
    MusicalNote(BaseNote.B, NoteModifier.None)
)

val noteSet12ToneFlat = arrayOf(
    MusicalNote(BaseNote.C, NoteModifier.None),
    MusicalNote(BaseNote.D, NoteModifier.Flat),
    MusicalNote(BaseNote.D, NoteModifier.None),
    MusicalNote(BaseNote.E, NoteModifier.Flat),
    MusicalNote(BaseNote.E, NoteModifier.None),
    MusicalNote(BaseNote.F, NoteModifier.None),
    MusicalNote(BaseNote.G, NoteModifier.Flat),
    MusicalNote(BaseNote.G, NoteModifier.None),
    MusicalNote(BaseNote.A, NoteModifier.Flat),
    MusicalNote(BaseNote.A, NoteModifier.None),
    MusicalNote(BaseNote.B, NoteModifier.Flat),
    MusicalNote(BaseNote.B, NoteModifier.None)
)