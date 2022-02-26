package de.moekadu.tuner

import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.roundToInt

class InstrumentsFragment : Fragment() {
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
    private val tuningEditorViewModel: TuningEditorViewModel by activityViewModels()
    private val tunerViewModel: TunerViewModel by activityViewModels() // ? = null

    private var recyclerView: RecyclerView? = null
    private val instrumentsPredefinedAdapter = InstrumentsAdapter()
    private val instrumentsCustomAdapter = InstrumentsAdapter()
    private val instrumentSectionPredefinedAdapter = InstrumentsSectionAdapter(R.string.predefinded_instruments)
    private val instrumentSectionCustomAdapter = InstrumentsSectionAdapter(R.string.custom_instruments)

    private var tuningEditorFab: FloatingActionButton? = null

    private var lastRemovedInstrumentIndex = -1
    private var lastRemovedInstrument: Instrument? = null

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
//        Log.v("Tuner", "InstrumentsFragment.onCreateView: Start creating view")

        val view = inflater.inflate(R.layout.instruments, container, false)

        recyclerView = view.findViewById(R.id.instrument_list)
        recyclerView?.setHasFixedSize(false)
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())

        instrumentSectionPredefinedAdapter.sectionClickedListener =
            InstrumentsSectionAdapter.SectionClickedListener {
                instrumentsViewModel.expandPredefinedDatabase(
                    !(instrumentsViewModel.predefinedDatabaseExpanded.value ?: true)
                )
            }
        instrumentSectionCustomAdapter.sectionClickedListener =
            InstrumentsSectionAdapter.SectionClickedListener {
                instrumentsViewModel.expandCustomDatabase(
                    !(instrumentsViewModel.customDatabaseExpanded.value ?: true)
                )
            }

        recyclerView?.adapter = ConcatAdapter(
            instrumentSectionCustomAdapter,
            instrumentsCustomAdapter,
            instrumentSectionPredefinedAdapter,
            instrumentsPredefinedAdapter
        )
        //recyclerView?.adapter = instrumentsCustomAdapter

        instrumentsPredefinedAdapter.onInstrumentClickedListener =
            InstrumentsAdapter.OnInstrumentClickedListener { instrument, stableId ->
//            Log.v("Tuner", "InstrumentsFragment.onCreateView: new instrument: $instrument")
                instrumentsViewModel.setInstrument(
                    instrument,
                    InstrumentsViewModel.Section.Predefined
                )
                (activity as MainActivity?)?.onBackPressed()
            }
        instrumentsCustomAdapter.onInstrumentClickedListener =
            InstrumentsAdapter.OnInstrumentClickedListener { instrument, stableId ->
//            Log.v("Tuner", "InstrumentsFragment.onCreateView: new instrument: $instrument")
                instrumentsViewModel.setInstrument(instrument, InstrumentsViewModel.Section.Custom)
                (activity as MainActivity?)?.onBackPressed()
            }

        val simpleTouchHelper = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT
        ) {

            val background = activity?.let {
                ContextCompat.getDrawable(
                    it,
                    R.drawable.instrument_below_background
                )
            }
            val deleteIcon =
                activity?.let { ContextCompat.getDrawable(it, R.drawable.ic_delete_instrument) }

            override fun getDragDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                //Log.v("Tuner", "InstrumentFragment.simpleTouchHelper.getDragDirs")
                return if (viewHolder is InstrumentsAdapter.ViewHolder && viewHolder.instrument?.stableId ?: -1 >= 0)
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN
                else
                    0
            }

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                //Log.v("Tuner", "InstrumentFragment.simpleTouchHelper.getSwipeDirs: itemId=${viewHolder.itemId}")
                return if (viewHolder is InstrumentsAdapter.ViewHolder && viewHolder.instrument?.stableId ?: -1 >= 0)
                    ItemTouchHelper.LEFT
                else
                    0
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (viewHolder is InstrumentsAdapter.ViewHolder && target is InstrumentsAdapter.ViewHolder
                    && viewHolder.instrument?.stableId ?: -1 >= 0 && target.instrument?.stableId ?: -1 >= 0
                ) {

                    val fromPos = viewHolder.bindingAdapterPosition
                    val toPos = target.bindingAdapterPosition
                    if (fromPos != toPos) {
                        instrumentsViewModel.customInstrumentDatabase.move(fromPos, toPos)
                    }
                    return true
                }
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (viewHolder is InstrumentsAdapter.ViewHolder && viewHolder.instrument?.stableId ?: -1 >= 0) {
                    lastRemovedInstrumentIndex = viewHolder.bindingAdapterPosition
//                    Log.v("Tuner", "InstrumentsFragment:onSwiped removing index $lastRemovedInstrumentIndex")
                    lastRemovedInstrument =
                        instrumentsViewModel.customInstrumentDatabase.remove(
                            lastRemovedInstrumentIndex
                        )

                    (getView() as ConstraintLayout?)?.let { coLayout ->
                        lastRemovedInstrument?.let { removedItem ->
                            Snackbar.make(
                                coLayout,
                                getString(R.string.instrument_deleted),
                                Snackbar.LENGTH_LONG
                            )
                                .setAction(R.string.undo) {
                                    if (lastRemovedInstrument != null) {
                                        instrumentsViewModel.customInstrumentDatabase.add(
                                            lastRemovedInstrumentIndex,
                                            removedItem
                                        )
                                        lastRemovedInstrument = null
                                        lastRemovedInstrumentIndex = -1
                                    }
                                }.show()
                        }

                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (viewHolder is InstrumentsAdapter.ViewHolder && viewHolder.instrument?.stableId ?: -1 >= 0) {
                    val itemView = viewHolder.itemView

                    // not sure why, but this method get's called for viewholder that are already swiped away
                    if (viewHolder.bindingAdapterPosition == RecyclerView.NO_POSITION) {
                        // not interested in those
                        return
                    }

                    background?.setBounds(
                        itemView.left,
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background?.draw(c)

                    if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                        deleteIcon?.alpha =
                            min(255, (255 * 3 * dX.absoluteValue / itemView.width).toInt())
                        val iconHeight =
                            (0.4f * (itemView.height - itemView.paddingTop - itemView.paddingBottom)).roundToInt()
                        val deleteIconLeft =
                            itemView.right - iconHeight - itemView.paddingRight //itemView.right + iconHeight + itemView.paddingRight + dX.roundToInt()
                        deleteIcon?.setBounds(
                            deleteIconLeft,
                            (itemView.top + itemView.bottom - iconHeight) / 2,
                            deleteIconLeft + iconHeight,
                            (itemView.top + itemView.bottom + iconHeight) / 2
                        )
                        deleteIcon?.draw(c)
                    }
                }
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }

        val touchHelper = ItemTouchHelper(simpleTouchHelper)
        touchHelper.attachToRecyclerView(recyclerView)

        tunerViewModel.tuningFrequencies.observe(viewLifecycleOwner) {
            instrumentsPredefinedAdapter.setTuningFrequencies(it, recyclerView)
            instrumentsCustomAdapter.setTuningFrequencies(it, recyclerView)
        }

        instrumentsViewModel.instrument.observe(viewLifecycleOwner) {
//            Log.v("Tuner", "InstrumentsFragment.onCreateView: setStableId: $it")
            when (it.section) {
                InstrumentsViewModel.Section.Predefined -> {
                    instrumentsPredefinedAdapter.setActiveStableId(
                        it.instrument.stableId,
                        recyclerView
                    )
                    instrumentsCustomAdapter.setActiveStableId(
                        Instrument.NO_STABLE_ID,
                        recyclerView
                    )
                }
                InstrumentsViewModel.Section.Custom -> {
                    instrumentsPredefinedAdapter.setActiveStableId(
                        Instrument.NO_STABLE_ID,
                        recyclerView
                    )
                    instrumentsCustomAdapter.setActiveStableId(it.instrument.stableId, recyclerView)
                }
            }
        }

        instrumentsViewModel.customInstrumentDatabaseAsLiveData.observe(viewLifecycleOwner) { database ->
            // switch of section titles if there are not custom instruments
            if (database.size == 0) {
                instrumentSectionCustomAdapter.visible = false
                instrumentSectionPredefinedAdapter.visible = false
                instrumentsViewModel.expandPredefinedDatabase(true)
            } else {
                instrumentSectionCustomAdapter.visible = true
                instrumentSectionPredefinedAdapter.visible = true
            }

            activity?.let {
                AppPreferences.writeCustomInstruments(database.getInstrumentsString(it), it)
            }
        }

        instrumentsViewModel.customInstrumentList.observe(viewLifecycleOwner) { instrumentList ->
//            Log.v("Tuner", "InstrumentsFragment: observing custom instruments: new size= ${instrumentList.size}")
//                Log.v("Tuner", "InstrumentFragment: Instrument stableId = ${i.stableId}")
            //Log.v("Tuner", "InstrumentsFragment: observing database: submitting database= ${databaseCopy.size}")
            instrumentsCustomAdapter.submitList(instrumentList)
        }

        instrumentsViewModel.predefinedInstrumentList.observe(viewLifecycleOwner) { instrumentList ->
            instrumentsPredefinedAdapter.submitList(instrumentList)
        }

        instrumentsViewModel.predefinedDatabaseExpanded.observe(viewLifecycleOwner) { expanded ->
            instrumentSectionPredefinedAdapter.expanded = expanded
        }

        instrumentsViewModel.customDatabaseExpanded.observe(viewLifecycleOwner) { expanded ->
            instrumentSectionCustomAdapter.expanded = expanded
        }

        tuningEditorFab = view.findViewById(R.id.tuning_editor_fab)
        tuningEditorFab?.setOnClickListener {
            tuningEditorViewModel.clear(0)
            (requireActivity() as MainActivity).loadTuningEditorFragment()
        }

//        Log.v("Tuner", "InstrumentsFragment.onCreateView: Finished creating view")
        return view
    }
}