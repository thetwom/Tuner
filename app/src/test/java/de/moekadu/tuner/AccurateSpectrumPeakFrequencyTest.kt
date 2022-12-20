package de.moekadu.tuner

import de.moekadu.tuner.notedetection.RealFFT
import de.moekadu.tuner.notedetection.AccurateSpectrumPeakFrequency
import de.moekadu.tuner.notedetection.FrequencySpectrum
import org.junit.Assert
import org.junit.Test
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sin

data class AccurateSpectrumPeakFrequencyTestResult(val frequency: Float, val errorLow: Float, val errorHigh: Float)

class AccurateSpectrumPeakFrequencyTest {

    @Test
    fun testAccuracies() {
        val numSamples = 64
        val relativeShift = 0.4f

        var freqIndexFloat = 5f
        while (freqIndexFloat < 12f) {
            freqIndexFloat += 0.1f
            val (_, errorLowAccuracy, errorHighAccuracy) = testAccuracy(
                numSamples, freqIndexFloat, relativeShift
            )
            Assert.assertTrue(errorHighAccuracy <= errorLowAccuracy)
            Assert.assertTrue(errorHighAccuracy < 1f)
        }
    }

    private fun testAccuracy(numSamples: Int, frequencyIndexFloat: Float, relativeShift: Float): AccurateSpectrumPeakFrequencyTestResult {

        val timeSeriesDuration = 3.0f
        val frequencyIndex = frequencyIndexFloat.roundToInt()
        val frequency = frequencyIndexFloat / timeSeriesDuration
        val timeShift = relativeShift * timeSeriesDuration
        val func = {x: Float -> sin(2 * PI.toFloat() * frequency * x)}

        val df = 1.0f / timeSeriesDuration
        val dt = timeSeriesDuration / numSamples
        val timeValues1 = FloatArray(numSamples) { it * dt }
        val values1 = FloatArray(numSamples) {func(timeValues1[it])}

        val timeValues2 = FloatArray(numSamples) {timeValues1[it] + timeShift}
        val values2 = FloatArray(numSamples) {func(timeValues2[it])}

        val spec1 = FrequencySpectrum(numSamples / 2 + 1, df)
        val spec2 = FrequencySpectrum(numSamples / 2 + 1, df)

        val fft = RealFFT(numSamples)
        fft.fft(values1, spec1.spectrum)
        fft.fft(values2, spec2.spectrum)

        val spectrumPeakFrequency = AccurateSpectrumPeakFrequency(spec1, spec2, timeShift)
        val frequencyHighAccuracy = spectrumPeakFrequency[frequencyIndex]

        val errorLowAccuracy = (frequency - frequencyIndex * df).absoluteValue / frequency * 100
        val errorHighAccuracy = (frequency - frequencyHighAccuracy).absoluteValue / frequency * 100

        return AccurateSpectrumPeakFrequencyTestResult(frequencyHighAccuracy, errorLowAccuracy, errorHighAccuracy)
    }
}