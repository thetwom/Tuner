package de.moekadu.tuner.temperaments

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Store the frequencies of a musical scale over all octaves.
 * @param frequencies All frequencies of the musical scale over all octaves.
 * @param indexOfReferenceNote Index where reference frequency is stored. Note that this index
 *   can be outside the frequencies array, if the reference note is outside the allowed octave
 *   range.
 */
@Immutable
@Serializable
class MusicalScaleFrequencies(
    private val frequencies: FloatArray,
    private val indexOfReferenceNote: Int
) {
    /** First available frequency index (included). */
    val indexStart = -indexOfReferenceNote
    /** End index of available frequencies (excluded). */
    val indexEnd = indexStart + frequencies.size

    /** Obtain frequency of given index.
     * @param index Note index (indexStart <= index < indexEnd)
     * @return Frequency at given index.
     */
    operator fun get(index: Int): Float {
        return frequencies[index + indexOfReferenceNote]
    }

    /** Obtain frequency by float index.
     * If indices are not integer numbers, the values are interpolated logarithmically.
     * This function also supports "out of bounds" access by extrapolation.
     * @param index Note index.
     * @return Frequency of given note index.
     */
    operator fun get(index: Float): Float {
        assert(indexStart < indexEnd)

        // noteIndex = closestNoteIndex +/- log10(frequencies[j] / frequency) / log10(frequencies[j] / frequencies[j +/- 1])
        // (noteIndex - closestNoteIndex) * log10(frequencies[j] / frequencies[j +/- 1]) = +/- log10(frequencies[j] / frequency)
        // 10**( +/- (noteIndex - closestNoteIndex) * log10(frequencies[j] / frequencies[j +/- 1])) = frequencies[j] / frequency
        // frequency = frequencies[i] * 10**( -/+ (noteIndex - closestNoteIndex) * log10(frequencies[j] / frequencies[j +/- 1]))
        // frequency = frequencies[i] * (frequencies[j] / frequencies[j +/- 1])**( -/+ (noteIndex - closestNoteIndex)
        val noteIndexLower = floor(index).toInt()
        return when {
            noteIndexLower < indexStart -> {
                get(indexStart) * (get(indexStart + 1) / get(indexStart)).pow(index - indexStart)
            }
            noteIndexLower >= indexEnd - 1 -> {
                get(indexEnd - 1) * (get(indexEnd - 1) / get(indexEnd -2)).pow(
                    index - (indexEnd - 1)
                )
            }
            else -> {
                get(noteIndexLower) * (get(noteIndexLower + 1) / get(noteIndexLower)).pow(
                    index - noteIndexLower
                )
            }
        }
    }

    /** Get frequency index for a given frequency.
     * The returned value is a float, since this also allows to get indices in between of two
     * frequencies. In order to get integer values, use getClosestFrequencyIndex().
     * @param frequency Frequency.
     * @return Index of the frequency, relative to the reference note.
     */
    fun getFrequencyIndex(frequency: Float): Float {
        if (frequencies.isEmpty())
            return 0f

        val closestIndex = getClosestFrequencyIndex(frequency)
        val closestFrequency = get(closestIndex)
        //val j = closestIndex - indexStart
        return if (frequency == closestFrequency || frequencies.size == 1)
            closestIndex.toFloat()
        else if (closestIndex > indexStart && (frequency < closestFrequency || closestIndex == indexEnd -1))
            closestIndex - log10(closestFrequency / frequency) / log10(closestFrequency / get(closestIndex - 1))
        else if (closestIndex < indexEnd - 1 && (frequency > closestFrequency || closestIndex == indexStart))
            closestIndex + log10(closestFrequency / frequency) / log10(closestFrequency / get(closestIndex + 1))
        else
            throw RuntimeException("cannot find tone index")
    }
    /** Get closest note index to a given frequency.
     * @param frequency Frequency.
     * @return Note index which corresponds to the closest available frequency.
     */
    fun getClosestFrequencyIndex(frequency: Float): Int {
        if (frequencies.isEmpty())
            return 0
        val index = frequencies.binarySearch(frequency)
        if (index >= 0)
            return index - indexOfReferenceNote
        val indexAfter = -index - 1
        val indexBefore = indexAfter - 1

        // Avoid accessing frequency array out of bounds
        if (indexAfter <= 0)
            return -indexOfReferenceNote
        if (indexAfter >= frequencies.size)
            return frequencies.size - 1 - indexOfReferenceNote

        // the sorting is as follows:
        // frequency at indexBefore < frequency < frequency at indexAfter
        // we want to check what is closest based on log scale
        return if (log10(frequency / frequencies[indexBefore]) < log10(frequencies[indexAfter] / frequency))
            indexBefore - indexOfReferenceNote
        else
            indexAfter - indexOfReferenceNote
    }

    companion object {
        /** Compute frequencies of a temperament over as many octaves as possible.
         * @param cents Temperament cent values. This must include also the octave value, meaning
         *   e.g. for edo-12: (0, 100, 200, 300, ..., 1100, 1200)
         * @param referenceIndexWithinOctave Index within the cents array, where the reference
         *   frequency is defined. E.g. for standard 12-tone scale where the reference note A
         *   has index 10, you would use 10.
         * @param referenceFrequency Reference frequency, which will match exactly at the note
         *   at position referenceIndexWithinOctave. E.g. at standard 12-tone scales, if index
         *   10 is given for note A, you could give 440 Hz here.
         * @param frequencyMin Collecting frequencies will end at this frequency, so there will
         *   be no frequency smaller than this value.
         * @param frequencyMax Collecting frequencies will end at this frequency, so there will
         *   be no frequency higher than this value.
         * @return Array with frequencies of the temperament over as many octaves as possible,
         *   between frequencyMin and frequencyMax.
         */
        fun create(
            cents: DoubleArray,
            referenceIndexWithinOctave: Int,
            referenceFrequency: Float,
            frequencyMin: Float,
            frequencyMax: Float,
            stretchTuning: StretchTuning
        ): MusicalScaleFrequencies {
//            cents.forEachIndexed { index, d ->
//                Log.v("Tuner", "MusicalScaleFrequencies.create: cent at index $index = $d")
//            }
            val numberOfNotesPerOctave: Int = cents.size - 1
            val centsReference = cents[referenceIndexWithinOctave]

            // first collect all frequencies starting from reference up to highest frequency
            var octaveWiseCentCounter = 0.0 // is increased at each octave jump
            var indexWithinOfOctave = referenceIndexWithinOctave
            val higherFrequencies = ArrayList<Double>()
            var globalIndexOfReferenceHigh = 0
            var currentFrequency = stretchTuning.getStretchedFrequency(referenceFrequency.toDouble())
            while (currentFrequency < frequencyMax) {
//                Log.v("Tuner", "MusicalScaleFrequencies.create: current f=$currentFrequency, max freq=$frequencyMax")
                // condition is needed when reference frequency is outside min/max
                if (currentFrequency > frequencyMin) {
                    higherFrequencies.add(currentFrequency)
                } else {
                    --globalIndexOfReferenceHigh
                }
                ++indexWithinOfOctave
                if (indexWithinOfOctave == numberOfNotesPerOctave) {
                    indexWithinOfOctave = 0
                    octaveWiseCentCounter += cents.last() - cents.first()
                }
                val centsNext =
                    octaveWiseCentCounter + cents[indexWithinOfOctave] - centsReference
                val frequency = centsToFrequency(centsNext, referenceFrequency.toDouble())
                currentFrequency = stretchTuning.getStretchedFrequency(frequency)
            }

            // second collect all frequencies starting from reference down to lowest frequency
            octaveWiseCentCounter = 0.0 // is decreased at each octave jump
            indexWithinOfOctave = referenceIndexWithinOctave
            val lowerFrequencies = ArrayList<Double>()
            var globalIndexOfReferenceLow = 0
            currentFrequency = stretchTuning.getStretchedFrequency(referenceFrequency.toDouble())
            while (currentFrequency > frequencyMin) {
//                Log.v("Tuner", "MusicalScaleFrequencies.create: current f=$currentFrequency, min freq=$frequencyMin")
                // condition is needed when reference frequency is outside min/max
                if (currentFrequency < frequencyMax) {
                    lowerFrequencies.add(currentFrequency)
                } else {
                    --globalIndexOfReferenceLow
                }
                --indexWithinOfOctave
                if (indexWithinOfOctave < 0) {
                    indexWithinOfOctave = numberOfNotesPerOctave - 1
                    octaveWiseCentCounter -= cents.last() - cents.first()
                }
                val centsNext =
                    octaveWiseCentCounter + cents[indexWithinOfOctave] - centsReference
                val frequency = centsToFrequency(centsNext, referenceFrequency.toDouble())
                currentFrequency = stretchTuning.getStretchedFrequency(frequency)
            }

            // store all frequencies in a common array and track index of reference frequency
            val scaleFrequencies = if (higherFrequencies.size == 0) {
                MusicalScaleFrequencies(
                    lowerFrequencies.reversed().map { it.toFloat() }.toFloatArray(),
                    lowerFrequencies.size - 1 - globalIndexOfReferenceLow
                )
            } else if (lowerFrequencies.size == 0) {
                MusicalScaleFrequencies(
                    higherFrequencies.map { it.toFloat() }.toFloatArray(),
                    globalIndexOfReferenceHigh
                )
            } else {
                val freqArray = FloatArray(higherFrequencies.size + lowerFrequencies.size - 1)
                lowerFrequencies.forEachIndexed { index, f ->
                    freqArray[lowerFrequencies.size - index - 1] = f.toFloat()
                }
                higherFrequencies.forEachIndexed { index, f ->
                    freqArray[lowerFrequencies.size - 1 + index] = f.toFloat()
                }
                MusicalScaleFrequencies(
                    freqArray,
                    lowerFrequencies.size - 1
                )
            }

            if (scaleFrequencies.indexOfReferenceNote in scaleFrequencies.frequencies.indices) {
                scaleFrequencies.frequencies[scaleFrequencies.indexOfReferenceNote] = referenceFrequency
            }

            return scaleFrequencies
        }
    }
}

