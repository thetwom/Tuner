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

package de.moekadu.tuner

import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt

/** Class containing notes for equal temperament.
 *
 * @param numNotesPerOctave Number of notes per octave
 * @param noteIndexAtReferenceFrequency Index of note which should have the reference frequency
 * @param _referenceFrequency Frequency of note at given index (noteIndexAtReferenceFrequency)
 */
class TuningEqualTemperament(
    private val _tuning: Tuning,
    private val numNotesPerOctave: Int = 12,
    private val noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
    private val _referenceFrequency: Float = 440f,
    private val frequencyMin: Float = 16.0f,  // 16.4Hz would be c0 if the a4 is 440Hz
    private val frequencyMax: Float = 17000f, //16744.1f  // 16744Hz would be c10 if the a4 is 440Hz
) : TuningFrequencies {
    /// Ratio between two neighboring half tones
    private val halfToneRatio = 2.0f.pow(1.0f / numNotesPerOctave)

    override fun getCircleOfFifths(): TuningCircleOfFifths? {
        return if (numNotesPerOctave == 12)
            circleOfFifthsEDO12
        else
            null
    }

    override fun getRationalNumberRatios(): Array<RationalNumber>? {
        return null
    }

    override fun getTuning(): Tuning {
        return _tuning
    }

    override fun getRootNote(): Int {
        return 0
    }

    override fun getNumberOfNotesPerOctave(): Int {
        return numNotesPerOctave
    }

    override fun getIndexOfReferenceNote(): Int {
        return noteIndexAtReferenceFrequency
    }

    override fun getReferenceFrequency(): Float {
        return _referenceFrequency
    }

    override fun getToneIndexBegin(): Int {
        return getClosestToneIndex(frequencyMin)
    }

    override fun getToneIndexEnd(): Int {
        return getClosestToneIndex(frequencyMax) + 1
    }
    /** Get tone index for the given frequency.
     *
     * @note We return a float here since a frequency can lay between two tones
     * @param frequency Frequency
     * @return Note index.
     */
    override fun getToneIndex(frequency : Float)  : Float {
        return log(frequency / _referenceFrequency, halfToneRatio) + noteIndexAtReferenceFrequency
    }

    /** Get tone index which is closest to the given frequency.
     *
     * @param frequency Frequency
     * @return Note index which is needed by several other class methods.
     */
    override fun getClosestToneIndex(frequency : Float)  : Int {
        return getToneIndex(frequency).roundToInt()
    }

    /** Get frequency of note with the given index.
     *
     * @param noteIndex Note index as e.g. returned by getClosestToneIndex. Two succeeding
     *   indices give a distance of one half tone.
     * @return Frequency of note index.
     */
    override fun getNoteFrequency(noteIndex : Int) : Float {
       return _referenceFrequency * halfToneRatio.pow(noteIndex - noteIndexAtReferenceFrequency)
    }

    /** Get frequency of note with the given index, where the index can also be in between two notes.
     *
     * @param noteIndex Note index as e.g. returned by getClosestToneIndex. Two succeeding
     *   indices give a distance of one half tone.
     * @return Frequency for note index.
    */
   override fun getNoteFrequency(noteIndex : Float) : Float {
       return _referenceFrequency * halfToneRatio.pow(noteIndex - noteIndexAtReferenceFrequency)
    }
}
