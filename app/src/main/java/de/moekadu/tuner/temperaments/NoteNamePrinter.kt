package de.moekadu.tuner.temperaments

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.SuperscriptSpan
import de.moekadu.tuner.R

private val baseNoteResourceIds = mapOf(
    BaseNote.C to R.string.c_note_name,
    BaseNote.D to R.string.d_note_name,
    BaseNote.E to R.string.e_note_name,
    BaseNote.F to R.string.f_note_name,
    BaseNote.G to R.string.g_note_name,
    BaseNote.A to R.string.a_note_name,
    BaseNote.B to R.string.b_note_name,
)

private val specialNoteNameResourceIds = mapOf(
    NoteNameStem(BaseNote.B, NoteModifier.Flat, BaseNote.A, NoteModifier.Sharp) to R.string.asharp_bflat_note_name,
    NoteNameStem(BaseNote.A, NoteModifier.Sharp, BaseNote.B, NoteModifier.Flat) to R.string.asharp_bflat_note_name
)

enum class MusicalNotePrintOptions { None, PreferFlat, PreferSharp }

/** Create a char sequence which allows to print a note.
 * @param context Context for obtaining string resources
 * @param printOption Extra options for printing.
 * @param withOctave If true, the octave index will be printed, else it will be omitted.
 * @return SpannableString of the note.
 */
fun MusicalNote.toCharSequence(context: Context, printOption: MusicalNotePrintOptions = MusicalNotePrintOptions.None, withOctave: Boolean = true): CharSequence {
    val spannableStringBuilder = SpannableStringBuilder()

    val specialNoteNameResourceId = specialNoteNameResourceIds[NoteNameStem.fromMusicalNote(this)]
    val specialNoteName = if (specialNoteNameResourceId == null)
        null
    else
        context.getString(specialNoteNameResourceId)

    val octaveToPrintIfEnabled: Int

    // if the special note name is "-", it means that for the given translation there actually
    // is not special note name.
    if (specialNoteName != null && specialNoteName != "") {
        spannableStringBuilder.append(specialNoteName)
        octaveToPrintIfEnabled = this.octave
    } else {
        val baseToPrint: BaseNote
        val modifierToPrint: NoteModifier
        if ((printOption == MusicalNotePrintOptions.PreferFlat && this.enharmonicBase != BaseNote.None && this.enharmonicModifier == NoteModifier.Flat)
            || (printOption == MusicalNotePrintOptions.PreferSharp && this.enharmonicBase != BaseNote.None && this.enharmonicModifier == NoteModifier.Sharp)) {
            baseToPrint = this.enharmonicBase
            modifierToPrint = this.enharmonicModifier
            octaveToPrintIfEnabled = if (this.octave == Int.MAX_VALUE) Int.MAX_VALUE else this.octave + this.enharmonicOctaveOffset
        } else {
            baseToPrint = this.base
            modifierToPrint = this.modifier
            octaveToPrintIfEnabled = this.octave
        }

        spannableStringBuilder.append(context.getString(baseNoteResourceIds[baseToPrint]!!))
        val modifierString = when (modifierToPrint) {
            NoteModifier.None -> ""
            NoteModifier.Sharp -> "\u266F"
            NoteModifier.Flat -> "\u266D"
        }
        spannableStringBuilder.append(modifierString)
    }

    if (this.octave != Int.MAX_VALUE && withOctave) {

        spannableStringBuilder.append(
            SpannableString(octaveToPrintIfEnabled.toString()).apply {
                setSpan(SuperscriptSpan(),0, length,0)
            }
        )
    }
    return spannableStringBuilder
}
