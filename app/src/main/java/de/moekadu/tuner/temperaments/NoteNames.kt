package de.moekadu.tuner.temperaments

import android.content.Context
import android.text.SpannableString
import android.text.style.SuperscriptSpan
import de.moekadu.tuner.R

/** Note names for standard 12-tone scale, and where A4 is the note at note index 0. */
val noteNames12Tone = NoteNames(
    noteNames = intArrayOf(
        R.string.c_note_name,
        R.string.csharp_note_name,
        R.string.d_note_name,
        R.string.dsharp_note_name,
        R.string.e_note_name,
        R.string.f_note_name,
        R.string.fsharp_note_name,
        R.string.g_note_name,
        R.string.gsharp_note_name,
        R.string.a_note_name,
        R.string.asharp_note_name,
        R.string.b_note_name
    ),
    octavesForNoteIndex0 = 4,
    arrayIndexForNoteIndex0 = 9
)

/** Note names for standard 19-tone scale, and where A4 is the note at note index 0 */
val noteNames19Tone = NoteNames(
    noteNames = intArrayOf(
        R.string.c_note_name,
        R.string.csharp_note_name,
        R.string.dflat_note_name,
        R.string.d_note_name,
        R.string.dsharp_note_name,
        R.string.eflat_note_name,
        R.string.e_note_name,
        R.string.esharp_note_name,
        R.string.f_note_name,
        R.string.fsharp_note_name,
        R.string.gflat_note_name,
        R.string.g_note_name,
        R.string.gsharp_note_name,
        R.string.aflat_note_name,
        R.string.a_note_name,
        R.string.asharp_note_name,
        R.string.bflat_note_name,
        R.string.b_note_name,
        R.string.bsharp_note_name
    ),
    octavesForNoteIndex0 = 4,
    arrayIndexForNoteIndex0 = 14
)

/** Base class, defining names of notes.
 *
 * @param noteNames Resource ids of note names. If the resource id points to a string "-",
 *   we will use the note before or after this index and add a sharp/flat character.
 * @param octavesForNoteIndex0 Tells at which octave the note at index [0] is (for 12-tone A4, this would be 4)
 * @param arrayIndexForNoteIndex0 Tells at which index within the noteNames-array the note with index [0] is
 *   (for 12-tone A4, this would be 9)
 */
class NoteNames(private val noteNames: IntArray,
                private val octavesForNoteIndex0: Int,
                private val arrayIndexForNoteIndex0: Int) {

    /** Get note name for a given note index.
     *
     * @param context Context needed to get string resources.
     * @param toneIndex Note index as e.g. returned by getClosestToneIndex. Two succeeding
     *   indices give a distance of one half tone.
     * @param preferFlat If the best fitting note is flat or sharp and this parameter is true,
     *   the "flat" version is preferred. Else the sharp version is returned.
     *  @param withOctaveIndex If true, we return the standard notation, e.g. A#4, if false
     *   we do not add the octave index, so it would e.g. A#.
     * @return Note name.
     */
    fun getNoteName(context: Context, toneIndex : Int, preferFlat : Boolean,
                    withOctaveIndex: Boolean = true) : CharSequence {
        var octaveIndex = (toneIndex + arrayIndexForNoteIndex0) / noteNames.size + octavesForNoteIndex0
        var noteIndexWithinOctave = (toneIndex + arrayIndexForNoteIndex0) % noteNames.size
        if (noteIndexWithinOctave < 0) {
            octaveIndex -= 1
            noteIndexWithinOctave += noteNames.size
        }

        // negative distances mean that our note is at a lower index, but sharp
        // positive distances mean that our note is at a higher index, but flat
        val noteDist = findClosestNoteDistance(context, noteIndexWithinOctave, preferFlat=preferFlat)

        noteIndexWithinOctave += noteDist
        if (noteIndexWithinOctave >= noteNames.size) {
            noteIndexWithinOctave -= noteNames.size
            octaveIndex += 1
        } else if (noteIndexWithinOctave < 0) {
            noteIndexWithinOctave += noteNames.size
            octaveIndex -= 1
        }

        val noteName = context.getString(noteNames[noteIndexWithinOctave])
        var noteModifier = ""

        if (noteDist == -1)
            noteModifier = "\u266F"
        else if (noteDist == 1)
            noteModifier = "\u266D"

        val result = if (withOctaveIndex) {
            val octaveText = octaveIndex.toString()
            SpannableString(noteName + noteModifier + octaveText).apply {
                setSpan(SuperscriptSpan(), noteName.length + noteModifier.length, length, 0)
            }
        } else {
            SpannableString(noteName + noteModifier)
        }
        return result
    }

    private fun findClosestNoteDistance(context: Context, noteIndexWithinOctave: Int, preferFlat: Boolean): Int {
        if (context.getString(noteNames[noteIndexWithinOctave]) != "-")
            return 0
        var i = -1
        while (true) {
            var noteIndex = noteIndexWithinOctave + i
            if (noteIndex < 0)
                noteIndex += noteNames.size
            if (context.getString(noteNames[noteIndex]) != "-")
                break
            --i
        }
        if (i == -1 && !preferFlat)
            return -1

        var j = 1
        while (true) {
            var noteIndex = noteIndexWithinOctave + j
            if (noteIndex >= noteNames.size)
                noteIndex -= noteNames.size
            if (context.getString(noteNames[noteIndex]) != "-")
                break
            ++j
        }

        return when {
            -i == j && preferFlat -> j
            -i == j && !preferFlat -> i
            -i < j -> i
            -i > j -> j
            else -> throw RuntimeException("Invalid case")
        }
    }
}