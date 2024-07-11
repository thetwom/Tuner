package de.moekadu.tuner.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import de.moekadu.tuner.R
import de.moekadu.tuner.preferences.PreferenceResources2
import de.moekadu.tuner.ui.instruments.InstrumentIconPicker
import de.moekadu.tuner.ui.misc.TunerScaffold
import de.moekadu.tuner.ui.screens.InstrumentEditor
import de.moekadu.tuner.viewmodels.InstrumentEditorViewModel2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

fun NavGraphBuilder.instrumentEditorGraph(
    controller: NavController,
    canNavigateUp: Boolean,
    onNavigateUpClicked: () -> Unit,
    preferences: PreferenceResources2,
    scope: CoroutineScope
) {
    navigation<InstrumentEditorGraphRoute>(
        startDestination = InstrumentEditorRoute
    ) {
        composable<InstrumentEditorRoute> {
            val parentEntry = remember(it) { controller.getBackStackEntry(InstrumentEditorGraphRoute) }
            val viewModel: InstrumentEditorViewModel2 = hiltViewModel(parentEntry)

            val musicalScale by preferences.musicalScale.collectAsStateWithLifecycle()
            val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()

            LifecycleResumeEffect(Unit) {
                viewModel.startTuner()
                onPauseOrDispose { viewModel.stopTuner() }
            }
            TunerScaffold(
                canNavigateUp = canNavigateUp,
                onNavigateUpClicked = onNavigateUpClicked,
                showPreferenceButton = false,
                onPreferenceButtonClicked = { controller.navigate(PreferencesGraphRoute) },
                showBottomBar = true,
                onSharpFlatClicked = { scope.launch { preferences.switchSharpFlatPreference() } },
                onReferenceNoteClicked = { // provided by musicalScalePropertiesGraph
                    controller.navigate(
                        ReferenceFrequencyDialogRoute.create(
                            preferences.musicalScale.value, null
                        )
                    )
                },
                onTemperamentClicked = { controller.navigate(TemperamentDialogRoute) },
                musicalScale = musicalScale,
                notePrintOptions = notePrintOptions,
                actionModeActive = true,
                actionModeTitle = stringResource(id = R.string.edit_instrument),
                actionModeTools = {
                    IconButton(onClick = {
                        controller.navigateUp() // TODO: store instrument
                    }) {
                        Icon(Icons.Default.Done, contentDescription = stringResource(id = R.string.done))
                    }
                },
                onActionModeFinishedClicked = { controller.navigateUp() }
            ) { paddingValues ->
                InstrumentEditor(
                    state = viewModel,
                    modifier = Modifier.padding(paddingValues),
                    musicalScale = musicalScale,
                    notePrintOptions = notePrintOptions,
                    onIconButtonClicked = { controller.navigate(IconPickerRoute) }
                )
            }
        }
        dialog<IconPickerRoute> {
            val parentEntry = remember(it) { controller.getBackStackEntry(InstrumentEditorGraphRoute) }
            val viewModel: InstrumentEditorViewModel2 = hiltViewModel(parentEntry)
            InstrumentIconPicker(
                onDismiss = { controller.navigateUp() },
                onIconSelected = { icon ->
                    viewModel.setIcon(icon)
                    controller.navigateUp()
                }
            )
        }
    }
}

@Serializable
data object InstrumentEditorGraphRoute
@Serializable
data object InstrumentEditorRoute
@Serializable
data object IconPickerRoute