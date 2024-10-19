package de.moekadu.tuner.temperaments2

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import de.moekadu.tuner.hilt.ApplicationScope
import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.misc.ResourcesDataStoreBase
import de.moekadu.tuner.temperaments.MusicalNote
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class TemperamentResources @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope val applicationScope: CoroutineScope
){
    @Serializable
    data class TemperamentWithNoteNames(
        val temperament: Temperament,
        val noteNames: NoteNames?
    ) {
        val stableId get() = temperament.stableId
        fun clone(newStableId: Long): TemperamentWithNoteNames {
            return TemperamentWithNoteNames(
                temperament = temperament.copy(stableId = newStableId),
                noteNames = noteNames?.copy(stableId = newStableId),
            )
        }
    }

    val store = ResourcesDataStoreBase(context, "temperaments")

    val predefinedTemperaments = temperamentDatabase
        .map { TemperamentWithNoteNames(it, null) }
        .toImmutableList()
    val defaultTemperament = predefinedTemperaments.first {
        it.temperament.equalOctaveDivision == 12
    }

    val customTemperaments = store.getTransformablePreferenceFlow(
        CUSTOM_TEMPERAMENTS_KEY, CustomTemperamentsDefault
    ) {
        try {
            Json.decodeFromString<Array<TemperamentWithNoteNames>>(it).toList().toPersistentList()
        } catch(ex: Exception) {
            CustomTemperamentsDefault
        }
    }

    val musicalScale = store.getSerializablePreferenceFlow(
        MUSICAL_SCALE_KEY,
        MusicalScale2Factory.create(
            defaultTemperament.temperament,
            noteNames = getSuitableNoteNames(defaultTemperament.temperament.numberOfNotesPerOctave),
            referenceNote = null,
            rootNote = null,
            referenceFrequency = DefaultValues.REFERENCE_FREQUENCY,
            frequencyMin = DefaultValues.FREQUENCY_MIN,
            frequencyMax = DefaultValues.FREQUENCY_MAX,
            stretchTuning = StretchTuning()
        )
    )

    val customTemperamentsExpanded = store.getPreferenceFlow(
        CUSTOM_TEMPERAMENTS_EXPANDED_KEY, CustomTemperamentsExpandedDefault
    )
    fun writeCustomTemperamentsExpanded(expanded: Boolean) {
        applicationScope.launch {
            store.writePreference(CUSTOM_TEMPERAMENTS_EXPANDED_KEY, expanded)
        }
    }

    val predefinedTemperamentsExpanded = store.getPreferenceFlow(
        PREDEFINED_TEMPERAMENTS_EXPANDED_KEY, PredefinedTemperamentsExpandedDefault
    )
    fun writePredefinedTemperamentsExpanded(expanded: Boolean) {
        applicationScope.launch {
            store.writePreference(PREDEFINED_TEMPERAMENTS_EXPANDED_KEY, expanded)
        }
    }

    fun writeMusicalScale(musicalScale: MusicalScale2) {
        applicationScope.launch {
            store.writeSerializablePreference(MUSICAL_SCALE_KEY, musicalScale)
        }
    }

    fun writeMusicalScale(
        temperament: TemperamentWithNoteNames? = null,
        referenceNote: MusicalNote? = null,
        rootNote: MusicalNote? = null,
        referenceFrequency: Float? = null,
        stretchTuning: StretchTuning? = null
    ) {
        val currentMusicalScale = musicalScale.value
        val temperamentResolved = temperament?.temperament ?: currentMusicalScale.temperament
        val noteNamesResolved = if (temperament?.noteNames != null) {
            temperament.noteNames
        } else if (currentMusicalScale.numberOfNotesPerOctave == temperamentResolved.numberOfNotesPerOctave) {
            currentMusicalScale.noteNames
        } else {
            getSuitableNoteNames(temperamentResolved.numberOfNotesPerOctave)
        }
        val referenceNoteResolved = if (referenceNote != null) {
            referenceNote
        } else if (noteNamesResolved.hasNote(currentMusicalScale.referenceNote)) {
            currentMusicalScale.referenceNote
        } else {
            null
        }
        val rootNoteResolved = if (rootNote != null) {
            rootNote
        } else if (noteNamesResolved.hasNote(currentMusicalScale.rootNote)) {
            currentMusicalScale.rootNote
        } else {
            null
        }

        val newMusicalScale = MusicalScale2Factory.create(
            temperamentResolved,
            noteNamesResolved,
            referenceNoteResolved,
            rootNoteResolved,
            referenceFrequency ?: currentMusicalScale.referenceFrequency,
            currentMusicalScale.frequencyMin,
            currentMusicalScale.frequencyMax,
            stretchTuning ?: currentMusicalScale.stretchTuning
        )
        applicationScope.launch {
            store.writeSerializablePreference(MUSICAL_SCALE_KEY, newMusicalScale)
        }
    }

    fun writeCustomTemperaments(temperaments: List<TemperamentWithNoteNames>) {
        val currentMusicalScale = musicalScale.value
        // if current temperament did change, update this also
        val currentTemperamentId = currentMusicalScale.temperament.stableId
        val modifiedCurrentTemperament = temperaments.firstOrNull {
            it.stableId == currentTemperamentId
        }
        if (modifiedCurrentTemperament != null) {
            writeMusicalScale(modifiedCurrentTemperament)
        }

        applicationScope.launch {
            store.writeSerializablePreference(CUSTOM_TEMPERAMENTS_KEY, temperaments.toTypedArray())
        }
    }

    /** Add temperament if stable id does not exist, else replace it.*/
    fun addNewOrReplaceTemperament(temperament: TemperamentWithNoteNames) {
        val newTemperament = if (temperament.stableId == Temperament.NO_STABLE_ID) {
            temperament.clone(getNewStableId())
        } else {
            temperament
        }

        val oldTemperaments = customTemperaments.value
        val newTemperaments = oldTemperaments.mutate { mutated ->
            val index = oldTemperaments.indexOfFirst { it.stableId == temperament.stableId }
            if (index >= 0)
                mutated[index] = newTemperament
            else
                mutated.add(newTemperament)
        }
        writeCustomTemperaments(newTemperaments)
    }

    fun appendTemperaments(temperaments: List<TemperamentWithNoteNames>) {
        val current = this.customTemperaments.value
        val newTemperamentsList = current.mutate { modified ->
            temperaments.forEach {
                val newKey = getNewStableId(modified)
                modified.add(it.clone(newKey))
            }
        }
        writeCustomTemperaments(newTemperamentsList)
    }

    fun prependTemperaments(temperaments: List<TemperamentWithNoteNames>) {
        val current = this.customTemperaments.value
        val newTemperamentsList = current.mutate { modified ->
            temperaments.forEachIndexed { index, temperament ->
                val newKey = getNewStableId(modified)
                modified.add(index, temperament.clone(newKey))
            }
        }
        writeCustomTemperaments(newTemperamentsList)
    }

    fun replaceTemperaments(temperaments: List<TemperamentWithNoteNames>) {
        var key = 0L
        val currentKey = musicalScale.value.temperament.stableId
        val newTemperamentsList = temperaments.map {
            ++key
            if (key == currentKey)
                ++key
            it.clone(key)
        }
        writeCustomTemperaments(newTemperamentsList)
    }

    private fun getNewStableId(
        existingTemperaments: List<TemperamentWithNoteNames> = customTemperaments.value
    ): Long {
        val currentKey = musicalScale.value.temperament.stableId
        while (true) {
            val stableId = Random.nextLong(0, Long.MAX_VALUE - 1)
            if ((currentKey != stableId) && (existingTemperaments.firstOrNull {it.stableId == stableId} == null))
                return stableId
        }
    }

    companion object {
        private val CustomTemperamentsDefault = persistentListOf<TemperamentWithNoteNames>()
        private const val CustomTemperamentsExpandedDefault = true
        private const val PredefinedTemperamentsExpandedDefault = true

        private val MUSICAL_SCALE_KEY= stringPreferencesKey("musical scale")
        private val CUSTOM_TEMPERAMENTS_KEY = stringPreferencesKey("custom temperaments")

        private val CUSTOM_TEMPERAMENTS_EXPANDED_KEY = booleanPreferencesKey(
            "custom temperaments expanded"
        )

        private val PREDEFINED_TEMPERAMENTS_EXPANDED_KEY = booleanPreferencesKey(
            "predefined temperaments expanded"
        )
    }
}