/*
* Copyright 2024 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.moekadu.tuner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentIO
import de.moekadu.tuner.instruments.InstrumentResources
import de.moekadu.tuner.instruments.migratingFromV6
import de.moekadu.tuner.navigation.ImportInstrumentsDialogRoute
import de.moekadu.tuner.navigation.InstrumentsRoute
import de.moekadu.tuner.navigation.TunerRoute
import de.moekadu.tuner.navigation.instrumentEditorGraph
import de.moekadu.tuner.navigation.musicalScalePropertiesGraph
import de.moekadu.tuner.navigation.preferenceGraph
import de.moekadu.tuner.navigation.mainGraph
import de.moekadu.tuner.preferences.NightMode
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var pref: PreferenceResources

    @Inject
    lateinit var instruments: InstrumentResources

    private val loadInstrumentIntentChannel = Channel<List<Instrument>>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        Log.v("Tuner", "MainActivity2.onCreate: savedInstanceState = $savedInstanceState")

        if (savedInstanceState == null)
            handleFileLoadingIntent(intent)

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
                // TODO: preference screen does not show back button
//                val backStack by controller.currentBackStackEntryAsState()
//                val inPreferencesGraph = backStack?.destination?.hierarchy?.any {
//                    it.hasRoute(PreferencesGraphRoute::class)
//                } == true

//                var canNavigateUp by remember { mutableStateOf(false) }
//                Log.v("Tuner", "MainActivity2: canNavigateUp: $canNavigateUp")

                LaunchedEffect(loadInstrumentIntentChannel, controller) {
                    for (instruments in loadInstrumentIntentChannel) {
//                        Log.v("Tuner", "MainActivity2: Loading file ...")
                        controller.popBackStack(TunerRoute, inclusive = false)
                        controller.navigate(InstrumentsRoute)
                        controller.navigate(ImportInstrumentsDialogRoute(
                            Json.encodeToString(instruments.toTypedArray())
                        ))
                    }
                }

//                DisposableEffect(controller) {
//                    val listener = NavController.OnDestinationChangedListener { controller, _, _ ->
//                        canNavigateUp = controller.previousBackStackEntry != null
//                    }
//                    controller.addOnDestinationChangedListener(listener)
//                    onDispose {
//                        controller.removeOnDestinationChangedListener(listener)
//                    }
//                }

                NavHost(
                    modifier = Modifier.fillMaxSize(),
                    navController = controller,
                    startDestination = TunerRoute
                ) {
                    mainGraph(
                        controller = controller,
                        // canNavigateUp = canNavigateUp,
                        onNavigateUpClicked = { controller.navigateUp() },
                        preferences = pref,
                        instrumentResources = instruments
                    )
                    preferenceGraph(
                        controller = controller,
                        //canNavigateUp = canNavigateUp,
                        onNavigateUpClicked = { controller.navigateUp() },
                        preferences = pref
                    )
                    // provides TemperamentDialogRoute and ReferenceNoteDialog
                    musicalScalePropertiesGraph(
                        controller = controller,
                        preferences = pref
                    )

                    instrumentEditorGraph(
                        controller = controller,
                        canNavigateUp = true,
                        onNavigateUpClicked = {},
                        preferences = pref,
                        instruments = instruments
                    )
                }
            }
        }
//        Log.v("Tuner", "Migrating instruments? ${pref.isMigratingFromV6}")
        if (pref.isMigratingFromV6)
            instruments.migratingFromV6(this)
    }

    override fun onNewIntent(intent: Intent) {
        handleFileLoadingIntent(intent)
        super.onNewIntent(intent)
    }

    private fun handleFileLoadingIntent(intent: Intent?) {
//        Log.v("Tuner", "MainActivity2.onNewIntent: action=${intent?.action}")
        val uri = intent?.data
        if ((intent?.action == Intent.ACTION_SEND || intent?.action == Intent.ACTION_VIEW) && uri != null) {
//            Log.v("Tuner", "MainActivity2.onNewIntent: intent=${intent.data}")
            val (readState, instruments) = InstrumentIO.readInstrumentsFromFile(this, uri)
            InstrumentIO.toastFileLoadingResult(this, readState, uri)
            if (instruments.isNotEmpty()) {
                loadInstrumentIntentChannel.trySend(instruments)
            }
        }
    }
}
