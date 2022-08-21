/*
 * Copyright 2020 Michael Moessner
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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.*

data class MostProbablePitchFromSpectrum(val ampSqrSpecIndex: Int?, val rating: Float)
data class MostProbablePitchFromCorrelation(val correlationIndex: Int?, val rating: Float)

/** Compute most probable pitch frequency for some given frequency peaks of a spectrum.
 *
 * Find the pitch matches best for the given frequency peaks. Each frequency which is a harmonic
 * of the pitch increases the probability that this is the true pitch.
 * This function will always include the frequencyPeak at index 0 to be a harmonic of the resulting
 * pitch.
 * The rating in the returned class is maximal if it has a value of 1.0 (all given frequencies
 * match the given pitch) and minimal for a value for 0.0.
 *
 * @param orderedSpectrumPeakIndices Indices in the ampSpec-array where the correlation
 *   has local maxima. The first index has to refer to the highest local maximum, the second index
 *   to the second highest local maximum, ...
 * @param ampSqrSpec Squared amplitude spectrum
 * @param frequency Functions which computes the frequency based on the index in the ampSqrSpec
 *   array.
 * @param hint Hint for a suitable pitch based on the history. If the current results are
 *   uncertain, this hin can help to choose an appropriate value.
 * @param harmonicTolerance Tolerance when the ratio "frequency divided by pitch frequency" makes
 *   the frequency a harmonic. E.g. if the pitch frequency is 400Hz and we now consider the frequency
 *   1250Hz, the ratio is 1250/400=3.125. This would be closest to the 3rd harmonic but we have a
 *   difference of 3.125-3 = 0.125. If the harmonicTolerance is 0.05, then our frequency would be
 *   not treated as a harmonic. If in contrast, we consider the frequency 1210Hz, the ratio would
 *   be 1210/400=3.025, which would be 0.025 off our 3rd harmonic which would then be treated as a
 *   valid harmonic.
 * @param maxHarmonic The peak frequency given by frequencyPeak at index 0 cannot be a higher
 *   harmonic than the given value. E.g. if the frequency is 300 and the maxHarmonic is 3, then
 *   the possible returned frequencies can be 100, 200, 300 but not 75, since for this pitch, our
 *   frequency would be the 4th harmonic.
 * @param harmonicLimit While maxHarmonic referred to frequencyPeak at index 0, which always had to
 *   to contribute to the pitch, the harmonicLimit refers to the frequencyPeaks at index 1 and higher.
 *   These peaks will only increase the probability for a given pitch if they do not exceed
 *   the harmonicLimit.
 * @param hintTolerance Each pitch candidate we compare against the hint. If it matches within the
 *   given tolerance, we increase to probability of the candidate. The tolerance is a relative
 *   tolerance: if abs(1 - hint / candidate) < hintTolerance we increase the probability.
 *   The value should better depends on the half tone ratio. For equal temperament it is 1.059, thus
 *   a value of about 0.025 seems reasonable (1.059 - 1.0 = 0.059 so 0.025 is well below)
 * @param hintAdditionalWeight If the hint is within the hintTolerance, we add this to probability.
 *   Note, that a probability of 1.0 is the maximum probability, so this value should be well below
 *   this value.
 * @return Pitch together with a quality for which the frequencyPeaks fit best.
 */
fun calculateMostProbableSpectrumPitch(
    orderedSpectrumPeakIndices: ArrayList<Int>?, ampSqrSpec: FloatArray, frequency: (Int) -> Float,
    hint: Float?, harmonicTolerance: Float = 0.05f, maxHarmonic: Int = 3, harmonicLimit: Int = 5,
    hintTolerance: Float = 0.025f, hintAdditionalWeight: Float = 0.2f): MostProbablePitchFromSpectrum {

    var weightMax = 0.0f
    var mostProbablePitchFrequency = hint

    if (orderedSpectrumPeakIndices != null && orderedSpectrumPeakIndices.isNotEmpty()) {

        // should we only consider the N first maxima?
        val totalPeakSum = orderedSpectrumPeakIndices.map{ ampSqrSpec[it] }.sum()
        for (harmonicNumber in 1..maxHarmonic) {
            val pitchFrequency = frequency(orderedSpectrumPeakIndices[0]) / harmonicNumber
            var harmonicPeakSum = 0.0f

            for (index in orderedSpectrumPeakIndices) {

                val harmonic = frequency(index) / pitchFrequency
                val harmonicRounded = harmonic.roundToInt()

                if ((harmonic - harmonicRounded).absoluteValue < harmonicTolerance
                    && harmonicRounded <= harmonicLimit) {
                    harmonicPeakSum += ampSqrSpec[index]
                }
            }
            var weight = harmonicPeakSum / totalPeakSum

            if (hint != null && pitchFrequency > 0.0f && abs(1.0f - hint / pitchFrequency) <= hintTolerance)
                weight += hintAdditionalWeight

            if (weight > weightMax) {
                weightMax = weight
                mostProbablePitchFrequency = pitchFrequency
            }
        }
    }

    mostProbablePitchFrequency?.let {f ->
        val df = frequency(1)
        val fI = (f / df).roundToInt()
        val ampSqrSpecIndex = findLocalMaximum(ampSqrSpec, fI, fI-1, fI+1) ?: fI

        return MostProbablePitchFromSpectrum(ampSqrSpecIndex, weightMax)
    }

    return MostProbablePitchFromSpectrum(null, weightMax)
}

/** Compute most probable pitch frequency based on the autocorrelation peaks.
 *
 * Basically the highest autocorrelation peak is a very probable pitch frequencies. However, we
 * assume, that multiples of the frequency at the highest pitch can exist which are the real pitch
 * frequency. E.g. if the true pitch frequency is 400Hz, we will also get correlation peaks at
 * 200Hz and 100Hz, which have a very similar correlation value, for bad signals, the might exceed
 * the value at 400Hz slightly. If for example 100Hz shows the highest peak, we will look if there
 * are similar peaks for 200Hz, 300Hz, 400Hz and if there are, we will prefer the higher
 * frequencies.
 *
 * @param orderedCorrelationPeakIndices Indices in the correlation-array where the correlation
 *   has local maxima. The first index has to refer to the highest local maximum, the second index
 *   to the second highest local maximum, ...
 * @param correlation Autocorrelation array.
 * @param frequency Functions which computes the frequency based on the index in the correlation
 *   array.
 * @param hint Hint for a suitable pitch based on the history. If the current results are
 *   uncertain, this hin can help to choose an appropriate value.
 * @param harmonicTolerance Tolerance for treating a frequency as a multiple of another frequency.
 *   E.g. Lets say we set the tolerance to 0.05. If we have a frequency ratio of 1250Hz/400Hz=3.125,
 *   this would be closest to the integer number 3, but the difference of 3.125-3 = 0.125 exceeds
 *   our tolerance. Hence 1250Hz is not considered a multiple of 400Hz. If in contrast, we consider
 *   the frequency 1210Hz, the ratio would be 1210Hz/400Hz=3.025, which would be 0.025 off our
 *   closest integer number 3 and we would consider 1210Hz as a valid multiple of 400Hz.
 * @param minimumPeakRatio We only consider local maxima as more probable candidates than the highest
 *   maximum, if it is not below this ratio. E.g. if we set the ratio to 0.8 and the the highest
 *   maximum is 10, we would consider a local maximum of 8 still as a valid candidate, but 7 would
 *   be not considered.
 * @param hintTolerance Each pitch candidate we compare against the hint. If it matches within the
 *   given tolerance, we increase to probability of the candidate. The tolerance is a relative
 *   tolerance: if abs(1 - hint / candidate) < hintTolerance we increase the probability.
 *   The value should better depend on the half tone ratio. For equal temperament it is 1.059, thus
 *   a value of about 0.025 seems reasonable (1.059 - 1.0 = 0.059 so 0.025 is well below)
 * @param hintAdditionalWeight If the hint is within the hintTolerance, we add this to probability.
 *   Note, that a probability of 1.0 is the maximum probability, so this value should be well below
 *   this value.
 * @return Pitch together with a quality for which the frequencyPeaks fit best.
 */
fun calculateMostProbableCorrelationPitch(
    orderedCorrelationPeakIndices: ArrayList<Int>?, correlation: FloatArray, frequency: (Int) -> Float,
    hint: Float?, harmonicTolerance: Float = 0.1f, minimumPeakRatio: Float = 0.8f,
    hintTolerance: Float = 0.025f, hintAdditionalWeight: Float = 0.1f): MostProbablePitchFromCorrelation {
    var weight = 0.0f
    var mostProbablePitchFrequency = -1.0f
    var correlationIndex: Int? = null

    if (orderedCorrelationPeakIndices != null && orderedCorrelationPeakIndices.isNotEmpty()) {
        val peak0 = correlation[orderedCorrelationPeakIndices[0]]

        // find most probable candidate
        val freq0 = frequency(orderedCorrelationPeakIndices[0])

        for (index in orderedCorrelationPeakIndices) {
            val freq = frequency(index)
            val freqRatio = freq / freq0
            val freqRatioRounded = freqRatio.roundToInt()
            val peakRatio = correlation[index] / peak0
            if (freq >= freq0
                && (freqRatio - freqRatioRounded).absoluteValue / freqRatioRounded < harmonicTolerance
                && peakRatio >= minimumPeakRatio
                && freq > mostProbablePitchFrequency) {
                weight = peakRatio
                mostProbablePitchFrequency = freq
                correlationIndex = index

                  // if we match the hint, don't go on looking ...
                if (hint != null && freq > 0.0f && abs(1.0f - hint / freq) <= hintTolerance) {
                    weight += hintAdditionalWeight
                    break
                }

            }
        }
    }

    return MostProbablePitchFromCorrelation(correlationIndex, weight)
}

fun increaseAccuracyForSpectrumBasedFrequency(
    ampSqrSpecIndex: Int, workingData: WorkingData, previousWorkingData: WorkingData?): Float {
    val frequency = workingData.frequencyFromSpectrum(ampSqrSpecIndex)

    var frequencyHighAccuracy = if (previousWorkingData != null) {
        val dt = workingData.dt
        val spec1 = previousWorkingData.spectrum
        val spec2 = workingData.spectrum
        val df = workingData.frequencyFromSpectrum(1)

        val frequencyHighAccuracyFromSpectrum = increaseFrequencyAccuracy(
            spec1, spec2, ampSqrSpecIndex, df,
            dt * (workingData.framePosition - previousWorkingData.framePosition)
        )
        if (frequencyHighAccuracyFromSpectrum > frequency - df && frequencyHighAccuracyFromSpectrum < frequency + df)
            frequencyHighAccuracyFromSpectrum
        else
            null
    }
    else {
        null
    }

    // if we were not able to increase the accuracy based on the spectrum, try increasing it
    // using the correlation results
    if (frequencyHighAccuracy == null) {
        // find a local maximum in the correlation within tight bounds
        val frequencyBoundLower = workingData.frequencyFromSpectrum(ampSqrSpecIndex - 1)
        val frequencyBoundUpper = workingData.frequencyFromSpectrum(ampSqrSpecIndex + 1)
        val correlationIndexInitial = (1.0f / frequency / workingData.dt).roundToInt()
        val correlationIndexLower = ceil(1.0f / frequencyBoundUpper / workingData.dt).toInt()
        val correlationIndexUpper = floor(1.0f / frequencyBoundLower / workingData.dt).toInt()
        val correlationMaximum = findLocalMaximum(workingData.correlation, correlationIndexInitial,
            correlationIndexLower, correlationIndexUpper)

        // if we have a local maximum, increase the accuracy by polynomial fitting
        if (correlationMaximum != null) {
            val timeShiftHighAccuracy = increaseTimeShiftAccuracy(
                workingData.correlation, correlationMaximum, workingData.dt)
            val frequencyHighAccuracyFromCorrelation = 1.0f / timeShiftHighAccuracy
            if (frequencyHighAccuracyFromCorrelation > frequencyBoundLower
                && frequencyHighAccuracyFromCorrelation < frequencyBoundUpper)
                    frequencyHighAccuracy = frequencyHighAccuracyFromCorrelation
        }
    }
    return frequencyHighAccuracy ?: frequency
}

fun increaseAccuracyForCorrelationBasedFrequency(
    correlationIndex: Int, workingData: WorkingData, previousWorkingData: WorkingData?): Float {

    val dt = workingData.dt

    val timeShiftHighAccuracy = increaseTimeShiftAccuracy(
        workingData.correlation, correlationIndex, dt
    )
    var frequencyHighAccuracy = 1.0f / timeShiftHighAccuracy

    // use spectra to increase accuracy if available and if it is within tight ranges.
    if (previousWorkingData != null) {
        val dfSpec = workingData.frequencyFromSpectrum(1)
        val frequencyIndexSpecInitial = (frequencyHighAccuracy / dfSpec).roundToInt()

        // compute frequency range within which there has to be a local maximum
        val timeShiftUpperBound = workingData.timeShiftFromCorrelation(correlationIndex + 1) //timeShiftHighAccuracy + 1.5f * dt
        val frequencyLowerBound = 1.0 / timeShiftUpperBound
        val frequencyIndexSpecLowerBound = floor(frequencyLowerBound / dfSpec).toInt()

        val timeShiftLowerBound = workingData.timeShiftFromCorrelation(correlationIndex - 1) //timeShiftHighAccuracy - 1.5f * dt
        val frequencyUpperBound = 1.0 / timeShiftLowerBound
        val frequencyIndexSpecUpperBound = ceil(frequencyUpperBound / dfSpec).toInt()

        val frequencyIndexSpec = findLocalMaximum(
            workingData.ampSqrSpec, frequencyIndexSpecInitial,
            min(frequencyIndexSpecInitial - 1, frequencyIndexSpecLowerBound),
            max(frequencyIndexSpecInitial + 1, frequencyIndexSpecUpperBound)
        )

        if (frequencyIndexSpec != null) {
            val spec1 = previousWorkingData.spectrum
            val spec2 = workingData.spectrum

            val frequencyHighAccuracySpec = increaseFrequencyAccuracy(
                spec1, spec2, frequencyIndexSpec, dfSpec,
                dt * (workingData.framePosition - previousWorkingData.framePosition)
            )

            if (frequencyHighAccuracySpec > frequencyLowerBound
                && frequencyHighAccuracySpec < frequencyUpperBound
                && frequencyHighAccuracySpec > frequencyIndexSpec * (dfSpec - 1)
                && frequencyHighAccuracySpec < frequencyIndexSpec * (dfSpec + 1)
            ) {
                // normally we would also compare against frequencyLowerBound an
                //  frequencyUpperBound, but it seems as the frequency based improvement can
                //  even yield improved results outside the correlation based bounds.
//            if (frequencyHighAccuracySpec > frequencyIndexSpec * (dfSpec - 1)
//                && frequencyHighAccuracySpec < frequencyIndexSpec * (dfSpec + 1)
//            ) {
                frequencyHighAccuracy = frequencyHighAccuracySpec
            }
        }
    }
    return frequencyHighAccuracy
}

/** Check if a given index is a local maximum in a values array.
 * A local maximum is defined here that the index must have a left and right neighbor, and that
 * the value of the neighbors are smaller the value at the index.
 *
 * @param index Index which should be checked for local maxima
 * @param values Value array where we check for the local maximum
 * @return True if the left and right neighbor are smaller than the value at the given index, else
 *   false.
 */
fun isLocalMaximum(index: Int, values: FloatArray): Boolean {
    return when {
        index <= 0 -> false
        index >= values.size - 1 -> false
        else -> values[index] > values[index - 1]  && values[index] > values[index + 1]
    }
}

/** Find the local maximum in an array, starting the search from an initial index.
 * @param values Array where we search for the local maximum.
 * @param initialIndex Index in values-array where we start the search.
 * @param lowerIndex Lowest index which will be included for the search
 * @param upperIndex Upper index which will be included for the search
 * @return Local maximum index (lowerIndex <= return value <= upperIndex)
 *   or null if no local maximum present.
 */
fun findLocalMaximum(values: FloatArray, initialIndex: Int, lowerIndex: Int, upperIndex: Int): Int? {
    if (isLocalMaximum(initialIndex, values))
        return initialIndex

    // search local maximum at i > initialIndex
    var peakIndexRight: Int? = null
    for (i in initialIndex + 1 .. min(values.size - 2, upperIndex)) {
        if (isLocalMaximum(i, values))
        {
            peakIndexRight = i
            break
        }
    }

    // search local maximum at i < initialIndex
    var peakIndexLeft: Int? = null
    for (i in initialIndex - 1 downTo max(1, lowerIndex)) {
        if (isLocalMaximum(i, values))
        {
            peakIndexLeft = i
            break
        }
    }

    if (peakIndexLeft != null && peakIndexRight != null) {
        // return peak index closest to the initial index, else return the index at the higher peak
        return when {
            initialIndex - peakIndexLeft < peakIndexRight - initialIndex -> peakIndexLeft
            initialIndex - peakIndexLeft > peakIndexRight - initialIndex -> peakIndexRight
            values[peakIndexLeft] > values[peakIndexRight] -> peakIndexLeft
            else -> peakIndexRight
        }
    }
    else if (peakIndexLeft != null) {
        return peakIndexLeft
    }
    else if (peakIndexRight != null) {
        return peakIndexRight
    }

    return null
}

/** Class which stores chooses a pitch based on precomputed spectra, correlations and maxima. */
class PitchChooserAndAccuracyIncreaser {
    /** Results from the previous call, this helps to increase the accuracy by evaluation phase data. */
    private var _lastWorkingData: WorkingData? = null
    /** Mutex for setting the last tuner results. */
    private val tunerResultsMutex = Mutex()

    /** Choose pitch and increase accuracy
     * @param nextWorkingData Latest precomputed results.
     * @param hint Hint for a suitable pitch based on the history. If the current results are
     *   uncertain, this hin can help to choose an appropriate value.
     * @return Current pitch frequency.
     */
    suspend fun run(nextWorkingData: WorkingData, hint: Float?): Float? {

        var pitchFrequency: Float? = null

        withContext(Dispatchers.Default) {

            var lastWorkingData: WorkingData? = tunerResultsMutex.withLock { _lastWorkingData }
            // set last tuner results to null if it is not compatible to the current results
            if (lastWorkingData != null && (
                        nextWorkingData.framePosition - lastWorkingData.framePosition <= 0
                                || lastWorkingData.dt != nextWorkingData.dt
                                || lastWorkingData.sampleRate != nextWorkingData.sampleRate)) {
                lastWorkingData = null
            }

            val mostProbableValueFromSpec = calculateMostProbableSpectrumPitch(
                nextWorkingData.specMaximaIndices,
                nextWorkingData.ampSqrSpec,
                nextWorkingData.frequencyFromSpectrum,
                hint)
            val mostProbableValueFromCorrelation = calculateMostProbableCorrelationPitch(
                nextWorkingData.correlationMaximaIndices,
                nextWorkingData.correlation,
                nextWorkingData.frequencyFromCorrelation,
                hint)

            if (mostProbableValueFromSpec.ampSqrSpecIndex != null
                && mostProbableValueFromSpec.rating > mostProbableValueFromCorrelation.rating) {
                pitchFrequency = increaseAccuracyForSpectrumBasedFrequency(mostProbableValueFromSpec.ampSqrSpecIndex,
                    nextWorkingData, lastWorkingData)
            }
            else if (mostProbableValueFromCorrelation.correlationIndex != null
                && mostProbableValueFromCorrelation.rating >= mostProbableValueFromSpec.rating) {
                pitchFrequency = increaseAccuracyForCorrelationBasedFrequency(mostProbableValueFromCorrelation.correlationIndex,
                    nextWorkingData, lastWorkingData)
            }

            tunerResultsMutex.withLock {
                _lastWorkingData = nextWorkingData
            }
        }
//        Log.v("TestRecordFlow", "PitchChooserAndAccuracyIncreaser.run: pitchFrequency=$pitchFrequency")
        return pitchFrequency
    }
}
