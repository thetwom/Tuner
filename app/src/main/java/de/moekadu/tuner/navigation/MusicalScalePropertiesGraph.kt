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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import de.moekadu.tuner.R
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.NoteNameScaleFactory
import de.moekadu.tuner.ui.preferences.ReferenceNoteDialog
import de.moekadu.tuner.ui.preferences.TemperamentDialog
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun NavGraphBuilder.musicalScalePropertiesGraph(
    controller: NavController,
    preferences: PreferenceResources
) {
    dialog<ReferenceFrequencyDialogRoute> {
        val state = it.toRoute<ReferenceFrequencyDialogRoute>()
        val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
        ReferenceNoteDialog(
            initialState = state.getMusicalScaleProperties(),
            onReferenceNoteChange = { newState ->
                preferences.writeMusicalScaleProperties(newState)
                controller.navigateUp()
            },
            notePrintOptions = notePrintOptions,
            warning = state.warning,
            onDismiss = { controller.navigateUp() }
        )
    }

    dialog<TemperamentDialogRoute> {
        val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
        val resources = LocalContext.current.resources
        TemperamentDialog(
            initialState = PreferenceResources.MusicalScaleProperties.create(
                preferences.musicalScale.value
            ),
            onTemperamentChange = { newProperties ->
                val newNoteNameScale = NoteNameScaleFactory.create(newProperties.temperamentType)
                if (newNoteNameScale.hasNote(newProperties.referenceNote)) {
                    preferences.writeMusicalScaleProperties(newProperties)
                    controller.navigateUp()
                } else {
                    val proposedCorrectedProperties = newProperties.copy(
                        referenceNote = newNoteNameScale.referenceNote
                    )
                    controller.navigate(
                        ReferenceFrequencyDialogRoute.create(
                            proposedCorrectedProperties,
                            resources.getString(R.string.new_temperament_requires_adapting_reference_note)
                        )
                    ) {
                        popUpTo(TemperamentDialogRoute) { inclusive = true }
                    }
                }
            },
            notePrintOptions =  notePrintOptions,
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
    fun getMusicalScaleProperties() =
        Json.decodeFromString<PreferenceResources.MusicalScaleProperties>(serializedString)
    companion object {
        fun create(musicalScale: MusicalScale, warning: String?) = ReferenceFrequencyDialogRoute(
            Json.encodeToString(PreferenceResources.MusicalScaleProperties.create(musicalScale)),
            warning
        )
        fun create(properties: PreferenceResources.MusicalScaleProperties, warning: String?)
                = ReferenceFrequencyDialogRoute(Json.encodeToString(properties), warning)
    }
}

@Serializable
data object TemperamentDialogRoute
