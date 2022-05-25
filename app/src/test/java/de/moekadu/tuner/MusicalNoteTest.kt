package de.moekadu.tuner

import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteNameScale
import de.moekadu.tuner.temperaments.NoteModifier
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

        val scale = NoteNameScale(
            MusicalNote(BaseNote.A, NoteModifier.None, 4),
            notes
        )

        assertEquals(0, scale.getIndexOfNote(MusicalNote(BaseNote.A, NoteModifier.None, 4)))
        // TODO: add more tests
    }
}