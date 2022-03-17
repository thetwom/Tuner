package de.moekadu.tuner

import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

class PythagoreanTuning(rootNoteIndex: Int = -9,
                        noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
                        referenceFrequency: Float = 440f)
    : TuningRatioBased(
    ratios = floatArrayOf(
        1.0f, // c
        256f / 243f, // cis
        9.0f / 8.0f, //d
        32.0f / 27.0f, // dis
        81.0f / 64.0f, // e
        4.0f / 3.0f,  // f
        729f / 512f, // fis    or 1024 / 729
        3.0f / 2.0f, // g
        128f / 81f, // gis
        27.0f / 16.0f, // a
        16.0f / 9.0f,  // ais
        243.0f / 128.0f,  // b
        2f // c
    ),
    rootNoteIndex = rootNoteIndex,
    noteIndexAtReferenceFrequency = noteIndexAtReferenceFrequency,
    referenceFrequency = referenceFrequency
)

open class TuningRatioBased(private val ratios: FloatArray,
                            rootNoteIndex: Int = -9, // the frequency ratios are based on this index
                            noteIndexAtReferenceFrequency: Int = 0, // 0 for 12-tone is a4
                            referenceFrequency: Float = 440f)
        : TuningFrequencies {

    private val noteIndexMin = -50
    private val noteIndexMax = 50

    private var frequencies = FloatArray(noteIndexMax - noteIndexMin + 1)

    init {
        val numNotesPerOctave = ratios.size - 1
        val octaveRatio = ratios.last() / ratios.first()
        var ratioIndexReference = (noteIndexAtReferenceFrequency - rootNoteIndex) % numNotesPerOctave
        if (ratioIndexReference < 0)
            ratioIndexReference += (ratios.size - 1)

        val ratiosRef = FloatArray(numNotesPerOctave) {
            if (it + ratioIndexReference < ratios.size)
                ratios[it + ratioIndexReference] / ratios[ratioIndexReference]
            else
                octaveRatio * ratios[it + ratioIndexReference - numNotesPerOctave] / ratios[ratioIndexReference]
        }

        for (i in noteIndexMin .. noteIndexMax) {
            val arrayIndex = i - noteIndexMin
            val numOctaves = if (i >= noteIndexAtReferenceFrequency)
                (i - noteIndexAtReferenceFrequency) / numNotesPerOctave
            else
                (i - noteIndexAtReferenceFrequency) / numNotesPerOctave - 1
            val ratioRefIndex = i - noteIndexAtReferenceFrequency - numOctaves * numNotesPerOctave
            frequencies[arrayIndex] = referenceFrequency * octaveRatio.pow(numOctaves) * ratiosRef[ratioRefIndex]
        }
    }

    override fun getToneIndex(frequency: Float): Float {
        val closestToneIndex = getClosestToneIndex(frequency)
        val j = closestToneIndex - noteIndexMin
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
        val index = frequencies.binarySearch(frequency)
        if (index >= 0)
            return index + noteIndexMin
        val indexAfter = -index - 1
        val indexBefore = indexAfter - 1

        // Avoid accessing frequency array out of bounds
        if (indexBefore < 0)
            return 0
        if (indexAfter >= frequencies.size)
            frequencies.size - 1

        // the sorting is as follows:
        // frequency at indexBefore < frequency < frequency at indexAfter
        // we want to check what is closest based on log scale
        return if (log10(frequency / frequencies[indexBefore]) < log10(frequencies[indexAfter] / frequency))
            indexBefore
        else
            indexAfter
    }

    override fun getNoteFrequency(noteIndex: Int): Float {
        return frequencies[noteIndex - noteIndexMin]
    }

    override fun getNoteFrequency(noteIndex: Float): Float {
        // noteIndex = closestNoteIndex +/- log10(frequencies[j] / frequency) / log10(frequencies[j] / frequencies[j +/- 1])
        // (noteIndex - closestNoteIndex) * log10(frequencies[j] / frequencies[j +/- 1]) = +/- log10(frequencies[j] / frequency)
        // 10**( +/- (noteIndex - closestNoteIndex) * log10(frequencies[j] / frequencies[j +/- 1])) = frequencies[j] / frequency
        // frequency = frequencies[i] * 10**( -/+ (noteIndex - closestNoteIndex) * log10(frequencies[j] / frequencies[j +/- 1]))
        // frequency = frequencies[i] * (frequencies[j] / frequencies[j +/- 1])**( -/+ (noteIndex - closestNoteIndex)
        val noteIndexLower = floor(noteIndex - noteIndexMin).toInt()
        val arrayIndexLower = noteIndexLower - noteIndexMin
        return when {
            arrayIndexLower < 0 -> {
                frequencies[0] * (frequencies[1] / frequencies[0]).pow(noteIndex - noteIndexMin)
            }
            arrayIndexLower >= frequencies.size - 1 -> {
                frequencies[frequencies.size - 1] * (frequencies[frequencies.size - 1] / frequencies[frequencies.size - 2]).pow(
                    noteIndex - (frequencies.size - 1)
                )
            }
            else -> {
                frequencies[arrayIndexLower] * (frequencies[arrayIndexLower + 1] / frequencies[arrayIndexLower]).pow(
                    noteIndex - arrayIndexLower
                )
            }
        }
    }
}