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

import androidx.compose.runtime.Immutable
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteNames
import de.moekadu.tuner.stretchtuning.StretchTuning
import de.moekadu.tuner.temperaments.Temperament
import de.moekadu.tuner.temperaments.Temperament3
import de.moekadu.tuner.temperaments.predefinedTemperamentEDO
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Old musical scale class. */
@Serializable
@Immutable
data class MusicalScale(
    val temperament: Temperament,
    val noteNames: NoteNames,
    val rootNote: MusicalNote,
    val referenceNote: MusicalNote,
    val referenceFrequency: Float,
    val frequencyMin: Float,
    val frequencyMax: Float,
    val stretchTuning: StretchTuning
) {
    fun toNew(): MusicalScale2 {
        return MusicalScale2(
            temperament = temperament.toNew(noteNames),
            _rootNote = rootNote,
            _referenceNote = referenceNote,
            referenceFrequency = referenceFrequency,
            frequencyMin = frequencyMin,
            frequencyMax = frequencyMax,
            _stretchTuning = stretchTuning
        )
    }
}

@Serializable
@Immutable
data class MusicalScale2(
    val temperament: Temperament3,
    val _rootNote: MusicalNote?,
    val _referenceNote: MusicalNote?,
    val referenceFrequency: Float,
    val frequencyMin: Float,
    val frequencyMax: Float,
    val _stretchTuning: StretchTuning?
) {
    @Transient
    val numberOfNotesPerOctave = temperament.size

    @Transient
    private val noteNameScale
            = MusicalScaleNoteNames2(temperament.noteNames(_rootNote), _referenceNote)

    val rootNote get() = noteNameScale.noteNames[0]

    val referenceNote get() = noteNameScale.referenceNote

    @Transient
    val stretchTuning = _stretchTuning ?: StretchTuning()

    @Transient
    private val musicalScaleFrequencies = MusicalScaleFrequencies.create(
        temperament.cents(),
        noteNameScale.referenceNoteIndexWithinOctave,
        referenceFrequency,
        frequencyMin,
        frequencyMax,
        stretchTuning
    )

    /** Smallest note index (included). */
    @Transient
    val noteIndexBegin: Int = musicalScaleFrequencies.indexStart
    /** Last note index (excluded). */
    @Transient
    val noteIndexEnd: Int = musicalScaleFrequencies.indexEnd

    /** Obtain note representation based on class internal note index.
     * @param noteIndex Local index of note (noteIndexBegin <= noteIndex < noteIndexEnd).
     * @return Musical note representation.
     */
    fun getNote(noteIndex: Int): MusicalNote {
        return noteNameScale.getNoteOfIndex(noteIndex)
    }

    /** Obtain note frequency of a given index relative to the reference note.
     * @param noteIndex Note index relative to reference note,
     *   (noteIndexBegin <= noteIndex < noteIndexEnd).
     * @return Note frequency.
     */
    fun getNoteFrequency(noteIndex: Int): Float {
        return musicalScaleFrequencies[noteIndex]
    }

    /** Obtain note frequency based on class internal note index for noteIndex type Float.
     * This method allows using "odd" note indices.
     * @param noteIndex Local floating point index relative to reference note
     *   (noteIndexBegin <= noteIndex < noteIndexEnd).
     * @return Note frequency.
     */
    fun getNoteFrequency(noteIndex: Float): Float {
        return musicalScaleFrequencies[noteIndex]
    }

    /** Get index relative to reference note of a given frequency.
     * This method can return non-integer note indices, so if the frequency is between two notes
     * we still return a meaningful index as a value between the two notes.
     * @param frequency Frequency.
     * @return Note index as float.
     */
    fun getNoteIndex(frequency: Float): Float {
        return musicalScaleFrequencies.getFrequencyIndex(frequency)
    }

    /** Get index relative to reference note which is closest to a given frequency.
     * @param frequency Frequency.
     * @return Note index.
     */
    fun getClosestNoteIndex(frequency: Float): Int {
        return musicalScaleFrequencies.getClosestFrequencyIndex(frequency)
    }

    /** Get note index of a given musical note representation.
     * @param note Musical note representation.
     * @return Local index of the note or Int.MAX_VALUE if not does not exist in scale.
     */
    fun getNoteIndex(note: MusicalNote): Int {
        return noteNameScale.getIndexOfNote(note)
    }

    companion object {
        fun createTestEdo12(): MusicalScale2 {
            return MusicalScale2(
                predefinedTemperamentEDO(12, 1L),
                _rootNote = null,
                _referenceNote = null,
                referenceFrequency = 440f,
                frequencyMin = 30f,
                frequencyMax = 18000f,
                _stretchTuning = null
            )
        }
    }
}

