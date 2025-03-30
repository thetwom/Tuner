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
package de.moekadu.tuner.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.R
import de.moekadu.tuner.hilt.ApplicationScope
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentIO
import de.moekadu.tuner.instruments.InstrumentResources
import de.moekadu.tuner.ui.common.EditableListPredefinedSectionImmutable
import de.moekadu.tuner.ui.common.EditableListData
import de.moekadu.tuner.ui.instruments.InstrumentsData
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InstrumentViewModel @Inject constructor(
    val instruments: InstrumentResources,
    @ApplicationScope val applicationScope: CoroutineScope
): ViewModel(), InstrumentsData {
    override val listData = EditableListData(
        predefinedItemSections = persistentListOf(
            EditableListPredefinedSectionImmutable(
                R.string.predefined_items,
                instruments.predefinedInstruments,
                instruments.predefinedInstrumentsExpanded,
                { instruments.writePredefinedInstrumentsExpanded(it) }
            )
        ),
        editableItems = instruments.customInstruments,
        editableItemsExpanded = instruments.customInstrumentsExpanded,
        toggleEditableItemsExpanded = { instruments.writeCustomInstrumentsExpanded(it) },
        getStableId = { it.stableId },
        activeItem = instruments.currentInstrument,
        setNewItems = { instruments.writeCustomInstruments(it) },
    )

    fun setCurrentInstrument(instrument: Instrument) {
        instruments.writeCurrentInstrument(instrument)
    }

    override fun saveInstruments(context: Context, uri: Uri, instruments: List<Instrument>) {
        applicationScope.launch(Dispatchers.IO) {
            context.contentResolver?.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(
                    InstrumentIO.instrumentsListToString(context, instruments).toByteArray()
                )
            }
        }
    }
}