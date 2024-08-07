package de.moekadu.tuner.navigation

import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import de.moekadu.tuner.R
import de.moekadu.tuner.preferences.PreferenceResources2
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.NoteNameScaleFactory
import de.moekadu.tuner.ui.preferences.ReferenceNoteDialog
import de.moekadu.tuner.ui.preferences.TemperamentDialog
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun NavGraphBuilder.musicalScalePropertiesGraph(
    controller: NavController,
    preferences: PreferenceResources2
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
            initialState = PreferenceResources2.MusicalScaleProperties.create(
                preferences.musicalScale.value
            ),
            onTemperamentChange = { newProperties ->
                val newNoteNameScale = NoteNameScaleFactory.create(newProperties.temperamentType)
                if (newNoteNameScale.hasNote(newProperties.referenceNote)) {
                    preferences.writeMusicalScaleProperties(newProperties)
                    controller.navigateUp()
                } else {
                    val proposedCorrectedProperties = newProperties.copy(
                        referenceNote = newNoteNameScale.defaultReferenceNote
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
        Json.decodeFromString<PreferenceResources2.MusicalScaleProperties>(serializedString)
    companion object {
        fun create(musicalScale: MusicalScale, warning: String?) = ReferenceFrequencyDialogRoute(
            Json.encodeToString(PreferenceResources2.MusicalScaleProperties.create(musicalScale)),
            warning
        )
        fun create(properties: PreferenceResources2.MusicalScaleProperties, warning: String?)
                = ReferenceFrequencyDialogRoute(Json.encodeToString(properties), warning)
    }
}

@Serializable
data object TemperamentDialogRoute
