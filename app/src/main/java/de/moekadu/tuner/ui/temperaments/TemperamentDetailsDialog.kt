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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.NoteNames
import de.moekadu.tuner.temperaments.Temperament
import de.moekadu.tuner.temperaments.createTestTemperamentWerckmeisterVI
import de.moekadu.tuner.temperaments.getSuitableNoteNames
import de.moekadu.tuner.ui.notes.CentAndRatioTable
import de.moekadu.tuner.ui.notes.CircleOfFifthTable
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.theme.TunerTheme

@Composable
fun TemperamentDetailsDialog(
    temperament: Temperament,
    noteNames: NoteNames,
    notePrintOptions: NotePrintOptions,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.acknowledged))
            }
        },
        modifier = modifier,
        icon = {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_temperament),
                contentDescription = "temperament"
            )
        },
        title = { Text(stringResource(id = R.string.details)) },

        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                CentAndRatioTable(
                    temperament,
                    noteNames,
                    rootNoteIndex = 0,
                    notePrintOptions = notePrintOptions,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalContentPadding = 16.dp
                )

                if (temperament.circleOfFifths != null) {
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
                        noteNames = noteNames,
                        rootNoteIndex = 0,
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

@Preview(widthDp = 400, heightDp = 500)
@Composable
private fun TemperamentDetailsDialogPreview() {
    TunerTheme {
        val temperament = remember { createTestTemperamentWerckmeisterVI() }
        val noteNames = remember { getSuitableNoteNames(temperament.numberOfNotesPerOctave)!! }
        TemperamentDetailsDialog(
            temperament,
            noteNames,
            NotePrintOptions()
        )
    }
}