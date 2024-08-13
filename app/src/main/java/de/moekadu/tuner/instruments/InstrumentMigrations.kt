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

fun InstrumentResources.migratingFromV6(context: Context) {
    val resources = InstrumentResourcesOld(context)
    resources.customInstrumentsExpanded?.let {
        this.writeCustomInstrumentsExpanded(it)
    }
    resources.predefinedInstrumentsExpanded?.let {
        this.writePredefinedInstrumentsExpanded(it)
    }
    resources.customInstruments?.let {
        this.writeCustomInstruments(it)
    }
    resources.currentInstrument?.let {
        this.writeCurrentInstrument(it)
    }
}