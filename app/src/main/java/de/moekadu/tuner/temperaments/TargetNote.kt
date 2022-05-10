package de.moekadu.tuner.temperaments

import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.instrumentDatabase
import kotlin.math.pow
import kotlin.math.roundToInt

class TargetNote {
    enum class TuningStatus {TooLow, TooHigh, InTune, Unknown}

    /// Tuning frequency class which connects tone indices with frequencies
    var temperamentFrequencies: TemperamentFrequencies = TemperamentFactory.create(Temperament.EDO12, -9, 0, 440f)
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
            recomputeTargetNoteProperties(toneIndex, value, temperamentFrequencies)
        }

    /** Current auto-detect frequency range
     * - only when a frequency is outside these bounds, we will search for better fitting tone
     * - to unset a range use a larger value for the first value compared to the second value
     */
    private val frequencyRange = floatArrayOf(1000f, 0f)

    /** Frequency used for last target note detection
     * Negative values unset any previously set frequencies
     */
    private var frequencyForLastTargetNoteDetection = -1f

    /// Current target note index
    var toneIndex = 0
        private set

    /// String index of target note
    var stringIndex = -1

    /// Frequency of current target note
    var frequency = 0f
        private set
    /// Upper frequency bound for a note to be in tune
    var frequencyUpperTolerance = 0f

        private set
    /// Lower frequency bound for a note to be in tune
    var frequencyLowerTolerance = 0f
        private set

    /** We need some hysteresis effect, before we changing our current tone estimate
     *
     * A value of 0.5f would mean that if we exactly between two half tone, we change our pitch, but
     * this would have no hysteresis effect. So better give a value somewhere between 0.5f and 1.0f
     */
    private val allowedHalfToneDeviationBeforeChangingTarget = 0.6f

    /** Return the tuning status of a given frequency.
     *
     * @param currentFrequency Current frequency which should be rated.
     * @return Tuning status of the given frequency.
     */
    fun getTuningStatus(currentFrequency: Float?) = when {
        currentFrequency == null -> TuningStatus.Unknown
        currentFrequency < frequencyLowerTolerance -> TuningStatus.TooLow
        currentFrequency > frequencyUpperTolerance -> TuningStatus.TooHigh
        else -> TuningStatus.InTune
    }

    /** Set the tone index of the target note explicitly.
     *
     * @param toneIndex The tone index which should be set.
     */
    fun setToneIndexExplicitly(toneIndex: Int) {
        frequencyRange[0] = 100f
        frequencyRange[1] = -100f
        if (toneIndex != this.toneIndex) {
            this.toneIndex = toneIndex
            recomputeTargetNoteProperties(toneIndex, toleranceInCents, temperamentFrequencies)
        }
    }

    /** Set target note based on a given frequency
     *
     * @param frequency The frequency for which we should set the target note
     * @param ignoreFrequencyRange If this is true, we will set note which is closest to the frequency
     *   if it is false, we only switch to a new note if we are very clearly closer to another note
     *   (allowedHalfToneDeviationBeforeTarget).
     */
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
        when {
            numStrings == 1 -> {
                frequencyRange[0] = Float.NEGATIVE_INFINITY
                frequencyRange[1] = Float.POSITIVE_INFINITY
                toneIndex = instrument.strings[0]
            }
            instrument.isChromatic -> {
                toneIndex = temperamentFrequencies.getClosestToneIndex(frequency)
                frequencyRange[0] = temperamentFrequencies.getNoteFrequency(toneIndex - allowedHalfToneDeviationBeforeChangingTarget)
                frequencyRange[1] = temperamentFrequencies.getNoteFrequency(toneIndex + allowedHalfToneDeviationBeforeChangingTarget)
            }
            else -> {
                val exactToneIndex = temperamentFrequencies.getToneIndex(frequency)
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
                    temperamentFrequencies.getNoteFrequency(0.4f * instrument.stringsSorted[stringIndex] + 0.6f * instrument.stringsSorted[stringIndex - 1])

                frequencyRange[1] = if (stringIndex == numStrings - 1)
                    Float.POSITIVE_INFINITY
                else
                    temperamentFrequencies.getNoteFrequency(0.4f * instrument.stringsSorted[stringIndex] + 0.6f * instrument.stringsSorted[stringIndex + 1])
                toneIndex = instrument.stringsSorted[stringIndex].roundToInt()
            }
        }

        recomputeTargetNoteProperties(toneIndex, toleranceInCents, temperamentFrequencies)

        return toneIndex
    }

    /// Recompute current target status.
    private fun recomputeTargetNoteProperties(toneIndex: Int, toleranceInCents: Int, temperamentFrequencies: TemperamentFrequencies) {
        frequency = temperamentFrequencies.getNoteFrequency(toneIndex)
        val toleranceRatio = (2.0.pow(toleranceInCents / 1200.0)).toFloat()
        frequencyLowerTolerance = frequency / toleranceRatio
        frequencyUpperTolerance = frequency * toleranceRatio
    }

//    fun getNoteName(context: Context, preferFlat: Boolean): CharSequence {
//        return tuningFrequencies.getNoteName(context, toneIndex, preferFlat)
//    }
}