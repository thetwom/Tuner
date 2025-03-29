package de.moekadu.tuner

import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.temperaments.NoteNames
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicalNoteTest {

    @Test
    fun testMusicalScaleIndexing() {
        val notes = arrayOf(
            MusicalNote(BaseNote.C, NoteModifier.None),
            MusicalNote(BaseNote.C, NoteModifier.Sharp),
            MusicalNote(BaseNote.D, NoteModifier.None),
            MusicalNote(BaseNote.D, NoteModifier.Sharp),
            MusicalNote(BaseNote.E, NoteModifier.None),
            MusicalNote(BaseNote.F, NoteModifier.None),
            MusicalNote(BaseNote.F, NoteModifier.Sharp),
            MusicalNote(BaseNote.G, NoteModifier.None),
            MusicalNote(BaseNote.G, NoteModifier.Sharp),
            MusicalNote(BaseNote.A, NoteModifier.None),
            MusicalNote(BaseNote.A, NoteModifier.Sharp),
            MusicalNote(BaseNote.B, NoteModifier.None),
        )

        val scale = NoteNames(
            notes,
            MusicalNote(BaseNote.A, NoteModifier.None, 4)
        )

        assertEquals(-1, scale.getNoteIndex(MusicalNote(BaseNote.G, NoteModifier.Sharp, 4)))
        assertEquals(0, scale.getNoteIndex(MusicalNote(BaseNote.A, NoteModifier.None, 4)))
        assertEquals(1, scale.getNoteIndex(MusicalNote(BaseNote.A, NoteModifier.Sharp, 4)))
        assertEquals(2, scale.getNoteIndex(MusicalNote(BaseNote.B, NoteModifier.None, 4)))
        assertEquals(3, scale.getNoteIndex(MusicalNote(BaseNote.C, NoteModifier.None, 5)))
        assertEquals(4, scale.getNoteIndex(MusicalNote(BaseNote.C, NoteModifier.Sharp, 5)))
        assertEquals(16, scale.getNoteIndex(MusicalNote(BaseNote.C, NoteModifier.Sharp, 6)))

        for (someIndex in -100 .. 100) {
            val someNote = scale[someIndex]
            val recoveredIndex = scale.getNoteIndex(someNote)
            assertEquals(someIndex, recoveredIndex)
        }
    }
}