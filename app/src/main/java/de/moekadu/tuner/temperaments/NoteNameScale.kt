package de.moekadu.tuner.temperaments

/** Note names, which can map indices between musical notes and via verse.
 * * @param notes Scale of note names, must contain the referenceNote. The octaves of
 *   the notes are not needed and must not be set.
 * @param defaultReferenceNote Reference note of the scale, which refers to noteIndex 0.
 *   This note must be part of the notes-array given as first argument.
 */
class NoteNameScale(
    val notes: Array<MusicalNote>,
    val defaultReferenceNote: MusicalNote
) {
    // TODO: test getNoteOfIndex and getIndexOfNote
    private val referenceNoteIndexWithinOctave = notes.indexOfFirst {
        MusicalNote.notesEqualIgnoreOctave(it, defaultReferenceNote)
    }
    private val referenceNoteOctave = defaultReferenceNote.octave

    /** Number of notes contained in the scale. */
    val size = notes.size

    /** Return note which belongs to a given index.
     * @param noteIndex Index of note.
     * @return Note belonging to index.
     */
    fun getNoteOfIndex(noteIndex: Int): MusicalNote {
        var octave = (noteIndex + referenceNoteIndexWithinOctave) / notes.size + referenceNoteOctave
        var localNoteIndex = (noteIndex + referenceNoteIndexWithinOctave) % notes.size
        if (localNoteIndex < 0) {
            octave -= 1
            localNoteIndex += notes.size
        }
//        Log.v("StaticLayoutTest", "NoteNameScale.getNoteOfIndex: noteIndex=$noteIndex, octave=$octave, localNoteIndex=$localNoteIndex, referenceNoteIndexWithinOctave=$referenceNoteIndexWithinOctave")
        return notes[localNoteIndex].copy(octave = octave)
    }

    /** Return index of a given note.
     * @param musicalNote Some musical note.
     * @return Index of given note or Int.MAX_VALUE if note is not part of the scale.
     */
    fun getIndexOfNote(musicalNote: MusicalNote): Int {
        val localNoteIndex = notes.indexOfFirst { MusicalNote.notesEqualIgnoreOctave(it, musicalNote) }
        return if (localNoteIndex < 0)
            Int.MAX_VALUE
        else
            (musicalNote.octave - referenceNoteOctave) * notes.size + localNoteIndex - referenceNoteIndexWithinOctave
    }

    /** Return a scale where the note names and modifiers are switch with their enharmonics.
     * @return New note name scale where notes and enharmonics are exchanged.
     */
    fun switchEnharmonic(): NoteNameScale {
        return NoteNameScale(notes.map {it.switchEnharmonic()}.toTypedArray(), defaultReferenceNote.switchEnharmonic())
    }
}

private val noteNameScale12ToneRefA4Sharp = createNoteNameScale12Tone(MusicalNote(BaseNote.A, NoteModifier.None, 4), preferFlat = false)

/** Return note for a note index as used in old versions, where the note index was seen as stable value.
 *  Previously the note index was always defined as a 12-tone index with A4 as reference note.
 *  In newer versions, the note index is only valid for a specific NoteNameScale.
 *  In order to still be able to read settings of older versions, this function allows to convert
 *  a note index stored with an old version to the corresponding musical note.
 *  @param noteIndex Note index as used in old versions.
 *  @return musical note corresponding to the note index.
 */
fun legacyNoteIndexToNote(noteIndex: Int): MusicalNote {
    return noteNameScale12ToneRefA4Sharp.getNoteOfIndex(noteIndex)
}

fun createNoteNameScale12Tone(referenceNote: MusicalNote?, preferFlat: Boolean): NoteNameScale {
    val referenceNoteResolved = referenceNote ?: MusicalNote(BaseNote.A, NoteModifier.None, 4)
    return if (preferFlat) {
        createNoteNameScale12Tone(referenceNote, preferFlat = false).switchEnharmonic()
    } else {
        NoteNameScale(
            arrayOf(
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.C, modifier = NoteModifier.Sharp, enharmonicBase = BaseNote.D, enharmonicModifier = NoteModifier.Flat),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.D, modifier = NoteModifier.Sharp, enharmonicBase = BaseNote.E, enharmonicModifier = NoteModifier.Flat),
                MusicalNote(base = BaseNote.E, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.F, modifier = NoteModifier.Sharp, enharmonicBase = BaseNote.G, enharmonicModifier = NoteModifier.Flat),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.G, modifier = NoteModifier.Sharp, enharmonicBase = BaseNote.A, enharmonicModifier = NoteModifier.Flat),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.None),
                MusicalNote(base = BaseNote.A, modifier = NoteModifier.Sharp, enharmonicBase = BaseNote.B, enharmonicModifier = NoteModifier.Flat),
                MusicalNote(base = BaseNote.B, modifier = NoteModifier.None)
            ),
            referenceNoteResolved
        )
    }
}
