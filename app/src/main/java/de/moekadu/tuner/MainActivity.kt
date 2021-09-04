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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {
    // TODO: Allow setting minimum and maximum allowed note
    // ... more settings possible?
    // TODO: Allow setting the tolerance in the settings

    private var scientificMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val nightMode = when (sharedPreferences.getString("appearance", "auto")) {
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        scientificMode = sharedPreferences.getBoolean("scientific_mode", false)
        AppCompatDelegate.setDefaultNightMode(nightMode)

        val screenOn = sharedPreferences.getBoolean("screenon", false)
        if(screenOn)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        if (savedInstanceState == null) {
            if (scientificMode) {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace<TunerFragment>(R.id.main_content)
                }
            } else {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace<TunerFragmentSimple>(R.id.main_content)
                }
            }
        }

        setDisplayHomeButton()
        supportFragmentManager.addOnBackStackChangedListener { setDisplayHomeButton() }
    }

    override fun onSupportNavigateUp(): Boolean {
        supportFragmentManager.popBackStack()
        return super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val scientificIcon = menu?.findItem(R.id.scientific_mode)
        scientificIcon?.setIcon(if (scientificMode) R.drawable.ic_developer_on else R.drawable.ic_developer_off)
        return super.onPrepareOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            // User chose the "Settings" item, show the app settings UI...
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace<SettingsFragment>(R.id.main_content)
                addToBackStack(null)
            }
            true
        }
        R.id.scientific_mode -> {
            toggleScientificMode()
            true
        }
        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    private fun setDisplayHomeButton() {
        val showDisplayHomeButton = supportFragmentManager.backStackEntryCount > 0
        supportActionBar?.setDisplayHomeAsUpEnabled(showDisplayHomeButton)
    }

    private fun toggleScientificMode() {
        scientificMode = !scientificMode
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.edit().putBoolean("scientific_mode", scientificMode).apply()

        if (supportFragmentManager.backStackEntryCount > 0)
            supportFragmentManager.popBackStack()

        if (scientificMode) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace<TunerFragment>(R.id.main_content)
            }
        } else {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace<TunerFragmentSimple>(R.id.main_content)
            }
        }
        invalidateOptionsMenu()
    }
}
