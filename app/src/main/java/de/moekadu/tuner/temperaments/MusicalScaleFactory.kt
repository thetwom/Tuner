package de.moekadu.tuner.temperaments

class MusicalScaleFactory {
    companion object {

        fun create(
            temperamentType: TemperamentType,
            noteNameScale: NoteNameScale,
            referenceNote: MusicalNote? = null,
            rootNote: MusicalNote? = null,
            referenceFrequency: Float = 440f,
            frequencyMin: Float = 16.0f,
            frequencyMax: Float = 17000.0f
        ): MusicalScale {
            val rootNoteResolved = rootNote ?: noteNameScale.notes[0]
            val referenceNoteResolved = referenceNote ?: noteNameScale.defaultReferenceNote
            return when (temperamentType) {
                TemperamentType.EDO12 -> MusicalScaleEqualTemperament(
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
                TemperamentType.Pythagorean -> MusicalScaleRatioBasedTemperaments(
                    temperamentType,
                    circleOfFifthsPythagorean,
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
                TemperamentType.Pure -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, rationalNumberTemperamentPure,
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
                TemperamentType.QuarterCommaMeanTone -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsQuarterCommaMeanTone,
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
                TemperamentType.ThirdCommaMeanTone -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsThirdCommaMeanTone,
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
                TemperamentType.WerckmeisterIII -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsWerckmeisterIII,
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
                TemperamentType.WerckmeisterIV -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsWerckmeisterIV,
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
                TemperamentType.WerckmeisterV -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsWerckmeisterV,
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
                TemperamentType.WerckmeisterVI -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, rationalNumberTemperamentWerckmeisterVI,
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
                TemperamentType.Kirnberger1 -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsKirnberger1,
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
                TemperamentType.Kirnberger2 -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsKirnberger2,
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
                TemperamentType.Kirnberger3 -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsKirnberger3,
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
                TemperamentType.Neidhardt1 -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsNeidhardt1,
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
                TemperamentType.Neidhardt2 -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsNeidhardt2,
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
                TemperamentType.Neidhardt3 -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsNeidhardt3,
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
//                Tuning.Neidhardt4 -> TuningRatioBased(
//                    tuning, circleOfFifthsNeidhardt4,
//                   noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
//                )
                TemperamentType.Valotti -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsValotti,
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
                TemperamentType.Young2 -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsYoung2,
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
                TemperamentType.EDO19 -> MusicalScaleEqualTemperament(
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
                TemperamentType.Test -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, rationalNumberTemperamentTest,
                    noteNameScale, referenceNoteResolved, referenceFrequency, rootNoteResolved, frequencyMin, frequencyMax
                )
            }
        }
        fun create(
            temperamentType: TemperamentType,
            referenceNote: MusicalNote? = null,
            rootNote: MusicalNote? = null,
            referenceFrequency: Float = 440f,
            frequencyMin: Float = 16.0f,
            frequencyMax: Float = 17000.0f
        ): MusicalScale {
            val noteNameScale = NoteNameScaleFactory.create(temperamentType)
            return create(temperamentType, noteNameScale, referenceNote, rootNote,
                referenceFrequency, frequencyMin, frequencyMax)
        }
    }
}