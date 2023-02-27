package de.moekadu.tuner.temperaments

import android.content.Context
import de.moekadu.tuner.R

enum class NotationType {
    Standard,
    International,
    Solfege
}

// if more special note names are needed, (like ashparp_bflat -> B in German), this can easily
// be added here. Just make sure that the standard translation is "-", such that it will be ignored
// for languages, which don't have these special names.
private val noteResourceIds = mapOf(
    NoteNameStem(BaseNote.C) to R.string.c_note_name,
    NoteNameStem(BaseNote.D) to R.string.d_note_name,
    NoteNameStem(BaseNote.E) to R.string.e_note_name,
    NoteNameStem(BaseNote.F) to R.string.f_note_name,
    NoteNameStem(BaseNote.G) to R.string.g_note_name,
    NoteNameStem(BaseNote.A) to R.string.a_note_name,
    NoteNameStem(BaseNote.A, NoteModifier.Sharp, BaseNote.B, NoteModifier.Flat) to R.string.asharp_bflat_note_name,
    NoteNameStem(BaseNote.B) to R.string.b_note_name,
)

private val noteSolfegeResourceIds = mapOf(
    NoteNameStem(BaseNote.C) to R.string.c_solfege,
    NoteNameStem(BaseNote.D) to R.string.d_solfege,
    NoteNameStem(BaseNote.E) to R.string.e_solfege,
    NoteNameStem(BaseNote.F) to R.string.f_solfege,
    NoteNameStem(BaseNote.G) to R.string.g_solfege,
    NoteNameStem(BaseNote.A) to R.string.a_solfege,
    NoteNameStem(BaseNote.B) to R.string.b_solfege,
)

private val noteInternationalResourceIds = mapOf(
    NoteNameStem(BaseNote.C) to R.string.c_note_international,
    NoteNameStem(BaseNote.D) to R.string.d_note_international,
    NoteNameStem(BaseNote.E) to R.string.e_note_international,
    NoteNameStem(BaseNote.F) to R.string.f_note_international,
    NoteNameStem(BaseNote.G) to R.string.g_note_international,
    NoteNameStem(BaseNote.A) to R.string.a_note_international,
    NoteNameStem(BaseNote.B) to R.string.b_note_international,
)

//private val noteCarnaticResourceIds = mapOf(
//    NoteNameStem(BaseNote.C) to R.string.c_carnatic,
//    NoteNameStem(BaseNote.C, NoteModifier.Sharp, BaseNote.D, NoteModifier.Flat) to R.string.csharp_dflat_carnatic,
//    NoteNameStem(BaseNote.D) to R.string.d_carnatic,
//    NoteNameStem(BaseNote.D, NoteModifier.Sharp, BaseNote.E, NoteModifier.Flat) to R.string.dsharp_eflat_carnatic,
//    NoteNameStem(BaseNote.E) to R.string.e_carnatic,
//    NoteNameStem(BaseNote.F) to R.string.f_carnatic,
//    NoteNameStem(BaseNote.F, NoteModifier.Sharp, BaseNote.G, NoteModifier.Flat) to R.string.fsharp_gflat_carnatic,
//    NoteNameStem(BaseNote.G) to R.string.g_carnatic,
//    NoteNameStem(BaseNote.G, NoteModifier.Sharp, BaseNote.A, NoteModifier.Flat) to R.string.gsharp_aflat_carnatic,
//    NoteNameStem(BaseNote.A) to R.string.a_carnatic,
//    NoteNameStem(BaseNote.A, NoteModifier.Sharp, BaseNote.B, NoteModifier.Flat) to R.string.asharp_bflat_carnatic,
//    NoteNameStem(BaseNote.B) to R.string.b_carnatic,
//)

fun createNoteNamePrinter(
    context: Context,
    notationType: NotationType,
    sharpFlatPreference: NoteNamePrinter.SharpFlatPreference): NoteNamePrinter {
    return when (notationType) {
        NotationType.Standard -> {
            NoteNamePrinter(
                context, sharpFlatPreference, noteResourceIds,
                NoteNamePrinter.MaxNoteNameWidth.SingleLetter
            )
        }
        NotationType.International -> {
            NoteNamePrinter(
                context, sharpFlatPreference, noteInternationalResourceIds,
                NoteNamePrinter.MaxNoteNameWidth.SingleLetter
            )
        }
        NotationType.Solfege -> {
            NoteNamePrinter(
                context, sharpFlatPreference, noteSolfegeResourceIds,
                NoteNamePrinter.MaxNoteNameWidth.MultipleLetters
            )
        }
    }
}
