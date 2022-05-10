package de.moekadu.tuner.temperaments

import de.moekadu.tuner.R

enum class Temperament {
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

fun getTuningNameResourceId(temperament: Temperament) = when(temperament) {
    Temperament.EDO12 -> R.string.equal_temperament_12
    Temperament.Pythagorean -> R.string.pythagorean_tuning
    Temperament.Pure -> R.string.pure_tuning
    Temperament.QuarterCommaMeanTone -> R.string.quarter_comma_mean_tone
    Temperament.ThirdCommaMeanTone -> R.string.third_comma_mean_tone
    Temperament.WerckmeisterIII -> R.string.werckmeister_iii
    Temperament.WerckmeisterIV -> R.string.werckmeister_iv
    Temperament.WerckmeisterV -> R.string.werckmeister_v
    Temperament.WerckmeisterVI -> R.string.werckmeister_vi
    Temperament.Kirnberger1 -> R.string.kirnberger1
    Temperament.Kirnberger2 -> R.string.kirnberger2
    Temperament.Kirnberger3 -> R.string.kirnberger3
    Temperament.Neidhardt1 -> R.string.neidhardt1
    Temperament.Neidhardt2 -> R.string.neidhardt2
    Temperament.Neidhardt3 -> R.string.neidhardt3
    Temperament.Valotti -> R.string.valotti
    Temperament.Young2 -> R.string.young2
     Temperament.Test -> R.string.test_tuning
}

fun getTuningDescriptionResourceId(temperament: Temperament) = when(temperament) {
    Temperament.EDO12 -> R.string.equal_temperament_12_desc
    Temperament.Pythagorean -> null
    Temperament.Pure -> R.string.pure_tuning_desc
    Temperament.QuarterCommaMeanTone -> R.string.quarter_comma_mean_tone_desc
    Temperament.ThirdCommaMeanTone -> R.string.third_comma_mean_tone_desc
    Temperament.WerckmeisterIII -> R.string.werckmeister_iii_desc
    Temperament.WerckmeisterIV -> R.string.werckmeister_iv_desc
    Temperament.WerckmeisterV -> R.string.werckmeister_v_desc
    Temperament.WerckmeisterVI -> R.string.werckmeister_vi_desc
    Temperament.Kirnberger1 -> R.string.kirnberger1_desc
    Temperament.Kirnberger2 -> R.string.kirnberger2_desc
    Temperament.Kirnberger3 -> R.string.kirnberger3_desc
    Temperament.Neidhardt1 -> R.string.neidhardt1_desc
    Temperament.Neidhardt2 -> R.string.neidhardt2_desc
    Temperament.Neidhardt3 -> R.string.neidhardt3_desc
    Temperament.Valotti -> null
    Temperament.Young2 -> null
    Temperament.Test -> null
}

