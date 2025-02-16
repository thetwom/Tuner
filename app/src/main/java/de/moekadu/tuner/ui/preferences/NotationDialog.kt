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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import de.moekadu.tuner.R
import de.moekadu.tuner.ui.notes.NotationType
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.theme.TunerTheme

@Composable
fun NotationDialog(
    notePrintOptions: NotePrintOptions,
    onNotationChange: (notation: NotationType, isHelmholtz: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    var notationType by remember { mutableStateOf(notePrintOptions.notationType) }
    var helmholtz by remember { mutableStateOf(notePrintOptions.helmholtzNotation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onNotationChange(notationType, helmholtz)
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
                ImageVector.vectorResource(id = R.drawable.ic_solfege),
                contentDescription = null
            )
        },
        title = {
            Text(stringResource(id = R.string.notation))
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Column(Modifier.selectableGroup()) {
                    for (n in NotationType.entries) {
                        RadioButtonLine(
                            selected = notationType == n,
                            stringRes = n.stringResourceId,
                            onClick = { notationType = n }
                        )
                    }
                }
                HorizontalDivider()
                CheckedButtonLine(
                    checked = helmholtz,
                    stringRes = R.string.helmholtz_notation,
                    onClick = { helmholtz = it }
                )
            }
        }
    )
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun NotationDialogTest() {
    TunerTheme {
        var notePrintOptions by remember { mutableStateOf(NotePrintOptions()) }
        NotationDialog(
            notePrintOptions = notePrintOptions,
            onNotationChange = { n, h ->
                notePrintOptions = notePrintOptions.copy(
                    notationType = n, helmholtzNotation = h
                )
            }
        )
    }
}
