package de.moekadu.tuner.ui

import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteModifier
import de.moekadu.tuner.ui.notes.GrandStaffPlacement
import de.moekadu.tuner.ui.notes.grandStaffPlacement
import de.moekadu.tuner.ui.notes.ledgerLinePositions
import de.moekadu.tuner.ui.notes.staffPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteStaffTest {
    @Test
    fun staffPositionUsesPrintedOctaveAndSpelling() {
        // middle C
        assertEquals(28, MusicalNote(BaseNote.C, NoteModifier.None, 4).staffPosition(false))
        // Cb4 belongs to octave 3 of the scale but is printed in octave 4
        val cFlat4 = MusicalNote(BaseNote.C, NoteModifier.Flat, 3, octaveOffset = 1)
        assertEquals(28, cFlat4.staffPosition(false))
        // B#3 belongs to octave 4 but is printed in octave 3
        val bSharp3 = MusicalNote(BaseNote.B, NoteModifier.Sharp, 4, octaveOffset = -1)
        assertEquals(27, bSharp3.staffPosition(false))

        val cSharp4 = MusicalNote(
            BaseNote.C, NoteModifier.Sharp, 4,
            enharmonicBase = BaseNote.D, enharmonicModifier = NoteModifier.Flat
        )
        assertEquals(28, cSharp4.staffPosition(false))
        assertEquals(29, cSharp4.staffPosition(true))
        // no enharmonic available -> keep spelling
        assertEquals(28, MusicalNote(BaseNote.C, NoteModifier.None, 4).staffPosition(true))
    }

    @Test
    fun staffPositionNeedsBaseAndOctave() {
        assertNull(MusicalNote(BaseNote.None, NoteModifier.None, 4).staffPosition(false))
        assertNull(MusicalNote(BaseNote.C, NoteModifier.None).staffPosition(false))
    }

    @Test
    fun placementShiftsOnlyOutsideLedgerRange() {
        // E6 and A1 are the outermost notes drawn with ledger lines
        assertEquals(GrandStaffPlacement(44, 0), grandStaffPlacement(44))
        assertEquals(GrandStaffPlacement(12, 0), grandStaffPlacement(12))
        // F6 -> 8va
        assertEquals(GrandStaffPlacement(38, 1), grandStaffPlacement(45))
        // C8 -> 15ma
        assertEquals(GrandStaffPlacement(42, 2), grandStaffPlacement(56))
        // A0 -> 8vb
        assertEquals(GrandStaffPlacement(12, -1), grandStaffPlacement(5))
        // G1 -> 8vb
        assertEquals(GrandStaffPlacement(17, -1), grandStaffPlacement(10))
    }

    @Test
    fun ledgerLines() {
        assertEquals(listOf(28), ledgerLinePositions(28).toList()) // middle C
        assertEquals(emptyList<Int>(), ledgerLinePositions(27).toList()) // B3 above bass staff
        assertEquals(emptyList<Int>(), ledgerLinePositions(29).toList()) // D4 below treble staff
        assertEquals(emptyList<Int>(), ledgerLinePositions(39).toList()) // G5 on top of staff
        assertEquals(listOf(40), ledgerLinePositions(41).toList()) // B5
        assertEquals(listOf(40, 42, 44), ledgerLinePositions(44).toList())
        assertEquals(emptyList<Int>(), ledgerLinePositions(17).toList()) // F2 below staff
        assertEquals(listOf(16, 14), ledgerLinePositions(13).toList())
        assertEquals(listOf(16, 14, 12), ledgerLinePositions(12).toList())
    }
}
