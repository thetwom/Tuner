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
import de.moekadu.tuner.notedetection.WindowingFunction
import de.moekadu.tuner.preferences.PreferenceResources2
import de.moekadu.tuner.ui.notes.NotationType
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.theme.TunerTheme

@Composable
fun WindowingFunctionDialog(
    initialWindowingFunction: WindowingFunction,
    onWindowingFunctionChanged: (windowingFunction: WindowingFunction) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    var windowingFunction by remember { mutableStateOf(initialWindowingFunction) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onWindowingFunctionChanged(windowingFunction)
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
                ImageVector.vectorResource(id = R.drawable.ic_window_function),
                contentDescription = null
            )
        },
        title = {
            Text(stringResource(id = R.string.windowing_function))
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Column(Modifier.selectableGroup()) {
                    for (w in WindowingFunction.entries) {
                        RadioButtonLine(
                            selected = w == windowingFunction,
                            stringRes = w.stringResourceId,
                            onClick = { windowingFunction = w }
                        )
                    }
                }
            }
        }
    )
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun WindowingFunctionDialogTest() {
    TunerTheme {
        WindowingFunctionDialog(
            WindowingFunction.Hann,
            onWindowingFunctionChanged = {}
        )
    }
}
