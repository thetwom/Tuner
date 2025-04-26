package de.moekadu.tuner.temperaments

import de.moekadu.tuner.R
import de.moekadu.tuner.misc.GetTextFromResId
import de.moekadu.tuner.misc.GetTextFromString

fun predefinedTemperaments(): ArrayList<Temperament3> {
    val temperaments = ArrayList<Temperament3>()

    temperaments.add(
        predefinedTemperamentsEDO(12, (-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentPythagorean((-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentPure((-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentQuarterCommaMeanTone((-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentExtendedQuarterCommaMeanTone((-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentThirdCommaMeanTone((-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentFifthCommaMeanTone((-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentWerckmeisterIII((-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentWerckmeisterIV((-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentWerckmeisterV((-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentWerckmeisterVI((-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentKirnberger1((-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentKirnberger2((-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentKirnberger3((-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentNeidhardt1((-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentNeidhardt2((-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentNeidhardt3((-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentValotti((-temperaments.size - 1).toLong())
    )
    temperaments.add(
        predefinedTemperamentYoung2((-temperaments.size - 1).toLong())
    )

    return temperaments
}

fun predefinedTemperamentsEDO(notesPerOctave: Int, stableId: Long)
        = Temperament3EDO(
    stableId = stableId,
    notesPerOctave = notesPerOctave
)

fun predefinedTemperamentPythagorean(stableId: Long) = Temperament3ChainOfFifthsNoEnharmonics(
    name = GetTextFromResId(R.string.pythagorean_tuning),
    abbreviation = GetTextFromResId(R.string.pythagorean_tuning_abbr),
    description = GetTextFromString(""),
    stableId = stableId,
    fifths = arrayOf(
        FifthModification(), // Db - Ab
        FifthModification(), // Ab - Eb
        FifthModification(), // Eb - Bb
        FifthModification(), // Bb - F
        FifthModification(), // F  - C
        FifthModification(), // C  - G <- root
        FifthModification(), // G  - D
        FifthModification(), // D  - A
        FifthModification(), // A  - E
        FifthModification(), // E  - B
        FifthModification(), // B  - F#
    ),
    rootIndex = 5
)

fun predefinedTemperamentPure(stableId: Long) = Temperament3RationalNumbersEDONames(
    name = GetTextFromResId(R.string.pure_tuning),
    abbreviation = GetTextFromResId(R.string.pure_tuning_abbr),
    description = GetTextFromResId(R.string.pure_tuning_desc),
    rationalNumbers = arrayOf(
        RationalNumber(1, 1), // C
        RationalNumber(16, 15), // C#
        RationalNumber(9, 8), // D
        RationalNumber(6, 5), // Eb
        RationalNumber(5, 4), // E
        RationalNumber(4, 3), // F
        RationalNumber(45, 32), // F#
        RationalNumber(3, 2), // G
        RationalNumber(8, 5), // G#
        RationalNumber(5, 3), // A
        RationalNumber(9, 5), // Bb (sometimes 16.0/9.0)
        RationalNumber(15, 8), // B
        RationalNumber(2, 1) // C2
    ),
    stableId = stableId
)

fun predefinedTemperamentQuarterCommaMeanTone(stableId: Long)
        = Temperament3ChainOfFifthsNoEnharmonics(
    name = GetTextFromResId(R.string.quarter_comma_mean_tone),
    abbreviation = GetTextFromResId(R.string.quarter_comma_mean_tone_abbr),
    description = GetTextFromResId(R.string.quarter_comma_mean_tone_desc),
    stableId = stableId,
    fifths = arrayOf(
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // Eb - Bb
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // Bb - F
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // F  - C
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // C  - G <- root
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // G  - D
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // D  - A
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // A  - E
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // E  - B
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // B  - F#
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // F# - C#
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // C# - G#
    ),
    rootIndex = 3
)

fun predefinedTemperamentExtendedQuarterCommaMeanTone(stableId: Long)
        = Temperament3ChainOfFifthsNoEnharmonics(
    name = GetTextFromResId(R.string.extended_quarter_comma_mean_tone),
    abbreviation = GetTextFromResId(R.string.extended_quarter_comma_mean_tone_abbr),
    description = GetTextFromResId(R.string.extended_quarter_comma_mean_tone_desc),
    stableId = stableId,
    fifths = arrayOf(
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // Ab - Eb
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // Eb - Bb
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // Bb - F
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // F  - C
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // C  - G <- root
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // G  - D
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // D  - A
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // A  - E
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // E  - B
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // B  - F#
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // F# - C#
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // C# - G#
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // G# - D#
        FifthModification(syntonicComma = RationalNumber(-1, 4)), // D# - A#
    ),
    rootIndex = 4
)

fun predefinedTemperamentThirdCommaMeanTone(stableId: Long)
        = Temperament3ChainOfFifthsNoEnharmonics(
    name = GetTextFromResId(R.string.third_comma_mean_tone),
    abbreviation = GetTextFromResId(R.string.third_comma_mean_tone_abbr),
    description = GetTextFromResId(R.string.third_comma_mean_tone_desc),
    stableId = stableId,
    fifths = arrayOf(
        FifthModification(syntonicComma = RationalNumber(-1, 3)), // Eb - Bb
        FifthModification(syntonicComma = RationalNumber(-1, 3)), // Bb - F
        FifthModification(syntonicComma = RationalNumber(-1, 3)), // F  - C
        FifthModification(syntonicComma = RationalNumber(-1, 3)), // C  - G <- root
        FifthModification(syntonicComma = RationalNumber(-1, 3)), // G  - D
        FifthModification(syntonicComma = RationalNumber(-1, 3)), // D  - A
        FifthModification(syntonicComma = RationalNumber(-1, 3)), // A  - E
        FifthModification(syntonicComma = RationalNumber(-1, 3)), // E  - B
        FifthModification(syntonicComma = RationalNumber(-1, 3)), // B  - F#
        FifthModification(syntonicComma = RationalNumber(-1, 3)), // F# - C#
        FifthModification(syntonicComma = RationalNumber(-1, 3)), // C# - G#
    ),
    rootIndex = 3
)


fun predefinedTemperamentFifthCommaMeanTone(stableId: Long)
        = Temperament3ChainOfFifthsNoEnharmonics(
    name = GetTextFromResId(R.string.fifth_comma_mean_tone),
    abbreviation = GetTextFromResId(R.string.fifth_comma_mean_tone_abbr),
    description = GetTextFromResId(R.string.fifth_comma_mean_tone_desc),
    stableId = stableId,
    fifths = arrayOf(
        FifthModification(syntonicComma = RationalNumber(-1, 5)), // Eb - Bb
        FifthModification(syntonicComma = RationalNumber(-1, 5)), // Bb - F
        FifthModification(syntonicComma = RationalNumber(-1, 5)), // F  - C
        FifthModification(syntonicComma = RationalNumber(-1, 5)), // C  - G <- root
        FifthModification(syntonicComma = RationalNumber(-1, 5)), // G  - D
        FifthModification(syntonicComma = RationalNumber(-1, 5)), // D  - A
        FifthModification(syntonicComma = RationalNumber(-1, 5)), // A  - E
        FifthModification(syntonicComma = RationalNumber(-1, 5)), // E  - B
        FifthModification(syntonicComma = RationalNumber(-1, 5)), // B  - F#
        FifthModification(syntonicComma = RationalNumber(-1, 5)), // F# - C#
        FifthModification(syntonicComma = RationalNumber(-1, 5)), // C# - G#
    ),
    rootIndex = 3
)

fun predefinedTemperamentWerckmeisterIII(stableId: Long)
        = Temperament3ChainOfFifthsEDONames(
    name = GetTextFromResId(R.string.werckmeister_iii),
    abbreviation = GetTextFromResId(R.string.werckmeister_iii_abbr),
    description = GetTextFromResId(R.string.werckmeister_iii_desc),
    stableId = stableId,
    fifths = arrayOf(
        FifthModification(pythagoreanComma = RationalNumber(-1, 4)), // C     - G
        FifthModification(pythagoreanComma = RationalNumber(-1, 4)), // G     - D
        FifthModification(pythagoreanComma = RationalNumber(-1, 4)), // D     - A
        FifthModification(),                                                             // A      - E
        FifthModification(),                                                             // E     - B
        FifthModification(pythagoreanComma = RationalNumber(-1, 4)), // B     - F#/Gb
        FifthModification(),                                                             // F#/Gb - C#/Db
        FifthModification(),                                                             // C#/Db - G#/Ab
        FifthModification(),                                                             // G#/Ab - D#/Eb
        FifthModification(),                                                             // D#/Eb - A#/Bb
        FifthModification(),                                                             // A#/Bb - F
    ),
    rootIndex = 0
)

fun predefinedTemperamentWerckmeisterIV(stableId: Long)
        = Temperament3ChainOfFifthsEDONames(
    name = GetTextFromResId(R.string.werckmeister_iv),
    abbreviation = GetTextFromResId(R.string.werckmeister_iv_abbr),
    description = GetTextFromResId(R.string.werckmeister_iv_desc),
    stableId = stableId,
    fifths = arrayOf(
        /* CG */ FifthModification(pythagoreanComma = RationalNumber(-1, 3)),
        /* GD */ FifthModification(),
        /* DA */ FifthModification(pythagoreanComma = RationalNumber(-1, 3)),
        /* AE */ FifthModification(),
        /* EB */ FifthModification(pythagoreanComma = RationalNumber(-1, 3)),
        /* BF# */ FifthModification(),
        /* F#C# */ FifthModification(pythagoreanComma = RationalNumber(-1, 3)),
        /* C#G# */ FifthModification(),
        /* G#E# */ FifthModification(pythagoreanComma = RationalNumber(1, 3)),
        /* EbBb */ FifthModification(pythagoreanComma = RationalNumber(1, 3)),
        /* BbF */ FifthModification(pythagoreanComma = RationalNumber(-1, 3)),
    ),
    rootIndex = 0
)

fun predefinedTemperamentWerckmeisterV(stableId: Long)
        = Temperament3ChainOfFifthsEDONames(
    name = GetTextFromResId(R.string.werckmeister_v),
    abbreviation = GetTextFromResId(R.string.werckmeister_v_abbr),
    description = GetTextFromResId(R.string.werckmeister_v_desc),
    stableId = stableId,
    fifths = arrayOf(
        /* CG */ FifthModification(),
        /* GD */ FifthModification(),
        /* DA */ FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
        /* AE */ FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
        /* EB */ FifthModification(),
        /* BF# */ FifthModification(),
        /* F#C# */ FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
        /* C#G# */ FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
        /* G#Eb */ FifthModification(pythagoreanComma = RationalNumber(1, 4)),
        /* EbBb */ FifthModification(),
        /* BbF */ FifthModification(),
        //FC = FifthModification(pythagoreanComma = RationalNumber(-1, 4))
    ),
    rootIndex = 0
)

fun predefinedTemperamentWerckmeisterVI(stableId: Long)
        = Temperament3RationalNumbersEDONames(
    GetTextFromResId(R.string.werckmeister_vi),
    GetTextFromResId(R.string.werckmeister_vi_abbr),
    GetTextFromResId(R.string.werckmeister_vi_desc),
    rationalNumbers = arrayOf(
        RationalNumber(1, 1), // C
        RationalNumber(196, 186), // C#
        RationalNumber(196, 175), // D
        RationalNumber(196, 165), // Eb
        RationalNumber(196, 156), // E
        RationalNumber(196, 147), // F
        RationalNumber(196, 139), // F#
        RationalNumber(196, 131), // G
        RationalNumber(196, 124), // G#
        RationalNumber(196, 117), // A
        RationalNumber(196, 110), // Bb
        RationalNumber(196, 104), // B
        RationalNumber(2, 1), // C2
    ),
    stableId = stableId
)

fun predefinedTemperamentKirnberger1(stableId: Long)
        = Temperament3ChainOfFifthsEDONames(
    name = GetTextFromResId(R.string.kirnberger1),
    abbreviation = GetTextFromResId(R.string.kirnberger1_abbr),
    description = GetTextFromResId(R.string.kirnberger1_desc),
    stableId = stableId,
    fifths = arrayOf(
        /* CG */ FifthModification(),
        /* GD */ FifthModification(),
        /* DA */ FifthModification(syntonicComma = RationalNumber(-1, 1)),
        /* AE */FifthModification(),
        /* EB */ FifthModification(),
        /* BF# */ FifthModification(),
        /* F#C# */ FifthModification(schisma = RationalNumber(-1, 1)),
        /* C#G# */ FifthModification(),
        /* G#Eb */ FifthModification(),
        /* EbBb */ FifthModification(),
        /* BbF */FifthModification(),
        //FC = FifthModification()
    ),
    rootIndex = 0,
)

fun predefinedTemperamentKirnberger2(stableId: Long)
        = Temperament3ChainOfFifthsEDONames(
    name = GetTextFromResId(R.string.kirnberger2),
    abbreviation = GetTextFromResId(R.string.kirnberger2_abbr),
    description = GetTextFromResId(R.string.kirnberger2_desc),
    stableId = stableId,
    fifths = arrayOf(
        /* CG */ FifthModification(),
        /* GD */ FifthModification(),
        /* DA */  FifthModification(syntonicComma = RationalNumber(-1, 2)),
        /* AE */ FifthModification(syntonicComma = RationalNumber(-1, 2)),
        /* EB */ FifthModification(),
        /* BF# */ FifthModification(),
        /* F#C# */ FifthModification(schisma = RationalNumber(-1, 1)),
        /* C#G# */ FifthModification(),
        /* G#Eb */ FifthModification(),
        /* EbBb */ FifthModification(),
        /* BbF */ FifthModification(),
        // FC = FifthModification()
    ),
    rootIndex = 0
)

fun predefinedTemperamentKirnberger3(stableId: Long)
        = Temperament3ChainOfFifthsEDONames(
    name = GetTextFromResId(R.string.kirnberger3),
    abbreviation = GetTextFromResId(R.string.kirnberger3_abbr),
    description = GetTextFromResId(R.string.kirnberger3_desc),
    stableId = stableId,
    fifths = arrayOf(
        /* CG */ FifthModification(syntonicComma = RationalNumber(-1, 4)),
        /* GD */ FifthModification(syntonicComma = RationalNumber(-1, 4)),
        /* DA */ FifthModification(syntonicComma = RationalNumber(-1, 4)),
        /* AE */ FifthModification(syntonicComma = RationalNumber(-1, 4)),
        /* EB */ FifthModification(),
        /* BF# */ FifthModification(),
        /* F#C# */ FifthModification(schisma = RationalNumber(-1, 1)),
        /* C#G# */ FifthModification(),
        /* G#Eb */ FifthModification(),
        /* EbBb */ FifthModification(),
        /* BbF */ FifthModification(),
        // FC = FifthModification()
    ),
    rootIndex = 0
)

// Neidhardt 1, für ein Dorf, 1732
fun predefinedTemperamentNeidhardt1(stableId: Long)
        = Temperament3ChainOfFifthsEDONames(
    name = GetTextFromResId(R.string.neidhardt1),
    abbreviation = GetTextFromResId(R.string.neidhardt1_abbr),
    description = GetTextFromResId(R.string.neidhardt1_desc),
    stableId = stableId,
    fifths = arrayOf(
        /* CG */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
        /* GD */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* DA */ FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
        /* AE */ FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
        /* EB */ FifthModification(),
        /* BF# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
        /* F#C# */ FifthModification(),
        /* C#G# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
        /* G#Eb */ FifthModification(),
        /* EbBb */ FifthModification(),
        /* BbF */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
        // FC = FifthModification()
    ),
    rootIndex = 0
)

//  Neidhardt 2, für ein Dorf, 1724  / für eine kleine Stadt, 1732
fun predefinedTemperamentNeidhardt2(stableId: Long)
        = Temperament3ChainOfFifthsEDONames(
    name = GetTextFromResId(R.string.neidhardt2),
    abbreviation = GetTextFromResId(R.string.neidhardt2_abbr),
    description = GetTextFromResId(R.string.neidhardt2_desc),
    stableId = stableId,
    fifths = arrayOf(
        /* CG */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* GD */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* DA */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* AE */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* EB */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
        /* BF# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
        /* F#C# */ FifthModification(),
        /* C#G# */ FifthModification(),
        /* G#Eb */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
        /* EbBb */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
        /* BbF */ FifthModification(),
        // FC = FifthModification()
    ),
    rootIndex = 0
)

// Neidhardt 3, für eine kleine Stadt, 1724 / für eine große Stadt, 1732
fun predefinedTemperamentNeidhardt3(stableId: Long)
        = Temperament3ChainOfFifthsEDONames(
    name = GetTextFromResId(R.string.neidhardt3),
    abbreviation = GetTextFromResId(R.string.neidhardt3_abbr),
    description = GetTextFromResId(R.string.neidhardt3_desc),
    stableId = stableId,
    fifths = arrayOf(
        /* CG */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* GD */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* DA */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* AE */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
        /* EB */ FifthModification(),
        /* BF# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
        /* F#C# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
        /* C#G# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
        /* G#Eb */ FifthModification(),
        /* EbBb */ FifthModification(),
        /* BbF */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
        // FC = FifthModification(pythagoreanComma = RationalNumber(-1, 12))
    ),
    rootIndex = 0
)

// für eine große Stadt, 1724
// fun predefinedTemperamentNeidhardt4(stableId: Long)
//       = Temperament3ChainOfFifthsEDONames(
//            name = GetTextFromResId(R.string.neidhardt4),
//            abbreviation = GetTextFromResId(R.string.neidhardt4_abbr),
//            description = GetTextFromResId(R.string.neidhardt4_desc),
//            stableId = stableId,
//            fifths = arrayOf(
//                /* CG */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//                /* GD */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//                /* DA */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//                /* AE */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//                /* EB */ FifthModification(),
//                /* BF# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//                /* F#C# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//                /* C#G# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//                /* G#Eb */ FifthModification(),
//                /* EbBb */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//                /* BbF */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//                //FC = FifthModification()
//            ),
//            rootIndex = 0
//        )

// Valotti
fun predefinedTemperamentValotti(stableId: Long)
        = Temperament3ChainOfFifthsEDONames(
    name = GetTextFromResId(R.string.valotti),
    abbreviation = GetTextFromResId(R.string.valotti_abbr),
    description = GetTextFromString(""),
    stableId = stableId,
    fifths = arrayOf(
        /* CG */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* GD */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* DA */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* AE */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* EB */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* BFsharp */ FifthModification(),
        /* FsharpCsharp */ FifthModification(),
        /* CsharpGsharp */ FifthModification(),
        /* GsharpEflat */ FifthModification(),
        /* EFlatBflat */ FifthModification(),
        /* BflatF */ FifthModification(),
        //FC = FifthModification(pythagoreanComma = RationalNumber(-1, 6))
    ),
    rootIndex = 0
)

fun predefinedTemperamentYoung2(stableId: Long)
        = Temperament3ChainOfFifthsEDONames(
    name = GetTextFromResId(R.string.young2),
    abbreviation = GetTextFromResId(R.string.young2_abbr),
    description = GetTextFromString(""),
    stableId = stableId,
    fifths = arrayOf(
        /* CG */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* GD */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* DA */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* AE */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* EB */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* BF# */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
        /* F#C# */ FifthModification(),
        /* C#G# */ FifthModification(),
        /* G#Eb */ FifthModification(),
        /* EbBb */ FifthModification(),
        /* BbF */ FifthModification(),
        // FC = FifthModification()
    ),
    rootIndex = 0
)
