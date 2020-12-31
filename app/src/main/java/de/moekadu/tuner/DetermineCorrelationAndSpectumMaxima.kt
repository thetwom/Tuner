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

import kotlin.math.ceil
import kotlin.math.min

fun determineCorrelationMaxima(correlation: FloatArray, minimumFrequency: Float, maximumFrequency: Float, dt: Float) : ArrayList<Int> {
    val localMaxima: ArrayList<Int>

    val indexMinimumFrequency =
        min(correlation.size, ceil((1.0f / minimumFrequency) / dt).toInt())
    val indexMaximumFrequency =
        min(correlation.size, ceil((1.0f / maximumFrequency) / dt).toInt())

    // index of maximum frequency is the smaller index ..., we have to start from zero, otherwise its hard to interpret the first maximum
    localMaxima = findMaximaOfPositiveSections(correlation, 0, indexMinimumFrequency)
    localMaxima.removeAll { it < indexMaximumFrequency }

    return localMaxima
}

fun determineSpectrumMaxima(ampSqrSpec: FloatArray, minimumFrequency: Float, maximumFrequency: Float, dt: Float, localMaximaSNR: Float) : ArrayList<Int> {
    val localMaxima: ArrayList<Int>

    /// Minimum index in transformed spectrum, which should be considered
    val startIndex = RealFFT.closestFrequencyIndex(minimumFrequency, ampSqrSpec.size - 1, dt)
    /// Maximum index in transformed spectrum, which should be considered (endIndex is included in range)
    val endIndex = RealFFT.closestFrequencyIndex(maximumFrequency, ampSqrSpec.size - 1, dt)
    localMaxima = findLocalMaxima(ampSqrSpec, localMaximaSNR, startIndex, endIndex + 1)

    return localMaxima
}
