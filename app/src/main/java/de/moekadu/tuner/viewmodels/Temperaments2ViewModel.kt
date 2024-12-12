package de.moekadu.tuner.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.hilt.ApplicationScope
import de.moekadu.tuner.temperaments2.TemperamentIO
import de.moekadu.tuner.temperaments2.TemperamentResources
import de.moekadu.tuner.temperaments2.TemperamentWithNoteNames
import de.moekadu.tuner.ui.common.EditableListData
import de.moekadu.tuner.ui.screens.Temperaments2Data
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class Temperaments2ViewModel @Inject constructor(
    val pref: TemperamentResources,
    @ApplicationScope val applicationScope: CoroutineScope
) : Temperaments2Data, ViewModel() {

    private val activeTemperament = MutableStateFlow<TemperamentWithNoteNames?>(null)

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
}