package de.moekadu.tuner

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class InstrumentsFragment : Fragment() {
    private val instrumentsViewModel: InstrumentsViewModel by activityViewModels {
        InstrumentsViewModel.Factory(
            AppPreferences.readInstrumentId(requireActivity()),
            requireActivity().application
        )
    }

    private var recyclerView: RecyclerView? = null
    private val instrumentsAdapter = InstrumentsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }
    override fun onPrepareOptionsMenu(menu : Menu) {
//        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_settings)?.isVisible = false
        menu.findItem(R.id.action_instruments)?.isVisible = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.instruments, container, false)

        recyclerView = view.findViewById(R.id.instrument_list)
        recyclerView?.setHasFixedSize(false)
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        recyclerView?.adapter = instrumentsAdapter

        instrumentsAdapter.submitList(instrumentDatabase)

        instrumentsAdapter.onInstrumentClickedListener = InstrumentsAdapter.OnInstrumentClickedListener { instrument, stableId ->
//            Log.v("Tuner", "InstrumentsFragment.onCreateView: new instrument: $instrument")
            instrumentsViewModel.setInstrument(instrument)
        }

        instrumentsViewModel.instrument.observe(viewLifecycleOwner) {
//            Log.v("Tuner", "InstrumentsFragment.onCreateView: setStableId: $it")
            instrumentsAdapter.setActiveStableId(it.stableId, recyclerView)
        }
        return view
    }
}