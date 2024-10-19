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
package de.moekadu.tuner.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.temperaments2.TemperamentResources
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    val pref: PreferenceResources,
    val temperaments: TemperamentResources
) : ViewModel() {
    val musicalScale get() = temperaments.musicalScale
}