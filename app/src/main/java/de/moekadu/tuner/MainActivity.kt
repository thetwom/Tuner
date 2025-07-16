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

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.DisposableEffect
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
import de.moekadu.tuner.misc.FileCheck
import de.moekadu.tuner.misc.toastPotentialFileCheckError
import de.moekadu.tuner.navigation.InstrumentsRoute
import de.moekadu.tuner.navigation.TemperamentDialogRoute
import de.moekadu.tuner.navigation.TunerRoute
import de.moekadu.tuner.navigation.instrumentEditorGraph
import de.moekadu.tuner.navigation.preferenceGraph
import de.moekadu.tuner.navigation.mainGraph
import de.moekadu.tuner.navigation.temperamentEditorGraph
import de.moekadu.tuner.navigation.TemperamentEditorGraphRoute
import de.moekadu.tuner.preferences.NightMode
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.preferences.migrateFromV6
import de.moekadu.tuner.temperaments.EditableTemperament
import de.moekadu.tuner.temperaments.TemperamentIO
import de.moekadu.tuner.temperaments.TemperamentResources
import de.moekadu.tuner.temperaments.hasErrors
import de.moekadu.tuner.temperaments.toTemperament3Custom
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var pref: PreferenceResources

    @Inject
    lateinit var instruments: InstrumentResources

    @Inject
    lateinit var temperaments: TemperamentResources

    private val loadInstrumentIntentChannel = Channel<List<Instrument>>(Channel.CONFLATED)
    private val loadTemperamentIntentChannel = Channel<List<EditableTemperament>>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
//        Log.v("Tuner", "MainActivity2.onCreate: savedInstanceState = $savedInstanceState")

        runBlocking {
            migrateFromV6(this@MainActivity, pref, temperaments, instruments)
        }

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

                // handle incoming intents for loading instruments
                LaunchedEffect(loadInstrumentIntentChannel, controller) {
                    for (instrumentList in loadInstrumentIntentChannel) {
//                        Log.v("Tuner", "MainActivity2: Loading file ...")
                        controller.popBackStack(TunerRoute, inclusive = false)
                        controller.navigate(InstrumentsRoute)
                        loadInstruments(instrumentList, instruments, this@MainActivity)
                    }
                }

                // handle incoming intents for loading temperaments
                LaunchedEffect(loadTemperamentIntentChannel, controller) {
                    for (temperamentList in loadTemperamentIntentChannel) {
//                        Log.v("Tuner", "MainActivity2: Loading file ...")
                        controller.popBackStack(TunerRoute, inclusive = false)
                        controller.navigate(TemperamentDialogRoute)
//                         controller.navigate(TemperamentsManagerRoute(temperaments.musicalScale.value.temperament.stableId))
                        loadTemperaments(
                            controller, temperamentList, temperaments, this@MainActivity
                        )
                    }
                }
                DisposableEffect(controller) {
                    val listener = NavController.OnDestinationChangedListener { _, _, _ ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            if (controller.previousBackStackEntry == null) {
                                setShowWhenLocked(pref.displayOnLockScreen.value)
                            } else {
                                setShowWhenLocked(false)
                            }
                        }
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
                        onNavigateUpClicked = { controller.navigateUp() },
                        preferences = pref,
                        temperamentResources = temperaments,
                        onLoadInstruments = {
                            loadInstruments(it, instruments, this@MainActivity)
                        },
                        onLoadTemperaments = { temperamentList ->
                            loadTemperaments(
                                controller, temperamentList, temperaments, this@MainActivity
                            )
                        }
                    )
                    preferenceGraph(
                        controller = controller,
                        onNavigateUpClicked = { controller.navigateUp() },
                        preferences = pref,
                        temperaments = temperaments
                    )

                    instrumentEditorGraph(
                        controller = controller,
                        preferences = pref,
                        instruments = instruments,
                        temperaments = temperaments
                    )

                    temperamentEditorGraph(
                        controller = controller,
                        preferences = pref
                    )
                }
            }
        }
//        Log.v("Tuner", "Migrating instruments? ${pref.isMigratingFromV6}")
//        if (pref.isMigratingFromV6)
//            instruments.migratingFromV6(this)
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
            val (stateInstruments, instruments) = InstrumentIO.readInstrumentsFromFile(
                this, uri
            )
            val (stateTemperaments, temperaments) = TemperamentIO.readTemperamentsFromFile(
                this, uri
            )
            when {
                stateInstruments == FileCheck.Ok && instruments.isNotEmpty() -> {
                    loadInstrumentIntentChannel.trySend(instruments)
                }
                stateTemperaments == FileCheck.Ok && temperaments.isNotEmpty() -> {
                    loadTemperamentIntentChannel.trySend(temperaments)
                }
                stateInstruments == FileCheck.Empty || stateTemperaments == FileCheck.Empty -> {
                    FileCheck.Empty.toastPotentialFileCheckError(this, uri)
                }
                else -> {
                    FileCheck.Invalid.toastPotentialFileCheckError(this, uri)
                }
            }
        }
    }
}

private fun loadTemperaments(
    controller: NavController,
    temperamentList: List<EditableTemperament>,
    temperamentResources: TemperamentResources,
    context: Context
) {
    if (temperamentList.firstOrNull { it.hasErrors() } == null) {
        Toast.makeText(
            context,
            context.resources.getQuantityString(
                R.plurals.load_temperaments, temperamentList.size, temperamentList.size
            ),
            Toast.LENGTH_LONG
        ).show()

        temperamentResources.appendTemperaments(
            temperamentList.mapNotNull { it.toTemperament3Custom() }
        )
    } else if (temperamentList.size == 1) { // one temperament with errors
        controller.navigate(
            TemperamentEditorGraphRoute(temperamentList[0])
        )
    }
    // TODO: else branches:
    //  - if several temperaments, where there are errors -> launch dialog telling this
    //    maybe with options to load the correct ones?
}

private fun loadInstruments(
    instrumentList: List<Instrument>,
    instrumentResources: InstrumentResources,
    context: Context
) {
    Toast.makeText(
        context,
        context.resources.getQuantityString(
            R.plurals.load_instruments,
            instrumentList.size,
            instrumentList.size
        ),
        Toast.LENGTH_LONG
    ).show()
    instrumentResources.appendInstruments(instrumentList)
}

