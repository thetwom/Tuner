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

import androidx.annotation.DrawableRes
import de.moekadu.tuner.R

enum class InstrumentIcon(
    @DrawableRes val resourceId: Int
){
    guitar(R.drawable.ic_guitar),
    ukulele(R.drawable.ic_ukulele),
    eguitar(R.drawable.ic_eguitar),
    bass(R.drawable.ic_bass),
    violin(R.drawable.ic_violin),
    cello(R.drawable.ic_cello),
    double_bass(R.drawable.ic_double_bass),
    trumpet(R.drawable.ic_trumpet),
    saxophone(R.drawable.ic_saxophone),
    flute(R.drawable.ic_flute),
    harp(R.drawable.ic_harp),
    tar(R.drawable.ic_tar),
    setar(R.drawable.ic_setar),
    kamancheh(R.drawable.ic_kamancheh),
    piano(R.drawable.ic_piano)
}

fun String.toInstrumentIcon(): InstrumentIcon {
    val s = this.replace(" ", "_")
    return try {
        InstrumentIcon.valueOf(s)
    } catch (ex: IllegalArgumentException) {
        InstrumentIcon.entries[0]
    }
}
