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

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/** Class containing the notes of one octave.
 * @param notes Array of the note names within the octave. The octave index of the note is of no
 *   importance here.
 * @param defaultReferenceNote The note which normally is used as reference note within tuning. Note
 *   that here, the octave index is important.
 */
@Serializable
@Immutable
data class NoteNames(
    val notes: Array<MusicalNote>,
    val defaultReferenceNote: MusicalNote
) {
    /** Number of notes. */
    val size get() = notes.size

    /** Return note index within the notes-array.
     * @param note Note for which the index. Note that the octave index is ignored.
     * @return Index of note within the notes-array or -1 if note is not found.
     */
    fun getNoteIndex(note: MusicalNote): Int {
        val index = notes.indexOfFirst {
            it.equalsIgnoreOctave(note)
        }
        return index
    }

    /** Obtain note at given index.
     * @param index Index of note.
     * @return Note.
     */
    operator fun get(index: Int): MusicalNote {
        return notes[index]
    }

    /** Obtain note at given index if it exists else null.
     * @param index Index of note.
     * @return Note or null if it does not exist.
     */
    fun getOrNull(index: Int): MusicalNote? {
        return notes.getOrNull(index)
    }

    /** Get new note name scale, where base note and enharmonics are switched.
     * This will not change name, description or stableIds, since it effectively are the same
     * notes.
     * @return New note name scale where base notes are exchanged with the enharmonics.
     */
    fun switchEnharmonics(): NoteNames {
        return NoteNames(
            notes.map { it.switchEnharmonic() }.toTypedArray(),
            defaultReferenceNote.switchEnharmonic()
        )
    }

    /** Check if a given note is part of the note names array.
     * @param note Note. Octave index is ignored.
     * @return True if note is part of the note names array, else false.
     */
    fun hasNote(note: MusicalNote): Boolean {
        return notes.any{ it.equalsIgnoreOctave(note) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NoteNames
        if (!notes.contentEquals(other.notes)) return false
        if (defaultReferenceNote != other.defaultReferenceNote) return false

        return true
    }

    override fun hashCode(): Int {
        var result = notes.contentHashCode()
        result = 31 * result + defaultReferenceNote.hashCode()
        return result
    }
}

fun NoteNames.toNew(): NoteNames2 {
    return NoteNames2(notes, defaultReferenceNote, notes[0])
}

private fun getSuitableNoteNames(numberOfNotesPerOctave: Int): NoteNames? {
    return when (numberOfNotesPerOctave) {
        // 12 tones
        12 -> {
            val notes = arrayOf(
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.None),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.None),
                MusicalNote(
                    base = BaseNote.D,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.E,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.None),
                MusicalNote(
                    base = BaseNote.F,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.None),
                MusicalNote(
                    base = BaseNote.G,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.None),
                MusicalNote(
                    base = BaseNote.A,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.B,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.None)
            )

            NoteNames(notes, notes[9].copy(octave = 4))
        }
        // 15 tones
        15 -> {
            val notes = arrayOf(
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.None),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.Sharp),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.Flat),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.None),
                MusicalNote(
                    base = BaseNote.F,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.Sharp),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.Flat),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.Sharp),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.Flat),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.None),
            )

            NoteNames(notes, notes[11].copy(octave = 4))
        }
        // 17 tone
        17 -> {
            val notes = arrayOf(
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.None),
                //
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.None),
            )

            NoteNames(notes, notes[13].copy(octave = 4))
        }
        // 19 tone
        19 -> {
            val notes = arrayOf(
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.Sharp),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.Flat),
                //
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.Sharp),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.Flat),
                //
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.None),
                MusicalNote(
                    base = BaseNote.E,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.F,
                    enharmonicModifier = NoteModifier.Flat
                ),
                //
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.Sharp),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.Flat),
                //
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.Sharp),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.Flat),
                //
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.Sharp),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.Flat),
                //
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.None),
                MusicalNote(
                    base = BaseNote.B,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.C,
                    enharmonicModifier = NoteModifier.Flat,
                    enharmonicOctaveOffset = 1
                )
            )

            NoteNames(notes, notes[14].copy(octave = 4))
        }
        // 22 tone
        22 -> {
            val notes = arrayOf(
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.D,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.E,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.None),
                //
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.F,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.G,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.A,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.B,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.None),
            )

            NoteNames(notes, notes[17].copy(octave = 4))
        }
        // 24 tone
        24 -> {
            val notes = arrayOf(
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.D,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.E,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.None),
                MusicalNote(
                    base = BaseNote.E,
                    modifier = NoteModifier.NaturalUp,
                    enharmonicBase = BaseNote.F,
                    enharmonicModifier = NoteModifier.NaturalDown
                ),
                //
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.F,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.G,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.A,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.B,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.None),
                MusicalNote(
                    base = BaseNote.B,
                    modifier = NoteModifier.NaturalUp,
                    enharmonicBase = BaseNote.C,
                    enharmonicModifier = NoteModifier.NaturalDown,
                    enharmonicOctaveOffset = 1
                ),
            )

            NoteNames(notes, notes[18].copy(octave = 4))
        }
        // 27 tone
        27 -> {
            val notes = arrayOf(
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.NaturalUpUp),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalDownDown),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalUpUp),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.NaturalDownDown),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.None),
                //
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.NaturalUpUp),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalDownDown),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalUpUp),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalDownDown),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalUpUp),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.NaturalDownDown),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.None),
            )

            NoteNames(notes, notes[21].copy(octave = 4))
        }
    // 29 tone
        29 -> {
            val notes = arrayOf(
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.D,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.E,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(
                    base = BaseNote.D,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.E,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.None),
                MusicalNote(
                    base = BaseNote.E,
                    modifier = NoteModifier.NaturalUp,
                    enharmonicBase = BaseNote.F,
                    enharmonicModifier = NoteModifier.NaturalDown
                ),
                //
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.F,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(
                    base = BaseNote.F,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.G,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(
                    base = BaseNote.G,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.A,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.B,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(
                    base = BaseNote.A,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.B,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.None),
                MusicalNote(
                    base = BaseNote.B,
                    modifier = NoteModifier.NaturalUp,
                    enharmonicBase = BaseNote.C,
                    enharmonicModifier = NoteModifier.NaturalDown,
                    enharmonicOctaveOffset = 1
                ),
            )

            NoteNames(notes, notes[22].copy(octave = 4))
        }
        // 31 tone
        31 -> {
            val notes = arrayOf(
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.Sharp),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.Flat),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.Sharp),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.Flat),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.Sharp),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.Flat),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.Sharp),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.Flat),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.Sharp),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.Flat),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.NaturalDown,
                    octaveOffset = 1
                ),
            )

            NoteNames(notes, notes[23].copy(octave = 4))
        }
        // 41 tone
        41 -> {
            val notes = arrayOf(
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.NaturalUpUp,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.FlatDown
                ),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.SharpUp,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.NaturalDownDown
                ),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.D,
                    modifier = NoteModifier.NaturalUpUp,
                    enharmonicBase = BaseNote.E,
                    enharmonicModifier = NoteModifier.FlatDown
                ),
                MusicalNote(
                    base = BaseNote.D,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.E,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(
                    base = BaseNote.D,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.E,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(
                    base = BaseNote.D,
                    modifier = NoteModifier.SharpUp,
                    enharmonicBase = BaseNote.E,
                    enharmonicModifier = NoteModifier.NaturalDownDown
                ),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.F,
                    modifier = NoteModifier.NaturalUpUp,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.FlatDown
                ),
                MusicalNote(
                    base = BaseNote.F,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(
                    base = BaseNote.F,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(
                    base = BaseNote.F,
                    modifier = NoteModifier.SharpUp,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.NaturalDownDown
                ),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.G,
                    modifier = NoteModifier.NaturalUpUp,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.FlatDown
                ),
                MusicalNote(
                    base = BaseNote.G,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(
                    base = BaseNote.G,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(
                    base = BaseNote.G,
                    modifier = NoteModifier.SharpUp,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.NaturalDownDown
                ),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.A,
                    modifier = NoteModifier.NaturalUpUp,
                    enharmonicBase = BaseNote.B,
                    enharmonicModifier = NoteModifier.FlatDown
                ),
                MusicalNote(
                    base = BaseNote.A,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.B,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(
                    base = BaseNote.A,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.B,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(
                    base = BaseNote.A,
                    modifier = NoteModifier.SharpUp,
                    enharmonicBase = BaseNote.B,
                    enharmonicModifier = NoteModifier.NaturalDownDown
                ),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.NaturalDown,
                    octaveOffset = 1
                ),
            )

            NoteNames(notes, notes[31].copy(octave = 4))
        }
        // 53 tone
        53 -> {
            val notes = arrayOf(
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.NaturalUpUp),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.SharpDownDown,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.FlatDown
                ),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.SharpUp,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.FlatUpUp,
                ),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalDownDown),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalUpUp),
                MusicalNote(
                    base = BaseNote.D,
                    modifier = NoteModifier.SharpDownDown,
                    enharmonicBase = BaseNote.E,
                    enharmonicModifier = NoteModifier.FlatDown
                ),
                MusicalNote(
                    base = BaseNote.D,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.E,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(
                    base = BaseNote.D,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.E,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(
                    base = BaseNote.D,
                    modifier = NoteModifier.SharpUp,
                    enharmonicBase = BaseNote.E,
                    enharmonicModifier = NoteModifier.FlatUpUp
                ),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.NaturalDownDown),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.E,
                    modifier = NoteModifier.NaturalUpUp,
                    enharmonicBase = BaseNote.F,
                    enharmonicModifier = NoteModifier.NaturalDownDown
                ),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.NaturalUpUp),
                MusicalNote(
                    base = BaseNote.F,
                    modifier = NoteModifier.SharpDownDown,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.FlatDown
                ),
                MusicalNote(
                    base = BaseNote.F,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(
                    base = BaseNote.F,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(
                    base = BaseNote.F,
                    modifier = NoteModifier.SharpUp,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.FlatUpUp
                ),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalDownDown),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalUpUp),
                MusicalNote(
                    base = BaseNote.G,
                    modifier = NoteModifier.SharpDownDown,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.FlatDown
                ),
                MusicalNote(
                    base = BaseNote.G,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(
                    base = BaseNote.G,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(
                    base = BaseNote.G,
                    modifier = NoteModifier.SharpUp,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.FlatUpUp
                ),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalDownDown),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalUp),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalUpUp),
                MusicalNote(
                    base = BaseNote.A,
                    modifier = NoteModifier.SharpDownDown,
                    enharmonicBase = BaseNote.B,
                    enharmonicModifier = NoteModifier.FlatDown
                ),
                MusicalNote(
                    base = BaseNote.A,
                    modifier = NoteModifier.SharpDown,
                    enharmonicBase = BaseNote.B,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(
                    base = BaseNote.A,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.B,
                    enharmonicModifier = NoteModifier.FlatUp
                ),
                MusicalNote(
                    base = BaseNote.A,
                    modifier = NoteModifier.SharpUp,
                    enharmonicBase = BaseNote.B,
                    enharmonicModifier = NoteModifier.FlatUpUp
                ),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.NaturalDownDown),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.NaturalDown),
                //
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.NaturalUp),
                MusicalNote(
                    base = BaseNote.B,
                    modifier = NoteModifier.NaturalUpUp,
                    enharmonicBase = BaseNote.C,
                    enharmonicModifier = NoteModifier.NaturalDownDown,
                    enharmonicOctaveOffset = 1
                ),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.NaturalDown,
                    octaveOffset = 1
                ),
            )

            NoteNames(notes, notes[40].copy(octave = 4))
        }
        else -> {
            null
        }
    }
}

