package de.moekadu.tuner.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import de.moekadu.tuner.R

@Composable
fun ImportExportOverflowMenu(
    onExportClicked: () -> Unit,
    onImportClicked: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = {
            expanded = !expanded
        }) {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_import_export),
                contentDescription = "import/export"
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.save_to_disk)) },
                leadingIcon = {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.ic_archive),
                        contentDescription = "archive"
                    )
                },
                onClick = {
                    onExportClicked()
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.load_from_disk)) },
                leadingIcon = {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.ic_unarchive),
                        contentDescription = "unarchive"
                    )
                },
                onClick = {
                    onImportClicked()
                    expanded = false
                }
            )
        }
    }
}
