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

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import androidx.navigation.toRoute
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentResources
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.temperaments.TemperamentResources
import de.moekadu.tuner.ui.instruments.InstrumentEditor
import de.moekadu.tuner.ui.instruments.InstrumentIconPicker
import de.moekadu.tuner.ui.misc.rememberTunerAudioPermission
import de.moekadu.tuner.viewmodels.InstrumentEditorViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
private fun createViewModel(controller: NavController, backStackEntry: NavBackStackEntry)
: InstrumentEditorViewModel {
    val parentEntry = remember(backStackEntry) {
        controller.getBackStackEntry<InstrumentEditorGraphRoute>()
    }
    val instrument = parentEntry.toRoute<InstrumentEditorGraphRoute>().getInstrument()
    return hiltViewModel<InstrumentEditorViewModel, InstrumentEditorViewModel.Factory>(
        parentEntry
    ) { factory ->
        factory.create(instrument)
    }
}

fun NavGraphBuilder.instrumentEditorGraph(
    controller: NavController,
    preferences: PreferenceResources,
    instruments: InstrumentResources,
    temperaments: TemperamentResources
) {
    navigation<InstrumentEditorGraphRoute>(
        startDestination = InstrumentEditorRoute
    ) {
        composable<InstrumentEditorRoute> {
            val viewModel = createViewModel(controller = controller, backStackEntry = it)

            val musicalScale by temperaments.musicalScale.collectAsStateWithLifecycle()
            val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }
            val permissionGranted = rememberTunerAudioPermission(snackbarHostState)

            LifecycleResumeEffect(permissionGranted) {
                if (permissionGranted)
                    viewModel.startTuner()
                onPauseOrDispose { viewModel.stopTuner() }
            }
            InstrumentEditor(
                state = viewModel,
                musicalScale = musicalScale,
                notePrintOptions = notePrintOptions,
                onIconButtonClicked = { controller.navigate(IconPickerRoute) },
                onNavigateUpClicked = { controller.navigateUp() },
                onSaveNewInstrumentClicked = {
                    val newInstrument = viewModel.getInstrument()
                    instruments.addNewOrReplaceInstrument(newInstrument)
                    controller.navigateUp()
                },
                snackbarHostState = snackbarHostState
            )
        }
        dialog<IconPickerRoute> {
            val viewModel = createViewModel(controller = controller, backStackEntry = it)
            InstrumentIconPicker(
                onDismiss = { controller.navigateUp() },
                onIconSelected = { icon ->
                    viewModel.setIcon(icon)
                    controller.navigateUp()
                }
            )
        }
    }
}

@Serializable
data class InstrumentEditorGraphRoute(val instrumentSerialized: String) {

    fun getInstrument(): Instrument {
        return Json.decodeFromString<Instrument>(instrumentSerialized)
    }
    companion object {
        fun create(instrument: Instrument): InstrumentEditorGraphRoute {
            return InstrumentEditorGraphRoute(
                Json.encodeToString<Instrument>(instrument)
            )
        }
    }
}
@Serializable
data object InstrumentEditorRoute
@Serializable
data object IconPickerRoute