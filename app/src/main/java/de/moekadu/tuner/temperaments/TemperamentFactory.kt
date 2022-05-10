package de.moekadu.tuner.temperaments

class TemperamentFactory {
    companion object {
        fun create(
            temperament: Temperament,
            rootNoteIndex: Int,
            noteIndexAtReferenceFrequency: Int,
            referenceFrequency: Float
        ): TemperamentFrequencies {
            return when (temperament) {
                Temperament.EDO12 -> TemperamentEqualTemperament(
                    temperament,
                    12,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Temperament.Pythagorean -> TemperamentRatioBased(
                    temperament,
                    circleOfFifthsPythagorean,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Temperament.Pure -> TemperamentRatioBased(
                    temperament,
                    rationalNumberTemperamentPure,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Temperament.QuarterCommaMeanTone -> TemperamentRatioBased(
                    temperament,
                    circleOfFifthsQuarterCommaMeanTone,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Temperament.ThirdCommaMeanTone -> TemperamentRatioBased(
                    temperament,
                    circleOfFifthsThirdCommaMeanTone,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Temperament.WerckmeisterIII -> TemperamentRatioBased(
                    temperament,
                    circleOfFifthsWerckmeisterIII,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Temperament.WerckmeisterIV -> TemperamentRatioBased(
                    temperament,
                    circleOfFifthsWerckmeisterIV,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Temperament.WerckmeisterV -> TemperamentRatioBased(
                    temperament,
                    circleOfFifthsWerckmeisterV,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Temperament.WerckmeisterVI -> TemperamentRatioBased(
                    temperament,
                    rationalNumberTemperamentWerckmeisterVI,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Temperament.Kirnberger1 -> TemperamentRatioBased(
                    temperament,
                    circleOfFifthsKirnberger1,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Temperament.Kirnberger2 -> TemperamentRatioBased(
                    temperament,
                    circleOfFifthsKirnberger2,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Temperament.Kirnberger3 -> TemperamentRatioBased(
                    temperament,
                    circleOfFifthsKirnberger3,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Temperament.Neidhardt1 -> TemperamentRatioBased(
                    temperament,
                    circleOfFifthsNeidhardt1,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Temperament.Neidhardt2 -> TemperamentRatioBased(
                    temperament,
                    circleOfFifthsNeidhardt2,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Temperament.Neidhardt3 -> TemperamentRatioBased(
                    temperament,
                    circleOfFifthsNeidhardt3,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
//                Tuning.Neidhardt4 -> TuningRatioBased(
//                    tuning,
//                    circleOfFifthsNeidhardt4,
//                    rootNoteIndex,
//                    noteIndexAtReferenceFrequency,
//                    referenceFrequency
//                )
                Temperament.Valotti -> TemperamentRatioBased(
                    temperament,
                    circleOfFifthsValotti,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Temperament.Young2 -> TemperamentRatioBased(
                    temperament,
                    circleOfFifthsYoung2,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Temperament.Test -> TemperamentRatioBased(
                    temperament,
                    rationalNumberTemperamentTest,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
            }
        }
    }
}