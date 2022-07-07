/*
 * Copyright 2020 Michael Moessner
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
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import de.moekadu.tuner.fragments.*
import de.moekadu.tuner.preferences.AppPreferences
import de.moekadu.tuner.preferences.AppearancePreference
import de.moekadu.tuner.viewmodels.InstrumentsViewModel

class MainActivity : AppCompatActivity() {
    // TODO: accuracyincreaser-test fails
    // TODO: anchor-drawable should use round edges
    // TODO: baseline-alignment for marks
    enum class TunerMode {Simple, Scientific, Unknown}

    private val instrumentsViewModel: InstrumentsViewModel by viewModels {
        InstrumentsViewModel.Factory(
            AppPreferences.readInstrumentId(this),
            AppPreferences.readInstrumentSection(this),
            AppPreferences.readCustomInstruments(this),
            AppPreferences.readPredefinedSectionExpanded(this),
            AppPreferences.readCustomSectionExpanded(this),
            application
        )
    }

    // private var scientificMode = TunerMode.Unknown

    override fun onCreate(savedInstanceState: Bundle?) {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val appearanceString = sharedPreferences.getString("appearance", "auto") ?: "auto"
//        Log.v("Tuner", "MainActivity.onCreate: appearanceString: $appearanceString, systemAccents=${AppearancePreference.getUseSystemColorAccents(appearanceString)}, blackNight=${AppearancePreference.getBlackNightEnabledFromValue(appearanceString)}")
        if (AppearancePreference.getUseSystemColorAccents(appearanceString))
            DynamicColors.applyToActivityIfAvailable(this)
        if (AppearancePreference.getBlackNightEnabledFromValue(appearanceString))
            overlayThemeForBlackNight()

        super.onCreate(savedInstanceState)

        val screenOn = sharedPreferences.getBoolean("screenon", false)
        if (screenOn)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        if (savedInstanceState == null)
            loadSimpleOrScientificFragment()
        setDisplayHomeButton()

        supportFragmentManager.addFragmentOnAttachListener { _, _ ->
            setDisplayHomeButton()
        }

        supportFragmentManager.addOnBackStackChangedListener {
            setDisplayHomeButton()
            //if (supportFragmentManager.backStackEntryCount == 0)
            //    loadSimpleOrScientificFragment()
        }

        setStatusAndNavigationBarColors()

        if (savedInstanceState == null)
            handleFileLoadingIntent(intent)
//        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
    }

    override fun onStop() {
        AppPreferences.writeTunerPreferences(
            instrumentsViewModel.instrument.value?.instrument?.stableId,
            instrumentsViewModel.instrument.value?.section?.name,
            instrumentsViewModel.predefinedDatabaseExpanded.value ?: true,
            instrumentsViewModel.customDatabaseExpanded.value ?: true,
            this)
        super.onStop()
    }

    override fun onBackPressed() {
//        Log.v("Tuner", "MainActivity.onBackPressed")
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else if (!isCurrentFragmentATunerFragment()){
            loadSimpleOrScientificFragment()
        } else {
            super.onBackPressed()
        }
    }
    override fun onSupportNavigateUp(): Boolean {
//        Log.v("Tuner", "MainActivity.onSupportNavigateUp")
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else if (!isCurrentFragmentATunerFragment()){
            loadSimpleOrScientificFragment()
        }
        return super.onSupportNavigateUp()
    }

    fun loadSettingsFragment() {
        supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace<SettingsFragment>(R.id.main_content)
                if (!isCurrentFragmentATunerFragment())
                    addToBackStack(null)
            }
    }
    fun loadTuningEditorFragment() {
//        Log.v("Tuner", "MainActivity.loadTuningEditorFragment")
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<InstrumentEditorFragment>(R.id.main_content)
            if (!isCurrentFragmentATunerFragment())
                addToBackStack(null)
        }
    }

    fun loadInstrumentsFragment() {
//        Log.v("Tuner", "MainActivity.loadInstrumentsFragment")
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<InstrumentsFragment>(R.id.main_content)
            if (!isCurrentFragmentATunerFragment())
                addToBackStack(null)
        }
    }

    private fun setDisplayHomeButton() {
        val showDisplayHomeButton = !isCurrentFragmentATunerFragment() //supportFragmentManager.backStackEntryCount > 0
        supportActionBar?.setDisplayHomeAsUpEnabled(showDisplayHomeButton)
    }

    private fun isCurrentFragmentATunerFragment(): Boolean {
        return when (supportFragmentManager.findFragmentById(R.id.main_content)) {
            is TunerFragment -> true
            is TunerFragmentSimple -> true
            else -> false
        }
    }

    private fun loadSimpleOrScientificFragment() {
//        Log.v("Tuner", "MainActivity.loadSimpleOrScientificFragment")
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val currentMode = when (supportFragmentManager.findFragmentById(R.id.main_content)) {
            is TunerFragment -> TunerMode.Scientific // Log.v("Tuner", "MainActivity.loadSimpleOrScientificFragment: activeFragment = TunerFragment")
            is TunerFragmentSimple -> TunerMode.Simple // Log.v("Tuner", "MainActivity.loadSimpleOrScientificFragment: activeFragment = TunerFragmentSimple")
            //null ->  Log.v("Tuner", "MainActivity.loadSimpleOrScientificFragment: activeFragment = null")
            else -> TunerMode.Unknown // Log.v("Tuner", "MainActivity.loadSimpleOrScientificFragment: activeFragment = something else")
        }
        val modeFromPreferences = if (sharedPreferences.getBoolean("scientific", false))
            TunerMode.Scientific
        else
            TunerMode.Simple

        if (modeFromPreferences != currentMode) {
            when (modeFromPreferences) {
                TunerMode.Scientific -> {
                    supportFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace<TunerFragment>(R.id.main_content)
                    }
                }
                TunerMode.Simple -> {
                    supportFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace<TunerFragmentSimple>(R.id.main_content)
                    }
                }
                TunerMode.Unknown -> {

                }
            }
            // scientificMode = modeFromPreferences
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
//        Log.v("Tuner", "MainActivity.onNewIntent: intent=$intent")
        handleFileLoadingIntent(intent)
    }

    private fun handleFileLoadingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND || intent?.action == Intent.ACTION_VIEW) {
//            Log.v("Tuner", "MainActivity.handleFileLoadingIntent: intent=${intent.data}")

            intent.data?.let { uri ->
//                Log.v("Tuner", "MainActivity.handleFileLoadingIntent: uri=$uri")
                supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                instrumentsViewModel.loadInstrumentsFromFile(uri)
                loadInstrumentsFragment()
            }
        }
    }

    fun setStatusAndNavigationBarColors() {
        val uiMode = resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)
//        Log.v("Tuner", "MainActivity.setStatusAndNavigationBarColors: uiMode = $uiMode, ${if(uiMode==Configuration.UI_MODE_NIGHT_YES) "NIGHT_MODE_YES" else if (uiMode==Configuration.UI_MODE_NIGHT_NO) "NIGHT_MODE_NO" else "OTHER_UI_MODE"}")
        if (uiMode == null || uiMode == Configuration.UI_MODE_NIGHT_UNDEFINED)
            return

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        val backgroundColor = typedValue.data

        // set status bar color
        window.statusBarColor = backgroundColor

        // set status bar icon colors to dark if dark-mode is off
        if(uiMode == Configuration.UI_MODE_NIGHT_NO) {
            if (Build.VERSION.SDK_INT < 30) {
                val view = window.decorView
                view.systemUiVisibility = view.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
//                Log.v("Tuner", "MainActivity: set light appearance status bars")
                window.insetsController?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            }
        }

        // set navigation bar color to background color only
        // - if we are in dark mode
        // - we have SDK >= 27
        // since for light mode and SDK < 27 we cannot set dark icons
        if (uiMode == Configuration.UI_MODE_NIGHT_YES || Build.VERSION.SDK_INT >= 27)
            window.navigationBarColor = backgroundColor

        // set dark icons in navigation bar if navigation bar is light color
        if (Build.VERSION.SDK_INT >= 27 && uiMode == Configuration.UI_MODE_NIGHT_NO) {
            if (Build.VERSION.SDK_INT < 30) {
                val view = window.decorView
                view.systemUiVisibility = view.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                window.insetsController?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
        }
    }

    private fun overlayThemeForBlackNight() {
        val uiMode = resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)
//        Log.v("Tuner", "MainActivity.overlayTheme: uiMode = $uiMode")
        if (uiMode == Configuration.UI_MODE_NIGHT_YES)
            theme.applyStyle(R.style.ThemeOverlay_BlackNight, true)
    }

}