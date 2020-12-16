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

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    private var text: TextView? = null
    private var text2: TextView? = null
    private var counter = 0
    var viewModel: TunerViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val nightMode = when (sharedPreferences.getString("appearance", "auto")) {
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(nightMode)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        if (savedInstanceState == null)
            supportFragmentManager.beginTransaction().replace(R.id.main_content, TunerFragment())
                .commit()

        setDisplayHomeButton()
        supportFragmentManager.addOnBackStackChangedListener { setDisplayHomeButton() }


//        viewModel = ViewModelProvider(this).get(TunerViewModel::class.java)
//
//        viewModel?.preprocessingResults?.observe(this) {
//            if (it.specMaximaIndices?.size ?: 0 > 0 && it.correlationMaximaIndices?.size ?: 0 > 0) {
//                val frequencyIndex = it.specMaximaIndices?.get(0)
//                val correlationIndex = it.correlationMaximaIndices?.get(0)
//                if (frequencyIndex != null && correlationIndex != null) {
//                    val freqSpectrum = it.frequencyFromSpectrum(frequencyIndex)
//                    val freqCorrelation = it.frequencyFromCorrelation(correlationIndex)
//                    text?.text = "$counter: $freqSpectrum, $freqCorrelation"
//                    counter++
//                }
//            }
//        }
//
//        viewModel?.postprocessingResults?.observe(this) {
//            text2?.text = "${it.frequency} Hz"
//        }

    }

    override fun onStart() {
        super.onStart()
        askForPermissionAndNotifyViewModel.launch(Manifest.permission.RECORD_AUDIO)
    }

    override fun onStop() {
        // TODO: this should go the the tuner fragment, same for the permission asking
        viewModel?.stopSampling()
        super.onStop()
    }
    /// Instance for requesting audio recording permission.
    /**
     * This will create the sourceJob as soon as the permissions are granted.
     */
    private val askForPermissionAndNotifyViewModel = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        if (result) {
            viewModel?.startSampling()
        } else {
            // TODO: use string resource
            Toast.makeText(this, "No audio recording permission is granted", Toast.LENGTH_LONG)
                .show()
            Log.v(
                "TestRecordFlow",
                "SoundSource.onRequestPermissionsResult: No audio recording permission is granted."
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        supportFragmentManager.popBackStack()
        return super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            // User chose the "Settings" item, show the app settings UI...
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_content, SettingsFragment())
                .addToBackStack("blub")
                .commit();
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
}
