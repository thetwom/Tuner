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
import de.moekadu.tuner.notenames.NoteNames
import de.moekadu.tuner.notenames.NoteNames2

/** Note names, which can map indices between musical notes and via verse.
 * @param noteNames Note names of one octave.
 * @param referenceNote Reference note of the scale, which refers to noteIndex 0.
 *   This note must be part of the note names.
 */
@Stable
class MusicalScaleNoteNames2(
    val noteNames: NoteNames2,
    val referenceNote: MusicalNote
) {
    /** Reference note index within the octave (index in notes). */
    private val referenceNoteIndexWithinNoteNames = noteNames.getNoteIndex(referenceNote)

    /** Reference note index within the octave (index in notes). */
    private val octaveSwitchIndexWithinNoteNames
            = noteNames.getNoteIndex(noteNames.firstNoteOfOctave)

    /** Octave of reference note. */
    private val referenceOctave =
        if (referenceNoteIndexWithinNoteNames < octaveSwitchIndexWithinNoteNames)
            referenceNote.octave
        else
            referenceNote.octave - 1

    /** Number of notes contained in the scale. */
    val size = noteNames.size

    /** Return note which belongs to a given index.
     * @param noteIndex Index of note, relative to the reference note (reference note has index 0)
     * @return Note which corresponds to the given index.
     */
    fun getNoteOfIndex(noteIndex: Int): MusicalNote {
        var octave = (noteIndex + referenceNoteIndexWithinNoteNames) / size + referenceOctave
        var localNoteIndex = (noteIndex + referenceNoteIndexWithinNoteNames) % size
        if (localNoteIndex < 0) {
            octave -= 1
            localNoteIndex += size
        }
        if (localNoteIndex >= octaveSwitchIndexWithinNoteNames)
            octave += 1
//        Log.v("StaticLayoutTest", "NoteNameScale.getNoteOfIndex: noteIndex=$noteIndex, octave=$octave, localNoteIndex=$localNoteIndex, referenceNoteIndexWithinOctave=$referenceNoteIndexWithinOctave")
        return noteNames[localNoteIndex].copy(octave = octave)
    }

    /** Return index of a given note.
     * @param musicalNote Some musical note.
     * @return Note index relative to reference note or Int.MAX_VALUE if note is not part of the scale.
     */
    fun getIndexOfNote(musicalNote: MusicalNote): Int {
        val localNoteIndex = noteNames.getNoteIndex(musicalNote)
        return if (localNoteIndex < 0) {
            Int.MAX_VALUE
        } else {
            val octave = if (localNoteIndex < octaveSwitchIndexWithinNoteNames)
                    musicalNote.octave
            else
                musicalNote.octave - 1
            (octave - referenceOctave) * size + localNoteIndex - referenceNoteIndexWithinNoteNames
        }
    }
}

/** Note names, which can map indices between musical notes and via verse.
 * @param noteNames Note names of one octave.
 * @param referenceNote Reference note of the scale, which refers to noteIndex 0.
 *   This note must be part of the note names.
 */
@Stable
class MusicalScaleNoteNames(
    val noteNames: NoteNames,
    val referenceNote: MusicalNote
) {
    /** Reference note index within the octave (index in notes). */
    private val referenceNoteIndexWithinOctave = noteNames.getNoteIndex(referenceNote)

    /** Octave of reference note. */
    private val referenceNoteOctave = referenceNote.octave

    /** Number of notes contained in the scale. */
    val size = noteNames.size

    /** Return note which belongs to a given index.
     * @param noteIndex Index of note, relative to the reference note (reference note has index 0)
     * @return Note which corresponds to the given index.
     */
    fun getNoteOfIndex(noteIndex: Int): MusicalNote {
        var octave = (noteIndex + referenceNoteIndexWithinOctave) / size + referenceNoteOctave
        var localNoteIndex = (noteIndex + referenceNoteIndexWithinOctave) % size
        if (localNoteIndex < 0) {
            octave -= 1
            localNoteIndex += size
        }
//        Log.v("StaticLayoutTest", "NoteNameScale.getNoteOfIndex: noteIndex=$noteIndex, octave=$octave, localNoteIndex=$localNoteIndex, referenceNoteIndexWithinOctave=$referenceNoteIndexWithinOctave")
        return noteNames[localNoteIndex].copy(octave = octave)
    }

    /** Return index of a given note.
     * @param musicalNote Some musical note.
     * @return Note index relative to reference note or Int.MAX_VALUE if note is not part of the scale.
     */
    fun getIndexOfNote(musicalNote: MusicalNote): Int {
        val localNoteIndex = noteNames.getNoteIndex(musicalNote)
        return if (localNoteIndex < 0) {
            Int.MAX_VALUE
        } else {
            (musicalNote.octave - referenceNoteOctave) * size + localNoteIndex - referenceNoteIndexWithinOctave
        }
    }

    /** Return a scale where the note names and modifiers are switch with their enharmonics.
     * @return New note name scale where notes and enharmonics are exchanged.
     */
    fun switchEnharmonic(): MusicalScaleNoteNames {
        return MusicalScaleNoteNames(
            noteNames.switchEnharmonics(),
            referenceNote.switchEnharmonic()
        )
    }

    /** Check if a note is part this class instance.
     * @param note Note.
     * @return True if note is part of this class instance, else false.
     */
    fun hasNote(note: MusicalNote): Boolean {
        return noteNames.hasNote(note)
    }
}

