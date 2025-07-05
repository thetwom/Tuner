package de.moekadu.tuner.temperaments

import de.moekadu.tuner.misc.GetText
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteNames2
import de.moekadu.tuner.notenames.NoteNamesEDOGenerator
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Temperament based on a list of rational numbers. Note naming is done with EDO name generator.
 * @param name Temperament name.
 * @param abbreviation Short name for temperament.
 * @param description Further description of temperament.
 * @param rationalNumbers List of rational numbers. This must also include the ratio of the octave
 *   (normally RationalNumber(2,1)). So the size of this array must be (notes_per_octave + 1)
 * @param stableId Unique id.
 * @param uniqueIdentifier Unique identifier string between all predefined temperaments. This is
 *   when loading the currently active temperament at app start, to reload it and not just
 *   deserializing it, since GetText can point to string ids, which can change between app versions.
 *   Also it allows to provide fixes over app versions and make sure, that they are actually loaded.
 */
@Serializable
data class Temperament3RationalNumbersEDONames(
    override val name: GetText,
    override val abbreviation: GetText,
    override val description: GetText,
    private val rationalNumbers: Array<RationalNumber>,
    override val stableId: Long,
    val uniqueIdentifier: String
) : Temperament3 {
    @Transient
    override val size = rationalNumbers.size - 1

    override fun cents() = rationalNumbers.map { ratioToCents(it.toDouble()) }.toDoubleArray()

    override fun chainOfFifths(): ChainOfFifths? = null

    override fun equalOctaveDivision(): Int? = null
    override fun rationalNumbers(): Array<RationalNumber> = this.rationalNumbers
    override fun possibleRootNotes(): Array<MusicalNote>
            = NoteNamesEDOGenerator.possibleRootNotes(size)
    override fun noteNames(rootNote: MusicalNote?): NoteNames2
            = NoteNamesEDOGenerator.getNoteNames(size, rootNote)!!

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Temperament3RationalNumbersEDONames

        if (stableId != other.stableId) return false
        if (name != other.name) return false
        if (abbreviation != other.abbreviation) return false
        if (description != other.description) return false
        if (!rationalNumbers.contentEquals(other.rationalNumbers)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stableId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + abbreviation.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + rationalNumbers.contentHashCode()
        return result
    }
}
