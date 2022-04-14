package de.moekadu.tuner

class TuningFactory {
    companion object {
        fun create(
            tuning: Tuning,
            rootNoteIndex: Int,
            noteIndexAtReferenceFrequency: Int,
            referenceFrequency: Float
        ): TuningFrequencies {
            return when (tuning) {
                Tuning.EDO12 -> TuningEqualTemperament(
                    tuning,
                    12,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Pythagorean -> TuningRatioBased(
                    tuning,
                    circleOfFifthsPythagorean,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Pure -> TuningRatioBased(
                    tuning,
                    rationalNumberTuningPure,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.QuarterCommaMeanTone -> TuningRatioBased(
                    tuning,
                    circleOfFifthsQuarterCommaMeanTone,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.ThirdCommaMeanTone -> TuningRatioBased(
                    tuning,
                    circleOfFifthsThirdCommaMeanTone,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.WerckmeisterIII -> TuningRatioBased(
                    tuning,
                    circleOfFifthsWerckmeisterIII,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.WerckmeisterIV -> TuningRatioBased(
                    tuning,
                    circleOfFifthsWerckmeisterIV,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.WerckmeisterV -> TuningRatioBased(
                    tuning,
                    circleOfFifthsWerckmeisterV,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.WerckmeisterVI -> TuningRatioBased(
                    tuning,
                    rationalNumberTuningWerckmeisterVI,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Kirnberger1 -> TuningRatioBased(
                    tuning,
                    circleOfFifthsKirnberger1,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Kirnberger2 -> TuningRatioBased(
                    tuning,
                    circleOfFifthsKirnberger2,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Kirnberger3 -> TuningRatioBased(
                    tuning,
                    circleOfFifthsKirnberger3,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Neidhardt1 -> TuningRatioBased(
                    tuning,
                    circleOfFifthsNeidhardt1,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Neidhardt2 -> TuningRatioBased(
                    tuning,
                    circleOfFifthsNeidhardt2,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Neidhardt3 -> TuningRatioBased(
                    tuning,
                    circleOfFifthsNeidhardt3,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
//                Tuning.Neidhardt4 -> TuningRatioBased(
//                    tuning,
//                    circleOfFifthsNeidthardt4,
//                    rootNoteIndex,
//                    noteIndexAtReferenceFrequency,
//                    referenceFrequency
//                )
                Tuning.Valotti -> TuningRatioBased(
                    tuning,
                    circleOfFifthsValotti,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Young2 -> TuningRatioBased(
                    tuning,
                    circleOfFifthsYoung2,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
//                Tuning.Test -> TuningRatioBased(
//                    tuning,
//                    rationalNumberTuningTest,
//                    rootNoteIndex,
//                    noteIndexAtReferenceFrequency,
//                    referenceFrequency
//                )
            }
        }
    }
}