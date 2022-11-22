package de.moekadu.tuner.notedetection2

import de.moekadu.tuner.misc.UpdatableStatistics
import kotlin.math.sqrt

class HarmonicStatistics() {
    val frequency get() = frequencyStatistics.mean
    val frequencyVariance get() = frequencyStatistics.variance
    val frequencyStandardDeviation get() = frequencyStatistics.standardDeviation

    val inharmonicity get() = inharmonicityStatistics.mean
    val inharmonicityVariance get() = inharmonicityStatistics.variance
    val inharmonicityStandardDeviation get() = inharmonicityStatistics.standardDeviation

    private val frequencyStatistics = UpdatableStatistics()
    private val inharmonicityStatistics = UpdatableStatistics()

    fun clear() {
        frequencyStatistics.clear()
        inharmonicityStatistics.clear()
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

        for (i in 1 until harmonics.size) {
            val harmonic1 = harmonics[i - 1]
            val harmonic2 = harmonics[i]

            val amplitude1 = sqrt(harmonic1.spectrumAmplitudeSquared)
            val amplitude2 = sqrt(harmonic2.spectrumAmplitudeSquared)
            val freq1 = harmonic1.frequency
            val freq2 = harmonic2.frequency

            val weight = (weighting.applyToAmplitude(amplitude1, freq1) * weighting.applyToAmplitude(amplitude2, freq2))
            val inharmonicity = computeInharmonicity(
                freq1, harmonic1.harmonicNumber,
                freq2, harmonic2.harmonicNumber,
            )
            inharmonicityStatistics.update(inharmonicity, weight)
        }
    }
}
