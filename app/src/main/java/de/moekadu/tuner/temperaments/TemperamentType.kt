package de.moekadu.tuner.temperaments

import de.moekadu.tuner.R

enum class TemperamentType {
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
    EDO17,
    EDO19,
    EDO22,
    EDO24,
    EDO27,
    EDO29,
    EDO31,
    EDO41,
    EDO53,
    Test
}

fun getTuningNameResourceId(temperamentType: TemperamentType) = when(temperamentType) {
    TemperamentType.EDO12 -> R.string.equal_temperament_12
    TemperamentType.Pythagorean -> R.string.pythagorean_tuning
    TemperamentType.Pure -> R.string.pure_tuning
    TemperamentType.QuarterCommaMeanTone -> R.string.quarter_comma_mean_tone
    TemperamentType.ThirdCommaMeanTone -> R.string.third_comma_mean_tone
    TemperamentType.WerckmeisterIII -> R.string.werckmeister_iii
    TemperamentType.WerckmeisterIV -> R.string.werckmeister_iv
    TemperamentType.WerckmeisterV -> R.string.werckmeister_v
    TemperamentType.WerckmeisterVI -> R.string.werckmeister_vi
    TemperamentType.Kirnberger1 -> R.string.kirnberger1
    TemperamentType.Kirnberger2 -> R.string.kirnberger2
    TemperamentType.Kirnberger3 -> R.string.kirnberger3
    TemperamentType.Neidhardt1 -> R.string.neidhardt1
    TemperamentType.Neidhardt2 -> R.string.neidhardt2
    TemperamentType.Neidhardt3 -> R.string.neidhardt3
    TemperamentType.Valotti -> R.string.valotti
    TemperamentType.Young2 -> R.string.young2
    TemperamentType.EDO17 -> R.string.equal_temperament_17
    TemperamentType.EDO19 -> R.string.equal_temperament_19
    TemperamentType.EDO22 -> R.string.equal_temperament_22
    TemperamentType.EDO24 -> R.string.equal_temperament_24
    TemperamentType.EDO27 -> R.string.equal_temperament_27
    TemperamentType.EDO29 -> R.string.equal_temperament_29
    TemperamentType.EDO31 -> R.string.equal_temperament_31
    TemperamentType.EDO41 -> R.string.equal_temperament_41
    TemperamentType.EDO53 -> R.string.equal_temperament_53
    TemperamentType.Test -> R.string.test_tuning
}

fun getTuningDescriptionResourceId(temperamentType: TemperamentType) = when(temperamentType) {
    TemperamentType.EDO12 -> R.string.equal_temperament_12_desc
    TemperamentType.Pythagorean -> null
    TemperamentType.Pure -> R.string.pure_tuning_desc
    TemperamentType.QuarterCommaMeanTone -> R.string.quarter_comma_mean_tone_desc
    TemperamentType.ThirdCommaMeanTone -> R.string.third_comma_mean_tone_desc
    TemperamentType.WerckmeisterIII -> R.string.werckmeister_iii_desc
    TemperamentType.WerckmeisterIV -> R.string.werckmeister_iv_desc
    TemperamentType.WerckmeisterV -> R.string.werckmeister_v_desc
    TemperamentType.WerckmeisterVI -> R.string.werckmeister_vi_desc
    TemperamentType.Kirnberger1 -> R.string.kirnberger1_desc
    TemperamentType.Kirnberger2 -> R.string.kirnberger2_desc
    TemperamentType.Kirnberger3 -> R.string.kirnberger3_desc
    TemperamentType.Neidhardt1 -> R.string.neidhardt1_desc
    TemperamentType.Neidhardt2 -> R.string.neidhardt2_desc
    TemperamentType.Neidhardt3 -> R.string.neidhardt3_desc
    TemperamentType.Valotti -> null
    TemperamentType.Young2 -> null
    TemperamentType.EDO17 -> R.string.equal_temperament_17_desc
    TemperamentType.EDO19 -> R.string.equal_temperament_19_desc
    TemperamentType.EDO22 -> R.string.equal_temperament_22_desc
    TemperamentType.EDO24 -> R.string.equal_temperament_24_desc
    TemperamentType.EDO27 -> R.string.equal_temperament_27_desc
    TemperamentType.EDO29 -> R.string.equal_temperament_29_desc
    TemperamentType.EDO31 -> R.string.equal_temperament_31_desc
    TemperamentType.EDO41 -> R.string.equal_temperament_41_desc
    TemperamentType.EDO53 -> R.string.equal_temperament_53_desc
    TemperamentType.Test -> null
}

