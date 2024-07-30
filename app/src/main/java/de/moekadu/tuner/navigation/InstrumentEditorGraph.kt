package de.moekadu.tuner.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import androidx.navigation.toRoute
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentResources2
import de.moekadu.tuner.preferences.PreferenceResources2
import de.moekadu.tuner.ui.instruments.InstrumentIconPicker
import de.moekadu.tuner.ui.misc.TunerScaffold
import de.moekadu.tuner.ui.misc.rememberTunerAudioPermission
import de.moekadu.tuner.ui.screens.InstrumentEditor
import de.moekadu.tuner.viewmodels.InstrumentEditorViewModel2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
private fun createViewModel(controller: NavController, backStackEntry: NavBackStackEntry)
: InstrumentEditorViewModel2 {
    val parentEntry = remember(backStackEntry) {
        controller.getBackStackEntry<InstrumentEditorGraphRoute>()
    }
    val instrument = parentEntry.toRoute<InstrumentEditorGraphRoute>().getInstrument()
    return hiltViewModel<InstrumentEditorViewModel2, InstrumentEditorViewModel2.Factory>(
        parentEntry
    ) { factory ->
        factory.create(instrument)
    }
}

fun NavGraphBuilder.instrumentEditorGraph(
    controller: NavController,
    canNavigateUp: Boolean,
    onNavigateUpClicked: () -> Unit,
    preferences: PreferenceResources2,
    instruments: InstrumentResources2,
    scope: CoroutineScope
) {
    navigation<InstrumentEditorGraphRoute>(
        startDestination = InstrumentEditorRoute
    ) {
//        val instrumentEditorGraphInstance = controller.currentBackStackEntry?.toRoute<InstrumentEditorGraphRoute>()
//        val instrumentEditorGraphInstance = controller.currentBackStackEntry?.toRoute<InstrumentEditorGraphRoute>()
//        val instrument = instrumentEditorGraphInstance?.getInstrument()
//        val instrument = controller
//            .currentBackStackEntry
//            ?.toRoute<InstrumentEditorGraphRoute>()
//            ?.getInstrument()
//            ?: Instrument(
//                "", null, arrayOf(), R.drawable.ic_piano, 0L, false
//            ) // TODO: do not pass 0L as stable id

        composable<InstrumentEditorRoute> {
//            val parentEntry = remember(it) { controller.getBackStackEntry<InstrumentEditorGraphRoute>()}
//            val instrument = parentEntry.toRoute<InstrumentEditorGraphRoute>().getInstrument()
//            val viewModel = hiltViewModel<InstrumentEditorViewModel2, InstrumentEditorViewModel2.Factory>(
//                parentEntry
//            ) { factory ->
//                factory.create(instrument)
//            }
            // val viewModel: InstrumentEditorViewModel2 = hiltViewModel(parentEntry)
            val viewModel = createViewModel(controller = controller, backStackEntry = it)

            val musicalScale by preferences.musicalScale.collectAsStateWithLifecycle()
            val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }
            val permissionGranted = rememberTunerAudioPermission(snackbarHostState)

//            LaunchedEffect(key1 = instrument) {
//                viewModel.setInstrument(instrument)
//            }

            LifecycleResumeEffect(permissionGranted) {
                if (permissionGranted)
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
                        val newInstrument = viewModel.getInstrument()
                        scope.launch {
                            instruments.addNewOrReplaceInstrument(newInstrument)
                        }
                        controller.navigateUp()
                    }) {
                        Icon(Icons.Default.Done, contentDescription = stringResource(id = R.string.done))
                    }
                },
                onActionModeFinishedClicked = { controller.navigateUp() },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
            val viewModel = createViewModel(controller = controller, backStackEntry = it)
//            val parentEntry = remember(it) { controller.getBackStackEntry<InstrumentEditorGraphRoute>()}
//            val viewModel = hiltViewModel<InstrumentEditorViewModel2, InstrumentEditorViewModel2.Factory>(
//                parentEntry
//            ) { factory ->
//                factory.create(instrument)
//            }

            //val viewModel: InstrumentEditorViewModel2 = hiltViewModel(parentEntry)

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
data class InstrumentEditorGraphRoute(val instrumentSerialized: String) {

    fun getInstrument(): Instrument {
        return Json.decodeFromString<Instrument>(instrumentSerialized)
    }
    companion object {
        fun create(instrument: Instrument): InstrumentEditorGraphRoute {
            return InstrumentEditorGraphRoute(
                Json.encodeToString<Instrument>(instrument)
            )
        }
    }
}
@Serializable
data object InstrumentEditorRoute
@Serializable
data object IconPickerRoute