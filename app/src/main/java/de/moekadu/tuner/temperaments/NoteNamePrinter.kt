package de.moekadu.tuner.temperaments

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.SuperscriptSpan
import de.moekadu.tuner.R

private data class NoteNameStem(val baseNote: BaseNote, val modifier: NoteModifier,
                                val enharmonicBaseNote: BaseNote, val enharmonicModifier: NoteModifier) {
    companion object {
        fun fromMusicalNote(note: MusicalNote): NoteNameStem {
            return NoteNameStem(note.base, note.modifier, note.enharmonicBase, note.enharmonicModifier)
        }
    }
}

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

    // if the special note name is "-", it means that for the given translation there actually
    // is not special note name.
    if (specialNoteName != null && specialNoteName != "") {
        spannableStringBuilder.append(specialNoteName)
    } else {
        val baseToPrint: BaseNote
        val modifierToPrint: NoteModifier
        if ((printOption == MusicalNotePrintOptions.PreferFlat && this.enharmonicBase != BaseNote.None && this.enharmonicModifier == NoteModifier.Flat)
            || (printOption == MusicalNotePrintOptions.PreferSharp && this.enharmonicBase != BaseNote.None && this.enharmonicModifier == NoteModifier.Sharp)) {
            baseToPrint = this.enharmonicBase
            modifierToPrint = this.enharmonicModifier
        } else {
            baseToPrint = this.base
            modifierToPrint = this.modifier
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
            SpannableString(this.octave.toString()).apply {
                setSpan(SuperscriptSpan(),0, length,0)
            }
        )
    }
    return spannableStringBuilder
}

data class NoteNameMeasures(val minWidth: Float, val maxWidth: Float, val minHeight: Float, val maxHeight: Float)

//fun NoteNameScale.getNoteNameMeasures(context: Context, octaveBegin: Int, octaveEnd: Int, paint: TextPaint? = null, printOption: MusicalNotePrintOptions = MusicalNotePrintOptions.None): NoteNameMeasures {
//    val label = notes[0].toCharSequence(context, printOption, withOctave = false)
//    val paintResolved = paint ?: TextPaint()
//    val desiredWidth = ceil(StaticLayout.getDesiredWidth(label, paint)).toInt()
//    val layout = StaticLayout.Builder.obtain(label, 0, label.length, paintResolved, desiredWidth).build()
//}