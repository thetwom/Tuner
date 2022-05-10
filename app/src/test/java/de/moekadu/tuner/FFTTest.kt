package de.moekadu.tuner

import de.moekadu.tuner.notedetection.RealFFT
import de.moekadu.tuner.notedetection.getFrequency
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class FFTTest {
    @Test
    fun sinTest() {
        val numSamples = 32
        val frequency = 1f
        val amp = 2f
        val offset = 2.3f
        val samples = FloatArray(numSamples) {i -> offset + amp * sin(2 * kotlin.math.PI.toFloat() * frequency * i / numSamples.toFloat())}
        val fft = RealFFT(numSamples)
        val result = FloatArray(numSamples + 2)
        fft.fft(samples, result)
        val offsetFFT = result[0] / numSamples
        assertEquals(offset, offsetFFT, 1e-5f)
        val ampFFT = -result[3] / numSamples * 2
        assertEquals(amp, ampFFT, 1e-5f)
    }

    @Test
    fun cosTest() {
        val numSamples = 32
        val frequency = 2f
        val amp = 2f
        val offset = 2.3f
        val samples = FloatArray(numSamples) {i -> offset + amp * cos(2 * kotlin.math.PI.toFloat() * frequency * i / numSamples.toFloat())}
        val fft = RealFFT(numSamples)
        val result = FloatArray(numSamples + 2)
        fft.fft(samples, result)
        val offsetFFT = result[0] / numSamples
        assertEquals(offset, offsetFFT, 1e-5f)
        val ampFFT = result[4] / numSamples * 2
        assertEquals(amp, ampFFT, 1e-5f)
    }

    @Test
    fun freqTest() {
        val numSamples = 32
        val frequency = 1f
        val dt = 1f / numSamples
        val freqFFT = RealFFT.getFrequency(1, numSamples, dt)
        assertEquals(frequency, freqFFT, 1e-6f)
    }
}