package de.moekadu.tuner.viewmodels

import androidx.lifecycle.*
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentResources
import de.moekadu.tuner.models.InstrumentListModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** View model for showing available instruments.
 * @param instrumentResources Instrument resources.
 */
class InstrumentsViewModel(
    private val instrumentResources: InstrumentResources
) : ViewModel() {

//    /** Shortcut for current instrument. */
//    val instrument get() = instrumentResources.instrument
//
//    /** Model for list of instruments. */
//    private val _instrumentListModel = MutableStateFlow(InstrumentListModel.fromInstrumentResources(instrumentResources))
//    val instrumentListModel: StateFlow<InstrumentListModel> get() = _instrumentListModel
//
//    /** Database of custom instruments. */
//    val customInstrumentDatabase get() = instrumentResources.customInstrumentDatabase
//
//    init {
//        viewModelScope.launch {
//            instrumentResources.customInstruments.collect {
//                _instrumentListModel.value = InstrumentListModel.fromInstrumentResources(instrumentResources)
//            }
//        }
//        viewModelScope.launch {
//            instrumentResources.customInstrumentsExpanded.collect {
//                _instrumentListModel.value = InstrumentListModel.fromInstrumentResources(instrumentResources)
//            }
//        }
//        viewModelScope.launch {
//            instrumentResources.predefinedInstrumentsExpanded.collect {
//                _instrumentListModel.value = InstrumentListModel.fromInstrumentResources(instrumentResources)
//            }
//        }
//        viewModelScope.launch {
//            instrumentResources.instrument.collect {
//                _instrumentListModel.value = InstrumentListModel.fromInstrumentResources(instrumentResources)
//            }
//        }
//
//    }
//
//    /** Set active instrument.
//     * @param instrument New instrument.
//     */
//    fun setInstrument(instrument: Instrument) {
//        instrumentResources.selectInstrument(instrument)
//    }

//    /** Expand the predefined instruments list.
//     * @param isExpanded True, to expand the list or false to collapse the list.
//     */
//    fun expandPredefinedDatabase(isExpanded: Boolean = true) {
//        instrumentResources.setPredefinedInstrumentsExpanded(isExpanded)
//    }
//
//    /** Expand the custom instruments list.
//     * @param isExpanded True, to expand the list or false to collapse the list.
//     */
//    fun expandCustomDatabase(isExpanded: Boolean = true) {
//        instrumentResources.setCustomInstrumentsExpanded(isExpanded)
//    }
//
//    /** Move a custom instrument.
//     * @param fromIndex Index of instrument before moving.
//     * @param toIndex Index of instrument after moving.
//     */
//    fun moveCustomInstrument(fromIndex: Int, toIndex: Int) {
//        instrumentResources.moveCustomInstrument(fromIndex, toIndex)
//    }
//
//    /** Remove a custom instrument.
//     * @param position Position in instrument list.
//     */
//    fun removeCustomInstrument(position: Int): Instrument {
//        return instrumentResources.removeCustomInstrument(position)
//    }
//
//    /** Add a custom instrument.
//     * @param position Position where to place the instrument.
//     * @param instrument Instrument to add.
//     */
//    fun addCustomInstrument(position: Int, instrument: Instrument) {
//        instrumentResources.addCustomInstrument(position, instrument)
//    }

    /** Factory to create the view model.
     * @param instrumentResources Instrument resources.
     */
    class Factory(private val instrumentResources: InstrumentResources) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
//            Log.v("Tuner", "InstrumentsViewModel.Factory.create")
            return InstrumentsViewModel(instrumentResources) as T
        }
    }
}