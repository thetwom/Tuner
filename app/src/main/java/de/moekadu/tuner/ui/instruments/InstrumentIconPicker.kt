package de.moekadu.tuner.ui.instruments

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.instrumentIcons
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.toImmutableList

@Composable
fun InstrumentIconPicker(
    modifier: Modifier = Modifier,
    onIconSelected: (icon: Int) -> Unit = { },
    onDismiss: () -> Unit = { }
) {
    val icons = remember {
        instrumentIcons.toList().toImmutableList()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.abort))
            }
        },
        title = {
            Text(stringResource(id = R.string.pick_icon))
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(50.dp)
            ) {
                items(icons) {
                    IconButton(onClick = { onIconSelected(it.resourceId) }) {
                        Icon(
                            ImageVector.vectorResource(id = it.resourceId),
                            contentDescription = it.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        },
        modifier = modifier
    )
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun InstrumentIconPickerTest() {
    TunerTheme {
        InstrumentIconPicker {

        }
    }
}