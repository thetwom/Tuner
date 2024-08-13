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

/** Return type of the maximum finding function.
 * @param time Time of maximum of fitted polynomial.
 * @param value Maximum value of fitted polynomial.
 */
data class MaximumOfPolynomialFit(val time: Float, val value: Float)

/** Get extremum of fit of a parabola going through three equally distributed points.
 * @param valueLeft Value at "timeCenter - dt".
 * @param valueCenter Value at "timeCenter".
 * @param valueRight Value at "timeCenter + dt".
 * @param timeCenter Time of valueCenter.
 * @param dt Time spacing between the values.
 * @return Pair with (time of extremum of fitted parabola, value of extremum)
 */
fun getPeakOfPolynomialFit(
    valueLeft: Float, valueCenter: Float, valueRight: Float, timeCenter: Float, dt: Float
) : MaximumOfPolynomialFit {
    if ((valueLeft == valueCenter && valueRight == valueCenter) || valueLeft - 2 * valueCenter + valueRight == 0f)
        return MaximumOfPolynomialFit(timeCenter, valueCenter)

    val a = 0.5f * (valueLeft + valueRight) - valueCenter
    val b = 0.5f * (valueRight - valueLeft)
    val c = valueCenter
    val tRel = (valueLeft - valueRight) / (2 * (valueLeft - 2 * valueCenter + valueRight))

    return MaximumOfPolynomialFit(dt * tRel + timeCenter, a * tRel.pow(2) + b * tRel + c)
}

/** Find peak index as float based on a polynomial fit.
 * The polynomial will go through "indexCenter - 1", "indexCenter" and
 * "indexCenter + 1". If not all values exist since indexCenter is at
 * the bounds, we will return the value at indexCenter.
 * @param indexCenter Center index in data array of the polynomial fit.
 * @param data Array with values where the indexCenter refers to.
 * @return (index as float of extremum of fitted parabola, value of extremum)
 */
fun getPeakOfPolynomialFitArray(indexCenter: Int, data: FloatArray): MaximumOfPolynomialFit {
    if (indexCenter == 0 || indexCenter == data.size - 1)
        return MaximumOfPolynomialFit(indexCenter.toFloat(), data[indexCenter])
    return getPeakOfPolynomialFit(
        data[indexCenter - 1], data[indexCenter], data[indexCenter + 1], indexCenter.toFloat(),1.0f
    )
}
