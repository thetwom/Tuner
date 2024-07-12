package de.moekadu.tuner.navigation

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.instrumentIcons
import de.moekadu.tuner.preferences.PreferenceResources2
import de.moekadu.tuner.ui.misc.TunerScaffold
import de.moekadu.tuner.ui.screens.InstrumentTuner
import de.moekadu.tuner.ui.screens.Instruments
import de.moekadu.tuner.ui.screens.ScientificTuner
import de.moekadu.tuner.viewmodels.InstrumentTunerViewModel
import de.moekadu.tuner.viewmodels.InstrumentViewModel2
import de.moekadu.tuner.viewmodels.ScientificTunerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable


fun NavGraphBuilder.tunerGraph(
    controller: NavController,
    scope: CoroutineScope,
    canNavigateUp: Boolean,
    onNavigateUpClicked: () -> Unit,
    preferences: PreferenceResources2,
) {
    composable<TunerRoute> {
        val isScientificTuner by preferences.scientificMode.collectAsStateWithLifecycle()
        val musicalScale by preferences.musicalScale.collectAsStateWithLifecycle()
        val notePrintOptions by preferences.notePrintOptions.collectAsStateWithLifecycle()
        //Log.v("Tuner", "TunerGraph: isScientificMode = $isScientificTuner")

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
            notePrintOptions = notePrintOptions
        ) {
            if (isScientificTuner) {
                val viewModel: ScientificTunerViewModel = hiltViewModel()
                LifecycleResumeEffect(Unit) {
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
                LifecycleResumeEffect(Unit) {
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
            onPreferenceButtonClicked = { controller.navigate(PreferencesGraphRoute) }
        )
    }
}

@Serializable
data object TunerRoute

@Serializable
data object InstrumentsRoute