package de.moekadu.tuner

import de.moekadu.tuner.notedetection.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.*

class PitchChooserAndAccuracyIncreaserTest {

    private fun createSampleSignal(frequency: Float, periods: Float, size: Int, offset: Int,
                                   harmonicAmps: FloatArray = floatArrayOf(1.0f)): WorkingData {
        val sampleRate = ((frequency * size) / periods).roundToInt()
        val result = WorkingData(size, sampleRate, offset)
        val sampleData = FloatArray(size) { i ->
            var s = 0f
            for (n in harmonicAmps.indices)
                    s += harmonicAmps[n] * sin((n+1) * 2 * PI.toFloat() * frequency * (i + offset) * result.dt)
            s
        }

        val correlation = Correlation(result.size, WindowingFunction.Tophat)
        correlation.correlate(sampleData, result.correlation, spectrum = result.spectrum)
        for (i in result.ampSqrSpec.indices)
            result.ampSqrSpec[i] = result.spectrum[2*i].pow(2) + result.spectrum[2*i+1].pow(2)

        result.specMaximaIndices = findLocalMaxima(result.ampSqrSpec, 10.0f)
        result.correlationMaximaIndices = findMaximaOfPositiveSections(result.correlation)
        result.correlationMaximaIndices?.removeFirst()
        return result
    }

    @Test
    fun veryBasicTest() {
        val frequency = 12f
        val result = createSampleSignal(frequency, 2f, 32, 0)

        // very basic frequency check from spectrum result
        val freqAtMaxSpec = result.ampSpecSqrFrequencies[result.specMaximaIndices!![0]]
        assertEquals(frequency, freqAtMaxSpec, 0.0001f)

        val timeShiftAtMaxCorr = result.timeShiftFromCorrelation(result.correlationMaximaIndices!![0])
        val freqAtMaxCorr = 1.0f / timeShiftAtMaxCorr
        assertEquals(frequency, freqAtMaxCorr, 0.0001f)
    }

    @Test
    fun increaseSpecAccuracy() {
        val frequency = 12f
        val result1 = createSampleSignal(frequency, 2.2f, 128, 0)
        val result2 = createSampleSignal(frequency, 2.2f, 128, 64)
        val maxIndex = result2.specMaximaIndices!![0]
        val df = result2.frequencyFromSpectrum(1)
        val shift = result2.dt * (result2.framePosition - result1.framePosition)
        val frequencyHighAccuracy = increaseFrequencyAccuracy(result1.spectrum, result2.spectrum, maxIndex, df, shift)
        //assertEquals(frequency, result2.frequencyFromSpectrum(maxIndex), 0.001f)
        assertEquals(frequency, frequencyHighAccuracy, 0.05f)
        assertTrue(abs(frequency - frequencyHighAccuracy) < abs(frequency - result2.frequencyFromSpectrum(maxIndex)))
    }

    @Test
    fun increaseCorrAccuracy() {
        val frequency = 12f
        val result = createSampleSignal(frequency, 2.2f, 128, 0)
        val timeShiftHighAccuracy = increaseTimeShiftAccuracy(result.correlation, result.correlationMaximaIndices!![0], result.dt)
        val frequencyHighAccuracy = 1.0f / timeShiftHighAccuracy

        val frequencyLowAccuracy = result.frequencyFromCorrelation(result.correlationMaximaIndices!![0])

        //assertEquals(frequency, frequencyLowAccuracy, 0.001f)
        assertEquals(frequency, frequencyHighAccuracy, 0.5f)
        assertTrue(abs(frequency - frequencyHighAccuracy) < abs(frequency - frequencyLowAccuracy))
    }

    @Test
    fun choosePitch() {
        val frequency = 12f
        val results = createSampleSignal(frequency, 2.2f, 128, 0)
        val mostProbableValueFromSpec = calculateMostProbableSpectrumPitch(
            results.specMaximaIndices, results.ampSqrSpec, results.frequencyFromSpectrum, null)
        val mostProbableValueFromCorrelation = calculateMostProbableCorrelationPitch(
            results.correlationMaximaIndices, results.correlation, results.frequencyFromCorrelation, null)

        assertEquals(mostProbableValueFromSpec.rating, 1f, 1e-8f)
        assertEquals(mostProbableValueFromCorrelation.rating, 1f, 1e-8f)

        val frequencySpec = results.frequencyFromSpectrum(mostProbableValueFromSpec.ampSqrSpecIndex!!)
        assertEquals(frequency, frequencySpec, 2f)
        val frequencyCorr = results.frequencyFromCorrelation(mostProbableValueFromCorrelation.correlationIndex!!)
        assertEquals(frequency, frequencyCorr, 1f)
    }

    @Test
    fun increaseAccuracy() {
        val frequency = 12f
        val resultsPrevious = createSampleSignal(frequency, 2.2f, 128, 0)
        val results = createSampleSignal(frequency, 2.2f, 128, 64)

        val mostProbableValueFromCorrelation = calculateMostProbableCorrelationPitch(
            results.correlationMaximaIndices, results.correlation, results.frequencyFromCorrelation, null)

        val frequencyHighAccuracy = increaseAccuracyForCorrelationBasedFrequency(
            mostProbableValueFromCorrelation.correlationIndex!!, results, resultsPrevious)
        assertEquals(frequency, frequencyHighAccuracy, 0.1f)

        val mostProbableValueFromSpec = calculateMostProbableSpectrumPitch(
            results.specMaximaIndices, results.ampSqrSpec, results.frequencyFromSpectrum, null)
        val frequencyHighAccuracy2 = increaseAccuracyForSpectrumBasedFrequency(
            mostProbableValueFromSpec.ampSqrSpecIndex!!, results, resultsPrevious)
        assertEquals(frequency, frequencyHighAccuracy2, 0.1f)

        val frequencyHighAccuracy3 = increaseAccuracyForSpectrumBasedFrequency(
            mostProbableValueFromSpec.ampSqrSpecIndex!!, results, null)
        assertEquals(frequency, frequencyHighAccuracy3, 0.3f)
    }

    @Test
    fun choosePitch2() {
        val frequency = 12f
        val results = createSampleSignal(frequency, 2.0f, 128, 0, floatArrayOf(0.5f, 0.5f, 1.0f))

        val mostProbableValueFromCorrelation = calculateMostProbableCorrelationPitch(
            results.correlationMaximaIndices, results.correlation, results.frequencyFromCorrelation, null)
        val frequencyFromCorrelation = results.frequencyFromCorrelation(mostProbableValueFromCorrelation.correlationIndex!!)
        assertEquals(3.0f * frequency, frequencyFromCorrelation,1f)

        val mostProbableValueFromCorrelationHinted = calculateMostProbableCorrelationPitch(
            results.correlationMaximaIndices, results.correlation, results.frequencyFromCorrelation, frequency)
        val frequencyFromCorrelationHinted = results.frequencyFromCorrelation(mostProbableValueFromCorrelationHinted.correlationIndex!!)
        assertEquals(frequency, frequencyFromCorrelationHinted,0.0001f)
    }

    @Test
    fun choosePitch3() {
        val frequency = 12f
        val results = createSampleSignal(frequency, 5.0f, 256, 0, floatArrayOf(0.2f, 0.2f, 1.0f))

        val mostProbableValueFromSpectrum = calculateMostProbableSpectrumPitch(
            results.specMaximaIndices, results.ampSqrSpec, results.frequencyFromSpectrum, null)
        val frequencyFromSpectrum = results.frequencyFromSpectrum(mostProbableValueFromSpectrum.ampSqrSpecIndex!!)
        assertEquals(frequency, frequencyFromSpectrum,1f)

        val mostProbableValueFromSpectrumHinted = calculateMostProbableSpectrumPitch(
            results.specMaximaIndices, results.ampSqrSpec, results.frequencyFromSpectrum, 3*frequency)
        val frequencyFromSpectrumHinted = results.frequencyFromSpectrum(mostProbableValueFromSpectrumHinted.ampSqrSpecIndex!!)
        assertEquals(3.0f * frequency, frequencyFromSpectrumHinted,0.1f)
    }
}