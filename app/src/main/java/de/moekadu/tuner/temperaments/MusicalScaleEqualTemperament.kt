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
 * @param noteNameScale Note names of the scale, which also maps note indices to note names.
 * @param referenceNote Note which should have the referenceFrequency.
 * @param referenceFrequency Frequency of reference note.
 * @param rootNote Root note of temperament, this is not needed for equal temperaments, but
 *   we still store it, so that if one switches between different temperaments, we don't loose
 *   the info of which rootNote was used before.
 * @param frequencyMin Minimum frequency to be covered by the scale.
 * @param frequencyMax Maximum frequency to be covered by the scale.
 */
class MusicalScaleEqualTemperament(
    override val noteNameScale: NoteNameScale,
    override val referenceNote: MusicalNote,
    override val referenceFrequency: Float = 440f,
    override val rootNote: MusicalNote = MusicalNote(BaseNote.C, NoteModifier.None),
    private val frequencyMin: Float = 16.0f,  // 16.4Hz would be c0 if the a4 is 440Hz
    private val frequencyMax: Float = 17000f, //16744.1f  // 16744Hz would be c10 if the a4 is 440Hz
) : MusicalScale {
    override val numberOfNotesPerOctave: Int = noteNameScale.size

    /** Ratio between two neighboring half tones. */
    private val halfToneRatio = 2.0f.pow(1.0f / numberOfNotesPerOctave)

    /** Note index of reference note. */
    private val noteIndexOfReferenceNote = noteNameScale.getIndexOfNote(referenceNote)

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
                17 -> TemperamentType.EDO17
                19 -> TemperamentType.EDO19
                22 -> TemperamentType.EDO22
                24 -> TemperamentType.EDO24
                27 -> TemperamentType.EDO27
                29 -> TemperamentType.EDO29
                31 -> TemperamentType.EDO31
                41 -> TemperamentType.EDO41
                53 -> TemperamentType.EDO53
                else -> throw RuntimeException("Equal temperament for $numberOfNotesPerOctave number of notes is not implemented")
            }
        }

    override val noteIndexBegin: Int = getNoteIndex(frequencyMin).roundToInt()
    override val noteIndexEnd: Int = getNoteIndex(frequencyMax).roundToInt() + 1

    override fun getNoteIndex(frequency: Float) : Float {
        return getNoteIndexRelativeToReferenceNote(frequency) + noteIndexOfReferenceNote
    }

    override fun getNoteIndex(note: MusicalNote): Int {
        return noteNameScale.getIndexOfNote(note)
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
        val noteIndex = getClosestNoteIndex(frequency)
        return noteNameScale.getNoteOfIndex(noteIndex)
    }

    override fun getNote(noteIndex: Int): MusicalNote {
        return noteNameScale.getNoteOfIndex(noteIndex)
    }

    private fun getNoteIndexRelativeToReferenceNote(frequency: Float): Float {
        return log(frequency / referenceFrequency, halfToneRatio)
    }
}
