/*
 * Copyright 2020 Michael Moessner
 *
 * This file is part of Tuner.
 *
 * Tuner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tuner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
 */

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
