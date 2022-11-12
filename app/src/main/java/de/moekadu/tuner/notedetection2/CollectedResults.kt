package de.moekadu.tuner.notedetection2

import de.moekadu.tuner.notedetection.RealFFT
import de.moekadu.tuner.notedetection.getFrequency
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CollectedResults(val sizeOfTimeSeries: Int, sampleRate: Int) {
    val timeSeries = TimeSeries(sizeOfTimeSeries, 1.0f / sampleRate)

    /** Frequency spectrum data.
     * Use twice the size than required for the FFT since we will double the signal by zero padding,
     * which is needed for correlation computation.
     */
    val frequencySpectrum = FrequencySpectrum(
        2 * (sizeOfTimeSeries + 1), // factor two since we will zeropad input
        RealFFT.getFrequency(1, 2 * sizeOfTimeSeries, timeSeries.dt)) // factor 2 since we will zeropad input

    val autoCorrelation = AutoCorrelation(sizeOfTimeSeries + 1, timeSeries.dt)

    val harmonics = Harmonics(sizeOfTimeSeries)

    var frequency = 0f
        private set
    var inharmonicity = 0f
        private set

    private var refCount = 0
    private val refCountMutex = Mutex()

    //private val pool = ...
    suspend fun incrementRefCount() {
        refCountMutex.withLock { ++refCount }
    }
    suspend fun decrementRefCount() {
        refCountMutex.withLock {
            --refCount
//            if (refCount == 0)
//                pool.recycle(this)
        }
    }
}