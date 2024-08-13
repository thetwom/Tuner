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
package de.moekadu.tuner.instruments

import android.content.Context
import android.content.Context.MODE_PRIVATE
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class InstrumentResourcesOld @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sharedPreferences = context.getSharedPreferences("instrument resources", MODE_PRIVATE)

//    /** Predefined instruments, this is simply and no database, since it never changes. */
    private val predefinedInstruments: List<Instrument> = instrumentDatabase


    val predefinedInstrumentsExpanded get() = getBoolean(PREDEFINED_SECTION_EXPANDED_KEY)
    val customInstrumentsExpanded get() = getBoolean(CUSTOM_SECTION_EXPANDED_KEY)

    val customInstruments get() = getString(CUSTOM_INSTRUMENTS_KEY)?.let {
        InstrumentIO.stringToInstruments(it).instruments
    }
    val currentInstrument get() = getLong(CURRENT_INSTRUMENT_ID_KEY)?.let { key ->
        customInstruments?.let { instruments ->
            instruments.firstOrNull{ it.stableId == key }
        } ?: predefinedInstruments.firstOrNull { it.stableId == key }
    }

    private fun getBoolean(key: String) = if(sharedPreferences.contains(key))
        sharedPreferences.getBoolean(key, false)
    else
        null

    private fun getLong(key: String) = if(sharedPreferences.contains(key))
        sharedPreferences.getLong(key, 0L)
    else
        null

    private fun getString(key: String) = if(sharedPreferences.contains(key))
        sharedPreferences.getString(key, null)
    else
        null

    companion object {
        const val CUSTOM_SECTION_EXPANDED_KEY = "custom_section_expanded"
        const val PREDEFINED_SECTION_EXPANDED_KEY = "predefined_section_expanded"

        const val CURRENT_INSTRUMENT_ID_KEY = "instrument_id"
        const val SECTION_OF_CURRENT_INSTRUMENT_KEY = "instrument_section"

        const val CUSTOM_INSTRUMENTS_KEY = "custom_instruments"
    }
}