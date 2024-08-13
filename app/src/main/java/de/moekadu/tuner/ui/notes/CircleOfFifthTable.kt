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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.NoteNameScaleFactory
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.ui.theme.TunerTheme

/** Visualize the circle of fifths distances within a musical scale.
 * @param musicalScale Musical scale which defines the notes to be shown.
 * @param notePrintOptions How to print the notes.
 * @param modifier Modifier.
 */
@Composable
fun CircleOfFifthTable(
    musicalScale: MusicalScale,
    notePrintOptions: NotePrintOptions,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val noteTypography = MaterialTheme.typography.labelLarge
    val density = LocalDensity.current
    val bottomToBaselineDistance = remember(noteTypography, density) {
        val measureResult = textMeasurer.measure("M", noteTypography)
        val baseline = measureResult.firstBaseline
        val bottom = measureResult.size.height
        val distance = with(density) {(bottom - baseline).toDp()}
        distance
    }

    val fifthArray = remember(musicalScale) {
        val cof = musicalScale.circleOfFifths
        if (cof != null) {
            arrayOf(cof.CG, cof.GD, cof.DA, cof.AE, cof.EB, cof.BFsharp, cof.FsharpCsharp,
                cof.CsharpGsharp, cof.GsharpEflat, cof.EFlatBflat, cof.BflatF, cof.FC)
        } else {
            arrayOf()
        }
    }

    val rootNoteIndex = remember(musicalScale) {
        musicalScale.getNoteIndex(musicalScale.rootNote)
    }
    LazyRow(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        items(2 * musicalScale.numberOfNotesPerOctave + 1) {
            if (it % 2 == 0) {
                val noteIndex = it / 2
                Note(
                    musicalScale.noteNameScale.getNoteOfIndex(rootNoteIndex + 7 * noteIndex),
                    notePrintOptions = notePrintOptions,
                    withOctave = false,
                    fontWeight = FontWeight.Bold,
                    style = noteTypography
                )
            } else {
                val cofIndex = it / 2
                FifthJumpOverArrow(
                    fifthModification = fifthArray[cofIndex],
                    style = MaterialTheme.typography.labelSmall,
                    arrowHeight = noteTypography.fontSize / 5 * 3, // the factor is trial and error, meaning, that for other fonts it could look bad
                    modifier = Modifier.padding(bottom = (bottomToBaselineDistance))
                    )
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CircleOfFifthTablePreview() {
    TunerTheme {
        val temperamentType = TemperamentType.ThirdCommaMeanTone
        val noteNameScale = NoteNameScaleFactory.create(temperamentType)
        val rootNote = noteNameScale.notes[4].copy(octave = 4)
        val musicalScale =
            MusicalScaleFactory.create(temperamentType, noteNameScale, rootNote = rootNote)
        val notePrintOptions = NotePrintOptions(
            sharpFlatPreference = NotePrintOptions.SharpFlatPreference.Sharp,
            helmholtzNotation = false,
            notationType = NotationType.Standard
        )
        CircleOfFifthTable(
            musicalScale,
            notePrintOptions = notePrintOptions
        )
    }
}