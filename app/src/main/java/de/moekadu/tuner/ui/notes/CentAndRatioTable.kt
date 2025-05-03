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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.temperaments.Temperament3
import de.moekadu.tuner.temperaments.predefinedTemperamentWerckmeisterVI
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlin.math.roundToInt


/** Table showing cents are ratios between musical scale notes.
 * @param temperament Temperament.
 * @param rootNote Root Note.
 * @param notePrintOptions How to print the notes.
 * @param modifier Modifier.
 */
@Composable
fun CentAndRatioTable(
    temperament: Temperament3,
    rootNote: MusicalNote?,
    notePrintOptions: NotePrintOptions,
    modifier: Modifier = Modifier,
    horizontalContentPadding: Dp = 16.dp
    ) {

    val centArray = remember(temperament) {
        temperament.cents()
    }
    val rationalNumbers = remember(temperament) {
        temperament.rationalNumbers()
    }
    val noteNames = remember(temperament, rootNote) {
        temperament.noteNames(rootNote)
    }
    val notePrintOptionsDefault = remember(notePrintOptions) {
        notePrintOptions.copy(useEnharmonic = false)
    }
    val notePrintOptionsEnharmonic = remember(notePrintOptions) {
        notePrintOptions.copy(useEnharmonic = true)
    }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = horizontalContentPadding)
    ) {
        items(temperament.size + 1) {
            //val note = musicalScale.noteNameScale.getNoteOfIndex(rootNoteIndex + it)
            val note = noteNames[it % noteNames.size]

            Column(
                modifier = Modifier.width(IntrinsicSize.Max),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.height(40.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                    //contentAlignment = Alignment.Center
                ) {
                    Spacer(modifier = Modifier.width(16.dp))
                    if (note.base != BaseNote.None) {
                        Note(
                            note,
                            notePrintOptions = notePrintOptionsDefault,
                            withOctave = false,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (note.base != BaseNote.None && note.enharmonicBase != BaseNote.None) {
                        Text("/", Modifier.padding(horizontal = 2.dp))
                    }
                    if (note.enharmonicBase != BaseNote.None) {
                        Note(
                            note,
                            notePrintOptions = notePrintOptionsEnharmonic,
                            withOctave = false,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline))

                Text(
                    stringResource(id = R.string.cent_nosign, centArray[it].roundToInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (rationalNumbers != null) {
                    Fraction(
                        rationalNumbers[it].numerator,
                        rationalNumbers[it].denominator,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                    )
                }
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 200)
@Composable
private fun CentTablePreview() {
    TunerTheme {
//        val temperamentType = TemperamentType.WerckmeisterVI
//        val noteNameScale = NoteNameScaleFactory.create(temperamentType)
//        val rootNote = noteNameScale.notes[6]
        val notePrintOptions = NotePrintOptions(
            helmholtzNotation = false,
            notationType = NotationType.Standard
        )
        val temperament = remember{ predefinedTemperamentWerckmeisterVI(0L) }

        CentAndRatioTable(
            temperament,
            rootNote = null,
            notePrintOptions = notePrintOptions
        )
    }
}