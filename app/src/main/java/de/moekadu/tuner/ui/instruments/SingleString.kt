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
package de.moekadu.tuner.ui.instruments

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.ui.common.Label
import de.moekadu.tuner.ui.notes.Note
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlin.math.roundToInt

/** Show a note label on a line as a representation of a instrument string.
 * @param note Note of string.
 * @param positionIndex Defines the horizontal position of the label. There are numPosition equally
 *   spaced positions and the index given here defines where the label is.
 * @param numPositions Number of equally spaced positions, where a note label can be placed.
 * @param modifier Modifier.
 * @param outerPadding Space at start and end of string, which will be excluded from the available
 *   space for positioning strings.
 * @param color String and label background color.
 * @param contentColor Label text color.
 * @param stringLineWidth Line width of the string. The idea is that strings with lower frequencies
 *   can be printed thicker than strings with higher frequencies.
 * @param notePrintOptions Defines how to print the notes.
 * @param labelSize Size of note label. If DpSize.Unspecified it will be set according to its content.
 * @param fontSize Font size for label text.
 * @param onClick Callback, when the note label is clicked.
 */
@Composable
fun SingleString(
    note: MusicalNote,
    positionIndex: Int,
    numPositions: Int,
    modifier: Modifier = Modifier,
    outerPadding: Dp = 0.dp,
    color: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    stringLineWidth: Dp = 2.dp,
    labelSize: DpSize = DpSize.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    onClick: () -> Unit = {}
) {
    val colorResolved = if (color == Color.Unspecified) MaterialTheme.colorScheme.inverseSurface else color
    val xAnimated: Float by animateFloatAsState(
        targetValue = (positionIndex + 0.5f) / numPositions,
        label = "label position"
    )

    Layout(
        content = {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stringLineWidth)
                    .background(colorResolved)
            )
//            Text(
//                note.base.toString(),
//                modifier = Modifier
//                    .background(colorResolved)
//                    .clickable {
//                    onClick()
//                }.then(
//                    if (labelSize == DpSize.Unspecified) Modifier else Modifier.size(labelSize)
//                )
//            )
            Label(
                content = {
                    Note(note,
                         notePrintOptions = notePrintOptions,
                         fontSize = fontSize,
                         color = contentColor
                    )
                },
                modifier = if (labelSize == DpSize.Unspecified) Modifier else Modifier.size(labelSize),
                color = colorResolved,
                onClick = onClick
            )
        },
         modifier = modifier // .background(Color.Gray.copy(alpha=0.5f))
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints)}

        val w = placeables.maxOf{ it.width }
        val h = placeables.maxOf{ it.height }

        layout(w, h) {
//            Log.v("Tuner", "SingleString: w=$w, h=$h,$labelSize ")
            // line
            with(placeables[0]) {
                place(0, (h - height) / 2)
            }
            // label
            with(placeables[1]) {
                val x = ((w - 2 * outerPadding.toPx()) * xAnimated) + outerPadding.toPx()
                place((x - width / 2).roundToInt(), 0)
            }

        }
    }
}


@Preview(showBackground = true, widthDp = 150)
@Composable
private fun SingleStringPreview() {
    TunerTheme {
        val note = MusicalNote(BaseNote.A, NoteModifier.Sharp, octave = 4)
        val notePrintOptions = NotePrintOptions()

        Column {

            repeat(4) {
                SingleString(
                    note = note,
                    positionIndex = it,
                    numPositions = 4,
                    notePrintOptions = notePrintOptions,
                    outerPadding = 4.dp,
                    labelSize = DpSize(30.dp, 20.dp)
                )
            }
        }
    }
}
