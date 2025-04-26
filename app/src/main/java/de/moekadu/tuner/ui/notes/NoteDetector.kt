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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.unit.times
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.musicalscale.MusicalScale2
import de.moekadu.tuner.musicalscale.MusicalScaleFactory
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf

class NoteDetectorState {
    data class NoteWithCounter(val note: MusicalNote?, val counter: Long)

    var counter = 0L
        private set

    var notes by mutableStateOf(persistentListOf(NoteWithCounter(null, -1)))
        private set

    fun hitNote(note: MusicalNote) {
        val notesCopy = notes
        val index = notesCopy.indexOfFirst { note == it.note }
        notes = if (index >= 0) {
            ++counter
            notesCopy.mutate { it[index] = it[index].copy(counter = counter) }

        } else {
            var indexMin = -1
            var counterMin = Long.MAX_VALUE
            notesCopy.forEachIndexed { i, n ->
                if (n.counter < counterMin) {
                    counterMin = n.counter
                    indexMin = i
                }
            }
            ++counter
            notesCopy.mutate { it[indexMin] = NoteWithCounter(note, counter) }
        }
    }

    fun setNumNotes(numNotes: Int) {
        val notesCopy = notes
        notes = if (notesCopy.size < numNotes) {
            notesCopy.mutate {
                for (i in notesCopy.size until numNotes)
                    it.add(NoteWithCounter(null, -1))
            }
        } else if (notesCopy.size > numNotes){
            notesCopy.mutate {
                // this could of course only delete the oldest note, but this makes the code much
                // more difficult, while this scenario would normally not occur
                it.subList(numNotes, notesCopy.size).clear()
            }
        } else {
            notesCopy
        }
    }

}

@Composable
fun NoteDetector(
    state: NoteDetectorState,
    musicalScale: MusicalScale2,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    textStyle: TextStyle? = MaterialTheme.typography.labelLarge,
    textColor: Color = Color.Unspecified,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    onNoteClicked: (note: MusicalNote) -> Unit = {}
) {
    val fontSizeResolved = fontSize.takeOrElse {
        (textStyle?.fontSize ?: TextUnit.Unspecified).takeOrElse {
            LocalTextStyle.current.fontSize.takeOrElse { 12.sp }
        }
    }
    val fontWeightResolved = textStyle?.fontWeight
    val octaveRange = remember(musicalScale) {
        musicalScale.getNote(musicalScale.noteIndexBegin).octave..musicalScale.getNote(musicalScale.noteIndexEnd - 1).octave
    }

    val minSingleNoteSize = rememberMaxNoteSize(
        notes = musicalScale.noteNames.notes,
        notePrintOptions = notePrintOptions,
        fontSize = fontSizeResolved,
        fontWeight = fontWeightResolved,
        octaveRange = octaveRange
    ) + DpSize(16.dp, 4.dp)

    val singleNoteSize = DpSize(
        if (minSingleNoteSize.width >= 48.dp) minSingleNoteSize.width else 48.dp,
        if (minSingleNoteSize.height >= 40.dp) minSingleNoteSize.height else 40.dp
    )

    BoxWithConstraints(modifier = modifier) {
        val numNotes = (maxWidth / singleNoteSize.width).toInt()
        val singleNoteWidth = maxWidth / numNotes

        LaunchedEffect(key1 = numNotes) {
            state.setNumNotes(numNotes)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val notes = state.notes
            val counter = state.counter
            val sizeFactorOlderNote = 0.5f
            notes.forEach { note ->
                Box(
                    modifier = Modifier
                        .width(singleNoteWidth)
                        .height(singleNoteSize.height)
                        .then(
                            if (note.note != null) {
                                Modifier.clickable { onNoteClicked(note.note) }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    note.note?.let {
                        val animatedFontSize by animateFloatAsState(
                            if (note.counter == counter) 1f else sizeFactorOlderNote,
                            label = "note scaling",
                            animationSpec = tween(500)
                        )
                        Note(
                            musicalNote = it,
                            fontSize = animatedFontSize * fontSizeResolved,
                            fontWeight = fontWeightResolved,
                            color = textColor,
                            modifier = Modifier.align(
                                Alignment.Center
                            )
                        )
                    }
                }
            }
        }
    }
}



@Preview(widthDp = 300, heightDp = 100, showBackground = true)
@Composable
private fun NoteDetectorPreview() {
    TunerTheme {
        val state = remember { NoteDetectorState() }
        val musicalScale = remember { MusicalScaleFactory.createTestEdo12() }

        val noteList = remember {
            (20..30).map{ musicalScale.getNote(it) }
        }
        Column {
            NoteDetector(
                state = state,
                musicalScale = musicalScale,
                fontSize = 30.sp
            )
            Button(
                onClick = { state.hitNote(noteList.random()) }
            ) {
                Text("Hit Note")
            }
        }
    }
}