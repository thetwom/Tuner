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

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt

/** Class for computing the accurate frequency at a peak of a spectrum index.
 * @param spec1 First spectrum. Can be null if spec2 is not null.
 * @param spec2 Second spectrum. Can be null if spec1 is not null.
 *   Note that the second spectrum must have thee same size as spec1, but it must be based on a
 *   time signal which is slightly later.
 * @param timeShiftBetweenSpecs: Time shift between spec1 and spec 2,
 *   "start time spec2" - "start time spec1",
 *   in dimension of 1/spec1.df or 1/spec2.df
 *   Only required if spec1 and spec2 are not null.
 *   You can disable the high-accuracy mode also be providing 0f here (then we use f = df * index)
 */
class AccurateSpectrumPeakFrequency(
    private val spec1: FrequencySpectrum?,
    private val spec2: FrequencySpectrum?,
    var timeShiftBetweenSpecs: Float = 0f
) {
    /** Frequency resolution. */
    private val df = spec1?.df ?: spec2?.df ?: 1.0f

    init {
        require(spec1 != null || spec2 != null)
    }

    /** Return frequency at given spectrum index.
     * Is spec1 and spec2 is given, this will not return df*spec_index, but a more accurate
     * approximation at a spectrum peak.
     * @param index Spectrum peak index, where the frequency should be computed.
     * @return  Frequency.
     */
    operator fun get(index: Int): Float {
        val freqLowAccuracy = index * df
        if (spec1 == null || spec2 == null || timeShiftBetweenSpecs == 0f)
            return freqLowAccuracy

        val phase1 = atan2(spec1.imag(index), spec1.real(index))
        val phase2 = atan2(spec2.imag(index), spec2.real(index))

        var dPhase = phase2 - phase1
        if (dPhase < PI)
            dPhase += 2 * PI.toFloat()
        else if (dPhase > PI.toFloat())
            dPhase -= 2 * PI.toFloat()

        val numWaves = (timeShiftBetweenSpecs * freqLowAccuracy - dPhase / (2 * PI.toFloat())).roundToInt()

        return (numWaves + dPhase / (2 * PI.toFloat())) / timeShiftBetweenSpecs
    }
}