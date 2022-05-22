package de.moekadu.tuner.temperaments

class TemperamentFactory {
    companion object {
        fun create(
            temperamentType: TemperamentType,
            rootNoteIndex: Int,
            noteIndexAtReferenceFrequency: Int,
            referenceFrequency: Float
        ): MusicalScale {
            return when (temperamentType) {
                TemperamentType.EDO12 -> TemperamentEqualTemperament(
                    temperamentType,
                    12,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                TemperamentType.Pythagorean -> TemperamentRatioBased(
                    temperamentType,
                    circleOfFifthsPythagorean,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                TemperamentType.Pure -> TemperamentRatioBased(
                    temperamentType,
                    rationalNumberTemperamentPure,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                TemperamentType.QuarterCommaMeanTone -> TemperamentRatioBased(
                    temperamentType,
                    circleOfFifthsQuarterCommaMeanTone,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                TemperamentType.ThirdCommaMeanTone -> TemperamentRatioBased(
                    temperamentType,
                    circleOfFifthsThirdCommaMeanTone,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                TemperamentType.WerckmeisterIII -> TemperamentRatioBased(
                    temperamentType,
                    circleOfFifthsWerckmeisterIII,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                TemperamentType.WerckmeisterIV -> TemperamentRatioBased(
                    temperamentType,
                    circleOfFifthsWerckmeisterIV,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                TemperamentType.WerckmeisterV -> TemperamentRatioBased(
                    temperamentType,
                    circleOfFifthsWerckmeisterV,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                TemperamentType.WerckmeisterVI -> TemperamentRatioBased(
                    temperamentType,
                    rationalNumberTemperamentWerckmeisterVI,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                TemperamentType.Kirnberger1 -> TemperamentRatioBased(
                    temperamentType,
                    circleOfFifthsKirnberger1,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                TemperamentType.Kirnberger2 -> TemperamentRatioBased(
                    temperamentType,
                    circleOfFifthsKirnberger2,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                TemperamentType.Kirnberger3 -> TemperamentRatioBased(
                    temperamentType,
                    circleOfFifthsKirnberger3,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                TemperamentType.Neidhardt1 -> TemperamentRatioBased(
                    temperamentType,
                    circleOfFifthsNeidhardt1,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                TemperamentType.Neidhardt2 -> TemperamentRatioBased(
                    temperamentType,
                    circleOfFifthsNeidhardt2,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                TemperamentType.Neidhardt3 -> TemperamentRatioBased(
                    temperamentType,
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
                TemperamentType.Valotti -> TemperamentRatioBased(
                    temperamentType,
                    circleOfFifthsValotti,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                TemperamentType.Young2 -> TemperamentRatioBased(
                    temperamentType,
                    circleOfFifthsYoung2,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                TemperamentType.Test -> TemperamentRatioBased(
                    temperamentType,
                    rationalNumberTemperamentTest,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
            }
        }
    }
}