package de.moekadu.tuner

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import de.moekadu.tuner.navigation.PreferencesGraphRoute
import de.moekadu.tuner.navigation.PreferencesRoute
import de.moekadu.tuner.navigation.ReferenceFrequencyDialogRoute
import de.moekadu.tuner.navigation.TemperamentDialogRoute
import de.moekadu.tuner.navigation.TunerRoute
import de.moekadu.tuner.navigation.musicalScalePropertiesGraph
import de.moekadu.tuner.navigation.preferenceGraph
import de.moekadu.tuner.navigation.tunerGraph
import de.moekadu.tuner.preferences.NightMode
import de.moekadu.tuner.preferences.PreferenceResources2
import de.moekadu.tuner.ui.misc.QuickSettingsBar
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity2 : ComponentActivity() {

    @Inject lateinit var pref: PreferenceResources2

    @OptIn(ExperimentalMaterial3Api::class)
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

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(getString(R.string.app_name)) },
                            navigationIcon = {
                                if (canNavigateUp) {
                                    IconButton(onClick = { controller.navigateUp() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "back")
                                    }
                                }
                            },
                            actions = {
                                if (!inPreferencesGraph) {
                                    IconButton(onClick = {
                                        controller.navigate(PreferencesGraphRoute)
                                    }) {
                                        Icon(Icons.Filled.Settings, "settings")
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        if (!inPreferencesGraph) {
                            val musicalScale by pref.musicalScale.collectAsStateWithLifecycle()
                            val notePrintOptions by pref.notePrintOptions.collectAsStateWithLifecycle()
                            QuickSettingsBar(
                                musicalScale = musicalScale,
                                notePrintOptions = notePrintOptions,
                                onSharpFlatClicked = {
                                    scope.launch {
                                        val currentFlatSharpChoice = pref.notePrintOptions.value.sharpFlatPreference
                                        val newFlatShapeChoice = if (currentFlatSharpChoice == NotePrintOptions.SharpFlatPreference.Flat)
                                            NotePrintOptions.SharpFlatPreference.Sharp
                                        else
                                            NotePrintOptions.SharpFlatPreference.Flat
                                        pref.writeNotePrintOptions(pref.notePrintOptions.value.copy(
                                            sharpFlatPreference = newFlatShapeChoice
                                        ))
                                    }
                                },
                                onReferenceNoteClicked = {
                                    // provided by musicalScalePropertiesGraph
                                    controller.navigate(ReferenceFrequencyDialogRoute.create(
                                        pref.musicalScale.value, null
                                    ))
                                },
                                onTemperamentClicked = {
                                    // provided by musicalScalePropertiesGraph
                                    controller.navigate(TemperamentDialogRoute)
                                }
                            )
                        }
                    }
                ) { padding ->
                    NavHost(
                        modifier = Modifier.padding(padding),
                        navController = controller,
                        startDestination = TunerRoute
                    ) {
                        tunerGraph(navController = controller, preferences = pref)
                        preferenceGraph(controller = controller, preferences = pref, scope = scope)
                        // provides TemperamentDialogRoute and ReferenceNoteDialog
                        musicalScalePropertiesGraph(controller = controller, preferences = pref, scope = scope)
                    }
                }
            }
        }
    }
}
