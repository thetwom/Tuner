package de.moekadu.tuner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import de.moekadu.tuner.ui.screens.ScientificTuner
import de.moekadu.tuner.ui.theme.TunerTheme
import de.moekadu.tuner.viewmodels.ScientificTunerViewModel
import kotlinx.serialization.Serializable

@AndroidEntryPoint
class MainActivity2 : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TunerTheme {
                val controller = rememberNavController()

                NavHost(
                    navController = controller,
                    startDestination = ScientificTunerRoute
                ) {
                    composable<ScientificTunerRoute> {
                        val viewModel: ScientificTunerViewModel = hiltViewModel()
                        LifecycleResumeEffect(Unit) {
                            viewModel.startTuner()
                            onPauseOrDispose { viewModel.stopTuner() }
                        }
                        ScientificTuner(
                            data = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )        
                    }
                }
            }
        }
    }
}

@Serializable
data object ScientificTunerRoute