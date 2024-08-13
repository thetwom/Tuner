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

import android.os.Build
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
import de.moekadu.tuner.preferences.NightMode
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.ui.theme.TunerTheme



@Composable
fun AppearanceDialog(
    appearance: PreferenceResources.Appearance,
    onAppearanceChanged: (PreferenceResources.Appearance)-> Unit,
    onDismiss: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.done))
            }
        },
        icon = {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_appearance),
                contentDescription = null
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.appearance)
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Column(Modifier.selectableGroup()) {
                    RadioButtonLine(
                        selected = appearance.mode == NightMode.Auto,
                        stringRes = R.string.system_appearance_short,
                        onClick = {
                            if (appearance.mode != NightMode.Auto)
                                onAppearanceChanged(appearance.copy(mode = NightMode.Auto))
                        }
                    )
                    RadioButtonLine(
                        selected = appearance.mode == NightMode.Off,
                        stringRes = R.string.light_appearance_short,
                        onClick = {
                            if (appearance.mode != NightMode.Off)
                                onAppearanceChanged(appearance.copy(mode = NightMode.Off))
                        }
                    )
                    RadioButtonLine(
                        selected = appearance.mode == NightMode.On,
                        stringRes = R.string.dark_appearance_short,
                        onClick = {
                            if (appearance.mode != NightMode.On)
                                onAppearanceChanged(appearance.copy(mode = NightMode.On))
                        }
                    )
                }
                HorizontalDivider()
                CheckedButtonLine(
                    checked = appearance.blackNightEnabled,
                    stringRes = R.string.black_night_mode,
                    onClick = {
                        if (appearance.blackNightEnabled != it)
                            onAppearanceChanged(appearance.copy(blackNightEnabled = it))
                    }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    CheckedButtonLine(
                        checked = appearance.useSystemColorAccents,
                        stringRes = R.string.system_color_accents,
                        onClick = {
                            if (appearance.useSystemColorAccents != it)
                                onAppearanceChanged(appearance.copy(useSystemColorAccents = it))
                        }
                    )
                }
            }
        }
    )
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun AppearanceDialogTest() {
    TunerTheme {
        var appearance by remember {mutableStateOf(PreferenceResources.Appearance())}

        AppearanceDialog(
            appearance,
            onAppearanceChanged = {
                appearance = it
            }
        )
    }
}