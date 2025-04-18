package de.moekadu.tuner

import de.moekadu.tuner.temperaments.FifthModification
import de.moekadu.tuner.temperaments.RationalNumber
import de.moekadu.tuner.temperaments.chainOfFifthsEDO12
import de.moekadu.tuner.temperaments.chainOfFifthsExtendedQuarterCommaMeantone
import de.moekadu.tuner.temperaments.chainOfFifthsFifthCommaMeantone
import de.moekadu.tuner.temperaments.chainOfFifthsKirnberger1
import de.moekadu.tuner.temperaments.chainOfFifthsKirnberger2
import de.moekadu.tuner.temperaments.chainOfFifthsKirnberger3
import de.moekadu.tuner.temperaments.chainOfFifthsNeidhardt1
import de.moekadu.tuner.temperaments.chainOfFifthsNeidhardt2
import de.moekadu.tuner.temperaments.chainOfFifthsNeidhardt3
import de.moekadu.tuner.temperaments.chainOfFifthsNeidhardt4
import de.moekadu.tuner.temperaments.chainOfFifthsPythagorean
import de.moekadu.tuner.temperaments.chainOfFifthsQuarterCommaMeantone
import de.moekadu.tuner.temperaments.chainOfFifthsThirdCommaMeantone
import de.moekadu.tuner.temperaments.chainOfFifthsValotti
import de.moekadu.tuner.temperaments.chainOfFifthsWerckmeisterIII
import de.moekadu.tuner.temperaments.chainOfFifthsWerckmeisterIV
import de.moekadu.tuner.temperaments.chainOfFifthsWerckmeisterV
import de.moekadu.tuner.temperaments.chainOfFifthsYoung2
import de.moekadu.tuner.temperaments.circleOfFifthsKirnberger1
import de.moekadu.tuner.temperaments.circleOfFifthsKirnberger2
import de.moekadu.tuner.temperaments.circleOfFifthsKirnberger3
import de.moekadu.tuner.temperaments.circleOfFifthsNeidhardt1
import de.moekadu.tuner.temperaments.circleOfFifthsNeidhardt2
import de.moekadu.tuner.temperaments.circleOfFifthsNeidhardt3
import de.moekadu.tuner.temperaments.circleOfFifthsNeidhardt4
import de.moekadu.tuner.temperaments.circleOfFifthsPythagorean
import de.moekadu.tuner.temperaments.circleOfFifthsQuarterCommaMeanTone
import de.moekadu.tuner.temperaments.circleOfFifthsValotti
import de.moekadu.tuner.temperaments.circleOfFifthsWerckmeisterIII
import de.moekadu.tuner.temperaments.circleOfFifthsWerckmeisterIV
import de.moekadu.tuner.temperaments.circleOfFifthsWerckmeisterV
import de.moekadu.tuner.temperaments.circleOfFifthsYoung2
import de.moekadu.tuner.temperaments.extendedQuarterCommaMeantone
import de.moekadu.tuner.temperaments.ratioToCents
import org.junit.Assert.assertEquals
import org.junit.Test

class TemperamentChainOfFifthsTest {

    @Test
    fun pythagorean() {
        val chain = chainOfFifthsPythagorean
        val circle = circleOfFifthsPythagorean

        circle.getRatios().zip(chain.getSortedRatios()).forEach {
            println("${it.first}, ${it.second}")
            assertEquals(it.first, it.second, 1e-12)
        }
        val lastFifth = chain.getClosingCircleCorrection()
        println(lastFifth)
    }

    @Test
    fun quarterCommaMeantone() {
        val chain = chainOfFifthsQuarterCommaMeantone
        val circle = circleOfFifthsQuarterCommaMeanTone

        circle.getRatios().zip(chain.getSortedRatios()).forEach {
            println("${it.first}, ${it.second}")
            assertEquals(it.first, it.second, 1e-12)
        }
        val lastFifth = chain.getClosingCircleCorrection()
        println(lastFifth)
    }

    @Test
    fun extendedQuarterCommaMeantoneTest() {
        val chain = chainOfFifthsExtendedQuarterCommaMeantone
        val cents = extendedQuarterCommaMeantone

        chain.getSortedRatios().zip(cents).forEach {
            val centChain = ratioToCents(it.first)
            println("$centChain, ${it.second}")
            //assertEquals(centChain, it.second, 1e-12)
        }
    }

    @Test
    fun testOther() {
        val chain = chainOfFifthsYoung2
        val circle = circleOfFifthsYoung2
        
        circle.getRatios().zip(chain.getSortedRatios()).forEach {
            println("${it.first}, ${it.second}")
            assertEquals(it.first, it.second, 1e-12)
        }
        val lastFifth = chain.getClosingCircleCorrection()
        println(lastFifth)
    }
    @Test
    fun other() {
        val chain = chainOfFifthsFifthCommaMeantone

        chain.getSortedRatios().forEach {
            println("${it}")
        }
        val lastFifth = chain.getClosingCircleCorrection()
        println(lastFifth)
        println(lastFifth.toDouble())
        val ref = FifthModification(RationalNumber(-1, 1), syntonicComma = RationalNumber(11, 5))
        println(ref.toDouble())
    }
}