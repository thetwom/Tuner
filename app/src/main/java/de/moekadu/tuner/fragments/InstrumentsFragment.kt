package de.moekadu.tuner.fragments

import android.content.res.Resources
import android.graphics.Canvas
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import de.moekadu.tuner.MainActivity
import de.moekadu.tuner.R
import de.moekadu.tuner.dialogs.ImportInstrumentsDialog
import de.moekadu.tuner.dialogs.InstrumentsSharingDialog
import de.moekadu.tuner.instrumentResources
import de.moekadu.tuner.instruments.*
import de.moekadu.tuner.preferenceResources
import de.moekadu.tuner.viewmodels.InstrumentsViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.roundToInt

class InstrumentsFragment : Fragment() {
    val instrumentsViewModel: InstrumentsViewModel by activityViewModels {
        InstrumentsViewModel.Factory(requireActivity().instrumentResources)
    }

    private var recyclerView: RecyclerView? = null
    private val instrumentsPredefinedAdapter = InstrumentsAdapter(InstrumentsAdapter.Mode.Copy)
    private val instrumentsCustomAdapter = InstrumentsAdapter(InstrumentsAdapter.Mode.EditCopy)
    private val instrumentSectionPredefinedAdapter = InstrumentsSectionAdapter(R.string.predefined_instruments)
    private val instrumentSectionCustomAdapter = InstrumentsSectionAdapter(R.string.custom_instruments)

    private var tuningEditorFab: FloatingActionButton? = null

    private var lastRemovedInstrumentIndex = -1
    private var lastRemovedInstrument: Instrument? = null

    private val instrumentArchiving = InstrumentArchiving(
        { instrumentsViewModel.customInstrumentDatabase} ,
        this, { parentFragmentManager }, { requireContext() }
    )

    private val deleteIconSpacing = (12f * Resources.getSystem().displayMetrics.density).toInt()

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.instruments, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.action_archive -> {
                    if (instrumentsViewModel.customInstrumentDatabase.size == 0) {
                        Toast.makeText(requireContext(), R.string.database_empty, Toast.LENGTH_LONG)
                            .show()
                    } else {
                        instrumentArchiving.archiveInstruments(instrumentsViewModel.customInstrumentDatabase)
                    }
                    return true
                }
                R.id.action_unarchive -> {
                    instrumentArchiving.unarchiveInstruments()
                    return true
                }
                R.id.action_share -> {
                    if (instrumentsViewModel.customInstrumentDatabase.size == 0) {
                        Toast.makeText(
                            requireContext(),
                            R.string.no_instruments_for_sharing,
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val dialogFragment = InstrumentsSharingDialog(
                            requireContext(),
                            instrumentsViewModel.customInstrumentDatabase.instruments
                        )
                        dialogFragment.show(parentFragmentManager, "tag")
                    }
                }
            }
            return false
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        Log.v("Tuner", "InstrumentsFragment.onCreateView: Start creating view")

        val view = inflater.inflate(R.layout.instruments, container, false)

        activity?.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        parentFragmentManager.setFragmentResultListener(ImportInstrumentsDialog.REQUEST_KEY, viewLifecycleOwner) {
                _, bundle ->
            val instrumentsString = bundle.getString(ImportInstrumentsDialog.INSTRUMENTS_KEY, "")
            val instruments = InstrumentDatabase.stringToInstruments(instrumentsString).instruments
            val taskString = bundle.getString(ImportInstrumentsDialog.INSERT_MODE_KEY, InstrumentDatabase.InsertMode.Append.toString())
            val task = InstrumentDatabase.InsertMode.valueOf(taskString)
            instrumentsViewModel.customInstrumentDatabase.loadInstruments(instruments, task)
        }

        recyclerView = view.findViewById(R.id.instrument_list)
        recyclerView?.setHasFixedSize(false)
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())

        instrumentSectionPredefinedAdapter.sectionClickedListener =
            InstrumentsSectionAdapter.SectionClickedListener {
                instrumentsViewModel.expandPredefinedDatabase(
                    !(instrumentsViewModel.instrumentListModel.value.predefinedInstrumentsExpanded)
                )
            }
        instrumentSectionCustomAdapter.sectionClickedListener =
            InstrumentsSectionAdapter.SectionClickedListener {
                instrumentsViewModel.expandCustomDatabase(
                    !(instrumentsViewModel.instrumentListModel.value.customInstrumentsExpanded)
                )
            }

        recyclerView?.adapter = ConcatAdapter(
            instrumentSectionCustomAdapter,
            instrumentsCustomAdapter,
            instrumentSectionPredefinedAdapter,
            instrumentsPredefinedAdapter
        )

        instrumentsPredefinedAdapter.onInstrumentClickedListener =
            object : InstrumentsAdapter.OnInstrumentClickedListener {
                override fun onInstrumentClicked(instrument: Instrument, stableId: Long) {
//                    Log.v("Tuner", "InstrumentsFragment.onCreateView: new instrument: $instrument")
                    instrumentsViewModel.setInstrument(instrument)
                    (activity as MainActivity?)?.handleGoBackCommand()}

                override fun onEditIconClicked(instrument: Instrument, stableId: Long) {}

                override fun onCopyIconClicked(instrument: Instrument, stableId: Long) {
                    val instrumentCopy = instrument.copy(
                        name = context?.getString(R.string.copy_extension, instrument.getNameString(context)),
                        nameResource = null,
                        stableId = Instrument.NO_STABLE_ID,
                        strings = instrument.strings.copyOf()
                    )
                    (requireActivity() as MainActivity).loadTuningEditorFragment(instrumentCopy)
                }
            }
        instrumentsCustomAdapter.onInstrumentClickedListener =
            object : InstrumentsAdapter.OnInstrumentClickedListener {
                override fun onInstrumentClicked(instrument: Instrument, stableId: Long) {
//            Log.v("Tuner", "InstrumentsFragment.onCreateView: new instrument: $instrument")
                    instrumentsViewModel.setInstrument(instrument)
                    (activity as MainActivity?)?.handleGoBackCommand()
                }

                override fun onEditIconClicked(instrument: Instrument, stableId: Long) {
                    (requireActivity() as MainActivity).loadTuningEditorFragment(instrument)
                }

                override fun onCopyIconClicked(instrument: Instrument, stableId: Long) {
                    val instrumentCopy = instrument.copy(
                        name = context?.getString(R.string.copy_extension, instrument.getNameString(context)),
                        nameResource = null,
                        stableId = Instrument.NO_STABLE_ID,
                        strings = instrument.strings.copyOf()
                    )
                    (requireActivity() as MainActivity).loadTuningEditorFragment(instrumentCopy)
                }
            }

        val simpleTouchHelper = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {

            val background = activity?.let {
                ContextCompat.getDrawable(
                    it,
                    R.drawable.instrument_below_background
                )
            }
            val deleteIcon = activity?.let { ContextCompat.getDrawable(it, R.drawable.ic_delete_instrument) }

            override fun getDragDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                //Log.v("Tuner", "InstrumentFragment.simpleTouchHelper.getDragDirs")
                return if (viewHolder is InstrumentsAdapter.ViewHolder && (viewHolder.instrument?.stableId ?: -1) >= 0)
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN
                else
                    0
            }

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                //Log.v("Tuner", "InstrumentFragment.simpleTouchHelper.getSwipeDirs: itemId=${viewHolder.itemId}")
                return if (viewHolder is InstrumentsAdapter.ViewHolder && (viewHolder.instrument?.stableId ?: -1) >= 0)
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                else
                    0
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (viewHolder is InstrumentsAdapter.ViewHolder && target is InstrumentsAdapter.ViewHolder
                    && (viewHolder.instrument?.stableId ?: -1) >= 0 && (target.instrument?.stableId ?: -1) >= 0
                ) {

                    val fromPos = viewHolder.bindingAdapterPosition
                    val toPos = target.bindingAdapterPosition
                    if (fromPos != toPos) {
                        instrumentsViewModel.moveCustomInstrument(fromPos, toPos)
                    }
                    return true
                }
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (viewHolder is InstrumentsAdapter.ViewHolder && (viewHolder.instrument?.stableId ?: -1) >= 0) {
                    lastRemovedInstrumentIndex = viewHolder.bindingAdapterPosition
//                    Log.v("Tuner", "InstrumentsFragment:onSwiped removing index $lastRemovedInstrumentIndex")
                    lastRemovedInstrument =
                        instrumentsViewModel.removeCustomInstrument(
                            lastRemovedInstrumentIndex
                        )

                    (getView() as CoordinatorLayout?)?.let { coLayout ->
                        lastRemovedInstrument?.let { removedItem ->
                            Snackbar.make(
                                coLayout,
                                getString(R.string.instrument_deleted),
                                Snackbar.LENGTH_LONG
                            )
                                .setAction(R.string.undo) {
                                    if (lastRemovedInstrument != null) {
                                        instrumentsViewModel.addCustomInstrument(
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
                if (viewHolder is InstrumentsAdapter.ViewHolder && (viewHolder.instrument?.stableId ?: -1) >= 0) {
                    val itemView = viewHolder.itemView

                    // not sure why, but this method gets called for view holder that are already swiped away
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
                        deleteIcon?.alpha = min(255, (255 * 3 * dX.absoluteValue / itemView.width).toInt())
                        val iconHeight = (0.4f * (itemView.height - itemView.paddingTop - itemView.paddingBottom)).roundToInt()
                        val deleteIconLeft = if (dX < 0f)
                            itemView.right - iconHeight - itemView.paddingRight - deleteIconSpacing
                        else
                            itemView.left + itemView.paddingLeft + deleteIconSpacing
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

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                requireContext().preferenceResources.notePrintOptions.collect {
                    instrumentsPredefinedAdapter.setNotePrintOptions(notePrintOptions = it, recyclerView)
                    instrumentsCustomAdapter.setNotePrintOptions(notePrintOptions = it, recyclerView)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                instrumentsViewModel.instrumentListModel.collect {
                    instrumentSectionPredefinedAdapter.expanded = it.predefinedInstrumentsExpanded
                    instrumentSectionCustomAdapter.expanded = it.customInstrumentsExpanded
                    instrumentSectionCustomAdapter.visible = it.areSectionTitlesVisible
                    instrumentSectionPredefinedAdapter.visible = it.areSectionTitlesVisible

                    instrumentsCustomAdapter.submitList(it.customInstruments)
                    instrumentsPredefinedAdapter.submitList(it.predefinedInstruments)

                    when (it.instrumentAndSection.section) {
                        InstrumentResources.Section.Predefined -> {
                            instrumentsPredefinedAdapter.setActiveStableId(
                                it.instrumentAndSection.instrument.stableId,
                                recyclerView
                            )
                            instrumentsCustomAdapter.setActiveStableId(
                                Instrument.NO_STABLE_ID,
                                recyclerView
                            )
                        }
                        InstrumentResources.Section.Custom -> {
                            instrumentsPredefinedAdapter.setActiveStableId(
                                Instrument.NO_STABLE_ID,
                                recyclerView
                            )
                            instrumentsCustomAdapter.setActiveStableId(
                                it.instrumentAndSection.instrument.stableId,
                                recyclerView
                            )
                        }
                        InstrumentResources.Section.Undefined -> {
                            instrumentsPredefinedAdapter.setActiveStableId(
                                Instrument.NO_STABLE_ID,
                                recyclerView
                            )
                            instrumentsCustomAdapter.setActiveStableId(
                                Instrument.NO_STABLE_ID,
                                recyclerView
                            )
                        }
                    }
                }
            }
        }

        tuningEditorFab = view.findViewById(R.id.tuning_editor_fab)
        tuningEditorFab?.setOnClickListener {
            (requireActivity() as MainActivity).loadTuningEditorFragment(null)
        }

//        Log.v("Tuner", "InstrumentsFragment.onCreateView: Finished creating view")
        return view
    }

    override fun onResume() {
        super.onResume()
        activity?.let {
            it.setTitle(R.string.select_instrument)
            if (it is MainActivity) {
                it.setStatusAndNavigationBarColors()
                it.setPreferenceBarVisibilty(View.VISIBLE)
            }
        }
    }
}