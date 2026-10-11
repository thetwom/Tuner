package de.moekadu.tuner.ui

import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteModifier
import de.moekadu.tuner.ui.notes.SpelledNote
import de.moekadu.tuner.ui.notes.StaffClef
import de.moekadu.tuner.ui.notes.StaffPlacement
import de.moekadu.tuner.ui.notes.ledgerLinePositions
import de.moekadu.tuner.ui.notes.spellNote
import de.moekadu.tuner.ui.notes.staffPlacement
import de.moekadu.tuner.ui.notes.staffPosition
import de.moekadu.tuner.ui.notes.useEnharmonicForKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteStaffTest {
    private val cSharp4 = MusicalNote(
        BaseNote.C,
        NoteModifier.Sharp,
        4,
        enharmonicBase = BaseNote.D,
        enharmonicModifier = NoteModifier.Flat
    )
    private val dFlat4 = cSharp4.switchEnharmonic()
    private val aSharp4 = MusicalNote(
        BaseNote.A,
        NoteModifier.Sharp,
        4,
        enharmonicBase = BaseNote.B,
        enharmonicModifier = NoteModifier.Flat
    )
    private val bFlat4 = aSharp4.switchEnharmonic()
    private val f4 = MusicalNote(BaseNote.F, NoteModifier.None, 4)
    private val c4 = MusicalNote(
        BaseNote.C,
        NoteModifier.None,
        4,
        enharmonicBase = BaseNote.B,
        enharmonicModifier = NoteModifier.Sharp,
        enharmonicOctaveOffset = -1
    )

    @Test
    fun staffPositionUsesPrintedOctave() {
        // middle C
        assertEquals(28, MusicalNote(BaseNote.C, NoteModifier.None, 4).staffPosition())
        // Cb4 belongs to octave 3 of the scale but is printed in octave 4
        val cFlat4 = MusicalNote(BaseNote.C, NoteModifier.Flat, 3, octaveOffset = 1)
        assertEquals(28, cFlat4.staffPosition())
        // B#3 belongs to octave 4 but is printed in octave 3
        assertEquals(27, c4.switchEnharmonic().staffPosition())
        assertEquals(29, dFlat4.staffPosition())
    }

    @Test
    fun staffPositionNeedsBaseAndOctave() {
        assertNull(MusicalNote(BaseNote.None, NoteModifier.None, 4).staffPosition())
        assertNull(MusicalNote(BaseNote.C, NoteModifier.None).staffPosition())
    }

    @Test
    fun spellingWithoutKeySignatureFollowsEnharmonicPreference() {
        assertEquals(SpelledNote(cSharp4, true), spellNote(cSharp4, false, null))
        assertEquals(SpelledNote(dFlat4, true), spellNote(cSharp4, true, null))
        assertEquals(SpelledNote(f4, false), spellNote(f4, true, null))
    }

    @Test
    fun spellingPrefersNotesOfTheKey() {
        // D major: C# is part of the key, no accidental; F needs a natural sign
        assertEquals(SpelledNote(cSharp4, false), spellNote(cSharp4, true, 2))
        assertEquals(SpelledNote(f4, true), spellNote(f4, false, 2))
        // F major: Bb, regardless of the enharmonic preference
        assertEquals(SpelledNote(bFlat4, false), spellNote(aSharp4, false, -1))
        // Bb major: C#/Db is not in the key -> flat spelling with accidental
        assertEquals(SpelledNote(dFlat4, true), spellNote(cSharp4, false, -2))
        // G major: C#/Db not in the key -> sharp spelling
        assertEquals(SpelledNote(cSharp4, true), spellNote(dFlat4, false, 1))
        // C major: preference decides
        assertEquals(SpelledNote(dFlat4, true), spellNote(cSharp4, true, 0))
        assertEquals(SpelledNote(c4, false), spellNote(c4, true, 0))
        // C# major: C is written as B#
        assertEquals(SpelledNote(c4.switchEnharmonic(), false), spellNote(c4, false, 7))
    }

    @Test
    fun grandStaffShiftsOnlyOutsideLedgerRange() {
        // E6 and A1 are the outermost notes drawn with ledger lines
        assertEquals(StaffPlacement(44, 0), staffPlacement(44, StaffClef.Grand))
        assertEquals(StaffPlacement(12, 0), staffPlacement(12, StaffClef.Grand))
        // F6 is written an octave lower, marked with 8 above the treble clef
        assertEquals(StaffPlacement(38, -1), staffPlacement(45, StaffClef.Grand))
        // C8 -> 15 above
        assertEquals(StaffPlacement(42, -2), staffPlacement(56, StaffClef.Grand))
        // A0 and G1 are written an octave higher, marked with 8 below the bass clef
        assertEquals(StaffPlacement(12, 1), staffPlacement(5, StaffClef.Grand))
        assertEquals(StaffPlacement(17, 1), staffPlacement(10, StaffClef.Grand))
    }

    @Test
    fun tenorClefWritesOneOctaveHigher() {
        // A3 is written as A4 in the second space of the treble staff
        assertEquals(StaffPlacement(33, 1), staffPlacement(26, StaffClef.Tenor))
        // C5 written as C6 with two ledger lines
        assertEquals(StaffPlacement(42, 1), staffPlacement(35, StaffClef.Tenor))
        // E2 would need four ledger lines below -> one more octave up, 15 below the clef
        assertEquals(StaffPlacement(30, 2), staffPlacement(16, StaffClef.Tenor))
        // F3 written as F4 in the first space
        assertEquals(StaffPlacement(31, 1), staffPlacement(24, StaffClef.Tenor))
    }

    @Test
    fun singleStavesUseTheirOwnLedgerRange() {
        // treble: F3 is the lowest note with ledger lines, E3 is shifted
        assertEquals(StaffPlacement(24, 0), staffPlacement(24, StaffClef.Treble))
        assertEquals(StaffPlacement(30, 1), staffPlacement(23, StaffClef.Treble))
        // bass: G4 is the highest note with ledger lines, A4 is shifted
        assertEquals(StaffPlacement(32, 0), staffPlacement(32, StaffClef.Bass))
        assertEquals(StaffPlacement(26, -1), staffPlacement(33, StaffClef.Bass))
    }

    @Test
    fun grandStaffLedgerLines() {
        val g = StaffClef.Grand
        assertEquals(listOf(28), ledgerLinePositions(28, g).toList()) // middle C
        assertEquals(emptyList<Int>(), ledgerLinePositions(27, g).toList()) // B3 above bass staff
        assertEquals(emptyList<Int>(), ledgerLinePositions(29, g).toList()) // D4 below treble
        assertEquals(emptyList<Int>(), ledgerLinePositions(39, g).toList()) // G5 on top of staff
        assertEquals(listOf(40), ledgerLinePositions(41, g).toList()) // B5
        assertEquals(listOf(40, 42, 44), ledgerLinePositions(44, g).toList())
        assertEquals(emptyList<Int>(), ledgerLinePositions(17, g).toList()) // F2 below staff
        assertEquals(listOf(16, 14), ledgerLinePositions(13, g).toList())
        assertEquals(listOf(16, 14, 12), ledgerLinePositions(12, g).toList())
    }

    @Test
    fun singleStaffLedgerLines() {
        // middle C needs a ledger line below the treble and above the bass staff
        assertEquals(listOf(28), ledgerLinePositions(28, StaffClef.Treble).toList())
        assertEquals(listOf(28), ledgerLinePositions(28, StaffClef.Bass).toList())
        // D4 hangs below the treble staff, B3 sits above the bass staff
        assertEquals(emptyList<Int>(), ledgerLinePositions(29, StaffClef.Treble).toList())
        assertEquals(emptyList<Int>(), ledgerLinePositions(27, StaffClef.Bass).toList())
        assertEquals(listOf(28, 26, 24), ledgerLinePositions(24, StaffClef.Tenor).toList())
        assertEquals(listOf(28, 30, 32), ledgerLinePositions(32, StaffClef.Bass).toList())
    }

    @Test
    fun noteNamingFollowsKeyDirection() {
        // usual note names: sharp spelling first, flat spelling as enharmonic
        val sharpFirst = listOf(c4, cSharp4, f4)
        assertEquals(false, useEnharmonicForKey(2, sharpFirst))
        assertEquals(true, useEnharmonicForKey(-1, sharpFirst))
        // note names with flat spelling first
        val flatFirst = listOf(c4, dFlat4, f4)
        assertEquals(true, useEnharmonicForKey(3, flatFirst))
        assertEquals(false, useEnharmonicForKey(-2, flatFirst))
        // C major does not decide, neither do note names without sharp/flat pairs
        assertNull(useEnharmonicForKey(0, sharpFirst))
        assertNull(useEnharmonicForKey(1, listOf(c4, f4)))
    }
}
