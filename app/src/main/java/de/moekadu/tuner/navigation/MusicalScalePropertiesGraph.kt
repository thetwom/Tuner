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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import de.moekadu.tuner.R
import de.moekadu.tuner.misc.StringOrResId
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.temperaments2.MusicalScale2
import de.moekadu.tuner.temperaments2.MusicalScale2Factory
import de.moekadu.tuner.temperaments2.Temperament
import de.moekadu.tuner.temperaments2.TemperamentResources
import de.moekadu.tuner.temperaments2.getSuitableNoteNames
import de.moekadu.tuner.ui.preferences.ReferenceNoteDialog
import de.moekadu.tuner.ui.screens.Temperaments
import de.moekadu.tuner.viewmodels.TemperamentViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun NavGraphBuilder.musicalScalePropertiesGraph(
    controller: NavController,
    preferences: PreferenceResources,
    temperaments: TemperamentResources
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
        val viewModel: TemperamentViewModel = hiltViewModel()
        val context = LocalContext.current

        Temperaments(
            viewModel,
            modifier = Modifier.fillMaxSize(),
            notePrintOptions = notePrintOptions,
            onAbort = { controller.navigateUp() },
            onNewTemperament = { temperament, rootNote ->
                val noteNames = temperament.noteNames ?: getSuitableNoteNames(temperament.temperament.numberOfNotesPerOctave)
                if (noteNames?.hasNote(temperaments.musicalScale.value.referenceNote) == true) {
                    temperaments.writeMusicalScale(temperament = temperament, rootNote = rootNote)
                    controller.navigateUp()
                } else {
                    val oldScale = temperaments.musicalScale.value
                    val proposedScale = MusicalScale2Factory.create(
                        temperament = temperament.temperament,
                        noteNames = temperament.noteNames,
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
            onEditTemperamentClicked = { temperament, copy ->
                controller.navigate(
                    TemperamentEditorGraphRoute(
                        if (copy) {
                            temperament.temperament.copy(
                                name = StringOrResId(
                                    if (temperament.temperament.name.value(context) == "")
                                        ""
                                    else
                                        "${temperament.temperament.name.value(context)} (${resources.getString(R.string.copy)})"
                                ),
                                stableId = Temperament.NO_STABLE_ID
                            )
                        } else {
                            temperament.temperament
                        },
                        temperament.noteNames
                    )
                )
            }
//            onTemperamentChange = { newProperties ->
//                val newNoteNameScale = NoteNameScaleFactory.create(newProperties.temperamentType)
//                if (newNoteNameScale.hasNote(newProperties.referenceNote)) {
//                    preferences.writeMusicalScaleProperties(newProperties)
//                    controller.navigateUp()
//                } else {
//                    val proposedCorrectedProperties = newProperties.copy(
//                        referenceNote = newNoteNameScale.referenceNote
//                    )
//                    controller.navigate(
//                        ReferenceFrequencyDialogRoute.create(
//                            proposedCorrectedProperties,
//                            resources.getString(R.string.new_temperament_requires_adapting_reference_note)
//                        )
//                    ) {
//                        popUpTo(TemperamentDialogRoute) { inclusive = true }
//                    }
//                }
//            },
//            notePrintOptions =  notePrintOptions,
//            onDismiss = { controller.navigateUp() }
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
