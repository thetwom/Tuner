package de.moekadu.tuner.preferences

import android.content.Context
import android.util.Log
import kotlin.math.roundToInt

fun PreferenceResources2.migrateFromV6(context: Context): Boolean {
    Log.v("Tuner", "PreferenceMigrations: complete = ${migrationsFromV6Complete.value}")
    if (migrationsFromV6Complete.value) {
        Log.v("Tuner", "PreferenceMigrations: Do not migrate, since already done")
        return false
    }
    Log.v("Tuner", "PreferenceMigrations: Migrating preferences from v6")
    val from = PreferenceResources(context)
    from.appearance?.let {
        writeAppearance(
            PreferenceResources2.Appearance(
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
        PreferenceResources2.MusicalScaleProperties.create(it)
    ) }
    writeMigrationsFromV6Complete()
    return true
}