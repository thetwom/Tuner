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

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentIO
import de.moekadu.tuner.instruments.InstrumentIcon
import de.moekadu.tuner.instruments.InstrumentResources
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.temperaments2.TemperamentResources
import de.moekadu.tuner.ui.instruments.ImportInstrumentsDialog
import de.moekadu.tuner.ui.screens.InstrumentTuner
import de.moekadu.tuner.ui.screens.Instruments
import de.moekadu.tuner.ui.screens.ScientificTuner
import de.moekadu.tuner.viewmodels.InstrumentTunerViewModel
import de.moekadu.tuner.viewmodels.InstrumentViewModel
import de.moekadu.tuner.viewmodels.ScientificTunerViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun NavGraphBuilder.mainGraph(
    controller: NavController,
    onNavigateUpClicked: () -> Unit,
    preferences: PreferenceResources,
    instrumentResources: InstrumentResources,
    temperamentResources: TemperamentResources
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
                onSharpFlatClicked = { preferences.switchSharpFlatPreference() },
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
                onSharpFlatClicked = { preferences.switchSharpFlatPreference() },
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
            musicalScale = musicalScale,
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
            onSharpFlatClicked = { preferences.switchSharpFlatPreference() },
            onReferenceNoteClicked = { // provided by musicalScalePropertiesGraph
                controller.navigate(
                    ReferenceFrequencyDialogRoute(
                        temperamentResources.musicalScale.value, null
                    )
                )
            },
            onTemperamentClicked = { controller.navigate(TemperamentDialogRoute) },
            onPreferenceButtonClicked = { controller.navigate(PreferencesGraphRoute) },
            onLoadInstruments = {
                controller.navigate(
                    ImportInstrumentsDialogRoute(Json.encodeToString(it.toTypedArray()))
                )
            }
        )
    }

    dialog<ImportInstrumentsDialogRoute> {
        val instrumentsString = it.toRoute<ImportInstrumentsDialogRoute>().instrumentsString
        val instruments = remember(instrumentsString) {
            Json.decodeFromString<Array<Instrument>>(instrumentsString).toList().toImmutableList()
        }
        val context = LocalContext.current
        ImportInstrumentsDialog(
            instruments = instruments,
            onDismiss = { controller.navigateUp() },
            onImport = { mode, importedInstruments ->
                when (mode) {
                    InstrumentIO.InsertMode.Replace -> {
                        instrumentResources.replaceInstruments(importedInstruments)
                    }
                    InstrumentIO.InsertMode.Prepend -> {
                        instrumentResources.prependInstruments(importedInstruments)
                    }
                    InstrumentIO.InsertMode.Append -> {
                        instrumentResources.appendInstruments(importedInstruments)
                    }
                }
                Toast.makeText(
                    context,
                    context.resources.getQuantityString(
                        R.plurals.load_instruments,
                        importedInstruments.size,
                        importedInstruments.size
                    ),
                    Toast.LENGTH_LONG
                ).show()
                controller.navigateUp()
            }
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

@Serializable
data class ImportInstrumentsDialogRoute(val instrumentsString: String)

//@Serializable
//data object TestRoute