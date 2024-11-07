package de.moekadu.tuner.ui.notes

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier

@Composable
private fun rememberMaxNoteSizeForNoteEditor(
    notePrintOptions: NotePrintOptions,
    fontSize: TextUnit
) : DpSize {
    val baseNotes = remember {
        BaseNote.entries.filter { it != BaseNote.None }.map {
            MusicalNote(base = it, modifier = NoteModifier.None)
        }.toTypedArray()
    }
    val modifiedNotes = remember {
        NoteModifier.entries.map {
            MusicalNote(
                base = BaseNote.None,
                modifier = it
            )
        }.toTypedArray()
    }
    val noNote = remember {
        arrayOf(MusicalNote(BaseNote.None, NoteModifier.None))
    }
    val maxBaseSize = rememberMaxNoteSize(
        notes = baseNotes,
        notePrintOptions = notePrintOptions,
        fontSize = fontSize,
        octaveRange = null
    )
    val maxModifierSize = rememberMaxNoteSize(
        notes = modifiedNotes,
        notePrintOptions = notePrintOptions,
        fontSize = fontSize,
        octaveRange = null
    )
    val maxNoNoteSize = rememberMaxNoteSize(
        notes = noNote,
        notePrintOptions = notePrintOptions,
        fontSize = fontSize,
        octaveRange = null
    )
    return DpSize(
        maxBaseSize.width - maxNoNoteSize.width + maxModifierSize.width,
        maxModifierSize.height
        )
}



@Composable
fun NoteEditorSimple(
    base: BaseNote,
    noteModifier: NoteModifier,
    octaveOffset: Int,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    onNoteChange: (BaseNote, NoteModifier, octaveOffset: Int) -> Unit = {_, _, _ ->}
) {
//    Log.v("Tuner ", "NoteEditorSimple: base=$base, modifier = $noteModifier")
    val baseNotes = remember {
        BaseNote.entries.filter { it != BaseNote.None }.map {
            MusicalNote(base = it, modifier = NoteModifier.None)
        }.toTypedArray()
    }
    val baseNoteIndex by rememberUpdatedState(BaseNote.entries.indexOf(base))

    val modifiedNotes = remember {
        NoteModifier.entries.map {
            MusicalNote(
                base = BaseNote.None,
                modifier = it
            )
        }.toTypedArray()
    }

    val modifierIndex by rememberUpdatedState(NoteModifier.entries.indexOf(noteModifier))

    val octaveOffsetRemembered by rememberUpdatedState(newValue = octaveOffset)

    Column(modifier = modifier) {
        NoteSelector(
            selectedIndex = baseNoteIndex,
            notes = baseNotes,
            notePrintOptions = notePrintOptions,
            onIndexChanged = {
                onNoteChange(
                    BaseNote.entries[it],
                    NoteModifier.entries[modifierIndex],
                    octaveOffsetRemembered
                )
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        NoteSelector(
            selectedIndex = modifierIndex,
            notes = modifiedNotes,
            notePrintOptions = notePrintOptions,
            onIndexChanged = {
                onNoteChange(
                    BaseNote.entries[baseNoteIndex],
                    NoteModifier.entries[it],
                    octaveOffsetRemembered
                )
            }

        )
        Text(
            stringResource(id = R.string.octave_offset),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()

        ) {
            val options = remember { (-1..1).toList() }
            options.forEachIndexed { index, offset ->
                SegmentedButton(
                    selected = octaveOffset == offset,
                    onClick = {
                        onNoteChange(
                            BaseNote.entries[baseNoteIndex],
                            NoteModifier.entries[modifierIndex],
                            offset
                        )
                    },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = (index), count = options.size
                    )
                ) {
                    Text(if (offset == 0) "0" else String.format("%+d", offset))
                }
            }
        }
    }
}



@Preview(widthDp = 200, heightDp = 200, showBackground = true)
@Composable
private fun NoteEditorSiimplePreview() {
    var baseNote by remember { mutableStateOf(BaseNote.A) }
    var noteModifier by remember { mutableStateOf(NoteModifier.None) }
    var octaveOffset by remember { mutableIntStateOf(0) }
    Box(modifier = Modifier.fillMaxSize()) {
        NoteEditorSimple(
            base = baseNote,
            noteModifier = noteModifier,
            octaveOffset = octaveOffset,
            onNoteChange = { n, m, o ->
                baseNote = n
                noteModifier = m
                octaveOffset = o
            },
            notePrintOptions = NotePrintOptions(notationType = NotationType.Standard)
        )
    }
}