package de.moekadu.tuner.temperaments

import kotlin.math.*

open class TemperamentRatioBased(
    private val _temperament: Temperament,
    private val ratios: DoubleArray,
    private val rootNoteIndex: Int = -9, // the first ratio of ratios is set at this index (-9 for 12-tone is c)
    private val noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
    private val _referenceFrequency: Float = 440f,
    frequencyMin: Float = 16.0f, // this would be c0 if the noteIndexAtReferenceFrequency is 0 (~16.4Hz for equal temperament)
    frequencyMax: Float = 17000.0f) // this would be c10 if the noteIndexAtReferenceFrequency is 0 (~16744Hz for equal temperament)
    : TemperamentFrequencies {

    private var noteIndexBegin = Int.MAX_VALUE
    private var noteIndexEnd = Int.MIN_VALUE

    private var frequencies = FloatArray(0)
    private var circleOfFifths: TemperamentCircleOfFifths? = null
    private var rationalNumberRatios: Array<RationalNumber>? = null

    constructor(
        temperament: Temperament,
        circleOfFifths: TemperamentCircleOfFifths,
        rootNoteIndex: Int,
        noteIndexAtReferenceFrequency: Int,
        referenceFrequency: Float
    ) : this(
        temperament,
        circleOfFifths.getRatios(),
        rootNoteIndex,
        noteIndexAtReferenceFrequency,
        referenceFrequency
    ) {
        this.circleOfFifths = circleOfFifths
    }

    constructor(
        temperament: Temperament,
        rationalNumbers: Array<RationalNumber>,
        rootNoteIndex: Int,
        noteIndexAtReferenceFrequency: Int,
        referenceFrequency: Float
    ) : this(
        temperament,
        rationalNumbers.map {it.toDouble()}.toDoubleArray(),
        rootNoteIndex,
        noteIndexAtReferenceFrequency,
        referenceFrequency
    ) {
        this.rationalNumberRatios = rationalNumbers
    }

    init {
        //require(rootNoteIndex >= 0)
        //require(rootNoteIndex < ratios.size - 1)

        val numNotesPerOctave = ratios.size - 1
        val octaveRatio = ratios.last() / ratios.first()
        var ratioIndexReference = (noteIndexAtReferenceFrequency - rootNoteIndex) % numNotesPerOctave
        if (ratioIndexReference < 0)
            ratioIndexReference += numNotesPerOctave

        // recompute ratios such that the referenceNote comes first
        val ratiosRef = DoubleArray(numNotesPerOctave) {
            if (it + ratioIndexReference < ratios.size)
                (ratios[it + ratioIndexReference] / ratios[ratioIndexReference])
            else
                (octaveRatio * ratios[it + ratioIndexReference - numNotesPerOctave] / ratios[ratioIndexReference])
        }

        val positiveIndices = ArrayList<Float>()
        val negativeIndices = ArrayList<Float>()

        var i = 0
        while (true) {
            val f = computeNoteFrequency(i, ratiosRef, octaveRatio)
            if (f > frequencyMax) {
                break
            } else if (f >= frequencyMin) {
                positiveIndices.add(f)
                noteIndexEnd = i + 1
                // the min means, that we only set this at the first loop cycle
                noteIndexBegin = min(i, noteIndexBegin)
            }
            ++i
        }

        i = -1
        while (true) {
            val f = computeNoteFrequency(i, ratiosRef, octaveRatio)
            if (f < frequencyMin) {
                break
            } else if (f <= frequencyMax) {
                negativeIndices.add(f)
                noteIndexBegin = i
                // the max means, that we only set this at the first loop cycle
                noteIndexEnd = max(i + 1, noteIndexEnd)
            }
            --i
        }

        frequencies = FloatArray(negativeIndices.size + positiveIndices.size)
        negativeIndices.reversed().forEachIndexed { index, f -> frequencies[index] = f }
        positiveIndices.forEachIndexed { index, f -> frequencies[negativeIndices.size + index] = f }
//        Log.v("Tuner", "TuningRatioBased.init: end")
    }

    override fun getCircleOfFifths(): TemperamentCircleOfFifths? {
        return circleOfFifths
    }

    override fun getRationalNumberRatios(): Array<RationalNumber>? {
        return rationalNumberRatios
    }

    override fun getTemperament(): Temperament {
        return _temperament
    }

    override fun getRootNote(): Int {
        return rootNoteIndex
    }

    override fun getNumberOfNotesPerOctave(): Int {
        return ratios.size - 1
    }
    override fun getIndexOfReferenceNote(): Int {
       return noteIndexAtReferenceFrequency
    }

    override fun getReferenceFrequency(): Float {
        return _referenceFrequency
    }

    override fun getToneIndexBegin(): Int {
        return noteIndexBegin
    }

    override fun getToneIndexEnd(): Int {
        return noteIndexEnd
    }

    override fun getToneIndex(frequency: Float): Float {
        if (frequencies.isEmpty())
            return 0f
        if (frequency <= frequencies.first())
            return noteIndexBegin.toFloat()
        else if (frequency >= frequencies.last())
            return (noteIndexEnd - 1).toFloat()

        val closestToneIndex = getClosestToneIndex(frequency)
        val j = closestToneIndex - noteIndexBegin
        return if (frequency == frequencies[j] || frequencies.size == 1)
            closestToneIndex.toFloat()
        else if (j > 0 && (frequency < frequencies[j] || j == frequencies.size - 1))
            closestToneIndex - log10(frequencies[j] / frequency) / log10(frequencies[j] / frequencies[j - 1])
        else if (j < frequencies.size - 1 && (frequency > frequencies[j] || closestToneIndex == 0))
            closestToneIndex + log10(frequencies[j] / frequency) / log10(frequencies[j] / frequencies[j + 1])
        else
            throw RuntimeException("cannot find tone index")
    }

    override fun getClosestToneIndex(frequency: Float): Int {
        if (frequencies.isEmpty())
            return 0
        val index = frequencies.binarySearch(frequency)
        if (index >= 0)
            return index + noteIndexBegin
        val indexAfter = -index - 1
        val indexBefore = indexAfter - 1

        // Avoid accessing frequency array out of bounds
        if (indexAfter <= 0)
            return noteIndexBegin
        if (indexAfter >= frequencies.size)
            return noteIndexEnd - 1

        // the sorting is as follows:
        // frequency at indexBefore < frequency < frequency at indexAfter
        // we want to check what is closest based on log scale
        return if (log10(frequency / frequencies[indexBefore]) < log10(frequencies[indexAfter] / frequency))
            indexBefore + noteIndexBegin
        else
            indexAfter + noteIndexBegin
    }

    override fun getNoteFrequency(noteIndex: Int): Float {
        return when {
            noteIndexEnd <= noteIndexBegin -> _referenceFrequency
            noteIndex < noteIndexBegin -> frequencies.first()
            noteIndex >= noteIndexEnd -> frequencies.last()
            else ->frequencies[noteIndex - noteIndexBegin]
        }
    }

    override fun getNoteFrequency(noteIndex: Float): Float {
        // there are no frequencies ...
        if (noteIndexEnd <= noteIndexBegin)
            return _referenceFrequency

        // noteIndex = closestNoteIndex +/- log10(frequencies[j] / frequency) / log10(frequencies[j] / frequencies[j +/- 1])
        // (noteIndex - closestNoteIndex) * log10(frequencies[j] / frequencies[j +/- 1]) = +/- log10(frequencies[j] / frequency)
        // 10**( +/- (noteIndex - closestNoteIndex) * log10(frequencies[j] / frequencies[j +/- 1])) = frequencies[j] / frequency
        // frequency = frequencies[i] * 10**( -/+ (noteIndex - closestNoteIndex) * log10(frequencies[j] / frequencies[j +/- 1]))
        // frequency = frequencies[i] * (frequencies[j] / frequencies[j +/- 1])**( -/+ (noteIndex - closestNoteIndex)
        val noteIndexLower = floor(noteIndex).toInt()
        val arrayIndexLower = noteIndexLower - noteIndexBegin
        return when {
            arrayIndexLower < 0 -> {
                frequencies[0] * (frequencies[1] / frequencies[0]).pow(noteIndex - noteIndexBegin)
            }
            arrayIndexLower >= frequencies.size - 1 -> {
                frequencies[frequencies.size - 1] * (frequencies[frequencies.size - 1] / frequencies[frequencies.size - 2]).pow(
                    noteIndex - (frequencies.size - 1)
                )
            }
            else -> {
                frequencies[arrayIndexLower] * (frequencies[arrayIndexLower + 1] / frequencies[arrayIndexLower]).pow(
                    noteIndex - noteIndexLower
                )
            }
        }
    }

    private fun computeNoteFrequency(index: Int, ratios: DoubleArray, octaveRatio: Double): Float {
        val numNotesPerOctave = ratios.size
        val numOctaves = if (index >= noteIndexAtReferenceFrequency)
            (index - noteIndexAtReferenceFrequency) / numNotesPerOctave
        else
            -((noteIndexAtReferenceFrequency - index + numNotesPerOctave - 1) / numNotesPerOctave)
        val ratioIndex = index - noteIndexAtReferenceFrequency - numOctaves * numNotesPerOctave
        return (_referenceFrequency * octaveRatio.pow(numOctaves) * ratios[ratioIndex]).toFloat()
    }
}

//class PythagoreanTuning(rootNoteIndex: Int = -9,
//                        noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
//                        referenceFrequency: Float = 440f)
//    : TuningRatioBased(
//    circleOfFifths = circleOfFifthsPythagorean,
//    rootNoteIndex = rootNoteIndex,
//    noteIndexAtReferenceFrequency = noteIndexAtReferenceFrequency,
//    referenceFrequency = referenceFrequency
//)
//
//class EDO12Tuning(rootNoteIndex: Int = -9,
//                  noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
//                  referenceFrequency: Float = 440f)
//    : TuningRatioBased(
//    circleOfFifths = circleOfFifthsEDO12,
//    rootNoteIndex = rootNoteIndex,
//    noteIndexAtReferenceFrequency = noteIndexAtReferenceFrequency,
//    referenceFrequency = referenceFrequency
//)
//
//// five-limit tuning -> asymmetric
//class PureTuning(rootNoteIndex: Int = -9,
//                 noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
//                 referenceFrequency: Float = 440f)
//    : TuningRatioBased(
//    ratios = doubleArrayOf(
//        1.0, // C
//        16.0 / 15.0, // C#
//        9.0 / 8.0, // D
//        6.0 / 5.0, // Eb
//        5.0 / 4.0, // E
//        4.0 / 3.0,  // F
//        45.0 / 32.0, // F#
//        3.0 / 2.0, // G
//        8.0 / 5.0, // G#
//        5.0 / 3.0, // A
//        9.0 / 5.0,  // Bb   (sometimes 16.0/9.0)
//        15.0 / 8.0,  // B
//        2.0 // C
//    ),
//    rootNoteIndex = rootNoteIndex,
//    noteIndexAtReferenceFrequency = noteIndexAtReferenceFrequency,
//    referenceFrequency = referenceFrequency
//)
//
//class QuarterCommaMeanToneTuning(rootNoteIndex: Int = -9,
//                 noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
//                 referenceFrequency: Float = 440f)
//    : TuningRatioBased(
//    circleOfFifths = circleOfFifthsQuarterCommaMeanTone,
//    rootNoteIndex = rootNoteIndex,
//    noteIndexAtReferenceFrequency = noteIndexAtReferenceFrequency,
//    referenceFrequency = referenceFrequency
//)
//
//class ThirdCommaMeanToneTuning(rootNoteIndex: Int = -9,
//                                 noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
//                                 referenceFrequency: Float = 440f)
//    : TuningRatioBased(
//    circleOfFifths = circleOfFifthsThirdCommaMeanTone,
//    rootNoteIndex = rootNoteIndex,
//    noteIndexAtReferenceFrequency = noteIndexAtReferenceFrequency,
//    referenceFrequency = referenceFrequency
//)
//
//class WerckmeisterIIITuning(rootNoteIndex: Int = -9,
//                            noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
//                            referenceFrequency: Float = 440f)
//    : TuningRatioBased(
//    circleOfFifths = circleOfFifthsWerckmeisterIII,
//    rootNoteIndex = rootNoteIndex,
//    noteIndexAtReferenceFrequency = noteIndexAtReferenceFrequency,
//    referenceFrequency = referenceFrequency
//)
//
//class WerckmeisterIVTuning(rootNoteIndex: Int = -9,
//                           noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
//                           referenceFrequency: Float = 440f)
//    : TuningRatioBased(
//    circleOfFifths = circleOfFifthsWerckmeisterIV,
//    rootNoteIndex = rootNoteIndex,
//    noteIndexAtReferenceFrequency = noteIndexAtReferenceFrequency,
//    referenceFrequency = referenceFrequency
//)
//
//class WerckmeisterVTuning(rootNoteIndex: Int = -9,
//                           noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
//                           referenceFrequency: Float = 440f)
//    : TuningRatioBased(
//    circleOfFifths = circleOfFifthsWerckmeisterV,
//    rootNoteIndex = rootNoteIndex,
//    noteIndexAtReferenceFrequency = noteIndexAtReferenceFrequency,
//    referenceFrequency = referenceFrequency
//)
//
//class WerckmeisterVITuning(rootNoteIndex: Int = -9,
//                          noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
//                          referenceFrequency: Float = 440f)
//    : TuningRatioBased(
//    ratios = doubleArrayOf(
//        1.0, // c
//        196.0 / 186.0, // c sharp
//        196.0 / 175.0, // d
//        196.0 / 165.0, // d sharp
//        196.0 / 156.0, // e
//        196.0 / 147.0, // f
//        196.0 / 139.0, // f sharp
//        196.0 / 131.0, // g
//        196.0 / 124.0, // g sharp
//        196.0 / 117.0, // a
//        196.0 / 110.0, // a sharp
//        196.0 / 104.0, // b
//        2.0, // c
//    ),
//    rootNoteIndex = rootNoteIndex,
//    noteIndexAtReferenceFrequency = noteIndexAtReferenceFrequency,
//    referenceFrequency = referenceFrequency
//)
//
//class Kirnberger1Tuning(rootNoteIndex: Int = -9,
//                        noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
//                        referenceFrequency: Float = 440f)
//    : TuningRatioBased(
//    circleOfFifths = circleOfFifthsKirnberger1,
//    rootNoteIndex = rootNoteIndex,
//    noteIndexAtReferenceFrequency = noteIndexAtReferenceFrequency,
//    referenceFrequency = referenceFrequency
//)
//
//class Kirnberger2Tuning(rootNoteIndex: Int = -9,
//                  noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
//                  referenceFrequency: Float = 440f)
//    : TuningRatioBased(
//    circleOfFifths = circleOfFifthsKirnberger2,
//    rootNoteIndex = rootNoteIndex,
//    noteIndexAtReferenceFrequency = noteIndexAtReferenceFrequency,
//    referenceFrequency = referenceFrequency
//)
//
//class Kirnberger3Tuning(rootNoteIndex: Int = -9,
//                  noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
//                  referenceFrequency: Float = 440f)
//    : TuningRatioBased(
//    circleOfFifths = circleOfFifthsKirnberger3,
//    rootNoteIndex = rootNoteIndex,
//    noteIndexAtReferenceFrequency = noteIndexAtReferenceFrequency,
//    referenceFrequency = referenceFrequency
//)
//
//// für ein Dorf, 1724  / für eine kleine Stadt, 1732
//class NeidhardtITuning(rootNoteIndex: Int = -9,
//                  noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
//                  referenceFrequency: Float = 440f)
//    : TuningRatioBased(
//    circleOfFifths = circleOfFifthsNeidhardtI,
//    rootNoteIndex = rootNoteIndex,
//    noteIndexAtReferenceFrequency = noteIndexAtReferenceFrequency,
//    referenceFrequency = referenceFrequency
//)
//
//// für eine kleine Stadt, 1724 / für eine große Stadt, 1732
//class NeidhardtIITuning(rootNoteIndex: Int = -9,
//                 noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
//                 referenceFrequency: Float = 440f)
//    : TuningRatioBased(
//    circleOfFifths = circleOfFifthsNeidhardtII,
//    rootNoteIndex = rootNoteIndex,
//    noteIndexAtReferenceFrequency = noteIndexAtReferenceFrequency,
//    referenceFrequency = referenceFrequency
//)
//
//// für ein Dorf, 1732
//class NeidhardtIIITuning(rootNoteIndex: Int = -9,
//                 noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
//                 referenceFrequency: Float = 440f)
//    : TuningRatioBased(
//    circleOfFifths = circleOfFifthsNeidhardtIII,
//    rootNoteIndex = rootNoteIndex,
//    noteIndexAtReferenceFrequency = noteIndexAtReferenceFrequency,
//    referenceFrequency = referenceFrequency
//)
//
//// für eine große Stadt, 1724
//class NeidhardtIVTuning(rootNoteIndex: Int = -9,
//                 noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
//                 referenceFrequency: Float = 440f)
//    : TuningRatioBased(
//    circleOfFifths = circleOfFifthsNeidhardtIV,
//    rootNoteIndex = rootNoteIndex,
//    noteIndexAtReferenceFrequency = noteIndexAtReferenceFrequency,
//    referenceFrequency = referenceFrequency
//)
