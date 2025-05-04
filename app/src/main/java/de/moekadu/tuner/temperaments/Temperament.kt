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
package de.moekadu.tuner.temperaments

import androidx.compose.runtime.Immutable
import de.moekadu.tuner.misc.StringOrResId
import de.moekadu.tuner.notenames.NoteNames
import kotlinx.serialization.Serializable
import kotlin.math.absoluteValue
import kotlin.math.log
import kotlin.math.pow

fun ratioToCents(ratio: Float): Float {
    return (1200.0 * log(ratio.toDouble(), 2.0)).toFloat()
}

fun ratioToCents(ratio: Double): Double {
    return (1200.0 * log(ratio, 2.0))
}

fun centsToFrequency(cent: Double, referenceFrequency: Double): Double {
    return referenceFrequency * centsToRatio(cent)
}

fun frequencyToCents(frequency: Double, referenceFrequency: Double): Double {
    return ratioToCents(frequency / referenceFrequency)
}

fun centsToRatio(cents: Float): Float {
    return (2.0.pow(cents / 1200.0)).toFloat()
}

private fun centsToRatio(cents: Double): Double {
    return (2.0.pow(cents / 1200.0))
}

/** Old temperament class. */
@Serializable
@Immutable
data class Temperament(
    val name: StringOrResId,
    val abbreviation: StringOrResId,
    val description: StringOrResId,
    val cents: DoubleArray,
    val rationalNumbers: Array<RationalNumber>?,
    val circleOfFifths: TemperamentCircleOfFifths?,
    val equalOctaveDivision: Int?,
    val stableId: Long
) {
    fun toNew(noteNames: NoteNames): Temperament3 {
        return if (name.resId != null) {
            // toNew is not unique so we either use edo, or we use the old one based on cents comparison
            val p = predefinedTemperaments()
            val minPredefinedKey = p.minOf { it.stableId }
            if (equalOctaveDivision != null) {
                Temperament3EDO(minPredefinedKey - 1 + equalOctaveDivision + 5, equalOctaveDivision)
            } else {
                p.firstOrNull {
                    centsEqual(cents, it.cents())
                } ?: Temperament3EDO(minPredefinedKey - 1 + 12 + 5, 12)
            }
        } else {
            Temperament3Custom(
                _name = name.value(null),
                _abbreviation = abbreviation.value(null),
                _description = description.value(null),
                cents = cents,
                _rationalNumbers = arrayOf(),
                _noteNames = noteNames.notes,
                stableId = stableId
            )
        }
    }
}

private fun centsEqual(c1: DoubleArray, c2: DoubleArray): Boolean {
    if (c1.size != c2. size)
        return false
    for (i in c1.indices) {
        if ((c1[i] - c2[i]).absoluteValue > 0.01)
            return false
    }
    return true
}
