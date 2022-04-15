package de.moekadu.tuner

import org.junit.Test
import kotlin.math.log
import kotlin.math.pow

class TuningCircleOfFifthsTest {
    val notes = arrayOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B", "C")
    fun cent(ratio: Double): Double {
        val centRatio = 2.0.pow(1.0/1200)
        return log(ratio, centRatio)
    }

    @Test
    fun quarterCommaMeantone() {
        val ratios = circleOfFifthsQuarterCommaMeanTone.getRatios()
        ratios.zip(notes).forEach { p ->
            println("Quarter-comma mean tone: ${p.second}: ${cent(p.first)}")
        }
    }

    @Test
    fun thirdCommaMeantone() {
        val ratios = circleOfFifthsThirdCommaMeanTone.getRatios()
        ratios.zip(notes).forEach { p ->
            println("Third-comma mean tone: ${p.second}: ${cent(p.first)}")
        }
    }

    @Test
    fun werckmeisterIII() {
        val ratios = circleOfFifthsWerckmeisterIII.getRatios()
        ratios.zip(notes).forEach { p ->
            println("WerckmeisterIII: ${p.second}: ${cent(p.first)}")
        }
    }

    @Test
    fun werckmeisterIV() {
        val ratios = circleOfFifthsWerckmeisterIV.getRatios()
        ratios.zip(notes).forEach { p ->
            println("WerckmeisterIV: ${p.second}: ${cent(p.first)}")
        }
    }

    @Test
    fun werckmeisterV() {
        val ratios = circleOfFifthsWerckmeisterV.getRatios()
        ratios.zip(notes).forEach { p ->
            println("WerckmeisterV: ${p.second}: ${cent(p.first)}")
        }
    }

    @Test
    fun kirnberger1() {
        val ratios = circleOfFifthsKirnberger1.getRatios()
        ratios.zip(notes).forEach { p ->
            println("Kirnberger 1: ${p.second}: ${cent(p.first)}")
        }
    }

    @Test
    fun kirnberger2() {
        val ratios = circleOfFifthsKirnberger2.getRatios()
        ratios.zip(notes).forEach { p ->
            println("Kirnberger 2: ${p.second}: ${cent(p.first)}")
        }
    }

    @Test
    fun kirnberger3() {
        val ratios = circleOfFifthsKirnberger3.getRatios()
        ratios.zip(notes).forEach { p ->
            println("Kirnberger 3: ${p.second}: ${cent(p.first)}")
        }
    }

    @Test
    fun neidhardt1() {
        val ratios = circleOfFifthsNeidhardt1.getRatios()
        ratios.zip(notes).forEach { p ->
            println("Neidhardt 1: ${p.second}: ${cent(p.first)}")
        }
    }

    @Test
    fun neidhardt2() {
        val ratios = circleOfFifthsNeidhardt2.getRatios()
        ratios.zip(notes).forEach { p ->
            println("Neidhardt 2: ${p.second}: ${cent(p.first)}")
        }
    }

    @Test
    fun neidhardt3() {
        val ratios = circleOfFifthsNeidhardt3.getRatios()
        ratios.zip(notes).forEach { p ->
            println("Neidhardt 3: ${p.second}: ${cent(p.first)}")
        }
    }

//    @Test
//    fun neidhardt4() {
//        val ratios = circleOfFifthsNeidhardtIV.getRatios()
//        ratios.zip(notes).forEach { p ->
//            println("Neidhardt 4: ${p.second}: ${cent(p.first)}")
//        }
//    }

    @Test
    fun valotti() {
        val ratios = circleOfFifthsValotti.getRatios()
        ratios.zip(notes).forEach { p ->
            println("Valotti: ${p.second}: ${cent(p.first)}")
        }
    }

    @Test
    fun young2() {
        val ratios = circleOfFifthsYoung2.getRatios()
        ratios.zip(notes).forEach { p ->
            println("Young 2: ${p.second}: ${cent(p.first)}")
        }
    }

    @Test
    fun edo12() {
        val ratios = circleOfFifthsEDO12.getRatios()
        ratios.zip(notes).forEach { p ->
            println("EDO12: ${p.second}: ${cent(p.first)}")
        }
    }
}