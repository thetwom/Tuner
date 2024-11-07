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
package de.moekadu.tuner.misc

import android.content.Context
import android.util.Log
import androidx.datastore.core.IOException
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ResourcesDataStoreBase(
    @ApplicationContext context: Context,
    filename: String
) {
    val dataStore = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() })
    ) { context.preferencesDataStoreFile(filename) }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun<T> getPreferenceFlow(key: Preferences.Key<T>, default: T): StateFlow<T> {
        return dataStore.data
            .catch {
//                Log.v("Tuner", "PreferenceResources2: except: $it, $key")
                if (it is IOException) {
                    emit(emptyPreferences())
                }else {
                    throw it
                }
            }
            .map {
//                Log.v("Tuner", "PreferenceRessources2: $key ${it[key]}")
                it[key] ?: default
            }
            .stateIn(scope, SharingStarted.Eagerly, default)
    }
    fun<K, T> getTransformablePreferenceFlow(key: Preferences.Key<K>, default: T, transform: (K) -> T): StateFlow<T> {
        return dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { it[key] }
            .distinctUntilChanged()
            .map {
                if (it == null) default else transform(it)
            }
            .stateIn(scope, SharingStarted.Eagerly, default)
    }
    inline fun<reified T> getSerializablePreferenceFlow(key: Preferences.Key<String>, default: T): StateFlow<T> {
        return dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { it[key] }
            .distinctUntilChanged()
            .map {
                if (it == null) {
                    default
                } else {
                    try {
                        Json.decodeFromString<T>(it)
                    } catch(ex: Exception) {
                        default
                    }
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, default)
    }

    suspend fun<T> writePreference(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    suspend inline fun<reified T> writeSerializablePreference(key: Preferences.Key<String>, value: T) {
        dataStore.edit {
            it[key] = Json.encodeToString(value)
        }
    }

    init {
        // block everything until, all data is read to avoid incorrect startup behaviour
        runBlocking {
            dataStore.data.first()
        }
    }

}