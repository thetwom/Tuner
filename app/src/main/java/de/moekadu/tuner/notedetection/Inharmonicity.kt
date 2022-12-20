package de.moekadu.tuner.notedetection

import de.moekadu.tuner.misc.MemoryPool
import de.moekadu.tuner.misc.UpdatableStatistics
import kotlin.math.log
import kotlin.math.sqrt

/** Class for detecting inharmonicities.
 * @param maxNumHarmonics Maximum number of harmonics, which should be used for the computation
 *   of harmonics. In theory you can use a big value here, but the underlying algorithm will become
 *   expensive. So better use values between 3 and 10.
 */
class InharmonicityDetector(val maxNumHarmonics: Int) {

    /** Buffer for the harmonics sorted by the amplitudes (largest comes first). */
    private val harmonics = Array<Harmonic?>(maxNumHarmonics) {null}
    /** Object for averaging the available harmonics. */
    private val statistics = UpdatableStatistics()

    /** Extract harmonics from the input, store them internally sorted by amplitude and return.
     * This is only public to better test.
     * @param harmonics All available harmonics, which need not to be sorted.
     * @return The maxNumHarmonics harmonics sorted by the amplitude (largest comes first). If
     *   the number of available harmonics is smaller than maxNumHarmonics, the remaining entries
     *   will be null.
     */
    fun extractAndSortHarmonicsWithHighestAmplitude(harmonics: Harmonics): Array<Harmonic?> {
        this.harmonics.fill(null)

        for (i in 0 until harmonics.size) {
            var h = harmonics[i]
            val currentMin = this.harmonics.last()?.spectrumAmplitudeSquared ?: Float.NEGATIVE_INFINITY

            if (h.spectrumAmplitudeSquared > currentMin) {
                for (j in 0 until this.harmonics.size){
                    val harmonicJ = this.harmonics[j]
                    if (harmonicJ == null) {
                        this.harmonics[j] = h
                        break
                    }
                    else if (h.spectrumAmplitudeSquared > harmonicJ.spectrumAmplitudeSquared) {
                        this.harmonics[j] = h
                        h = harmonicJ
                    }
                }
            }
        }
        return this.harmonics
    }

    /** Compute inharmonicity.
     * @param harmonics Harmonics which do not need to be sorted, of which the inharmonicity
     *   should be computed.
     * @param acousticWeighting Frequency dependent weighting function.
     * @return Averaged inharmonicity value as computed by the function computeInharmonicity.
     */
    fun computeInharmonicity(harmonics: Harmonics, acousticWeighting: AcousticWeighting): Float {
        val maxHarmonics = extractAndSortHarmonicsWithHighestAmplitude(harmonics)
        if (maxHarmonics.size < 2 || maxHarmonics[1] == null)
            return 0f
        statistics.clear()

        var hPrev = maxHarmonics[0] ?: return 0f
        var weightPrev = acousticWeighting.applyToAmplitude(
            sqrt(hPrev.spectrumAmplitudeSquared),
            hPrev.frequency
        )

        for (i in 1 until maxHarmonics.size) {
            val hI = maxHarmonics[i] ?: break
            val weightI = acousticWeighting.applyToAmplitude(
                sqrt(hI.spectrumAmplitudeSquared),
                hI.frequency
            )

            val inharmonicity = computeInharmonicity(
                hPrev.frequency, hPrev.harmonicNumber,
                hI.frequency, hI.harmonicNumber
            )

            statistics.update(inharmonicity, weightPrev * weightI)
            hPrev = hI
            weightPrev = weightI
        }
        return statistics.mean
    }
}

class MemoryPoolInharmonicityDetector {
    private val pool = MemoryPool<InharmonicityDetector>()

    fun get(maxNumHarmonics: Int) = pool.get(
        factory = { InharmonicityDetector(maxNumHarmonics) },
        checker = { it.maxNumHarmonics == maxNumHarmonics }
    )
}

/** Compute inharmonicity between two frequencies.
 * We define the inharmonicity as f = f_1 * h**(inharmonicity + 1)
 * Such that if two frequencies and corresponding harmonicities are given, we have
 *   inharmonicity = log(fa / fb) / log(ha / hb) - 1
 * which mean that when
 *   - inharmonicity == 0 -> we have the ideal frequency ratio
 *   - inharmonicity  < 0 -> the frequencies are closer together than the ideal frequency ratio.
 *   - inharmonicity  > 0 -> the frequencies are further together than the ideal frequency ratio.
 *
 *   The formula also means that
 *   - inharmonicity ==  1 -> we have twice the ideal stretching
 *   - inharmonicity == -1 -> we have zero stretching
 *
 * @param frequency1 First frequency.
 * @param harmonicNumber1 Harmonic number of first frequency.
 * @param frequency2 Second frequency.
 * @param harmonicNumber2 Harmonic number of second frequency.
 * @return Inharmonicity value.
 */
fun computeInharmonicity(frequency1: Float, harmonicNumber1: Int, frequency2: Float, harmonicNumber2: Int): Float {
    return if (harmonicNumber1 > harmonicNumber2) {
        log(frequency1 / frequency2, harmonicNumber1.toFloat() / harmonicNumber2.toFloat()) - 1
    } else {
        log(frequency2 / frequency1,harmonicNumber2.toFloat() / harmonicNumber1.toFloat()) - 1
    }
}
