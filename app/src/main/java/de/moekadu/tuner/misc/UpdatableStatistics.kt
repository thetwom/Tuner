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
package de.moekadu.tuner.misc

import kotlin.math.pow

/** Class for computing online mean and variance.
 * This use weighted incremental algorithm of Welford, see
 *  https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
 * Usage:
 *   1. Create class, and call "update()" with all values which should contribute to
 *      mean and variance.
 *   2. Obtain mean, variance, standard_deviation at any time you need the values, by
 *      the methods "get_mean()", get_variance(), get_standard_deviation().
 */
class UpdatableStatistics {
    /** Current sum of weights. */
    private var weightSum = 0.0f
    /** Intermediate value needed for variance. */
    @Suppress("PrivatePropertyName")
    private var S = 0.0f

    /** Current mean value. */
    var mean = 0.0f
        private set

    /** Current variance. */
    val variance
        get() = if (weightSum == 0.0f) 0.0f else S / weightSum

    /** Current standard deviation. */
    val standardDeviation
        get() = variance.pow(0.5f)

    /** Reset to zero. */
    fun clear() {
        weightSum = 0f
        S = 0f
        mean = 0f
    }

    /** Update the class with an additional value together with a weight.
     * @param value Value which should be considered in mean and variance.
     * @param weight Weight for weighted mean and variance.
     */
    fun update(value: Float, weight: Float) {
        weightSum += weight
        val meanOld = mean
        mean = meanOld + (weight / weightSum) * (value - meanOld)
        S += weight * (value - meanOld) * (value - mean)
    }
}