package de.moekadu.tuner

import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.temperaments.generateNoteNames
import junit.framework.TestCase.assertEquals
import org.junit.Test

private fun noteToString(note: MusicalNote): String {
    val builder = StringBuilder()
    if (note.base == BaseNote.None) {
        builder.append("-")
    } else {
        builder.append(noteToString(note.base, note.modifier, note.octaveOffset))
    }
    if (note.enharmonicBase != BaseNote.None) {
        builder.append("/")
        builder.append(
            noteToString(
                note.enharmonicBase,
                note.enharmonicModifier,
                note.enharmonicOctaveOffset
            )
        )
    }
    return builder.toString()
}

private fun noteToString(base: BaseNote, modifier: NoteModifier, offset: Int): String {
    val offsetString = if (offset == 0) "" else "%+d".format(offset)
    return "${base.name}${noteModifierToString(modifier)}$offsetString"
}

private fun noteModifierToString(modifier: NoteModifier) = when (modifier) {
    NoteModifier.FlatFlatFlatDownDownDown -> "vvvbbb"
    NoteModifier.FlatFlatFlatDownDown -> "vvbbb"
    NoteModifier.FlatFlatFlatDown -> "vbbb"
    NoteModifier.FlatFlatFlat -> "bbb"
    NoteModifier.FlatFlatFlatUp -> "^bbb"
    NoteModifier.FlatFlatFlatUpUp -> "^^bbb"
    NoteModifier.FlatFlatFlatUpUpUp -> "^^^bbb"
    NoteModifier.FlatFlatDownDownDown -> "vvvbb"
    NoteModifier.FlatFlatDownDown -> "vvbb"
    NoteModifier.FlatFlatDown -> "vbb"
    NoteModifier.FlatFlat -> "bb"
    NoteModifier.FlatFlatUp -> "^bb"
    NoteModifier.FlatFlatUpUp -> "^^bb"
    NoteModifier.FlatFlatUpUpUp -> "^^^bb"
    NoteModifier.FlatDownDownDown -> "vvvb"
    NoteModifier.FlatDownDown -> "vvb"
    NoteModifier.FlatDown -> "vb"
    NoteModifier.Flat -> "b"
    NoteModifier.FlatUp -> "^b"
    NoteModifier.FlatUpUp -> "^^b"
    NoteModifier.FlatUpUpUp -> "^^^b"
    NoteModifier.NaturalDownDownDown -> "vvv"
    NoteModifier.NaturalDownDown -> "vv"
    NoteModifier.NaturalDown -> "v"
    NoteModifier.None -> ""
    NoteModifier.NaturalUp -> "^"
    NoteModifier.NaturalUpUp -> "^^"
    NoteModifier.NaturalUpUpUp -> "^^^"
    NoteModifier.SharpDownDownDown -> "vvv#"
    NoteModifier.SharpDownDown -> "vv#"
    NoteModifier.SharpDown -> "v#"
    NoteModifier.Sharp -> "#"
    NoteModifier.SharpUp -> "^#"
    NoteModifier.SharpUpUp -> "^^#"
    NoteModifier.SharpUpUpUp -> "^^^#"
    NoteModifier.SharpSharpDownDownDown -> "vvv##"
    NoteModifier.SharpSharpDownDown -> "vv##"
    NoteModifier.SharpSharpDown -> "v##"
    NoteModifier.SharpSharp -> "##"
    NoteModifier.SharpSharpUp -> "^##"
    NoteModifier.SharpSharpUpUp -> "^^##"
    NoteModifier.SharpSharpUpUpUp -> "^^^##"
    NoteModifier.SharpSharpSharpDownDownDown -> "vvv###"
    NoteModifier.SharpSharpSharpDownDown -> "vv###"
    NoteModifier.SharpSharpSharpDown -> "v###"
    NoteModifier.SharpSharpSharp -> "###"
    NoteModifier.SharpSharpSharpUp -> "^###"
    NoteModifier.SharpSharpSharpUpUp -> "^^###"
    NoteModifier.SharpSharpSharpUpUpUp -> "^^^###"
}

class NoteNamesGeneratorTest {

    private fun testNumberOfNotesImpl(numberOfNotesPerOctave: Int) {
        val names = generateNoteNames(numberOfNotesPerOctave)
        assertEquals(numberOfNotesPerOctave, names.size)
    }

    @Test
    fun testNumberOfNotes() {
        for (numberOfNotesPerOctave in 5 ..72)
            testNumberOfNotesImpl(numberOfNotesPerOctave)
    }
    @Test
    fun testNoteNames() {
        val numberOfNotePerOctave = 20
        val names = generateNoteNames(numberOfNotePerOctave)
        assertEquals(numberOfNotePerOctave, names.size)
        names.notes.forEachIndexed { i, n ->
            println("$i: ${noteToString(n)}")
        }
    }
}