package de.moekadu.tuner

import de.moekadu.tuner.notedetection.HarmonicPredictor
import org.junit.Test
import org.junit.Assert.assertEquals

class HarmonicPredictorTest {
    @Test
    fun testPredictor() {
        val predictor = HarmonicPredictor()

        predictor.add(3, 300f)

        val f1 = predictor.predict(1)
        var f2 = predictor.predict(2)
        assertEquals(f1, 100f, 1e-12f)
        assertEquals(f2, 200f, 1e-12f)

        predictor.add(5, 500f)

        f2 = predictor.predict(2)
        assertEquals(f2, 200f, 1e-12f)

        predictor.clear()
        f2 = predictor.predict(2)
        assertEquals(f2, 0f)
    }

    @Test
    fun testPredictorNonlinear()
    {
        val predictor = HarmonicPredictor()

        val freqBase = 300f
        val beta = 0.2f
        val testFunc = { h: Int -> freqBase * h * (1 + beta * h) }

        predictor.add(3, testFunc(3))

        val f1 = predictor.predict(1)
        val f2 = predictor.predict(2)
        assertEquals(f1, testFunc(3) / 3, 1e-12f)
        assertEquals(f2, 2 * testFunc(3) / 3, 1e-12f)

        predictor.add(5, testFunc(5))

        val f5 = predictor.predict(5)
        assertEquals(f5, testFunc(5), 1e-12f)

        predictor.add(6, testFunc(6))
        val f3 = predictor.predict(3)
        assertEquals(f3, testFunc(3), 1e-12f)
    }
}