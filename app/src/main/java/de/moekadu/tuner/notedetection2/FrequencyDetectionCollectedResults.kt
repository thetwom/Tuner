package de.moekadu.tuner.notedetection2

import de.moekadu.tuner.misc.MemoryPool
import de.moekadu.tuner.notedetection.MemoryPoolCorrelation
import de.moekadu.tuner.notedetection.RealFFT
import de.moekadu.tuner.notedetection.WindowingFunction
import de.moekadu.tuner.notedetection.getFrequency
import kotlinx.coroutines.channels.Channel
import kotlin.math.pow
import kotlin.math.sqrt

class FrequencyDetectionCollectedResults(val sizeOfTimeSeries: Int, val sampleRate: Int) {
    val timeSeries = TimeSeries(sizeOfTimeSeries, 1.0f / sampleRate)

    /** Standard deviation computed from time series. */
    var timeSeriesStandardDeviation = 0f

    /** Frequency spectrum data.
     * Use twice the size than required for the FFT since we will double the signal by zero padding,
     * which is needed for correlation computation.
     */
    val frequencySpectrum = FrequencySpectrum(
        (sizeOfTimeSeries + 1),
        RealFFT.getFrequency(1, 2 * sizeOfTimeSeries, timeSeries.dt)) // factor 2 since we will zeropad input

    /** Frame position on which the previousSpectrum is based on. -1 if there is not previous spectrum.n*/
    var previousFramePosition = -1

    /** Store the spectrum with complex numbers of the previous evaluation. This is needed
     * for high accuracy frequency computation.
     */
    val previousSpectrum = FrequencySpectrum(frequencySpectrum.size, frequencySpectrum.df)

    val accuratePeakFrequency = AccurateSpectrumPeakFrequency(previousSpectrum, frequencySpectrum, 0f)

    val autoCorrelation = AutoCorrelation(sizeOfTimeSeries + 1, timeSeries.dt)

    /** Relative noise in the signal (1->high noise, 0 -> low noise) */
    var noise = 0.0f

    val correlationBasedFrequency = CorrelationBasedFrequency(0f , 0f, 0f)

    val harmonics = Harmonics(sizeOfTimeSeries)

    val harmonicStatistics = HarmonicStatistics()

    val frequency
        get() = if (harmonicStatistics.frequency != 0f)
            harmonicStatistics.frequency
        else correlationBasedFrequency.frequency

    var inharmonicity = 0f
}

class MemoryPoolFrequencyDetectionCollectedResults {
    private val pool = MemoryPool<FrequencyDetectionCollectedResults>()

    fun get(sizeOfTimeSeries: Int, sampleRate: Int) = pool.get(
        factory = { FrequencyDetectionCollectedResults(sizeOfTimeSeries, sampleRate) },
        checker = { it.sizeOfTimeSeries == sizeOfTimeSeries && it.sampleRate == sampleRate }
    )
}

class FrequencyDetectionResultCollector(
    private val frequencyMin: Float,
    private val frequencyMax: Float,
    private val subharmonicsTolerance: Float = 0.05f,
    private val subharmonicsPeakRatio: Float = 0.9f,
    private val harmonicTolerance: Float = 0.1f,
    private val minimumFactorOverLocalMean: Float = 5f,
    private val maxGapBetweenHarmonics: Int = 10,
    private val maxNumHarmonicsForInharmonicity: Int = 8,
    private val windowType: WindowingFunction = WindowingFunction.Tophat,
    private val acousticWeighting: AcousticWeighting = AcousticCWeighting()
) {
    private val collectedResultsMemory = MemoryPoolFrequencyDetectionCollectedResults()
    private val spectrumAndCorrelationMemory = MemoryPoolCorrelation()
    private val inharmonicityDetectorMemory = MemoryPoolInharmonicityDetector()
    private var previousResultsBuffer = Channel<MemoryPool<FrequencyDetectionCollectedResults>.RefCountedMemory>(Channel.CONFLATED)

    fun collectResults(sampleData: MemoryPool<SampleData>.RefCountedMemory): MemoryPool<FrequencyDetectionCollectedResults>.RefCountedMemory {
        val collectedResults = collectedResultsMemory.get(sampleData.memory.size, sampleData.memory.sampleRate)

        val previousResults = previousResultsBuffer.tryReceive().getOrNull()
        copyPreviousResultsToNewResults(previousResults?.memory, collectedResults.memory)
        previousResults?.decRef() // don't need this anymore in the following

        copySampleDataToTimeSeries(sampleData.memory, collectedResults.memory.timeSeries)
        collectedResults.memory.timeSeriesStandardDeviation =
            computeTimeSeriesStandardDeviation(collectedResults.memory.timeSeries)

        computeSpectrumAndCorrelation(
            sampleData.memory,
            collectedResults.memory.autoCorrelation,
            collectedResults.memory.frequencySpectrum
        )

        collectedResults.memory.noise = 1f - collectedResults.memory.autoCorrelation[1] / collectedResults.memory.autoCorrelation[0]

        // set time shift for accurate frequencies
        collectedResults.memory.accuratePeakFrequency.timeShiftBetweenSpecs = if (collectedResults.memory.previousFramePosition == 0)
            0f
        else
            collectedResults.memory.timeSeries.dt * (collectedResults.memory.timeSeries.framePosition - collectedResults.memory.previousFramePosition)
        // Log.v("Tuner", "CollectedResults.collectResults: frameShift at frame ${collectedResults.memory.timeSeries.framePosition} = ${collectedResults.memory.timeSeries.framePosition - collectedResults.memory.previousFramePosition}")

        findCorrelationBasedFrequency(
            collectedResults.memory.correlationBasedFrequency,
            collectedResults.memory.autoCorrelation,
            frequencyMin,
            frequencyMax,
            subharmonicsTolerance = subharmonicsTolerance,
            subharmonicPeakRatio = subharmonicsPeakRatio
        )
//        Log.v("Tuner", "CollectedResults.collectResults: correlationBased frequency = ${collectedResults.memory.correlationBasedFrequency}")
        if (collectedResults.memory.correlationBasedFrequency.frequency != 0f) {
            findHarmonicsFromSpectrum(
                collectedResults.memory.harmonics,
                collectedResults.memory.correlationBasedFrequency.frequency,
                frequencyMin,
                frequencyMax,
                collectedResults.memory.frequencySpectrum,
                collectedResults.memory.accuratePeakFrequency,
                harmonicTolerance = harmonicTolerance,
                minimumFactorOverLocalMean = minimumFactorOverLocalMean,
                maxNumFail = maxGapBetweenHarmonics,
            )
            collectedResults.memory.harmonics.sort()

            collectedResults.memory.harmonicStatistics.evaluate(collectedResults.memory.harmonics, acousticWeighting)

            val inharmonicityDetector = inharmonicityDetectorMemory.get(maxNumHarmonicsForInharmonicity)
            collectedResults.memory.inharmonicity = inharmonicityDetector.memory.computeInharmonicity(
                collectedResults.memory.harmonics,
                acousticWeighting
            )
            inharmonicityDetector.decRef()
        } else {
            collectedResults.memory.harmonics.clear()
            collectedResults.memory.harmonicStatistics.clear()
            collectedResults.memory.inharmonicity = 0f
        }

        collectedResults.incRef() // increment ref count to avoid recycling while its in the previousResultsBuffer
        previousResultsBuffer.trySend(collectedResults)

        return collectedResults
    }

    private fun copyPreviousResultsToNewResults(
        previousResults: FrequencyDetectionCollectedResults?,
        newResults: FrequencyDetectionCollectedResults
    ) {
        if (previousResults != null && previousResults.sizeOfTimeSeries == newResults.sizeOfTimeSeries) {
            newResults.previousFramePosition = previousResults.timeSeries.framePosition
            previousResults.frequencySpectrum.spectrum.copyInto(newResults.previousSpectrum.spectrum)
        } else {
            newResults.previousFramePosition = -1
        }
    }

    private fun computeTimeSeriesStandardDeviation(timeSeries: TimeSeries): Float {
        val average = timeSeries.values.average().toFloat()
        return sqrt(
            timeSeries.values.fold(0f) { sum, element -> sum + (element - average).pow(2)}
                    / timeSeries.values.size
        )
    }

    private fun copySampleDataToTimeSeries(sampleData: SampleData, timeSeries: TimeSeries) {
        require(sampleData.data.size == timeSeries.size)
        timeSeries.framePosition = sampleData.framePosition
        sampleData.data.copyInto(timeSeries.values)
    }

    private fun computeSpectrumAndCorrelation(
        sampleData: SampleData, autoCorrelation: AutoCorrelation, spectrum: FrequencySpectrum
    ) {
        val spectrumAndCorrelation = spectrumAndCorrelationMemory.get(sampleData.size, windowType)
        spectrumAndCorrelation.memory.correlate(
            input = sampleData.data,
            output = autoCorrelation.values,
            disableWindow = false,
            spectrum = spectrum.spectrum
        )

        for (i in spectrum.amplitudeSpectrumSquared.indices) {
            spectrum.amplitudeSpectrumSquared[i] =
                spectrum.spectrum[2 * i].pow(2) + spectrum.spectrum[2 * i + 1].pow(2)
        }

        // register for reuse
        spectrumAndCorrelation.decRef()
    }
}
