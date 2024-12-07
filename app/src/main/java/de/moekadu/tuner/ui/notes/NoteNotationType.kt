/*
* Copyright 2024 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.moekadu.tuner.ui.notes

import androidx.annotation.StringRes
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.NoteModifier

/** Available note notation types. */
enum class NotationType(@StringRes val stringResourceId: Int) {
    Standard(R.string.notation_standard), /**< Default C, D, E, ...*/
    International(R.string.notation_international),  /**< C, D, E, ..., but using a B and never a H */
    Solfege(R.string.notation_solfege), /**< Solfege */
    Carnatic(R.string.notation_carnatic), /**< Carnatic Indian notation */
    Hindustani(R.string.notation_hindustani) /**< Hindustani Indian notation */
}

fun NotationType.resourceIds(): Map<NoteNameStem, Int> {
    return when (this) {
        NotationType.Standard -> noteResourceIds
        NotationType.International -> noteInternationalResourceIds
        NotationType.Solfege -> noteSolfegeResourceIds
        NotationType.Carnatic -> noteCarnaticResourceIds
        NotationType.Hindustani -> noteHindustaniResourceIds
    }
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
    NoteNameStem(BaseNote.D, NoteModifier.Sharp, BaseNote.E, NoteModifier.Flat) to R.string.ga1_carnatic,
    NoteNameStem(BaseNote.E) to R.string.ga2_carnatic,
    NoteNameStem(BaseNote.F) to R.string.ma1_carnatic,
    NoteNameStem(BaseNote.F, NoteModifier.Sharp, BaseNote.G, NoteModifier.Flat) to R.string.ma2_carnatic,
    NoteNameStem(BaseNote.G) to R.string.pa_carnatic,
    NoteNameStem(BaseNote.G, NoteModifier.Sharp, BaseNote.A, NoteModifier.Flat) to R.string.dha1_carnatic,
    NoteNameStem(BaseNote.A) to R.string.dha2_carnatic,
    NoteNameStem(BaseNote.A, NoteModifier.Sharp, BaseNote.B, NoteModifier.Flat) to R.string.ni1_carnatic,
    NoteNameStem(BaseNote.B) to R.string.ni2_carnatic,
)

private val noteHindustaniResourceIds = mapOf(
    NoteNameStem(BaseNote.C) to R.string.sa_hindustani,
    NoteNameStem(BaseNote.C, NoteModifier.Sharp, BaseNote.D, NoteModifier.Flat) to R.string.re1_hindustani,
    NoteNameStem(BaseNote.D) to R.string.re2_hindustani,
    NoteNameStem(BaseNote.D, NoteModifier.Sharp, BaseNote.E, NoteModifier.Flat) to R.string.ga1_hindustani,
    NoteNameStem(BaseNote.E) to R.string.ga2_hindustani,
    NoteNameStem(BaseNote.F) to R.string.ma1_hindustani,
    NoteNameStem(BaseNote.F, NoteModifier.Sharp, BaseNote.G, NoteModifier.Flat) to R.string.ma2_hindustani,
    NoteNameStem(BaseNote.G) to R.string.pa_hindustani,
    NoteNameStem(BaseNote.G, NoteModifier.Sharp, BaseNote.A, NoteModifier.Flat) to R.string.dha1_hindustani,
    NoteNameStem(BaseNote.A) to R.string.dha2_hindustani,
    NoteNameStem(BaseNote.A, NoteModifier.Sharp, BaseNote.B, NoteModifier.Flat) to R.string.ni1_hindustani,
    NoteNameStem(BaseNote.B) to R.string.ni2_hindustani,
)

//fun noteNameResourceIdOfStem(notationType: NotationType, stem: NoteNameStem): Int? {
//    return when (notationType) {
//        NotationType.Standard -> noteResourceIds[stem]
//        NotationType.International -> noteInternationalResourceIds[stem]
//        NotationType.Solfege -> noteSolfegeResourceIds[stem]
//        NotationType.Carnatic -> noteCarnaticResourceIds[stem]
//        NotationType.Hindustani -> noteHindustaniResourceIds[stem]
//    }
//}