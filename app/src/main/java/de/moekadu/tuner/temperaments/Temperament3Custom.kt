package de.moekadu.tuner.temperaments

import de.moekadu.tuner.misc.GetTextFromString
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteNameHelpers
import de.moekadu.tuner.notenames.NoteNames2
import de.moekadu.tuner.notenames.NoteNamesEDOGenerator
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.max

/** User defined (or custom) temperaments.
 * @param _name Temperament name.
 * @param _abbreviation Temperament short name.
 * @param _description Temperament description.
 * @param cents Cent array. This must also include the octave value, so the size of this array
 *   must be number of notes per octave + 1.
 * @param _rationalNumbers List of rational numbers, which can be used instead of the cents array.
 *   Values can be null, if they are note defined. Length of array (if used) must be
 *   number of notes per octave + 1, since the octave value must be included.
 * @param _noteNames Note names. If null, we will use the EDO name generator.
 * @param stableId Unique id.
 */
@Serializable
data class Temperament3Custom(
    val _name: String,
    val _abbreviation: String,
    val _description: String,
    private val cents: DoubleArray,
    val _rationalNumbers: Array<RationalNumber?>,
    private val _noteNames: Array<MusicalNote>?,
    override val stableId: Long
) : Temperament3 {
    @Transient
    override val name = GetTextFromString(_name)
    @Transient
    override val abbreviation = GetTextFromString(_abbreviation)
    @Transient
    override val description = GetTextFromString(_description)
    @Transient
    override val size = max(_rationalNumbers.size, cents.size)

    override fun cents(): DoubleArray {
        return DoubleArray(size + 1) {
            val ratio = _rationalNumbers.getOrNull(it)
            if (ratio != null)
                ratioToCents(ratio.toDouble())
            else
                cents.getOrNull(it) ?: throw RuntimeException("Cent value not defined of value at index $it")
        }
    }

    override fun chainOfFifths(): ChainOfFifths? = null

    override fun equalOctaveDivision(): Int? = null
    override fun rationalNumbers(): Array<RationalNumber>? {
        return if (_rationalNumbers.size != size || _rationalNumbers.contains(null))
            null
        else
            _rationalNumbers.requireNoNulls()
    }

    override fun possibleRootNotes(): Array<MusicalNote>
            = _noteNames ?: NoteNamesEDOGenerator.possibleRootNotes(size)

    override fun noteNames(rootNote: MusicalNote?): NoteNames2 {
        return if (_noteNames.isNullOrEmpty() || _noteNames.size != size) {
            NoteNamesEDOGenerator.getNoteNames(size, rootNote)!!
        } else {
            val referenceNote = NoteNameHelpers.findDefaultReferenceNote(_noteNames)
            val octaveSwitchAt = _noteNames[0]
            val shiftLeft = _noteNames
                .indexOfFirst { it.equalsIgnoreOctave(rootNote) }
                .coerceAtLeast(0) // don't reorder if root note is not found, should not happen
            // this rotates the array, such that element at shiftLeft is at position 0
            val names = (
                    _noteNames.sliceArray(shiftLeft until _noteNames.size) +
                            _noteNames.sliceArray(0 until shiftLeft)
                    )

            NoteNames2(names, referenceNote, octaveSwitchAt)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Temperament3Custom

        if (stableId != other.stableId) return false
        if (_name != other._name) return false
        if (_abbreviation != other._abbreviation) return false
        if (_description != other._description) return false
        if (!cents.contentEquals(other.cents)) return false
        if (!_rationalNumbers.contentEquals(other._rationalNumbers)) return false
        if (_noteNames != null) {
            if (other._noteNames == null) return false
            if (!_noteNames.contentEquals(other._noteNames)) return false
        } else if (other._noteNames != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stableId.hashCode()
        result = 31 * result + _name.hashCode()
        result = 31 * result + _abbreviation.hashCode()
        result = 31 * result + _description.hashCode()
        result = 31 * result + cents.contentHashCode()
        result = 31 * result + _rationalNumbers.contentHashCode()
        result = 31 * result + (_noteNames?.contentHashCode() ?: 0)
        return result
    }
}