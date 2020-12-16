package de.moekadu.tuner

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.pow

class CorrelationAndSpectrumComputer {
    private var correlation : Correlation? = null
    private val correlationMutex = Mutex()

    suspend fun run(sampleData: SampleData, windowType: WindowingFunction) : TunerResults {
        return withContext(Dispatchers.Default) {

            val results = TunerResults(sampleData.size, sampleData.sampleRate, sampleData.framePosition)

            correlationMutex.withLock {
                // recreate correlation instance if sample size changed
                if (correlation?.size != sampleData.size || windowType != correlation?.windowType)
                    correlation = Correlation(sampleData.size, windowType)

                correlation?.correlate(sampleData.data, results.correlation, false, results.spectrum)
            }

            for(i in results.ampSqrSpec.indices)
                results.ampSqrSpec[i] = results.spectrum[2*i].pow(2) + results.spectrum[2*i+1].pow(2)
            results
        }
    }
}
