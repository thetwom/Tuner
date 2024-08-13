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

import kotlin.math.absoluteValue

data class RationalNumber(var numerator: Int, var denominator: Int) {
    companion object {
        fun gcd(a: Int, b: Int): Int {
            return when {
                a < 0 || b < 0 -> gcd(a.absoluteValue, b.absoluteValue)
                b == 0 -> a
                b > a -> gcd(b, a)
                else -> gcd(b, a % b)
            }
        }
    }

    val isZero get() = numerator == 0

    init {
        reduce()
    }

    fun toDouble(): Double {
        return numerator.toDouble() / denominator.toDouble()
    }

    fun toFloat(): Float {
        return this.toDouble().toFloat()
    }

    fun reduce() {
        val g = gcd(numerator, denominator)
        numerator /= g
        denominator /= g
        if (denominator < 0) {
            numerator *= -1
            denominator *= -1
        }
    }

    operator fun plus(other: RationalNumber): RationalNumber {
        val result = RationalNumber(numerator * other.denominator + denominator * other.numerator, denominator * other.denominator)
        result.reduce()
        return result
    }

    operator fun plusAssign(other: Int) {
        numerator += other * denominator
        reduce()
    }

//    operator fun plusAssign(other: RationalNumber) {
//        val newNumerator = numerator * other.denominator + denominator * other.numerator
//        val newDenominator = denominator * other.denominator
//        numerator = newNumerator
//        denominator = newDenominator
//        reduce()
//    }

    operator fun minus(other: RationalNumber): RationalNumber {
        val result = RationalNumber(numerator * other.denominator - denominator * other.numerator, denominator * other.denominator)
        result.reduce()
        return result
    }

    operator fun minusAssign(other: Int) {
        numerator -= other * denominator
        reduce()
    }

    operator fun unaryMinus(): RationalNumber {
        return RationalNumber(-numerator, denominator)
    }

//    operator fun minusAssign(other: RationalNumber) {
//        val newNumerator = numerator * other.denominator - denominator * other.numerator
//        val newDenominator = denominator * other.denominator
//        numerator = newNumerator
//        denominator = newDenominator
//        reduce()
//    }

    operator fun times(other: RationalNumber): RationalNumber {
        val result = RationalNumber(numerator * other.numerator, denominator * other.denominator)
        result.reduce()
        return result
    }

    operator fun times(other: Int): RationalNumber {
        val result = RationalNumber(numerator * other, denominator)
        result.reduce()
        return result
    }

    operator fun div(other: Int): RationalNumber {
        val result = RationalNumber(numerator, denominator * other)
        result.reduce()
        return result
    }

    fun pow(exponent: Int): RationalNumber {
        var n = 1
        var d = 1
        for (i in 0 until exponent) {
            n *= numerator
            d *= denominator
        }
        val result = RationalNumber(n, d)
        result.reduce()
        return result
    }

    fun setZero() {
        numerator = 0
        denominator = 1
    }
}