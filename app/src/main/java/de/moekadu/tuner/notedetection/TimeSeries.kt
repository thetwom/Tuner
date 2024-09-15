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

/** Store a time series of data with constant time spacing.
 * @param size Number of values in time series.
 * @param dt Time difference between two successive samples.
 */
class TimeSeries(val size: Int, val dt: Float) {
    /** Frame position. */
    var framePosition = 0
    /** Values of time series. */
    val values = FloatArray(size)

    /** Access data of given index.
     * @param index Index where to access the time series.
     * @return Value of time series at given index.
     */
    operator fun get(index: Int) = values[index]
}
