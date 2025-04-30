package de.moekadu.tuner.temperaments

import de.moekadu.tuner.misc.GetText
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteNames2
import de.moekadu.tuner.notenames.NoteNamesChainOfFifthsGenerator
import de.moekadu.tuner.notenames.NoteNamesEDOGenerator
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Temperament based on a chain of fifths.
 * @param name Temperament name.
 * @param abbreviation Short name for temperament.
 * @param description Further description of temperament.
 * @param stableId Unique id.
 * @param fifths Fifths modifications. This must contain (notes_per_octave - 1) values, since
 *   it is always the fifth between two notes. The octave ratio is fixed at 2, so there is
 *   no ratio needed to close the octave.
 * @param rootIndex Index of the fifths, which will be used as the first note of the octave.
 */
@Serializable
data class Temperament3ChainOfFifthsNoEnharmonics(
    override val name: GetText,
    override val abbreviation: GetText,
    override val description: GetText,
    override val stableId: Long,
    val fifths: Array<out FifthModification>,
    val rootIndex: Int
) : Temperament3 {
    @Transient
    private val _chainOfFifths = ChainOfFifths(fifths, rootIndex)

    @Transient
    override val size = fifths.size + 1

    override fun cents(): DoubleArray =
        _chainOfFifths.getSortedRatios().map{ ratioToCents(it) }.toDoubleArray() + doubleArrayOf(1200.0)
    override fun chainOfFifths(): ChainOfFifths = _chainOfFifths
    override fun equalOctaveDivision(): Int? = null
    override fun rationalNumbers(): Array<RationalNumber>? = null
    override fun possibleRootNotes(): Array<MusicalNote>
        = NoteNamesChainOfFifthsGenerator.possibleRootNotes()
    override fun noteNames(rootNote: MusicalNote?): NoteNames2
        = NoteNamesChainOfFifthsGenerator.getNoteNames(_chainOfFifths, rootNote)!!

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Temperament3ChainOfFifthsNoEnharmonics

        if (stableId != other.stableId) return false
        if (rootIndex != other.rootIndex) return false
        if (name != other.name) return false
        if (abbreviation != other.abbreviation) return false
        if (description != other.description) return false
        if (!fifths.contentEquals(other.fifths)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stableId.hashCode()
        result = 31 * result + rootIndex
        result = 31 * result + name.hashCode()
        result = 31 * result + abbreviation.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + fifths.contentHashCode()
        return result
    }
}

@Serializable
data class Temperament3ChainOfFifthsEDONames(
    override val name: GetText,
    override val abbreviation: GetText,
    override val description: GetText,
    override val stableId: Long,
    val fifths: Array<out FifthModification>,
    val rootIndex: Int
) : Temperament3 {
    @Transient
    private val _chainOfFifths = ChainOfFifths(fifths, rootIndex)

    @Transient
    override val size = fifths.size + 1

    override fun cents(): DoubleArray =
        _chainOfFifths.getSortedRatios().map{ ratioToCents(it) }.toDoubleArray() + doubleArrayOf(1200.0)

    override fun chainOfFifths(): ChainOfFifths = _chainOfFifths
    override fun equalOctaveDivision(): Int? = null
    override fun rationalNumbers(): Array<RationalNumber>? = null
    override fun possibleRootNotes(): Array<MusicalNote>
            = NoteNamesEDOGenerator.possibleRootNotes(fifths.size + 1)
    override fun noteNames(rootNote: MusicalNote?): NoteNames2
            = NoteNamesEDOGenerator.getNoteNames( fifths.size + 1, rootNote)!!

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Temperament3ChainOfFifthsEDONames

        if (stableId != other.stableId) return false
        if (rootIndex != other.rootIndex) return false
        if (name != other.name) return false
        if (abbreviation != other.abbreviation) return false
        if (description != other.description) return false
        if (!fifths.contentEquals(other.fifths)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stableId.hashCode()
        result = 31 * result + rootIndex
        result = 31 * result + name.hashCode()
        result = 31 * result + abbreviation.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + fifths.contentHashCode()
        return result
    }


}
