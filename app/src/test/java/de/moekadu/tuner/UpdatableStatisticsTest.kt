package de.moekadu.tuner

import de.moekadu.tuner.misc.UpdatableStatistics
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.absoluteValue
import kotlin.math.pow

class UpdatableStatisticsTest {

    @Test
    fun testSimple() {
        val stat = UpdatableStatistics()
        assertEquals(0.0f, stat.mean)
        assertEquals(0.0f, stat.variance)

        stat.update(4.0f, 0.456f)
        assertEquals(4.0f, stat.mean)
        assertEquals(0.0f, stat.variance)
    }

    @Test
    fun testConst() {
        val value = 40.0f
        val stat = UpdatableStatistics()
        for (i in 0 until 100)
            stat.update(value, 1.0f)

        assertEquals(value, stat.mean)
        assertEquals(0.0f, stat.variance)
    }

    @Test
    fun testNumbers() {
        val values = floatArrayOf(3.0f, 4.3f, 6.5f, 2.1f, 4.3f)
        val mean = values.sum() / values.size
        var variance = 0.0f
        for (v in values) {
            variance += (v - mean).pow(2)
        }
        variance /= values.size

        val stat = UpdatableStatistics()
        for (v in values)
            stat.update(v, 1.0f)

        assertEquals(mean, stat.mean, 1e-6f * mean.absoluteValue)
        assertEquals(variance, stat.variance, 1e-6f * variance)
    }

    @Test
    fun testNumbersAndWeights() {
        val values = floatArrayOf(3.0f, 4.3f, 6.5f, 2.1f, 4.3f)
        val weights = floatArrayOf(0.1f, 0.4f, 3.1f, 0.2f, 0.9f)
        var mean = 0.0f
        var weightSum = 0.0f

        values.zip(weights) { v, w ->
            mean += v * w
            weightSum += w
        }
        mean /= weightSum

        var variance = 0.0f
        values.zip(weights) { v, w ->
            variance += w * (v - mean).pow(2)
        }
        variance /= weightSum

        val stat = UpdatableStatistics()
        values.zip(weights) { v, w ->
            stat.update(v, w)
        }

        assertEquals(mean, stat.mean, 1e-6f * mean.absoluteValue)
        assertEquals(variance, stat.variance, 1e-6f * variance)
    }

}