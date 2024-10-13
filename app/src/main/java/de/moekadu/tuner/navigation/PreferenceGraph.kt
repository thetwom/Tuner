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
package de.moekadu.tuner.navigation

import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.R
import de.moekadu.tuner.ui.misc.TunerScaffold
import de.moekadu.tuner.ui.preferences.AboutDialog
import de.moekadu.tuner.ui.preferences.AppearanceDialog
import de.moekadu.tuner.ui.preferences.NotationDialog
import de.moekadu.tuner.ui.preferences.ResetDialog
import de.moekadu.tuner.ui.preferences.WindowingFunctionDialog
import de.moekadu.tuner.ui.screens.Preferences
import de.moekadu.tuner.viewmodels.PreferencesViewModel
import kotlinx.serialization.Serializable

fun NavGraphBuilder.preferenceGraph(
    controller: NavController,
    //canNavigateUp: Boolean,
    onNavigateUpClicked: () -> Unit,
    preferences: PreferenceResources
) {
    navigation<PreferencesGraphRoute>(
        startDestination = PreferencesRoute,
    ) {
        composable<PreferencesRoute> {
            val musicalScale by preferences.musicalScale.collectAsStateWithLifecycle()
            val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
            TunerScaffold(
                canNavigateUp = controller.rememberCanNavigateUp(),
                onNavigateUpClicked = onNavigateUpClicked,
                title = stringResource(id = R.string.settings),
                showPreferenceButton = false,
                showBottomBar = false,
                musicalScale = musicalScale,
                notePrintOptions = notePrintOptions
            ) {
                val viewModel: PreferencesViewModel = hiltViewModel()
                Preferences(
                    viewModel = viewModel,
                    onAppearanceClicked = { controller.navigate(AppearanceDialogRoute) },
                    onReferenceFrequencyClicked = {
                        controller.navigate(
                            ReferenceFrequencyDialogRoute.create(
                                preferences.musicalScale.value,
                                null
                            )
                        )
                    },
                    onNotationClicked = { controller.navigate(NotationDialogRoute) },
                    onTemperamentClicked = { controller.navigate(TemperamentDialogRoute) },
                    onWindowingFunctionClicked = { controller.navigate(WindowingFunctionDialogRoute) },
                    onResetClicked = { controller.navigate(ResetDialogRoute) },
                    onAboutClicked = { controller.navigate(AboutDialogRoute) }
                )
            }
        }
        dialog<AppearanceDialogRoute> {
            val appearance by preferences.appearance.collectAsStateWithLifecycle()
            AppearanceDialog(
                appearance = appearance,
                onAppearanceChanged = { preferences.writeAppearance(it) },
                onDismiss = { controller.navigateUp() }
            )
        }
        dialog<NotationDialogRoute> {
            NotationDialog(
                notePrintOptions = preferences.notePrintOptions.value,
                onNotationChange = { notation, helmholtz ->
                    val newNotePrintOptions = preferences.notePrintOptions.value.copy(
                        notationType = notation, helmholtzNotation = helmholtz
                    )
                    preferences.writeNotePrintOptions(newNotePrintOptions)
                    controller.navigateUp()
                },
                onDismiss = { controller.navigateUp() }
            )
        }
        dialog<WindowingFunctionDialogRoute> {
            WindowingFunctionDialog(
                initialWindowingFunction = preferences.windowing.value,
                onWindowingFunctionChanged = {
                    preferences.writeWindowing(it)
                    controller.navigateUp()
                } ,
                onDismiss = { controller.navigateUp() }
            )
        }
        dialog<ResetDialogRoute> {
            ResetDialog(
                onReset = {
                    preferences.resetAllSettings()
                    controller.navigateUp()
                },
                onDismiss = { controller.navigateUp() }
            )
        }
        dialog<AboutDialogRoute> {
            AboutDialog(
                onDismiss = { controller.navigateUp() }
            )
        }
        // dialogs for reference frequency and temperament
        musicalScalePropertiesGraph(controller, preferences)
    }
}

@Serializable
data object PreferencesGraphRoute
@Serializable
data object PreferencesRoute
@Serializable
data object AppearanceDialogRoute
@Serializable
data object NotationDialogRoute
@Serializable
data object WindowingFunctionDialogRoute
@Serializable
data object ResetDialogRoute
@Serializable
data object AboutDialogRoute
