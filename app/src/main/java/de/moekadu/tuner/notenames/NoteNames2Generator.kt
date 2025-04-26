package de.moekadu.tuner.notenames

import de.moekadu.tuner.temperaments.ChainOfFifths
import kotlinx.serialization.Serializable

/** Interface to generate note names. */
@Serializable
sealed interface NoteNames2Generator {
    /** Return the possible root notes (first note of temperament).
     * @param cents Cents of temperament for which the note names should be generated. Size of
     *   array is number of notes per octave + 1, since the cents of the octave is included.
     * @param chainOfFifths If available, the chain of fifth information of the temperament is
     *   passed. If not available, this is null.
     * @return Possible root notes, which can be used.
     */
    fun possibleRootNotes(
        cents: DoubleArray, chainOfFifths: ChainOfFifths?
    ): Array<MusicalNote>

    /** Generate note names.
     * @param rootNote Name of first note. Must be a note of array returned by possibleRootNotes().
     * @param cents Cents of temperament for which the note names should be generated. Size of
     *   array is number of notes per octave + 1, since the cents of the octave is included.
     * @param chainOfFifths If available, the chain of fifth information of the temperament is
     *   passed. If not available, this is null.
     * @return Note for each value of the temperament excluding the name of the octave (i.e. each
     *   value of the cents array excluding the last one). null, if note names cannot be generated.
     */
    fun getNoteNames(
        rootNote: MusicalNote, cents: DoubleArray, chainOfFifths: ChainOfFifths?
    ): NoteNames2?
}
