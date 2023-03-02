package de.moekadu.tuner.temperaments

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class BaseNote {
    C, D, E, F, G, A, B, None
}

/** Modifiers for base notes.
 * The ordering is due to the sharpness level used for printing, the first
 * note is the most flat note and the last note is the most sharp note.
 * As said, the ordering is just as used for printing, it does NOT mean,
 * that within an actual scale, this is true. It is e.g. possible that NaturalUpUp
 * is more sharp than SharpDownDown.
 */
enum class NoteModifier {
    FlatDownDown, FlatDown, Flat, FlatUp, FlatUpUp,
    NaturalDownDown, NaturalDown, None, NaturalUp, NaturalUpUp,
    SharpDownDown, SharpDown, Sharp, SharpUp, SharpUpUp,
}

fun NoteModifier.flatSharpIndex(): Int {
    return this.ordinal - NoteModifier.None.ordinal
}

data class NoteNameStem(val baseNote: BaseNote,
                        val modifier: NoteModifier = NoteModifier.None,
                        val enharmonicBaseNote: BaseNote = BaseNote.None,
                        val enharmonicModifier: NoteModifier = NoteModifier.None) {
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
 * @param octaveOffset While the note is on a specific octave, the actually printed octave
 *   should can be different and the octaveOffset is defining this difference. E.g. if you have
 *   scale like
 *      octave 3: C3, D3, .... B3, Cb4    ;   octave 4: C4, D4 ...
 *   In this case th Cb4 belongs to octave 3, but it should be printed as Cb4, so the octaveOffset
 *   would be 1 -> printedOctave = octave + octaveOffest = 3 + 1 = 4
 * @param enharmonicBase base of enharmonic note, which represents the same note. If no enharmonic
 *   should is available, BaseNote.None must be used.
 * @param enharmonicModifier modifier of enharmonic note, which represents the same note. This value
 *   has no meaning if enharmonicBase is BaseNote.None
 * @param enharmonicOctaveOffset Same as octaveOffset, but for the enharmonic note
 */
@Parcelize
data class MusicalNote(val base: BaseNote, val modifier: NoteModifier, val octave: Int = Int.MAX_VALUE,
                       val octaveOffset: Int = 0,
                       val enharmonicBase: BaseNote = BaseNote.None,
                       val enharmonicModifier: NoteModifier = NoteModifier.None,
                       val enharmonicOctaveOffset: Int = 0) : Parcelable {
    /** Get string representation of note, which can be later on parsed to get back the note. */
    fun asString(): String {
        return "MusicalNote(base=$base,modifier=$modifier,octave=$octave,octaveOffset=$octaveOffset,enharmonicBase=$enharmonicBase,enharmonicModifier=$enharmonicModifier,enharmonicOctaveOffset=$enharmonicOctaveOffset)"
    }

    /** Return a note, where enharmonic and base represenation are exchanged. */
    fun switchEnharmonic(switchAlsoForBaseNone: Boolean = false): MusicalNote {
        if (enharmonicBase == BaseNote.None && !switchAlsoForBaseNone)
            return this
        return MusicalNote(base = enharmonicBase, modifier = enharmonicModifier, octave = octave,
            octaveOffset = enharmonicOctaveOffset,
            enharmonicBase = base, enharmonicModifier = modifier,
            enharmonicOctaveOffset = octaveOffset)
    }
    companion object {
        /** Parse a string (which normally is created with "asString" and return the resulting note. */
        fun fromString(string: String): MusicalNote {
            val className = "MusicalNote"
            if (string.length < className.length + 2 || string.substring(0, className.length + 1) != "$className(" || string.substring(string.length-1, string.length) != ")")
                throw RuntimeException("$className.fromString: $string cannot be parsed")
            val contentString = string.substring(className.length + 1, string.length-1)
            var base = BaseNote.None
            var modifier = NoteModifier.None
            var octave = Int.MAX_VALUE
            var octaveOffset = 0
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
                    "octaveOffset" -> octaveOffset = keyAndValue[1].toInt()
                    "enharmonicBase" -> enharmonicBase = BaseNote.valueOf(keyAndValue[1])
                    "enharmonicModifier" -> enharmonicModifier = NoteModifier.valueOf(keyAndValue[1])
                    "enharmonicOctaveOffset" -> enharmonicOctaveOffset = keyAndValue[1].toInt()
                }
            }
            return MusicalNote(base = base, modifier = modifier, octave = octave,
                octaveOffset = octaveOffset,
                enharmonicBase = enharmonicBase, enharmonicModifier = enharmonicModifier,
                enharmonicOctaveOffset = enharmonicOctaveOffset)
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
                notesEnharmonic(first, second) && first.octave == second.octave
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
                    && first.octaveOffset == second.octaveOffset
                    && first.enharmonicBase == second.enharmonicBase
                    && first.enharmonicModifier == second.enharmonicModifier
                    && first.enharmonicOctaveOffset == second.enharmonicOctaveOffset)
        }

        private fun notesEnharmonic(first: MusicalNote, second: MusicalNote): Boolean {
            return (first.base == second.enharmonicBase && first.modifier == second.enharmonicModifier
                    && first.enharmonicBase == second.base && first.enharmonicModifier == second.modifier
                    && first.octaveOffset == second.enharmonicOctaveOffset
                    && first.enharmonicOctaveOffset == second.octaveOffset)
        }
    }
}
