/*
 * Copyright 2020 Michael Moessner
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

package de.moekadu.tuner.temperaments

interface MusicalScale {
    val temperamentType: TemperamentType
    val noteNameScale: NoteNameScale
    val rootNote: MusicalNote
    val referenceNote: MusicalNote
    val referenceFrequency: Float
    val numberOfNotesPerOctave: Int
    /// Smallest note index (included).
    val noteIndexBegin: Int
    /// Last note index (excluded).
    val noteIndexEnd: Int
    /// Return circle of fifth instance if the underlying tuning can provide one.
    val circleOfFifths: TemperamentCircleOfFifths?
    /// Return the ratios as rational numbers (first one 1/1 and octave 2/1 must be included) if possible
    val rationalNumberRatios: Array<RationalNumber>?

    /** Obtain note representation based on class internal note index.
     * @param noteIndex Local index of note (noteIndexBegin <= noteIndex < noteIndexEnd).
     * @return Musical note representation.
     */
    fun getNote(noteIndex: Int): MusicalNote {
        return noteNameScale.getNoteOfIndex(noteIndex)
    }

    /** Obtain note frequency based on class internal note index for noteIndex type Int.
     * @param noteIndex Local index of note (noteIndexBegin <= noteIndex < noteIndexEnd).
     * @return Note frequency.
     */
    fun getNoteFrequency(noteIndex: Int): Float

    /** Obtain note frequency based on class internal note index for noteIndex type Float.
     * This method allows using "odd" note indices.
     * @param noteIndex Local index as float of note (noteIndexBegin <= noteIndex < noteIndexEnd).
     * @return Note frequency.
     */
    fun getNoteFrequency(noteIndex: Float): Float

    /** Get note representation of note closest to given frequency.
     * @param frequency Frequency.
     * @return Note representation of note closest to given frequency.
     */
    fun getClosestNote(frequency: Float): MusicalNote

    /** Get local note index of a given frequency.
     * This method can return non-integer note indices, so if the frequency is between two notes
     * we still return a meaningful index as a value between the two notes.
     * @param frequency Frequency.
     * @return Note index as float.
     */
    fun getNoteIndex(frequency: Float): Float

    /** Get local note index which si closest to a given frequency.
     * @param frequency Frequency.
     * @return Note index.
     */
    fun getClosestNoteIndex(frequency: Float): Int

    /** Get note index of a given musical note representation.
     * @param note Musical note representation.
     * @return Local index of the note or Int.MAX_VALUE if not does not exist in scale.
     */
    fun getNoteIndex(note: MusicalNote): Int
}
