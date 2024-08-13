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

import kotlin.math.pow

data class FifthModification(
    var pythagoreanComma: RationalNumber = RationalNumber(0, 1),
    var syntonicComma: RationalNumber = RationalNumber(0, 1),
    var schisma: RationalNumber = RationalNumber(0, 1)
) {
    companion object {
        val pythagoreanCommaRatio = RationalNumber(531441, 524288)
        val syntonicCommaRatio = RationalNumber(81, 80)
        val schismaRatio = RationalNumber(32805, 32768)
    }
    // 1 pythagoreanComma = 1 syntonicComma + 1 schisma
    // 1 syntonicComma = 1 pythagoreanComma - 1 schisma
    // 1 schisma = 1 pythagoreanComma - 1 syntonicComma

    init {
        simplify()
    }

    fun toDouble(): Double {
        return (pythagoreanCommaRatio.toDouble().pow(pythagoreanComma.toDouble())
                * syntonicCommaRatio.toDouble().pow(syntonicComma.toDouble())
                * schismaRatio.toDouble().pow(schisma.toDouble()))
    }

    operator fun plus(other: FifthModification): FifthModification {
        val result = FifthModification(
            pythagoreanComma + other.pythagoreanComma,
            syntonicComma + other.syntonicComma,
            schisma + other.schisma)
        result.simplify()
        return result
    }

    operator fun minus(other: FifthModification): FifthModification {
        val result = FifthModification(
            pythagoreanComma - other.pythagoreanComma,
            syntonicComma - other.syntonicComma,
            schisma - other.schisma)
        result.simplify()
        return result
    }

    operator fun unaryMinus(): FifthModification {
        return FifthModification(-pythagoreanComma, -syntonicComma, -schisma)
    }

    private fun simplify() {
        when {
            syntonicComma == schisma -> {
                pythagoreanComma += schisma
                schisma.setZero()
                syntonicComma.setZero()
            }
            pythagoreanComma == -schisma -> {
                syntonicComma += pythagoreanComma
                schisma.setZero()
                pythagoreanComma.setZero()
            }
            pythagoreanComma == -syntonicComma -> {
                schisma += pythagoreanComma
                pythagoreanComma.setZero()
                syntonicComma.setZero()
            }
        }
    }
}