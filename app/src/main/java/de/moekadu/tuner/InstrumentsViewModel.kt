package de.moekadu.tuner

import android.app.Application
import android.util.Log
import androidx.lifecycle.*


class InstrumentsViewModel(initialInstrumentId: Long, application: Application) : AndroidViewModel(application) {
    // TODO: show active instrument in fragments
    // TODO: make notes in string view red when not in tune
    // TODO: landscape layout
    private var _instrument = MutableLiveData<Instrument>().apply {
        value = instrumentDatabase.find { it.stableId == initialInstrumentId } ?: instrumentDatabase[0]
    }

    val instrument: LiveData<Instrument>
        get() = _instrument

    fun setInstrument(instrument: Instrument) {
        val currentInstrument = _instrument.value
        if (currentInstrument == null || currentInstrument.stableId != instrument.stableId) {
//            Log.v("Tuner", "InstrumentsViewModel.setInstrument: $instrument")
            _instrument.value = instrument
        }
    }

    class Factory(private val initialInstrumentId: Long, private val application: Application) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
//            Log.v("Metronome", "ScenesViewModel.factory.create")
            return InstrumentsViewModel(initialInstrumentId, application) as T
        }
    }
}