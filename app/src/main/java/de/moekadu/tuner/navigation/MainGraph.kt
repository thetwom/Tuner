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
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentIcon
import de.moekadu.tuner.musicalscale.MusicalScale2
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.temperaments.EditableTemperament
import de.moekadu.tuner.temperaments.Temperament3
import de.moekadu.tuner.temperaments.TemperamentResources
import de.moekadu.tuner.temperaments.toEditableTemperament
import de.moekadu.tuner.ui.instruments.Instruments
import de.moekadu.tuner.ui.preferences.ReferenceNoteDialog
import de.moekadu.tuner.ui.screens.InstrumentTuner
import de.moekadu.tuner.ui.screens.ScientificTuner
import de.moekadu.tuner.ui.temperaments.TemperamentDetailsDialog
import de.moekadu.tuner.ui.temperaments.TemperamentDialog
import de.moekadu.tuner.ui.temperaments.TemperamentsManager
import de.moekadu.tuner.viewmodels.InstrumentTunerViewModel
import de.moekadu.tuner.viewmodels.InstrumentViewModel
import de.moekadu.tuner.viewmodels.ScientificTunerViewModel
import de.moekadu.tuner.viewmodels.TemperamentDialogViewModel
import de.moekadu.tuner.viewmodels.TemperamentsManagerViewModel
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


fun NavGraphBuilder.mainGraph(
    controller: NavController,
    onNavigateUpClicked: () -> Unit,
    preferences: PreferenceResources,
    temperamentResources: TemperamentResources,
    onLoadInstruments: (List<Instrument>) -> Unit,
    onLoadTemperaments: (List<EditableTemperament>) -> Unit
) {
    composable<TunerRoute> {
        val isScientificTuner by preferences.scientificMode.collectAsStateWithLifecycle()
//        Log.v("Tuner", "MainGraph : isScientificTuner = $isScientificTuner")
        if (isScientificTuner) {
            val viewModel: ScientificTunerViewModel = hiltViewModel()
            ScientificTuner(
                data = viewModel,
                canNavigateUp = false,
                onNavigateUpClicked = onNavigateUpClicked,
                onPreferenceButtonClicked = { controller.navigate(PreferencesGraphRoute) },
                onSharpFlatClicked = { preferences.switchEnharmonicPreference() },
                onReferenceNoteClicked = {
                    controller.navigate(
                        // provided by musicalScalePropertiesGraph
                        ReferenceFrequencyDialogRoute(
                            temperamentResources.musicalScale.value, null
                        )
                    )
                },
                onTemperamentClicked = { controller.navigate(TemperamentDialogRoute) }
            )
        } else {
            val viewModel: InstrumentTunerViewModel = hiltViewModel()
            InstrumentTuner(
                canNavigateUp = false,
                onNavigateUpClicked = onNavigateUpClicked,
                onPreferenceButtonClicked = { controller.navigate(PreferencesGraphRoute) },
                onSharpFlatClicked = { preferences.switchEnharmonicPreference() },
                onReferenceNoteClicked = {
                    controller.navigate(
                        // provided by musicalScalePropertiesGraph
                        ReferenceFrequencyDialogRoute(
                            temperamentResources.musicalScale.value, null
                        )
                    )
                },
                onTemperamentClicked = { controller.navigate(TemperamentDialogRoute) },
                onInstrumentButtonClicked = { controller.navigate(InstrumentsRoute) },
                data = viewModel
            )
        }
    }

    composable<InstrumentsRoute> {
        val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
        val musicalScale by temperamentResources.musicalScale.collectAsStateWithLifecycle()
        val viewModel: InstrumentViewModel = hiltViewModel()
        val context = LocalContext.current
        Instruments(
            state = viewModel,
            modifier = Modifier.fillMaxSize(),
            notePrintOptions = notePrintOptions,
//            musicalScale = musicalScale,
            onNavigateUpClicked = { controller.navigateUp() },
            onInstrumentClicked = {
                viewModel.setCurrentInstrument(it)
                controller.navigateUp()
            },
            onEditInstrumentClicked = { instrument, copy ->
                val newInstrument = if (copy) {
                    instrument.copy(
                        nameResource = null,
                        name = context.getString(R.string.copy_extension, instrument.getNameString(context)),
                        stableId = Instrument.NO_STABLE_ID
                    )
                } else {
                    instrument.copy(
                        nameResource = null,
                        name = instrument.getNameString(context)
                    )
                }
                controller.navigate(InstrumentEditorGraphRoute.create(newInstrument))
            },
            onCreateNewInstrumentClicked = {
                val newInstrument = Instrument(
                    nameResource = null,
                    name = "",
                    strings = arrayOf(musicalScale.referenceNote),
                    icon = InstrumentIcon.entries[0],
                    stableId = Instrument.NO_STABLE_ID
                )
                controller.navigate(InstrumentEditorGraphRoute.create(newInstrument))
            },
//            onSharpFlatClicked = { preferences.switchEnharmonicPreference() },
//            onReferenceNoteClicked = { // provided by musicalScalePropertiesGraph
//                controller.navigate(
//                    ReferenceFrequencyDialogRoute(
//                        temperamentResources.musicalScale.value, null
//                    )
//                )
//            },
//            onTemperamentClicked = { controller.navigate(TemperamentDialogRoute) },
            onPreferenceButtonClicked = { controller.navigate(PreferencesGraphRoute) },
            onLoadInstruments = onLoadInstruments
        )
    }

    dialog<ReferenceFrequencyDialogRoute> {
        val state = it.toRoute<ReferenceFrequencyDialogRoute>()
        val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
        ReferenceNoteDialog(
            initialState = state.musicalScale,
            onReferenceNoteChange = { newState ->
                temperamentResources.writeMusicalScale(newState)
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
            onDone = { temperament, rootNote ->
//                val predefinedNoteNames = generateNoteNames(temperament.numberOfNotesPerOctave)
//                val noteNamesResolved =
//                    if (predefinedNoteNames?.notes?.contentEquals(noteNames.notes) == true)
//                        null
//                    else
//                        noteNames
//                val temperamentWithNoteNames = TemperamentWithNoteNames2(
//                    temperament, noteNamesResolved
//                )
                val currentReferenceNote = temperamentResources.musicalScale.value.referenceNote
                val noteNames = temperament.noteNames(rootNote)

                if (noteNames.hasNote(currentReferenceNote)) {
                    temperamentResources.writeMusicalScale(
                        temperament = temperament,
                        rootNote = rootNote
                    )
                    controller.navigateUp()
                } else {
                    val oldScale = temperamentResources.musicalScale.value
                    val proposedScale = MusicalScale2(
                        temperament = temperament,
                        _rootNote = rootNote,
                        _referenceNote = null,
                        referenceFrequency = oldScale.referenceFrequency,
                        frequencyMin = oldScale.frequencyMin,
                        frequencyMax = oldScale.frequencyMax,
                        _stretchTuning = oldScale.stretchTuning
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
        val viewModel = hiltViewModel<TemperamentsManagerViewModel, TemperamentsManagerViewModel.Factory>{ factory ->
            factory.create(initialTemperamentKey)
        }
        val viewModelParentDialog = createTemperamentDialogViewModel(
            controller = controller,
            backStackEntry = it
        )
        TemperamentsManager(
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
                val name = temperament.name.value(context)
                controller.navigate(
                    TemperamentEditorGraphRoute(
                        temperament.toEditableTemperament(
                            context = context,
                            name = when {
                                copy && name == "" -> ""
                                copy -> "$name (${resources.getString(R.string.copy_)})"
                                else -> null // i.e. use name from temperament
                            },
                            stableId = if (copy) Temperament3.NO_STABLE_ID else null // null means use stable from temperament
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
            temperament = temperament,
            notePrintOptions = notePrintOptions,
            onDismiss = { controller.navigateUp() }
        )
    }


//    dialog<TestRoute>(
//        deepLinks = listOf(
//            navDeepLink<TestRoute>(
//                basePath = "tuner://instruments",
//                deepLinkBuilder = {
//                    uriPattern = "*"
//                    action = Intent.ACTION_VIEW
//                }
//            )
//        )
//    ) {
//        AboutDialog(
//            onDismiss = {controller.navigateUp()}
//        )
//    }
}

@Serializable
data object TunerRoute

@Serializable
data object InstrumentsRoute

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
    constructor(temperament: Temperament3) : this(
        Json.encodeToString(temperament)
    )
    fun obtainTemperament(): Temperament3 {
        return Json.decodeFromString<Temperament3>(serializedTemperament)
    }
}
//@Serializable
//data object TestRoute