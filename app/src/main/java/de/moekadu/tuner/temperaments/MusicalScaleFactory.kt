package de.moekadu.tuner.temperaments

class MusicalScaleFactory {
    companion object {

        fun create(
            temperamentType: TemperamentType,
            referenceNote: MusicalNote?,
            rootNote: MusicalNote?,
            referenceFrequency: Float,
            preferFlat: Boolean,
            frequencyMin: Float = 16.0f,
            frequencyMax: Float = 17000.0f
        ): MusicalScale {
            val noteNameScale = NoteNameScaleFactory.create(temperamentType, referenceNote, preferFlat)
            val rootNoteResolved = rootNote ?: noteNameScale.notes[0]
            return when (temperamentType) {
                TemperamentType.EDO12 -> MusicalScaleEqualTemperament(
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
                TemperamentType.Pythagorean -> MusicalScaleRatioBasedTemperaments(
                    temperamentType,
                    circleOfFifthsPythagorean,
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
                TemperamentType.Pure -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, rationalNumberTemperamentPure,
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
                TemperamentType.QuarterCommaMeanTone -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsQuarterCommaMeanTone,
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
                TemperamentType.ThirdCommaMeanTone -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsThirdCommaMeanTone,
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
                TemperamentType.WerckmeisterIII -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsWerckmeisterIII,
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
                TemperamentType.WerckmeisterIV -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsWerckmeisterIV,
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
                TemperamentType.WerckmeisterV -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsWerckmeisterV,
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
                TemperamentType.WerckmeisterVI -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, rationalNumberTemperamentWerckmeisterVI,
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
                TemperamentType.Kirnberger1 -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsKirnberger1,
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
                TemperamentType.Kirnberger2 -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsKirnberger2,
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
                TemperamentType.Kirnberger3 -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsKirnberger3,
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
                TemperamentType.Neidhardt1 -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsNeidhardt1,
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
                TemperamentType.Neidhardt2 -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsNeidhardt2,
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
                TemperamentType.Neidhardt3 -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsNeidhardt3,
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
//                Tuning.Neidhardt4 -> TuningRatioBased(
//                    tuning, circleOfFifthsNeidhardt4,
//                    noteNameScale, rootNote, referenceFrequency, frequencyMin, frequencyMax
//                )
                TemperamentType.Valotti -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsValotti,
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
                TemperamentType.Young2 -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, circleOfFifthsYoung2,
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
                TemperamentType.Test -> MusicalScaleRatioBasedTemperaments(
                    temperamentType, rationalNumberTemperamentTest,
                    noteNameScale, rootNoteResolved, referenceFrequency, frequencyMin, frequencyMax
                )
            }
        }
    }
}