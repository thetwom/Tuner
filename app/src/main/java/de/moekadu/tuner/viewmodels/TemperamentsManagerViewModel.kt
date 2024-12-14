package de.moekadu.tuner.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.hilt.ApplicationScope
import de.moekadu.tuner.temperaments.TemperamentIO
import de.moekadu.tuner.temperaments.TemperamentResources
import de.moekadu.tuner.temperaments.TemperamentWithNoteNames
import de.moekadu.tuner.ui.common.EditableListData
import de.moekadu.tuner.ui.temperaments.TemperamentsManagerData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = TemperamentsManagerViewModel.Factory::class)
class TemperamentsManagerViewModel @AssistedInject constructor(
    @Assisted initialTemperamentKey: Long,
    val pref: TemperamentResources,
    @ApplicationScope val applicationScope: CoroutineScope
) : TemperamentsManagerData, ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(initialTemperamentKey: Long): TemperamentsManagerViewModel
    }

    private val activeTemperament = MutableStateFlow(
        pref.predefinedTemperaments.firstOrNull {
            it.temperament.stableId == initialTemperamentKey
        } ?: pref.customTemperaments.value.firstOrNull {
            it.temperament.stableId == initialTemperamentKey
        }
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

    fun activateTemperament(key: Long) {
        activeTemperament.value = pref.predefinedTemperaments.firstOrNull {
            it.temperament.stableId == key
        } ?: pref.customTemperaments.value.firstOrNull {
            it.temperament.stableId == key
        }
    }
}