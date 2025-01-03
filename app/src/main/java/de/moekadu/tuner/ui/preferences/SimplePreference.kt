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
package de.moekadu.tuner.ui.preferences

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import de.moekadu.tuner.R
import de.moekadu.tuner.ui.theme.TunerTheme

@Composable
fun SimplePreference(
    name: String,
    @DrawableRes iconId: Int,
    modifier: Modifier = Modifier,
    supporting: String? = null
) {
    ListItem(
        headlineContent = {
            Text(name)
        },
        supportingContent = supporting?.let {{
            Text(it)
        }},
        leadingContent = {
            Icon(ImageVector.vectorResource(id = iconId), null)
        },
        modifier = modifier
    )
}

@Composable
fun SimplePreference(
    name: String,
    @DrawableRes iconId: Int,
    supporting: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(name)
        },
        supportingContent = supporting,
        leadingContent = {
            Icon(ImageVector.vectorResource(id = iconId), null)
        },
        modifier = modifier
    )
}


@Preview(widthDp = 400, heightDp = 200, showBackground = true)
@Composable
private fun SimplePreferencePreview() {
    TunerTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            SimplePreference(
                name = "My preference",
                iconId = R.drawable.ic_harmonic_energy,
                supporting = "Extra text"
            )
            HorizontalDivider()
        }
    }
}