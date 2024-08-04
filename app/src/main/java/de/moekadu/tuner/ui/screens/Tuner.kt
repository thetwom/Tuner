package de.moekadu.tuner.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.ui.misc.TunerScaffold
import de.moekadu.tuner.ui.misc.rememberTunerAudioPermission
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.viewmodels.InstrumentTunerViewModel
import de.moekadu.tuner.viewmodels.ScientificTunerViewModel

@Composable
fun Tuner(
    isScientificTuner: Boolean,
    musicalScale: MusicalScale,
    notePrintOptions: NotePrintOptions,
    canNavigateUp: Boolean,
    onNavigateUpClicked: () -> Unit,
    onPreferenceButtonClicked: () -> Unit,
    onSharpFlatClicked: () -> Unit,
    onReferenceNoteClicked: () -> Unit,
    onTemperamentClicked: () -> Unit,
    onInstrumentButtonClicked: () -> Unit,
    waveWriterDuration: Int,
    modifier: Modifier = Modifier
){
    val snackbarHostState = remember { SnackbarHostState() }
    val permissionGranted = rememberTunerAudioPermission(snackbarHostState)

    TunerScaffold(
        canNavigateUp = canNavigateUp,
        onNavigateUpClicked = onNavigateUpClicked,
        showPreferenceButton = true,
        onPreferenceButtonClicked = onPreferenceButtonClicked,
        showBottomBar = true,
        onSharpFlatClicked = onSharpFlatClicked,
        onReferenceNoteClicked = onReferenceNoteClicked,
        onTemperamentClicked = onTemperamentClicked,
        musicalScale = musicalScale,
        notePrintOptions = notePrintOptions,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        floatingActionButton = {
            if (isScientificTuner && waveWriterDuration > 0) {
                FloatingActionButton(
                    onClick = { /* TODO  */ }
                ) {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.ic_mic),
                        contentDescription = "record"
                    )
                }
            }
        },
        floatingActionBarPosition = FabPosition.Start,
        modifier = modifier
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
                onInstrumentButtonClicked = onInstrumentButtonClicked
            )
        }
    }
}