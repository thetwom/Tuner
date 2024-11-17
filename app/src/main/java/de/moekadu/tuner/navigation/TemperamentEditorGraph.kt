package de.moekadu.tuner.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
import de.moekadu.tuner.temperaments2.EditableTemperament
import de.moekadu.tuner.temperaments2.NoteNames
import de.moekadu.tuner.temperaments2.Temperament
import de.moekadu.tuner.temperaments2.TemperamentWithNoteNames
import de.moekadu.tuner.temperaments2.toEditableTemperament
import de.moekadu.tuner.ui.screens.TemperamentEditor
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
