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
package de.moekadu.tuner.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
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
fun EditableListSection(
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
        modifier = modifier.clickable {
            onExpandClicked(!expanded)
        }
    )
}

@Preview(widthDp = 300, heightDp = 100, showBackground = true)
@Composable
private fun InstrumentListSectionPreview() {
    TunerTheme {
        var expanded by remember {mutableStateOf(true)}
        Column(modifier = Modifier.fillMaxSize()) {
            EditableListSection(
                "My section",
                expanded = expanded,
                onExpandClicked = { expanded = it }
                )
            HorizontalDivider()
        }
    }
}