/*
* Copyright 2024 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.moekadu.tuner.notedetection

import kotlin.math.*

/** Details about a single harmonics.
 * @param harmonicNumber Harmonic number.
 * @param frequency Frequency of harmonic.
 * @param spectrumIndex Index in original spectrum, where the harmonic is located.
 * @param spectrumAmplitudeSquared Value of squared amplitude spectrum at given index.
 */
data class Harmonic(
    var harmonicNumber: Int,
    var frequency: Float,
    var spectrumIndex: Int,
    var spectrumAmplitudeSquared: Float
) : Comparable<Harmonic> {
    /** Define ordering for harmonics which is based on harmonic number. */
    override fun compareTo(other: Harmonic): Int {
        return harmonicNumber - other.harmonicNumber
    }
}

/** Container, storing several harmonics.
 * @param maxCapacity Maximum number of harmonics, which can be stored here.
 */
class Harmonics(maxCapacity: Int) {
    /** Array, where the harmonic are stored. */
    private val harmonics = Array(maxCapacity) {
        Harmonic(-1, 0f, -1, 0f)
    }
    private var isSorted = true

    /** Number of harmonics. */
    var size = 0
        private set

    private var tmpHarmonics: Harmonics? = null
    private var isTmp = false // flag which allows checking that we don' create tmp of tmp ...

    /** Sort the harmonics according to the harmonic number. */
    fun sort() {
        if (!isSorted)
            harmonics.sort(0, size)
    }

    /** Add a new harmonic to the container.
     * @param harmonicNumber Harmonic number.
     * @param frequency Frequency of harmonic.
     * @param spectrumIndex Index in spectrum of the harmonic.
     * @param spectrumAmplitudeSquared Value of squared amplitude spectrum at given index.
     */
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

    /** Get harmonic at given container index.
     * @param index Container index.
     * @return Harmonic at given index.
     */
    operator fun get(index: Int): Harmonic {
        require(index < size)
        return harmonics[index]
    }

    /** Clear container. */
    fun clear() {
        size = 0
        isSorted = true
    }

    /** Adopt harmonics from other class. */
    fun adopt(other: Harmonics){
        if (other === this)
            return
        clear()
        for (i in 0 until other.size) {
            addHarmonic(
                other[i].harmonicNumber,
                other[i].frequency,
                other[i].spectrumIndex,
                other[i].spectrumAmplitudeSquared
            )
        }
    }

    /** Return temporary harmonics class.
     * This is needed by algorithms like findBestMatchingHarmonics()
     */
    fun getTemporary(): Harmonics {
        assert(!isTmp)
        val tmp = tmpHarmonics ?: Harmonics(harmonics.size)
        tmp.isTmp = true
        tmpHarmonics = tmp
        return tmp
    }
}

/** Find global maximum in an array between two given indices.
 *
 * Besides of getting the maximum value in-between the two indices, we ensure that it is also
 * a local maximum. Which in the ends needs to make sure, that the boundaries are not the maximum.
 * @param indexBegin Index where search is started (included).
 * @param indexEnd Index where search is ends (excluded).
 * @param values Array with values where we search the maximum.
 * return Index of maximum found or -1 if no maximum is found.
 */
fun findGlobalMaximumIndex(indexBegin: Int, indexEnd: Int, values: FloatArray): Int {
    // functor for local maximum check
    val isLocalMax = {i: Int, data: FloatArray -> data[i] >= data[i-1] && data[i] >= data[i+1]}
    var globalMaximumIndex = -1
    var globalMaximumValue = Float.NEGATIVE_INFINITY
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
 * @param meanRadius Range for computing minimumFactorOverMean. Mean is computed around the found
 *   maximum (note the given center).
 * @param threshold A value can only be a peak, if it is higher than this given value.
 * @return Index of the maximum or None if no maximum is found.
 */
fun findLocalMaximumIndex(
    values: FloatArray,
    center: Float,
    searchRadius: Float,
    minimumFactorOverMean: Float,
    meanRadius: Int,
    threshold: Float = 0f
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

    if (maximumValue < max(average * minimumFactorOverMean, threshold))
        return -1
    return maximumValueIndex
}

/** Check if all harmonics have a common divisor.
 *
 * Currently we only check:
 * - Only one harmonic, which is larger than 1
 * - divisible by 2 (or of course multiples of 2)
 * - divisible by 3 (or of course multiples of 3)
 * @return true if there is a common divisor, else false.
 */
fun Harmonics.hasCommonDivisors(): Boolean {
    return ((size == 1 && this[0].harmonicNumber > 1) ||
            allHarmonicsDividableBy(2) ||
            allHarmonicsDividableBy(3)
            )
}

/** Check if all harmonics can be divided by a given factor.
 * @param factor Factor.
 * @return true if all harmonics can be divided by the factor, else false.
 */
private fun Harmonics.allHarmonicsDividableBy(factor: Int): Boolean {
    for (i in 0 until size)
        if (this[i].harmonicNumber % factor > 0)
            return false
    return true
}

/** Compute a rating value for the harmonics.
 * The actual value is of no importance, it is just, the larger the value, the better.
 * @return Rating value.
 */
private fun Harmonics.rating(): Float {
    if (size == 0)
        return 0f
    val EXPONENT = 0.4f
    val ADDITIONAL_CONTRIBUTION = 0.05f

    var harmonicSum = 0.0f
    var frequencySum = 0.0f
    var maximumAmplitude = 0.0f

    for (i in 0 until size) {
        val h = this[i]
        harmonicSum += h.spectrumAmplitudeSquared.pow(0.25f)
        maximumAmplitude = max(maximumAmplitude, h.spectrumAmplitudeSquared)
        frequencySum += h.frequency / h.harmonicNumber

    }
    harmonicSum += this.size * maximumAmplitude.pow(0.25f) * ADDITIONAL_CONTRIBUTION
    return (frequencySum / size).pow(EXPONENT) * harmonicSum
}

/** Find a good starting point of a spectrum peak for the harmonic search.
 * @param frequencyBase Base frequency.
 * @param frequencyMax: Highest frequency which is considered.
 * @param spectrum Frequency spectrum where we find the peak.
 * @param globalMaximumIndex Global maximum index in spectrum within the valid frequency bounds.
 * @param accurateSpectrumPeakFrequency Object for improving the accuracy beyond the resolution
 *   given in the spectrum.
 * @param relativePeakThreshold A local maximum is only considered a real peak if its value is
 *   higher than the global maximum times this value.
 * @return Harmonic of suitable peak or null if none is found.
*/
fun findSuitableSpectrumPeak(
    frequencyBase: Float,
    frequencyMax: Float,
    spectrum: FrequencySpectrum,
    globalMaximumIndex: Int,
    accurateSpectrumPeakFrequency: AccurateSpectrumPeakFrequency,
    relativePeakThreshold: Float = 5e-3f
): Harmonic? {
    val harmonicTolerance = 0.1f
    val maxHarmonicTolerance = 0.2f
    val minimumFactorOverMean = 3f

    val meanRadiusMax = max(1, (frequencyBase / (2.0 * spectrum.df)).roundToInt())
    val meanRadius = min(meanRadiusMax, 5) // TODO: check if using e.g. 10 is better, in python reference,the spectrum was not zeropadded, so df is smaller here

    val frequencyOfGlobalMax = accurateSpectrumPeakFrequency[globalMaximumIndex]
    val harmonicOfGlobalMax = (frequencyOfGlobalMax / frequencyBase).roundToInt()

    val harmonicErrorMaxHarmonic
            = (harmonicOfGlobalMax - frequencyOfGlobalMax / frequencyBase).absoluteValue

    if (harmonicErrorMaxHarmonic < harmonicTolerance && harmonicOfGlobalMax > 0) {
        return Harmonic(
            harmonicOfGlobalMax,
            frequencyOfGlobalMax,
            globalMaximumIndex,
            spectrum.amplitudeSpectrumSquared[globalMaximumIndex]
        )
    }

    val searchRadius = maxHarmonicTolerance * frequencyBase / spectrum.df + 1
    val threshold = spectrum.amplitudeSpectrumSquared[globalMaximumIndex] * relativePeakThreshold

    val maxHarmonic = ceil(globalMaximumIndex * spectrum.df * 1.2 / frequencyBase)
        .toInt()
        .coerceAtLeast(2)
    var smallestError = 1f
    var harmonicOfSmallestError: Harmonic? = null

    for (harmonic in 1 until maxHarmonic) {
        val freq = frequencyBase * harmonic
        if (freq > frequencyMax)
            break

        val maximumIndex = findLocalMaximumIndex(
            values = spectrum.amplitudeSpectrumSquared,
            center = freq / spectrum.df,
            searchRadius = searchRadius,
            minimumFactorOverMean = minimumFactorOverMean,
            meanRadius = meanRadius,
            threshold = threshold
        )

        if (maximumIndex > 0) {
            val freqHarmonic = accurateSpectrumPeakFrequency[maximumIndex]
            val harmonicError = (freq - freqHarmonic).absoluteValue / frequencyBase
            val h = Harmonic(
                harmonic,
                freqHarmonic,
                maximumIndex,
                spectrum.amplitudeSpectrumSquared[maximumIndex]
            )

            if (harmonicError < harmonicTolerance) {
                return h
            } else if (harmonicError < smallestError) {
                smallestError = harmonicError
                harmonicOfSmallestError = h
            }
        }
    }
    return if (harmonicOfGlobalMax > 0) {
        Harmonic(
            harmonicOfGlobalMax,
            frequencyOfGlobalMax,
            globalMaximumIndex,
            spectrum.amplitudeSpectrumSquared[globalMaximumIndex]
        )
    } else  {
        harmonicOfSmallestError
    }
}


/** Extract harmonics from a spectrum
 *
 * @param harmonics Harmonics class to which we add the single harmonics.
 * @param frequency Fundamental frequency. This can e.g. found by auto correlation.
 * @param frequencyMin Lowest frequency which is considered.
 * @param frequencyMax Highest frequency which is considered.
 * @param spectrum Frequency spectrum on which we find the harmonics.
 * @param accurateSpectrumPeakFrequency Object for improving the accuracy beyond the resolution
 *   given in the spectrum.
 * @param harmonicTolerance Allowed tolerance for a spectrum peak being allowed to be a harmonic.
 *   This is given as relative value of the base frequency. So harmonicTolerance * baseFrequency
 *   is the frequency search radius.
 * @param minimumFactorOverLocalMean  We compute the mean of the search_radius range around a
 *   potentially found maximum (the maximum itself is excluded during the mean computation). The
 *   maximum is only considered as a maximum if it is higher than
 *     minimum_factor_over_mean * average.
 * @param maxNumFail We start searching from the harmonic of the peak in the frequency searching
 *   for harmonics. If we don't find successive expected harmonics for the given number, we stop
 *   searching for more harmonics.
 */
fun findHarmonicsFromSpectrum(
    harmonics: Harmonics,
    frequency: Float,
    frequencyMin: Float,
    frequencyMax: Float,
    spectrum: FrequencySpectrum,
    accurateSpectrumPeakFrequency: AccurateSpectrumPeakFrequency,
    harmonicTolerance: Float = 0.1f,
    minimumFactorOverLocalMean: Float = 5f,
    maxNumFail: Int = 2
) {
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
    val meanRadius = max(1, (globalMaximumIndex / (2f * harmonicOfGlobalMax)).roundToInt())

    for (increment in -1 .. 1 step 2) { // this just means, do it once for -1 and once for 1
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

/** Extract harmonics from a spectrum.
 *
 * @param initialHarmonic Harmonic, where we start the search for further harmonics.
 * @param frequencyMin Lowest frequency which is considered.
 * @param frequencyMax Highest frequency which is considered.
 * @param spectrum Frequency spectrum on which we find the harmonics.
 * @param globalMaximumIndex Global maximum index in spectrum within the valid frequency bounds.
 * @param accurateSpectrumPeakFrequency Object for improving the accuracy beyond the resolution
 *   given in the spectrum.
 * @param harmonicTolerance Allowed tolerance for a spectrum peak being allowed to be a harmonic.
 *   This is given as relative value of the base frequency. So harmonicTolerance * baseFrequency
 *   is the frequency search radius.
 * @param minimumFactorOverLocalMean  We compute the mean of the search_radius range around a
 *   potentially found maximum (the maximum itself is excluded during the mean computation). The
 *   maximum is only considered as a maximum if it is higher than
 *     minimum_factor_over_mean * average.
 * @param maxNumFail We start searching from the harmonic of the peak in the frequency searching
 *   for harmonics. If we don't find successive expected harmonics for the given number, we stop
 *   searching for more harmonics.
 * @param relativePeakThreshold A local maximum is only considered a real peak if its value is
 *   higher than the global maximum times this value.
 */
fun Harmonics.findHarmonicsFromSpectrum2(
    initialHarmonic: Harmonic,
    frequencyMin: Float,
    frequencyMax: Float,
    spectrum: FrequencySpectrum,
    globalMaximumIndex: Int,
    accurateSpectrumPeakFrequency: AccurateSpectrumPeakFrequency,
    harmonicTolerance: Float = 0.1f,
    minimumFactorOverLocalMean: Float = 5f,
    maxNumFail: Int = 2,
    relativePeakThreshold: Float = 5e-3f
) {
    val MEAN_RADIUS_LIMIT = 15 // consider higher value like 30, since in python reference, the spectrum was not zeropadded
    clear()

    val df = spectrum.df
    val ampSpecSqr = spectrum.amplitudeSpectrumSquared
    val frequencyBase = initialHarmonic.frequency / initialHarmonic.harmonicNumber

    if (globalMaximumIndex < 1)
        return

    addHarmonic(
        initialHarmonic.harmonicNumber,
        initialHarmonic.frequency,
        initialHarmonic.spectrumIndex,
        initialHarmonic.spectrumAmplitudeSquared
    )

    val searchRadius = (
            harmonicTolerance * initialHarmonic.spectrumIndex / initialHarmonic.harmonicNumber + 1
            )
    val meanRadiusMax = max(1, (frequencyBase / (2 * df)).roundToInt())
    val meanRadius = min(meanRadiusMax, MEAN_RADIUS_LIMIT)

    val threshold = ampSpecSqr[globalMaximumIndex] * relativePeakThreshold
    val predictor = HarmonicPredictor().apply {
        add(initialHarmonic.harmonicNumber, initialHarmonic.frequency)
    }

    for (increment in -1 .. 1 step 2) { // this just means, do it once for -1 and once for 1
        var previouslyFoundHarmonicResult = initialHarmonic
        var probableHarmonicNumber = previouslyFoundHarmonicResult.harmonicNumber + increment
        var numFail = 0

        while (numFail < maxNumFail && probableHarmonicNumber > 0) {
            val freqX = predictor.predict(probableHarmonicNumber)
            val centerIndexFloat = freqX / df

            val maximumIndex = findLocalMaximumIndex(
                ampSpecSqr,
                centerIndexFloat,
                searchRadius,
                minimumFactorOverLocalMean,
                meanRadius,
                threshold
            )
            if (maximumIndex > 0 && maximumIndex != previouslyFoundHarmonicResult.spectrumIndex) {
                val actualFreqX = accurateSpectrumPeakFrequency[maximumIndex]
                if (actualFreqX < frequencyMin || actualFreqX > frequencyMax)
                    break
                val lowerBound = freqX - harmonicTolerance * frequencyBase
                val upperBound = freqX + harmonicTolerance * frequencyBase

                if (actualFreqX in lowerBound .. upperBound) {
                    addHarmonic(
                        probableHarmonicNumber,
                        actualFreqX,
                        maximumIndex,
                        ampSpecSqr[maximumIndex]
                    )
                    predictor.add(probableHarmonicNumber, actualFreqX)
                    previouslyFoundHarmonicResult = this[size - 1]
                    numFail = 0
                } else {
                    ++numFail
                }
            } else if (freqX < frequencyMin || freqX > frequencyMax) {
                break
            } else {
                ++numFail
            }
            probableHarmonicNumber += increment
        }
    }
}

fun Harmonics.findBestMatchingHarmonics(
    correlationBasedFrequency: CorrelationBasedFrequency,
    correlation: AutoCorrelation,
    frequencyMin: Float,
    frequencyMax: Float,
    spectrum: FrequencySpectrum,
    accurateSpectrumPeakFrequency: AccurateSpectrumPeakFrequency,
    harmonicTolerance: Float = 0.1f,
    minimumFactorOverLocalMean: Float = 5f,
    maxNumFail: Int = 2,
    relativePeakThreshold: Float = 5e-3f
) {
    val LOWEST_SUBHARMONIC = 6
    val HIGHEST_HIGHER_HARMONIC = 2
    // correlation at a base frequency must have at least CORRELATION_PEAK_FACTOR * initialPeak
    // the value to be valid.
    val CORRELATION_PEAK_FACTOR = 0.3f

    var bestHarmonics = this
    bestHarmonics.clear()
    var otherHarmonics = getTemporary()

    val globalMaximumIndex = findGlobalMaximumIndex(
        indexBegin = ceil(frequencyMin / spectrum.df).toInt(),
        indexEnd = min(spectrum.size, floor(frequencyMax / spectrum.df).toInt() + 1),
        values = spectrum.amplitudeSpectrumSquared
    )
    if (globalMaximumIndex <= 0)
        return

    var bestRating = 0f

    val probableBaseFrequency = correlationBasedFrequency.frequency

    val subharmonic = min(LOWEST_SUBHARMONIC, ceil(probableBaseFrequency / frequencyMin).toInt() - 1)
    val higherHarmonic = min(HIGHEST_HIGHER_HARMONIC, ceil(frequencyMax / probableBaseFrequency).toInt() - 1)

    for (harmonicVariant in -(subharmonic-1) .. higherHarmonic) {
        val freqBase = if (harmonicVariant < 0)
            probableBaseFrequency / (-harmonicVariant + 1) //  i.e. / 2, / 3, ...
        else
            probableBaseFrequency * (harmonicVariant + 1) // i.e.  * 1, * 2, ...

        val closestIndex = (1f / (freqBase * correlation.dt)).roundToInt()
        val correlationInitialPeak = correlationBasedFrequency.correlationAtTimeShift

        if (closestIndex < correlation.size &&
            correlation[closestIndex] > CORRELATION_PEAK_FACTOR * correlationInitialPeak) {
            val initialHarmonic = findSuitableSpectrumPeak(
                frequencyBase = freqBase,
                frequencyMax = frequencyMax,
                spectrum = spectrum,
                globalMaximumIndex = globalMaximumIndex,
                accurateSpectrumPeakFrequency = accurateSpectrumPeakFrequency,
                relativePeakThreshold = relativePeakThreshold
            )

            if (initialHarmonic != null) {
                otherHarmonics.findHarmonicsFromSpectrum2(
                    initialHarmonic = initialHarmonic,
                    frequencyMin = frequencyMin,
                    frequencyMax = frequencyMax,
                    spectrum = spectrum,
                    globalMaximumIndex = globalMaximumIndex,
                    accurateSpectrumPeakFrequency = accurateSpectrumPeakFrequency,
                    harmonicTolerance = harmonicTolerance,
                    minimumFactorOverLocalMean = minimumFactorOverLocalMean,
                    maxNumFail = maxNumFail,
                    relativePeakThreshold = relativePeakThreshold
                )

                val rating = otherHarmonics.rating()

                if (rating > bestRating && !otherHarmonics.hasCommonDivisors()) {
                    val tmp = bestHarmonics
                    bestHarmonics = otherHarmonics
                    otherHarmonics = tmp
                    bestRating = rating
                }
            }
        }
    }
    adopt(bestHarmonics)
}

fun computeEnergyContentOfHarmonicsInSignalRelative(harmonics: Harmonics, ampspecSqr: FloatArray, radius: Int = 1): Float {
    val totalEnergy = ampspecSqr.sumOf { it.toDouble() }
    var harmonicEnergy = 0.0
    for ( i in 0 until harmonics.size) {
        val startIndex = max(0, harmonics[i].spectrumIndex - radius)
        val endIndex = min(ampspecSqr.size, harmonics[i].spectrumIndex + radius + 1)
        for (j in startIndex until endIndex)
            harmonicEnergy += ampspecSqr[j]
    }
    return (harmonicEnergy / totalEnergy).toFloat()
}

fun computeEnergyContentOfHarmonicsInSignalAbsolute(harmonics: Harmonics, ampspecSqr: FloatArray, radius: Int = 1): Float {
    var harmonicEnergy = 0.0f
    for ( i in 0 until harmonics.size) {
        val startIndex = max(0, harmonics[i].spectrumIndex - radius)
        val endIndex = min(ampspecSqr.size, harmonics[i].spectrumIndex + radius + 1)
        for (j in startIndex until endIndex)
            harmonicEnergy += ampspecSqr[j]
    }
    return harmonicEnergy
}