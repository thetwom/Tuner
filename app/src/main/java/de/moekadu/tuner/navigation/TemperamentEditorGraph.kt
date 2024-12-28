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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import androidx.navigation.toRoute
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.temperaments.EditableTemperament
import de.moekadu.tuner.ui.temperaments.TemperamentEditor
import de.moekadu.tuner.ui.temperaments.NumberOfNotesDialog
import de.moekadu.tuner.viewmodels.TemperamentEditorViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
private fun createViewModel(controller: NavController, backStackEntry: NavBackStackEntry)
        : TemperamentEditorViewModel {
    val parentEntry = remember(backStackEntry) {
        controller.getBackStackEntry<TemperamentEditorGraphRoute>()
    }
    val temperament = parentEntry.toRoute<TemperamentEditorGraphRoute>().getEditableTemperament()
    return hiltViewModel<TemperamentEditorViewModel, TemperamentEditorViewModel.Factory>(
        parentEntry
    ) { factory -> factory.create(temperament) }
}


fun NavGraphBuilder.temperamentEditorGraph(
    controller: NavController,
    preferences: PreferenceResources
) {
    navigation<TemperamentEditorGraphRoute>(
        startDestination = TemperamentEditorRoute
    ) {
        composable<TemperamentEditorRoute> {
            val viewModel = createViewModel(controller = controller, backStackEntry = it)
            val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()

            TemperamentEditor(
                state = viewModel,
                notePrintOptions = notePrintOptions,
                onAbortClicked = { controller.navigateUp() },
                onSaveClicked = {
                    viewModel.saveTemperament()
                    controller.navigateUp()
                },
                onNumberOfNotesClicked = { controller.navigate(NumberOfNotesDialogRoute) }
            )
        }

        dialog<NumberOfNotesDialogRoute> {
            val viewModel = createViewModel(controller = controller, backStackEntry = it)
            NumberOfNotesDialog(
                initialNumberOfNotes = viewModel.numberOfValues.value,
                onDismiss = { controller.navigateUp() },
                onDoneClicked = { numberOfNotes ->
                    viewModel.changeNumberOfValues(numberOfNotes)
                    controller.navigateUp()
                }
            )
        }
    }
}

@Serializable
data class TemperamentEditorGraphRoute(
    val serializedEditableTemperament: String,
    ) {
    constructor(temperament: EditableTemperament) : this(
        Json.encodeToString(temperament)
    )
    fun getEditableTemperament(): EditableTemperament {
        return Json.decodeFromString<EditableTemperament>(serializedEditableTemperament)
    }
}

@Serializable
data object TemperamentEditorRoute

@Serializable
data object NumberOfNotesDialogRoute
