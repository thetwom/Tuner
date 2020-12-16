package de.moekadu.tuner

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PeakRating(val frequency: Float, val rating: Float)

/** Compute most probable pitch frequency for some given frequency peaks of a spectrum.
 *
 * TODO: Add a "hint"-parameter which contains a value from the history. By this value we can
 *   rate pitch frequencies higher if it better matches the history.
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
 * @return Pitch together with a quality for wihch the frequencyPeaks fit best.
 */
fun calculateMostProbableSpectrumPitch(
    orderedSpectrumPeakIndices: ArrayList<Int>?, ampSqrSpec: FloatArray, frequency: (Int) -> Float,
    harmonicTolerance: Float = 0.05f, maxHarmonic: Int = 3, harmonicLimit: Int = 5): PeakRating {

    var weightMax = 0.0f
    var mostProbablePitchFrequency = -1f // here we should put in the hint frequency

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
            val weight = harmonicPeakSum / totalPeakSum
            if (weight > weightMax) { // introduce a hinted weight, which is introduced if our pitch frequency is close the hint frequency (but how much, and how close?)
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
 *   to the secon hightest local maximum, ...
 * @param correlation Autocorrelation array.
 * @param frequency Functions which computes the frequency based on the index in the correlation
 *   array.
 * @param harmonicTolerance Tolerance for treating a frequency as a mulitple of another frequency.
 *   E.g. Lets say we set the tolerance to 0.05. If we have a frequency ratio of 1250Hz/400Hz=3.125,
 *   this would be closest to the integer number 3, but the difference of 3.125-3 = 0.125 exceeds
 *   our tolerance. Hence 1250Hz is not considered a multiple of 400Hz. If in contrast, we consider
 *   the frequency 1210Hz, the ratio would be 1210Hz/400Hz=3.025, which would be 0.025 off our
 *   closest integer number 3 and we would consider 1210Hz as a valid multiple of 400Hz.
 * @param minimumPeakRatio We only consider local maxima as more probable candites than the highest
 *   maximum, if it is not below this ratio. E.g. if we set teh ratio t0 0.8 and the the highest
 *   maximum is 10, we would consider a local maximum of 8 still as a valid candidate, but 7 would
 *   be not considered.
 */
fun calculateMostProbableCorrelationPitch(
    orderedCorrelationPeakIndices: ArrayList<Int>?, correlation: FloatArray, frequency: (Int) -> Float,
    harmonicTolerance: Float = 0.05f, minimumPeakRatio: Float=0.8f): PeakRating {
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
            }
        }
    }

    return PeakRating(mostProbablePitchFrequency, weight)
}

/// Choose a resulting pitch frequency based on spectrum und correlation peaks.
/**
 * @param tunerResults Results from the preprocessing
 * @return Highest rated frequency.
 */
fun chooseResultingFrequency(tunerResults: TunerResults): Float {

    val mostProbableValueFromSpec = calculateMostProbableSpectrumPitch(
        tunerResults.specMaximaIndices,
        tunerResults.ampSqrSpec,
        tunerResults.frequencyFromSpectrum)
    val mostProbableValueFromCorrelation = calculateMostProbableCorrelationPitch(
        tunerResults.correlationMaximaIndices,
        tunerResults.correlation,
        tunerResults.frequencyFromCorrelation)
//    Log.v("TestRecordFlow","chooseResultingFrequency: mostPropableValueFromSpec=$mostProbableValueFromSpec, mostProbableValueFromCorrelation=$mostProbableValueFromCorrelation")
    return if (mostProbableValueFromSpec.rating > mostProbableValueFromCorrelation.rating) {
        //Log.v("TestRecordFlow", "Postprocessing: chooseResultingFrequency: using spectrum value")
        mostProbableValueFromSpec.frequency
    }
    else {
        //Log.v("TestRecordFlow", "Postprocessing: chooseResultingFrequency: using correlation value")
        mostProbableValueFromCorrelation.frequency
    }
}

/// Find the local maximum in an array, starting the serach from an initial index.
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

class PitchChooserAndAccuracyIncreaser {
    private var _lastTunerResults: TunerResults? = null
    private val tunerResultsMutex = Mutex()

    suspend fun run(nextTunerResults: TunerResults): Float {

        val pitchFrequency: Float

        withContext(Dispatchers.Main) {

            val lastTunerResults = tunerResultsMutex.withLock { _lastTunerResults }

            // this is the frequency computed from the shift at the maximum correlation, however, this the frequency
            // resolution is not high enough for our needs.
            val approximateFrequency = chooseResultingFrequency(nextTunerResults)
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
