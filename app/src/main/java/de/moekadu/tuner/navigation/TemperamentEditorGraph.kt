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
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.temperaments2.NoteNames
import de.moekadu.tuner.temperaments2.Temperament
import de.moekadu.tuner.temperaments2.TemperamentResources
import de.moekadu.tuner.ui.screens.TemperamentEditor
import de.moekadu.tuner.ui.temperaments.NumberOfNotesDialog
import de.moekadu.tuner.ui.temperaments.TemperamentDescriptionDialog
import de.moekadu.tuner.ui.temperaments.TemperamentLineDialog
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
    val temperament = parentEntry.toRoute<TemperamentEditorGraphRoute>().getTemperamentWithNoteNames()
    return hiltViewModel<TemperamentEditorViewModel, TemperamentEditorViewModel.Factory>(
        parentEntry
    ) { factory ->
        factory.create(temperament)
    }
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
                onNumberOfNotesClicked = { controller.navigate(NumberOfNotesDialogRoute) },
                onDescriptionClicked = { controller.navigate(TemperamentDescriptionDialogRoute) },
                onCentValueChanged = { i, v ->
                    viewModel.modifyCentOrRatioValue(i, v)
                },
                onNoteNameClicked = { index ->
                    controller.navigate(TemperamentLineDialogRoute(index))
                }
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

        dialog<TemperamentDescriptionDialogRoute> {
            val viewModel = createViewModel(controller = controller, backStackEntry = it)
            val context = LocalContext.current
            TemperamentDescriptionDialog(
                initialName = viewModel.name.value.value(context),
                initialAbbreviation = viewModel.abbreviation.value.value(context),
                initialDescription =  viewModel.description.value.value(context),
                onDismiss = { controller.navigateUp() },
                onDoneClicked = { n, a, d ->
                    viewModel.changeDescription(n, a, d)
                    controller.navigateUp()
                }
            )
        }

        dialog<TemperamentLineDialogRoute> {
            val viewModel = createViewModel(controller = controller, backStackEntry = it)

            val state = it.toRoute<TemperamentLineDialogRoute>()
            val index = state.index
            val line = viewModel.temperamentValues.value.getOrNull(index)
            val note = line?.note ?: MusicalNote(BaseNote.A, NoteModifier.None, octave = 4)
            val isReferenceNote = line?.isReferenceNote == true
            val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()

            TemperamentLineDialog(
                initialNote = note,
                initialIsReferenceNote = isReferenceNote,
                initialReferenceNoteOctave = note.octave,
                notePrintOptions = notePrintOptions,
                onDismiss = { controller.navigateUp() },
                onDoneClicked = { newNote, newIsReference ->
                    viewModel.modifyNote(index, newNote, newIsReference)
                    controller.navigateUp()
                }
            )
        }
    }
}

@Serializable
data class TemperamentEditorGraphRoute(
    val serializedTemperament: String,
    val serializedNoteNames: String?
    ) {
    constructor(temperament: Temperament, noteNames: NoteNames?) : this(
        Json.encodeToString(temperament),
        noteNames?.let { Json.encodeToString(it) }
    )
    fun getTemperamentWithNoteNames(): TemperamentResources.TemperamentWithNoteNames {
        return TemperamentResources.TemperamentWithNoteNames(
            Json.decodeFromString<Temperament>(serializedTemperament),
            serializedNoteNames?.let { Json.decodeFromString<NoteNames>(it) }
        )
    }
}

@Serializable
data object TemperamentEditorRoute

@Serializable
data object TemperamentDescriptionDialogRoute

@Serializable
data object NumberOfNotesDialogRoute

@Serializable
data class TemperamentLineDialogRoute(val index: Int)