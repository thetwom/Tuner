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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.temperaments.FifthModification
import de.moekadu.tuner.temperaments.Temperament3
import de.moekadu.tuner.temperaments.predefinedTemperamentEDO
import de.moekadu.tuner.ui.theme.TunerTheme

// C  C#  D   D# ...
// 0 100 200 300 ...

// ratios    |
//           v
// r1 r2 r3 r4 r5 ...

data class Fifth(
    val startNote: MusicalNote,
    val modification: FifthModification?
)
//
@Composable
private fun rememberChain(temperament: Temperament3, rootNote: MusicalNote?): Array<Fifth>{
    return remember(temperament, rootNote) {
        val chain = temperament.chainOfFifths()
        if (chain == null) {
            arrayOf()
        } else {
            val unsorted = chain.getRatiosAlongFifths()
            val sorted = chain.getSortedRatios()
            val notes = temperament.noteNames(rootNote)
            unsorted.mapIndexed { index, us ->
                val i = sorted.indexOfFirst { us == it }
                Fifth(notes[i], chain.fifths.getOrNull(index))
            }.toTypedArray()
        }
    }
}


/** Visualize the circle of fifths distances within a musical scale.
 * @param temperament Temperament for which the circle of fifths should be shown.
 * @param rootNote Root note.
 * @param notePrintOptions How to print the notes.
 * @param modifier Modifier.
 */
@Composable
fun CircleOfFifthTable(
    temperament: Temperament3,
    rootNote: MusicalNote?,
    notePrintOptions: NotePrintOptions,
    modifier: Modifier = Modifier,
    horizontalContentPadding: Dp = 16.dp
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

    val fifthArray = rememberChain(temperament, rootNote)

//    val fifthArray = remember(temperament) {
//        val cof = temperament.circleOfFifths
//        if (cof != null) {
//            arrayOf(cof.CG, cof.GD, cof.DA, cof.AE, cof.EB, cof.BFsharp, cof.FsharpCsharp,
//                cof.CsharpGsharp, cof.GsharpEflat, cof.EFlatBflat, cof.BflatF, cof.FC)
//        } else {
//            arrayOf()
//        }
//    }

    LazyRow(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = horizontalContentPadding)
    ) {
        if (fifthArray.isNotEmpty()) {
            fifthArray.forEach { fifth ->
                item {
                    Note(
                        fifth.startNote,
                        notePrintOptions = notePrintOptions,
                        withOctave = false,
                        fontWeight = FontWeight.Bold,
                        style = noteTypography
                    )
                }
                fifth.modification?.let { fifthModification ->
                    item {
                        FifthJumpOverArrow(
                            fifthModification = fifthModification,
                            style = MaterialTheme.typography.labelSmall,
                            arrowHeight = noteTypography.fontSize / 5 * 3, // the factor is trial and error, meaning, that for other fonts it could look bad
                            modifier = Modifier.padding(bottom = (bottomToBaselineDistance))
                        )
                    }
                }
            }
        } else {
            item {
                val rootNoteResolved = remember(rootNote, temperament){
                    rootNote ?: temperament.noteNames(null)[0]
                }
                Note(
                    rootNoteResolved,
                    notePrintOptions = notePrintOptions,
                    withOctave = false,
                    fontWeight = FontWeight.Bold,
                    style = noteTypography
                )
            }
            item {
                FifthJumpOverArrow(
                    fifthModification = null,
                    style = MaterialTheme.typography.labelSmall,
                    arrowHeight = noteTypography.fontSize / 5 * 3, // the factor is trial and error, meaning, that for other fonts it could look bad
                    modifier = Modifier.padding(bottom = (bottomToBaselineDistance))
                )
            }
            item {
                Text("...", fontWeight = FontWeight.Bold, style = noteTypography)
            }
        }

//        if (fifthArray.isNotEmpty()) {
//            items(2 * fifthArray.size) {
//                if (it % 2 == 0) {
//                    val noteIndex = it / 2
//                    Note(
//                        noteNames[(rootNoteIndex + 7 * noteIndex) % noteNames.size],
//                        notePrintOptions = notePrintOptions,
//                        withOctave = false,
//                        fontWeight = FontWeight.Bold,
//                        style = noteTypography
//                    )
//                } else {
//                    val cofIndex = it / 2
//                    FifthJumpOverArrow(
//                        fifthModification = fifthArray[cofIndex],
//                        style = MaterialTheme.typography.labelSmall,
//                        arrowHeight = noteTypography.fontSize / 5 * 3, // the factor is trial and error, meaning, that for other fonts it could look bad
//                        modifier = Modifier.padding(bottom = (bottomToBaselineDistance))
//                    )
//                }
//            }
//        } else {
//            item {
//                Note(
//                    noteNames[rootNoteIndex % noteNames.size],
//                    notePrintOptions = notePrintOptions,
//                    withOctave = false,
//                    fontWeight = FontWeight.Bold,
//                    style = noteTypography
//                )
//            }
//            item {
//                FifthJumpOverArrow(
//                    fifthModification = null,
//                    style = MaterialTheme.typography.labelSmall,
//                    arrowHeight = noteTypography.fontSize / 5 * 3, // the factor is trial and error, meaning, that for other fonts it could look bad
//                    modifier = Modifier.padding(bottom = (bottomToBaselineDistance))
//                )
//            }
//            item {
//                Text("...", fontWeight = FontWeight.Bold, style = noteTypography)
//            }
//        }
    }
}

//@Preview(showBackground = true)
//@Composable
//private fun CircleOfFifthTablePreview() {
//    TunerTheme {
//        val temperamentType = TemperamentType.ThirdCommaMeanTone
//        val noteNameScale = NoteNameScaleFactory.create(temperamentType)
//        val rootNote = noteNameScale.notes[4].copy(octave = 4)
//        val musicalScale =
//            MusicalScaleFactory.create(temperamentType, noteNameScale, rootNote = rootNote)
//        val notePrintOptions = NotePrintOptions(
//            sharpFlatPreference = NotePrintOptions.SharpFlatPreference.Sharp,
//            helmholtzNotation = false,
//            notationType = NotationType.Standard
//        )
//        CircleOfFifthTable(
//            musicalScale,
//            notePrintOptions = notePrintOptions
//        )
//    }
//}

@Preview(showBackground = true)
@Composable
private fun CircleOfFifthTable2Preview() {
    TunerTheme {
        val notePrintOptions = remember {
            NotePrintOptions(
                helmholtzNotation = false,
                notationType = NotationType.Standard
            )
        }
        val temperament = remember { predefinedTemperamentEDO(12, 0L) }
        val rootNote = remember { temperament.possibleRootNotes()[0] }
        CircleOfFifthTable(
            temperament,
            rootNote,
            notePrintOptions = notePrintOptions
        )
    }
}