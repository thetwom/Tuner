package de.moekadu.tuner.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentResources2
import de.moekadu.tuner.ui.screens.InstrumentsData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class InstrumentViewModel2 @Inject constructor(
    val instruments: InstrumentResources2
): ViewModel(), InstrumentsData {
    override val activeInstrument get() = instruments.currentInstrument

    override val predefinedInstruments get() = instruments.predefinedInstruments

    override val predefinedInstrumentsExpanded get() = instruments.predefinedInstrumentsExpanded

    override val customInstruments get() = instruments.customInstruments
    override val customInstrumentsExpanded get() = instruments.customInstrumentsExpanded

    private val _selectedInstruments = MutableStateFlow(persistentSetOf<Long>())
    override val selectedInstruments get() = _selectedInstruments.asStateFlow()

    override val customInstrumentsBackup = Channel<InstrumentsData.InstrumentDeleteInfo>(
        Channel.CONFLATED
    )

    override suspend fun expandPredefinedInstruments(isExpanded: Boolean) {
        instruments.writePredefinedInstrumentsExpanded(isExpanded)
    }

    override suspend fun expandCustomInstruments(isExpanded: Boolean) {
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

    override suspend fun moveInstrumentsUp(instrumentKeys: Set<Long>) {
        val original = customInstruments.value
        if (original.size <= 1)
            return

        val modified = original.mutate { instruments ->
            for (i in 1 until original.size) {
                val instrument = original[i]
                val instrumentPrev = instruments[i-1]
                if (instrumentKeys.contains(instrument.stableId)
                    && !instrumentKeys.contains(instrumentPrev.stableId))
                    instruments.add(i - 1, instruments.removeAt(i))
            }
        }
        instruments.writeCustomInstruments(modified)
    }

    override suspend fun moveInstrumentsDown(instrumentKeys: Set<Long>) {
        val original = customInstruments.value
        if (original.size <= 1)
            return

        val modified = original.mutate { instruments ->
            for (i in original.size - 2 downTo 0) {
                val instrument = original[i]
                val instrumentNext = instruments[i+1]
                if (instrumentKeys.contains(instrument.stableId)
                    && !instrumentKeys.contains(instrumentNext.stableId))
                    instruments.add(i + 1, instruments.removeAt(i))
            }
        }
        instruments.writeCustomInstruments(modified)
    }

    override suspend fun deleteInstruments(instrumentKeys: Set<Long>) {
        val backup = customInstruments.value
        val modified = customInstruments.value.removeAll { instrumentKeys.contains(it.stableId) }
        instruments.writeCustomInstruments(modified)
        _selectedInstruments.value = selectedInstruments.value.removeAll(instrumentKeys)
        if (backup.size != modified.size) {
            customInstrumentsBackup.trySend(InstrumentsData.InstrumentDeleteInfo(
                backup, backup.size - modified.size
            ))
        }
    }

    override suspend fun deleteAllInstruments() {
        val backup = customInstruments.value
        instruments.writeCustomInstruments(persistentListOf())
        _selectedInstruments.value = selectedInstruments.value.clear()
        if (backup.size > 0) {
            customInstrumentsBackup.trySend(InstrumentsData.InstrumentDeleteInfo(
                backup, backup.size
            ))
        }
    }

    suspend fun setCurrentInstrument(instrument: Instrument) {
        instruments.writeCurrentInstrument(instrument)
    }

    override suspend fun setInstruments(instruments: ImmutableList<Instrument>) {
        this.instruments.writeCustomInstruments(instruments)
    }
}