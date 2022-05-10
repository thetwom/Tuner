package de.moekadu.tuner

import de.moekadu.tuner.temperaments.RationalNumber
import org.junit.Assert.assertEquals
import org.junit.Test

class RationalNumberTest {
    @Test
    fun testGCD() {
        var result = RationalNumber.gcd(100, 5)
        assertEquals(5, result)

        result = RationalNumber.gcd(5, 100)
        assertEquals(5, result)

        result = RationalNumber.gcd(48, 18)
        assertEquals(6, result)

        result = RationalNumber.gcd(18, 48)
        assertEquals(6, result)

        result = RationalNumber.gcd(7, 5)
        assertEquals(1, result)

        result = RationalNumber.gcd(5, 7)
        assertEquals(1, result)

        result = RationalNumber.gcd(-12, 8)
        assertEquals(4, result)
    }

    @Test
    fun reduce() {
        var r = RationalNumber(48, 18).apply { reduce() }
        assertEquals(r.numerator, 8)
        assertEquals(r.denominator, 3)

        r = RationalNumber(18, 48).apply { reduce() }
        assertEquals(r.numerator, 3)
        assertEquals(r.denominator, 8)

        r = RationalNumber(100, 5).apply { reduce() }
        assertEquals(r.numerator, 20)
        assertEquals(r.denominator, 1)

        r = RationalNumber(5, 100).apply { reduce() }
        assertEquals(r.numerator, 1)
        assertEquals(r.denominator, 20)

        r = RationalNumber(7, 5).apply { reduce() }
        assertEquals(r.numerator, 7)
        assertEquals(r.denominator, 5)

        r = RationalNumber(5, 6).apply { reduce() }
        assertEquals(r.numerator, 5)
        assertEquals(r.denominator, 6)
    }

    @Test
    fun plus() {
        var r1 = RationalNumber(5, 8)
        var r2 = RationalNumber(8, 8)
        var r = r1 + r2
        assertEquals(13, r.numerator)
        assertEquals(8, r.denominator)

        r1 = RationalNumber(5, 4)
        r2 = RationalNumber(3, 2)
        r = r1 + r2
        assertEquals(11, r.numerator)
        assertEquals(4, r.denominator)

        r1 = RationalNumber(5, 4)
        r2 = RationalNumber(-3, 2)
        r = r1 + r2
        assertEquals(-1, r.numerator)
        assertEquals(4, r.denominator)

        r1 = RationalNumber(5, 4)
        r2 = RationalNumber(3, -2)
        r = r1 + r2
        assertEquals(-1, r.numerator)
        assertEquals(4, r.denominator)

        r1 = RationalNumber(2, 3)
        r2 = RationalNumber(4, 5)
        r = r1 + r2
        assertEquals(22, r.numerator)
        assertEquals(15, r.denominator)

        r1 = RationalNumber(2, 3)
        r2 = RationalNumber(4, 5)
        val r3 = RationalNumber(2, 30)
        r = r1 + r2 + r3
        assertEquals(23, r.numerator)
        assertEquals(15, r.denominator)

        r = RationalNumber(2, 3)
        r += RationalNumber(5,2)
        assertEquals(19, r.numerator)
        assertEquals(6, r.denominator)
    }

    @Test
    fun plusAssign() {
        val r = RationalNumber(5, 3)
        r += 2
        assertEquals(11, r.numerator)
        assertEquals(3, r.denominator)
    }

    @Test
    fun minus() {
        var r1 = RationalNumber(5, 8)
        var r2 = RationalNumber(8, 8)
        var r = r1 - r2
        assertEquals(-3, r.numerator)
        assertEquals(8, r.denominator)

        r1 = RationalNumber(5, 4)
        r2 = RationalNumber(-3, 2)
        r = r1 - r2
        assertEquals(11, r.numerator)
        assertEquals(4, r.denominator)
    }

    @Test
    fun minusAssign() {
        val r = RationalNumber(5, 3)
        r -= 1
        assertEquals(2, r.numerator)
        assertEquals(3, r.denominator)
    }
    @Test
    fun multiply() {
        var r1 = RationalNumber(5, 8)
        var r2 = RationalNumber(11, 7)
        var r = r1 * r2
        assertEquals(55, r.numerator)
        assertEquals(56, r.denominator)

        r1 = RationalNumber(5, 4)
        r2 = RationalNumber(-3, 2)
        r = r1 * r2
        assertEquals(-15, r.numerator)
        assertEquals(8, r.denominator)
    }

    @Test
    fun div() {
        var r = RationalNumber(5,7)
        r /= 3
        assertEquals(5, r.numerator)
        assertEquals(21, r.denominator)

        r = RationalNumber(6,7)
        r /= 3
        assertEquals(2, r.numerator)
        assertEquals(7, r.denominator)
    }
    @Test
    fun pow() {
        var r1 = RationalNumber(5, 8)
        var r = r1.pow(3)
        assertEquals(5*5*5, r.numerator)
        assertEquals(8*8*8, r.denominator)

        r1 = RationalNumber(5, 4)
        r = r1.pow(0)
        assertEquals(1, r.numerator)
        assertEquals(1, r.denominator)
    }

    @Test
    fun equals() {
        var r1 = RationalNumber(10,6)
        assert(r1 == RationalNumber(5, 3))
        r1 = RationalNumber(-10,6)
        assert(r1 == -RationalNumber(5, 3))
    }

    @Test
    fun zero() {
        var r = RationalNumber(0, 1)
        assertEquals(0, r.numerator)
        assertEquals(1, r.denominator)

        r = RationalNumber(0, -1)
        assertEquals(0, r.numerator)
        assertEquals(1, r.denominator)
    }

    @Test
    fun toDouble() {
        val r = RationalNumber(5,2)
        assertEquals(r.toDouble(), 2.5, 1e-24)
    }
}