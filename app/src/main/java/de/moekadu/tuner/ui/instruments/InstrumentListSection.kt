package de.moekadu.tuner.ui.instruments

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import de.moekadu.tuner.ui.theme.TunerTheme

@Composable
fun InstrumentListSection(
    title: String,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onExpandClicked: (newState: Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            IconButton(onClick = { onExpandClicked(!expanded) }) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "collapse"
                )
            }
        },
        modifier = modifier
    )
}

@Preview(widthDp = 300, heightDp = 100, showBackground = true)
@Composable
private fun InstrumentListSectionPreview() {
    TunerTheme {
        var expanded by remember {mutableStateOf(true)}
        Column(modifier = Modifier.fillMaxSize()) {
            InstrumentListSection(
                "My section",
                expanded = expanded,
                onExpandClicked = { expanded = it }
                )
            HorizontalDivider()
        }
    }
}