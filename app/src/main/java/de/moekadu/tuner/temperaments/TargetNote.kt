package de.moekadu.tuner.temperaments

import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.instrumentDatabase
import kotlin.math.pow

class TargetNote {
    enum class TuningStatus {TooLow, TooHigh, InTune, Unknown}

    /// Tuning frequency class which connects tone indices with frequencies
    var musicalScale: MusicalScale = MusicalScaleFactory.create(TemperamentType.EDO12, null, null, 440f)
        set(value) {
            field = value
            sortedAndDistinctNoteIndices = sortStringsAccordingToNoteIndex(instrument)
            if (frequencyRange[1] > frequencyRange[0]) { // target note is set automatically, so we check if we must change it
                // this will already call "recomputeTargetNoteProperties"
                setTargetNoteBasedOnFrequency(frequencyForLastTargetNoteDetection, ignoreFrequencyRange = true)
            } else { // target note was set manually, so we only recompute the properties
                recomputeTargetNoteProperties(note, toleranceInCents, field)
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



    /** Tolerance in cents for a note to be in tune. */
    var toleranceInCents = 5
        set(value) {
            field = value
            recomputeTargetNoteProperties(note, value, musicalScale)
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
            recomputeTargetNoteProperties(note, toleranceInCents, musicalScale)
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

        // the last entry of the sorted note indices can be Int.MAX_VALUE if not all notes are
        // part of the musical scale.
        val numDifferentNotes = when {
            sortedAndDistinctNoteIndices.isEmpty() -> 0
            sortedAndDistinctNoteIndices.last() == Int.MAX_VALUE -> sortedAndDistinctNoteIndices.size - 1
            else -> sortedAndDistinctNoteIndices.size
        }

        when {
            instrument.isChromatic || numDifferentNotes == 0 -> {
                note = musicalScale.getClosestNote(frequency)
                val noteIndex = musicalScale.getNoteIndex(note)
                frequencyRange[0] = musicalScale.getNoteFrequency(noteIndex - allowedHalfToneDeviationBeforeChangingTarget)
                frequencyRange[1] = musicalScale.getNoteFrequency(noteIndex + allowedHalfToneDeviationBeforeChangingTarget)
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

                frequencyRange[0] = if (uniqueNoteListIndex == 0) {
                    Float.NEGATIVE_INFINITY
                } else {
                    // ok, here the "allowedHalfToneRatio... is rather allowedRatioBetweenTwoNeighboringStrings ...
                    musicalScale.getNoteFrequency(
                        (1.0f - allowedHalfToneDeviationBeforeChangingTarget) * sortedAndDistinctNoteIndices[uniqueNoteListIndex]
                                + allowedHalfToneDeviationBeforeChangingTarget * sortedAndDistinctNoteIndices[uniqueNoteListIndex - 1])
                }

                frequencyRange[1] = if (uniqueNoteListIndex == numDifferentNotes - 1) {
                    Float.POSITIVE_INFINITY
                } else {
                    musicalScale.getNoteFrequency(
                        (1.0f - allowedHalfToneDeviationBeforeChangingTarget) * sortedAndDistinctNoteIndices[uniqueNoteListIndex]
                                + allowedHalfToneDeviationBeforeChangingTarget * sortedAndDistinctNoteIndices[uniqueNoteListIndex + 1]
                    )
                }
                note = musicalScale.getNote(sortedAndDistinctNoteIndices[uniqueNoteListIndex])
            }
        }

        recomputeTargetNoteProperties(note, toleranceInCents, musicalScale)

        return note
    }

    /// Recompute current target status.
    private fun recomputeTargetNoteProperties(note: MusicalNote?, toleranceInCents: Int, musicalScale: MusicalScale) {
        frequency = if (note != null) {
            val noteIndex = musicalScale.getNoteIndex(note)
            musicalScale.getNoteFrequency(noteIndex)
        } else {
            musicalScale.referenceFrequency
        }
        val toleranceRatio = (2.0.pow(toleranceInCents / 1200.0)).toFloat()
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