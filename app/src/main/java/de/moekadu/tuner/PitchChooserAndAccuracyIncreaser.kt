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

package de.moekadu.tuner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.*

data class PeakRating(val frequency: Float, val rating: Float)

/** Compute most probable pitch frequency for some given frequency peaks of a spectrum.
 *
 * Find the pitch matches best for the given frequency peaks. Each frequency which is a harmonic
 * of the pitch increases the probability that this is the true pitch.
 * This function will always include frequencyPeaks[0].frequency to be a harmonic of the resulting
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
 * @param maxHarmonic The peak frequency given by frequencyPeaks[0].frequency cannot be a higher
 *   harmonic than the given value. E.g. if the frequency is 300 and the maxHarmonic is 3, then
 *   the possible returned frequencies can be 100, 200, 300 but not 75, since for this pitch, our
 *   frequency would be the 4th harmonic.
 * @param harmonicLimit While maxHarmonic referred to frequencyPeaks[0], which always had to
 *   to contribute to the pitch, the harmonicLimit refers to the frequencyPeaks[1] and higher.
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
    hintTolerance: Float = 0.025f, hintAdditionalWeight: Float = 0.1f): PeakRating {

    var weightMax = 0.0f
    var mostProbablePitchFrequency = hint ?: -1f // here we should put in the hint frequency

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
    return PeakRating(mostProbablePitchFrequency, weightMax)
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
 *   The value should better depends on the half tone ratio. For equal temperament it is 1.059, thus
 *   a value of about 0.025 seems reasonable (1.059 - 1.0 = 0.059 so 0.025 is well below)
 * @param hintAdditionalWeight If the hint is within the hintTolerance, we add this to probability.
 *   Note, that a probability of 1.0 is the maximum probability, so this value should be well below
 *   this value.
 * @return Pitch together with a quality for which the frequencyPeaks fit best.
 */
fun calculateMostProbableCorrelationPitch(
    orderedCorrelationPeakIndices: ArrayList<Int>?, correlation: FloatArray, frequency: (Int) -> Float,
    hint: Float?, harmonicTolerance: Float = 0.05f, minimumPeakRatio: Float=0.8f,
    hintTolerance: Float = 0.025f, hintAdditionalWeight: Float = 0.1f): PeakRating {
    var weight = 0.0f
    var mostProbablePitchFrequency = -1.0f

    if (orderedCorrelationPeakIndices != null && orderedCorrelationPeakIndices.isNotEmpty()) {
        val peak0 = correlation[orderedCorrelationPeakIndices[0]]
        val freq0 = frequency(orderedCorrelationPeakIndices[0])

        for (index in orderedCorrelationPeakIndices) {
            val freq = frequency(index)
            val freqRatio = freq / freq0
            val freqRatioRounded = freqRatio.roundToInt()
            val peakRatio = correlation[index] / peak0
            if (freq >= freq0
                && (freqRatio - freqRatioRounded).absoluteValue < harmonicTolerance
                && peakRatio >= minimumPeakRatio
                && freq > mostProbablePitchFrequency) {
                weight = peakRatio
                mostProbablePitchFrequency = freq

                // if we match the hint, don't go on looking ...
                if (hint != null && freq > 0.0f && abs(1.0f - hint / freq) <= hintTolerance) {
                    weight += hintAdditionalWeight
                    break
                }
            }
        }
    }

    return PeakRating(mostProbablePitchFrequency, weight)
}

/// Choose a resulting pitch frequency based on spectrum und correlation peaks.
/**
 * @param tunerResults Latest precomputed results.
 * @param hint Hint for a suitable pitch based on the history. If the current results are
 *   uncertain, this hin can help to choose an appropriate value.
 * @return Highest rated frequency.
 */
fun chooseResultingFrequency(tunerResults: TunerResults, hint: Float?): Float {

    val mostProbableValueFromSpec = calculateMostProbableSpectrumPitch(
        tunerResults.specMaximaIndices,
        tunerResults.ampSqrSpec,
        tunerResults.frequencyFromSpectrum,
        hint)
    val mostProbableValueFromCorrelation = calculateMostProbableCorrelationPitch(
        tunerResults.correlationMaximaIndices,
        tunerResults.correlation,
        tunerResults.frequencyFromCorrelation,
        hint)
//    Log.v("TestRecordFlow","chooseResultingFrequency: mostProbableValueFromSpec=$mostProbableValueFromSpec, mostProbableValueFromCorrelation=$mostProbableValueFromCorrelation")
    return if (mostProbableValueFromSpec.rating > mostProbableValueFromCorrelation.rating) {
        //Log.v("TestRecordFlow", "Postprocessing: chooseResultingFrequency: using spectrum value")
        mostProbableValueFromSpec.frequency
    }
    else {
        //Log.v("TestRecordFlow", "Postprocessing: chooseResultingFrequency: using correlation value")
        mostProbableValueFromCorrelation.frequency
    }
}

/// Find the local maximum in an array, starting the search from an initial index.
/**
 * @param values Array where we search for values.
 * @param initialIndex Index in values-array where we start the search
 * @param searchRange Number of neighboring elements which are considered in the search.
 * @return Local maximum index in values.
 */
fun findLocalMaximum(values: FloatArray, initialIndex: Int, searchRange: Int): Int {
    // search local maximum at i > initialIndex
    var maxRight = values[initialIndex]
    var peakIndexRight = initialIndex
    for (i in initialIndex + 1 until min(values.size, initialIndex + searchRange + 1)) {
        if (values[i] > maxRight) {
            maxRight = values[i]
            peakIndexRight = i
        }
        else if (values[i] > maxRight) {
            break
        }
    }

    // search local maximum at i < initialIndex
    var maxLeft = values[initialIndex]
    var peakIndexLeft = initialIndex
    for (i in initialIndex - 1 downTo max(0, initialIndex - searchRange)) {
        if (values[i] > maxLeft) {
            maxLeft = values[i]
            peakIndexLeft = i
        }
        else if (values[i] > maxLeft) {
            break
        }
    }

    return if (maxRight > maxLeft) peakIndexRight else peakIndexLeft
}

/// Class which stores chooses a pitch based on precomputed spectra, correlations and maxima
class PitchChooserAndAccuracyIncreaser {
    /// Results from the previous call, this helps to increase the accuracy by evaluation phase data.
    private var _lastTunerResults: TunerResults? = null
    /// Mutex for setting the last tuner results.
    private val tunerResultsMutex = Mutex()

    /// Choose pitch and increase accuracy
    /**
     * @param nextTunerResults Latest precomputed results.
     * @param hint Hint for a suitable pitch based on the history. If the current results are
     *   uncertain, this hin can help to choose an appropriate value.
     * @return Current pitch frequency.
     */
    suspend fun run(nextTunerResults: TunerResults, hint: Float?): Float {

        val pitchFrequency: Float

        withContext(Dispatchers.Main) {

            val lastTunerResults = tunerResultsMutex.withLock { _lastTunerResults }

            // this is the frequency which we choose base on the latest results, however, the frequency
            // resolution is not high enough for our needs.
            val approximateFrequency = chooseResultingFrequency(nextTunerResults, hint)
//            Log.v("TestRecordFlow", "PitchChooserAndAccuracyIncreaser.run: approximateFrequency=$approximateFrequency")
            pitchFrequency =
                if (approximateFrequency < 0) {
                    0f // no frequency available
                }
                else if (lastTunerResults != null
                    && nextTunerResults.framePosition - lastTunerResults.framePosition > 0
                    && lastTunerResults.dt == nextTunerResults.dt
                    && lastTunerResults.sampleRate == nextTunerResults.sampleRate
                ) {

                    val dt = nextTunerResults.dt
                    val spec1 = lastTunerResults.spectrum
                    val spec2 = nextTunerResults.spectrum
                    val df = nextTunerResults.frequencyFromSpectrum(1)

                    val freqIndex = (approximateFrequency / df).roundToInt()
                    val freqIndexLocalMax = findLocalMaximum(nextTunerResults.ampSqrSpec,
                        freqIndex,2)

                    increaseFrequencyAccuracy(
                        spec1, spec2, freqIndexLocalMax, df,
                        dt * (nextTunerResults.framePosition - lastTunerResults.framePosition)
                    )
                } else {
                    approximateFrequency
                }

            tunerResultsMutex.withLock {
                _lastTunerResults = nextTunerResults
            }
        }
//        Log.v("TestRecordFlow", "PitchChooserAndAccuracyIncreaser.run: pitchFrequency=$pitchFrequency")
        return pitchFrequency
    }
}
