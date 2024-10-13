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
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.misc.StringOrResId
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.NoteNameScaleFactory
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.temperaments2.MusicalScale2
import de.moekadu.tuner.temperaments2.MusicalScale2Factory
import de.moekadu.tuner.temperaments2.StretchTuning
import de.moekadu.tuner.temperaments2.Temperament
import de.moekadu.tuner.temperaments2.ratioToCents
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt

/** Table showing cents between musical scale notes.
 * @param musicalScale Musical scale which defines the notes shown in the table.
 * @param notePrintOptions How to print the notes.
 * @param modifier Modifier.
 */
@Composable
fun CentTable(
    musicalScale: MusicalScale2,
    notePrintOptions: NotePrintOptions,
    modifier: Modifier = Modifier) {

    val rootNoteIndex = remember(musicalScale) {
        val rootNote4 = musicalScale.rootNote.copy(octave = 4)
        musicalScale.getNoteIndex(rootNote4)
    }

    val centArray = musicalScale.temperament.cents

    LazyRow(modifier = modifier) {
        items(musicalScale.numberOfNotesPerOctave + 1) {
            //val note = musicalScale.noteNameScale.getNoteOfIndex(rootNoteIndex + it)
            val note = musicalScale.getNote(rootNoteIndex + it)
            Column(
                modifier = Modifier.width(IntrinsicSize.Max),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Note(
                        note,
                        notePrintOptions = notePrintOptions,
                        withOctave = false,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    stringResource(id = R.string.cent_nosign, centArray[it].roundToInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 200)
@Composable
private fun CentTablePreview() {
    TunerTheme {
        val temperamentType = TemperamentType.WerckmeisterVI
        val noteNameScale = NoteNameScaleFactory.create(temperamentType)
        val notePrintOptions = NotePrintOptions(
            sharpFlatPreference = NotePrintOptions.SharpFlatPreference.Sharp,
            helmholtzNotation = false,
            notationType = NotationType.Standard
        )
        val musicalScale = MusicalScale2Factory.create(
            Temperament.create(
                StringOrResId("Test 1"),
                StringOrResId("A 1"),
                StringOrResId("Description 1"),
                12,
                1
            ),
            noteNames = null,
            rootNote = null,
            referenceNote = null,
            referenceFrequency = 440f,
            frequencyMin = 10f,
            frequencyMax = 20000f,
            stretchTuning = StretchTuning()
        )

        CentTable(
            musicalScale,
            notePrintOptions = notePrintOptions
        )
    }
}