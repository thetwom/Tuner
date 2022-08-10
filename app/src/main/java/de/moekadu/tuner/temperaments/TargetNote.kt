package de.moekadu.tuner.temperaments

import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.instrumentDatabase
import de.moekadu.tuner.misc.DefaultValues
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private fun ratioToCents(ratio: Float): Float {
    return (1200.0 * log(ratio.toDouble(), 2.0)).toFloat()
}

private fun centsToRatio(cents: Float): Float {
    return (2.0.pow(cents / 1200.0)).toFloat()
}

class TargetNote {
    enum class TuningStatus {TooLow, TooHigh, InTune, Unknown}

    /// Tuning frequency class which connects tone indices with frequencies
    var musicalScale: MusicalScale = MusicalScaleFactory.create(DefaultValues.TEMPERAMENT, null, null, DefaultValues.REFERENCE_FREQUENCY)
        set(value) {
            field = value
            sortedAndDistinctNoteIndices = sortStringsAccordingToNoteIndex(instrument)
            if (frequencyRange[1] > frequencyRange[0]) { // target note is set automatically, so we check if we must change it
                // this will already call "recomputeTargetNoteProperties"
                setTargetNoteBasedOnFrequency(frequencyForLastTargetNoteDetection, ignoreFrequencyRange = true)
            } else { // target note was set manually, so we only recompute the properties
                recomputeTargetNoteFrequencyAndTolerances(note, toleranceInCents, field)
            }
        }

    var instrument: Instrument = instrumentDatabase[0]
        set(value) {
            field = value
            sortedAndDistinctNoteIndices = sortStringsAccordingToNoteIndex(value)
        }

    private var sortedAndDistinctNoteIndices = sortStringsAccordingToNoteIndex(instrument)
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

    /** Number of different notes.
     * Note that the last entry of the sorted note indices can be Int.MAX_VALUE if not all notes are
     * part of the musical scale.
     */
    private val numDifferentNotes get() = when {
        sortedAndDistinctNoteIndices.isEmpty() -> 0
        sortedAndDistinctNoteIndices.last() == Int.MAX_VALUE -> sortedAndDistinctNoteIndices.size - 1
        else -> sortedAndDistinctNoteIndices.size
    }


    /** Tolerance in cents for a note to be in tune. */
    var toleranceInCents = 5
        set(value) {
            field = value
            recomputeTargetNoteFrequencyAndTolerances(note, value, musicalScale)
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

    /** Current target note. */
    var note: MusicalNote = musicalScale.referenceNote
        private set

    /** Flag which tells if currently a target note is available.
     * Even if no target note is available, we set note and there frequency range to
     * meaningful values, however, if this is true, there are no valid strings.
     */
    val isTargetNoteAvailable: Boolean
        get() = when {
            instrument.isChromatic -> true
            sortedAndDistinctNoteIndices.isEmpty() -> false
            sortedAndDistinctNoteIndices.size == 1 && sortedAndDistinctNoteIndices.last() == Int.MAX_VALUE -> false
            else -> true
        }

    /** Flag which tells if there are strings, which are not part of the musical scale. */
    val hasStringsNotPartOfMusicalScale:Boolean
        get() = when {
            instrument.isChromatic -> false
            sortedAndDistinctNoteIndices.isEmpty() -> false
            sortedAndDistinctNoteIndices.last() == Int.MAX_VALUE -> true
            else -> false
        }

    /** Frequency of current target note. */
    var frequency = 0f
        private set
    /** Upper frequency bound for a note to be in tune. */
    var frequencyUpperTolerance = 0f
        private set
    /** Lower frequency bound for a note to be in tune. */
    var frequencyLowerTolerance = 0f
        private set

    /** We need some hysteresis effect, before we changing our current tone estimate
     * A value of 0.5f would mean that if we exactly between two possible target note,
     * we change our pitch, but this would have no hysteresis effect. So better give
     * a value somewhere between 0.5f and 1.0f
     */
    private val relativeDeviationForChangingTarget = 0.6f

    /** Minimum cent deviation before for changing target.
     * Basically, the relativeDeviationForChangingTarget defines at which point we should
     * jump to next target note. However, for target notes which are extremely close
     * (e.g. when scale is EDO 41 and we use chromatic target notes), this can lead to
     * high-frequency jumps between target note. So, here we can give a minimum ratio
     * such that we must be closer to the next note that the relativeDeviationForChangingTarget
     * defines. E.g. by setting it to 20cents, we don't change to a new target note if
     * we are not more than 20 cents away. However, this value is reduced to
     * "target note ratio - tolerance". So if two target notes are only 15cents apart
     * and the tolerance is 5cents, the real minimumCentDeviationBeforeChangingTone
     * is 15-5 = 10cents.
     */
    private val minimumCentDeviationBeforeChangingTarget = 20f

    /** Return the tuning status of a given frequency.
     *
     * @param currentFrequency Current frequency which should be rated.
     * @return Tuning status of the given frequency.
     */
    fun getTuningStatus(currentFrequency: Float?) = when {
        currentFrequency == null -> TuningStatus.Unknown
        !isTargetNoteAvailable -> TuningStatus.InTune
        currentFrequency < frequencyLowerTolerance -> TuningStatus.TooLow
        currentFrequency > frequencyUpperTolerance -> TuningStatus.TooHigh
        else -> TuningStatus.InTune
    }

    /** Set the target note explicitly.
     *
     * @param note The target note.
     */
    fun setNoteExplicitly(note: MusicalNote) {
        frequencyRange[0] = 100f
        frequencyRange[1] = -100f
        if (note != this.note) {
            this.note = note
            recomputeTargetNoteFrequencyAndTolerances(note, toleranceInCents, musicalScale)
        }
    }

    /** Set target note based on a given frequency
     *
     * @param frequency The frequency for which we should set the target note
     * @param ignoreFrequencyRange If this is true, we will set note which is closest to the frequency
     *   if it is false, we only switch to a new note if we are very clearly closer to another note
     *   (allowedHalfToneDeviationBeforeTarget).
     * @return Current target note
     */
    fun setTargetNoteBasedOnFrequency(frequency: Float?, ignoreFrequencyRange: Boolean = false): MusicalNote {
        if (ignoreFrequencyRange) {
            frequencyRange[0] = 100f
            frequencyRange[1] = -100f
        }

        if (frequency == null)
            return note

        frequencyForLastTargetNoteDetection = frequency

        if (frequency in frequencyRange[0] .. frequencyRange[1] && !ignoreFrequencyRange)
            return note
//        Log.v("Tuner", "TargetNote: setTargetNoteBasedOnFrequency: instrument.isChromatic: ${instrument.isChromatic} || numDifferentNotes==$numDifferentNotes")
        when {
            instrument.isChromatic || numDifferentNotes == 0 -> {
                note = musicalScale.getClosestNote(frequency)
                val noteIndex = musicalScale.getNoteIndex(note)
                setFrequencyRangeForChromaticTarget(noteIndex)
            }
            numDifferentNotes == 1 -> {
                frequencyRange[0] = Float.NEGATIVE_INFINITY
                frequencyRange[1] = Float.POSITIVE_INFINITY
                note = musicalScale.getNote(sortedAndDistinctNoteIndices[0])
            }
            else -> {
                val exactNoteIndex = musicalScale.getNoteIndex(frequency)
                var index = sortedAndDistinctNoteIndices.binarySearchBy(exactNoteIndex) { it.toFloat() }
                //var index = instrument.stringsSorted.binarySearch(exactNoteIndex)
                if (index < 0)
                    index = -(index + 1)

                val uniqueNoteListIndex = when {
                    index == 0 -> 0
                    index >= numDifferentNotes -> numDifferentNotes - 1
                    //exactNoteIndex - instrument.stringsSorted[index - 1] < instrument.stringsSorted[index] - exactNoteIndex -> index - 1
                    exactNoteIndex - sortedAndDistinctNoteIndices[index - 1] < sortedAndDistinctNoteIndices[index] - exactNoteIndex -> index - 1
                    else -> index
                }
                setFrequencyRangeForInstrumentTarget(uniqueNoteListIndex)
                note = musicalScale.getNote(sortedAndDistinctNoteIndices[uniqueNoteListIndex])
            }
        }

        recomputeTargetNoteFrequencyAndTolerances(note, toleranceInCents, musicalScale)

        return note
    }

    /** Set frequencyRange for targetNoteIndex.
     *
     * Lets say, we have the following scenario:
     *
     * M1: ========= frequency of next target note I + 1 (frequencyOfUpperTarget) ==============
     *
     * M2: ------- frequency bound of target note I + 1 (frequencyOfUpperTarget - tolerance) ---
     *
     *
     * M3: ============ frequency of current target note (centerFrequency) ====================
     *
     *
     *  M4: ------- frequency bound of target note I - 1 (frequencyOfLowerTarget + tolerance) ---
     *
     * M5: ========= frequency of next target note I - 1 (frequencyOfLowerTarget) ==============
     *
     * Then we define two frequency ranges:
     * - A deviation based frequency range (e.g. range is 60% of space between center and upper/lower target.)
     * - A cent based frequency range (e.g. range is 20 cents away from center).
     * Which range to use? We use the larger one, BUT for the cent-based frequency range, we have
     * one condition:
     * - It is not allowed to exceed the frequency bound of target note I +/- 1 (M2/M4 in the picture)
     */
    private fun setFrequencyRangeForChromaticTarget(targetNoteIndex: Int) {

        val centerFrequency = musicalScale.getNoteFrequency(targetNoteIndex)

        val lowerFrequencyDeviationBased = musicalScale.getNoteFrequency(targetNoteIndex - relativeDeviationForChangingTarget)
        val upperFrequencyDeviationBased = musicalScale.getNoteFrequency(targetNoteIndex + relativeDeviationForChangingTarget)

        val frequencyOfLowerTarget = musicalScale.getNoteFrequency(targetNoteIndex - 1)
        val frequencyOfUpperTarget = musicalScale.getNoteFrequency(targetNoteIndex + 1)

        // in the picture above these are the cents between M3 and M4, as well as between M3 and M2
        val centsToLowerTargetBound = ratioToCents(centerFrequency / frequencyOfLowerTarget) - toleranceInCents
        val centsToUpperTargetBound = ratioToCents(frequencyOfUpperTarget / centerFrequency) - toleranceInCents

        val lowerFrequencyCentBased = centerFrequency / centsToRatio(min(minimumCentDeviationBeforeChangingTarget, centsToLowerTargetBound))
        val upperFrequencyCentBased = centerFrequency * centsToRatio(min(minimumCentDeviationBeforeChangingTarget, centsToUpperTargetBound))

        frequencyRange[0] = min(lowerFrequencyDeviationBased, lowerFrequencyCentBased)
        frequencyRange[1] = max(upperFrequencyDeviationBased, upperFrequencyCentBased)
    }

    /** See description for setFrequencyRangeForInstrumentTarget for details.
     *
     */
    private fun setFrequencyRangeForInstrumentTarget(sortedStringListIndex: Int) {

        val centerFrequency = musicalScale.getNoteFrequency(sortedAndDistinctNoteIndices[sortedStringListIndex])

        frequencyRange[0] = if (sortedStringListIndex == 0) {
            Float.NEGATIVE_INFINITY
        } else {
            val lowerFrequencyDeviationBased = musicalScale.getNoteFrequency(
                (1.0f - relativeDeviationForChangingTarget) * sortedAndDistinctNoteIndices[sortedStringListIndex]
                        + relativeDeviationForChangingTarget * sortedAndDistinctNoteIndices[sortedStringListIndex - 1])
            val frequencyOfLowerTarget = musicalScale.getNoteFrequency(sortedAndDistinctNoteIndices[sortedStringListIndex - 1])
            // in the picture above these are the cents between M3 and M4, as well as between M3 and M2
            val centsToLowerTargetBound = ratioToCents(centerFrequency / frequencyOfLowerTarget) - toleranceInCents
            val lowerFrequencyCentBased = centerFrequency / centsToRatio(min(minimumCentDeviationBeforeChangingTarget, centsToLowerTargetBound))
            min(lowerFrequencyDeviationBased, lowerFrequencyCentBased)
        }

        frequencyRange[1] = if (sortedStringListIndex == numDifferentNotes - 1) {
            Float.POSITIVE_INFINITY
        } else {
            val upperFrequencyDeviationBased = musicalScale.getNoteFrequency(
                (1.0f - relativeDeviationForChangingTarget) * sortedAndDistinctNoteIndices[sortedStringListIndex]
                        + relativeDeviationForChangingTarget * sortedAndDistinctNoteIndices[sortedStringListIndex + 1]
            )
            val frequencyOfUpperTarget = musicalScale.getNoteFrequency(sortedAndDistinctNoteIndices[sortedStringListIndex + 1])
            val centsToUpperTargetBound = ratioToCents(frequencyOfUpperTarget / centerFrequency) - toleranceInCents
            val upperFrequencyCentBased = centerFrequency * centsToRatio(min(minimumCentDeviationBeforeChangingTarget, centsToUpperTargetBound))
            max(upperFrequencyDeviationBased, upperFrequencyCentBased)
        }
    }

    /// Recompute current target status.
    private fun recomputeTargetNoteFrequencyAndTolerances(note: MusicalNote?, toleranceInCents: Int, musicalScale: MusicalScale) {
        frequency = if (note != null) {
            val noteIndex = musicalScale.getNoteIndex(note)
            musicalScale.getNoteFrequency(noteIndex)
        } else {
            musicalScale.referenceFrequency
        }
        val toleranceRatio = centsToRatio(toleranceInCents.toFloat())
        frequencyLowerTolerance = frequency / toleranceRatio
        frequencyUpperTolerance = frequency * toleranceRatio
    }

    //private fun sortStringsAccordingToFrequency(instrument: Instrument): Array<FrequencyAndNote> {
    private fun sortStringsAccordingToNoteIndex(instrument: Instrument): List<Int> {
        if (instrument.isChromatic)
            return ArrayList()
        val strings = instrument.strings
        val noteIndices = strings.map { musicalScale.getNoteIndex(it) }.distinct().sorted()

        return noteIndices
    }
//    fun getNoteName(context: Context, preferFlat: Boolean): CharSequence {
//        return tuningFrequencies.getNoteName(context, toneIndex, preferFlat)
//    }
}