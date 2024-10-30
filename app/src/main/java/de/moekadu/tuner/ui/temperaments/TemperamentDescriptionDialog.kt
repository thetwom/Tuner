package de.moekadu.tuner.ui.temperaments

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.ui.theme.TunerTheme

@Composable
fun TemperamentDescriptionDialog(
    initialName: String,
    initialAbbreviation: String,
    initialDescription: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onDoneClicked: (name: String, abbreviation: String, description: String) -> Unit = {_, _, _ -> },
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var abbreviation by rememberSaveable { mutableStateOf(initialAbbreviation) }
    var description by rememberSaveable { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = {
                    onDoneClicked(name, abbreviation, description)
                },
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
        title = { Text(stringResource(id = R.string.description)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.temperament_name)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = abbreviation,
                    onValueChange = { abbreviation = it },
                    label = { Text(stringResource(id = R.string.temperament_abbreviation)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(id = R.string.temperament_description)) }
                )
            }
        }
    )
}

@Preview(widthDp = 400, heightDp = 500)
@Composable
private fun TemperamentDescriptionDialogPreview() {
    TunerTheme {
        TemperamentDescriptionDialog(
            "test name",
            "t",
            "longer description"
        )
    }
}