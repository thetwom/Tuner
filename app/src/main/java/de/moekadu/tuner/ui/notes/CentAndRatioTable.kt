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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import de.moekadu.tuner.temperaments2.MusicalScale2
import de.moekadu.tuner.temperaments2.MusicalScale2Factory
import de.moekadu.tuner.temperaments2.NoteNames
import de.moekadu.tuner.temperaments2.Temperament
import de.moekadu.tuner.temperaments2.createTestTemperamentEdo12
import de.moekadu.tuner.temperaments2.createTestTemperamentWerckmeisterVI
import de.moekadu.tuner.temperaments2.getSuitableNoteNames
import de.moekadu.tuner.temperaments2.ratioToCents
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlin.math.roundToInt

///** Table showing cents are ratios between musical scale notes.
// * @param musicalScale Musical scale which defines the notes shown in the table.
// * @param notePrintOptions How to print the notes.
// * @param modifier Modifier.
// */
//@Composable
//fun CentAndRatioTable(
//    musicalScale: MusicalScale,
//    notePrintOptions: NotePrintOptions,
//    modifier: Modifier = Modifier) {
//
//    val rootNoteIndex = remember(musicalScale) {
//        val rootNote4 = musicalScale.rootNote.copy(octave = 4)
//        musicalScale.getNoteIndex(rootNote4)
//    }
//    val rootNoteFrequency = remember(rootNoteIndex) {
//        musicalScale.getNoteFrequency(rootNoteIndex)
//    }
//
//    val centArray = remember(musicalScale) {
//        IntArray(musicalScale.numberOfNotesPerOctave + 1) {
//            ratioToCents(
//                musicalScale.getNoteFrequency(it + rootNoteIndex)
//                        / rootNoteFrequency
//            ).roundToInt()
//        }
//    }
//
//    LazyRow(modifier = modifier) {
//        items(musicalScale.numberOfNotesPerOctave + 1) {
//            //val note = musicalScale.noteNameScale.getNoteOfIndex(rootNoteIndex + it)
//            val note = musicalScale.getNote(rootNoteIndex + it)
//            Column(
//                modifier = Modifier.width(IntrinsicSize.Max),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Box(
//                    modifier = Modifier.height(40.dp),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Note(
//                        note,
//                        notePrintOptions = notePrintOptions,
//                        withOctave = false,
//                        fontWeight = FontWeight.Bold,
//                        style = MaterialTheme.typography.bodyMedium,
//                        modifier = Modifier.padding(horizontal = 16.dp)
//                    )
//                }
//                Spacer(modifier = Modifier
//                    .fillMaxWidth()
//                    .height(1.dp)
//                    .background(MaterialTheme.colorScheme.outline))
//
//                Text(
//                    stringResource(id = R.string.cent_nosign, centArray[it]),
//                    style = MaterialTheme.typography.bodyMedium,
//                    maxLines = 1,
//                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
//                )
//
//                musicalScale.rationalNumberRatios?.let { rationalNumbers ->
//                    Fraction(
//                        rationalNumbers[it].numerator,
//                        rationalNumbers[it].denominator,
//                        style = MaterialTheme.typography.bodyMedium,
//                        modifier = Modifier
//                            .padding(horizontal = 16.dp)
//                            .padding(bottom = 8.dp)
//                    )
//                }
//                Spacer(modifier = Modifier
//                    .fillMaxWidth()
//                    .height(1.dp)
//                    .background(MaterialTheme.colorScheme.outline))
//            }
//        }
//    }
//}

/** Table showing cents are ratios between musical scale notes.
  * @param temperament Temperament.
  * @param noteNames Note names to print.
  * @param rootNoteIndex Index of root (first) note within the note names.
  * @param notePrintOptions How to print the notes.
 * @param modifier Modifier.
 */
@Composable
fun CentAndRatioTable(
    temperament: Temperament,
    noteNames: NoteNames,
    rootNoteIndex: Int,
    notePrintOptions: NotePrintOptions,
    modifier: Modifier = Modifier,
    horizontalContentPadding: Dp = 16.dp
    ) {
    val centArray = temperament.cents

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = horizontalContentPadding)
    ) {
        items(temperament.numberOfNotesPerOctave + 1) {
            //val note = musicalScale.noteNameScale.getNoteOfIndex(rootNoteIndex + it)
            val note = noteNames[(rootNoteIndex + it) % noteNames.size]

            Column(
                modifier = Modifier.width(IntrinsicSize.Max),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.height(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Note(
                        note,
                        notePrintOptions = notePrintOptions,
                        withOctave = false,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
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

                temperament.rationalNumbers?.let { rationalNumbers ->
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
        val temperament = remember { createTestTemperamentWerckmeisterVI() }
        val noteNames = remember {
            getSuitableNoteNames(temperament.numberOfNotesPerOctave)!!
        }

        CentAndRatioTable(
            temperament,
            noteNames,
            8,
            notePrintOptions = notePrintOptions
        )
    }
}