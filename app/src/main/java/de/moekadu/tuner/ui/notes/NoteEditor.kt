package de.moekadu.tuner.ui.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteModifier

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
fun NoteEditor(
    base: BaseNote,
    noteModifier: NoteModifier,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    onNoteChange: (BaseNote, NoteModifier) -> Unit = { _, _ ->}
) {
    val baseNotes = remember {
        BaseNote.entries.filter { it != BaseNote.None }.map {
            MusicalNote(base = it, modifier = NoteModifier.None)
        }.toTypedArray()
    }
    val baseNoteIndex = remember(base) {
            BaseNote.entries.indexOf(base)
    }
//    val modifiedNotes = remember(baseNoteIndex) {
//        NoteModifier.entries.map {
//            MusicalNote(
//                base = BaseNote.entries[baseNoteIndex],
//                modifier = it
//            )
//        }.toTypedArray()
//    }
    val modifiedNotes = remember {
        NoteModifier.entries.map {
            MusicalNote(
                base = BaseNote.None,
                modifier = it
            )
        }.toTypedArray()
    }

    val modifierIndex = remember (noteModifier) {
            NoteModifier.entries.indexOf(noteModifier)
    }
    val note = remember(base, noteModifier) {
        MusicalNote(base, noteModifier)
    }
    val noteFontStyle = MaterialTheme.typography.titleLarge
//    val maxNoteSize = rememberMaxNoteSize(
//        notes = modifiedNotes,
//        notePrintOptions = notePrintOptions,
//        fontSize = noteFontStyle.fontSize,
//        octaveRange = null
//    )
    val maxNoteSize = rememberMaxNoteSizeForNoteEditor(
        notePrintOptions = notePrintOptions,
        fontSize = noteFontStyle.fontSize
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            NoteSelector(
                selectedIndex = baseNoteIndex,
                notes = baseNotes,
                notePrintOptions = notePrintOptions,
                onIndexChanged = {
                    onNoteChange(BaseNote.entries[it], NoteModifier.entries[modifierIndex])
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            NoteSelector(
                selectedIndex = modifierIndex,
                notes = modifiedNotes,
                notePrintOptions = notePrintOptions,
                onIndexChanged = {
                    onNoteChange(BaseNote.entries[baseNoteIndex], NoteModifier.entries[it])
                }

            )
        }
        Surface(
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)
        ) {
            Box(
                modifier = Modifier.size(maxNoteSize.width + 16.dp, maxNoteSize.height + 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Note(
                    musicalNote = note,
                    notePrintOptions = notePrintOptions,
                    style = noteFontStyle
                )
            }
        }
    }
//    var baseMenuExpanded by remember { mutableStateOf(false) }
//    var modifierMenuExpanded by remember { mutableStateOf(false) }
//
//    ExposedDropdownMenuBox(
//        expanded = baseMenuExpanded,
//        onExpandedChange = { baseMenuExpanded = !baseMenuExpanded }
//    ) {
//        TextField(
//            value = base.toString(), // TODO: use notenameprinter info and more to resolve this value
//            onValueChange = {},
//            readOnly = true,
//            trailingIcon = {
//                ExposedDropdownMenuDefaults.TrailingIcon(expanded = baseMenuExpanded)
//            },
//            colors = ExposedDropdownMenuDefaults.textFieldColors(),
//            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
//        )
//        ExposedDropdownMenu(
//            expanded = baseMenuExpanded,
//            onDismissRequest = { baseMenuExpanded = false}
//        ) {
//            BaseNote.entries.forEach {
//                DropdownMenuItem(
//                    text = { Text(it.toString()) },// TODO: use notenameprinter info and more to resolve this value
//                    onClick = {
//                        onNoteChange(it, noteModifier)
//                        baseMenuExpanded = false
//                    }
//                )
//            }
//        }
//    }

}


@Preview(widthDp = 200, heightDp = 200, showBackground = true)
@Composable
private fun NoteEditorPreview() {
    var baseNote by remember { mutableStateOf(BaseNote.A) }
    var noteModifier by remember { mutableStateOf(NoteModifier.None) }
    Box(modifier = Modifier.fillMaxSize()) {
        NoteEditor(
            base = baseNote,
            noteModifier = noteModifier,
            onNoteChange = { n, m ->
                baseNote = n
                noteModifier = m
            },
            notePrintOptions = NotePrintOptions(notationType = NotationType.Standard)
        )
    }
}