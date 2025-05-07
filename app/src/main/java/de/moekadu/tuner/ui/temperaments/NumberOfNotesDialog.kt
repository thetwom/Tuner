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

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import de.moekadu.tuner.R
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.coroutines.android.awaitFrame

@Composable
fun NumberOfNotesDialog(
    initialNumberOfNotes: Int,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onDoneClicked: (numberOfNotes: Int) -> Unit = {},
) {
    val maxNumberOfNotes = 200
    var numberOfNotes by rememberSaveable(
            stateSaver = TextFieldValue.Saver
            ) { mutableStateOf(
        TextFieldValue(initialNumberOfNotes.toString(), TextRange(initialNumberOfNotes.toString().length))
    ) }
    val hasErrors by remember {
        derivedStateOf {
            val num = numberOfNotes.text.toIntOrNull()
            num == null || num <= 0 || num > maxNumberOfNotes
        }
    }
    val maxValueExceeded by remember {
        derivedStateOf {
            val num = numberOfNotes.text.toIntOrNull()
            num != null && num > maxNumberOfNotes
        }
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        awaitFrame()
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = { onDoneClicked(numberOfNotes.text.toIntOrNull() ?: 12) },
                enabled = !hasErrors
            ) {
                Text(stringResource(id = R.string.done))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() }
            ) {
                Text(stringResource(id = R.string.abort))
            }
        },
        modifier = modifier,
        title = { Text(stringResource(id = R.string.note_number)) },
        text = {
            TextField(
                value = numberOfNotes,
                onValueChange = { numberOfNotes = it },
                isError = hasErrors,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (!hasErrors)
                            onDoneClicked(numberOfNotes.text.toIntOrNull() ?: 12)
                    }
                ),
                supportingText = if (maxValueExceeded) {
                    { Text(stringResource(id = R.string.maximum_value_exceeded, maxNumberOfNotes)) }
                } else {
                    null
                },
                trailingIcon = {
                    IconButton(onClick = { numberOfNotes = TextFieldValue("") }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                },
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
    )
}

@Preview(widthDp = 400, heightDp = 500)
@Composable
private fun NumberOfNotesDialogPreview() {
    TunerTheme {
        NumberOfNotesDialog(initialNumberOfNotes = 12)
    }
}