package de.moekadu.tuner

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.pow

suspend fun determineCorrelationMaxima(correlation: FloatArray, minimumFrequency: Float, maximumFrequency: Float, dt: Float) : ArrayList<Int> {
    val localMaxima: ArrayList<Int>

    withContext(Dispatchers.Default) {
        val indexMinimumFrequency =
            min(correlation.size, ceil((1.0f / minimumFrequency) / dt).toInt())
        val indexMaximumFrequency =
            min(correlation.size, ceil((1.0f / maximumFrequency) / dt).toInt())

        // index of maximum frequency is the smaller index ..., we have to start from zero, otherwise its hard to interpret the first maximum
        localMaxima = findMaximaOfPositiveSections(correlation, 0, indexMinimumFrequency)
        localMaxima.removeAll { it < indexMaximumFrequency }
    }
    return localMaxima
}

suspend fun determineSpectrumMaxima(ampSqrSpec: FloatArray, minimumFrequency: Float, maximumFrequency: Float, dt: Float, localMaximaSNR: Float) : ArrayList<Int> {
    val localMaxima: ArrayList<Int>

    withContext(Dispatchers.Default) {
        /// Minimum index in transformed spectrum, which should be considered
        val startIndex = RealFFT.closestFrequencyIndex(minimumFrequency, ampSqrSpec.size - 1, dt)
        /// Maximum index in transformed spectrum, which should be considered (endIndex is included in range)
        val endIndex = RealFFT.closestFrequencyIndex(maximumFrequency, ampSqrSpec.size - 1, dt)
        localMaxima = findLocalMaxima(ampSqrSpec, localMaximaSNR, startIndex, endIndex+1)
    }

    return localMaxima
}
