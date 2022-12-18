package de.moekadu.tuner.notedetection2

import de.moekadu.tuner.misc.UpdatableStatistics
import kotlin.math.sqrt

class HarmonicStatistics {
    val frequency get() = frequencyStatistics.mean
    val frequencyVariance get() = frequencyStatistics.variance
    val frequencyStandardDeviation get() = frequencyStatistics.standardDeviation

    private val frequencyStatistics = UpdatableStatistics()

    fun clear() {
        frequencyStatistics.clear()
    }

    fun evaluate(harmonics: Harmonics, weighting: AcousticWeighting) {
        clear()

        for (i in 0 until harmonics.size) {
            val harmonic = harmonics[i]
            val amplitude = sqrt(harmonic.spectrumAmplitudeSquared)
            val weight = weighting.applyToAmplitude(amplitude, harmonic.frequency)
            val frequencyBase = harmonic.frequency / harmonic.harmonicNumber
            frequencyStatistics.update(frequencyBase, weight)
        }
    }
}
