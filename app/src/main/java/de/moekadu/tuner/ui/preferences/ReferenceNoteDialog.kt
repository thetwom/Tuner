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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.musicalscale.MusicalScale2
import de.moekadu.tuner.musicalscale.MusicalScaleFactory
import de.moekadu.tuner.ui.misc.rememberNumberFormatter
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.NoteSelector
import de.moekadu.tuner.ui.theme.TunerTheme
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParsePosition

private fun DecimalFormat.toFloatOrNull(string: String): Float? {
    return try {
        val trimmed = string.trim()
        val position = ParsePosition(0)
        val result = this.parse(trimmed, position)?.toFloat()
        if (position.index == trimmed.length)
            result
        else
            null
    } catch (ex: Exception) {
        null
    }
}

@Composable
fun ReferenceNoteDialog(
    initialState: MusicalScale2,
    onReferenceNoteChange: (modifiedState: MusicalScale2) -> Unit,
    notePrintOptions: NotePrintOptions,
    modifier: Modifier = Modifier,
    warning: String? = null,
    onDismiss: () -> Unit = {}
) {
    var selectedNoteIndex by rememberSaveable { mutableIntStateOf(
        initialState.getNoteIndex(initialState.referenceNote) - initialState.noteIndexBegin
    ) }
    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        LocalConfiguration.current.locales[0]
    } else {
        LocalConfiguration.current.locale
    }
    val decimalFormat = rememberNumberFormatter()
    var frequencyAsString by rememberSaveable {
        mutableStateOf(decimalFormat.format(initialState.referenceFrequency))
    }
    val numberFormat = remember(locale) {
        NumberFormat.getNumberInstance(locale) as DecimalFormat
    }
    val validFrequency by remember { derivedStateOf {
        numberFormat.toFloatOrNull(frequencyAsString) != null
    }}
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val note = initialState.getNote(selectedNoteIndex + initialState.noteIndexBegin)
                    onReferenceNoteChange(
                        initialState.copy(
                            referenceNote = note,
                            referenceFrequency = numberFormat.toFloatOrNull(frequencyAsString)
                                ?: initialState.referenceFrequency
                        )
                    )
                },
                enabled = validFrequency
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
                ImageVector.vectorResource(id = R.drawable.ic_frequency_a),
                contentDescription = null
            )
        },
        title = {
            Text(stringResource(id = R.string.reference_frequency))
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
                NoteSelector(
                    selectedIndex = selectedNoteIndex,
                    musicalScale = initialState,
                    notePrintOptions = notePrintOptions,
                    fontSize = MaterialTheme.typography.labelLarge.fontSize,
                    onIndexChanged = { selectedNoteIndex = it }
                )
                Spacer(
                    modifier = Modifier.height(8.dp)
                )
                TextField(
                    value = frequencyAsString,
                    onValueChange = { frequencyAsString = it },
                    label = { Text(stringResource(id = R.string.frequency))},
                    suffix = { Text(stringResource(id = R.string.hertz_str, ""))},
                    isError = !validFrequency,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(
                    modifier = Modifier.height(8.dp)
                )
                OutlinedButton(
                    onClick = {
                        val note = initialState.noteNames.defaultReferenceNote
                        selectedNoteIndex = initialState.getNoteIndex(note) - initialState.noteIndexBegin
                        frequencyAsString = decimalFormat.format(PreferenceResources.ReferenceFrequencyDefault)
                    },
                    modifier = Modifier.fillMaxWidth()
                    ) {
                    Text(stringResource(id = R.string.set_default))
                }
            }
        }
    )
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun AppearanceDialogTest() {
    TunerTheme {
        val state = remember { MusicalScaleFactory.createTestEdo12() }
        val notePrintOptions = remember { NotePrintOptions() }
        ReferenceNoteDialog(
            state,
            notePrintOptions = notePrintOptions,
            onReferenceNoteChange = { }
        )
    }
}
