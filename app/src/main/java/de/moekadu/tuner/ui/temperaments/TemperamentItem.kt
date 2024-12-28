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
package de.moekadu.tuner.ui.temperaments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.misc.StringOrResId
import de.moekadu.tuner.temperaments.Temperament
import de.moekadu.tuner.ui.common.EditableListItem
import de.moekadu.tuner.ui.common.ListItemTask
import de.moekadu.tuner.ui.theme.TunerTheme

@Composable
fun TemperamentItem(
    temperament: Temperament,
    modifier: Modifier = Modifier,
    onOptionsClicked: (temperament: Temperament, task: ListItemTask) -> Unit = { _, _ ->},
    isActive: Boolean = false,
    isSelected: Boolean = false,
    readOnly: Boolean = false, // disable delete/edit options
    isCopyable: Boolean = true // disable copy-option
) {
    val context = LocalContext.current
    val name = temperament.name.value(context)
    val iconTextSize = with(LocalDensity.current) {18.dp.toSp()}
    EditableListItem(
        title = {
            Text(name)
        },
        description = {
            Text(temperament.description.value(context))
        },
        icon = {
            Surface(
                shape = CircleShape,
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent,
                border = BorderStroke(1.dp, LocalContentColor.current)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${name[0]}",
                        fontSize = iconTextSize
                    )
                }
            }
        },
        modifier = modifier,
        onOptionsClicked = { onOptionsClicked(temperament, it) },
        isActive = isActive,
        isSelected = isSelected,
        readOnly = readOnly,
        isCopyable = isCopyable
    )
}

@Preview(widthDp = 400, heightDp = 400, showBackground = true)
@Composable
private fun TemperamentItemPreview() {
    TunerTheme {
        val temperament1 = remember {
            Temperament.create(
                StringOrResId("Name 1"),
                StringOrResId("Abbr"),
                StringOrResId("The description of the first"),
                12,
                1L
            )
        }
        val temperament2 = remember {
            Temperament.create(
                StringOrResId("Name 2"),
                StringOrResId("Abbr"),
                StringOrResId("The description of the second"),
                12,
                1L
            )
        }
        Column {
            TemperamentItem(
                temperament = temperament1
            )
            TemperamentItem(
                temperament = temperament2,
                isSelected = true
            )
            TemperamentItem(
                temperament = temperament1,
                isActive = true,
                readOnly = true
            )
        }
    }
}
