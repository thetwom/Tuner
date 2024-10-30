package de.moekadu.tuner.ui.temperaments

import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.moekadu.tuner.R
import de.moekadu.tuner.ui.theme.TunerTheme

@Composable
fun NumberOfNotesDialog(
    initialNumberOfNotes: Int,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onDoneClicked: (numberOfNotes: Int) -> Unit = {},
) {
    var numberOfNotes by rememberSaveable { mutableStateOf(initialNumberOfNotes.toString()) }
    val hasErrors by remember {
        derivedStateOf {
            val num = numberOfNotes.toIntOrNull()
            num == null || num <= 0
        }
    }
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = { onDoneClicked(numberOfNotes.toIntOrNull() ?: 12) },
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
                isError = hasErrors
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