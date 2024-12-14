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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import de.moekadu.tuner.R
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.temperaments2.EditableTemperament
import de.moekadu.tuner.temperaments2.MusicalScale2
import de.moekadu.tuner.temperaments2.MusicalScale2Factory
import de.moekadu.tuner.temperaments2.Temperament
import de.moekadu.tuner.temperaments2.TemperamentResources
import de.moekadu.tuner.temperaments2.TemperamentWithNoteNames
import de.moekadu.tuner.temperaments2.getSuitableNoteNames
import de.moekadu.tuner.temperaments2.toEditableTemperament
import de.moekadu.tuner.ui.preferences.ReferenceNoteDialog
import de.moekadu.tuner.ui.preferences.TemperamentDialog
import de.moekadu.tuner.ui.screens.Temperaments2
import de.moekadu.tuner.ui.temperaments.TemperamentDetailsDialog
import de.moekadu.tuner.viewmodels.TemperamentDialogViewModel
import de.moekadu.tuner.viewmodels.Temperaments2ViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
private fun createTemperamentDialogViewModel(
    controller: NavController, backStackEntry: NavBackStackEntry
): TemperamentDialogViewModel? {
    val parentEntry = remember(backStackEntry) {
        try {
            controller.getBackStackEntry<TemperamentDialogRoute>()
        } catch (ex: IllegalArgumentException) {
            null
        }
    }
    return if (parentEntry == null)
        null
    else
        hiltViewModel<TemperamentDialogViewModel>(parentEntry)
}


fun NavGraphBuilder.musicalScalePropertiesGraph(
    controller: NavController,
    preferences: PreferenceResources,
    temperaments: TemperamentResources,
    onLoadTemperaments: (List<EditableTemperament>) -> Unit
) {
    dialog<ReferenceFrequencyDialogRoute> {
        val state = it.toRoute<ReferenceFrequencyDialogRoute>()
        val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
        ReferenceNoteDialog(
            initialState = state.musicalScale,
            onReferenceNoteChange = { newState ->
                temperaments.writeMusicalScale(newState)
                controller.navigateUp()
            },
            notePrintOptions = notePrintOptions,
            warning = state.warning,
            onDismiss = { controller.navigateUp() }
        )
    }

    composable<TemperamentDialogRoute> {
        val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
        val resources = LocalContext.current.resources
//        val viewModel: TemperamentViewModel = hiltViewModel()
        val viewModel: TemperamentDialogViewModel = createTemperamentDialogViewModel(
            controller = controller,
            backStackEntry = it
        )!!
//        val context = LocalContext.current

        TemperamentDialog(
            state = viewModel,
            notePrintOptions = notePrintOptions,
            modifier = Modifier.fillMaxSize(),
            onDismiss = { controller.navigateUp() },
            onDone = { temperament, noteNames, rootNote ->
                val predefinedNoteNames = getSuitableNoteNames(temperament.numberOfNotesPerOctave)
                val noteNamesResolved =
                    if (predefinedNoteNames?.notes?.contentEquals(noteNames.notes) == true)
                        null
                    else
                        noteNames
                val temperamentWithNoteNames = TemperamentWithNoteNames(
                    temperament, noteNamesResolved
                )
                val currentReferenceNote = temperaments.musicalScale.value.referenceNote
                if (noteNames.hasNote(currentReferenceNote)) {
                    temperaments.writeMusicalScale(
                        temperament = temperamentWithNoteNames,
                        rootNote = rootNote
                    )
                    controller.navigateUp()
                } else {
                    val oldScale = temperaments.musicalScale.value
                    val proposedScale = MusicalScale2Factory.create(
                        temperament = temperament,
                        noteNames = noteNamesResolved,
                        referenceNote = null,
                        rootNote = rootNote,
                        referenceFrequency = oldScale.referenceFrequency,
                        frequencyMin = oldScale.frequencyMin,
                        frequencyMax = oldScale.frequencyMax,
                        stretchTuning = oldScale.stretchTuning
                    )
                    controller.navigate(
                        ReferenceFrequencyDialogRoute(
                            proposedScale,
                            resources.getString(R.string.new_temperament_requires_adapting_reference_note)
                        )
                    ) {
                        popUpTo(TemperamentDialogRoute) { inclusive = true }
                    }
                }
            },
            onChooseTemperaments = { controller.navigate(TemperamentsManagerRoute(
                viewModel.temperament.value.stableId
            )) }
        )
    }

    composable<TemperamentsManagerRoute> {
        val context = LocalContext.current
        val resources = context.resources
        val initialTemperamentKey = it.toRoute<TemperamentsManagerRoute>().currentTemperamentKey
        val viewModel = hiltViewModel<Temperaments2ViewModel, Temperaments2ViewModel.Factory>{ factory ->
            factory.create(initialTemperamentKey)
        }
        val viewModelParentDialog = createTemperamentDialogViewModel(
            controller = controller,
            backStackEntry = it
        )
        Temperaments2(
            state = viewModel,
            modifier = Modifier.fillMaxSize(),
            onTemperamentClicked = { temperament ->
                if (viewModelParentDialog != null) {
                    viewModelParentDialog.setNewTemperament(temperament)
                    controller.navigateUp()
                } else {
                    viewModel.activateTemperament(temperament.stableId)
                }
            },
            onEditTemperamentClicked = { temperament, copy ->
                val name = temperament.temperament.name.value(context)
                controller.navigate(
                    TemperamentEditorGraphRoute(
                        temperament.toEditableTemperament(
                            context = context,
                            name = when {
                                copy && name == "" -> ""
                                copy -> "$name (${resources.getString(R.string.copy)})"
                                else -> null // i.e. use name from temperament
                            },
                            stableId = if (copy) Temperament.NO_STABLE_ID else null // null means use stable from temperament
                        )
                    )
                )
            },
            onLoadTemperaments = onLoadTemperaments,
            onTemperamentInfoClicked = { temperament ->
                controller.navigate(TemperamentInfoDialogRoute(temperament))
            },
            onNavigateUp = { controller.navigateUp() }
        )
    }

    dialog<TemperamentInfoDialogRoute> {
        val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
        val temperament = it.toRoute<TemperamentInfoDialogRoute>().obtainTemperament()
        TemperamentDetailsDialog(
            temperament = temperament.temperament,
            noteNames = temperament.noteNames
                ?: getSuitableNoteNames(temperament.temperament.numberOfNotesPerOctave)!!,
            notePrintOptions = notePrintOptions,
            onDismiss = { controller.navigateUp() }
        )
    }
}

// it seems that we cannot use complex arguments, so we must serialize ...
@Serializable
data class ReferenceFrequencyDialogRoute(
    val serializedString: String,
    val warning: String?
) {
    constructor(musicalScale: MusicalScale2, warning: String?) : this(
        Json.encodeToString(musicalScale),
        warning
    )
    val musicalScale get() = Json.decodeFromString<MusicalScale2>(serializedString)

//    fun getMusicalScaleProperties() =
//        Json.decodeFromString<PreferenceResources.MusicalScaleProperties>(serializedString)
//    companion object {
//        fun create(musicalScale: MusicalScale2, warning: String?) = ReferenceFrequencyDialogRoute(
//            Json.encodeToString(PreferenceResources.MusicalScaleProperties.create(musicalScale)),
//            warning
//        )
//        fun create(properties: PreferenceResources.MusicalScaleProperties, warning: String?)
//                = ReferenceFrequencyDialogRoute(Json.encodeToString(properties), warning)
//    }
}

@Serializable
data object TemperamentDialogRoute

@Serializable
data class TemperamentsManagerRoute(
    val currentTemperamentKey: Long
)

@Serializable
data class TemperamentInfoDialogRoute(
    val serializedTemperament: String
) {
    constructor(temperament: TemperamentWithNoteNames) : this(
        Json.encodeToString(temperament)
    )
    fun obtainTemperament(): TemperamentWithNoteNames {
        return Json.decodeFromString<TemperamentWithNoteNames>(serializedTemperament)
    }
}