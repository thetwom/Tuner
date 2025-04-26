package de.moekadu.tuner.notenames

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/** Class containing the notes of one octave.
 * @param notes Array of the note names within the octave. The octave index of the note is of no
 *   importance here.
 * @param defaultReferenceNote The note which normally is used as reference note within tuning. Note
 *   that here, the octave index is important.
 * @param firstNoteOfOctave First note of an octave. In other words, when cycling between note names
 *   and when this note is reached, the octave counter is increased.
 */
@Serializable
@Immutable
data class NoteNames2(
    val notes: Array<MusicalNote>,
    val defaultReferenceNote: MusicalNote,
    val firstNoteOfOctave: MusicalNote
) {
    /** Number of notes. */
    val size get() = notes.size

    /** Index within notes, where the first note of octave is placed. */
    val indexOfFirstNoteOfOctave = notes.indexOfFirst { it == firstNoteOfOctave }

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
    fun switchEnharmonics(): NoteNames2 {
        return NoteNames2(
            notes = notes.map { it.switchEnharmonic() }.toTypedArray(),
            defaultReferenceNote = defaultReferenceNote.switchEnharmonic(),
            firstNoteOfOctave = firstNoteOfOctave.switchEnharmonic()
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

        other as NoteNames2

        if (!notes.contentEquals(other.notes)) return false
        if (defaultReferenceNote != other.defaultReferenceNote) return false
        if (firstNoteOfOctave != other.firstNoteOfOctave) return false

        return true
    }

    override fun hashCode(): Int {
        var result = notes.contentHashCode()
        result = 31 * result + defaultReferenceNote.hashCode()
        result = 31 * result + firstNoteOfOctave.hashCode()
        return result
    }
}