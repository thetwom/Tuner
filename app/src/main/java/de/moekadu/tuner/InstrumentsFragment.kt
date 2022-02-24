package de.moekadu.tuner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class InstrumentsFragment : Fragment() {
    // TODO: show string list automatically (do not use the strings)
    private val instrumentsViewModel: InstrumentsViewModel by activityViewModels {
        InstrumentsViewModel.Factory(
            AppPreferences.readInstrumentId(requireActivity()),
            AppPreferences.readInstrumentSection(requireActivity()),
            AppPreferences.readCustomInstruments(requireActivity()),
            AppPreferences.readPredefinedSectionExpanded(requireActivity()),
            AppPreferences.readCustomSectionExpanded(requireActivity()),
            requireActivity().application
        )
    }

    private var recyclerView: RecyclerView? = null
    private val instrumentsPredefinedAdapter = InstrumentsAdapter()
    private val instrumentsCustomAdapter = InstrumentsAdapter()
    private val instrumentSectionPredefinedAdapter = InstrumentsSectionAdapter(R.string.predefinded_instruments)
    private val instrumentSectionCustomAdapter = InstrumentsSectionAdapter(R.string.custom_instruments)

    private var tuningEditorFab: FloatingActionButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }
    override fun onPrepareOptionsMenu(menu : Menu) {
//        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_settings)?.isVisible = false
//        menu.findItem(R.id.action_instruments)?.isVisible = false
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

        instrumentSectionPredefinedAdapter.sectionClickedListener = InstrumentsSectionAdapter.SectionClickedListener {
            instrumentsViewModel.expandPredefinedDatabase(!(instrumentsViewModel.predefinedDatabaseExpanded.value ?: true))
            //instrumentSectionPredefinedAdapter.expanded = !instrumentSectionPredefinedAdapter.expanded
        }
        instrumentSectionCustomAdapter.sectionClickedListener = InstrumentsSectionAdapter.SectionClickedListener {
            instrumentsViewModel.expandCustomDatabase(!(instrumentsViewModel.customDatabaseExpanded.value ?: true))
            // instrumentSectionCustomAdapter.expanded = !instrumentSectionCustomAdapter.expanded
        }

        recyclerView?.adapter = ConcatAdapter(
            instrumentSectionCustomAdapter,
            instrumentsCustomAdapter,
            instrumentSectionPredefinedAdapter,
            instrumentsPredefinedAdapter
        )

        // instrumentsPredefinedAdapter.submitList(instrumentDatabase)

        instrumentsPredefinedAdapter.onInstrumentClickedListener = InstrumentsAdapter.OnInstrumentClickedListener { instrument, stableId ->
//            Log.v("Tuner", "InstrumentsFragment.onCreateView: new instrument: $instrument")
            instrumentsViewModel.setInstrument(instrument, InstrumentsViewModel.Section.Predefined)
            (activity as MainActivity?)?.onBackPressed()
        }
        instrumentsCustomAdapter.onInstrumentClickedListener = InstrumentsAdapter.OnInstrumentClickedListener { instrument, stableId ->
//            Log.v("Tuner", "InstrumentsFragment.onCreateView: new instrument: $instrument")
            instrumentsViewModel.setInstrument(instrument, InstrumentsViewModel.Section.Custom)
            (activity as MainActivity?)?.onBackPressed()
        }

        instrumentsViewModel.instrument.observe(viewLifecycleOwner) {
//            Log.v("Tuner", "InstrumentsFragment.onCreateView: setStableId: $it")
            when (it.section) {
                InstrumentsViewModel.Section.Predefined -> {
                    instrumentsPredefinedAdapter.setActiveStableId(it.instrument.stableId, recyclerView)
                    instrumentsCustomAdapter.setActiveStableId(Instrument.NO_STABLE_ID, recyclerView)
                }
                InstrumentsViewModel.Section.Custom -> {
                    instrumentsPredefinedAdapter.setActiveStableId(Instrument.NO_STABLE_ID, recyclerView)
                    instrumentsCustomAdapter.setActiveStableId(it.instrument.stableId, recyclerView)
                }
            }
        }

        instrumentsViewModel.customInstrumentDatabase.observe(viewLifecycleOwner) { database ->
            if (instrumentsViewModel.customDatabaseExpanded.value == false) {
                instrumentsCustomAdapter.submitList(ArrayList())
            } else {
                val databaseCopy = ArrayList<Instrument>(database.instruments.size)
                database.instruments.forEach { databaseCopy.add(it.copy()) }
                instrumentsCustomAdapter.submitList(databaseCopy)
            }
            activity?.let {
                AppPreferences.writeCustomInstruments(database.getInstrumentsString(it), it)
            }
        }

        instrumentsViewModel.predefinedDatabaseExpanded.observe(viewLifecycleOwner) { expanded ->
            instrumentSectionPredefinedAdapter.expanded = expanded
            if (expanded)
                instrumentsPredefinedAdapter.submitList(instrumentDatabase)
            else
                instrumentsPredefinedAdapter.submitList(ArrayList())
        }

        instrumentsViewModel.customDatabaseExpanded.observe(viewLifecycleOwner) { expanded ->
            instrumentSectionCustomAdapter.expanded = expanded
            if (expanded)
                instrumentsCustomAdapter.submitList(instrumentsViewModel.customInstrumentDatabase.value?.instruments ?: ArrayList())
            else
                instrumentsCustomAdapter.submitList(ArrayList())
        }

        tuningEditorFab = view.findViewById(R.id.tuning_editor_fab)
        tuningEditorFab?.setOnClickListener {
            (requireActivity() as MainActivity).loadTuningEditorFragment()
        }
        return view
    }
}