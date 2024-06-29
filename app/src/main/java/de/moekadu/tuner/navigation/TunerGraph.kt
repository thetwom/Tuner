package de.moekadu.tuner.navigation

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import de.moekadu.tuner.preferences.PreferenceResources2
import de.moekadu.tuner.ui.screens.Preferences
import de.moekadu.tuner.ui.screens.InstrumentTuner
import de.moekadu.tuner.ui.screens.ScientificTuner
import de.moekadu.tuner.viewmodels.InstrumentTunerViewModel
import de.moekadu.tuner.viewmodels.PreferencesViewModel
import de.moekadu.tuner.viewmodels.ScientificTunerViewModel
import kotlinx.serialization.Serializable


fun NavGraphBuilder.tunerGraph(navController: NavController, preferences: PreferenceResources2) {
    composable<TunerRoute> {
        val isScientificTuner by preferences.scientificMode.collectAsStateWithLifecycle()
        Log.v("Tuner", "TunerGraph: isScientificMode = $isScientificTuner")

        if (isScientificTuner) {
            val viewModel: ScientificTunerViewModel = hiltViewModel()
            LifecycleResumeEffect(Unit) {
                viewModel.startTuner()
                onPauseOrDispose { viewModel.stopTuner() }
            }
            ScientificTuner(
                data = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val viewModel: InstrumentTunerViewModel = hiltViewModel()
            LifecycleResumeEffect(Unit) {
                viewModel.startTuner()
                onPauseOrDispose { viewModel.stopTuner() }
            }
            InstrumentTuner(
                data = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

//    composable<PreferencesRoute> {
//        val viewModel: PreferencesViewModel = hiltViewModel()
//        Preferences(viewModel = viewModel)
//    }
}

@Serializable
data object TunerRoute

//@Serializable
//data object PreferencesRoute