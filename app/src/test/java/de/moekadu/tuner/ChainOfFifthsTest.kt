package de.moekadu.tuner

import de.moekadu.tuner.temperaments.FifthModification
import de.moekadu.tuner.temperaments.RationalNumber
import de.moekadu.tuner.temperaments.circleOfFifthsPythagorean
import de.moekadu.tuner.temperaments.circleOfFifthsQuarterCommaMeanTone
import de.moekadu.tuner.temperaments.circleOfFifthsYoung2
import de.moekadu.tuner.temperaments.extendedQuarterCommaMeantone
import de.moekadu.tuner.temperaments.predefinedTemperamentExtendedQuarterCommaMeanTone
import de.moekadu.tuner.temperaments.predefinedTemperamentFifthCommaMeanTone
import de.moekadu.tuner.temperaments.predefinedTemperamentPythagorean
import de.moekadu.tuner.temperaments.predefinedTemperamentQuarterCommaMeanTone
import de.moekadu.tuner.temperaments.predefinedTemperamentYoung2
import de.moekadu.tuner.temperaments.ratioToCents
import org.junit.Assert.assertEquals
import org.junit.Test

class ChainOfFifthsTest {

    @Test
    fun pythagorean() {
        val chain = predefinedTemperamentPythagorean(0L).chainOfFifths()
        val circle = circleOfFifthsPythagorean

        circle.getRatios().zip(chain.getSortedRatios()).forEach {
            println("${it.first}, ${it.second}")
            assertEquals(it.first, it.second, 1e-12)
        }
        val lastFifth = chain.getClosingCircleCorrection()
        println(lastFifth)
    }

    @Test
    fun quarterCommaMeanTone() {
        val chain = predefinedTemperamentQuarterCommaMeanTone(0L).chainOfFifths()
        val circle = circleOfFifthsQuarterCommaMeanTone

        circle.getRatios().zip(chain.getSortedRatios()).forEach {
            println("${it.first}, ${it.second}")
            assertEquals(it.first, it.second, 1e-12)
        }
        val lastFifth = chain.getClosingCircleCorrection()
        println(lastFifth)
    }

    @Test
    fun extendedQuarterCommaMeanToneTest() {
        val chain = predefinedTemperamentExtendedQuarterCommaMeanTone(0L).chainOfFifths()
        val cents = extendedQuarterCommaMeantone

        chain.getSortedRatios().zip(cents).forEach {
            val centChain = ratioToCents(it.first)
            println("$centChain, ${it.second}")
            //assertEquals(centChain, it.second, 1e-12)
        }
    }

    @Test
    fun testOther() {
        val chain = predefinedTemperamentYoung2(0L).chainOfFifths()
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
        val chain = predefinedTemperamentFifthCommaMeanTone(0L).chainOfFifths()

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