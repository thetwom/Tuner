package de.moekadu.tuner

import de.moekadu.tuner.notedetection2.getPeakOfPolynomialFit
import org.junit.Assert
import org.junit.Test
import kotlin.math.pow

class MaximumOfPolynomialFitTest {

    @Test
    fun testPeak() {
        val tPeak = 0.45f
        val polynomial = {x: Float ->  -(x - tPeak).pow(2) + 0.23f}
        val dt = 12.0f
        val tc = tPeak - 0.6f * dt

        val (tPeakFromFit, peakValueFromFit) = getPeakOfPolynomialFit(
            polynomial(tc - dt),
            polynomial(tc),
            polynomial(tc + dt),
            tc, dt
        )

        Assert.assertEquals(tPeakFromFit, tPeak, 1e-4f * tPeak)
        Assert.assertEquals(peakValueFromFit, polynomial(tPeak), 1e-4f * polynomial(tPeak))
    }
}