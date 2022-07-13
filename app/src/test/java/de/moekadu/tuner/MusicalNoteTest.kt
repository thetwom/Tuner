package de.moekadu.tuner

import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.temperaments.NoteNameScale
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
            notes,
            MusicalNote(BaseNote.A, NoteModifier.None, 4)
        )

        assertEquals(-1, scale.getIndexOfNote(MusicalNote(BaseNote.G, NoteModifier.Sharp, 4)))
        assertEquals(0, scale.getIndexOfNote(MusicalNote(BaseNote.A, NoteModifier.None, 4)))
        assertEquals(1, scale.getIndexOfNote(MusicalNote(BaseNote.A, NoteModifier.Sharp, 4)))
        assertEquals(2, scale.getIndexOfNote(MusicalNote(BaseNote.B, NoteModifier.None, 4)))
        assertEquals(3, scale.getIndexOfNote(MusicalNote(BaseNote.C, NoteModifier.None, 5)))
        assertEquals(4, scale.getIndexOfNote(MusicalNote(BaseNote.C, NoteModifier.Sharp, 5)))
        assertEquals(16, scale.getIndexOfNote(MusicalNote(BaseNote.C, NoteModifier.Sharp, 6)))

        for (someIndex in -100 .. 100) {
            val someNote = scale.getNoteOfIndex(someIndex)
            val recoveredIndex = scale.getIndexOfNote(someNote)
            assertEquals(someIndex, recoveredIndex)
        }
        // TODO: add more tests
    }

    @Test
    fun testClosestNote() {
        val notes1 = arrayOf(
            MusicalNote(BaseNote.C, NoteModifier.None),
            MusicalNote(BaseNote.E, NoteModifier.None),
            MusicalNote(BaseNote.G, NoteModifier.None),
            MusicalNote(BaseNote.D, NoteModifier.Sharp, enharmonicBase = BaseNote.E, enharmonicModifier = NoteModifier.Flat)
        )

        val notes2 = arrayOf(
            MusicalNote(BaseNote.E, NoteModifier.Flat, enharmonicBase = BaseNote.D, enharmonicModifier = NoteModifier.Sharp),
            MusicalNote(BaseNote.A, NoteModifier.None),
            MusicalNote(BaseNote.F, NoteModifier.Sharp),
            MusicalNote(BaseNote.C, NoteModifier.None)
        )

        val scale1 = NoteNameScale(notes1, notes1[0].copy(octave = 4))
        val scale2 = NoteNameScale(notes2, notes2[0].copy(octave = 4))

        // if equal note exists, take it ...
        val testNote1 = notes1[0].copy(octave = 2)
        val closeNote1 = scale2.getClosestNote(testNote1, scale1)
        assertEquals(testNote1.base, closeNote1.base)
        assertEquals(testNote1.modifier, closeNote1.modifier)
        assertEquals(testNote1.octave, closeNote1.octave)

        // if equal note exists but enharmonic, we take it but get the note with enharmoic order of scale2
        val testNote2 = notes1[3].copy(octave = 2)
        val testNote2S2 = notes2[0].copy(octave = 2)
        val closeNote2 = scale2.getClosestNote(testNote2, scale1)
        assertEquals(testNote2S2.base, closeNote2.base)
        assertEquals(testNote2S2.modifier, closeNote2.modifier)
        assertEquals(testNote2S2.octave, closeNote2.octave)
        assertEquals(testNote2S2.enharmonicBase, closeNote2.enharmonicBase)
        assertEquals(testNote2S2.enharmonicModifier, closeNote2.enharmonicModifier)

        // use closest relative position
        val testNote3 = notes1[2].copy(octave = 2)
        val closeNote3 = scale2.getClosestNote(testNote3, scale1)
        assertEquals(BaseNote.F, closeNote3.base)
        assertEquals(NoteModifier.Sharp, closeNote3.modifier)
        assertEquals(testNote1.octave, closeNote3.octave)
    }
}