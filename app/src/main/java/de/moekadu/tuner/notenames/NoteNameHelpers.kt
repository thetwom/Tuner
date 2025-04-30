package de.moekadu.tuner.notenames

object NoteNameHelpers {
    /**
     * Code for edo naming is based on the logic given here:
     * - https://www.tallkite.com/apps/noteNamer.html
     * - https://github.com/thetwom/Tuner/issues/98
     */

    fun findDefaultReferenceNote(noteNames: Array<MusicalNote>): MusicalNote {
        val note = noteNames
            .firstOrNull {
                (it.base == BaseNote.A && it.modifier == NoteModifier.None) ||
                        (it.enharmonicBase == BaseNote.A && it.enharmonicModifier == NoteModifier.None)
            } ?: noteNames.getOrElse(0) { MusicalNote(BaseNote.A, NoteModifier.None) }

        return note.copy(octave = 4)
    }
}