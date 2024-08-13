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
