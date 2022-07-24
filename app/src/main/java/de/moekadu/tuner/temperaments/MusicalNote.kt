package de.moekadu.tuner.temperaments

enum class BaseNote {
    C, D, E, F, G, A, B, None
}

enum class NoteModifier {
    Sharp, SharpUp, SharpUpUp, SharpDown, SharpDownDown,
    Flat, FlatUp, FlatUpUp, FlatDown, FlatDownDown,
    NaturalUp, NaturalUpUp, NaturalDown, NaturalDownDown,
    None
}

data class NoteNameStem(val baseNote: BaseNote, val modifier: NoteModifier,
                        val enharmonicBaseNote: BaseNote, val enharmonicModifier: NoteModifier) {
    companion object {
        fun fromMusicalNote(note: MusicalNote): NoteNameStem {
            return NoteNameStem(note.base, note.modifier, note.enharmonicBase, note.enharmonicModifier)
        }
    }
}

/** Representation of a musical note.
 * @param base Base of note (C, D, E, ...).
 * @param modifier Modifier of note (none, sharp, flat, ...).
 * @param octave Octave index if the note.
 * @param enharmonicBase base of enharmonic note, which represents the same note. If no enharmonic
 *   should is available, BaseNote.None must be used.
 * @param enharmonicModifier modifier of enharmonic note, which represents the same note. This value
 *   has no meaning if enharmonicBase is BaseNote.None
 * @param enharmonicOctaveOffset Offset to get octave of enharmonic from base octave, according to
 *   the following relation: enharmonicOctave = octave + enharmonicOctaveOffset
 *   Example:
 *     C4-flat with its enharmonic B3
 *     octave = 4, enharmonicOctaveOffset = -1 -> enharmonicOctave = 3
 */
data class MusicalNote(val base: BaseNote, val modifier: NoteModifier, val octave: Int = Int.MAX_VALUE,
                       val enharmonicBase: BaseNote = BaseNote.None,
                       val enharmonicModifier: NoteModifier = NoteModifier.None,
                       val enharmonicOctaveOffset: Int = 0) {
    /** Get string representation of note, which can be later on parsed to get back the note. */
    fun asString(): String {
        return "MusicalNote(base=$base,modifier=$modifier,octave=$octave,enharmonicBase=$enharmonicBase,enharmonicModifier=$enharmonicModifier,enharmonicOctaveOffset=$enharmonicOctaveOffset)"
    }

    /** Return a note, where enharmonic and base represenation are exchanged. */
    fun switchEnharmonic(): MusicalNote {
        if (enharmonicBase == BaseNote.None)
            return this
        // enharmonicOctave = octave + enharmonicOctaveOffset
        // -> oldEnharmonicOctave = oldOctave + oldEnharmonicOctaveOffset
        // and since newOctave = oldEnharmonicOctave
        // -> newOctave = oldOctave + oldEnharmonicOctaveOffset
        // -> newEnharmonicOctave = newOctave + newEnharmonicOctaveOffset
        // since newEnharmonicOctave = oldOctave
        // -> oldOctave = newOctave + newEnharmonicOctaveOffset
        // -> newEnharmonicOctaveOffset = oldOctave - newOctave
        // -> newEnharmonicOctaveOffset = oldOctave - oldOctave - oldEnharmonicOctaveOffset
        // -> newEnharmonicOctaveOffset = -oldEnharmonicOctaveOffset
        return MusicalNote(base = enharmonicBase, modifier = enharmonicModifier, octave = octave + enharmonicOctaveOffset,
            enharmonicBase = base, enharmonicModifier = modifier, enharmonicOctaveOffset = -enharmonicOctaveOffset)
    }
    companion object {
        /** Parse a string (which normally is created with "asString" and return the resulting note. */
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
            var enharmonicOctaveOffset = 0

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
                    "enharmonicOctaveOffset" -> enharmonicOctaveOffset = keyAndValue[1].toInt()
                }
            }
            return MusicalNote(base, modifier, octave, enharmonicBase, enharmonicModifier, enharmonicOctaveOffset)
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
            return if (first == null && second == null)
                true
            else if (first == null || second == null)
                false
            else if (noteStemEqual(first, second) && first.octave == second.octave)
                true
            else
                notesEnharmonic(first, second) && first.octave == second.octave + second.enharmonicOctaveOffset
        }

        /** Check if two notes are the same, while ignoring the octave.
         * We return true, if the notes are either both the same or if they are enharmonic, but
         * we ignore the octave during comparison.
         * @param first First note to compare.
         * @param second Second note to compare.
         * @return True if notes are the same (ignoring the octave), if both notes are null, we return
         *   false.
         */
        fun notesEqualIgnoreOctave(first: MusicalNote?, second: MusicalNote?): Boolean {
            return if (first == null || second == null) {
                false
            } else {
                noteStemEqual(first, second) || notesEnharmonic(first, second)
            }
        }

        private fun noteStemEqual(first: MusicalNote, second: MusicalNote): Boolean {
            return (first.base == second.base && first.modifier == second.modifier
                    && first.enharmonicBase == second.enharmonicBase
                    && first.enharmonicModifier == second.enharmonicModifier
                    && first.enharmonicOctaveOffset == second.enharmonicOctaveOffset)
        }

        private fun notesEnharmonic(first: MusicalNote, second: MusicalNote): Boolean {
            return (first.base == second.enharmonicBase && first.modifier == second.enharmonicModifier
                    && first.enharmonicBase == second.base && first.enharmonicModifier == second.modifier
                    && first.enharmonicOctaveOffset == -second.enharmonicOctaveOffset)
        }
    }
}
