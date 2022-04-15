package de.moekadu.tuner

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
    Young2,
    Test
}

fun getTuningNameResourceId(tuning: Tuning) = when(tuning) {
    Tuning.EDO12 -> R.string.equal_temperament_12
    Tuning.Pythagorean -> R.string.pythagorean_tuning
    Tuning.Pure -> R.string.pure_tuning
    Tuning.QuarterCommaMeanTone -> R.string.quarter_comma_mean_tone
    Tuning.ThirdCommaMeanTone -> R.string.third_comma_mean_tone
    Tuning.WerckmeisterIII -> R.string.werckmeister_iii
    Tuning.WerckmeisterIV -> R.string.werckmeister_iv
    Tuning.WerckmeisterV -> R.string.werckmeister_v
    Tuning.WerckmeisterVI -> R.string.werckmeister_vi
    Tuning.Kirnberger1 -> R.string.kirnberger1
    Tuning.Kirnberger2 -> R.string.kirnberger2
    Tuning.Kirnberger3 -> R.string.kirnberger3
    Tuning.Neidhardt1 -> R.string.neidhardt1
    Tuning.Neidhardt2 -> R.string.neidhardt2
    Tuning.Neidhardt3 -> R.string.neidhardt3
    Tuning.Valotti -> R.string.valotti
    Tuning.Young2 -> R.string.young2
     Tuning.Test -> R.string.test_tuning
}

fun getTuningDescriptionResourceId(tuning: Tuning) = when(tuning) {
    Tuning.EDO12 -> R.string.equal_temperament_12_desc
    Tuning.Pythagorean -> null
    Tuning.Pure -> R.string.pure_tuning_desc
    Tuning.QuarterCommaMeanTone -> R.string.quarter_comma_mean_tone_desc
    Tuning.ThirdCommaMeanTone -> R.string.third_comma_mean_tone_desc
    Tuning.WerckmeisterIII -> R.string.werckmeister_iii_desc
    Tuning.WerckmeisterIV -> R.string.werckmeister_iv_desc
    Tuning.WerckmeisterV -> R.string.werckmeister_v_desc
    Tuning.WerckmeisterVI -> R.string.werckmeister_vi_desc
    Tuning.Kirnberger1 -> R.string.kirnberger1_desc
    Tuning.Kirnberger2 -> R.string.kirnberger2_desc
    Tuning.Kirnberger3 -> R.string.kirnberger3_desc
    Tuning.Neidhardt1 -> R.string.neidhardt1_desc
    Tuning.Neidhardt2 -> R.string.neidhardt2_desc
    Tuning.Neidhardt3 -> R.string.neidhardt3_desc
    Tuning.Valotti -> null
    Tuning.Young2 -> null
    Tuning.Test -> null
}

