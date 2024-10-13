package de.moekadu.tuner.temperaments2

import androidx.compose.runtime.Immutable
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteNameScale
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@Immutable
data class MusicalScale2(
    val temperament: Temperament,
    val noteNames: NoteNames,
    val rootNote: MusicalNote,
    val referenceNote: MusicalNote,
    val referenceFrequency: Float,
    val frequencyMin: Float,
    val frequencyMax: Float,
    val stretchTuning: StretchTuning
) {
    @Transient
    val numberOfNotesPerOctave: Int = temperament.cents.size - 1

    @Transient
    private val noteNameScale = MusicalScaleNoteNames(noteNames, referenceNote)

    @Transient
    val musicalScaleFrequencies = MusicalScaleFrequencies.create(
        temperament.cents,
        computeReferenceNoteIndexWithinOctave(),
        referenceFrequency,
        frequencyMin,
        frequencyMax,
        stretchTuning
    )

    /** Smallest note index (included). */
    @Transient
    val noteIndexBegin: Int = musicalScaleFrequencies.indexStart
    /** Last note index (excluded). */
    @Transient
    val noteIndexEnd: Int = musicalScaleFrequencies.indexEnd

    init {
        assert(numberOfNotesPerOctave == noteNames.size)
    }

    /** Obtain note representation based on class internal note index.
     * @param noteIndex Local index of note (noteIndexBegin <= noteIndex < noteIndexEnd).
     * @return Musical note representation.
     */
    fun getNote(noteIndex: Int): MusicalNote {
        return noteNameScale.getNoteOfIndex(noteIndex)
    }

    /** Obtain note frequency of a given index relative to the reference note.
     * @param noteIndex Note index relative to reference note,
     *   (noteIndexBegin <= noteIndex < noteIndexEnd).
     * @return Note frequency.
     */
    fun getNoteFrequency(noteIndex: Int): Float {
        return musicalScaleFrequencies[noteIndex]
    }

    /** Obtain note frequency based on class internal note index for noteIndex type Float.
     * This method allows using "odd" note indices.
     * @param noteIndex Local floating point index relative to reference note
     *   (noteIndexBegin <= noteIndex < noteIndexEnd).
     * @return Note frequency.
     */
    fun getNoteFrequency(noteIndex: Float): Float {
        return musicalScaleFrequencies[noteIndex]
    }

    /** Get index relative to reference note of a given frequency.
     * This method can return non-integer note indices, so if the frequency is between two notes
     * we still return a meaningful index as a value between the two notes.
     * @param frequency Frequency.
     * @return Note index as float.
     */
    fun getNoteIndex(frequency: Float): Float {
        return musicalScaleFrequencies.getFrequencyIndex(frequency)
    }

    /** Get index relative to reference note which is closest to a given frequency.
     * @param frequency Frequency.
     * @return Note index.
     */
    fun getClosestNoteIndex(frequency: Float): Int {
        return musicalScaleFrequencies.getClosestFrequencyIndex(frequency)
    }

    /** Get note index of a given musical note representation.
     * @param note Musical note representation.
     * @return Local index of the note or Int.MAX_VALUE if not does not exist in scale.
     */
    fun getNoteIndex(note: MusicalNote): Int {
        return noteNameScale.getIndexOfNote(note)
    }

    private fun computeReferenceNoteIndexWithinOctave(): Int {
        val rootNoteIndex = noteNameScale.getIndexOfNote(rootNote.copy(octave = referenceNote.octave))
        return if (rootNoteIndex <= 0) {
            -rootNoteIndex
        } else {
            numberOfNotesPerOctave - rootNoteIndex
        }
    }
}