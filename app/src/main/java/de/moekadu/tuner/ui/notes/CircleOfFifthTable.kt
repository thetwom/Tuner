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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.notenames.BaseNote
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
    val modification: FifthModification?,
    val isRoot: Boolean,
    val drawNoteLight: Boolean,
    val drawModificationLight: Boolean
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
            val result = unsorted.mapIndexed { index, us ->
                val i = sorted.indexOfFirst { us == it }
                Fifth(
                    startNote = notes[i],
                    modification = chain.fifths.getOrNull(index),
                    isRoot = i == 0,
                    drawNoteLight = false,
                    drawModificationLight = false
                )
            }.toTypedArray()

            if (temperament.size == 12 && result[0].isRoot) {
                result[result.size - 1] = result[result.size - 1].copy(
                    modification = chain.getClosingCircleCorrection(),
                    drawModificationLight = true
                )
                result + arrayOf(
                    Fifth(
                        result[0].startNote,
                        null,
                        false,
                        drawNoteLight = true,
                        drawModificationLight = false
                    )
                )
            } else if (temperament.size == 12) {
                result[result.size-1] = result[result.size-1].copy(
                    modification = chain.getClosingCircleCorrection(),
                    drawModificationLight = true
                )
                arrayOf(
                    Fifth(result.last().startNote,
                        chain.getClosingCircleCorrection(),
                        false,
                        drawNoteLight = true,
                        drawModificationLight = true
                    )
                ) + result + arrayOf(
                    Fifth(result[0].startNote,
                        null,
                        false,
                        drawNoteLight = true,
                        drawModificationLight = false
                    )
                )
            } else {
                result
            }
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
    val notePrintOptionsDefault = remember(notePrintOptions) {
        notePrintOptions.copy(useEnharmonic = false)
    }
    val notePrintOptionsEnharmonic = remember(notePrintOptions) {
        notePrintOptions.copy(useEnharmonic = true)
    }

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
                    Row(
                        //modifier = Modifier.height(40.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.graphicsLayer {
                            alpha = if(fifth.drawNoteLight) 0.5f else 1f
                        }

                        //contentAlignment = Alignment.Center
                    ) {
                        //Spacer(modifier = Modifier.width(16.dp))
                        if (fifth.startNote.base != BaseNote.None) {
                            Note(
                                fifth.startNote, //note,
                                notePrintOptions = notePrintOptionsDefault,
                                withOctave = false,
                                fontWeight = if (fifth.isRoot) FontWeight.ExtraBold else FontWeight.Normal,
                                style = noteTypography
                            )
                        }
                        if (fifth.startNote.base != BaseNote.None && fifth.startNote.enharmonicBase != BaseNote.None) {
                            Text(
                                "/",
                                Modifier.padding(horizontal = 2.dp),
                                fontWeight = if (fifth.isRoot) FontWeight.ExtraBold else FontWeight.Normal,
                                style = noteTypography
                            )
                        }
                        if (fifth.startNote.enharmonicBase != BaseNote.None) {
                            Note(
                                fifth.startNote,
                                notePrintOptions = notePrintOptionsEnharmonic,
                                withOctave = false,
                                fontWeight = if (fifth.isRoot) FontWeight.ExtraBold else FontWeight.Normal,
                                style = noteTypography
                            )
                        }
//                        Spacer(modifier = Modifier.width(16.dp))
//                    }
//                    Note(
//                        fifth.startNote,
//                        notePrintOptions = notePrintOptions,
//                        withOctave = false,
//                        fontWeight = FontWeight.Bold,
//                        style = noteTypography
//                    )
                    }
                }
                fifth.modification?.let { fifthModification ->
                    item {
                        FifthJumpOverArrow(
                            fifthModification = fifthModification,
                            style = MaterialTheme.typography.labelSmall,
                            arrowHeight = noteTypography.fontSize / 5 * 3, // the factor is trial and error, meaning, that for other fonts it could look bad
                            modifier = Modifier
                                .padding(bottom = (bottomToBaselineDistance))
                                .graphicsLayer {
                                    alpha = if(fifth.drawModificationLight) 0.5f else 1f
                                }
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
    }
}


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
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            CircleOfFifthTable(
                temperament,
                rootNote,
                notePrintOptions = notePrintOptions
            )
        }
    }
}