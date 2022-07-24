package de.moekadu.tuner.temperaments

import kotlin.math.min
import kotlin.math.roundToInt

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
    /** Reference note index within the octave (index in notes). */
    private val referenceNoteIndexWithinOctave = notes.indexOfFirst {
        MusicalNote.notesEqualIgnoreOctave(it, defaultReferenceNote)
    }

    /** Octave of reference note. */
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

    fun hasNote(note: MusicalNote): Boolean {
        return notes.any { MusicalNote.notesEqualIgnoreOctave(it, note) }
    }

    /** Return the note which is closest to another note of another note name scale.
     * Note: At the moment we either return the same note if it exists (we at least make sure
     *   that the enharmonic representation is the same) or, we assume equally distributed notes
     *   and return the note at the closest relative position.
     * @param note Note in another NoteNameScale
     * @param noteNameScaleOfNote NoteNameScale of note, which is given as first argument.
     * @return note which is part of the own note name scale, which is closest to the given note.
     */
    fun getClosestNote(note: MusicalNote, noteNameScaleOfNote: NoteNameScale): MusicalNote {
        // check if note exists in this scale
        val closeNote = notes.firstOrNull { MusicalNote.notesEqualIgnoreOctave(it, note) }
        if (closeNote != null && closeNote.base == note.base)
            return note
        else if (closeNote != null && closeNote.enharmonicBase == note.base)
            return note.switchEnharmonic()
        // find the closest note by using the relative position.
        val noteIndexOther = noteNameScaleOfNote.notes.indexOfFirst { MusicalNote.notesEqualIgnoreOctave(it, note) }
        val notePositionPercent = (noteIndexOther).toDouble() / (noteNameScaleOfNote.size).toDouble()
        val noteIndex = min((notePositionPercent * size).roundToInt(), size - 1)
        return notes[noteIndex].copy(octave = note.octave)
    }
}

private val noteNameScale12ToneRefA4Sharp = createNoteNameScale12Tone(MusicalNote(BaseNote.A, NoteModifier.None, 4))

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

fun createNoteNameScale12Tone(referenceNote: MusicalNote?): NoteNameScale {
    val referenceNoteResolved = referenceNote ?: MusicalNote(BaseNote.A, NoteModifier.None, 4)
    return NoteNameScale(
        arrayOf(
            MusicalNote(base = BaseNote.C, modifier = NoteModifier.None),
            MusicalNote(
                base = BaseNote.C,
                modifier = NoteModifier.Sharp,
                enharmonicBase = BaseNote.D,
                enharmonicModifier = NoteModifier.Flat
            ),
            MusicalNote(base = BaseNote.D, modifier = NoteModifier.None),
            MusicalNote(
                base = BaseNote.D,
                modifier = NoteModifier.Sharp,
                enharmonicBase = BaseNote.E,
                enharmonicModifier = NoteModifier.Flat
            ),
            MusicalNote(base = BaseNote.E, modifier = NoteModifier.None),
            MusicalNote(base = BaseNote.F, modifier = NoteModifier.None),
            MusicalNote(
                base = BaseNote.F,
                modifier = NoteModifier.Sharp,
                enharmonicBase = BaseNote.G,
                enharmonicModifier = NoteModifier.Flat
            ),
            MusicalNote(base = BaseNote.G, modifier = NoteModifier.None),
            MusicalNote(
                base = BaseNote.G,
                modifier = NoteModifier.Sharp,
                enharmonicBase = BaseNote.A,
                enharmonicModifier = NoteModifier.Flat
            ),
            MusicalNote(base = BaseNote.A, modifier = NoteModifier.None),
            MusicalNote(
                base = BaseNote.A,
                modifier = NoteModifier.Sharp,
                enharmonicBase = BaseNote.B,
                enharmonicModifier = NoteModifier.Flat
            ),
            MusicalNote(base = BaseNote.B, modifier = NoteModifier.None)
        ),
        referenceNoteResolved
    )
}

fun createNoteNameScale19Tone(referenceNote: MusicalNote?): NoteNameScale {
    val referenceNoteResolved = referenceNote ?: MusicalNote(BaseNote.A, NoteModifier.None, 4)
    return NoteNameScale(
        arrayOf(
            MusicalNote(base = BaseNote.C, modifier = NoteModifier.None),
            MusicalNote(base = BaseNote.C, modifier = NoteModifier.Sharp),
            MusicalNote(base = BaseNote.D, modifier = NoteModifier.Flat),
            MusicalNote(base = BaseNote.D, modifier = NoteModifier.None),
            MusicalNote(base = BaseNote.D, modifier = NoteModifier.Sharp),
            MusicalNote(base = BaseNote.E, modifier = NoteModifier.Flat),
            MusicalNote(base = BaseNote.E, modifier = NoteModifier.None),
            MusicalNote(base = BaseNote.E, modifier = NoteModifier.Sharp),
            MusicalNote(base = BaseNote.F, modifier = NoteModifier.None),
            MusicalNote(base = BaseNote.F, modifier = NoteModifier.Sharp),
            MusicalNote(base = BaseNote.G, modifier = NoteModifier.Flat),
            MusicalNote(base = BaseNote.G, modifier = NoteModifier.None),
            MusicalNote(base = BaseNote.G, modifier = NoteModifier.Sharp),
            MusicalNote(base = BaseNote.A, modifier = NoteModifier.Flat),
            MusicalNote(base = BaseNote.A, modifier = NoteModifier.None),
            MusicalNote(base = BaseNote.A, modifier = NoteModifier.Sharp),
            MusicalNote(base = BaseNote.B, modifier = NoteModifier.Flat),
            MusicalNote(base = BaseNote.B, modifier = NoteModifier.None),
            MusicalNote(base = BaseNote.B, modifier = NoteModifier.Sharp)
        ),
        referenceNoteResolved
    )
}

fun createNoteNameScale41Tone(referenceNote: MusicalNote?): NoteNameScale {
    val referenceNoteResolved = referenceNote ?: MusicalNote(BaseNote.A, NoteModifier.None, 4)
    return NoteNameScale(
        arrayOf(
            MusicalNote(base = BaseNote.C, modifier = NoteModifier.None),
            MusicalNote(base = BaseNote.C, modifier = NoteModifier.NaturalUp),
            MusicalNote(base = BaseNote.D, modifier = NoteModifier.FlatDown),
            MusicalNote(base = BaseNote.D, modifier = NoteModifier.Flat),
            MusicalNote(base = BaseNote.C, modifier = NoteModifier.SharpDown),
            MusicalNote(base = BaseNote.C, modifier = NoteModifier.SharpUp),
            MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalDown),
            //
            MusicalNote(base = BaseNote.D, modifier = NoteModifier.None),
            MusicalNote(base = BaseNote.D, modifier = NoteModifier.NaturalUp),
            MusicalNote(base = BaseNote.E, modifier = NoteModifier.FlatDown),
            MusicalNote(base = BaseNote.E, modifier = NoteModifier.Flat),
            MusicalNote(base = BaseNote.D, modifier = NoteModifier.SharpDown),
            MusicalNote(base = BaseNote.D, modifier = NoteModifier.SharpUp),
            MusicalNote(base = BaseNote.E, modifier = NoteModifier.NaturalDown),
            //
            MusicalNote(base = BaseNote.E, modifier = NoteModifier.None),
            MusicalNote(base = BaseNote.E, modifier = NoteModifier.NaturalUp),
            MusicalNote(base = BaseNote.F, modifier = NoteModifier.NaturalDown),
            //
            MusicalNote(base = BaseNote.F, modifier = NoteModifier.None),
            MusicalNote(base = BaseNote.F, modifier = NoteModifier.NaturalUp),
            MusicalNote(base = BaseNote.G, modifier = NoteModifier.FlatDown),
            MusicalNote(base = BaseNote.G, modifier = NoteModifier.Flat),
            MusicalNote(base = BaseNote.F, modifier = NoteModifier.SharpDown),
            MusicalNote(base = BaseNote.F, modifier = NoteModifier.SharpUp),
            MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalDown),
            //
            MusicalNote(base = BaseNote.G, modifier = NoteModifier.None),
            MusicalNote(base = BaseNote.G, modifier = NoteModifier.NaturalUp),
            MusicalNote(base = BaseNote.A, modifier = NoteModifier.FlatDown),
            MusicalNote(base = BaseNote.A, modifier = NoteModifier.Flat),
            MusicalNote(base = BaseNote.G, modifier = NoteModifier.SharpDown),
            MusicalNote(base = BaseNote.G, modifier = NoteModifier.SharpUp),
            MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalDown),
            //
            MusicalNote(base = BaseNote.A, modifier = NoteModifier.None),
            MusicalNote(base = BaseNote.A, modifier = NoteModifier.NaturalUp),
            MusicalNote(base = BaseNote.B, modifier = NoteModifier.FlatDown),
            MusicalNote(base = BaseNote.B, modifier = NoteModifier.Flat),
            MusicalNote(base = BaseNote.A, modifier = NoteModifier.SharpDown),
            MusicalNote(base = BaseNote.A, modifier = NoteModifier.SharpUp),
            MusicalNote(base = BaseNote.B, modifier = NoteModifier.NaturalDown),
            //
            MusicalNote(base = BaseNote.B, modifier = NoteModifier.None),
            MusicalNote(base = BaseNote.B, modifier = NoteModifier.NaturalUp),
            MusicalNote(base = BaseNote.C, modifier = NoteModifier.NaturalDown),
            ),
        referenceNoteResolved
    )
}