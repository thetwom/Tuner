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

private fun centsToRatio(cents: Float): Float {
    return (2.0.pow(cents / 1200.0)).toFloat()
}

enum class TuningState {InTune, TooLow, TooHigh, Unknown}

fun checkTuning(frequency: Float, targetFrequency: Float, toleranceInCents: Float): TuningState {
    if (frequency < 0f || targetFrequency < 0f || toleranceInCents < 0f)
        return TuningState.Unknown

    val ratio = centsToRatio(toleranceInCents)
    val lowerFrequencyBound = targetFrequency / ratio
    val upperFrequencyBound = targetFrequency * ratio

    return if (frequency < lowerFrequencyBound)
        TuningState.TooLow
        else if (frequency > upperFrequencyBound)
        TuningState.TooHigh
        else
        TuningState.InTune
}
