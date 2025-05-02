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
package de.moekadu.tuner.notenames

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

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
    FlatFlatFlatDownDownDown, FlatFlatFlatDownDown, FlatFlatFlatDown, FlatFlatFlat, FlatFlatFlatUp, FlatFlatFlatUpUp, FlatFlatFlatUpUpUp,
    FlatFlatDownDownDown, FlatFlatDownDown, FlatFlatDown, FlatFlat, FlatFlatUp, FlatFlatUpUp, FlatFlatUpUpUp,
    FlatDownDownDown, FlatDownDown, FlatDown, Flat, FlatUp, FlatUpUp, FlatUpUpUp,
    NaturalDownDownDown, NaturalDownDown, NaturalDown, None, NaturalUp, NaturalUpUp, NaturalUpUpUp,
    SharpDownDownDown, SharpDownDown, SharpDown, Sharp, SharpUp, SharpUpUp, SharpUpUpUp,
    SharpSharpDownDownDown, SharpSharpDownDown, SharpSharpDown, SharpSharp, SharpSharpUp, SharpSharpUpUp, SharpSharpUpUpUp,
    SharpSharpSharpDownDownDown, SharpSharpSharpDownDown, SharpSharpSharpDown, SharpSharpSharp, SharpSharpSharpUp, SharpSharpSharpUpUp, SharpSharpSharpUpUpUp
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
@Serializable
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

    /** Return a note, where enharmonic and base representation are exchanged. */
    fun switchEnharmonic(switchAlsoForBaseNone: Boolean = false): MusicalNote {
        if (enharmonicBase == BaseNote.None && !switchAlsoForBaseNone)
            return this
        return MusicalNote(base = enharmonicBase, modifier = enharmonicModifier, octave = octave,
            octaveOffset = enharmonicOctaveOffset,
            enharmonicBase = base, enharmonicModifier = modifier,
            enharmonicOctaveOffset = octaveOffset)
    }

    /** Check if two notes match.
     * "Match" means that we find at least one matching pair between for the note ore the enharmonics.
     * @param other Other musical for the check
     * @param ignoreOctave If true, it is not required that the octaves are equal.
     * @return True, if the note match else false.
     */
    fun match(other: MusicalNote?, ignoreOctave: Boolean = false): Boolean {
        if (other == null)
            return false
        if (!ignoreOctave && octave != other.octave)
            return false

        if (base != BaseNote.None) {
            if (base == other.base && modifier == other.modifier && octaveOffset == other.octaveOffset)
                return true
            else if (base == other.enharmonicBase && modifier == other.enharmonicModifier && octaveOffset == other.enharmonicOctaveOffset)
                return true
        }
        if (enharmonicBase != BaseNote.None) {
            if (enharmonicBase == other.base && enharmonicModifier == other.modifier && enharmonicOctaveOffset == other.octaveOffset)
                return true
            else if (enharmonicBase == other.enharmonicBase && enharmonicModifier == other.enharmonicModifier && enharmonicOctaveOffset == other.enharmonicOctaveOffset)
                return true
        }
        return false
    }

    /** Check if two notes are the same, while ignoring the octave.
     * @param other Note used to compare.
     * @return True if notes are the same (ignoring the octave but not octave offset).
     */
    fun equalsIgnoreOctave(other: MusicalNote?): Boolean {
        return if (other == null) {
            false
        } else {
            (base == other.base && modifier == other.modifier
                    && octaveOffset == other.octaveOffset
                    && enharmonicBase == other.enharmonicBase
                    && enharmonicModifier == other.enharmonicModifier
                    && enharmonicOctaveOffset == other.enharmonicOctaveOffset)
        }
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

//        /** Check if two notes are the same, while ignoring the octave.
//         * @param first First note to compare.
//         * @param second Second note to compare.
//         * @return True if notes are the same (ignoring the octave but not octave offset). If both
//         *   notes are null, we return false.
//         */
//        fun notesEqualIgnoreOctave(first: MusicalNote?, second: MusicalNote?): Boolean {
//            return if (first == null || second == null) {
//                false
//            } else {
//                (first.base == second.base && first.modifier == second.modifier
//                        && first.octaveOffset == second.octaveOffset
//                        && first.enharmonicBase == second.enharmonicBase
//                        && first.enharmonicModifier == second.enharmonicModifier
//                        && first.enharmonicOctaveOffset == second.enharmonicOctaveOffset)
//            }
//        }
    }
}
