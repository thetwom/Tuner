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
package de.moekadu.tuner.preferences

import android.content.Context
import android.util.Log
import kotlin.math.roundToInt

fun PreferenceResources.migrateFromV6(context: Context): Boolean {
//    Log.v("Tuner", "PreferenceMigrations: complete = ${migrationsFromV6Complete.value}")
    if (migrationsFromV6Complete.value) {
//        Log.v("Tuner", "PreferenceMigrations: Do not migrate, since already done")
        return false
    }
//    Log.v("Tuner", "PreferenceMigrations: Migrating preferences from v6")
    val from = PreferenceResourcesOld(context)
    from.appearance?.let {
        writeAppearance(
            PreferenceResources.Appearance(
                it.mode,
                it.blackNightEnabled,
                it.useSystemColorAccents
            )
        )
    }
    from.screenAlwaysOn?.let { writeScientificMode(it) }
    from.screenAlwaysOn?.let { writeScreenAlwaysOn(it) }
    from.notePrintOptions?.let { writeNotePrintOptions(it) }
    from.windowing?.let { writeWindowing(it) }
    from.overlap?.let { writeOverlap(it) }
    from.windowSizeExponent?.let { writeWindowSize(it) }
    from.pitchHistoryDuration?.let {
        writePitchHistoryDuration(((it / 0.25f).roundToInt() * 0.25f).coerceIn(0.25f, 10f))
    }
    from.pitchHistoryNumFaultyValues?.let { writePitchHistoryNumFaultyValues(it) }
    from.numMovingAverage?.let { writeNumMovingAverage(it) }
    from.sensitivity?.let { writeSensitivity(it) }
    from.toleranceInCents?.let { writeToleranceInCents(it) }
    from.waveWriterDurationInSeconds?.let { writeWaveWriterDurationInSeconds(it) }
    from.musicalScale?.let { writeMusicalScaleProperties(
        PreferenceResources.MusicalScaleProperties.create(it)
    ) }
    writeMigrationsFromV6Complete()
    return true
}