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
package de.moekadu.tuner.ui.preferences

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.musicalscale.MusicalScale2
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.temperaments.Temperament3
import de.moekadu.tuner.ui.misc.rememberNumberFormatter
import de.moekadu.tuner.ui.notes.CentAndRatioTable
import de.moekadu.tuner.ui.notes.CircleOfFifthTable
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.NoteSelector
import de.moekadu.tuner.ui.theme.TunerTheme
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParsePosition

@Composable
fun RootNoteDialog(
    initialRootNote: MusicalNote,
    temperament: Temperament3,
    onDone: (rootNote: MusicalNote) -> Unit,
    notePrintOptions: NotePrintOptions,
    modifier: Modifier = Modifier,
    warning: String? = null,
    onDismiss: () -> Unit = {}
) {
    val rootNotes = remember(temperament) {
         temperament.possibleRootNotes()
    }

    var selectedRootNoteIndex by rememberSaveable { mutableIntStateOf(
        rootNotes
            .indexOfFirst { it.equalsIgnoreOctave(initialRootNote) }
            .coerceAtLeast(0)
    ) }

    val hasChainOfFifths = remember(temperament) {
        temperament.chainOfFifths() != null
    }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDone(
                        rootNotes[selectedRootNoteIndex]
                    )
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
            // Text(stringResource(id = R.string.root_note))
            Text(temperament.name.value(context))
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                warning?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }
                Text(
                    stringResource(id = R.string.root_note),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                NoteSelector(
                    selectedIndex = selectedRootNoteIndex,
                    notes = rootNotes,
                    notePrintOptions = notePrintOptions,
                    onIndexChanged = { selectedRootNoteIndex = it }
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        selectedRootNoteIndex = 0
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(stringResource(id = R.string.use_default))
                }
                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp))
                Text(
                    stringResource(id = R.string.details),
                    // temperament.name.value(context),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )

                CentAndRatioTable(
                    temperament,
                    rootNotes[selectedRootNoteIndex],
                    notePrintOptions = notePrintOptions,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalContentPadding = 16.dp
                )

                if (hasChainOfFifths) {
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
                        temperament = temperament,
                        rootNote = rootNotes[selectedRootNoteIndex],
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

            }
        }
    )
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun RootNoteDialogTest() {
    TunerTheme {
        val state = remember { MusicalScale2.createTestEdo12() }
        val notePrintOptions = remember { NotePrintOptions() }
        RootNoteDialog(
            state.rootNote,
            state.temperament,
            notePrintOptions = notePrintOptions,
            onDone = { }
        )
    }
}
