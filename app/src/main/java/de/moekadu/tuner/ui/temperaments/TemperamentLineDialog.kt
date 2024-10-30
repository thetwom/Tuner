package de.moekadu.tuner.ui.temperaments

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.ui.notes.NotationType
import de.moekadu.tuner.ui.notes.NoteEditor
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.theme.TunerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemperamentLineDialog(
    initialNote: MusicalNote,
    //initialCent: Int,
    initialIsReferenceNote: Boolean,
    initialReferenceNoteOctave: Int,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    onDismiss: () -> Unit = {},
    onDoneClicked: (note: MusicalNote, isReferenceNote: Boolean) -> Unit = {_,_ -> }
) {
    var baseNote by rememberSaveable {
        mutableStateOf(initialNote.base)
    }
    var noteModifier by rememberSaveable {
        mutableStateOf(initialNote.modifier)
    }
    var octaveOffset by rememberSaveable { mutableIntStateOf(initialNote.octaveOffset) }

    var baseNote2 by rememberSaveable { mutableStateOf(initialNote.enharmonicBase) }
    var noteModifier2 by rememberSaveable { mutableStateOf(initialNote.enharmonicModifier) }

    var octaveOffset2 by rememberSaveable { mutableIntStateOf(initialNote.enharmonicOctaveOffset) }

//    var cent by rememberSaveable { mutableStateOf("$initialCent") }
//    val isCentError by remember { derivedStateOf { cent.toDoubleOrNull() == null } }

    var isReferenceNote by rememberSaveable { mutableStateOf(initialIsReferenceNote) }
    var referenceNoteOctave by rememberSaveable { mutableStateOf(initialReferenceNoteOctave.toString()) }
    val isReferenceOctaveError by remember { derivedStateOf { referenceNoteOctave.toIntOrNull() == null }}

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    // val hasErrors by remember { derivedStateOf { isCentError || (isReferenceNote && isReferenceOctaveError) }}
    val hasErrors by remember { derivedStateOf { isReferenceNote && isReferenceOctaveError }}

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = {
                    val note = MusicalNote(
                        baseNote,
                        noteModifier,
                        referenceNoteOctave.toIntOrNull() ?: 4,
                        octaveOffset,
                        baseNote2,
                        noteModifier2,
                        octaveOffset2
                    )
                    onDoneClicked(note, isReferenceNote)
                },
                enabled = !hasErrors
            ) {
                Text(stringResource(id = R.string.done))
            }
        },
        modifier = modifier,
        dismissButton = {
            TextButton(
                onClick = { onDismiss() }
            ) {
                Text(stringResource(id = R.string.abort))
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
//                TextField(
//                    value = cent,
//                    onValueChange = { cent = it },
//                    trailingIcon = { Text(stringResource(id = R.string.cent_str)) },
//                    isError = isCentError,
//                    textStyle = MaterialTheme.typography.bodyLarge
//                )
//                Spacer(
//                    modifier = Modifier.height(8.dp)
//                )

                Surface(shape = MaterialTheme.shapes.large) {
                    Column(modifier = Modifier.padding(8.dp)) {

                        PrimaryTabRow(
                            selectedTabIndex = selectedTabIndex,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = { selectedTabIndex = 0 },
                                text = { Text(stringResource(id = R.string.note_name)) }
                            )
                            Tab(
                                selected = selectedTabIndex == 1,
                                onClick = {
                                    selectedTabIndex = 1
                                },
                                text = { Text(stringResource(id = R.string.enharmonic_note_name)) }
                            )
                        }


                        //                Text(
                        //                    stringResource(R.string.note_name),
                        //                    modifier = Modifier.fillMaxWidth(),
                        //                    style = MaterialTheme.typography.labelSmall
                        //                )
                        Spacer(modifier = Modifier.height(8.dp))
                        NoteEditor(
                            base = if (selectedTabIndex == 0 || baseNote2 == BaseNote.None) baseNote else baseNote2,
                            noteModifier = if (selectedTabIndex == 0 || baseNote2 == BaseNote.None) noteModifier else noteModifier2,
                            onNoteChange = { n, m ->
                                if (selectedTabIndex == 0) {
                                    baseNote = n
                                    noteModifier = m
                                } else {
                                    baseNote2 = n
                                    noteModifier2 = m
                                }
                            },
                            notePrintOptions = notePrintOptions
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(id = R.string.octave_offset),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val options = remember { (-1..1).toList() }
                            options.forEachIndexed { index, offset ->
                                SegmentedButton(
                                    selected = if (selectedTabIndex == 0 || baseNote2 == BaseNote.None)
                                        octaveOffset == offset
                                    else
                                        octaveOffset2 == offset,
                                    onClick = {
                                        if (selectedTabIndex == 0) {
                                            octaveOffset = offset
                                        } else {
                                            // if enharmonic option was not set before, set it as initial value
                                            // to the base note
                                            if (baseNote2 == BaseNote.None) {
                                                baseNote2 = baseNote
                                                noteModifier2 = noteModifier
                                            }
                                            octaveOffset2 = offset
                                        }
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
                Surface(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .toggleable(
                                    value = isReferenceNote,
                                    onValueChange = { isReferenceNote = !isReferenceNote },
                                    role = Role.Checkbox
                                )
                                .padding(horizontal = 16.dp)
                                .height(56.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isReferenceNote,
                                onCheckedChange = null
                            )
                            Text(
                                stringResource(R.string.default_reference_note),
                                modifier = Modifier.padding(start = 16.dp).weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isReferenceNote) {
                            TextField(
                                value = referenceNoteOctave,
                                onValueChange = { referenceNoteOctave = it },
                                label = { Text(stringResource(id = R.string.octave)) },
                                isError = isReferenceOctaveError
                            )
                        }
                    }
                }
            }
        }
    )
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun TemperamentLineDialogPreview() {
    TunerTheme {
        TemperamentLineDialog(
            MusicalNote(BaseNote.A, NoteModifier.None),
//            334,
            initialIsReferenceNote = false,
            initialReferenceNoteOctave = 4,
            notePrintOptions = NotePrintOptions(notationType = NotationType.Solfege)
        )
    }
}