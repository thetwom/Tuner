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
import androidx.compose.runtime.rememberCoroutineScope
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
import de.moekadu.tuner.instruments.InstrumentResources2
import de.moekadu.tuner.navigation.ImportInstrumentsDialogRoute
import de.moekadu.tuner.navigation.InstrumentsRoute
import de.moekadu.tuner.navigation.TunerRoute
import de.moekadu.tuner.navigation.instrumentEditorGraph
import de.moekadu.tuner.navigation.musicalScalePropertiesGraph
import de.moekadu.tuner.navigation.preferenceGraph
import de.moekadu.tuner.navigation.mainGraph
import de.moekadu.tuner.preferences.NightMode
import de.moekadu.tuner.preferences.PreferenceResources2
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity2 : ComponentActivity() {

    @Inject
    lateinit var pref: PreferenceResources2

    @Inject
    lateinit var instruments: InstrumentResources2

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
                val scope = rememberCoroutineScope()

                var canNavigateUp by remember { mutableStateOf(false) }

                LaunchedEffect(loadInstrumentIntentChannel, controller) {
                    for (instruments in loadInstrumentIntentChannel) {
                        Log.v("Tuner", "MainActivity2: Loading file ...")
                        controller.popBackStack(TunerRoute, inclusive = false)
                        controller.navigate(InstrumentsRoute)
                        controller.navigate(ImportInstrumentsDialogRoute(
                            Json.encodeToString(instruments.toTypedArray())
                        ))
                    }
                }

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
