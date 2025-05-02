package de.moekadu.tuner.notenames

import kotlinx.serialization.Serializable
import java.util.Collections
import kotlin.math.absoluteValue
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlin.math.sign

/** Create note names for EDO scales. */
@Serializable
data object NoteNamesEDOGenerator {

    /** Return the possible root notes (first note of temperament).
     * @param notesPerOctave Number of notes per octave.
     * @return Possible root notes, which can be used.
     */
    fun possibleRootNotes(notesPerOctave: Int): Array<MusicalNote> {
        return generateNoteNamesImpl(notesPerOctave)
    }

    /** Generate note names.
     * @param notesPerOctave Number of notes per octave.
     * @param rootNote Name of first note. Must be a note of array returned by
     *   possibleRootNotes(). If null is passed, a default root note is used (C).
     * @return Note for each value of the temperament excluding the name of the octave (i.e. each
     *   value of the cents array excluding the last one). null, if note names cannot be generated.
     */
    fun getNoteNames(notesPerOctave: Int, rootNote: MusicalNote?): NoteNames2? {
        if (notesPerOctave > 72)
            return null
        val noteNames = generateNoteNamesImpl(notesPerOctave)
        val defaultReferenceNote = NoteNameHelpers.findDefaultReferenceNote(noteNames)
        val incrementOctaveAt = noteNames[0]

        if (rootNote != null) {
            val shiftLeft = noteNames
                .indexOfFirst { it.equalsIgnoreOctave(rootNote) }
                .coerceAtLeast(0) // don't reorder if root note is not found, should not happen
            Collections.rotate(noteNames.asList(), -shiftLeft)
        }
        return NoteNames2(noteNames, defaultReferenceNote, incrementOctaveAt)
    }
}

/** Type of seconds. */
private enum class Second{Maj, Min}

private data class BaseNoteStep(val from: BaseNote, val to: BaseNote, val second: Second)
private val baseNoteSteps = arrayOf(
    BaseNoteStep(BaseNote.C, BaseNote.D, Second.Maj),
    BaseNoteStep(BaseNote.D, BaseNote.E, Second.Maj),
    BaseNoteStep(BaseNote.E, BaseNote.F, Second.Min),
    BaseNoteStep(BaseNote.F, BaseNote.G, Second.Maj),
    BaseNoteStep(BaseNote.G, BaseNote.A, Second.Maj),
    BaseNoteStep(BaseNote.A, BaseNote.B, Second.Maj),
    BaseNoteStep(BaseNote.B, BaseNote.C, Second.Min)
)

private data class ExtendedModifier(val sharpness: Int, val ups: Int)

private fun ExtendedModifier.toNoteModifier(): NoteModifier {
    return when {
        sharpness == -3 && ups == -3 -> NoteModifier.FlatFlatFlatDownDownDown
        sharpness == -3 && ups == -2 -> NoteModifier.FlatFlatFlatDownDown
        sharpness == -3 && ups == -1 -> NoteModifier.FlatFlatFlatDown
        sharpness == -3 && ups == 0 -> NoteModifier.FlatFlatFlat
        sharpness == -3 && ups == 1 -> NoteModifier.FlatFlatFlatUp
        sharpness == -3 && ups == 2 -> NoteModifier.FlatFlatFlatUpUp
        sharpness == -3 && ups == 3 -> NoteModifier.FlatFlatFlatUpUpUp
        sharpness == -2 && ups == -3 -> NoteModifier.FlatFlatDownDownDown
        sharpness == -2 && ups == -2 -> NoteModifier.FlatFlatDownDown
        sharpness == -2 && ups == -1 -> NoteModifier.FlatFlatDown
        sharpness == -2 && ups == 0 -> NoteModifier.FlatFlat
        sharpness == -2 && ups == 1 -> NoteModifier.FlatFlatUp
        sharpness == -2 && ups == 2 -> NoteModifier.FlatFlatUpUp
        sharpness == -2 && ups == 3 -> NoteModifier.FlatFlatUpUpUp
        sharpness == -1 && ups == -3 -> NoteModifier.FlatDownDownDown
        sharpness == -1 && ups == -2 -> NoteModifier.FlatDownDown
        sharpness == -1 && ups == -1 -> NoteModifier.FlatDown
        sharpness == -1 && ups == 0 -> NoteModifier.Flat
        sharpness == -1 && ups == 1 -> NoteModifier.FlatUp
        sharpness == -1 && ups == 2 -> NoteModifier.FlatUpUp
        sharpness == -1 && ups == 3 -> NoteModifier.FlatUpUpUp
        sharpness == 0 && ups == -3 -> NoteModifier.NaturalDownDownDown
        sharpness == 0 && ups == -2 -> NoteModifier.NaturalDownDown
        sharpness == 0 && ups == -1 -> NoteModifier.NaturalDown
        sharpness == 0 && ups == 0 -> NoteModifier.None
        sharpness == 0 && ups == 1 -> NoteModifier.NaturalUp
        sharpness == 0 && ups == 2 -> NoteModifier.NaturalUpUp
        sharpness == 0 && ups == 3 -> NoteModifier.NaturalUpUpUp
        sharpness == 1 && ups == -3 -> NoteModifier.SharpDownDownDown
        sharpness == 1 && ups == -2 -> NoteModifier.SharpDownDown
        sharpness == 1 && ups == -1 -> NoteModifier.SharpDown
        sharpness == 1 && ups == 0 -> NoteModifier.Sharp
        sharpness == 1 && ups == 1 -> NoteModifier.SharpUp
        sharpness == 1 && ups == 2 -> NoteModifier.SharpUpUp
        sharpness == 1 && ups == 3 -> NoteModifier.SharpUpUpUp
        sharpness == 2 && ups == -3 -> NoteModifier.SharpSharpDownDownDown
        sharpness == 2 && ups == -2 -> NoteModifier.SharpSharpDownDown
        sharpness == 2 && ups == -1 -> NoteModifier.SharpSharpDown
        sharpness == 2 && ups == 0 -> NoteModifier.SharpSharp
        sharpness == 2 && ups == 1 -> NoteModifier.SharpSharpUp
        sharpness == 2 && ups == 2 -> NoteModifier.SharpSharpUpUp
        sharpness == 2 && ups == 3 -> NoteModifier.SharpSharpUpUpUp
        sharpness == 3 && ups == -3 -> NoteModifier.SharpSharpSharpDownDownDown
        sharpness == 3 && ups == -2 -> NoteModifier.SharpSharpSharpDownDown
        sharpness == 3 && ups == -1 -> NoteModifier.SharpSharpSharpDown
        sharpness == 3 && ups == 0 -> NoteModifier.SharpSharpSharp
        sharpness == 3 && ups == 1 -> NoteModifier.SharpSharpSharpUp
        sharpness == 3 && ups == 2 -> NoteModifier.SharpSharpSharpUpUp
        sharpness == 3 && ups == 3 -> NoteModifier.SharpSharpSharpUpUpUp
        else -> throw RuntimeException("Unknown NoteModifier for sharpness = $sharpness and ups = $ups")
    }
}

private data class GeneralSecondIntervalItem(
    val note0Index: Int,
    val modifier0: NoteModifier,
    val note1Index: Int,
    val modifier1: NoteModifier
) {
    companion object {
        val NOT_DEFINED = Int.MIN_VALUE
    }
}

private fun generateNoteModifiers(numSteps: Int, aug1: Int): Array<GeneralSecondIntervalItem> {
    if (numSteps == 0)
        return arrayOf()

    // set this to one, if enharmonic is allowed to have one up or sharp more
    val maxHigherComplexityOfEnharmonic = 0
    var sharpness = 0
    var ups = 0
    val maxUps = (aug1 / 2).absoluteValue

    val modifiers = Array(numSteps - 1) {
        ups += 1
        if (ups > maxUps) {
            ups -= aug1.absoluteValue
            sharpness += aug1.sign
        }
        ExtendedModifier(sharpness, ups)
    }

    val result = Array(numSteps) {
        if (it == 0) {
            GeneralSecondIntervalItem(
                0, NoteModifier.None,
                GeneralSecondIntervalItem.NOT_DEFINED, NoteModifier.None
            )
        } else {
            val iMod = it - 1
            sharpness = modifiers[iMod].sharpness
            ups = modifiers[iMod].ups
            val modifier2 = ExtendedModifier(
                -modifiers[modifiers.size - iMod - 1].sharpness,
                -modifiers[modifiers.size - iMod - 1].ups
                )
            val numMods = sharpness.absoluteValue + ups.absoluteValue
            val numMods2 = modifier2.sharpness.absoluteValue + modifier2.ups.absoluteValue
            if (numMods > numMods2 + maxHigherComplexityOfEnharmonic) {
                GeneralSecondIntervalItem(
                    1, modifier2.toNoteModifier(),
                    GeneralSecondIntervalItem.NOT_DEFINED, NoteModifier.None
                )
//                MusicalNote(
//                    note2, modifier2.toNoteModifier(),
//                    octaveOffset = if (note2 == BaseNote.C) 1 else 0
//                )
            } else if (numMods2 > numMods + maxHigherComplexityOfEnharmonic) {
                GeneralSecondIntervalItem(
                    0, modifiers[iMod].toNoteModifier(),
                    GeneralSecondIntervalItem.NOT_DEFINED, NoteModifier.None
                )
//                MusicalNote(note1, modifiers[it].toNoteModifier())
            } else {
                GeneralSecondIntervalItem(
                    0, modifiers[iMod].toNoteModifier(),
                    1, modifier2.toNoteModifier()
                )
//                MusicalNote(
//                    note1, modifiers[it].toNoteModifier(),
//                    enharmonicBase = note2,
//                    enharmonicModifier = modifier2.toNoteModifier(),
//                    enharmonicOctaveOffset = if (note2 == BaseNote.C) 1 else 0
//                )
            }
        }
    }

    return result
}

private fun generateNoteNamesImpl(notesPerOctave: Int): Array<MusicalNote> {
    if (notesPerOctave == 6) {
        val names12 = generateNoteNamesImpl(12)
        return Array(6) { names12[2 * it + 1] }
    } else if (notesPerOctave == 8) {
        val names24 = generateNoteNamesImpl(24)
        return Array(8) { names24[3 * it] }
    }

    val fifthTmp = (notesPerOctave * log2(3f/2f)).roundToInt()
    val fifth = if (5 * fifthTmp > 3 * notesPerOctave) fifthTmp - 1 else fifthTmp

    val maj2 = 2 * fifth - notesPerOctave
    val min2 = 3 * notesPerOctave - 5 * fifth
    val aug1 = (7 * fifth - 4 * notesPerOctave)  //  C to C# or Eb to E

    val min2Items = generateNoteModifiers(min2, aug1)
    val maj2Items = generateNoteModifiers(maj2, aug1)

    var lastBaseNoteStep = baseNoteSteps.last()
    val names = Array(notesPerOctave) { MusicalNote(BaseNote.A, NoteModifier.None) }

    var count = 0
    baseNoteSteps.forEach { baseNoteStep ->
        val secondIntervalItems = when (baseNoteStep.second) {
            Second.Min -> min2Items
            Second.Maj -> maj2Items
        }
        val lastSecondIntervalItems = when (lastBaseNoteStep.second) {
            Second.Min -> min2Items
            Second.Maj -> maj2Items
        }
        secondIntervalItems.forEachIndexed { index, secondIntervalItem ->
            if (index == 0 && lastSecondIntervalItems.isEmpty()) {
                names[count] = MusicalNote(
                    base = lastBaseNoteStep.from,
                    modifier = NoteModifier.None,
                    octaveOffset = if(lastBaseNoteStep.to == baseNoteSteps.first().from) -1 else 0,
                    enharmonicBase = lastBaseNoteStep.to,
                    enharmonicModifier = NoteModifier.None,
                    enharmonicOctaveOffset = 0
                )
            } else {
                val baseNote = when (secondIntervalItem.note0Index) {
                    0 -> baseNoteStep.from
                    1 -> baseNoteStep.to
                    GeneralSecondIntervalItem.NOT_DEFINED -> BaseNote.None
                    else -> throw RuntimeException("Invalid base note step, note0Index=${secondIntervalItem.note0Index}")
                }
                val enharmonicBaseNote = when (secondIntervalItem.note1Index) {
                    0 -> baseNoteStep.from
                    1 -> baseNoteStep.to
                    GeneralSecondIntervalItem.NOT_DEFINED -> BaseNote.None
                    else -> throw RuntimeException("Invalid base note step, note1Index=${secondIntervalItem.note1Index}")
                }

                var octaveOffset = 0
                var enharmonicOctaveOffset = 0
                if (baseNoteStep.to == baseNoteSteps.first().from) {
                    if (baseNote == baseNoteStep.to)
                        octaveOffset = 1
                    else if (enharmonicBaseNote == baseNoteStep.to)
                        enharmonicOctaveOffset = 1
                }

                names[count] = MusicalNote(
                    base = baseNote,
                    modifier = secondIntervalItem.modifier0,
                    octaveOffset = octaveOffset,
                    enharmonicBase = enharmonicBaseNote,
                    enharmonicModifier = secondIntervalItem.modifier1,
                    enharmonicOctaveOffset = enharmonicOctaveOffset
                )
            }
            count += 1
        }
        lastBaseNoteStep = baseNoteStep
    }

    return names
}

//private fun generateNoteNames(notesPerOctave: Int): NoteNames? {
//    // limit since we might not have enough note modifiers
//    // -> did not check, if with the current note modifiers more is possible?
//    if (notesPerOctave > 72)
//        return null
//    val names = generateNoteNamesImpl(notesPerOctave)
//    return NoteNames(names, NoteNameHelpers.findDefaultReferenceNote(names))
//}

