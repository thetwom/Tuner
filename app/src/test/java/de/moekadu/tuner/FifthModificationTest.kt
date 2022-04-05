package de.moekadu.tuner

import org.junit.Assert.assertEquals
import org.junit.Test

class FifthModificationTest {

    @Test
    fun plus() {
        var f = FifthModification(pythagoreanComma = RationalNumber(-1, 6))
        for (i in 0 until 5)
            f += FifthModification(pythagoreanComma = RationalNumber(-1, 6))
        assertEquals(f.pythagoreanComma.numerator, -1)
        assertEquals(f.pythagoreanComma.denominator, 1)
        assertEquals(f.syntonicComma.numerator, 0)
        assertEquals(f.schisma.numerator, 0)
    }

    @Test
    fun simplify() {
        var f = FifthModification(syntonicComma = RationalNumber(-1, 3))
        f += FifthModification(syntonicComma = RationalNumber(-1, 3))
        f += FifthModification(syntonicComma = RationalNumber(-1, 3))
        assertEquals(f.syntonicComma.numerator, -1)
        assertEquals(f.syntonicComma.denominator, 1)

        f += FifthModification(schisma = RationalNumber(-1, 1))

        // one syntonicComma + one schisma gives an pythagoreanComma
        assertEquals(f.pythagoreanComma.numerator, -1)
        assertEquals(f.pythagoreanComma.denominator, 1)
        assertEquals(f.syntonicComma.numerator, 0)
        assertEquals(f.schisma.numerator, 0)

        // one pythagoreanComma - one schisma gives a syntonicComma
        f = FifthModification(pythagoreanComma = RationalNumber(1, 1))
        f += FifthModification(schisma = RationalNumber(-1, 1))
        assertEquals(f.syntonicComma.numerator, 1)
        assertEquals(f.syntonicComma.denominator, 1)
        assertEquals(f.schisma.numerator, 0)
        assertEquals(f.pythagoreanComma.numerator, 0)

        // one pythagoreanComma - one syntonicComma gives a schisma
        f = FifthModification(pythagoreanComma = RationalNumber(1, 1))
        f += FifthModification(syntonicComma = RationalNumber(-1, 1))
        assertEquals(f.schisma.numerator, 1)
        assertEquals(f.schisma.denominator, 1)
        assertEquals(f.pythagoreanComma.numerator, 0)
        assertEquals(f.syntonicComma.numerator, 0)

        f = FifthModification(pythagoreanComma = RationalNumber(2, 3))
        f += FifthModification(syntonicComma = RationalNumber(-2, 3))
        assertEquals(f.schisma.numerator, 2)
        assertEquals(f.schisma.denominator, 3)
        assertEquals(f.pythagoreanComma.numerator, 0)
        assertEquals(f.syntonicComma.numerator, 0)
    }

    @Test
    fun toDouble() {
        val r = FifthModification()
        assertEquals(1.0, r.toDouble(), 1e-32)
    }
}