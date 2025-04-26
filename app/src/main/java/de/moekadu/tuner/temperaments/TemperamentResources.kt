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
package de.moekadu.tuner.temperaments

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import de.moekadu.tuner.hilt.ApplicationScope
import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.misc.ResourcesDataStoreBase
import de.moekadu.tuner.R
import de.moekadu.tuner.misc.GetTextFromResIdWithIntArg
import de.moekadu.tuner.musicalscale.MusicalScale
import de.moekadu.tuner.musicalscale.MusicalScale2
import de.moekadu.tuner.musicalscale.MusicalScaleFactory
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.generateNoteNames
import de.moekadu.tuner.stretchtuning.StretchTuning
import de.moekadu.tuner.ui.common.EditableListPredefinedSection
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class TemperamentResources @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope val applicationScope: CoroutineScope
){
    val store = ResourcesDataStoreBase(context, "temperaments")

    val predefinedTemperaments = temperamentDatabase
        .map { TemperamentWithNoteNames2(it, null) }
        .toImmutableList()

    val edoTemperamentsExpanded = store.getPreferenceFlow(
        EDO_TEMPERAMENTS_EXPANDED_KEY, EdoTemperamentsExpandedDefault
    )
    val edoTemperaments = object : EditableListPredefinedSection<TemperamentWithNoteNames2> {
        private val minEdo = 5
        private val maxEdo = 72
        private val minPredefinedKey = predefinedTemperaments.minOf { it.stableId }
        override val sectionStringResourceId = R.string.edo_temperaments
        override val size = maxEdo - minEdo + 1
        override fun get(index: Int): TemperamentWithNoteNames2 {
            val edo = minEdo + index
            return TemperamentWithNoteNames2(
                Temperament2(
                    name = GetTextFromResIdWithIntArg(R.string.equal_temperament_x, edo),
                    abbreviation = GetTextFromResIdWithIntArg(R.string.equal_temperament_x_abbr, edo),
                    description = GetTextFromResIdWithIntArg(R.string.equal_temperament_x_desc, edo),
                    edo,
                    minPredefinedKey - 1 - index
                ),
                null
            )
        }

        override val isExpanded get() = edoTemperamentsExpanded
        override val toggleExpanded: (isExpanded: Boolean) -> Unit = {
            writeEdoTemperamentsExpanded(it)
        }
    }

    val defaultTemperament = predefinedTemperaments.first {
        it.temperament.equalOctaveDivision == 12
    }

    val customTemperaments = store.getTransformablePreferenceFlow(
        CUSTOM_TEMPERAMENTS_KEY, CustomTemperamentsDefault
    ) {
        try {
            Json.decodeFromString<Array<TemperamentWithNoteNames2>>(it).toList().toPersistentList()
        } catch(ex: IllegalArgumentException) {
            try {
                Json.decodeFromString<Array<TemperamentWithNoteNames>>(it)
                    .map { old -> old.toNew() }.toPersistentList()
            } catch (ex: Exception) {
                CustomTemperamentsDefault
            }
        } catch (ex: Exception){
            CustomTemperamentsDefault
        }
    }

    private val musicalScaleDefault = MusicalScaleFactory.create(
        defaultTemperament.temperament,
        noteNames = generateNoteNames(defaultTemperament.temperament.numberOfNotesPerOctave),
        referenceNote = null,
        rootNote = null,
        referenceFrequency = DefaultValues.REFERENCE_FREQUENCY,
        frequencyMin = DefaultValues.FREQUENCY_MIN,
        frequencyMax = DefaultValues.FREQUENCY_MAX,
        stretchTuning = StretchTuning()
    )
    val musicalScale = store.getTransformablePreferenceFlow(MUSICAL_SCALE_KEY, musicalScaleDefault) {
        try {
            Json.decodeFromString<MusicalScale2>(it)
        } catch(ex: IllegalArgumentException) {
            try {
                Json.decodeFromString<MusicalScale>(it).toNew()
            } catch (ex: Exception) {
                musicalScaleDefault
            }
        } catch (ex: Exception){
            musicalScaleDefault
        }

    }
//    val musicalScale = store.getSerializablePreferenceFlow(
//        MUSICAL_SCALE_KEY,
//        MusicalScaleFactory.create(
//            defaultTemperament.temperament,
//            noteNames = generateNoteNames(defaultTemperament.temperament.numberOfNotesPerOctave),
//            referenceNote = null,
//            rootNote = null,
//            referenceFrequency = DefaultValues.REFERENCE_FREQUENCY,
//            frequencyMin = DefaultValues.FREQUENCY_MIN,
//            frequencyMax = DefaultValues.FREQUENCY_MAX,
//            stretchTuning = StretchTuning()
//        )
//    )

    val customTemperamentsExpanded = store.getPreferenceFlow(
        CUSTOM_TEMPERAMENTS_EXPANDED_KEY, CustomTemperamentsExpandedDefault
    )

    fun resetAllSettings() {
        applicationScope.launch {
             val noteNames = defaultTemperament.noteNames
                 ?: generateNoteNames(defaultTemperament.temperament.numberOfNotesPerOctave)!!
            writeMusicalScale(
                temperament = defaultTemperament,
                referenceNote = noteNames.defaultReferenceNote,
                rootNote = noteNames[0],
                referenceFrequency = DefaultValues.REFERENCE_FREQUENCY,
                stretchTuning = StretchTuning()
            )
        }
    }

    fun writeEdoTemperamentsExpanded(expanded: Boolean) {
        applicationScope.launch {
            store.writePreference(EDO_TEMPERAMENTS_EXPANDED_KEY, expanded)
        }
    }

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
        temperament: TemperamentWithNoteNames2? = null,
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
            generateNoteNames(temperamentResolved.numberOfNotesPerOctave)
        }
        val referenceNoteResolved = if (referenceNote != null) {
            referenceNote
        } else if (noteNamesResolved?.hasNote(currentMusicalScale.referenceNote) == true) {
            currentMusicalScale.referenceNote
        } else {
            null
        }
        val rootNoteResolved = if (rootNote != null) {
            rootNote
        } else if (noteNamesResolved?.hasNote(currentMusicalScale.rootNote) == true) {
            currentMusicalScale.rootNote
        } else {
            null
        }
//        Log.v("Tuner", "TemperamentResources:writeMusicalScale: ofmin=${currentMusicalScale.frequencyMin}, ofmax=${currentMusicalScale.frequencyMax}")
        val newMusicalScale = MusicalScaleFactory.create(
            temperament = temperamentResolved,
            noteNames = noteNamesResolved,
            referenceNote = referenceNoteResolved,
            rootNote = rootNoteResolved,
            referenceFrequency = referenceFrequency ?: currentMusicalScale.referenceFrequency,
            frequencyMin = currentMusicalScale.frequencyMin,
            frequencyMax = currentMusicalScale.frequencyMax,
            stretchTuning = stretchTuning ?: currentMusicalScale.stretchTuning
        )
        applicationScope.launch {
            store.writeSerializablePreference(MUSICAL_SCALE_KEY, newMusicalScale)
        }
    }

    fun writeCustomTemperaments(temperaments: List<TemperamentWithNoteNames2>) {
//        Log.v("Tuner", "TemperamentResources.writeCustomTemperaments: $temperaments")
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
//            Log.v("Tuner", "TemperamentResources.writeCustomTemperaments, write to store: $temperaments")
            store.writeSerializablePreference(CUSTOM_TEMPERAMENTS_KEY, temperaments.toTypedArray())
        }
    }

    /** Add temperament if stable id does not exist, else replace it.*/
    fun addNewOrReplaceTemperament(temperament: TemperamentWithNoteNames2) {
        val newTemperament = if (temperament.stableId == Temperament2.NO_STABLE_ID) {
            temperament.clone(getNewStableId())
        } else {
            temperament
        }

        val oldTemperaments = customTemperaments.value
        val newTemperaments = oldTemperaments.mutate { mutated ->
            val index = oldTemperaments.indexOfFirst { it.stableId == temperament.stableId }
//            Log.v("Tuner", "TemperamentResource.addNewOrReplaceTemperament: Writing temperament to index $index")
            if (index >= 0)
                mutated[index] = newTemperament
            else
                mutated.add(newTemperament)
        }
        writeCustomTemperaments(newTemperaments)
    }

    fun appendTemperaments(temperaments: List<TemperamentWithNoteNames2>) {
        val current = this.customTemperaments.value
        val newTemperamentsList = current.mutate { modified ->
            temperaments.forEach {
                val newKey = getNewStableId(modified)
                modified.add(it.clone(newKey))
            }
        }
//        Log.v("Tuner", "TemperamentResources.appendTemperaments: size=${temperaments.size}")
        writeCustomTemperaments(newTemperamentsList)
    }

    fun prependTemperaments(temperaments: List<TemperamentWithNoteNames2>) {
        val current = this.customTemperaments.value
        val newTemperamentsList = current.mutate { modified ->
            temperaments.forEachIndexed { index, temperament ->
                val newKey = getNewStableId(modified)
                modified.add(index, temperament.clone(newKey))
            }
        }
        writeCustomTemperaments(newTemperamentsList)
    }

    fun replaceTemperaments(temperaments: List<TemperamentWithNoteNames2>) {
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
        existingTemperaments: List<TemperamentWithNoteNames2> = customTemperaments.value
    ): Long {
        val currentKey = musicalScale.value.temperament.stableId
        while (true) {
            val stableId = Random.nextLong(0, Long.MAX_VALUE - 1)
            if ((currentKey != stableId) && (existingTemperaments.firstOrNull {it.stableId == stableId} == null))
                return stableId
        }
    }

    companion object {
        private val CustomTemperamentsDefault = persistentListOf<TemperamentWithNoteNames2>()
        private const val EdoTemperamentsExpandedDefault = true
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

        private val EDO_TEMPERAMENTS_EXPANDED_KEY = booleanPreferencesKey(
            "edo temperaments expanded"
        )
    }
}