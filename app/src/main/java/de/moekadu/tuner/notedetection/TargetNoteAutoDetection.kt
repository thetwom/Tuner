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
package de.moekadu.tuner.notedetection

import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentIcon
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.musicalscale.MusicalScale2
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private fun ratioToCents(ratio: Float): Float {
    return (1200.0 * log(ratio.toDouble(), 2.0)).toFloat()
}

private fun centsToRatio(cents: Float): Float {
    return (2.0.pow(cents / 1200.0)).toFloat()
}

/** Target note detector.
 * @param musicalScale Musical scale.
 * @param instrument Instrument which defines the possible target notes. If null, all note of the
 *   musical scale are possible target notes.
 *  @param toleranceInCents Tolerance in cents for which a not is defined to be in tune. This
 *    is used for better evaluation of hysteresis effects when to switch to a new note.
 */
class TargetNoteAutoDetection(
    private val musicalScale: MusicalScale2,
    instrument: Instrument?,
    private val toleranceInCents: Float
    ) {

    private val instrument = instrument ?: Instrument(name = null, nameResource = null, strings = arrayOf(), icon = InstrumentIcon.entries[0], stableId = 0, isChromatic = true)

    private val sortedAndDistinctInstrumentStrings = SortedAndDistinctInstrumentStrings(this.instrument, musicalScale)
    private val sortedAndDistinctNoteIndices get() = sortedAndDistinctInstrumentStrings.sortedAndDistinctNoteIndices
    private val numDifferentNotes get() = sortedAndDistinctInstrumentStrings.numDifferentNotes

    /** We need some hysteresis effect, before we changing our current tone estimate
     * A value of 0.5f would mean that if we exactly between two possible target note,
     * we change our pitch, but this would have no hysteresis effect. So better give
     * a value somewhere between 0.5f and 1.0f
     */
    private val relativeDeviationForChangingTarget = 0.6f

    /** Minimum cent deviation before for changing target.
     * Basically, the relativeDeviationForChangingTarget defines at which point we should
     * jump to next target note. However, for target notes which are extremely close
     * (e.g. when scale is EDO 41 and we use chromatic target notes), this can lead to
     * high-frequency jumps between target note. So, here we can give a minimum ratio
     * such that we must be closer to the next note that the relativeDeviationForChangingTarget
     * defines. E.g. by setting it to 20cents, we don't change to a new target note if
     * we are not more than 20 cents away. However, this value is reduced to
     * "target note ratio - tolerance". So if two target notes are only 15cents apart
     * and the tolerance is 5cents, the real minimumCentDeviationBeforeChangingTone
     * is 15-5 = 10cents.
     */
    private val minimumCentDeviationBeforeChangingTarget = 20f

    /** Detect target note based on a given frequency
     * @param frequency The frequency for which we should set the target note
     * @param previousNote Previously used target note, which will be preferred. Use null to
     *   get an unbiased output.
     * @return Target note or null for invalid frequencies ore if the instrument has not strings
     *   which matches the musical scale.
     */
    fun detect(frequency: Float, previousNote: MusicalNote? = null): MusicalNote? {
        if (frequency <= 0f || (!instrument.isChromatic && numDifferentNotes == 0))
            return null

        val frequencyRange = getFrequencyRangeWithinWhichWeReturnTheInputNote(previousNote)
//        Log.v("Tuner", "TargetNoteAutoDetection: frequencyRange=${frequencyRange[0]}--${frequencyRange[1]}, frequency=$frequency")
        if (frequency in frequencyRange[0] .. frequencyRange[1]
            && sortedAndDistinctInstrumentStrings.isNotePartOfInstrument(previousNote)) {
            return previousNote
        }

//        Log.v("Tuner", "TargetNote: setTargetNoteBasedOnFrequency: instrument.isChromatic: ${instrument.isChromatic} || numDifferentNotes==$numDifferentNotes")
        return when {
            instrument.isChromatic -> {
                val noteIndex = musicalScale.getClosestNoteIndex(frequency)
                musicalScale.getNote(noteIndex)
                //musicalScale.getClosestNote(frequency)
            }
            numDifferentNotes == 1 -> {
                musicalScale.getNote(sortedAndDistinctNoteIndices[0])
            }
            else -> {
                val exactNoteIndex = musicalScale.getNoteIndex(frequency)
                var index = sortedAndDistinctNoteIndices.binarySearchBy(exactNoteIndex) { it.toFloat() }
                //var index = instrument.stringsSorted.binarySearch(exactNoteIndex)
                if (index < 0)
                    index = -(index + 1)

                val uniqueNoteListIndex = when {
                    index == 0 -> 0
                    index >= numDifferentNotes -> numDifferentNotes - 1
                    exactNoteIndex - sortedAndDistinctNoteIndices[index - 1] < sortedAndDistinctNoteIndices[index] - exactNoteIndex -> index - 1
                    else -> index
                }
                musicalScale.getNote(sortedAndDistinctNoteIndices[uniqueNoteListIndex])
            }
        }
    }

    private fun getFrequencyRangeWithinWhichWeReturnTheInputNote(note: MusicalNote?): FloatArray {
        when {
            note == null || (!instrument.isChromatic && numDifferentNotes == 0) -> { // never return the input note
//                Log.v("Tuner", "TargetNoteAutoDetection: (!instrument.isChromatic && numDifferentNotes == 0)")
                return floatArrayOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
            }
            instrument.isChromatic -> {
//                Log.v("Tuner", "TargetNoteAutoDetection: instrument.isChromatic")
                val noteIndex = musicalScale.getNoteIndex(note)
//                Log.v("Tuner", "TargetNoteAutoDetection.getFrequencyRangeWithinWhichWeReturnTheInputNote: note=$note, noteIndex=$noteIndex")
                return setFrequencyRangeForChromaticTarget(noteIndex)
            }
            numDifferentNotes <= 1 -> { // always return the input note
//                Log.v("Tuner", "TargetNoteAutoDetection: numDifferentNotes <= 1, instrument=$instrument")
                return floatArrayOf(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)
            }
            else -> {
//                Log.v("Tuner", "TargetNoteAutoDetection: else")
                val noteIndex = musicalScale.getNoteIndex(note)
                val sortedStringListIndex = sortedAndDistinctNoteIndices.binarySearch(noteIndex)

                return if (sortedStringListIndex < 0 || sortedAndDistinctNoteIndices[sortedStringListIndex] == Int.MAX_VALUE) {
                    floatArrayOf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY) // never return input note
                } else {
                    setFrequencyRangeForInstrumentTarget(sortedStringListIndex)
                }
            }
        }
    }
    /** Set frequencyRange for targetNoteIndex.
     *
     * Lets say, we have the following scenario where M1-M5 are just references used later in the text:
     *
     * M1: ========= frequency of next target note I + 1 (frequencyOfUpperTarget) ==============
     *
     * M2: ------- frequency bound of target note I + 1 (frequencyOfUpperTarget - tolerance) ---
     *
     *
     * M3: ============ frequency of current target note (centerFrequency) ====================
     *
     *
     *  M4: ------- frequency bound of target note I - 1 (frequencyOfLowerTarget + tolerance) ---
     *
     * M5: ========= frequency of next target note I - 1 (frequencyOfLowerTarget) ==============
     *
     * Then we define two frequency ranges:
     * - A deviation based frequency range (e.g. range is 60% of space between center and upper/lower target.)
     * - A cent based frequency range (e.g. range is 20 cents away from center).
     * Which range to use? We use the larger one, BUT for the cent-based frequency range, we have
     * one condition:
     * - It is not allowed to exceed the frequency bound of target note I +/- 1 (M2/M4 in the picture)
     * @param targetNoteIndex Index of target note.
     * @return Array with min and max frequency of range.
     */
    private fun setFrequencyRangeForChromaticTarget(targetNoteIndex: Int): FloatArray {

        val centerFrequency = musicalScale.getNoteFrequency(targetNoteIndex)

        val lowerFrequencyDeviationBased = musicalScale.getNoteFrequency(targetNoteIndex - relativeDeviationForChangingTarget)
        val upperFrequencyDeviationBased = musicalScale.getNoteFrequency(targetNoteIndex + relativeDeviationForChangingTarget)

        val frequencyOfLowerTarget = musicalScale.getNoteFrequency(targetNoteIndex - 1)
        val frequencyOfUpperTarget = musicalScale.getNoteFrequency(targetNoteIndex + 1)

        // in the picture above these are the cents between M3 and M4, as well as between M3 and M2
        val centsToLowerTargetBound = ratioToCents(centerFrequency / frequencyOfLowerTarget) - toleranceInCents
        val centsToUpperTargetBound = ratioToCents(frequencyOfUpperTarget / centerFrequency) - toleranceInCents

        val lowerFrequencyCentBased = centerFrequency / centsToRatio(min(minimumCentDeviationBeforeChangingTarget, centsToLowerTargetBound))
        val upperFrequencyCentBased = centerFrequency * centsToRatio(min(minimumCentDeviationBeforeChangingTarget, centsToUpperTargetBound))

        return floatArrayOf(
            min(lowerFrequencyDeviationBased, lowerFrequencyCentBased),
            max(upperFrequencyDeviationBased, upperFrequencyCentBased)
        )
    }

    /** See description for setFrequencyRangeForChromaticTarget for details.
     * @param sortedStringListIndex Index within sorted list of strings.
     * @return Array with min and max frequency of range.
     */
    private fun setFrequencyRangeForInstrumentTarget(sortedStringListIndex: Int): FloatArray {

        val centerFrequency = musicalScale.getNoteFrequency(sortedAndDistinctNoteIndices[sortedStringListIndex])

        val lowerFrequency = if (sortedStringListIndex == 0) {
            Float.NEGATIVE_INFINITY
        } else {
            val lowerFrequencyDeviationBased = musicalScale.getNoteFrequency(
                (1.0f - relativeDeviationForChangingTarget) * sortedAndDistinctNoteIndices[sortedStringListIndex]
                        + relativeDeviationForChangingTarget * sortedAndDistinctNoteIndices[sortedStringListIndex - 1])
            val frequencyOfLowerTarget = musicalScale.getNoteFrequency(sortedAndDistinctNoteIndices[sortedStringListIndex - 1])
            // in the picture above these are the cents between M3 and M4, as well as between M3 and M2
            val centsToLowerTargetBound = ratioToCents(centerFrequency / frequencyOfLowerTarget) - toleranceInCents
            val lowerFrequencyCentBased = centerFrequency / centsToRatio(min(minimumCentDeviationBeforeChangingTarget, centsToLowerTargetBound))
            min(lowerFrequencyDeviationBased, lowerFrequencyCentBased)
        }

        val upperFrequency = if (sortedStringListIndex == numDifferentNotes - 1) {
            Float.POSITIVE_INFINITY
        } else {
            val upperFrequencyDeviationBased = musicalScale.getNoteFrequency(
                (1.0f - relativeDeviationForChangingTarget) * sortedAndDistinctNoteIndices[sortedStringListIndex]
                        + relativeDeviationForChangingTarget * sortedAndDistinctNoteIndices[sortedStringListIndex + 1]
            )
            val frequencyOfUpperTarget = musicalScale.getNoteFrequency(sortedAndDistinctNoteIndices[sortedStringListIndex + 1])
            val centsToUpperTargetBound = ratioToCents(frequencyOfUpperTarget / centerFrequency) - toleranceInCents
            val upperFrequencyCentBased = centerFrequency * centsToRatio(min(minimumCentDeviationBeforeChangingTarget, centsToUpperTargetBound))
            max(upperFrequencyDeviationBased, upperFrequencyCentBased)
        }
        return floatArrayOf(lowerFrequency, upperFrequency)
    }
}