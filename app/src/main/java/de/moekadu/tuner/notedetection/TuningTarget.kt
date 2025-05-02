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
import de.moekadu.tuner.temperaments.centsToRatio
import de.moekadu.tuner.temperaments.ratioToCents
import kotlin.math.absoluteValue
import kotlin.math.min

/** Tuning target.
 * @param note Tuning target note.
 * @param frequency Frequency of target note.
 * @param isPartOfInstrument True, if the note is part of an instrument (this is e.g. false
 *   if there is an instrument without strings, then we still can return a tuning target, but
 *   this is not part of the instrument, but just of the musical scale).
 * @param instrumentHasNoStrings Information if the underlying instrument has strings or not.
 */
data class TuningTarget(
    val note: MusicalNote,
    val frequency: Float,
    val isPartOfInstrument: Boolean,
    val instrumentHasNoStrings: Boolean
)

/** Determine a tuning target.
 * @param musicalScale Musical scale, which is currently used.
 * @param instrument Instruments, which defines the notes in the musical scale that are potential
 *   tuning targets. If null, all notes of the musical scales are tuning targets.
 *  @param toleranceInCents Tolerance in cents for which a not is defined to be in tune. This
 *    is used for better evaluation of hysteresis effects when to switch to a new note.
 */
class TuningTargetComputer(
    private val musicalScale: MusicalScale2,
    instrument: Instrument?,
    private val toleranceInCents: Float
) {
    private val instrument = instrument ?: Instrument(name = null, nameResource = null, strings = arrayOf(), icon = InstrumentIcon.entries[0], stableId = 0, isChromatic = true)
    private val sortedAndDistinctInstrumentStrings = SortedAndDistinctInstrumentStrings(this.instrument, musicalScale)
    private val targetNoteAutoDetection = TargetNoteAutoDetection(musicalScale, instrument, toleranceInCents)
    private val targetNoteAutoDetectionChromatic = TargetNoteAutoDetection(musicalScale, null, toleranceInCents)

    /** Find tuning target.
     * @param frequency Frequency for which the target should be found.
     * @param previousTargetNote Previously used target note. Only used if it is part of the
     *   musical scale.
     * @param userDefinedTargetNote If the user selected manually a target, this is used as a
     *   target.
     */
    operator fun invoke(
        frequency: Float,
        previousTargetNote: MusicalNote?,
        userDefinedTargetNote: MusicalNote?): TuningTarget {

        // check if we directly can use the user defined note and return if yes
        if (userDefinedTargetNote != null) {
            //val index = musicalScale.getNoteIndex(userDefinedTargetNote)
            val indices = musicalScale.getMatchingNoteIndices(userDefinedTargetNote)
            if (indices.isNotEmpty()) {
                val index = findBestMatch(frequency, indices, previousTargetNote)
            //if (index != Int.MAX_VALUE) {
                return TuningTarget(
                    userDefinedTargetNote,
                    musicalScale.getNoteFrequency(index),
                    isPartOfInstrument = sortedAndDistinctInstrumentStrings.isNotePartOfInstrument(userDefinedTargetNote),
                    instrumentHasNoStrings = !instrument.isChromatic && instrument.strings.isEmpty()
                )
            }
        }

        // no frequency, return something, which is not complete nonsense
        if (frequency <= 0f) {
            return TuningTarget(
                musicalScale.referenceNote,
                musicalScale.referenceFrequency,
                isPartOfInstrument = sortedAndDistinctInstrumentStrings.isNotePartOfInstrument(musicalScale.referenceNote),
                instrumentHasNoStrings = !instrument.isChromatic && instrument.strings.isEmpty()
            )
        }

        val detectedTargetNote = targetNoteAutoDetection.detect(frequency, previousTargetNote)
        //Log.v("Tuner", "TuningTarget: detectedTargetNote=$detectedTargetNote, f=$frequency")
        // found note which is part of instrument
        if (detectedTargetNote != null) {
            val index = musicalScale.getNoteIndex2(detectedTargetNote)
            return TuningTarget(
                detectedTargetNote,
                musicalScale.getNoteFrequency(index),
                isPartOfInstrument = true,
                instrumentHasNoStrings = !instrument.isChromatic && instrument.strings.isEmpty()
            )
        }

        // no instrument note available, return the closest chromatic
        val chromaticTargetNote = targetNoteAutoDetectionChromatic.detect(frequency, previousTargetNote)
        // the returned note will always be non null for chromatic instruments and non-zero frequencies
        val index = musicalScale.getNoteIndex2(chromaticTargetNote!!)
        return TuningTarget(
            chromaticTargetNote,
            musicalScale.getNoteFrequency(index),
            isPartOfInstrument = false,
            instrumentHasNoStrings = !instrument.isChromatic && instrument.strings.isEmpty()
        )
    }

    private fun findBestMatch(
        frequency: Float, musicalScaleIndices: IntArray, previousTargetNote: MusicalNote?
    ): Int {
        return if (musicalScaleIndices.size == 2) {
            val f0 = musicalScale.getNoteFrequency(musicalScaleIndices[0])
            val f1 = musicalScale.getNoteFrequency(musicalScaleIndices[1])
            val distInCents = ratioToCents(f0 / f1).absoluteValue
            // normally use toleranceInCents as tolerance, but the distance should be be
            // significantly smaller than the note distance.
            val tolerance = min(distInCents / 4,  toleranceInCents)
            var dist0 = ratioToCents(f0 / frequency).absoluteValue
            var dist1 = ratioToCents(f1 / frequency).absoluteValue
            if (previousTargetNote != null) {
                val previousTargetNoteIndex = musicalScale.getNoteIndex2(previousTargetNote)
                if (musicalScaleIndices[0] == previousTargetNoteIndex)
                    dist0 -= tolerance
                else if (musicalScaleIndices[1] == previousTargetNoteIndex)
                    dist1 -= tolerance
            }
            if (dist0 < dist1) musicalScaleIndices[0] else musicalScaleIndices[1]
        } else {
            // this refers the the case that we have one note.
            // more than 2 note should never happen, if notes in musical scale are unique.
            // so a correct handling for this is not implemented.
            musicalScaleIndices[0]
        }
    }
}
