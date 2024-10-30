package de.moekadu.tuner.temperaments2

import androidx.compose.runtime.Immutable
import de.moekadu.tuner.misc.StringOrResId
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier
import kotlinx.serialization.Serializable

/** Class containing the notes of one octave.
 * @param name Name of the instance for printing.
 * @param description Description of the instance for printing.
 * @param notes Array of the note names within the octave. The octave index of the note is of no
 *   importance here.
 * @param defaultReferenceNote The note which normally is used as reference note within tuning. Note
 *   that here, the octave index is important.
 * @param stableId Id to uniquely identify an instance of this class.
 */
@Serializable
@Immutable
data class NoteNames(
    val name: StringOrResId,
    val description: StringOrResId,
    val notes: Array<MusicalNote>,
    val defaultReferenceNote: MusicalNote,
    val stableId: Long
) {
    /** Number of notes. */
    val size get() = notes.size

    /** Return note index within the notes-array.
     * @param note Note for which the index. Note that the octave index is ignored.
     * @return Index of note within the notes-array or -1 if note is not found.
     */
    fun getNoteIndex(note: MusicalNote): Int {
        val index = notes.indexOfFirst {
            MusicalNote.notesEqualIgnoreOctave(it, note)
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
            name,
            description,
            notes.map { it.switchEnharmonic() }.toTypedArray(),
            defaultReferenceNote.switchEnharmonic(),
            stableId
        )
    }

    /** Check if a given note is part of the note names array.
     * @param note Note. Octave index is ignored.
     * @return True if note is part of the note names array, else false.
     */
    fun hasNote(note: MusicalNote): Boolean {
        return notes.any{ MusicalNote.notesEqualIgnoreOctave(it, note) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NoteNames

//        if (name != other.name) return false
//        if (description != other.description) return false
//        if (!notes.contentEquals(other.notes)) return false
//        if (defaultReferenceNote != other.defaultReferenceNote) return false
        if (stableId != other.stableId) return false

        return true
    }

    override fun hashCode(): Int {
//        var result = name.hashCode()
//        result = 31 * result + description.hashCode()
//        result = 31 * result + notes.contentHashCode()
//        result = 31 * result + defaultReferenceNote.hashCode()
//        result = 31 * result + stableId.hashCode()
//        return result
        return stableId.hashCode()
    }
}

fun getSuitableNoteNames(numberOfNotesPerOctave: Int): NoteNames? {
    return when (numberOfNotesPerOctave) {
        // 12 tones
        12 -> {
            var notes = arrayOf(
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.None),
                MusicalNote(
                    base = BaseNote.C,
                    modifier = NoteModifier.Sharp,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.Flat
                ),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.None),
                MusicalNote(
                    base = BaseNote.E,
                    modifier = NoteModifier.Flat,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.Sharp
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
                    base = BaseNote.B,
                    modifier = NoteModifier.Flat,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.Sharp
                ),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.None)
            )

            NoteNames(
                StringOrResId(""),
                StringOrResId(""),
                notes,
                notes[9].copy(octave = 4),
                -notes.size.toLong()
            )
        }
        // 15 tones
        15 -> {
            var notes = arrayOf(
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

            NoteNames(
                StringOrResId(""),
                StringOrResId(""),
                notes,
                notes[11].copy(octave = 4),
                -notes.size.toLong()
            )
        }
        // 17 tone
        17 -> {
            var notes = arrayOf(
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

            NoteNames(
                StringOrResId(""),
                StringOrResId(""),
                notes,
                notes[13].copy(octave = 4),
                -notes.size.toLong()
            )
        }
        // 19 tone
        19 -> {
            var notes = arrayOf(
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

            NoteNames(
                StringOrResId(""),
                StringOrResId(""),
                notes,
                notes[14].copy(octave = 4),
                -notes.size.toLong()
            )
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

            NoteNames(
                StringOrResId(""),
                StringOrResId(""),
                notes,
                notes[17].copy(octave = 4),
                -notes.size.toLong()
            )
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

            NoteNames(
                StringOrResId(""),
                StringOrResId(""),
                notes,
                notes[18].copy(octave = 4),
                -notes.size.toLong()
            )
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

            NoteNames(
                StringOrResId(""),
                StringOrResId(""),
                notes,
                notes[21].copy(octave = 4),
                -notes.size.toLong()
            )
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

            NoteNames(
                StringOrResId(""),
                StringOrResId(""),
                notes,
                notes[22].copy(octave = 4),
                -notes.size.toLong()
            )
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

            NoteNames(
                StringOrResId(""),
                StringOrResId(""),
                notes,
                notes[23].copy(octave = 4),
                -notes.size.toLong()
            )
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
                    base = BaseNote.D,
                    modifier = NoteModifier.FlatUp,
                    enharmonicBase = BaseNote.C,
                    enharmonicModifier = NoteModifier.Sharp
                ),
                MusicalNote(
                    base = BaseNote.D,
                    modifier = NoteModifier.NaturalDownDown,
                    enharmonicBase = BaseNote.C,
                    enharmonicModifier = NoteModifier.SharpUp
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
                    base = BaseNote.E,
                    modifier = NoteModifier.FlatUp,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.Sharp
                ),
                MusicalNote(
                    base = BaseNote.E,
                    modifier = NoteModifier.NaturalDownDown,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.SharpUp
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
                    base = BaseNote.G,
                    modifier = NoteModifier.FlatUp,
                    enharmonicBase = BaseNote.F,
                    enharmonicModifier = NoteModifier.Sharp
                ),
                MusicalNote(
                    base = BaseNote.G,
                    modifier = NoteModifier.NaturalDownDown,
                    enharmonicBase = BaseNote.F,
                    enharmonicModifier = NoteModifier.SharpUp
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
                    base = BaseNote.A,
                    modifier = NoteModifier.FlatUp,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.Sharp
                ),
                MusicalNote(
                    base = BaseNote.A,
                    modifier = NoteModifier.NaturalDownDown,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.SharpUp
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
                    base = BaseNote.B,
                    modifier = NoteModifier.FlatUp,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.Sharp
                ),
                MusicalNote(
                    base = BaseNote.B,
                    modifier = NoteModifier.NaturalDownDown,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.SharpUp
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

            NoteNames(
                StringOrResId(""),
                StringOrResId(""),
                notes,
                notes[31].copy(octave = 4),
                -notes.size.toLong()
            )
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
                    base = BaseNote.D,
                    modifier = NoteModifier.FlatUp,
                    enharmonicBase = BaseNote.C,
                    enharmonicModifier = NoteModifier.Sharp
                ),
                MusicalNote(
                    base = BaseNote.D,
                    modifier = NoteModifier.FlatUpUp,
                    enharmonicBase = BaseNote.C,
                    enharmonicModifier = NoteModifier.SharpUp
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
                    base = BaseNote.E,
                    modifier = NoteModifier.FlatUp,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.Sharp
                ),
                MusicalNote(
                    base = BaseNote.E,
                    modifier = NoteModifier.FlatUpUp,
                    enharmonicBase = BaseNote.D,
                    enharmonicModifier = NoteModifier.SharpUp
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
                    base = BaseNote.G,
                    modifier = NoteModifier.FlatUp,
                    enharmonicBase = BaseNote.F,
                    enharmonicModifier = NoteModifier.Sharp
                ),
                MusicalNote(
                    base = BaseNote.G,
                    modifier = NoteModifier.FlatUpUp,
                    enharmonicBase = BaseNote.F,
                    enharmonicModifier = NoteModifier.SharpUp
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
                    base = BaseNote.A,
                    modifier = NoteModifier.FlatUp,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.Sharp
                ),
                MusicalNote(
                    base = BaseNote.A,
                    modifier = NoteModifier.FlatUpUp,
                    enharmonicBase = BaseNote.G,
                    enharmonicModifier = NoteModifier.SharpUp
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
                    base = BaseNote.B,
                    modifier = NoteModifier.FlatUp,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.Sharp
                ),
                MusicalNote(
                    base = BaseNote.B,
                    modifier = NoteModifier.FlatUpUp,
                    enharmonicBase = BaseNote.A,
                    enharmonicModifier = NoteModifier.SharpUp
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

            NoteNames(
                StringOrResId(""),
                StringOrResId(""),
                notes,
                notes[40].copy(octave = 4),
                -notes.size.toLong()
            )
        }
        else -> {
            null
        }
    }
}

