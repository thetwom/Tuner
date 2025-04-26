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
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import de.moekadu.tuner.misc.GetTextFromResId
import de.moekadu.tuner.notedetection.WindowingFunction
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.temperaments.TemperamentTypeOld
import de.moekadu.tuner.temperaments.resourceId
import de.moekadu.tuner.temperaments.TemperamentWithNoteNames2
import de.moekadu.tuner.temperaments.temperamentDatabase
import de.moekadu.tuner.ui.notes.NotationType
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.NotePrintOptionsOld
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.roundToInt

@Singleton
class PreferenceResourcesOld @Inject constructor (
    @ApplicationContext private val context: Context,
) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    data class Appearance(
        var mode: NightMode = NightMode.Auto,  // Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        var blackNightEnabled: Boolean = true,
        var useSystemColorAccents: Boolean = true
    ) {
        private fun nightModeStringToID2(string: String) = when(string) {
            "dark" -> NightMode.On
            "light" -> NightMode.Off
            else -> NightMode.Auto
        }

        fun fromString(string: String?) {
            if (string == null)
                return
            val values = string.split(" ")
            blackNightEnabled = values.contains("blackNightEnabled")
            useSystemColorAccents = !(values.contains("noSystemColorAccents"))
            val modeString = if (values.contains("dark"))
                "dark"
            else if (values.contains("light"))
                "light"
            else
                "auto"
            mode = nightModeStringToID2(modeString)
        }
    }

    data class NotationPreferenceValue(var notation: String, var helmholtzEnabled: Boolean) {
        override fun toString(): String {
            var result = notation
            if (helmholtzEnabled)
                result += " helmholtzEnabled"
            return result
        }

        fun fromString(string: String?) {
            if (string == null)
                return
            val values = string.split(" ")
            helmholtzEnabled = values.contains("helmholtzEnabled")
            notation = when {
                values.contains("solfege") -> "solfege"
                values.contains("international") -> "international"
                values.contains("carnatic") -> "carnatic"
                values.contains("hindustani") -> "hindustani"
                else -> "standard"
            }
        }
    }

    val appearance get() = getString(APPEARANCE_KEY)?.let{
        val value = Appearance()
        value.fromString(sharedPreferences.getString(APPEARANCE_KEY, ""))
        value
    }

    val scientificMode get() = getBoolean(SCIENTIFIC_KEY)

    val screenAlwaysOn get() = getBoolean(SCREEN_ALWAYS_ON)
    private val preferFlat get() = getBoolean(PREFER_FLAT_KEY)
    private val notationPreference get() =  getString(NOTATION_KEY)?.let {
        val notationValue = NotationPreferenceValue("standard", false)
        notationValue.fromString(sharedPreferences.getString(NOTATION_KEY, ""))
        notationValue
    }
    val notePrintOptions: NotePrintOptions? get() {
        if (preferFlat == null && notationPreference == null)
            return null
        val pF = preferFlat
        val nP = notationPreference
        val def = NotePrintOptionsOld()
        val nT = when(nP?.notation) {
            "international" -> NotationType.International
            "solfege" -> NotationType.Solfege
            "carnatic" -> NotationType.Carnatic
            "hindustani" -> NotationType.Hindustani
            else -> NotationType.Standard
        }
        val hH = nP?.helmholtzEnabled ?: def.helmholtzNotation
        val sharpFlatPreference = when (pF) {
            null -> def.sharpFlatPreference
            true -> NotePrintOptionsOld.SharpFlatPreference.Flat
            else -> NotePrintOptionsOld.SharpFlatPreference.Sharp
        }
        return NotePrintOptions(
            sharpFlatPreference == NotePrintOptionsOld.SharpFlatPreference.Flat,
            hH,
            nT
        )
    }

    val windowing get() = getString(WINDOWING_KEY)?.let {
        when (sharedPreferences.getString(WINDOWING_KEY, null)) {
            "no_window" -> WindowingFunction.Tophat
            "window_hamming" -> WindowingFunction.Hamming
            "window_hann" -> WindowingFunction.Hann
            else -> null
        }
    }
    val overlap get() = getInt(OVERLAP_KEY)

    val windowSizeExponent get() = getInt(WINDOW_SIZE_KEY)?.let {
        it + 7
        //indexToWindowSize2(it)
    }
    val pitchHistoryDuration get() = getInt(PITCH_HISTORY_DURATION_KEY)?.let{
        percentToPitchHistoryDuration2(it)
    }
    val pitchHistoryNumFaultyValues get() = getInt(PITCH_HISTORY_NUM_FAULTY_VALUES_KEY)
    val numMovingAverage get() = getInt(NUM_MOVING_AVERAGE_KEY)

    // disable this setting for now by hardcoding a good value
    // fun obtainMaxNoise() = 0.1f// sharedPreferences.getInt(MAX_NOISE_KEY, 10) / 100f

    // disable this setting for now by hardcoding a good value
    // fun obtainMinHarmonicEnergyContent() = 0.1f // sharedPreferences.getInt(MIN_HARMONIC_ENERGY_CONTENT, 20) / 100f
    val sensitivity get() = getInt(SENSITIVITY)

    val toleranceInCents get() = getInt(TOLERANCE_IN_CENTS_KEY)?.let {
        indexToTolerance2(it)
    }

    val waveWriterDurationInSeconds get() = getInt(WAVE_WRITER_DURATION_IN_SECONDS_KEY)

    private val temperamentAndReferenceNote get() = TemperamentAndReferenceNoteFromPreference.fromSharedPreferences(sharedPreferences)

    val referenceNote get() = temperamentAndReferenceNote?.referenceNote
    val rootNote get() = temperamentAndReferenceNote?.rootNote
    val referenceFrequency get() = temperamentAndReferenceNote?.referenceFrequency
    val temperament: TemperamentWithNoteNames2?  get() {
        val temperamentType = temperamentAndReferenceNote?.temperamentType
        val rid = temperamentType?.resourceId()
        val t = temperamentDatabase.firstOrNull {
            if (it.name is GetTextFromResId) {
                it.name.id == rid
            } else {
                false
            }
        }
        return if (t == null)
            null
        else
            TemperamentWithNoteNames2(t, null)
    }

//    val musicalScale: MusicalScale? get() = temperamentAndReferenceNote?.let {
//            MusicalScaleFactory.create(
//                it.temperamentType,
//                it.referenceNote,
//                it.rootNote,
//                it.referenceFrequency.toFloatOrNull() ?: 440f
//            )
//        }

    private fun getBoolean(key: String) = if(sharedPreferences.contains(key))
        sharedPreferences.getBoolean(key, false)
    else
        null
    private fun getString(key: String) = if(sharedPreferences.contains(key))
        sharedPreferences.getString(key, null)
    else
        null
    private fun getInt(key: String) = if(sharedPreferences.contains(key))
        sharedPreferences.getInt(key, 0)
    else
        null

    companion object {
        const val APPEARANCE_KEY = "appearance"
        const val SCREEN_ALWAYS_ON = "screenon"
        const val PREFER_FLAT_KEY = "prefer_flat"
        const val NOTATION_KEY = "notation"
        const val WINDOWING_KEY = "windowing"
        const val OVERLAP_KEY = "overlap"
        const val WINDOW_SIZE_KEY = "window_size"
        const val SCIENTIFIC_KEY = "scientific"
        const val PITCH_HISTORY_DURATION_KEY = "pitch_history_duration"
        const val PITCH_HISTORY_NUM_FAULTY_VALUES_KEY = "pitch_history_num_faulty_values"
        const val NUM_MOVING_AVERAGE_KEY = "num_moving_average"
        const val SENSITIVITY = "sensitivity"
        const val TOLERANCE_IN_CENTS_KEY = "tolerance_in_cents"
        const val WAVE_WRITER_DURATION_IN_SECONDS_KEY = "wave_writer_duration_in_seconds"
    }
}

private data class TemperamentAndReferenceNoteFromPreference (
    val temperamentType: TemperamentTypeOld,
    val rootNote: MusicalNote,
    val referenceNote: MusicalNote,
    val referenceFrequency: String) {
    override fun toString(): String {
        return "$temperamentType ${rootNote.asString()} ${referenceNote.asString()} $referenceFrequency"
    }

    companion object {
        const val TEMPERAMENT_AND_REFERENCE_NOTE_PREFERENCE_KEY = "temperament_and_reference_note.key"

        fun fromSharedPreferences(pref: SharedPreferences): TemperamentAndReferenceNoteFromPreference? {
            if (!pref.contains(TEMPERAMENT_AND_REFERENCE_NOTE_PREFERENCE_KEY))
                return null
            return fromString(pref.getString(TEMPERAMENT_AND_REFERENCE_NOTE_PREFERENCE_KEY, null))
        }

        fun fromString(string: String?): TemperamentAndReferenceNoteFromPreference? {
            if (string == null)
                return null
            val values = string.split(" ")
            if (values.size != 4)
                return null
            val temperamentType = try {
                TemperamentTypeOld.valueOf(values[0])
            } catch (ex: IllegalArgumentException) {
                TemperamentTypeOld.EDO12
            }
            val rootNote = try {
                MusicalNote.fromString(values[1])
            } catch (ex: RuntimeException) {
                return null
            }
            val referenceNote = try {
                MusicalNote.fromString(values[2])
            } catch (ex: RuntimeException) {
                return null
            }
            val referenceFrequency = if (values[3].toFloatOrNull() == null)
                return null
            else
                values[3]
            return TemperamentAndReferenceNoteFromPreference(temperamentType, rootNote, referenceNote, referenceFrequency)
        }
    }
}

private fun indexToWindowSize2(index: Int): Int {
    return 2f.pow(7 + index).roundToInt()
}

private fun indexToTolerance2(index: Int): Int {
    return when (index) {
        0 -> 1
        1 -> 2
        2 -> 3
        3 -> 5
        4 -> 7
        5 -> 10
        6 -> 15
        7 -> 20
        else -> throw RuntimeException("Invalid index for tolerance")
    }
}

/** Compute pitch history duration in seconds based on a percent value.
 * This uses a exponential scale for setting the duration.
 * @param percent Percentage value where 0 stands for the minimum duration and 100 for the maximum.
 * @param durationAtFiftyPercent Duration in seconds at fifty percent.
 * @return Pitch history duration in seconds.
 */
private fun percentToPitchHistoryDuration2(percent: Int, durationAtFiftyPercent: Float = 3.0f) : Float {
    return durationAtFiftyPercent * 2.0f.pow(0.05f * (percent - 50))
}
