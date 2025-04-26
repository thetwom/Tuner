package de.moekadu.tuner.temperaments

import kotlinx.serialization.Serializable

@Serializable
data class ChainOfFifths(
    val fifths: Array<out FifthModification>,
    val rootIndex: Int
) {
    fun getClosingCircleCorrection(): FifthModification {
        var totalCorrection = FifthModification(
            pythagoreanComma = RationalNumber(-1, 1)
        )
        for (fifth in fifths)
            totalCorrection -= fifth
        return totalCorrection
    }

    fun getRatiosAlongFifths(): DoubleArray {
        //               |
        //               v
        //   Ab Eb Bb F  C  G  D  A  E  B F# C#  (G#)
        //    \  \  \  \  \  \  \  \  \  \  \ (\)  (\)
        //     0  1  2  3  4  5  6  7  8  9 10 (11) (12)
        val ratios = DoubleArray(fifths.size + 1)
        var totalCorrection = FifthModification()
        ratios[rootIndex] = 1.0
        var fifthRatio = RationalNumber(1, 1)

        val threeHalf = RationalNumber(3, 2)

        for (i in rootIndex until fifths.size) {
            totalCorrection += fifths[i]
            fifthRatio *= threeHalf
            if (fifthRatio.numerator > 2 * fifthRatio.denominator)
                fifthRatio /= 2
            ratios[i + 1] = fifthRatio.toDouble() * totalCorrection.toDouble()
        }

        fifthRatio = RationalNumber(1, 1)
        totalCorrection = FifthModification()
        for (i in rootIndex-1 downTo  0) {
            totalCorrection -= fifths[i]
            fifthRatio /= threeHalf
            if (fifthRatio.numerator < fifthRatio.denominator)
                fifthRatio *= 2
            ratios[i] = fifthRatio.toDouble() * totalCorrection.toDouble()
        }
        return ratios
    }

    fun getSortedRatios(): DoubleArray {
        return getRatiosAlongFifths().sortedArray()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChainOfFifths

        if (rootIndex != other.rootIndex) return false
        if (!fifths.contentEquals(other.fifths)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rootIndex
        result = 31 * result + fifths.contentHashCode()
        return result
    }

    companion object {
        fun create(vararg fifths: FifthModification, rootIndex: Int): ChainOfFifths {
            return ChainOfFifths(fifths, rootIndex)
        }
    }
}

//val chainOfFifthsPythagorean = ChainOfFifths.create(
//    FifthModification(), // Db - Ab
//    FifthModification(), // Ab - Eb
//    FifthModification(), // Eb - Bb
//    FifthModification(), // Bb - F
//    FifthModification(), // F  - C
//    FifthModification(), // C  - G <- root
//    FifthModification(), // G  - D
//    FifthModification(), // D  - A
//    FifthModification(), // A  - E
//    FifthModification(), // E  - B
//    FifthModification(), // B  - F#
//    rootIndex = 5
//)
//
//val chainOfFifthsQuarterCommaMeantone = ChainOfFifths.create(
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // Eb - Bb
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // Bb - F
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // F  - C
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // C  - G <- root
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // G  - D
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // D  - A
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // A  - E
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // E  - B
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // B  - F#
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // F# - C#
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // C# - G#
//    rootIndex = 3
//)
//
//val chainOfFifthsExtendedQuarterCommaMeantone = ChainOfFifths.create(
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // Ab - Eb
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // Eb - Bb
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // Bb - F
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // F  - C
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // C  - G <- root
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // G  - D
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // D  - A
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // A  - E
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // E  - B
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // B  - F#
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // F# - C#
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // C# - G#
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // G# - D#
//    FifthModification(syntonicComma = RationalNumber(-1, 4)), // D# - A#
//    rootIndex = 4
//)
//
//val chainOfFifthsThirdCommaMeantone = ChainOfFifths.create(
//    FifthModification(syntonicComma = RationalNumber(-1, 3)), // Eb - Bb
//    FifthModification(syntonicComma = RationalNumber(-1, 3)), // Bb - F
//    FifthModification(syntonicComma = RationalNumber(-1, 3)), // F  - C
//    FifthModification(syntonicComma = RationalNumber(-1, 3)), // C  - G <- root
//    FifthModification(syntonicComma = RationalNumber(-1, 3)), // G  - D
//    FifthModification(syntonicComma = RationalNumber(-1, 3)), // D  - A
//    FifthModification(syntonicComma = RationalNumber(-1, 3)), // A  - E
//    FifthModification(syntonicComma = RationalNumber(-1, 3)), // E  - B
//    FifthModification(syntonicComma = RationalNumber(-1, 3)), // B  - F#
//    FifthModification(syntonicComma = RationalNumber(-1, 3)), // F# - C#
//    FifthModification(syntonicComma = RationalNumber(-1, 3)), // C# - G#
//    rootIndex = 3
//)
//
//val chainOfFifthsFifthCommaMeantone = ChainOfFifths.create(
//    FifthModification(syntonicComma = RationalNumber(-1, 5)), // Eb - Bb
//    FifthModification(syntonicComma = RationalNumber(-1, 5)), // Bb - F
//    FifthModification(syntonicComma = RationalNumber(-1, 5)), // F  - C
//    FifthModification(syntonicComma = RationalNumber(-1, 5)), // C  - G <- root
//    FifthModification(syntonicComma = RationalNumber(-1, 5)), // G  - D
//    FifthModification(syntonicComma = RationalNumber(-1, 5)), // D  - A
//    FifthModification(syntonicComma = RationalNumber(-1, 5)), // A  - E
//    FifthModification(syntonicComma = RationalNumber(-1, 5)), // E  - B
//    FifthModification(syntonicComma = RationalNumber(-1, 5)), // B  - F#
//    FifthModification(syntonicComma = RationalNumber(-1, 5)), // F# - C#
//    FifthModification(syntonicComma = RationalNumber(-1, 5)), // C# - G#
//    rootIndex = 3
//)
//
//val chainOfFifthsEDO12 = ChainOfFifths.create(
//    FifthModification(pythagoreanComma = RationalNumber(-1, 12)), // C     - G
//    FifthModification(pythagoreanComma = RationalNumber(-1, 12)), // G     - D
//    FifthModification(pythagoreanComma = RationalNumber(-1, 12)), // D     - A
//    FifthModification(pythagoreanComma = RationalNumber(-1, 12)), // A     - E
//    FifthModification(pythagoreanComma = RationalNumber(-1, 12)), // E     - B
//    FifthModification(pythagoreanComma = RationalNumber(-1, 12)), // B     - F#/Gb
//    FifthModification(pythagoreanComma = RationalNumber(-1, 12)), // F#/Gb - C#/Db
//    FifthModification(pythagoreanComma = RationalNumber(-1, 12)), // C#/Db - G#/Ab
//    FifthModification(pythagoreanComma = RationalNumber(-1, 12)), // G#/Ab - D#/Eb
//    FifthModification(pythagoreanComma = RationalNumber(-1, 12)), // D#/Eb - A#/Bb
//    FifthModification(pythagoreanComma = RationalNumber(-1, 12)), // A#/Bb - F
//    //                                                                                 // F     - C
//    rootIndex = 0
//)
//
//val chainOfFifthsWerckmeisterIII = ChainOfFifths.create(
//    FifthModification(pythagoreanComma = RationalNumber(-1, 4)), // C     - G
//    FifthModification(pythagoreanComma = RationalNumber(-1, 4)), // G     - D
//    FifthModification(pythagoreanComma = RationalNumber(-1, 4)), // D     - A
//    FifthModification(),                                                             // A      - E
//    FifthModification(),                                                             // E     - B
//    FifthModification(pythagoreanComma = RationalNumber(-1, 4)), // B     - F#/Gb
//    FifthModification(),                                                             // F#/Gb - C#/Db
//    FifthModification(),                                                             // C#/Db - G#/Ab
//    FifthModification(),                                                             // G#/Ab - D#/Eb
//    FifthModification(),                                                             // D#/Eb - A#/Bb
//    FifthModification(),                                                             // A#/Bb - F
//    rootIndex = 0
//)
//
//val chainOfFifthsWerckmeisterIV = ChainOfFifths.create(
//    /* CG */ FifthModification(pythagoreanComma = RationalNumber(-1, 3)),
//    /* GD */ FifthModification(),
//    /* DA */ FifthModification(pythagoreanComma = RationalNumber(-1, 3)),
//    /* AE */ FifthModification(),
//    /* EB */ FifthModification(pythagoreanComma = RationalNumber(-1, 3)),
//    /* BF# */ FifthModification(),
//    /* F#C# */ FifthModification(pythagoreanComma = RationalNumber(-1, 3)),
//    /* C#G# */ FifthModification(),
//    /* G#E# */ FifthModification(pythagoreanComma = RationalNumber(1, 3)),
//    /* EbBb */ FifthModification(pythagoreanComma = RationalNumber(1, 3)),
//    /* BbF */ FifthModification(pythagoreanComma = RationalNumber(-1, 3)),
//    rootIndex = 0
//)
//
//val chainOfFifthsWerckmeisterV = ChainOfFifths.create(
//    /* CG */ FifthModification(),
//    /* GD */ FifthModification(),
//    /* DA */ FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
//    /* AE */ FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
//    /* EB */ FifthModification(),
//    /* BF# */ FifthModification(),
//    /* F#C# */ FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
//    /* C#G# */ FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
//    /* G#Eb */ FifthModification(pythagoreanComma = RationalNumber(1, 4)),
//    /* EbBb */ FifthModification(),
//    /* BbF */ FifthModification(),
//    //FC = FifthModification(pythagoreanComma = RationalNumber(-1, 4))
//    rootIndex = 0
//)
//
//val chainOfFifthsKirnberger1 = ChainOfFifths.create(
//    /* CG */ FifthModification(),
//    /* GD */ FifthModification(),
//    /* DA */ FifthModification(syntonicComma = RationalNumber(-1, 1)),
//    /* AE */FifthModification(),
//    /* EB */ FifthModification(),
//    /* BF# */ FifthModification(),
//    /* F#C# */ FifthModification(schisma = RationalNumber(-1, 1)),
//    /* C#G# */ FifthModification(),
//    /* G#Eb */ FifthModification(),
//    /* EbBb */ FifthModification(),
//    /* BbF */FifthModification(),
//    //FC = FifthModification()
//    rootIndex = 0
//)
//
//val chainOfFifthsKirnberger2 = ChainOfFifths.create(
//    /* CG */ FifthModification(),
//    /* GD */ FifthModification(),
//    /* DA */  FifthModification(syntonicComma = RationalNumber(-1, 2)),
//    /* AE */ FifthModification(syntonicComma = RationalNumber(-1, 2)),
//    /* EB */ FifthModification(),
//    /* BF# */ FifthModification(),
//    /* F#C# */ FifthModification(schisma = RationalNumber(-1, 1)),
//    /* C#G# */ FifthModification(),
//    /* G#Eb */ FifthModification(),
//    /* EbBb */ FifthModification(),
//    /* BbF */ FifthModification(),
//    // FC = FifthModification()
//    rootIndex = 0
//)
//
//val chainOfFifthsKirnberger3 = ChainOfFifths.create(
//    /* CG */ FifthModification(syntonicComma = RationalNumber(-1, 4)),
//    /* GD */ FifthModification(syntonicComma = RationalNumber(-1, 4)),
//    /* DA */ FifthModification(syntonicComma = RationalNumber(-1, 4)),
//    /* AE */ FifthModification(syntonicComma = RationalNumber(-1, 4)),
//    /* EB */ FifthModification(),
//    /* BF# */ FifthModification(),
//    /* F#C# */ FifthModification(schisma = RationalNumber(-1, 1)),
//    /* C#G# */ FifthModification(),
//    /* G#Eb */ FifthModification(),
//    /* EbBb */ FifthModification(),
//    /* BbF */ FifthModification(),
//    // FC = FifthModification()
//    rootIndex = 0
//)
//
//// für ein Dorf, 1732
//val chainOfFifthsNeidhardt1 = ChainOfFifths.create(
//    /* CG */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    /* GD */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* DA */ FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
//    /* AE */ FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
//    /* EB */ FifthModification(),
//    /* BF# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    /* F#C# */ FifthModification(),
//    /* C#G# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    /* G#Eb */ FifthModification(),
//    /* EbBb */ FifthModification(),
//    /* BbF */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    // FC = FifthModification()
//    rootIndex = 0
//)
//
//// für ein Dorf, 1724  / für eine kleine Stadt, 1732
//val chainOfFifthsNeidhardt2 = ChainOfFifths.create(
//    /* CG */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* GD */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* DA */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* AE */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* EB */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    /* BF# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    /* F#C# */ FifthModification(),
//    /* C#G# */ FifthModification(),
//    /* G#Eb */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    /* EbBb */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    /* BbF */ FifthModification(),
//    // FC = FifthModification()
//    rootIndex = 0
//)
//
//// für eine kleine Stadt, 1724 / für eine große Stadt, 1732
//val chainOfFifthsNeidhardt3 = ChainOfFifths.create(
//    /* CG */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* GD */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* DA */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* AE */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    /* EB */ FifthModification(),
//    /* BF# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    /* F#C# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    /* C#G# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    /* G#Eb */ FifthModification(),
//    /* EbBb */ FifthModification(),
//    /* BbF */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    // FC = FifthModification(pythagoreanComma = RationalNumber(-1, 12))
//    rootIndex = 0
//)
//
//// für eine große Stadt, 1724
//val chainOfFifthsNeidhardt4 = ChainOfFifths.create(
//    /* CG */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* GD */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* DA */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* AE */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    /* EB */ FifthModification(),
//    /* BF# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    /* F#C# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    /* C#G# */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    /* G#Eb */ FifthModification(),
//    /* EbBb */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    /* BbF */ FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
//    //FC = FifthModification()
//    rootIndex = 0
//)
//
//val chainOfFifthsValotti = ChainOfFifths.create(
//    /* CG */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* GD */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* DA */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* AE */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* EB */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* BFsharp */ FifthModification(),
//    /* FsharpCsharp */ FifthModification(),
//    /* CsharpGsharp */ FifthModification(),
//    /* GsharpEflat */ FifthModification(),
//    /* EFlatBflat */ FifthModification(),
//    /* BflatF */ FifthModification(),
//    //FC = FifthModification(pythagoreanComma = RationalNumber(-1, 6))
//    rootIndex = 0
//)
//
//val chainOfFifthsYoung2 = ChainOfFifths.create(
//    /* CG */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* GD */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* DA */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* AE */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* EB */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* BF# */ FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
//    /* F#C# */ FifthModification(),
//    /* C#G# */ FifthModification(),
//    /* G#Eb */ FifthModification(),
//    /* EbBb */ FifthModification(),
//    /* BbF */ FifthModification(),
//    // FC = FifthModification()
//    rootIndex = 0
//)
