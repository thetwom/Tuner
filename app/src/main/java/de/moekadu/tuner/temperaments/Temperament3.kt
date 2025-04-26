package de.moekadu.tuner.temperaments

import de.moekadu.tuner.misc.GetText
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteNames2
import kotlinx.serialization.Serializable

@Serializable
sealed interface Temperament3 {
    val name: GetText
    val abbreviation: GetText
    val description: GetText
    fun cents(): DoubleArray
    fun rationalNumbers(): Array<RationalNumber>?
    fun chainOfFifths(): ChainOfFifths?
    fun equalOctaveDivision(): Int?
    fun possibleRootNotes(): Array<MusicalNote>
    fun noteNames(rootNote: MusicalNote): NoteNames2
    val stableId: Long
}
