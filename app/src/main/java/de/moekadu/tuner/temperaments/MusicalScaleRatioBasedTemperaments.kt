package de.moekadu.tuner.temperaments

import kotlin.math.*

/** Base class for temperaments which are based on given frequency ratios in respect to a root note.
 * @param temperamentType Type of temperament
 * @param ratios Ratio for each note of the scale in respect to a root note, the last value must be
 *   the ratio of the octave. So for a 12-note scale, 13 values must be supplied. Example:
 *      C      C#     D     ...   B     C'
 *     [1.0,  1.012, 1.032, ..., 1.94, 2.0]
 * @param noteNameScale Note names of the scale, which also maps note indices to note names.
 * @param referenceNote Note which should have the referenceFrequency.
 * @param referenceFrequency Frequency of reference note.
 * @param rootNote Root note of temperament. the First ratio of the ratios-array refers to this
 *   note.
 * @param frequencyMin Minimum frequency to be covered by the scale.
 * @param frequencyMax Maximum frequency to be covered by the scale.
 * @param circleOfFifths Info about circle of fifths if available.
 * @param rationalNumberRatios Info about rational number ratios if available.
 */
open class MusicalScaleRatioBasedTemperaments(
    final override val temperamentType: TemperamentType,
    private val ratios: DoubleArray,
    final override val noteNameScale: NoteNameScale,
    final override val referenceNote: MusicalNote,
    final override val referenceFrequency: Float = 440f,
    final override val rootNote: MusicalNote = MusicalNote(BaseNote.C, NoteModifier.None), // the first ratio of ratios is set at this index (12-tone this is c)
    frequencyMin: Float = 16.0f, // this would be c0 if the noteIndexAtReferenceFrequency is 0 (~16.4Hz for equal temperament)
    frequencyMax: Float = 17000.0f, // this would be c10 if the noteIndexAtReferenceFrequency is 0 (~16744Hz for equal temperament)
    final override val circleOfFifths: TemperamentCircleOfFifths? = null,
    final override val rationalNumberRatios: Array<RationalNumber>? = null)
    : MusicalScale {

    final override val noteIndexBegin: Int
    final override val noteIndexEnd: Int
    final override val numberOfNotesPerOctave: Int = ratios.size - 1

    /** Array of all frequencies over all octaves, covered by this class */
    private var frequencies = FloatArray(0)

    /** Note index of reference note. */
    private val referenceNoteIndex: Int

    /** Special constructor for temperaments based on circle of fifths. */
    constructor(
        temperamentType: TemperamentType,
        circleOfFifths: TemperamentCircleOfFifths,
        noteNameScale: NoteNameScale,
        referenceNote: MusicalNote,
        referenceFrequency: Float,
        rootNote: MusicalNote,
        frequencyMin: Float,
        frequencyMax: Float
    ) : this(
        temperamentType,
        circleOfFifths.getRatios(),
        noteNameScale,
        referenceNote,
        referenceFrequency,
        rootNote,
        frequencyMin,
        frequencyMax,
        circleOfFifths,
        null
    )

    /** Special constructor for tempereraments based on rational number ratios. */
    constructor(
        temperamentType: TemperamentType,
        rationalNumbers: Array<RationalNumber>,
        noteNameScale: NoteNameScale,
        referenceNote: MusicalNote,
        referenceFrequency: Float,
        rootNote: MusicalNote,
        frequencyMin: Float,
        frequencyMax: Float
    ) : this(
        temperamentType,
        rationalNumbers.map {it.toDouble()}.toDoubleArray(),
        noteNameScale,
        referenceNote,
        referenceFrequency,
        rootNote,
        frequencyMin,
        frequencyMax,
        null,
        rationalNumbers
    )

    init {
        //require(rootNoteIndex >= 0)
        //require(rootNoteIndex < ratios.size - 1)
        val rootNoteCopy = rootNote.copy(octave = referenceNote.octave)
        val rootNoteIndex = noteNameScale.getIndexOfNote(rootNoteCopy)
        require(rootNoteIndex >= 0)
        referenceNoteIndex = noteNameScale.getIndexOfNote(referenceNote)
        require(referenceNoteIndex >= 0)

        val octaveRatio = ratios.last() / ratios.first()
        var ratioIndexOfReferenceNote = (referenceNoteIndex - rootNoteIndex) % numberOfNotesPerOctave
        if (ratioIndexOfReferenceNote < 0)
            ratioIndexOfReferenceNote += numberOfNotesPerOctave

        // recompute ratios such that the referenceNote comes first
        val ratiosRef = DoubleArray(numberOfNotesPerOctave) {
            if (it + ratioIndexOfReferenceNote < ratios.size)
                (ratios[it + ratioIndexOfReferenceNote] / ratios[ratioIndexOfReferenceNote])
            else
                (octaveRatio * ratios[it + ratioIndexOfReferenceNote - numberOfNotesPerOctave] / ratios[ratioIndexOfReferenceNote])
        }

        val positiveIndices = ArrayList<Float>()
        val negativeIndices = ArrayList<Float>()

        var noteIndexBeginTmp = Int.MAX_VALUE
        var noteIndexEndTmp = Int.MIN_VALUE

        var i = 0
        while (true) {
            val f = computeNoteFrequency(i, ratiosRef, octaveRatio)
            if (f > frequencyMax) {
                break
            } else if (f >= frequencyMin) {
                positiveIndices.add(f)
                noteIndexEndTmp = i + 1
                // the min means, that we only set this at the first loop cycle
                noteIndexBeginTmp = min(i, noteIndexBeginTmp)
            }
            ++i
        }

        i = -1
        while (true) {
            val f = computeNoteFrequency(i, ratiosRef, octaveRatio)
            if (f < frequencyMin) {
                break
            } else if (f <= frequencyMax) {
                negativeIndices.add(f)
                noteIndexBeginTmp = i
                // the max means, that we only set this at the first loop cycle
                noteIndexEndTmp = max(i + 1, noteIndexEndTmp)
            }
            --i
        }

        noteIndexBegin = noteIndexBeginTmp
        noteIndexEnd = noteIndexEndTmp
        frequencies = FloatArray(negativeIndices.size + positiveIndices.size)
        negativeIndices.reversed().forEachIndexed { index, f -> frequencies[index] = f }
        positiveIndices.forEachIndexed { index, f -> frequencies[negativeIndices.size + index] = f }
//        Log.v("Tuner", "TuningRatioBased.init: end")
    }

    override fun getNoteIndex(frequency: Float): Float {
        if (frequencies.isEmpty())
            return 0f
        if (frequency <= frequencies.first())
            return noteIndexBegin.toFloat()
        else if (frequency >= frequencies.last())
            return (noteIndexEnd - 1).toFloat()

        val closestToneIndex = getClosestNoteIndex(frequency)
        val j = closestToneIndex - noteIndexBegin
        return if (frequency == frequencies[j] || frequencies.size == 1)
            closestToneIndex.toFloat()
        else if (j > 0 && (frequency < frequencies[j] || j == frequencies.size - 1))
            closestToneIndex - log10(frequencies[j] / frequency) / log10(frequencies[j] / frequencies[j - 1])
        else if (j < frequencies.size - 1 && (frequency > frequencies[j] || closestToneIndex == 0))
            closestToneIndex + log10(frequencies[j] / frequency) / log10(frequencies[j] / frequencies[j + 1])
        else
            throw RuntimeException("cannot find tone index")
    }

    override fun getNoteIndex(note: MusicalNote): Int {
        return noteNameScale.getIndexOfNote(note)
    }

    override fun getClosestNoteIndex(frequency: Float): Int {
        if (frequencies.isEmpty())
            return 0
        val index = frequencies.binarySearch(frequency)
        if (index >= 0)
            return index + noteIndexBegin
        val indexAfter = -index - 1
        val indexBefore = indexAfter - 1

        // Avoid accessing frequency array out of bounds
        if (indexAfter <= 0)
            return noteIndexBegin
        if (indexAfter >= frequencies.size)
            return noteIndexEnd - 1

        // the sorting is as follows:
        // frequency at indexBefore < frequency < frequency at indexAfter
        // we want to check what is closest based on log scale
        return if (log10(frequency / frequencies[indexBefore]) < log10(frequencies[indexAfter] / frequency))
            indexBefore + noteIndexBegin
        else
            indexAfter + noteIndexBegin
    }

    override fun getNoteFrequency(noteIndex: Int): Float {
        return when {
            noteIndexEnd <= noteIndexBegin -> referenceFrequency
            noteIndex < noteIndexBegin -> frequencies.first()
            noteIndex >= noteIndexEnd -> frequencies.last()
            else -> frequencies[noteIndex - noteIndexBegin]
        }
    }

    override fun getNoteFrequency(noteIndex: Float): Float {
        // there are no frequencies ...
        if (noteIndexEnd <= noteIndexBegin)
            return referenceFrequency

        // noteIndex = closestNoteIndex +/- log10(frequencies[j] / frequency) / log10(frequencies[j] / frequencies[j +/- 1])
        // (noteIndex - closestNoteIndex) * log10(frequencies[j] / frequencies[j +/- 1]) = +/- log10(frequencies[j] / frequency)
        // 10**( +/- (noteIndex - closestNoteIndex) * log10(frequencies[j] / frequencies[j +/- 1])) = frequencies[j] / frequency
        // frequency = frequencies[i] * 10**( -/+ (noteIndex - closestNoteIndex) * log10(frequencies[j] / frequencies[j +/- 1]))
        // frequency = frequencies[i] * (frequencies[j] / frequencies[j +/- 1])**( -/+ (noteIndex - closestNoteIndex)
        val noteIndexLower = floor(noteIndex).toInt()
        val arrayIndexLower = noteIndexLower - noteIndexBegin
        return when {
            arrayIndexLower < 0 -> {
                frequencies[0] * (frequencies[1] / frequencies[0]).pow(noteIndex - noteIndexBegin)
            }
            arrayIndexLower >= frequencies.size - 1 -> {
                frequencies[frequencies.size - 1] * (frequencies[frequencies.size - 1] / frequencies[frequencies.size - 2]).pow(
                    noteIndex - (frequencies.size - 1)
                )
            }
            else -> {
                frequencies[arrayIndexLower] * (frequencies[arrayIndexLower + 1] / frequencies[arrayIndexLower]).pow(
                    noteIndex - noteIndexLower
                )
            }
        }
    }

    override fun getClosestNote(frequency: Float): MusicalNote {
        val noteIndex = getClosestNoteIndex(frequency)
        return noteNameScale.getNoteOfIndex(noteIndex)
    }

    private fun computeNoteFrequency(noteIndex: Int, ratios: DoubleArray, octaveRatio: Double): Float {
        val numOctaves = if (noteIndex >= referenceNoteIndex)
            (noteIndex - referenceNoteIndex) / numberOfNotesPerOctave
        else
            -((referenceNoteIndex - noteIndex + numberOfNotesPerOctave - 1) / numberOfNotesPerOctave)
        val ratioIndex = noteIndex - referenceNoteIndex - numOctaves * numberOfNotesPerOctave
        return (referenceFrequency * octaveRatio.pow(numOctaves) * ratios[ratioIndex]).toFloat()
    }
}
