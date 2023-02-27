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
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import de.moekadu.tuner.fragments.*
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentArchiving
import de.moekadu.tuner.preferences.ReferenceNotePreferenceDialog
import de.moekadu.tuner.preferences.TemperamentPreferenceDialog
import de.moekadu.tuner.temperaments.NoteNamePrinter
import de.moekadu.tuner.views.PreferenceBarContainer
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    // TODO: anchor-drawable should use round edges

    // TODO: automatic ticks for more or less ticks depending on range

    enum class TunerMode {Simple, Scientific, Unknown}

    private val instrumentArchiving = InstrumentArchiving(
        { instrumentResources.customInstrumentDatabase },
        this, { supportFragmentManager }, { this }
    )

    private lateinit var preferenceBarContainer: PreferenceBarContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        if (preferenceResources.appearance.value.useSystemColorAccents)
            DynamicColors.applyToActivityIfAvailable(this)
        if (preferenceResources.appearance.value.blackNightEnabled)
            overlayThemeForBlackNight()

        migrateInstrumentResources()

        super.onCreate(savedInstanceState)

        if (preferenceResources.screenAlwaysOn.value)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        preferenceBarContainer = PreferenceBarContainer(this)

        if (savedInstanceState == null)
            loadSimpleOrScientificFragment()
        setDisplayHomeButton()

        supportFragmentManager.addFragmentOnAttachListener { _, _ ->
            setDisplayHomeButton()
        }

        supportFragmentManager.addOnBackStackChangedListener {
            setDisplayHomeButton()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleGoBackCommand()
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                preferenceResources.noteNamePrinter.collect {
                    preferenceBarContainer.preferFlat = (it.sharpFlatPreference == NoteNamePrinter.SharpFlatPreference.Flat)
                    val currentPrefs = preferenceResources.temperamentAndReferenceNote.value
                    // update the reference note printing
                    preferenceBarContainer.setReferenceNote(
                        currentPrefs.referenceNote,
                        currentPrefs.referenceFrequency,
                        preferenceResources.noteNamePrinter.value
                    )
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                preferenceResources.temperamentAndReferenceNote.collect {
                    preferenceBarContainer.setTemperament(it.temperamentType)
                    preferenceBarContainer.setReferenceNote(
                        it.referenceNote, it.referenceFrequency,
                        preferenceResources.noteNamePrinter.value
                    )
                }
            }
        }

        ReferenceNotePreferenceDialog.setupFragmentResultListener(
            supportFragmentManager, this
        ) { preferenceResources.setTemperamentAndReferenceNote(it) }
        TemperamentPreferenceDialog.setupFragmentResultListener(
            supportFragmentManager, this, this,
            {preferenceResources.temperamentAndReferenceNote.value}
        ) {
            preferenceResources.setTemperamentAndReferenceNote(it)
        }

        setStatusAndNavigationBarColors()

        if (savedInstanceState == null)
            handleFileLoadingIntent(intent)
//        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
    }

    fun handleGoBackCommand() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else if (!isCurrentFragmentATunerFragment()){
            loadSimpleOrScientificFragment()
        } else {
            finish()
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
    fun loadTuningEditorFragment(instrument: Instrument?) {
//        Log.v("Tuner", "MainActivity.loadTuningEditorFragment")
        val bundle = Bundle(1)
        bundle.putParcelable(InstrumentEditorFragment.INSTRUMENT_KEY, instrument)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<InstrumentEditorFragment>(R.id.main_content, null, bundle)
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

    fun setPreferenceBarVisibilty(visiblity: Int) {
        preferenceBarContainer.visibility = visiblity
    }

    fun switchEnharmonicSetting() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val preferFlat = sharedPreferences.getBoolean("prefer_flat", false)
        val editor = sharedPreferences.edit()
        editor.putBoolean("prefer_flat", !preferFlat)
        editor.apply()
    }

    fun showReferenceNoteDialog() {
        val dialog = ReferenceNotePreferenceDialog.newInstance(
            preferenceResources.temperamentAndReferenceNote.value,
            warningMessage = null
        )
        dialog.show(supportFragmentManager, "tag")
    }

    fun showTemperamentDialog() {
        val dialog = TemperamentPreferenceDialog.newInstance(
            preferenceResources.temperamentAndReferenceNote.value
        )
        dialog.show(supportFragmentManager, "tag")
    }

    private fun setDisplayHomeButton() {
        val showDisplayHomeButton = !isCurrentFragmentATunerFragment()
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
                else -> { }
            }
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
                instrumentArchiving.loadInstruments(uri)
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

    private fun migrateInstrumentResources() {
        val preferences = getPreferences(MODE_PRIVATE)
        if (!preferences.contains("migration completed")) {
            Log.v("Tuner", "MainActivity.migrateInstrumentResources : migrating")
            instrumentResources.migrateFromOtherSharedPreferences(preferences)
            preferences.edit { putBoolean("migration completed", true) }
        } else {
            Log.v("Tuner", "MainActivity.migrateInstrumentResources : not migrating since already done")
        }
    }

}