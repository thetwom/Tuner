package de.moekadu.tuner.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.preferences.PreferenceResources2
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.NoteNameScaleFactory
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.temperaments.getTuningDescriptionResourceId
import de.moekadu.tuner.temperaments.getTuningNameResourceId
import de.moekadu.tuner.ui.notes.CentAndRatioTable
import de.moekadu.tuner.ui.notes.CircleOfFifthTable
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.NoteSelector
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlin.math.roundToInt

@Composable
fun TemperamentDialog(
    initialState: PreferenceResources2.MusicalScaleProperties,
    onTemperamentChange: (newState: PreferenceResources2.MusicalScaleProperties) -> Unit,
    notePrintOptions: NotePrintOptions,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val savedInitialState = rememberSaveable {
        initialState
    }
    var selectedRootNoteIndex by rememberSaveable {
        val initialMusicalScale = MusicalScaleFactory.create(savedInitialState.temperamentType)
        val indexOct4 = initialMusicalScale.getNoteIndex(initialState.rootNote.copy(octave = 4))
        val index0Oct4 = initialMusicalScale.getNoteIndex(initialMusicalScale.noteNameScale.notes[0].copy(octave = 4))
        mutableIntStateOf(indexOct4 - index0Oct4)
    }
    var temperament by rememberSaveable {
        mutableStateOf(savedInitialState.temperamentType)
    }
    val noteNameScale = remember(temperament) {
        NoteNameScaleFactory.create(temperament)
    }
    val musicalScale = remember(temperament, selectedRootNoteIndex) {
        val rootNote = noteNameScale.notes[selectedRootNoteIndex]
        MusicalScaleFactory.create(temperament, noteNameScale, rootNote = rootNote)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onTemperamentChange(savedInitialState.copy(
                        temperamentType = temperament,
                        rootNote = noteNameScale.notes[selectedRootNoteIndex].copy(octave = 4)
                    ))
                }
            ) {
                Text(stringResource(id = R.string.done))
            }
        },
        modifier = modifier,
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(id = R.string.abort))
            }
        },
        icon = {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_temperament),
                contentDescription = null
            )
        },
        title = {
            Text(stringResource(id = R.string.temperament))
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TemperamentChooser(
                    temperament = temperament,
                    onTemperamentClicked = {
                        val oldRootNote = noteNameScale.notes[selectedRootNoteIndex]
                        val newNoteNameScale = NoteNameScaleFactory.create(it)
                        val rootNoteIndexInNewScale = newNoteNameScale.getIndexOfNote(oldRootNote)

                        selectedRootNoteIndex = if (rootNoteIndexInNewScale == Int.MAX_VALUE) {
                            val relativeRootNoteIndex = selectedRootNoteIndex.toDouble() / noteNameScale.size
                            (relativeRootNoteIndex * newNoteNameScale.size)
                                .roundToInt()
                                .coerceIn(0, newNoteNameScale.size - 1)
                        } else {
                            rootNoteIndexInNewScale - newNoteNameScale.getIndexOfNote(
                                newNoteNameScale.notes[0].copy(octave = oldRootNote.octave)
                            )
                        }

                        temperament = it
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(id = R.string.root_note),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                NoteSelector(
                    selectedIndex = selectedRootNoteIndex,
                    noteNameScale = noteNameScale,
                    notePrintOptions = notePrintOptions,
                    onIndexChanged = { selectedRootNoteIndex = it }
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        temperament = TemperamentType.EDO12
                        selectedRootNoteIndex = 0
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.set_default))
                }
                Spacer(modifier = Modifier.height(32.dp))

                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                Text(
                    stringResource(id = R.string.details),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                CentAndRatioTable(
                    musicalScale = musicalScale,
                    notePrintOptions = notePrintOptions,
                    modifier = Modifier.fillMaxWidth()
                )

                if (musicalScale.circleOfFifths != null) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        stringResource(id = R.string.circle_of_fifths),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall
                    )
                    CircleOfFifthTable(
                        musicalScale = musicalScale,
                        notePrintOptions = notePrintOptions,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(id = R.string.pythagorean_comma_desc),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemperamentChooser(
    temperament: TemperamentType,
    onTemperamentClicked: (temperamentType: TemperamentType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        TextField(
            value = stringResource(id = getTuningNameResourceId(temperament)),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(id = R.string.temperament)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TemperamentType.entries.forEach { temperamentItem ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                stringResource(id = getTuningNameResourceId(temperamentItem)),
                                style = MaterialTheme.typography.labelLarge
                            )
                            getTuningDescriptionResourceId(temperamentItem)?.let { desc ->
                                Text(
                                    stringResource(id = desc),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    },
                    onClick = {
                        onTemperamentClicked(temperamentItem)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun TemperamentDialogPreview() {
    TunerTheme {
        val state = remember {
            val scale = MusicalScaleFactory.create(TemperamentType.EDO12)
            PreferenceResources2.MusicalScaleProperties.create(scale)
        }
        val notePrintOptions = remember { NotePrintOptions() }
        TemperamentDialog(
            state,
            notePrintOptions = notePrintOptions,
            onTemperamentChange = { }
        )
    }
}
