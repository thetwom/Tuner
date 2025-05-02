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
package de.moekadu.tuner.musicalscale

import androidx.compose.runtime.Stable
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteNames2

/** Note names, which can map indices between musical notes and via verse.
 * @param noteNames Note names of one octave.
 * @param referenceNote Reference note of the scale, which refers to noteIndex 0.
 *   This note must be part of the note names. Use null to use the default reference note.
 */
@Stable
class MusicalScaleNoteNames2(
    val noteNames: NoteNames2,
    referenceNote: MusicalNote?
) {
    /** Reference note. */
    val referenceNote = referenceNote ?: noteNames.defaultReferenceNote

    /** Reference note index within the octave (index in notes). */
    val referenceNoteIndexWithinOctave = noteNames.getNoteIndex(this.referenceNote)

    /** Reference note index within the octave (index in notes). */
    private val octaveSwitchIndexWithinNoteNames
            = noteNames.getNoteIndex(noteNames.octaveSwitchIndex)

    /** Octave of reference note. */
    private val referenceOctave =
        if (referenceNoteIndexWithinOctave < octaveSwitchIndexWithinNoteNames)
            this.referenceNote.octave
        else
            this.referenceNote.octave - 1

    /** Number of notes contained in the scale. */
    val size = noteNames.size

    /** Return note which belongs to a given index.
     * @param noteIndex Index of note, relative to the reference note (reference note has index 0)
     * @return Note which corresponds to the given index.
     */
    fun getNoteOfIndex(noteIndex: Int): MusicalNote {
        var octave = (noteIndex + referenceNoteIndexWithinOctave) / size + referenceOctave
        var localNoteIndex = (noteIndex + referenceNoteIndexWithinOctave) % size
        if (localNoteIndex < 0) {
            octave -= 1
            localNoteIndex += size
        }
        if (localNoteIndex >= octaveSwitchIndexWithinNoteNames)
            octave += 1
//        Log.v("Tuner", "MusicalScaleNoteNames.getNoteOfIndex: noteIndex=$noteIndex, octave=$octave, localNoteIndex=$localNoteIndex, referenceNoteIndexWithinOctave=$referenceNoteIndexWithinOctave, referenceNote=$referenceNote")
        return noteNames[localNoteIndex].copy(octave = octave)
    }

    /** Return index of a given note.
     * @param musicalNote Some musical note.
     * @return Note index relative to reference note or Int.MAX_VALUE if note is not part of the scale.
     */
    fun getNoteIndex(musicalNote: MusicalNote): Int {
        val localNoteIndex = noteNames.getNoteIndex(musicalNote)
        return if (localNoteIndex < 0) {
            Int.MAX_VALUE
        } else {
            val octave = if (localNoteIndex < octaveSwitchIndexWithinNoteNames)
                    musicalNote.octave
            else
                musicalNote.octave - 1
            (octave - referenceOctave) * size + localNoteIndex - referenceNoteIndexWithinOctave
        }
    }

    /** Return indices of all notes which match the given note.
     * A match means that either a combination of enharmonic or non enharmonic are the
     * same.
     * @param musicalNote Some musical note.
     * @return Note indices relative to reference note, where the note matches.
     */
    fun getMatchingNoteIndices(musicalNote: MusicalNote): IntArray {
        return noteNames.getMatchingNoteIndices(musicalNote).map { localNoteIndex ->
            val octave = if (localNoteIndex < octaveSwitchIndexWithinNoteNames)
                musicalNote.octave
            else
                musicalNote.octave - 1
            (octave - referenceOctave) * size + localNoteIndex - referenceNoteIndexWithinOctave
        }.toIntArray()
    }

    /** Check if a note matches any of the notes in this class.
     * A match means that either a combination of enharmonic or non enharmonic are the
     * same.
     * @param musicalNote Musical note to be checked.
     * @return False if note is null, or has not match. Else true.
     */
    fun hasMatchingNote(musicalNote: MusicalNote?): Boolean {
        return if (musicalNote == null)
            false
        else
            noteNames.hasNote(musicalNote)
    }
}
