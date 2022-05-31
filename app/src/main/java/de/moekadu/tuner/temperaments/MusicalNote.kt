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
    // TODO: write tests for member functions
    fun asString(): String {
        return "MusicalNote(base=$base,modifier=$modifier,octave=$octave,enharmonicBase=$enharmonicBase,enharmonicModifier=$enharmonicModifier)"
    }

    fun switchEnharmonic(): MusicalNote {
        if (enharmonicBase == BaseNote.None)
            return this
        return this.copy(base = enharmonicBase, modifier = enharmonicModifier, enharmonicBase = base, enharmonicModifier = modifier)
    }
    companion object {
        fun fromString(string: String): MusicalNote {
            val className = "MusicalNote"
            if (string.length < className.length + 2 || string.substring(0, className.length + 1) != "$className(" || string.substring(string.length-1, string.length) != ")")
                throw RuntimeException("$className.fromString: $string cannot be parsed")
            val contentString = string.substring(className.length + 1, string.length-1)
            var base: BaseNote = BaseNote.None
            var modifier = NoteModifier.None
            var octave: Int = Int.MAX_VALUE
            var enharmonicBase = BaseNote.None
            var enharmonicModifier = NoteModifier.None

            contentString.split(",").forEach {
                val keyAndValue = it.split("=")
                if (keyAndValue.size != 2) {
                    throw RuntimeException("$className.fromString: $it is no valid key-value pair")
                }
                when (keyAndValue[0]) {
                    "base" -> base = BaseNote.valueOf(keyAndValue[1])
                    "modifier" -> modifier = NoteModifier.valueOf(keyAndValue[1])
                    "octave" -> octave = keyAndValue[1].toInt()
                    "enharmonicBase" -> enharmonicBase = BaseNote.valueOf(keyAndValue[1])
                    "enharmonicModifier" -> enharmonicModifier = NoteModifier.valueOf(keyAndValue[1])
                }
            }
            return MusicalNote(base, modifier, octave, enharmonicBase, enharmonicModifier)
        }
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
            else if (first?.octave == second?.octave && notesEqualIgnoreOctave(first, second))
                return true
            return false
        }

        /** Check if two notes are the same, while ignoring the octave.
         * We will also return true, if note of first not matches the enharmonic of the second note
         * and via verse:
         * True if (all conditons in one item must be met):
         *   - base, modifier, enharmonic base, enharmonic modifier match
         *   - base, modifier of first and enharmonic base, enharmonic modifier of second match,
         *     enharmonic base, enharmonic moidifier of first and base, modifier of second note match.
         * @param first First note to compare.
         * @param second Second note to compare.
         * @return True if notes are the same (ignoring the octave), if both notes are null, we return
         *   false.
         */
        fun notesEqualIgnoreOctave(first: MusicalNote?, second: MusicalNote?): Boolean {
            if (first == null || second == null) {
                return false
            } else if (first.base == second.base && first.modifier == second.modifier
                && first.enharmonicBase == second.enharmonicBase
                && first.enharmonicModifier == second.enharmonicModifier) {
                return true
            } else if (first.base == second.enharmonicBase && first.modifier == second.enharmonicModifier
                && first.enharmonicBase == second.base && first.enharmonicModifier == second.modifier) {
                return true
            }
            return false
        }
    }
}
