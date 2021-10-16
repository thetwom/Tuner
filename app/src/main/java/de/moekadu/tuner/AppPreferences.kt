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

import android.content.Context
import androidx.fragment.app.FragmentActivity

class AppPreferences {
    companion object {
        private fun readPreferenceString(key: String, activity: FragmentActivity): String? {
            val preferences = activity.getPreferences(Context.MODE_PRIVATE)
            return preferences.getString(key, null)
        }

        private fun writePreferenceString(key: String, value: String, activity: FragmentActivity) {
            val preferences = activity.getPreferences(Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString(key, value)
            editor.apply()
        }

        private fun readPreferenceFloat(key: String, default: Float, activity: FragmentActivity): Float {
            val preferences = activity.getPreferences(Context.MODE_PRIVATE)
            return preferences.getFloat(key, default)
        }
        private fun writePreferenceFloat(key: String, value: Float, activity: FragmentActivity) {
            val preferences = activity.getPreferences(Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putFloat(key, value)
            editor.apply()
        }

        private fun readPreferenceLong(key: String, default: Long, activity: FragmentActivity): Long {
            val preferences = activity.getPreferences(Context.MODE_PRIVATE)
            return preferences.getLong(key, default)
        }
        private fun writePreferenceLong(key: String, value: Long, activity: FragmentActivity) {
            val preferences = activity.getPreferences(Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putLong(key, value)
            editor.apply()
        }

        private fun readPreferenceBoolean(key: String, default: Boolean, activity: FragmentActivity): Boolean {
            val preferences = activity.getPreferences(Context.MODE_PRIVATE)
            return preferences.getBoolean(key, default)
        }
        private fun writePreferenceBoolean(key: String, value: Boolean, activity: FragmentActivity) {
            val preferences = activity.getPreferences(Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putBoolean(key, value)
            editor.apply()
        }

        fun readInstrumentId(activity: FragmentActivity): Long {
            return readPreferenceLong("instrument_id", 0, activity)
        }
        fun writeInstrumentId(stableId: Long, activity: FragmentActivity) {
            writePreferenceLong("instrument_id", stableId, activity)
        }

        fun writeTunerPreferences(instrumentId: Long?, activity: FragmentActivity) {
            if (instrumentId != null)
                writeInstrumentId(instrumentId, activity)
        }
    }
}