package de.moekadu.tuner.notedetection

import kotlin.math.pow
import kotlin.math.sqrt

interface AcousticWeighting {
    /** Apply A-weighting to a amplitude at a given frequency.
     * @param amplitude Amplitude before weighting.
     * @param freq Frequency of amplitude.
     * @return A-weighted amplitude.
     */
    fun applyToAmplitude(amplitude: Float, freq: Float): Float
}

class AcousticZeroWeighting : AcousticWeighting {
    override fun applyToAmplitude(amplitude: Float, freq: Float): Float {
        return amplitude
    }
}
class AcousticAWeighting : AcousticWeighting {
    override fun applyToAmplitude(amplitude: Float, freq: Float): Float {
        return amplitude * getWeightingFactor(freq)
    }

    private val weightingNonNormalized1000 = getWeightingFactorNonNormalized(1000f)

    /** Compute weighting factor.
     * The amplitude at a specific frequency must multiplied with this value
     * to obtain the weighted value.
     * @param freq Frequency
     */
    private fun getWeightingFactor(freq: Float): Float {
        return getWeightingFactorNonNormalized(freq) / weightingNonNormalized1000
    }

    /** Compute weighting factor without normalization.
     * In order to normalize the weighting factor, it must be divided by the weighting factor
     * at 1000Hz.
     * @param freq Frequency
     * @return Weighting factor (not normalized)
     */
    private fun getWeightingFactorNonNormalized(freq: Float): Float {
        val fsqr = freq.pow(2)
        return ((12194 * fsqr).pow(2) /
                ((fsqr + 20.6f.pow(2)) * sqrt((fsqr + 107.7f.pow(2)) * (fsqr + 737.9f.pow(2))) * (fsqr + 12194f.pow(2)))
                )
    }
}

class AcousticCWeighting : AcousticWeighting {
    override fun applyToAmplitude(amplitude: Float, freq: Float): Float {
        return amplitude * getWeightingFactor(freq)
    }

    private val weightingNonNormalized1000 = getWeightingFactorNonNormalized(1000f)

    /** Compute weighting factor.
     * The amplitude at a specific frequency must multiplied with this value
     * to obtain the weighted value.
     * @param freq Frequency
     */
    private fun getWeightingFactor(freq: Float): Float {
        return getWeightingFactorNonNormalized(freq) / weightingNonNormalized1000
    }

    /** Compute weighting factor without normalization.
     * In order to normalize the weighting factor, it must be divided by the weighting factor
     * at 1000Hz.
     * @param freq Frequency
     * @return Weighting factor (not normalized)
     */
    private fun getWeightingFactorNonNormalized(freq: Float): Float {
        val fsqr = freq.pow(2)
        return ((12194 * fsqr) /
                ((fsqr + 20.6f.pow(2)) * (fsqr + 12194f.pow(2)))
                )
    }
}
