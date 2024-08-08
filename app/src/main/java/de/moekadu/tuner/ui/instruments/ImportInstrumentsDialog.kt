package de.moekadu.tuner.ui.instruments

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentIO
import de.moekadu.tuner.ui.preferences.RadioButtonLine
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private data class ImportOption(
    val insertMode: InstrumentIO.InsertMode,
    @StringRes val stringResource: Int
)

private val importOptions = persistentListOf(
    ImportOption(InstrumentIO.InsertMode.Replace, R.string.replace_current_list),
    ImportOption(InstrumentIO.InsertMode.Prepend, R.string.prepend_current_list),
    ImportOption(InstrumentIO.InsertMode.Append, R.string.append_current_list),
)

@Composable
fun ImportInstrumentsDialog(
    instruments: ImmutableList<Instrument>,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = { },
    onImport: (insertMode: InstrumentIO.InsertMode, instruments: ImmutableList<Instrument>) -> Unit = { _,_ -> }
) {
    var importChoice by rememberSaveable { mutableStateOf(InstrumentIO.InsertMode.Append) }
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onImport(importChoice, instruments)
            }) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.abort))
            }
        },
        title = {
            Text(
                context.resources.getQuantityString(
                    R.plurals.load_instruments, instruments.size, instruments.size
                )
            )
        },
        icon = {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_unarchive),
                contentDescription = "import"
            )
        },
        text = {
            Column(Modifier.selectableGroup()) {
                importOptions.forEach {
                    RadioButtonLine(
                        selected = (it.insertMode == importChoice),
                        stringRes = it.stringResource,
                        onClick = { importChoice = it.insertMode }
                    )
                }
            }
        },
        modifier = modifier
    )
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun ImportInstrumentsDialogTest() {
    TunerTheme {
        ImportInstrumentsDialog(
            instruments = persistentListOf()
        )
    }
}