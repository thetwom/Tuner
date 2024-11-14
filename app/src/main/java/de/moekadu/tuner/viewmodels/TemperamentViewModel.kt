package de.moekadu.tuner.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.hilt.ApplicationScope
import de.moekadu.tuner.temperaments2.TemperamentIO
import de.moekadu.tuner.temperaments2.TemperamentResources
import de.moekadu.tuner.temperaments2.TemperamentWithNoteNames
import de.moekadu.tuner.ui.common.EditableListData
import de.moekadu.tuner.ui.screens.TemperamentsData
import de.moekadu.tuner.ui.temperaments.ActiveTemperamentDetailChoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemperamentViewModel @Inject constructor(
    val pref: TemperamentResources,
    @ApplicationScope val applicationScope: CoroutineScope
) : ViewModel(), TemperamentsData {

    private val initialMusicalScale = pref.musicalScale.value

    private val activeTemperament = MutableStateFlow(
        TemperamentWithNoteNames(
            initialMusicalScale.temperament,
            initialMusicalScale.noteNames
        )
    )

    override val listData = EditableListData(
        predefinedItems = pref.predefinedTemperaments,
        editableItems = pref.customTemperaments,
        getStableId = { it.stableId },
        predefinedItemsExpanded = pref.predefinedTemperamentsExpanded,
        editableItemsExpanded = pref.customTemperamentsExpanded,
        activeItem = activeTemperament,
        setNewItems = { pref.writeCustomTemperaments(it) },
        togglePredefinedItemsExpanded = { pref.writePredefinedTemperamentsExpanded(it) },
        toggleEditableItemsExpanded = { pref.writeCustomTemperamentsExpanded(it) }
    )

    override val selectedRootNoteIndex = MutableStateFlow(
        initialMusicalScale.noteNames.getNoteIndex(initialMusicalScale.rootNote)
    )

    override val detailChoice = MutableStateFlow(ActiveTemperamentDetailChoice.Off)

    override fun changeDetailChoice(choice: ActiveTemperamentDetailChoice) {
        detailChoice.value = choice
    }

    override fun changeRootNoteIndex(index: Int) {
        selectedRootNoteIndex.value = index
    }

    override fun changeActiveTemperament(temperament: TemperamentWithNoteNames) {
        if (selectedRootNoteIndex.value >= temperament.temperament.numberOfNotesPerOctave)
            selectedRootNoteIndex.value = temperament.temperament.numberOfNotesPerOctave - 1
        activeTemperament.value = temperament
    }

    override fun saveTemperaments(
        context: Context,
        uri: Uri,
        temperaments: List<TemperamentWithNoteNames>
    ) {
        applicationScope.launch(Dispatchers.IO) {
            context.contentResolver?.openOutputStream(uri, "wt")?.use { stream ->
                stream.bufferedWriter().use { writer ->
                    TemperamentIO.writeTemperaments(temperaments, writer, context)
                }
            }
        }
    }

    override fun resetToDefault() {
        selectedRootNoteIndex.value = 0
        activeTemperament.value = pref.predefinedTemperaments[0]
    }

    init {
        // make sure that the currently active item will follow changes when it was changed in
        // the temperament editor
        viewModelScope.launch {
            pref.customTemperaments.collect{
                val stableId = activeTemperament.value.stableId
                it.firstOrNull { it.stableId == stableId }?.let { temperament ->
                    changeActiveTemperament(temperament)
                }
            }
        }
    }
}