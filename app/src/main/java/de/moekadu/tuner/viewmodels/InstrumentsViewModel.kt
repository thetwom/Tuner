package de.moekadu.tuner.viewmodels

import androidx.lifecycle.*
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentResources
import de.moekadu.tuner.models.InstrumentListModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class InstrumentsViewModel(
    private val instrumentResources: InstrumentResources
) : ViewModel() {

    val instrument get() = instrumentResources.instrument

    private val _instrumentListModel = MutableStateFlow(InstrumentListModel.fromInstrumentResources(instrumentResources))
    val instrumentListModel: StateFlow<InstrumentListModel> get() = _instrumentListModel

    val customInstrumentDatabase get() = instrumentResources.customInstrumentDatabase

    init {
        viewModelScope.launch {
            instrumentResources.customInstruments.collect {
                _instrumentListModel.value = InstrumentListModel.fromInstrumentResources(instrumentResources)
            }
        }
        viewModelScope.launch {
            instrumentResources.customInstrumentsExpanded.collect {
                _instrumentListModel.value = InstrumentListModel.fromInstrumentResources(instrumentResources)
            }
        }
        viewModelScope.launch {
            instrumentResources.predefinedInstrumentsExpanded.collect {
                _instrumentListModel.value = InstrumentListModel.fromInstrumentResources(instrumentResources)
            }
        }
        viewModelScope.launch {
            instrumentResources.instrument.collect {
                _instrumentListModel.value = InstrumentListModel.fromInstrumentResources(instrumentResources)
            }
        }

    }

    fun setInstrument(instrument: Instrument) {
        instrumentResources.selectInstrument(instrument)
    }

    fun expandPredefinedDatabase(isExpanded: Boolean = true) {
        instrumentResources.setPredefinedInstrumentsExpanded(isExpanded)
    }

    fun expandCustomDatabase(isExpanded: Boolean = true) {
        instrumentResources.setCustomInstrumentsExpanded(isExpanded)
    }

    fun moveCustomInstrument(fromIndex: Int, toIndex: Int) {
        instrumentResources.moveCustomInstrument(fromIndex, toIndex)
    }

    fun removeCustomInstrument(position: Int): Instrument {
        return instrumentResources.removeCustomInstrument(position)
    }

    fun addCustomInstrument(position: Int, instrument: Instrument) {
        instrumentResources.addCustomInstrument(position, instrument)
    }

    class Factory(private val instrumentResources: InstrumentResources) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
//            Log.v("Tuner", "InstrumentsViewModel.Factory.create")
            return InstrumentsViewModel(instrumentResources) as T
        }
    }
}