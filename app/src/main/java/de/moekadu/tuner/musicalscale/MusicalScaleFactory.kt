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

//import de.moekadu.tuner.notenames.MusicalNote
//import de.moekadu.tuner.notenames.NoteNames
//import de.moekadu.tuner.notenames.generateNoteNames
//import de.moekadu.tuner.stretchtuning.StretchTuning
//import de.moekadu.tuner.temperaments.Temperament2
//import de.moekadu.tuner.temperaments.createTestTemperamentEdo12
//import de.moekadu.tuner.temperaments.createTestTemperamentWerckmeisterVI
//
//class MusicalScaleFactory {
//    companion object {
//        fun create(
//            temperament: Temperament2,
//            noteNames: NoteNames?,
//            referenceNote: MusicalNote?,
//            rootNote: MusicalNote?,
//            referenceFrequency: Float,
//            frequencyMin: Float,
//            frequencyMax: Float,
//            stretchTuning: StretchTuning
//        ): MusicalScale2 {
//            val noteNamesResolved = noteNames ?: generateNoteNames(temperament.numberOfNotesPerOctave)!!
////            Log.v("Tuner", "MusicalScale2Factory: numberOfNotesPerOctave=${temperament.numberOfNotesPerOctave}, noteNames size=${noteNamesResolved.size}")
//            assert(temperament.numberOfNotesPerOctave == noteNamesResolved.size)
//            val rootNoteResolved = rootNote ?: noteNamesResolved[0]
//            val referenceNoteResolved = referenceNote ?: noteNamesResolved.defaultReferenceNote
////            Log.v("Tuner", "MusicalScale2Factory: $temperament")
//            return MusicalScale2(
//                temperament,
//                noteNamesResolved,
//                rootNoteResolved,
//                referenceNoteResolved,
//                referenceFrequency,
//                frequencyMin,
//                frequencyMax,
//                stretchTuning
//            )
//        }
//
//        /** Create simple test temperament. */
//        fun createTestEdo12(referenceFrequency: Float = 440f): MusicalScale2 {
//            return create(
//                createTestTemperamentEdo12(),
//                null,
//                null,
//                null,
//                referenceFrequency,
//                16f,
//                16000f,
//                StretchTuning()
//            )
//        }
//
//        fun createTestWerckmeisterVI(referenceFrequency: Float = 440f): MusicalScale2 {
//            return create(
//                createTestTemperamentWerckmeisterVI(),
//                null,
//                null,
//                null,
//                referenceFrequency,
//                16f,
//                16000f,
//                StretchTuning()
//            )
//        }
//    }
//}