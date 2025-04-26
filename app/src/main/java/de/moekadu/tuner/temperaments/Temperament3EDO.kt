package de.moekadu.tuner.temperaments

import de.moekadu.tuner.R
import de.moekadu.tuner.misc.GetText
import de.moekadu.tuner.misc.GetTextFromResIdWithIntArg
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteNames2
import de.moekadu.tuner.notenames.NoteNamesEDOGenerator
import kotlinx.serialization.Serializable

@Serializable
data class Temperament3EDO(
    override val stableId: Long,
    val notesPerOctave: Int,
) : Temperament3 {
    override val name: GetText
        get() = GetTextFromResIdWithIntArg(R.string.equal_temperament_x, notesPerOctave)
    override val abbreviation: GetText
        get() = GetTextFromResIdWithIntArg(R.string.equal_temperament_x_abbr, notesPerOctave)
    override val description: GetText
        get() = GetTextFromResIdWithIntArg(R.string.equal_temperament_x_desc, notesPerOctave)

    override fun cents() = DoubleArray(notesPerOctave + 1) {
        it * 1200.0 / notesPerOctave.toDouble()
    }

    override fun chainOfFifths(): ChainOfFifths? {
        return if (notesPerOctave == 12) {
            ChainOfFifths(
                Array(notesPerOctave - 1) {
                    FifthModification(pythagoreanComma = RationalNumber(-1, 12))
                },
                rootIndex = 0
            )
        } else {
            null
        }
    }
    override fun equalOctaveDivision(): Int = notesPerOctave
    override fun rationalNumbers(): Array<RationalNumber>? = null
    override fun possibleRootNotes(): Array<MusicalNote>
            = NoteNamesEDOGenerator.possibleRootNotes(notesPerOctave)
    override fun noteNames(rootNote: MusicalNote): NoteNames2
            = NoteNamesEDOGenerator.getNoteNames(rootNote, notesPerOctave)!!
}
