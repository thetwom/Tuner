package de.moekadu.tuner.notenames

import de.moekadu.tuner.temperaments.ChainOfFifths

data object NoteNamesChainOfFifthsGenerator {
    fun possibleRootNotes(): Array<MusicalNote> {
        return arrayOf(
            MusicalNote(BaseNote.C, NoteModifier.None),
            MusicalNote(BaseNote.C, NoteModifier.Sharp),
            MusicalNote(BaseNote.D, NoteModifier.Flat),
            MusicalNote(BaseNote.D, NoteModifier.None),
            MusicalNote(BaseNote.D, NoteModifier.Sharp),
            MusicalNote(BaseNote.E, NoteModifier.Flat),
            MusicalNote(BaseNote.E, NoteModifier.None),
            MusicalNote(BaseNote.F, NoteModifier.None),
            MusicalNote(BaseNote.F, NoteModifier.Sharp),
            MusicalNote(BaseNote.G, NoteModifier.Flat),
            MusicalNote(BaseNote.G, NoteModifier.None),
            MusicalNote(BaseNote.G, NoteModifier.Sharp),
            MusicalNote(BaseNote.A, NoteModifier.Flat),
            MusicalNote(BaseNote.A, NoteModifier.None),
            MusicalNote(BaseNote.A, NoteModifier.Sharp),
            MusicalNote(BaseNote.B, NoteModifier.Flat),
            MusicalNote(BaseNote.B, NoteModifier.None)
        )
    }

    fun getNoteNames(rootNote: MusicalNote, chainOfFifths: ChainOfFifths): NoteNames2? {
        return generateNoteNamesForChainOfFifths(chainOfFifths, rootNote)
    }

}

fun generateNoteNamesForChainOfFifths(
    chain: ChainOfFifths,
    rootNote: MusicalNote
): NoteNames2? {
    val names = Array(chain.fifths.size + 1){ MusicalNote(BaseNote.None, NoteModifier.None) }
    names[chain.rootIndex] = rootNote
    var note = NoteWithSharpness(rootNote.base, rootNote.modifier)
    var noteEnharmonic = NoteWithSharpness(rootNote.enharmonicBase, rootNote.enharmonicModifier)

    for (i in chain.rootIndex until chain.fifths.size) {
        note = note.nextFifth()
        noteEnharmonic = noteEnharmonic.nextFifth()
        names[i + 1] = MusicalNote(
            note.note, note.modifier ?: return null,
            enharmonicBase = noteEnharmonic.note,
            enharmonicModifier = noteEnharmonic.modifier ?: return null
        )
    }

    note = NoteWithSharpness(rootNote.base, rootNote.modifier)
    noteEnharmonic = NoteWithSharpness(rootNote.enharmonicBase, rootNote.enharmonicModifier)
    for (i in chain.rootIndex-1 downTo  0) {
        note = note.previousFifth()
        noteEnharmonic = noteEnharmonic.previousFifth()
        names[i] = MusicalNote(
            note.note, note.modifier ?: return null,
            enharmonicBase = noteEnharmonic.note,
            enharmonicModifier = noteEnharmonic.modifier ?: return null
        )
    }
    val sortedNames = chain.getRatiosAlongFifths()
        .zip(names)
        .sortedBy { it.first }
        .map { it.second }
        .toTypedArray()

    val octaveSwitchAt = sortedNames
        .minBy {
            val baseNoteIndex = it.base.toIndex()
            val measure = 10000 * baseNoteIndex + it.modifier.toSharpness()
            if (measure >= 0)
                measure
            else
                measure + 10000 * baseNoteIndexForOctave
        }
    val octaveSwitchIndex = sortedNames.indexOf(octaveSwitchAt)

//    var beforeSwitch = true
    sortedNames.forEachIndexed { index, noteInArray ->
        var indexRelativeToOctave = index - octaveSwitchIndex
        if (indexRelativeToOctave > sortedNames.size / 2)
            indexRelativeToOctave -= sortedNames.size
        else if (indexRelativeToOctave <= -sortedNames.size / 2)
            indexRelativeToOctave += sortedNames.size

        val noteIndex = noteInArray.base.toIndex()
        if (indexRelativeToOctave < 0 && noteIndex <= BaseNote.D.toIndex())
            sortedNames[index] = noteInArray.copy(octaveOffset = 1)
        else if (indexRelativeToOctave > 0 && noteIndex >= BaseNote.A.toIndex())
            sortedNames[index] = noteInArray.copy(octaveOffset = -1)
    }
    return NoteNames2(
        sortedNames,
        NoteNameHelpers.findDefaultReferenceNote(sortedNames),
        octaveSwitchAt
    )
}

private fun sharpnessToModifier(sharpness: Int): NoteModifier? {
    return when(sharpness) {
        -3 -> NoteModifier.FlatFlatFlat
        -2 -> NoteModifier.FlatFlat
        -1 -> NoteModifier.Flat
        0 -> NoteModifier.None
        1 -> NoteModifier.Sharp
        2 -> NoteModifier.SharpSharp
        3 -> NoteModifier.SharpSharpSharp
        else -> null
    }
}

private fun NoteModifier.toSharpness() = when (this) {
    NoteModifier.FlatFlatFlat -> -3
    NoteModifier.FlatFlat -> -2
    NoteModifier.Flat -> -1
    NoteModifier.None -> 0
    NoteModifier.Sharp -> 1
    NoteModifier.SharpSharp -> 2
    NoteModifier.SharpSharpSharp -> 3
    else -> throw RuntimeException("NoteWithSharpness: Only modifiers without ups/downs allowed")
}

private fun BaseNote.toIndex() = when(this) {
    BaseNote.C -> 0
    BaseNote.D -> 1
    BaseNote.E -> 2
    BaseNote.F -> 3
    BaseNote.G -> 4
    BaseNote.A -> 5
    BaseNote.B -> 6
    BaseNote.None -> throw RuntimeException("BaseNote.toIndex: Note allowed to call for BaseNote.None")
}
private val baseNoteIndexForOctave = 12

private data class NoteWithSharpness(val note: BaseNote, val sharpness: Int) {
    constructor(baseNote: BaseNote, modifier: NoteModifier) : this(baseNote, modifier.toSharpness())

    val modifier get() = sharpnessToModifier(sharpness)

    fun nextFifth(): NoteWithSharpness {
        return when (note) {
            BaseNote.C -> NoteWithSharpness(BaseNote.G, sharpness)
            BaseNote.D -> NoteWithSharpness(BaseNote.A, sharpness)
            BaseNote.E -> NoteWithSharpness(BaseNote.B, sharpness)
            BaseNote.F -> NoteWithSharpness(BaseNote.C, sharpness)
            BaseNote.G -> NoteWithSharpness(BaseNote.D, sharpness)
            BaseNote.A -> NoteWithSharpness(BaseNote.E, sharpness)
            BaseNote.B -> NoteWithSharpness(BaseNote.F, sharpness + 1)
            BaseNote.None -> NoteWithSharpness(BaseNote.None, sharpness)
        }
    }
    fun previousFifth(): NoteWithSharpness {
        return when (note) {
            BaseNote.C -> NoteWithSharpness(BaseNote.F, sharpness)
            BaseNote.D -> NoteWithSharpness(BaseNote.G, sharpness)
            BaseNote.E -> NoteWithSharpness(BaseNote.A, sharpness)
            BaseNote.F -> NoteWithSharpness(BaseNote.B, sharpness - 1)
            BaseNote.G -> NoteWithSharpness(BaseNote.C, sharpness)
            BaseNote.A -> NoteWithSharpness(BaseNote.D, sharpness)
            BaseNote.B -> NoteWithSharpness(BaseNote.E, sharpness)
            BaseNote.None -> NoteWithSharpness(BaseNote.None, sharpness)
        }
    }

}
