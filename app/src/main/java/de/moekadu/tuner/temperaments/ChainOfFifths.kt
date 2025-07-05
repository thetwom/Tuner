package de.moekadu.tuner.temperaments

import kotlinx.serialization.Serializable

/** Chain of fifths definition of a temperament.
 * @param fifths Array of fifths. Size is expected to be notes per octave - 1.
 *   This means that you must not include a closing modification to make circle of fifths.
 * @param rootIndex Index of fifths, where we start the temperament (position of not with ratio 1)
 */
@Serializable
data class ChainOfFifths(
    val fifths: Array<out FifthModification>,
    val rootIndex: Int
) {
    /** If you want to create a circle of fifths, you can close the circle with the returned
     * modification.
     * This makes e.g. sense for 12 notes per octave.
     */
    fun getClosingCircleCorrection(): FifthModification {
        var totalCorrection = FifthModification(
            pythagoreanComma = RationalNumber(-1, 1)
        )
        for (fifth in fifths)
            totalCorrection -= fifth
        return totalCorrection
    }

    /** Return the ratio between the root note and another note.
     * Ratio are divided/multiplied with a multiple of 2 (2^n) such that it is between 1 and 2.
     * @return Ratios in the same order as given by the fifths. Note that the size of the returned
     *   array is fifths.size + 1 since the fifths are always between two notes. So in the end
     *   the returned size corresponds to number of notes per octave. The ratio of the root note
     *   (which is always 1) is placed at rootIndex.
     */
    fun getRatiosAlongFifths(): DoubleArray {
        //               |
        //               v
        //   Ab Eb Bb F  C  G  D  A  E  B F# C#  (G#)
        //    \  \  \  \  \  \  \  \  \  \  \ (\)  (\)
        //     0  1  2  3  4  5  6  7  8  9 10 (11) (12)
        val ratios = DoubleArray(fifths.size + 1)
        var totalCorrection = FifthModification()
        ratios[rootIndex] = 1.0
        var fifthRatio = RationalNumber(1, 1)

        val threeHalf = RationalNumber(3, 2)

        for (i in rootIndex until fifths.size) {
            totalCorrection += fifths[i]
            fifthRatio *= threeHalf
            if (fifthRatio.numerator > 2 * fifthRatio.denominator)
                fifthRatio /= 2
            ratios[i + 1] = fifthRatio.toDouble() * totalCorrection.toDouble()
        }

        fifthRatio = RationalNumber(1, 1)
        totalCorrection = FifthModification()
        for (i in rootIndex-1 downTo  0) {
            totalCorrection -= fifths[i]
            fifthRatio /= threeHalf
            if (fifthRatio.numerator < fifthRatio.denominator)
                fifthRatio *= 2
            ratios[i] = fifthRatio.toDouble() * totalCorrection.toDouble()
        }
        return ratios
    }

    /** Return the ratios sorted according to the ratios value.
     * See getRatiosAlongFifths for details.
     */
    fun getSortedRatios(): DoubleArray {
        return getRatiosAlongFifths().sortedArray()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChainOfFifths

        if (rootIndex != other.rootIndex) return false
        if (!fifths.contentEquals(other.fifths)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rootIndex
        result = 31 * result + fifths.contentHashCode()
        return result
    }

    companion object {
        fun create(vararg fifths: FifthModification, rootIndex: Int): ChainOfFifths {
            return ChainOfFifths(fifths, rootIndex)
        }
    }
}
