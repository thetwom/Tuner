package de.moekadu.tuner.temperaments

import de.moekadu.tuner.misc.GetText
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteNames2
import kotlinx.serialization.Serializable

@Serializable
sealed interface Temperament3 {
    /** Temperament name. */
    val name: GetText
    /** Short name for temperament. */
    val abbreviation: GetText
    /** Description of temperament. */
    val description: GetText

    /** Number of notes per octave. */
    val size: Int

    /** Get list of cents including the octave.
     *  Something like [0,100,200,...,1100,1200], size is (notes_per_octave + 1) since it
     *  includes the octave value.
     *  @return Cent values.
     */
    fun cents(): DoubleArray
    /** Values as rational number if available.
     * Something like [1/1, ..., 2/1].
     * This should also include the octave ratio (normally 2/1), so th size is
     * (notes_per_octave + 1).
     * @return Temperament values as ratios relative to first note. null, if no ratio information
     *   is available.
     */
    fun rationalNumbers(): Array<RationalNumber>?

    /** Chain of fifths information if available.
     * @return Chain of fifths information or null if no such info is available.
     */
    fun chainOfFifths(): ChainOfFifths?

    /** Number of notes, which are equally distributed across an octave (EDO scales).
     * @return Number of notes per octave if the temperament is an edo scale, else null.
     */
    fun equalOctaveDivision(): Int?

    /** Possible notes, which can be root notes (note of first temperament value).
     * @return List of possible root notes.
     */
    fun possibleRootNotes(): Array<MusicalNote>

    /** Note name for each temperament value.
     * Contrary to cents()/rationalNumbers(), this does NOT include the octave note names.
     * @param rootNote Note of first value in temperament. Must be part of possibleRootNotes().
     *   null to use a default root note.
     * @return Instance which provides note names.
     */
    fun noteNames(rootNote: MusicalNote?): NoteNames2

    /** Unique id of temperament. */
    val stableId: Long

    companion object {
        const val NO_STABLE_ID = Long.MAX_VALUE
    }
}
