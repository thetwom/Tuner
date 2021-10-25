package de.moekadu.tuner

import android.content.Context
import android.util.Log
import kotlin.math.roundToInt

class TargetNote {
    enum class TuningStatus {TooLow, TooHigh, InTune, Unknown}

    /// Tuning frequency class which connects tone indices with frequencies
    var tuningFrequencies: TuningFrequencies = TuningEqualTemperament()
        set(value) {
            field = value
            if (frequencyRange[1] > frequencyRange[0]) {
                // this will already call "recomputeTargetNoteProperties"
                setTargetNoteBasedOnFrequency(frequencyForLastTargetNoteDetection, ignoreFrequencyRange = true)
            } else {
                recomputeTargetNoteProperties(toneIndex, toleranceInCents, field)
            }
        }

    var instrument: Instrument = instrumentDatabase[0]
//        set(value) {
//            Log.v("Tuner", "TargetNote.instrument.set: value=$value, field=$field")
//            if (value.stableId != field.stableId) {
//                field = value
//                Log.v("Tuner", "TargetNote.instrument.set: frequencyRange=${frequencyRange[0]} -- ${frequencyRange[1]}")
//                //if (frequencyRange[1] > frequencyRange[0]) {
//                // this will also call "recomputeTargetNoteProperties"
//                //setTargetNoteBasedOnFrequency(frequencyForLastTargetNoteDetection, ignoreFrequencyRange = true)
//                //}
//            }
//        }

    /// Tolerance in cents for a note to be in tune
    var toleranceInCents = 5
        set(value) {
            field = value
            recomputeTargetNoteProperties(toneIndex, value, tuningFrequencies)
        }

    /// Current auto-detect frequency range
    /**
     * - only when a frequency is outside these bounds, we will search for better fitting tone
     * - to unset a range use a larger value for the first value compared to the second value
     */
    private val frequencyRange = floatArrayOf(1000f, 0f)

    /// Frequency used for last target note detection
    /**
     * Negative values unset any previously set frequencies
     */
    private var frequencyForLastTargetNoteDetection = -1f

    /// Current target note index
    var toneIndex = 0
        private set

//    /// Current note name
//    var name: CharSequence? = null
//        private set

    /// Frequency of current target note
    var frequency = 0f
        private set
    /// Upper frequency bound for a note to be in tune
    var frequencyUpperTolerance = 0f

        private set
    /// Lower frequency bound for a note to be in tune
    var frequencyLowerTolerance = 0f
        private set

    /// We need some hysteresis effect, before we changing our current tone estimate
    /**
     * A value of 0.5f would mean that if we exactly between two half tone, we change our pitch, but
     * this would have no hysteresis effect. So better give a value somewhere between 0.5f and 1.0f
     */
    private val allowedHalfToneDeviationBeforeChangingTarget = 0.6f

    /// Return the tuning status of a
    // given frequency.
    /**
     * @param currentFrequency Current frequency which should be rated.
     * @return Tuning status of the given frequency.
     */
    fun getTuningStatus(currentFrequency: Float?) = when {
        currentFrequency == null -> TuningStatus.Unknown
        currentFrequency < frequencyLowerTolerance -> TuningStatus.TooLow
        currentFrequency > frequencyUpperTolerance -> TuningStatus.TooHigh
        else -> TuningStatus.InTune
    }

    fun setToneIndexExplicitly(toneIndex: Int) {
        frequencyRange[0] = 100f
        frequencyRange[1] = -100f
        if (toneIndex != this.toneIndex) {
            this.toneIndex = toneIndex
            recomputeTargetNoteProperties(toneIndex, toleranceInCents, tuningFrequencies)
        }
    }

    fun setTargetNoteBasedOnFrequency(frequency: Float?, ignoreFrequencyRange: Boolean = false): Int {
        if (ignoreFrequencyRange) {
            frequencyRange[0] = 100f
            frequencyRange[1] = -100f
        }

        if (frequency == null)
            return toneIndex

        frequencyForLastTargetNoteDetection = frequency

        if (frequency in frequencyRange[0] .. frequencyRange[1] && !ignoreFrequencyRange)
            return toneIndex

        val numStrings = instrument.strings.size
        if (numStrings == 1) {
            frequencyRange[0] = Float.NEGATIVE_INFINITY
            frequencyRange[1] = Float.POSITIVE_INFINITY
            toneIndex = instrument.strings[0]
        } else if (instrument.type == InstrumentType.Piano) {
            toneIndex = tuningFrequencies.getClosestToneIndex(frequency)
            frequencyRange[0] = tuningFrequencies.getNoteFrequency(toneIndex - allowedHalfToneDeviationBeforeChangingTarget)
            frequencyRange[1] = tuningFrequencies.getNoteFrequency(toneIndex + allowedHalfToneDeviationBeforeChangingTarget)
        } else {
            val exactToneIndex = tuningFrequencies.getToneIndex(frequency)
            var index = instrument.stringsSorted.binarySearch(exactToneIndex)
            if (index < 0)
                index = -(index + 1)

            val stringIndex = when {
                index == 0 -> 0
                index == numStrings -> numStrings - 1
                exactToneIndex - instrument.stringsSorted[index - 1] < instrument.stringsSorted[index] - exactToneIndex -> index - 1
                else -> index
            }

            frequencyRange[0] = if (stringIndex == 0)
                Float.NEGATIVE_INFINITY
            else
                tuningFrequencies.getNoteFrequency(0.4f * instrument.stringsSorted[stringIndex] + 0.6f * instrument.stringsSorted[stringIndex - 1])

            frequencyRange[1] = if (stringIndex == numStrings - 1)
                Float.POSITIVE_INFINITY
            else
                tuningFrequencies.getNoteFrequency(0.4f * instrument.stringsSorted[stringIndex] + 0.6f * instrument.stringsSorted[stringIndex + 1])
            toneIndex = instrument.stringsSorted[stringIndex].roundToInt()
        }

        recomputeTargetNoteProperties(toneIndex, toleranceInCents, tuningFrequencies)

        return toneIndex
    }

    /// Recompute current target status.
    private fun recomputeTargetNoteProperties(toneIndex: Int, toleranceInCents: Int, tuningFrequencies: TuningFrequencies) {
        frequency = tuningFrequencies.getNoteFrequency(toneIndex)
        frequencyLowerTolerance = tuningFrequencies.getNoteFrequency(
            toneIndex - toleranceInCents / 100f)
        frequencyUpperTolerance = tuningFrequencies.getNoteFrequency(
            toneIndex + toleranceInCents / 100f)
//        name = tuningFrequencies.getNoteName(frequency)
    }

    fun getNoteName(context: Context, preferFlat: Boolean): CharSequence {
        return tuningFrequencies.getNoteName(context, toneIndex, preferFlat)
    }
}