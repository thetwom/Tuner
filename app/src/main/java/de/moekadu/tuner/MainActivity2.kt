package de.moekadu.tuner

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import de.moekadu.tuner.instruments.InstrumentResources2
import de.moekadu.tuner.navigation.PreferencesGraphRoute
import de.moekadu.tuner.navigation.TunerRoute
import de.moekadu.tuner.navigation.instrumentEditorGraph
import de.moekadu.tuner.navigation.musicalScalePropertiesGraph
import de.moekadu.tuner.navigation.preferenceGraph
import de.moekadu.tuner.navigation.mainGraph
import de.moekadu.tuner.preferences.NightMode
import de.moekadu.tuner.preferences.PreferenceResources2
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity2 : ComponentActivity() {

    @Inject
    lateinit var pref: PreferenceResources2

    @Inject
    lateinit var instruments: InstrumentResources2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                pref.screenAlwaysOn.collect {
                    if (it)
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }

        setContent {
            val appearance by pref.appearance.collectAsStateWithLifecycle()
            TunerTheme(
                darkTheme = when (appearance.mode) {
                    NightMode.Auto -> isSystemInDarkTheme()
                    NightMode.Off -> false
                    NightMode.On -> true
                },
                dynamicColor = appearance.useSystemColorAccents,
                blackNightMode = appearance.blackNightEnabled
            ) {
                val controller = rememberNavController()
                val backStack by controller.currentBackStackEntryAsState()
                val inPreferencesGraph = backStack?.destination?.hierarchy?.any {
                    it.hasRoute(PreferencesGraphRoute::class)
                } == true
                val scope = rememberCoroutineScope()

                var canNavigateUp by remember { mutableStateOf(false) }

                DisposableEffect(controller) {
                    val listener = NavController.OnDestinationChangedListener { controller, _, _ ->
                        canNavigateUp = controller.previousBackStackEntry != null
                    }
                    controller.addOnDestinationChangedListener(listener)
                    onDispose {
                        controller.removeOnDestinationChangedListener(listener)
                    }
                }

                NavHost(
                    modifier = Modifier.fillMaxSize(),
                    navController = controller,
                    startDestination = TunerRoute
                ) {
                    mainGraph(
                        controller = controller,
                        scope = scope,
                        canNavigateUp = canNavigateUp,
                        onNavigateUpClicked = { controller.navigateUp() },
                        preferences = pref,
                        instrumentResources = instruments
                    )
                    preferenceGraph(
                        controller = controller,
                        canNavigateUp = canNavigateUp,
                        onNavigateUpClicked = { controller.navigateUp() },
                        preferences = pref,
                        scope = scope
                    )
                    // provides TemperamentDialogRoute and ReferenceNoteDialog
                    musicalScalePropertiesGraph(
                        controller = controller,
                        preferences = pref,
                        scope = scope
                    )

                    instrumentEditorGraph(
                        controller = controller,
                        canNavigateUp = true,
                        onNavigateUpClicked = {},
                        preferences = pref,
                        instruments = instruments,
                        scope = scope
                    )
                }
            }
        }
    }
}
