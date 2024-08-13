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

import de.moekadu.tuner.misc.UpdatableStatistics
import kotlin.math.sqrt

class HarmonicStatistics {
    val frequency get() = frequencyStatistics.mean
    val frequencyVariance get() = frequencyStatistics.variance
    val frequencyStandardDeviation get() = frequencyStatistics.standardDeviation

    private val frequencyStatistics = UpdatableStatistics()

    fun clear() {
        frequencyStatistics.clear()
    }

    fun evaluate(harmonics: Harmonics, weighting: AcousticWeighting) {
        clear()

        for (i in 0 until harmonics.size) {
            val harmonic = harmonics[i]
            val amplitude = sqrt(harmonic.spectrumAmplitudeSquared)
            val weight = weighting.applyToAmplitude(amplitude, harmonic.frequency)
            val frequencyBase = harmonic.frequency / harmonic.harmonicNumber
            frequencyStatistics.update(frequencyBase, weight)
        }
    }
}
