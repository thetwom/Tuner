package de.moekadu.tuner.instruments

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import de.moekadu.tuner.misc.ResourcesDataStoreBase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class InstrumentResources2 @Inject constructor(
    @ApplicationContext context: Context
) {
    val store = ResourcesDataStoreBase(context, "instruments")

    val predefinedInstruments = instrumentDatabase.toImmutableList()

    val currentInstrument = store.getSerializablePreferenceFlow(
        CURRENT_INSTRUMENT_KEY, predefinedInstruments[0]
    )
    suspend fun writeCurrentInstrument(instrument: Instrument) {
        store.writeSerializablePreference(CURRENT_INSTRUMENT_KEY, instrument)
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
    suspend fun writeCustomInstruments(instruments: List<Instrument>) {
        // if current instrument did change, update this also
        val currentInstrumentId = currentInstrument.value.stableId
        val modifiedCurrentInstrument = instruments.firstOrNull {
            it.stableId == currentInstrumentId
        }
        if (modifiedCurrentInstrument != null)
            writeCurrentInstrument(modifiedCurrentInstrument)

        store.writeSerializablePreference(CUSTOM_INSTRUMENTS_KEY, instruments.toTypedArray())
    }

    val customInstrumentsExpanded = store.getPreferenceFlow(
        CUSTOM_INSTRUMENTS_EXPANDED_KEY, CustomInstrumentExpandedDefault
    )
    suspend fun writeCustomInstrumentsExpanded(expanded: Boolean) {
        store.writePreference(CUSTOM_INSTRUMENTS_EXPANDED_KEY, expanded)
    }

    val predefinedInstrumentsExpanded = store.getPreferenceFlow(
        PREDEFINED_INSTRUMENTS_EXPANDED_KEY, PredefinedInstrumentExpandedDefault
    )
    suspend fun writePredefinedInstrumentsExpanded(expanded: Boolean) {
        store.writePreference(PREDEFINED_INSTRUMENTS_EXPANDED_KEY, expanded)
    }

    /** Add instrument if stable id does not exist, else replace it.*/
    suspend fun addNewOrReplaceInstrument(instrument: Instrument) {
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

    suspend fun appendInstruments(instruments: List<Instrument>) {
        val current = this.customInstruments.value
        val newInstrumentList = current.mutate { modified ->
            instruments.forEach {
                val newKey = getNewStableId(modified)
                modified.add(it.copy(stableId = newKey))
            }
        }
        writeCustomInstruments(newInstrumentList)
    }

    suspend fun prependInstruments(instruments: List<Instrument>) {
        val current = this.customInstruments.value
        val newInstrumentList = current.mutate { modified ->
            instruments.forEachIndexed { index, instrument ->
                val newKey = getNewStableId(modified)
                modified.add(index, instrument.copy(stableId = newKey))
            }
        }
        writeCustomInstruments(newInstrumentList)
    }

    suspend fun replaceInstruments(instruments: List<Instrument>) {
        var key = 0L
        val currentKey = currentInstrument.value.stableId
        val newInstrumentList = instruments.map {
            ++key
            if (key == currentKey)
                ++key
            it.copy(stableId = key)
        }

        writeCustomInstruments(newInstrumentList)
    }

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