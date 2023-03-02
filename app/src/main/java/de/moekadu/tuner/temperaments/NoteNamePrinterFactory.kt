package de.moekadu.tuner.temperaments

import android.content.Context
import de.moekadu.tuner.R

enum class NotationType {
    Standard,
    International,
    Solfege,
    Carnatic,
    Hindustani
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

private val noteCarnaticResourceIds = mapOf(
    NoteNameStem(BaseNote.C) to R.string.sa_carnatic,
    NoteNameStem(BaseNote.C, NoteModifier.Sharp, BaseNote.D, NoteModifier.Flat) to R.string.ri1_carnatic,
    NoteNameStem(BaseNote.D) to R.string.ri2_carnatic,
    NoteNameStem(BaseNote.None, enharmonicBaseNote = BaseNote.D) to R.string.ga1_carnatic,
    NoteNameStem(BaseNote.D, NoteModifier.Sharp, BaseNote.E, NoteModifier.Flat) to R.string.ri3_carnatic,
    NoteNameStem(BaseNote.E, NoteModifier.Flat, BaseNote.D, NoteModifier.Sharp) to R.string.ga2_carnatic,
    NoteNameStem(BaseNote.E) to R.string.ga3_carnatic,
    NoteNameStem(BaseNote.F) to R.string.ma1_carnatic,
    NoteNameStem(BaseNote.F, NoteModifier.Sharp, BaseNote.G, NoteModifier.Flat) to R.string.ma2_carnatic,
    NoteNameStem(BaseNote.G) to R.string.pa_carnatic,
    NoteNameStem(BaseNote.G, NoteModifier.Sharp, BaseNote.A, NoteModifier.Flat) to R.string.dha1_carnatic,
    NoteNameStem(BaseNote.A) to R.string.ni1_carnatic,
    NoteNameStem(BaseNote.None, enharmonicBaseNote = BaseNote.A) to R.string.dha2_carnatic,
    NoteNameStem(BaseNote.A, NoteModifier.Sharp, BaseNote.B, NoteModifier.Flat) to R.string.ni2_carnatic,
    NoteNameStem(BaseNote.B, NoteModifier.Flat, BaseNote.A, NoteModifier.Sharp) to R.string.dha3_carnatic,
    NoteNameStem(BaseNote.B) to R.string.ni3_carnatic,
)

private val noteHindustaniResourceIds = mapOf(
    NoteNameStem(BaseNote.C) to R.string.sa_hindustani,
    NoteNameStem(BaseNote.C, NoteModifier.Sharp, BaseNote.D, NoteModifier.Flat) to R.string.re1_hindustani,
    NoteNameStem(BaseNote.D) to R.string.re2_hindustani,
    NoteNameStem(BaseNote.None, enharmonicBaseNote = BaseNote.D) to R.string.ga1_hindustani,
    NoteNameStem(BaseNote.D, NoteModifier.Sharp, BaseNote.E, NoteModifier.Flat) to R.string.re3_hindustani,
    NoteNameStem(BaseNote.E, NoteModifier.Flat, BaseNote.D, NoteModifier.Sharp) to R.string.ga2_hindustani,
    NoteNameStem(BaseNote.E) to R.string.ga3_hindustani,
    NoteNameStem(BaseNote.F) to R.string.ma1_hindustani,
    NoteNameStem(BaseNote.F, NoteModifier.Sharp, BaseNote.G, NoteModifier.Flat) to R.string.ma2_hindustani,
    NoteNameStem(BaseNote.G) to R.string.pa_hindustani,
    NoteNameStem(BaseNote.G, NoteModifier.Sharp, BaseNote.A, NoteModifier.Flat) to R.string.dha1_hindustani,
    NoteNameStem(BaseNote.A) to R.string.ni1_hindustani,
    NoteNameStem(BaseNote.None, enharmonicBaseNote = BaseNote.A) to R.string.dha2_hindustani,
    NoteNameStem(BaseNote.A, NoteModifier.Sharp, BaseNote.B, NoteModifier.Flat) to R.string.ni2_hindustani,
    NoteNameStem(BaseNote.B, NoteModifier.Flat, BaseNote.A, NoteModifier.Sharp) to R.string.dha3_hindustani,
    NoteNameStem(BaseNote.B) to R.string.ni3_hindustani,
)

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
        NotationType.Carnatic -> {
            NoteNamePrinter(
                context, sharpFlatPreference, noteCarnaticResourceIds,
                NoteNamePrinter.MaxNoteNameWidth.MultipleLetters
            )
        }
        NotationType.Hindustani -> {
            NoteNamePrinter(
                context, sharpFlatPreference, noteHindustaniResourceIds,
                NoteNamePrinter.MaxNoteNameWidth.MultipleLetters
            )
        }
    }
}
