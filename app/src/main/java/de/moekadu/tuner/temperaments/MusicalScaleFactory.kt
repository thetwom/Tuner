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
package de.moekadu.tuner.temperaments

import de.moekadu.tuner.misc.DefaultValues
import kotlin.math.pow

private fun centsToRatio(cents: Double): Double {
    return 2.0.pow(cents / 1200.0)
}


//class MusicalScaleFactory {
//    companion object {
//
//        fun create(
//            temperamentType: TemperamentType,
//            noteNameScale: NoteNameScale,
//            referenceNote: MusicalNote? = null,
//            rootNote: MusicalNote? = null,
//            referenceFrequency: Float = 440f, //DefaultValues.REFERENCE_FREQUENCY,
//            frequencyMin: Float = 16.0f,
//            frequencyMax: Float = 17000.0f
//        ): MusicalScale {
//            val rootNoteResolved = rootNote ?: noteNameScale.notes[0]
//            val referenceNoteResolved = referenceNote ?: noteNameScale.referenceNote
//            return when (temperamentType) {
//                TemperamentType.EDO12 -> MusicalScaleEqualTemperament(
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.Pythagorean -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType,
//                    circleOfFifthsPythagorean,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.Pure -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, rationalNumberTemperamentPure,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.QuarterCommaMeanTone -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, circleOfFifthsQuarterCommaMeanTone,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.ExtendedQuarterCommaMeanTone -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, extendedQuarterCommaMeantone.map { centsToRatio(it) }.toDoubleArray(),
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.ThirdCommaMeanTone -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, circleOfFifthsThirdCommaMeanTone,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.FifthCommaMeanTone -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, circleOfFifthsFifthCommaMeanTone,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.WerckmeisterIII -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, circleOfFifthsWerckmeisterIII,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.WerckmeisterIV -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, circleOfFifthsWerckmeisterIV,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.WerckmeisterV -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, circleOfFifthsWerckmeisterV,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.WerckmeisterVI -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, rationalNumberTemperamentWerckmeisterVI,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.Kirnberger1 -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, circleOfFifthsKirnberger1,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.Kirnberger2 -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, circleOfFifthsKirnberger2,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.Kirnberger3 -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, circleOfFifthsKirnberger3,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.Neidhardt1 -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, circleOfFifthsNeidhardt1,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.Neidhardt2 -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, circleOfFifthsNeidhardt2,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.Neidhardt3 -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, circleOfFifthsNeidhardt3,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
////                Tuning.Neidhardt4 -> TuningRatioBased(
////                    tuning, circleOfFifthsNeidhardt4,
////                   noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
////                )
//                TemperamentType.Valotti -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, circleOfFifthsValotti,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.Young2 -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, circleOfFifthsYoung2,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.EDO17 -> MusicalScaleEqualTemperament(
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.EDO19 -> MusicalScaleEqualTemperament(
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.EDO22 -> MusicalScaleEqualTemperament(
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.EDO24 -> MusicalScaleEqualTemperament(
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.EDO27 -> MusicalScaleEqualTemperament(
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.EDO29 -> MusicalScaleEqualTemperament(
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.EDO31 -> MusicalScaleEqualTemperament(
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.EDO41 -> MusicalScaleEqualTemperament(
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.EDO53 -> MusicalScaleEqualTemperament(
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//                TemperamentType.Test -> MusicalScaleRatioBasedTemperaments(
//                    temperamentType, rationalNumberTemperamentTest,
//                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
//            }
//        }
//        fun create(
//            temperamentType: TemperamentType,
//            referenceNote: MusicalNote? = null,
//            rootNote: MusicalNote? = null,
//            referenceFrequency: Float = DefaultValues.REFERENCE_FREQUENCY,
//            frequencyMin: Float = 16.0f,
//            frequencyMax: Float = 17000.0f
//        ): MusicalScale {
//            val noteNameScale = NoteNameScaleFactory.create(temperamentType)
//            return create(temperamentType, noteNameScale, referenceNote, rootNote,
//                referenceFrequency, frequencyMin, frequencyMax)
//        }
//    }
//}