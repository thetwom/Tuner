package de.moekadu.tuner.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentIO
import de.moekadu.tuner.instruments.InstrumentResources2
import de.moekadu.tuner.instruments.instrumentIcons
import de.moekadu.tuner.preferences.PreferenceResources2
import de.moekadu.tuner.ui.instruments.ImportInstrumentsDialog
import de.moekadu.tuner.ui.misc.TunerScaffold
import de.moekadu.tuner.ui.misc.rememberTunerAudioPermission
import de.moekadu.tuner.ui.screens.InstrumentTuner
import de.moekadu.tuner.ui.screens.Instruments
import de.moekadu.tuner.ui.screens.ScientificTuner
import de.moekadu.tuner.viewmodels.InstrumentTunerViewModel
import de.moekadu.tuner.viewmodels.InstrumentViewModel2
import de.moekadu.tuner.viewmodels.ScientificTunerViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


fun NavGraphBuilder.mainGraph(
    controller: NavController,
    scope: CoroutineScope,
    canNavigateUp: Boolean,
    onNavigateUpClicked: () -> Unit,
    preferences: PreferenceResources2,
    instrumentResources: InstrumentResources2
) {
    composable<TunerRoute> {
        val isScientificTuner by preferences.scientificMode.collectAsStateWithLifecycle()
        val musicalScale by preferences.musicalScale.collectAsStateWithLifecycle()
        val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
//        val context = LocalContext.current
        //Log.v("Tuner", "TunerGraph: isScientificMode = $isScientificTuner")
        val snackbarHostState = remember { SnackbarHostState() }
        val permissionGranted = rememberTunerAudioPermission(snackbarHostState)

//        val reopenSnackbarChannel = Channel<Boolean>(Channel.CONFLATED)
//        val permission = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO) {
//            if (!it)
//                reopenSnackbarChannel.trySend(false)
//        }
//        Log.v("Tuner", "MainGraph: 1: permissions_granted = ${permission.status.isGranted}, rational = ${permission.status.shouldShowRationale}")
////            if (!permission.status.isGranted && permission.status.shouldShowRationale)
////                Column {
////                    Text(stringResource(id = R.string.audio_record_permission_rationale))
////                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
////                        Text("Request permission")
////                    }
////                }
//
//        LaunchedEffect(permission.status) {
//            Log.v("Tuner", "MainGraph: permissions_granted = ${permission.status.isGranted}, rational = ${permission.status.shouldShowRationale}")
//            if (!permission.status.isGranted && permission.status.shouldShowRationale) {// TODO: show rationale if requested
//                launch {
//                    val result = snackbarHostState.showSnackbar(
//                        context.getString(R.string.audio_record_permission_rationale),
//                        actionLabel = context.getString(R.string.settings), // TODO: string label
//                        withDismissAction = false
//                    )
//                    when (result) {
//                        SnackbarResult.Dismissed -> {}
//                        SnackbarResult.ActionPerformed -> {
//                            permission.launchPermissionRequest()
//                        }
//                    }
//                }
//            } else if (!permission.status.isGranted) {
//                permission.launchPermissionRequest()
//                for (reopen in reopenSnackbarChannel) {
//                    launch {
//                        val result = snackbarHostState.showSnackbar(
//                            context.getString(R.string.audio_record_permission_rationale),
//                            actionLabel = context.getString(R.string.settings), // TODO: string label
//                            withDismissAction = false
//                        )
//                        when (result) {
//                            SnackbarResult.Dismissed -> {}
//                            SnackbarResult.ActionPerformed -> {
//                                permission.launchPermissionRequest()
//                            }
//                        }
//                    }
//                }
//            }
//        }


        TunerScaffold(
            canNavigateUp = canNavigateUp,
            onNavigateUpClicked = onNavigateUpClicked,
            showPreferenceButton = true,
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
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) {
            if (isScientificTuner) {
                val viewModel: ScientificTunerViewModel = hiltViewModel()
                LifecycleResumeEffect(permissionGranted) {
                    if (permissionGranted)
                        viewModel.startTuner()
                    onPauseOrDispose { viewModel.stopTuner() }
                }
                ScientificTuner(
                    data = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = it)
                )
            } else {
                val viewModel: InstrumentTunerViewModel = hiltViewModel()
                LifecycleResumeEffect(permissionGranted) {
                    if (permissionGranted)
                        viewModel.startTuner()
                    onPauseOrDispose { viewModel.stopTuner() }
                }
                InstrumentTuner(
                    data = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = it),
                    onInstrumentButtonClicked = {
                        controller.navigate(InstrumentsRoute)
                    }
                )
            }
        }
    }

    composable<InstrumentsRoute> {
        val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
        val musicalScale by preferences.musicalScale.collectAsStateWithLifecycle()
        val viewModel: InstrumentViewModel2 = hiltViewModel()
        val context = LocalContext.current
        Instruments(
            state = viewModel,
            modifier = Modifier.fillMaxSize(),
            notePrintOptions = notePrintOptions,
            musicalScale = musicalScale,
            onNavigateUpClicked = { controller.navigateUp() },
            onInstrumentClicked = {
                scope.launch { viewModel.setCurrentInstrument(it) }
                controller.navigateUp()
            },
            onEditInstrumentClicked = { instrument, copy ->
                val newInstrument = if (copy) {
                    instrument.copy(
                        nameResource = null,
                        name = context.getString(R.string.copy_extension, instrument.getNameString2(context)),
                        stableId = Instrument.NO_STABLE_ID
                    )
                } else {
                    instrument.copy(
                        nameResource = null,
                        name = instrument.getNameString2(context)
                    )
                }
                controller.navigate(InstrumentEditorGraphRoute.create(newInstrument))
            },
            onCreateNewInstrumentClicked = {
                val newInstrument = Instrument(
                    nameResource = null,
                    name = "",
                    strings = arrayOf(musicalScale.referenceNote),
                    iconResource = instrumentIcons[0].resourceId,
                    stableId = Instrument.NO_STABLE_ID
                )
                controller.navigate(InstrumentEditorGraphRoute.create(newInstrument))
            },
            onSharpFlatClicked = { scope.launch { preferences.switchSharpFlatPreference() } },
            onReferenceNoteClicked = { // provided by musicalScalePropertiesGraph
                controller.navigate(
                    ReferenceFrequencyDialogRoute.create(
                        preferences.musicalScale.value, null
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

    // TODO: deep link to here to import
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
                scope.launch {
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