package de.moekadu.tuner.temperaments

val circleOfFifthsPythagorean = TemperamentCircleOfFifths(
    CG = FifthModification(),
    GD = FifthModification(),
    DA = FifthModification(),
    AE = FifthModification(),
    EB = FifthModification(),
    BFsharp = FifthModification(),
    FsharpCsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 1)),
    CsharpGsharp = FifthModification(),
    GsharpEflat = FifthModification(),
    EFlatBflat = FifthModification(),
    BflatF = FifthModification(),
    FC = FifthModification()
)

// quarter-comma meantone -> perfect major third
val circleOfFifthsQuarterCommaMeanTone = TemperamentCircleOfFifths(
    CG = FifthModification(syntonicComma = RationalNumber(-1, 4)),
    GD = FifthModification(syntonicComma = RationalNumber(-1, 4)),
    DA = FifthModification(syntonicComma = RationalNumber(-1, 4)),
    AE = FifthModification(syntonicComma = RationalNumber(-1, 4)),
    EB = FifthModification(syntonicComma = RationalNumber(-1, 4)),
    BFsharp = FifthModification(syntonicComma = RationalNumber(-1, 4)),
    FsharpCsharp = FifthModification(syntonicComma = RationalNumber(-1, 4)),
    CsharpGsharp = FifthModification(syntonicComma = RationalNumber(-1, 4)),
    GsharpEflat = FifthModification(pythagoreanComma = RationalNumber(-1, 1), syntonicComma = RationalNumber(11, 4)),
    EFlatBflat = FifthModification(syntonicComma = RationalNumber(-1, 4)),
    BflatF = FifthModification(syntonicComma = RationalNumber(-1, 4)),
    FC = FifthModification(syntonicComma = RationalNumber(-1, 4))
)

// third-comma meantone -> perfect minor third
val circleOfFifthsThirdCommaMeanTone = TemperamentCircleOfFifths(
    CG = FifthModification(syntonicComma = RationalNumber(-1, 3)),
    GD = FifthModification(syntonicComma = RationalNumber(-1, 3)),
    DA = FifthModification(syntonicComma = RationalNumber(-1, 3)),
    AE = FifthModification(syntonicComma = RationalNumber(-1, 3)),
    EB = FifthModification(syntonicComma = RationalNumber(-1, 3)),
    BFsharp = FifthModification(syntonicComma = RationalNumber(-1, 3)),
    FsharpCsharp = FifthModification(syntonicComma = RationalNumber(-1, 3)),
    CsharpGsharp = FifthModification(syntonicComma = RationalNumber(-1, 3)),
    GsharpEflat = FifthModification(pythagoreanComma = RationalNumber(-1, 1), syntonicComma = RationalNumber(11, 3)),
    EFlatBflat = FifthModification(syntonicComma = RationalNumber(-1, 3)),
    BflatF = FifthModification(syntonicComma = RationalNumber(-1, 3)),
    FC = FifthModification(syntonicComma = RationalNumber(-1, 3))
)

val circleOfFifthsEDO12 = TemperamentCircleOfFifths(
    CG = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    GD = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    DA = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    AE = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    EB = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    BFsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    FsharpCsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    CsharpGsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    GsharpEflat = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    EFlatBflat = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    BflatF = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    FC = FifthModification(pythagoreanComma = RationalNumber(-1, 12))
)

val circleOfFifthsWerckmeisterIII = TemperamentCircleOfFifths(
    CG = FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
    GD = FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
    DA = FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
    AE = FifthModification(),
    EB = FifthModification(),
    BFsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
    FsharpCsharp = FifthModification(),
    CsharpGsharp = FifthModification(),
    GsharpEflat = FifthModification(),
    EFlatBflat = FifthModification(),
    BflatF = FifthModification(),
    FC = FifthModification()
)

val circleOfFifthsWerckmeisterIV = TemperamentCircleOfFifths(
    CG = FifthModification(pythagoreanComma = RationalNumber(-1, 3)),
    GD = FifthModification(),
    DA = FifthModification(pythagoreanComma = RationalNumber(-1, 3)),
    AE = FifthModification(),
    EB = FifthModification(pythagoreanComma = RationalNumber(-1, 3)),
    BFsharp = FifthModification(),
    FsharpCsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 3)),
    CsharpGsharp = FifthModification(),
    GsharpEflat = FifthModification(pythagoreanComma = RationalNumber(1, 3)),
    EFlatBflat = FifthModification(pythagoreanComma = RationalNumber(1, 3)),
    BflatF = FifthModification(pythagoreanComma = RationalNumber(-1, 3)),
    FC = FifthModification()
)

val circleOfFifthsWerckmeisterV = TemperamentCircleOfFifths(
    CG = FifthModification(),
    GD = FifthModification(),
    DA = FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
    AE = FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
    EB = FifthModification(),
    BFsharp = FifthModification(),
    FsharpCsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
    CsharpGsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
    GsharpEflat = FifthModification(pythagoreanComma = RationalNumber(1, 4)),
    EFlatBflat = FifthModification(),
    BflatF = FifthModification(),
    FC = FifthModification(pythagoreanComma = RationalNumber(-1, 4))
)

val circleOfFifthsKirnberger1 = TemperamentCircleOfFifths(
    CG = FifthModification(),
    GD = FifthModification(),
    DA = FifthModification(syntonicComma = RationalNumber(-1, 1)),
    AE = FifthModification(),
    EB = FifthModification(),
    BFsharp = FifthModification(),
    FsharpCsharp = FifthModification(schisma = RationalNumber(-1, 1)),
    CsharpGsharp = FifthModification(),
    GsharpEflat = FifthModification(),
    EFlatBflat = FifthModification(),
    BflatF = FifthModification(),
    FC = FifthModification()
)

val circleOfFifthsKirnberger2 = TemperamentCircleOfFifths(
    CG = FifthModification(),
    GD = FifthModification(),
    DA = FifthModification(syntonicComma = RationalNumber(-1, 2)),
    AE = FifthModification(syntonicComma = RationalNumber(-1, 2)),
    EB = FifthModification(),
    BFsharp = FifthModification(),
    FsharpCsharp = FifthModification(schisma = RationalNumber(-1, 1)),
    CsharpGsharp = FifthModification(),
    GsharpEflat = FifthModification(),
    EFlatBflat = FifthModification(),
    BflatF = FifthModification(),
    FC = FifthModification()
)

val circleOfFifthsKirnberger3 = TemperamentCircleOfFifths(
    CG = FifthModification(syntonicComma = RationalNumber(-1, 4)),
    GD = FifthModification(syntonicComma = RationalNumber(-1, 4)),
    DA = FifthModification(syntonicComma = RationalNumber(-1, 4)),
    AE = FifthModification(syntonicComma = RationalNumber(-1, 4)),
    EB = FifthModification(),
    BFsharp = FifthModification(),
    FsharpCsharp = FifthModification(schisma = RationalNumber(-1, 1)),
    CsharpGsharp = FifthModification(),
    GsharpEflat = FifthModification(),
    EFlatBflat = FifthModification(),
    BflatF = FifthModification(),
    FC = FifthModification()
)

// für ein Dorf, 1732
val circleOfFifthsNeidhardt1 = TemperamentCircleOfFifths(
    CG = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    GD = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    DA = FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
    AE = FifthModification(pythagoreanComma = RationalNumber(-1, 4)),
    EB = FifthModification(),
    BFsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    FsharpCsharp = FifthModification(),
    CsharpGsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    GsharpEflat = FifthModification(),
    EFlatBflat = FifthModification(),
    BflatF = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    FC = FifthModification()
)

// für ein Dorf, 1724  / für eine kleine Stadt, 1732
val circleOfFifthsNeidhardt2 = TemperamentCircleOfFifths(
    CG = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    GD = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    DA = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    AE = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    EB = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    BFsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    FsharpCsharp = FifthModification(),
    CsharpGsharp = FifthModification(),
    GsharpEflat = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    EFlatBflat = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    BflatF = FifthModification(),
    FC = FifthModification()
)

// für eine kleine Stadt, 1724 / für eine große Stadt, 1732
val circleOfFifthsNeidhardt3 = TemperamentCircleOfFifths(
    CG = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    GD = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    DA = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    AE = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    EB = FifthModification(),
    BFsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    FsharpCsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    CsharpGsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    GsharpEflat = FifthModification(),
    EFlatBflat = FifthModification(),
    BflatF = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    FC = FifthModification(pythagoreanComma = RationalNumber(-1, 12))
)

// für eine große Stadt, 1724
val circleOfFifthsNeidthardt4 = TemperamentCircleOfFifths(
    CG = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    GD = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    DA = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    AE = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    EB = FifthModification(),
    BFsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    FsharpCsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    CsharpGsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    GsharpEflat = FifthModification(),
    EFlatBflat = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    BflatF = FifthModification(pythagoreanComma = RationalNumber(-1, 12)),
    FC = FifthModification()
)

val circleOfFifthsValotti = TemperamentCircleOfFifths(
    CG = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    GD = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    DA = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    AE = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    EB = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    BFsharp = FifthModification(),
    FsharpCsharp = FifthModification(),
    CsharpGsharp = FifthModification(),
    GsharpEflat = FifthModification(),
    EFlatBflat = FifthModification(),
    BflatF = FifthModification(),
    FC = FifthModification(pythagoreanComma = RationalNumber(-1, 6))
)

val circleOfFifthsYoung2 = TemperamentCircleOfFifths(
    CG = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    GD = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    DA = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    AE = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    EB = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    BFsharp = FifthModification(pythagoreanComma = RationalNumber(-1, 6)),
    FsharpCsharp = FifthModification(),
    CsharpGsharp = FifthModification(),
    GsharpEflat = FifthModification(),
    EFlatBflat = FifthModification(),
    BflatF = FifthModification(),
    FC = FifthModification()
)

class TemperamentCircleOfFifths(
    val CG: FifthModification,
    val GD: FifthModification,
    val DA: FifthModification,
    val AE: FifthModification,
    val EB: FifthModification,
    val BFsharp: FifthModification,
    val FsharpCsharp: FifthModification,
    val CsharpGsharp: FifthModification,
    val GsharpEflat: FifthModification,
    val EFlatBflat: FifthModification,
    val BflatF: FifthModification,
    val FC: FifthModification
) {

    fun getRatios(): DoubleArray {
        // 0   1   2   3   4   5   6   7   8   9   10   11   12
        // C   C#  D   Eb  E   F   F#  G   G#  A   Bb   B    C
        val ratios = DoubleArray(13)
        val threeHalf = RationalNumber(3, 2)
        var totalCorrection = FifthModification()
        ratios[0] = 1.0
        totalCorrection += CG
        ratios[7] =  threeHalf.toDouble() * totalCorrection.toDouble()
        totalCorrection += GD
        ratios[2] = (threeHalf.pow(2) / 2).toDouble() * totalCorrection.toDouble()
        totalCorrection += DA
        ratios[9] = (threeHalf.pow(3) / 2).toDouble() * totalCorrection.toDouble()
        totalCorrection += AE
        ratios[4] = (threeHalf.pow(4) / 4).toDouble() * totalCorrection.toDouble()
        totalCorrection += EB
        ratios[11] = (threeHalf.pow(5) / 4).toDouble() * totalCorrection.toDouble()
        totalCorrection += BFsharp
        ratios[6] = (threeHalf.pow(6) / 8).toDouble() * totalCorrection.toDouble()
        totalCorrection += FsharpCsharp
        ratios[1] = (threeHalf.pow(7) / 16).toDouble() * totalCorrection.toDouble()
        totalCorrection += CsharpGsharp
        ratios[8] = (threeHalf.pow(8) / 16).toDouble() * totalCorrection.toDouble()
        totalCorrection += GsharpEflat
        ratios[3] = (threeHalf.pow(9) / 32).toDouble() * totalCorrection.toDouble()
        totalCorrection += EFlatBflat
        ratios[10] = (threeHalf.pow(10) / 32).toDouble() * totalCorrection.toDouble()
        totalCorrection += BflatF
        ratios[5] = (threeHalf.pow(11) / 64).toDouble() * totalCorrection.toDouble()
        totalCorrection += FC
        ratios[12] = (threeHalf.pow(12) / 64).toDouble() * totalCorrection.toDouble()
        return ratios
    }
}