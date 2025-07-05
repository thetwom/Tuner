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

import android.util.Log
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import de.moekadu.tuner.hilt.ApplicationScope
import de.moekadu.tuner.misc.ResourcesDataStoreBase
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class InstrumentResources @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope val applicationScope: CoroutineScope
) {
    val store = ResourcesDataStoreBase(context, "instruments")

    val predefinedInstruments = instrumentDatabase.toImmutableList()

//    val currentInstrument = store.getSerializablePreferenceFlow(
//        CURRENT_INSTRUMENT_KEY, predefinedInstruments[0]
//    )

    val currentInstrument = store.getTransformablePreferenceFlow(
        CURRENT_INSTRUMENT_KEY, predefinedInstruments[0]
    ) {
        try {
            val instrument = Json.decodeFromString<Instrument>(it)
            if (instrument.isPredefined()) {
                reloadPredefinedInstrumentIfNeeded(instrument, predefinedInstruments)
            } else {
                instrument
            }
        } catch(ex: IllegalArgumentException) {
            try {
                Json.decodeFromString<InstrumentOld>(it).toNew()
            } catch (ex2: Exception ){
                predefinedInstruments[0]
            }
        } catch(ex: Exception) {
            instrumentDatabase[0]
        }
    }

    fun writeCurrentInstrument(instrument: Instrument) {
        applicationScope.launch {
            store.writeSerializablePreference(CURRENT_INSTRUMENT_KEY, instrument)
        }
    }

    // Reading custom instruments is currently a bit difficult, since serialization
    // of persistentLists is not directly possible
    //    val customInstruments = store.getSerializablePreferenceFlow(
    //        CUSTOM_INSTRUMENTS_KEY, CustomInstrumentsDefault
    //    )
    val customInstruments = store.getTransformablePreferenceFlow(
        CUSTOM_INSTRUMENTS_KEY, CustomInstrumentsDefault
    ) {
        try {
            Json.decodeFromString<Array<Instrument>>(it).toList().toPersistentList()

        } catch(ex: Exception) {
            CustomInstrumentsDefault
        }
    }
//
//    suspend fun writeCustomInstruments(instruments: ImmutableList<Instrument>) {
//        store.writeSerializablePreference(CUSTOM_INSTRUMENTS_KEY, ImmutableListWrapper(instruments))
//    }
    fun writeCustomInstruments(instruments: List<Instrument>) {
        // if current instrument did change, update this also
        val currentInstrumentId = currentInstrument.value.stableId
        val modifiedCurrentInstrument = instruments.firstOrNull {
            it.stableId == currentInstrumentId
        }
        if (modifiedCurrentInstrument != null)
            writeCurrentInstrument(modifiedCurrentInstrument)
        applicationScope.launch {
            store.writeSerializablePreference(CUSTOM_INSTRUMENTS_KEY, instruments.toTypedArray())
        }
    }

    val customInstrumentsExpanded = store.getPreferenceFlow(
        CUSTOM_INSTRUMENTS_EXPANDED_KEY, CustomInstrumentExpandedDefault
    )
    fun writeCustomInstrumentsExpanded(expanded: Boolean) {
        applicationScope.launch {
            store.writePreference(CUSTOM_INSTRUMENTS_EXPANDED_KEY, expanded)
        }
    }

    val predefinedInstrumentsExpanded = store.getPreferenceFlow(
        PREDEFINED_INSTRUMENTS_EXPANDED_KEY, PredefinedInstrumentExpandedDefault
    )
    fun writePredefinedInstrumentsExpanded(expanded: Boolean) {
        applicationScope.launch {
            store.writePreference(PREDEFINED_INSTRUMENTS_EXPANDED_KEY, expanded)
        }
    }

    /** Add instrument if stable id does not exist, else replace it.*/
    fun addNewOrReplaceInstrument(instrument: Instrument) {
        val newInstrument = if (instrument.stableId == Instrument.NO_STABLE_ID)
            instrument.copy(stableId = getNewStableId())
        else
            instrument

        val oldInstruments = customInstruments.value
        val newInstruments = oldInstruments.mutate { mutated ->
            val index = oldInstruments.indexOfFirst { it.stableId == instrument.stableId }
            if (index >= 0)
                mutated[index] = newInstrument
            else
                mutated.add(newInstrument)
        }
        writeCustomInstruments(newInstruments)
    }

    fun appendInstruments(instruments: List<Instrument>) {
        val current = this.customInstruments.value
        val newInstrumentList = current.mutate { modified ->
            instruments.forEach {
                val newKey = getNewStableId(modified)
                modified.add(it.copy(stableId = newKey))
            }
        }
        writeCustomInstruments(newInstrumentList)
    }

//    fun prependInstruments(instruments: List<Instrument>) {
//        val current = this.customInstruments.value
//        val newInstrumentList = current.mutate { modified ->
//            instruments.forEachIndexed { index, instrument ->
//                val newKey = getNewStableId(modified)
//                modified.add(index, instrument.copy(stableId = newKey))
//            }
//        }
//        writeCustomInstruments(newInstrumentList)
//    }

//    fun replaceInstruments(instruments: List<Instrument>) {
//        var key = 0L
//        val currentKey = currentInstrument.value.stableId
//        val newInstrumentList = instruments.map {
//            ++key
//            if (key == currentKey)
//                ++key
//            it.copy(stableId = key)
//        }
//
//        writeCustomInstruments(newInstrumentList)
//    }

    private fun getNewStableId(existingInstruments: List<Instrument> = customInstruments.value): Long {
        val currentKey = currentInstrument.value.stableId
        while (true) {
            val stableId = Random.nextLong(0, Long.MAX_VALUE - 1)
            if ((currentKey != stableId) && (existingInstruments.firstOrNull {it.stableId == stableId} == null))
                return stableId
        }
    }

    companion object {
        private val CustomInstrumentsDefault = persistentListOf<Instrument>()
        private const val CustomInstrumentExpandedDefault = true
        private const val PredefinedInstrumentExpandedDefault = true

        private val CURRENT_INSTRUMENT_KEY = stringPreferencesKey("current instrument")
        private val CUSTOM_INSTRUMENTS_KEY = stringPreferencesKey("custom instruments")
        private val CUSTOM_INSTRUMENTS_EXPANDED_KEY = booleanPreferencesKey(
            "custom instruments expanded"
        )

        private val PREDEFINED_INSTRUMENTS_EXPANDED_KEY = booleanPreferencesKey(
            "predefined instruments expanded"
        )
    }
}

private fun reloadPredefinedInstrumentIfNeeded(
    instrument: Instrument,
    predefinedInstruments: List<Instrument>
): Instrument {
    // if null, we get the string, not the id-based string, for predefined instruments, this
    // string is a unique identifier.
    val name = instrument.getNameString(null)
    return if (name == "") {
        instrument
    } else {
        predefinedInstruments.firstOrNull {
            it.getNameString(null) == name
        } ?: instrument
    }
}
