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

/** Store auto correlation result.
 * @param size Number of values in auto correlation.
 * @param dt Time shift between two values in auto correlation
 */
class AutoCorrelation(
    val size: Int,
    val dt: Float
) {
    /** Array with time shift for each auto correlation entry. */
    val times = FloatArray(size) { it * dt }
    /** Correlation values. */
    val values = FloatArray(size)

    /** Values, normalized to range 0 to 1. */
    val plotValuesNormalized = FloatArray(size)
    /** The zero position in plotValuesNormalized. */
    var plotValuesNormalizedZero = 0f

    /** Obtain correlation value at given index.
     * @param index Index where correlation value is needed.
     * @return Correlation value
     */
    operator fun get(index: Int) = values[index]
}