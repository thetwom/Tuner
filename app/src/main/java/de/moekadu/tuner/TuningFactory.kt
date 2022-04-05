package de.moekadu.tuner

class TuningFactory {
    companion object {
        enum class Tuning {
            EDO12,
            Pythagorean,
            Pure,
            QuarterCommaMeanTone,
            ThirdCommaMeanTone,
            WerckmeisterIII,
            WerckmeisterIV,
            WerckmeisterV,
            WerckmeisterVI,
            Kirnberger1,
            Kirnberger2,
            Kirnberger3,
            Neidhardt1,
            Neidhardt2,
            Neidhardt3,
            // Neidhardt4,
            Valotti,
            Young2
        }

        fun create(
            tuning: Tuning,
            rootNoteIndex: Int,
            noteIndexAtReferenceFrequency: Int,
            referenceFrequency: Float
        ): TuningFrequencies {
            return when (tuning) {
                Tuning.EDO12 -> TuningEqualTemperament(
                    R.string.equal_temperament_12,
                    R.string.equal_temperament_12_desc,
                    12,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Pythagorean -> TuningRatioBased(
                    circleOfFifthsPythagorean,
                    R.string.pythagorean_tuning,
                    null,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Pure -> TuningRatioBased(
                    rationalNumberTuningPure,
                    R.string.pure_tuning,
                    R.string.pure_tuning_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.QuarterCommaMeanTone -> TuningRatioBased(
                    circleOfFifthsQuarterCommaMeanTone,
                    R.string.quarter_comma_mean_tone,
                    R.string.quarter_comma_mean_tone_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.ThirdCommaMeanTone -> TuningRatioBased(
                    circleOfFifthsThirdCommaMeanTone,
                    R.string.third_comma_mean_tone,
                    R.string.third_comma_mean_tone_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.WerckmeisterIII -> TuningRatioBased(
                    circleOfFifthsWerckmeisterIII,
                    R.string.werckmeister_iii,
                    R.string.werckmeister_iii_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.WerckmeisterIV -> TuningRatioBased(
                    circleOfFifthsWerckmeisterIV,
                    R.string.werckmeister_iv,
                    R.string.werckmeister_iv_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.WerckmeisterV -> TuningRatioBased(
                    circleOfFifthsWerckmeisterV,
                    R.string.werckmeister_v,
                    R.string.werckmeister_v_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.WerckmeisterVI -> TuningRatioBased(
                    rationalNumberTuningWerckmeisterVI,
                    R.string.werckmeister_vi,
                    R.string.werckmeister_vi_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Kirnberger1 -> TuningRatioBased(
                    circleOfFifthsKirnberger1,
                    R.string.kirnberger1,
                    R.string.kirnberger1_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Kirnberger2 -> TuningRatioBased(
                    circleOfFifthsKirnberger2,
                    R.string.kirnberger2,
                    R.string.kirnberger2_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Kirnberger3 -> TuningRatioBased(
                    circleOfFifthsKirnberger3,
                    R.string.kirnberger3,
                    R.string.kirnberger3_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Neidhardt1 -> TuningRatioBased(
                    circleOfFifthsNeidhardt1,
                    R.string.neidhardt1,
                    R.string.neidhardt1_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Neidhardt2 -> TuningRatioBased(
                    circleOfFifthsNeidhardt2,
                    R.string.neidhardt2,
                    R.string.neidhardt2_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Neidhardt3 -> TuningRatioBased(
                    circleOfFifthsNeidhardt3,
                    R.string.neidhardt3,
                    R.string.neidhardt3_desc,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
//                Tuning.Neidhardt4 -> TuningRatioBased(
//                    circleOfFifthsNeidthardt4,
//                    null,
//                    null,
//                    rootNoteIndex,
//                    noteIndexAtReferenceFrequency,
//                    referenceFrequency
//                )
                Tuning.Valotti -> TuningRatioBased(
                    circleOfFifthsValotti,
                    R.string.valotti,
                    null,
                    rootNoteIndex,
                    noteIndexAtReferenceFrequency,
                    referenceFrequency
                )
                Tuning.Young2 -> TuningRatioBased(
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