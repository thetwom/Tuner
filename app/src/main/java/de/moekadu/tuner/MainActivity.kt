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

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {
    // TODO: Allow setting minimum and maximum allowed note
    // ... more settings possible?

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
    private val tuningEditorViewModel: TuningEditorViewModel by viewModels()

    // private var scientificMode = TunerMode.Unknown

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val nightMode = when (sharedPreferences.getString("appearance", "auto")) {
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(nightMode)

        val screenOn = sharedPreferences.getBoolean("screenon", false)
        if(screenOn)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        if (savedInstanceState == null)
            loadSimpleOrScientificFragment()
        setDisplayHomeButton()

        supportFragmentManager.addFragmentOnAttachListener { fragmentManager, fragment ->
            setDisplayHomeButton()
        }

        supportFragmentManager.addOnBackStackChangedListener {
            setDisplayHomeButton()
            //if (supportFragmentManager.backStackEntryCount == 0)
            //    loadSimpleOrScientificFragment()
        }
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
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else if (!isCurrentFragmentATunerFragment()){
            loadSimpleOrScientificFragment()
        } else {
            super.onBackPressed()
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else if (!isCurrentFragmentATunerFragment()){
            loadSimpleOrScientificFragment()
        }
        return super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            // User chose the "Settings" item, show the app settings UI...
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace<SettingsFragment>(R.id.main_content)
                if (!isCurrentFragmentATunerFragment())
                    addToBackStack(null)
            }
            true
        }
//        R.id.action_instruments -> {
//            supportFragmentManager.commit {
//                setReorderingAllowed(true)
//                replace<InstrumentsFragment>(R.id.main_content)
//                addToBackStack(null)
//            }
//            loadInstrumentsFragment()
//            true
//        }
        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    fun loadTuningEditorFragment() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<TuningEditorFragment>(R.id.main_content)
            if (!isCurrentFragmentATunerFragment())
                addToBackStack(null)
        }
//        tuningEditorViewModel.clear(0)
//       val actionMode = startSupportActionMode(TuningEditorActionCallback(this, instrumentsViewModel, tuningEditorViewModel))
//        actionMode?.setTitle(R.string.edit_instrument)
    }

    fun loadInstrumentsFragment() {
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
}
