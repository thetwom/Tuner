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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.ui.theme.TunerTheme

/** General label definition with a background and a content.
 * @param content Label content.
 * @param modifier Modifier.
 * @param color Background color.
 * @param onClick Callback for click on the label.
 */
@Composable
fun Label(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                enabled = true,
                onClick = onClick
            ),
        color = if (color == Color.Unspecified) MaterialTheme.colorScheme.inverseSurface else color
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LabelPreview() {
    TunerTheme {
        var count by remember { mutableIntStateOf(1) }

        Label(
            content = {
                Text(
                    "Label $count",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp)

                )
            },
            modifier = Modifier.padding(12.dp),
            onClick = { count++ },
            color = MaterialTheme.colorScheme.primary
        )
    }
}