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
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.preferences, rootKey)
    setHasOptionsMenu(true)
  }

  override fun onPrepareOptionsMenu(menu : Menu) {
//        super.onPrepareOptionsMenu(menu)
    menu.findItem(R.id.action_settings)?.isVisible = false
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

    val appearance = findPreference("appearance") as ListPreference?
      ?: throw RuntimeException("No appearance preference")

    appearance.summaryProvider =
      Preference.SummaryProvider<ListPreference> { preference ->
        when (preference?.value) {
          "dark" -> getString(R.string.dark_appearance)
          "light" -> getString(R.string.light_appearance)
          else -> getString(R.string.system_appearance)
        }
      }

    appearance.setOnPreferenceChangeListener { _, _ ->
      val act = activity as MainActivity?
      act?.recreate()
      true
    }

    val windowingFunction = findPreference("windowing") as ListPreference?
    windowingFunction?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

    return super.onCreateView(inflater, container, savedInstanceState)
  }
}