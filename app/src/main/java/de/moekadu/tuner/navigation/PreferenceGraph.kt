package de.moekadu.tuner.navigation

import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import androidx.navigation.toRoute
import de.moekadu.tuner.R
import de.moekadu.tuner.preferences.PreferenceResources2
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.NoteNameScaleFactory
import de.moekadu.tuner.ui.misc.TunerScaffold
import de.moekadu.tuner.ui.preferences.AboutDialog
import de.moekadu.tuner.ui.preferences.AppearanceDialog
import de.moekadu.tuner.ui.preferences.NotationDialog
import de.moekadu.tuner.ui.preferences.ReferenceNoteDialog
import de.moekadu.tuner.ui.preferences.ResetDialog
import de.moekadu.tuner.ui.preferences.TemperamentDialog
import de.moekadu.tuner.ui.preferences.WindowingFunctionDialog
import de.moekadu.tuner.ui.screens.Preferences
import de.moekadu.tuner.viewmodels.PreferencesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun NavGraphBuilder.preferenceGraph(
    controller: NavController,
    canNavigateUp: Boolean,
    onNavigateUpClicked: () -> Unit,
    preferences: PreferenceResources2, scope: CoroutineScope
) {
    navigation<PreferencesGraphRoute>(
        startDestination = PreferencesRoute,
    ) {
        composable<PreferencesRoute> {
            val musicalScale by preferences.musicalScale.collectAsStateWithLifecycle()
            val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
            TunerScaffold(
                canNavigateUp = canNavigateUp,
                onNavigateUpClicked = onNavigateUpClicked,
                showPreferenceButton = false,
                showBottomBar = false,
                musicalScale = musicalScale,
                notePrintOptions = notePrintOptions
            ) {
                val viewModel: PreferencesViewModel = hiltViewModel()
                Preferences(
                    viewModel = viewModel,
                    onAppearanceClicked = { controller.navigate(AppearanceDialogRoute) },
                    onReferenceFrequencyClicked = {
                        controller.navigate(
                            ReferenceFrequencyDialogRoute.create(
                                preferences.musicalScale.value,
                                null
                            )
                        )
                    },
                    onNotationClicked = { controller.navigate(NotationDialogRoute) },
                    onTemperamentClicked = { controller.navigate(TemperamentDialogRoute) },
                    onWindowingFunctionClicked = { controller.navigate(WindowingFunctionDialogRoute) },
                    onResetClicked = { controller.navigate(ResetDialogRoute) },
                    onAboutClicked = { controller.navigate(AboutDialogRoute) }
                )
            }
        }
        dialog<AppearanceDialogRoute> {
            val appearance by preferences.appearance.collectAsStateWithLifecycle()
            AppearanceDialog(
                appearance = appearance,
                onAppearanceChanged = {
                    scope.launch {
                        preferences.writeAppearance(it)
                    }
                },
                onDismiss = { controller.navigateUp() }
            )
        }
        dialog<NotationDialogRoute> {
            NotationDialog(
                notePrintOptions = preferences.notePrintOptions.value,
                onNotationChange = { notation, helmholtz ->
                    val newNotePrintOptions = preferences.notePrintOptions.value.copy(
                        notationType = notation, helmholtzNotation = helmholtz
                    )
                    scope.launch {
                        preferences.writeNotePrintOptions(newNotePrintOptions)
                    }
                    controller.navigateUp()
                },
                onDismiss = { controller.navigateUp() }
            )
        }
        dialog<WindowingFunctionDialogRoute> {
            WindowingFunctionDialog(
                initialWindowingFunction = preferences.windowing.value,
                onWindowingFunctionChanged = {
                    scope.launch { preferences.writeWindowing(it) }
                    controller.navigateUp()
                } ,
                onDismiss = { controller.navigateUp() }
            )
        }
        dialog<ResetDialogRoute> {
            ResetDialog(
                onReset = {
                    scope.launch { preferences.resetAllSettings() }
                    controller.navigateUp()
                },
                onDismiss = { controller.navigateUp() }
            )
        }
        dialog<AboutDialogRoute> {
            AboutDialog(
                onDismiss = { controller.navigateUp() }
            )
        }
        // dialogs for reference frequency and temperament
        musicalScalePropertiesGraph(controller, preferences, scope)
    }
}

@Serializable
data object PreferencesGraphRoute
@Serializable
data object PreferencesRoute
@Serializable
data object AppearanceDialogRoute
@Serializable
data object NotationDialogRoute
@Serializable
data object WindowingFunctionDialogRoute
@Serializable
data object ResetDialogRoute
@Serializable
data object AboutDialogRoute
