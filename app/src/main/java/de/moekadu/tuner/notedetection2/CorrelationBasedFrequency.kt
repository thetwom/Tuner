package de.moekadu.tuner.notedetection2

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Result of frequency detection from auto correlation.
 * @param frequency Detected frequency.
 * @param timeShift Time shift which corresponds to the detected frequency.
 * @param correlationAtTimeShift Correlation value at timeShift.
 */
data class CorrelationBasedFrequency(
    val frequency: Float,
    val timeShift: Float,
    val correlationAtTimeShift: Float
    )

/** Find index for shift of most probable tolerance.
 * @param correlation Array with correlation values.
 * @param subharmonicsTolerance With "subharmonics", we e.g. mean, when for a
 *   periodic signal, we in theory have a period duration of T, then the correlation
 *   will not only peak at T as shift, but also at 2*T, 3*T, ... . These multiples
 *   of the periodic duration we call "subharmonics". Now it is possible, that
 *   the highest peak is not found at T, but at a subharmonic. So, we will check
 *   also peak_index/2, peak_index/3, ... and see if there is a peak, which we
 *   should prefer. The "tolerance", given by this parameter means how far we can
 *   be off the perfect ratio. E.g. for a tolerance of 0.01 we accept a local maximum
 *   in the range of peak/2.01 -- peak/1.99, where the ideal value would be peak/2.0.
 *   From the definition, only values smaller 0.5 make sense since otherwise we are
 *   closer to other ratios.
 * @param subharmonicPeakRatio The concept of subharmonics is described for the argument
 *   subharmonic_tolerance. If now an alternative local maximum is found at a smaller
 *   time shift within the tolerance, we only take it if it is not smaller than
 *   subharmonic_peak_ratio * correlation_value_at_maximum_peak.
 * @return (Index in correlation for the most probable time shift,
 *   peak of polynomial fit at the given index)
 */
fun findCorrelationBasedFrequency(
    correlation: AutoCorrelation,
    subharmonicsTolerance: Float = 0.05f,
    subharmonicPeakRatio: Float = 0.9f
): CorrelationBasedFrequency {

    // functor for local maximum check
    val isLocalMax = {i: Int, data: AutoCorrelation -> data[i] >= data[i-1] && data[i] >= data[i+1]}

    val globalIndexEnd = correlation.size - 1

    // find first negative value (or 1 if there is not negative value)
    val firstNegativeValue = correlation.values.indexOfFirst { it < 0.0f }

    val globalIndexBegin = max(1, firstNegativeValue)

    var globalMaximumIndex = 0
    var maximumValue = Float.NEGATIVE_INFINITY

    // find global maximum
    for (i in globalIndexBegin until globalIndexEnd) {
        if (correlation[i] > maximumValue && isLocalMax(i, correlation)) {
            globalMaximumIndex = i
            maximumValue = correlation[i]
        }
    }
    var (fittedMaximumIndex, fittedPeak) = getPeakOfPolynomialFitArray(
        globalMaximumIndex, correlation.values
    )

    // check if there is a smaller peak, which we should prefer
    val requiredMaximumValue = subharmonicPeakRatio * fittedPeak
    val maximumDivision = ceil(fittedMaximumIndex / globalIndexBegin).toInt()

    for (division in maximumDivision downTo 2) {

        var indexBegin = (ceil(fittedMaximumIndex / (division + subharmonicsTolerance))).toInt()
        indexBegin = max(indexBegin, 1)

        // add 1, since idxEnd is excluded in a range
        var indexEnd = (fittedMaximumIndex / (division - subharmonicsTolerance)).toInt() + 1
        indexEnd = min(indexEnd, correlation.size - 1)

        if (indexEnd - indexBegin < 1) {
            indexBegin = (fittedMaximumIndex / division).roundToInt()
            indexEnd = indexBegin + 1
        }

        // find local maximum in given range
        var localMaximum = Float.NEGATIVE_INFINITY
        var localMaximumIndex = 0
        for (i in indexBegin until indexEnd) {
            if (correlation[i] > localMaximum && isLocalMax(i, correlation)) {
                localMaximum = correlation[i]
                localMaximumIndex = i
            }
        }

        if (localMaximumIndex > 0) {
            val (fittedLocalMaximumIndex, fittedLocalMaximum) = getPeakOfPolynomialFitArray(
                localMaximumIndex, correlation.values
            )
            if (fittedLocalMaximum >= requiredMaximumValue) {
                fittedMaximumIndex = fittedLocalMaximumIndex
                fittedPeak = fittedLocalMaximum
                break
            }
        }
    }
    return CorrelationBasedFrequency(
        1.0f / (fittedMaximumIndex * correlation.dt),
        fittedMaximumIndex * correlation.dt,
        fittedPeak
    )
}
