package de.moekadu.tuner.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import de.moekadu.tuner.R
import de.moekadu.tuner.ui.theme.TunerTheme

@Composable
fun ResetDialog(
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onReset
            ) {
                Text(stringResource(id = R.string.yes))
            }
        },
        modifier = modifier,
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(id = R.string.no))
            }
        },
        icon = {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_reset),
                contentDescription = null
            )
        },
        title = {
            Text(stringResource(id = R.string.reset_all_settings))
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(id = R.string.reset_settings_prompt))
            }
        }
    )
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun ResetDialogTest() {
    TunerTheme {
        ResetDialog(
            onReset = {}
        )
    }
}
