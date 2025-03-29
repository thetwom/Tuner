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
package de.moekadu.tuner.ui.temperaments

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteNames
import de.moekadu.tuner.temperaments.Temperament
import de.moekadu.tuner.temperaments.TemperamentWithNoteNames
import de.moekadu.tuner.temperaments.generateNoteNames
import de.moekadu.tuner.temperaments.temperamentDatabase
import de.moekadu.tuner.ui.notes.CentAndRatioTable
import de.moekadu.tuner.ui.notes.CircleOfFifthTable
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.NoteSelector
import de.moekadu.tuner.ui.theme.TunerTheme

interface TemperamentDialogState {
    val temperament: State<Temperament>
    val noteNames: State<NoteNames>
    //val predefinedTemperaments: ImmutableList<TemperamentWithNoteNames>
    //val customTemperaments: StateFlow<ImmutableList<TemperamentWithNoteNames>>
    val selectedRootNoteIndex: IntState

    val defaultTemperament: TemperamentWithNoteNames

    fun setNewTemperament(temperamentWithNoteNames: TemperamentWithNoteNames)
    fun selectRootNote(index: Int)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemperamentDialog(
    state: TemperamentDialogState,
    notePrintOptions: NotePrintOptions,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onDone: (Temperament, NoteNames, rootNote: MusicalNote) -> Unit = { _, _, _ -> },
    onChooseTemperaments: () -> Unit = {}
) {
//    val customTemperaments by state.customTemperaments.collectAsStateWithLifecycle()
//    val temperamentsList by remember { derivedStateOf {
//        (customTemperaments + state.predefinedTemperaments).toImmutableList()
//    }}
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.temperament)) },
                navigationIcon = {
                    IconButton(onClick = { onDismiss() }) {
                        Icon(Icons.Default.Close, "close")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        onDone(
                            state.temperament.value,
                            state.noteNames.value,
                            state.noteNames.value[state.selectedRootNoteIndex.intValue]
                        )
                    }) {
                        Text(stringResource(id = R.string.done))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                onClick = { onChooseTemperaments() },
                colors = CardDefaults.cardColors().copy(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(stringResource(
                    id = R.string.temperament),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    state.temperament.value.name.value(context),
                    modifier = Modifier.padding(horizontal=16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    state.temperament.value.description.value(context),
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 16.dp
                    ),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(id = R.string.root_note),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            NoteSelector(
                selectedIndex = state.selectedRootNoteIndex.intValue,
                notes = state.noteNames.value.notes,
                notePrintOptions = notePrintOptions,
                onIndexChanged = { state.selectRootNote(it) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    state.selectRootNote(0)
                    state.setNewTemperament(state.defaultTemperament)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(stringResource(id = R.string.set_default))
            }
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp))
            Text(
                stringResource(id = R.string.details),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall
            )

            CentAndRatioTable(
                state.temperament.value,
                state.noteNames.value,
                state.selectedRootNoteIndex.intValue,
                notePrintOptions = notePrintOptions,
                modifier = Modifier.fillMaxWidth(),
                horizontalContentPadding = 16.dp
            )

            if (state.temperament.value.circleOfFifths != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(id = R.string.circle_of_fifths),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
                CircleOfFifthTable(
                    temperament = state.temperament.value,
                    noteNames = state.noteNames.value,
                    rootNoteIndex = state.selectedRootNoteIndex.intValue,
                    notePrintOptions = notePrintOptions,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalContentPadding = 16.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(id = R.string.pythagorean_comma_desc),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            Spacer(modifier = Modifier.height(82.dp))
        }
    }
}

private class TemperamentDialogTestState : TemperamentDialogState {
    override var temperament = mutableStateOf(temperamentDatabase[0])
    override var noteNames
            = mutableStateOf(generateNoteNames(temperamentDatabase[0].numberOfNotesPerOctave)!!)

    override val defaultTemperament = TemperamentWithNoteNames(
        temperamentDatabase[0], null
    )

    override val selectedRootNoteIndex = mutableIntStateOf(0)

    override fun setNewTemperament(temperamentWithNoteNames: TemperamentWithNoteNames) {
        val oldRootNoteIndex = selectedRootNoteIndex.intValue
        val oldRootNote = noteNames.value[oldRootNoteIndex]
        val newNoteNames = temperamentWithNoteNames.noteNames
            ?: generateNoteNames(temperamentWithNoteNames.temperament.numberOfNotesPerOctave)!!
        val rootNoteIndexInNewScale = newNoteNames.getNoteIndex(oldRootNote)
        selectedRootNoteIndex.intValue = if (rootNoteIndexInNewScale == -1) {
            0
        } else {
            rootNoteIndexInNewScale
        }

        temperament.value = temperamentWithNoteNames.temperament
        noteNames.value = newNoteNames
    }

    override fun selectRootNote(index: Int) {
        selectedRootNoteIndex.intValue = index
    }

}
@Preview(widthDp = 300, heightDp = 500, showBackground = true)
@Composable
private fun TemperamentDialogPreview() {
    TunerTheme {
        val notePrintOptions = remember { NotePrintOptions() }
        val state = remember { TemperamentDialogTestState() }
        TemperamentDialog(
            state,
            notePrintOptions = notePrintOptions
        )
    }
}



//@Composable
//fun TemperamentDialog(
//    initialState: PreferenceResources.MusicalScaleProperties,
//    onTemperamentChange: (newState: PreferenceResources.MusicalScaleProperties) -> Unit,
//    notePrintOptions: NotePrintOptions,
//    modifier: Modifier = Modifier,
//    onDismiss: () -> Unit = {}
//) {
//    val savedInitialState = rememberSaveable {
//        initialState
//    }
//    var selectedRootNoteIndex by rememberSaveable {
//        val initialMusicalScale = MusicalScaleFactory.create(savedInitialState.temperamentType)
//        val indexOct4 = initialMusicalScale.getNoteIndex(initialState.rootNote.copy(octave = 4))
//        val index0Oct4 = initialMusicalScale.getNoteIndex(initialMusicalScale.noteNameScale.notes[0].copy(octave = 4))
//        mutableIntStateOf(indexOct4 - index0Oct4)
//    }
//    var temperament by rememberSaveable {
//        mutableStateOf(savedInitialState.temperamentType)
//    }
//    val noteNameScale = remember(temperament) {
//        NoteNameScaleFactory.create(temperament)
//    }
//    val musicalScale = remember(temperament, selectedRootNoteIndex) {
//        val rootNote = noteNameScale.notes[selectedRootNoteIndex]
//        MusicalScaleFactory.create(temperament, noteNameScale, rootNote = rootNote)
//    }
//
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        confirmButton = {
//            TextButton(
//                onClick = {
//                    onTemperamentChange(savedInitialState.copy(
//                        temperamentType = temperament,
//                        rootNote = noteNameScale.notes[selectedRootNoteIndex].copy(octave = 4)
//                    ))
//                }
//            ) {
//                Text(stringResource(id = R.string.done))
//            }
//        },
//        modifier = modifier,
//        dismissButton = {
//            TextButton(
//                onClick = onDismiss
//            ) {
//                Text(stringResource(id = R.string.abort))
//            }
//        },
//        icon = {
//            Icon(
//                ImageVector.vectorResource(id = R.drawable.ic_temperament),
//                contentDescription = null
//            )
//        },
//        title = {
//            Text(stringResource(id = R.string.temperament))
//        },
//        text = {
//            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
//                TemperamentChooser(
//                    temperament = temperament,
//                    onTemperamentClicked = {
//                        val oldRootNote = noteNameScale.notes[selectedRootNoteIndex]
//                        val newNoteNameScale = NoteNameScaleFactory.create(it)
//                        val rootNoteIndexInNewScale = newNoteNameScale.getIndexOfNote(oldRootNote)
//
//                        selectedRootNoteIndex = if (rootNoteIndexInNewScale == Int.MAX_VALUE) {
//                            val relativeRootNoteIndex = selectedRootNoteIndex.toDouble() / noteNameScale.size
//                            (relativeRootNoteIndex * newNoteNameScale.size)
//                                .roundToInt()
//                                .coerceIn(0, newNoteNameScale.size - 1)
//                        } else {
//                            rootNoteIndexInNewScale - newNoteNameScale.getIndexOfNote(
//                                newNoteNameScale.notes[0].copy(octave = oldRootNote.octave)
//                            )
//                        }
//
//                        temperament = it
//                    }
//                )
//                Spacer(modifier = Modifier.height(12.dp))
//                Text(
//                    stringResource(id = R.string.root_note),
//                    modifier = Modifier.fillMaxWidth(),
//                    textAlign = TextAlign.Center,
//                    style = MaterialTheme.typography.labelSmall
//                )
//                Spacer(modifier = Modifier.height(4.dp))
//                NoteSelector(
//                    selectedIndex = selectedRootNoteIndex,
//                    notes = noteNameScale.notes,
//                    notePrintOptions = notePrintOptions,
//                    onIndexChanged = { selectedRootNoteIndex = it }
//                )
//                Spacer(modifier = Modifier.height(12.dp))
//                OutlinedButton(
//                    onClick = {
//                        temperament = TemperamentType.EDO12
//                        selectedRootNoteIndex = 0
//                    },
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Text(stringResource(id = R.string.set_default))
//                }
//                Spacer(modifier = Modifier.height(32.dp))
//
//                HorizontalDivider(modifier = Modifier.fillMaxWidth())
//                Text(
//                    stringResource(id = R.string.details),
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(top = 4.dp),
//                    textAlign = TextAlign.Center,
//                    style = MaterialTheme.typography.labelSmall
//                )
//                Spacer(modifier = Modifier.height(8.dp))
//                CentAndRatioTable(
//                    musicalScale = musicalScale,
//                    notePrintOptions = notePrintOptions,
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                if (musicalScale.circleOfFifths != null) {
//                    Spacer(modifier = Modifier.height(32.dp))
//                    Text(
//                        stringResource(id = R.string.circle_of_fifths),
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(bottom = 4.dp),
//                        textAlign = TextAlign.Center,
//                        style = MaterialTheme.typography.labelSmall
//                    )
//                    CircleOfFifthTable(
//                        musicalScale = musicalScale,
//                        notePrintOptions = notePrintOptions,
//                        modifier = Modifier.fillMaxWidth()
//                    )
//                    Spacer(modifier = Modifier.height(12.dp))
//                    Text(
//                        stringResource(id = R.string.pythagorean_comma_desc),
//                        style = MaterialTheme.typography.bodySmall
//                    )
//                }
//            }
//        }
//    )
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun TemperamentChooser(
//    temperament: TemperamentType,
//    onTemperamentClicked: (temperamentType: TemperamentType) -> Unit,
//    modifier: Modifier = Modifier
//) {
//    var expanded by rememberSaveable { mutableStateOf(false) }
//    ExposedDropdownMenuBox(
//        expanded = expanded,
//        onExpandedChange = { expanded = it },
//        modifier = modifier.fillMaxWidth()
//    ) {
//        TextField(
//            value = stringResource(id = getTuningNameResourceId(temperament)),
//            onValueChange = {},
//            readOnly = true,
//            label = { Text(stringResource(id = R.string.temperament)) },
//            trailingIcon = {
//                ExposedDropdownMenuDefaults.TrailingIcon(
//                    expanded = expanded
//                )
//            },
//            modifier = Modifier
//                .fillMaxWidth()
//                .menuAnchor(),
//            colors = ExposedDropdownMenuDefaults.textFieldColors()
//        )
//        ExposedDropdownMenu(
//            expanded = expanded,
//            onDismissRequest = { expanded = false }
//        ) {
//            TemperamentType.entries.forEach { temperamentItem ->
//                DropdownMenuItem(
//                    text = {
//                        Column {
//                            Text(
//                                stringResource(id = getTuningNameResourceId(temperamentItem)),
//                                style = MaterialTheme.typography.labelLarge
//                            )
//                            getTuningDescriptionResourceId(temperamentItem)?.let { desc ->
//                                Text(
//                                    stringResource(id = desc),
//                                    style = MaterialTheme.typography.labelSmall
//                                )
//                            }
//                        }
//                    },
//                    onClick = {
//                        onTemperamentClicked(temperamentItem)
//                        expanded = false
//                    }
//                )
//            }
//        }
//    }
//}
//
//@Preview(widthDp = 300, heightDp = 500)
//@Composable
//private fun TemperamentDialogPreview() {
//    TunerTheme {
//        val state = remember {
//            val scale = MusicalScaleFactory.create(TemperamentType.EDO12)
//            PreferenceResources.MusicalScaleProperties.create(scale)
//        }
//        val notePrintOptions = remember { NotePrintOptions() }
//        TemperamentDialog(
//            state,
//            notePrintOptions = notePrintOptions,
//            onTemperamentChange = { }
//        )
//    }
//}
//
