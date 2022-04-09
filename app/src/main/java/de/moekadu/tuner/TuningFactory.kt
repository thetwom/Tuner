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
                    R.string.equal_temperament_12,
                    R.string.equal_temperament_12_desc,
                    12,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Pythagorean -> TuningRatioBased(
                    tuning,
                    circleOfFifthsPythagorean,
                    R.string.pythagorean_tuning,
                    null,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Pure -> TuningRatioBased(
                    tuning,
                    rationalNumberTuningPure,
                    R.string.pure_tuning,
                    R.string.pure_tuning_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.QuarterCommaMeanTone -> TuningRatioBased(
                    tuning,
                    circleOfFifthsQuarterCommaMeanTone,
                    R.string.quarter_comma_mean_tone,
                    R.string.quarter_comma_mean_tone_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.ThirdCommaMeanTone -> TuningRatioBased(
                    tuning,
                    circleOfFifthsThirdCommaMeanTone,
                    R.string.third_comma_mean_tone,
                    R.string.third_comma_mean_tone_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.WerckmeisterIII -> TuningRatioBased(
                    tuning,
                    circleOfFifthsWerckmeisterIII,
                    R.string.werckmeister_iii,
                    R.string.werckmeister_iii_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.WerckmeisterIV -> TuningRatioBased(
                    tuning,
                    circleOfFifthsWerckmeisterIV,
                    R.string.werckmeister_iv,
                    R.string.werckmeister_iv_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.WerckmeisterV -> TuningRatioBased(
                    tuning,
                    circleOfFifthsWerckmeisterV,
                    R.string.werckmeister_v,
                    R.string.werckmeister_v_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.WerckmeisterVI -> TuningRatioBased(
                    tuning,
                    rationalNumberTuningWerckmeisterVI,
                    R.string.werckmeister_vi,
                    R.string.werckmeister_vi_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Kirnberger1 -> TuningRatioBased(
                    tuning,
                    circleOfFifthsKirnberger1,
                    R.string.kirnberger1,
                    R.string.kirnberger1_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Kirnberger2 -> TuningRatioBased(
                    tuning,
                    circleOfFifthsKirnberger2,
                    R.string.kirnberger2,
                    R.string.kirnberger2_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Kirnberger3 -> TuningRatioBased(
                    tuning,
                    circleOfFifthsKirnberger3,
                    R.string.kirnberger3,
                    R.string.kirnberger3_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Neidhardt1 -> TuningRatioBased(
                    tuning,
                    circleOfFifthsNeidhardt1,
                    R.string.neidhardt1,
                    R.string.neidhardt1_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Neidhardt2 -> TuningRatioBased(
                    tuning,
                    circleOfFifthsNeidhardt2,
                    R.string.neidhardt2,
                    R.string.neidhardt2_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Neidhardt3 -> TuningRatioBased(
                    tuning,
                    circleOfFifthsNeidhardt3,
                    R.string.neidhardt3,
                    R.string.neidhardt3_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
//                Tuning.Neidhardt4 -> TuningRatioBased(
//                    tuning,
//                    circleOfFifthsNeidthardt4,
//                    null,
//                    null,
//                    rootNoteIndex,
//                    noteIndexAtReferenceFrequency,
//                    referenceFrequency
//                )
                Tuning.Valotti -> TuningRatioBased(
                    tuning,
                    circleOfFifthsValotti,
                    R.string.valotti,
                    null,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Young2 -> TuningRatioBased(
                    tuning,
                    circleOfFifthsYoung2,
                    R.string.young2,
                    null,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
            }
        }
    }
}