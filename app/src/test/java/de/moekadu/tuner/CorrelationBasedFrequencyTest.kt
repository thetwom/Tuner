package de.moekadu.tuner

import de.moekadu.tuner.notedetection2.AutoCorrelation
import de.moekadu.tuner.notedetection2.findCorrelationBasedFrequency
import org.junit.Assert
import org.junit.Test

class CorrelationBasedFrequencyTest {
    @Test
    fun checkPeaks() {
        val numTotal = 1200
        val peak1 = 1000
        val peak2 = 251
        val dt = 0.1f
        val correlation = AutoCorrelation(numTotal, dt)
        correlation.values.fill(0f)
        correlation.values[peak1] = 100f
        correlation.values[peak2] = 98f

        var tolerance = 0.01f
        val result1 = findCorrelationBasedFrequency(correlation, subharmonicsTolerance = tolerance)
        require(result1 != null)
        Assert.assertEquals(dt * peak1.toFloat(), result1.timeShift, 1e-6f)
        Assert.assertEquals(1f / (dt * peak1.toFloat()), result1.frequency, 1e-6f)
        Assert.assertEquals(correlation[peak1], result1.correlationAtTimeShift, 1e-6f)

        tolerance = 0.05f
        val result2 = findCorrelationBasedFrequency(correlation, subharmonicsTolerance = tolerance)
        require(result2 != null)
        Assert.assertEquals(dt * peak2.toFloat(), result2.timeShift, 1e-6f)
        Assert.assertEquals(1f / (dt * peak2.toFloat()), result2.frequency,1e-6f)
        Assert.assertEquals(correlation[peak2], result2.correlationAtTimeShift, 1e-6f)

        // restrict frequency around peak1, so we won't find peak 2
        val result3 = findCorrelationBasedFrequency(
            correlation,
            frequencyMin = 1 / (dt * (peak1 + 20)),
            frequencyMax = 1 / (dt * (peak1 - 20)),
            subharmonicsTolerance = tolerance)
        require(result3 != null)
        Assert.assertEquals(1f / (dt * peak1.toFloat()), result3.frequency, 1e-6f)
    }
}