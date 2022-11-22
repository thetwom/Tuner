package de.moekadu.tuner

import de.moekadu.tuner.notedetection2.AcousticAWeighting
import de.moekadu.tuner.notedetection2.AcousticCWeighting
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.log10

fun ampRatioToLog(amp: Float, ampRef: Float): Float {
    return 20f * log10(amp / ampRef)
}

class AcousticWeightingTest {

    @Test
    fun weightingA() {
        val weighting = AcousticAWeighting()
        assertEquals(10f, weighting.applyToAmplitude(10f, 1000f), 1e-6f)
        assertEquals(-19.1f, ampRatioToLog(weighting.applyToAmplitude(10f, 100f), 10f), 0.1f)
        assertEquals(-2.5f, ampRatioToLog(weighting.applyToAmplitude(10f, 10000f), 10f), 0.1f)
    }

    @Test
    fun weightingC() {
        val weighting = AcousticCWeighting()
        assertEquals(10f, weighting.applyToAmplitude(10f, 1000f), 1e-6f)
        assertEquals(-14.3f, ampRatioToLog(weighting.applyToAmplitude(10f, 10f), 10f), 0.1f)
        assertEquals(-4.5f, ampRatioToLog(weighting.applyToAmplitude(10f, 10000f), 10f), 0.1f)
    }

}