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
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale

data class TuningTarget(
    val note: MusicalNote,
    val frequency: Float,
    val isPartOfInstrument: Boolean,
    val instrumentHasNoStrings: Boolean
)

class TuningTargetComputer(
    private val musicalScale: MusicalScale,
    instrument: Instrument?,
    toleranceInCents: Float
) {
    private val instrument = instrument ?: Instrument(name = null, nameResource = null, strings = arrayOf(), iconResource = 0, stableId = 0, isChromatic = true)
    private val sortedAndDistinctInstrumentStrings = SortedAndDistinctInstrumentStrings(this.instrument, musicalScale)
    private val targetNoteAutoDetection = TargetNoteAutoDetection(musicalScale, instrument, toleranceInCents)
    private val targetNoteAutoDetectionChromatic = TargetNoteAutoDetection(musicalScale, null, toleranceInCents)

    operator fun invoke(
        frequency: Float,
        previousTargetNote: MusicalNote?,
        userDefinedTargetNote: MusicalNote?): TuningTarget {

        // check if we directly can use the user defined note and return if yes
        if (userDefinedTargetNote != null) {
            val index = musicalScale.getNoteIndex(userDefinedTargetNote)
            if (index != Int.MAX_VALUE) {
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
            val index = musicalScale.getNoteIndex(detectedTargetNote)
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
        val index = musicalScale.getNoteIndex(chromaticTargetNote!!)
        return TuningTarget(
            chromaticTargetNote,
            musicalScale.getNoteFrequency(index),
            isPartOfInstrument = false,
            instrumentHasNoStrings = !instrument.isChromatic && instrument.strings.isEmpty()
        )
    }
}
