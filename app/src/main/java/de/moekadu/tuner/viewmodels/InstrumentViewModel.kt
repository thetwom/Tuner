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
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.hilt.ApplicationScope
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentIO
import de.moekadu.tuner.instruments.InstrumentResources
import de.moekadu.tuner.ui.screens.InstrumentsData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InstrumentViewModel @Inject constructor(
    val instruments: InstrumentResources,
    @ApplicationScope val applicationScope: CoroutineScope
): ViewModel(), InstrumentsData {
    override val activeInstrument get() = instruments.currentInstrument

    override val predefinedInstruments get() = instruments.predefinedInstruments

    override val predefinedInstrumentsExpanded get() = instruments.predefinedInstrumentsExpanded

    override val customInstruments get() = instruments.customInstruments
    override val customInstrumentsExpanded get() = instruments.customInstrumentsExpanded
    @Volatile
    override var previousCustomInstruments: ImmutableList<Instrument> = customInstruments.value
        private set


    private val _selectedInstruments = MutableStateFlow(persistentSetOf<Long>())
    override val selectedInstruments get() = _selectedInstruments.asStateFlow()

    override val customInstrumentsBackup = Channel<InstrumentsData.InstrumentDeleteInfo>(
        Channel.CONFLATED
    )

    init {
        viewModelScope.launch {
            var backup = customInstruments.value
            customInstruments.collect {
                previousCustomInstruments = backup
                backup = it
            }
        }
    }

    override fun expandPredefinedInstruments(isExpanded: Boolean) {
        instruments.writePredefinedInstrumentsExpanded(isExpanded)
    }

    override fun expandCustomInstruments(isExpanded: Boolean) {
        instruments.writeCustomInstrumentsExpanded(isExpanded)
    }

    override fun selectInstrument(id: Long) {
        _selectedInstruments.value = selectedInstruments.value.add(id)
    }

    override fun deselectInstrument(id: Long) {
        _selectedInstruments.value = selectedInstruments.value.remove(id)
    }

    override fun toggleSelection(id: Long) {
        if (selectedInstruments.value.contains(id))
            _selectedInstruments.value = selectedInstruments.value.remove(id)
        else
            _selectedInstruments.value = selectedInstruments.value.add(id)
    }

    override fun clearSelectedInstruments() {
        _selectedInstruments.value = persistentSetOf()
    }

    override fun moveInstrumentsUp(instrumentKeys: Set<Long>): Boolean {
        val original = customInstruments.value
        if (original.size <= 1)
            return false
        var changed = false
        val modified = original.mutate { instruments ->
            for (i in 1 until original.size) {
                val instrument = original[i]
                val instrumentPrev = instruments[i-1]
                if (instrumentKeys.contains(instrument.stableId)
                    && !instrumentKeys.contains(instrumentPrev.stableId)) {
                    instruments.add(i - 1, instruments.removeAt(i))
                    changed = true
                }
            }
        }
        instruments.writeCustomInstruments(modified)
        return changed
    }

    override fun moveInstrumentsDown(instrumentKeys: Set<Long>): Boolean {
        val original = customInstruments.value
        if (original.size <= 1)
            return false
        var changed = false
        val modified = original.mutate { instruments ->
            for (i in original.size - 2 downTo 0) {
                val instrument = original[i]
                val instrumentNext = instruments[i+1]
                if (instrumentKeys.contains(instrument.stableId)
                    && !instrumentKeys.contains(instrumentNext.stableId)) {
                    instruments.add(i + 1, instruments.removeAt(i))
                    changed = true
                }
            }
        }
        instruments.writeCustomInstruments(modified)
        return changed
    }

    override fun deleteInstruments(instrumentKeys: Set<Long>) {
        val backup = customInstruments.value
        val modified = customInstruments.value.removeAll { instrumentKeys.contains(it.stableId) }
        instruments.writeCustomInstruments(modified)
        _selectedInstruments.value = selectedInstruments.value.removeAll(instrumentKeys)
        if (modified.isEmpty())
            instruments.writePredefinedInstrumentsExpanded(true)
        if (backup.size != modified.size) {
            customInstrumentsBackup.trySend(InstrumentsData.InstrumentDeleteInfo(
                backup, backup.size - modified.size
            ))
        }
    }

    override fun deleteAllInstruments() {
        val backup = customInstruments.value
        instruments.writeCustomInstruments(persistentListOf())
        _selectedInstruments.value = selectedInstruments.value.clear()
        instruments.writePredefinedInstrumentsExpanded(true)

        if (backup.size > 0) {
            customInstrumentsBackup.trySend(InstrumentsData.InstrumentDeleteInfo(
                backup, backup.size
            ))
        }
    }

    fun setCurrentInstrument(instrument: Instrument) {
        instruments.writeCurrentInstrument(instrument)
    }

    override fun setInstruments(instruments: ImmutableList<Instrument>) {
        this.instruments.writeCustomInstruments(instruments)
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