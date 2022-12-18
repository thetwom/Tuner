package de.moekadu.tuner.notedetection2

import kotlin.math.*

data class Harmonic(
    var harmonicNumber: Int,
    var frequency: Float,
    var spectrumIndex: Int,
    var spectrumAmplitudeSquared: Float
) : Comparable<Harmonic> {
    override fun compareTo(other: Harmonic): Int {
        return harmonicNumber - other.harmonicNumber
    }
}


class Harmonics(maxCapacity: Int) {
    private val harmonics = Array(maxCapacity) {
        Harmonic(-1, 0f, -1, 0f)
    }
    private var isSorted = true

    /** Number of harmonics. */
    var size = 0
        private set

    fun sort() {
        if (!isSorted)
            harmonics.sort(0, size)
    }

    fun addHarmonic(harmonicNumber: Int, frequency: Float, spectrumIndex: Int, spectrumAmplitudeSquared: Float) {
        require(size < harmonics.size)
        harmonics[size].harmonicNumber = harmonicNumber
        harmonics[size].frequency = frequency
        harmonics[size].spectrumIndex = spectrumIndex
        harmonics[size].spectrumAmplitudeSquared = spectrumAmplitudeSquared
        size += 1
        if (size > 1 && isSorted)
            isSorted = (harmonics[size-1].harmonicNumber >= harmonics[size-2].harmonicNumber)
    }

    operator fun get(index: Int): Harmonic {
        require(index < size)
        return harmonics[index]
    }

    fun clear() {
        size = 0
        isSorted = true
    }
}

fun findGlobalMaximumIndex(indexBegin: Int, indexEnd: Int, values: FloatArray): Int {
    // functor for local maximum check
    val isLocalMax = {i: Int, data: FloatArray -> data[i] >= data[i-1] && data[i] >= data[i+1]}
    var globalMaximumIndex = -1
    var globalMaximumValue = 0.0f
    val indexBeginResolved = max(indexBegin, 1)
    val indexEndResolved = min(indexEnd, values.size - 1)

    for (i in indexBeginResolved until indexEndResolved) {
        if (values[i] > globalMaximumValue && isLocalMax(i, values)) {
            globalMaximumIndex = i
            globalMaximumValue = values[i]
        }
    }
    return globalMaximumIndex
}

/** Find a local maxima within specific range.
 * @param values Array within which we are looking for a local maximum.
 * @param center Center index of the maximum search. We take a float, since the index
 *   can also be somewhere between.
 * @param searchRadius Radius around the center in which we search for the maximum.
 * @param minimumFactorOverMean We compute the mean of the search_radius range
 *   around a potentially found maximum (the maximum itself is excluded during
 *   the mean computation). The maximum is only considered as a maximum if it
 *   is higher than minimum_factor_over_mean * average.
 * @param meanRadius: Range for computing minimum_factor_over_mean.
 * @return Index of the maximum or None if no maximum is found.
 */
fun findLocalMaximumIndex(
    values: FloatArray, center: Float, searchRadius: Float, minimumFactorOverMean: Float, meanRadius: Int
): Int {
    val beginIndex = max(ceil(center - searchRadius).toInt(), 1)
    val endIndex = min(floor(center + searchRadius).toInt() + 1, values.size - 1)
    if (endIndex <= beginIndex)
        return -1

    var maximumValue = values[beginIndex]
    var maximumValueIndex = beginIndex
    for (i in beginIndex until endIndex) {
        if (values[i] > maximumValue) {
            maximumValue = values[i]
            maximumValueIndex = i
        }
    }
    if (values[maximumValueIndex - 1] >= maximumValue || values[maximumValueIndex + 1] >= maximumValue)
        return -1

    val meanIndexBegin = max(maximumValueIndex - meanRadius, 0)
    val meanIndexEnd = min(maximumValueIndex + meanRadius + 1, values.size)
    if (meanIndexEnd <= meanIndexBegin)
        return -1

    var average = 0.0f
    if (meanIndexEnd - meanIndexBegin > 1) {
        for (i in meanIndexBegin until meanIndexEnd)
            average += values[i]
        average -= maximumValue
        average /= meanIndexEnd - meanIndexBegin - 1
    }

    if (maximumValue < average * minimumFactorOverMean)
        return -1
    return maximumValueIndex
}


fun findHarmonicsFromSpectrum(
    harmonics: Harmonics,
    frequency: Float,
    frequencyMin: Float,
    frequencyMax: Float,
    spectrum: FrequencySpectrum,
    accurateSpectrumPeakFrequency: AccurateSpectrumPeakFrequency,
    harmonicTolerance: Float = 0.1f,
    minimumFactorOverLocalMean: Float = 5f,
    maxNumFail: Int = 2) {
    harmonics.clear()

    val df = spectrum.df
    val ampSpecSqr = spectrum.amplitudeSpectrumSquared
    // the second condition avoids that when rounding the harmonic later on, that we get the harmonic 0
    val indexBegin = max(ceil(frequencyMin / df).toInt(), ceil(0.5f * frequency / df).toInt())
    val indexEnd = min(ampSpecSqr.size, floor(frequencyMax / df).toInt() + 1)
    val globalMaximumIndex = findGlobalMaximumIndex(indexBegin, indexEnd, ampSpecSqr)

    if (globalMaximumIndex < 1)
        return

    val frequencyOfGlobalMax = accurateSpectrumPeakFrequency[globalMaximumIndex]
    val harmonicOfGlobalMax = (frequencyOfGlobalMax / frequency).roundToInt()

    if (harmonicOfGlobalMax == 0)
        return

    harmonics.addHarmonic(harmonicOfGlobalMax, frequencyOfGlobalMax, globalMaximumIndex, ampSpecSqr[globalMaximumIndex])

    val searchRadius = harmonicTolerance * globalMaximumIndex / harmonicOfGlobalMax
    val meanRadius = max(1, (globalMaximumIndex / (2f * harmonicOfGlobalMax).roundToInt()))

    for (increment in -1 .. 1 step 2) {
        var previouslyFoundHarmonicResult = harmonics[0] // this is the global maximum
        var probableHarmonicNumber = previouslyFoundHarmonicResult.harmonicNumber + increment
        var numFail = 0

        while (numFail < maxNumFail && probableHarmonicNumber > 0) {
            val freq1 = previouslyFoundHarmonicResult.frequency / previouslyFoundHarmonicResult.harmonicNumber
            val freqX = freq1 * probableHarmonicNumber
            val centerIndexFloat = freqX / df

            val maximumIndex = findLocalMaximumIndex(ampSpecSqr, centerIndexFloat, searchRadius, minimumFactorOverLocalMean, meanRadius)
            if (maximumIndex > 0 && maximumIndex != previouslyFoundHarmonicResult.spectrumIndex) {
                val actualFreqX = accurateSpectrumPeakFrequency[maximumIndex]
                if (actualFreqX < frequencyMin || actualFreqX > frequencyMax)
                    break
                harmonics.addHarmonic(probableHarmonicNumber, actualFreqX, maximumIndex, ampSpecSqr[maximumIndex])
                previouslyFoundHarmonicResult = harmonics[harmonics.size - 1]
                numFail = 0
            } else if (freqX < frequencyMin || freqX > frequencyMax) {
                break
            } else {
                ++numFail
            }
            probableHarmonicNumber += increment
        }
    }
}