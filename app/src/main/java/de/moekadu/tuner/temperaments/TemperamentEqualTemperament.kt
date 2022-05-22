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

import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt

/** Class containing notes for equal temperament.
 *
 * @param numNotesPerOctave Number of notes per octave
 * @param noteIndexAtReferenceFrequency Index of note which should have the reference frequency
 * @param _referenceFrequency Frequency of note at given index (noteIndexAtReferenceFrequency)
 */
class TemperamentEqualTemperament(
    private val musicalNotes: Array<MusicalNote> = noteSet12ToneSharp,
    override val numberOfNotesPerOctave: Int = 12,
    override val rootNote: MusicalNote = MusicalNote(BaseNote.C, NoteModifier.None, 4),
    override val referenceNote: MusicalNote = MusicalNote(BaseNote.A, NoteModifier.None, 4),
    override val referenceFrequency: Float = 440f,
    private val frequencyMin: Float = 16.0f,  // 16.4Hz would be c0 if the a4 is 440Hz
    private val frequencyMax: Float = 17000f, //16744.1f  // 16744Hz would be c10 if the a4 is 440Hz
) : MusicalScale {
    /// Ratio between two neighboring half tones
    private val halfToneRatio = 2.0f.pow(1.0f / numberOfNotesPerOctave)

    override val circleOfFifths: TemperamentCircleOfFifths?
        get() {
            return if (numberOfNotesPerOctave == 12)
                circleOfFifthsEDO12
            else
                null
        }

    override val rationalNumberRatios: Array<RationalNumber>? = null
    override val temperamentType: TemperamentType
        get() {
            return when (numberOfNotesPerOctave){
                12 -> TemperamentType.EDO12
                else -> throw RuntimeException("Equal temperament for $numberOfNotesPerOctave number of notes is not implemented")
            }
        }

    private val noteIndexOfReferenceNote = -getNoteIndexRelativeToReferenceNote(frequencyMin).roundToInt()

    override val numberOfNotes: Int =
        (getNoteIndexRelativeToReferenceNote(frequencyMax).roundToInt() + 1 + noteIndexOfReferenceNote)

    override fun getNoteIndex(frequency: Float)  : Float {
        return getNoteIndexRelativeToReferenceNote(frequency) + noteIndexOfReferenceNote
    }

    override fun getClosestNoteIndex(frequency : Float)  : Int {
        return getNoteIndex(frequency).roundToInt()
    }

    override fun getNoteFrequency(noteIndex : Int) : Float {
       return referenceFrequency * halfToneRatio.pow(noteIndex - noteIndexOfReferenceNote)
    }

   override fun getNoteFrequency(noteIndex : Float) : Float {
       return referenceFrequency * halfToneRatio.pow(noteIndex - noteIndexOfReferenceNote)
    }

    override fun getClosestNote(frequency: Float): MusicalNote {
        val i = getClosestNoteIndex(frequency)

    }

    override fun getNote(noteIndex: Int): MusicalNote {
        val noteIndexRelativeToReferenceNote = noteIndex - noteIndexOfReferenceNote

    }

    private fun getNoteIndexRelativeToReferenceNote(frequency: Float): Float {
        return log(frequency / referenceFrequency, halfToneRatio)
    }
}
