package de.moekadu.tuner.fragments

import android.content.res.Resources
import android.graphics.Canvas
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import de.moekadu.tuner.instruments.*
import de.moekadu.tuner.preferences.AppPreferences
import de.moekadu.tuner.viewmodels.InstrumentEditorViewModel
import de.moekadu.tuner.viewmodels.InstrumentsViewModel
import de.moekadu.tuner.viewmodels.TunerViewModel
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.roundToInt

class InstrumentsFragment : Fragment() {
    val instrumentsViewModel: InstrumentsViewModel by activityViewModels {
        InstrumentsViewModel.Factory(
            AppPreferences.readInstrumentId(requireActivity()),
            AppPreferences.readInstrumentSection(requireActivity()),
            AppPreferences.readCustomInstruments(requireActivity()),
            AppPreferences.readPredefinedSectionExpanded(requireActivity()),
            AppPreferences.readCustomSectionExpanded(requireActivity()),
            requireActivity().application
        )
    }

    private val instrumentEditorViewModel: InstrumentEditorViewModel by activityViewModels()
    private val tunerViewModel: TunerViewModel by activityViewModels()

    private var recyclerView: RecyclerView? = null
    private val instrumentsPredefinedAdapter = InstrumentsAdapter(InstrumentsAdapter.Mode.Copy)
    private val instrumentsCustomAdapter = InstrumentsAdapter(InstrumentsAdapter.Mode.EditCopy)
    private val instrumentSectionPredefinedAdapter = InstrumentsSectionAdapter(R.string.predefined_instruments)
    private val instrumentSectionCustomAdapter = InstrumentsSectionAdapter(R.string.custom_instruments)

    private var tuningEditorFab: FloatingActionButton? = null

    private var lastRemovedInstrumentIndex = -1
    private var lastRemovedInstrument: Instrument? = null

    private val instrumentArchiving = InstrumentArchiving(this)

    private val deleteIconSpacing = (12f * Resources.getSystem().displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.instruments, menu)
    }
    override fun onPrepareOptionsMenu(menu : Menu) {
//        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_settings)?.isVisible = false
//        menu.findItem(R.id.action_instruments)?.isVisible = false
    }

    override fun onOptionsItemSelected(item : MenuItem) : Boolean {
        when (item.itemId) {
            R.id.action_archive -> {
                if (instrumentsViewModel.customInstrumentDatabase.size == 0) {
                    Toast.makeText(requireContext(), R.string.database_empty, Toast.LENGTH_LONG).show()
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
                    Toast.makeText(requireContext(), R.string.no_instruments_for_sharing, Toast.LENGTH_LONG).show()
                } else {
                    val dialogFragment = InstrumentsSharingDialog(
                        requireContext(),
                        instrumentsViewModel.customInstrumentDatabase.instruments
                    )
                    dialogFragment.show(parentFragmentManager, "tag")
                }

//                val numInstruments = instrumentsViewModel.customInstrumentDatabase.size
//
//                if (instrumentsViewModel.customInstrumentDatabase.size == 0) {
//                    Toast.makeText(requireContext(), R.string.no_instruments_for_sharing, Toast.LENGTH_LONG).show()
//                } else {
//                    val content = instrumentsViewModel.customInstrumentDatabase.getInstrumentsString(context)
//
//                    val sharePath = File(context?.cacheDir, "share").also { it.mkdir() }
//                    val sharedFile = File(sharePath.path, "tuner.txt")
//                    sharedFile.writeBytes(content.toByteArray())
//
//                    val uri = FileProvider.getUriForFile(requireContext(), requireContext().packageName, sharedFile)
//
//                    val shareIntent = Intent().apply {
//                        action = Intent.ACTION_SEND
//                        putExtra(Intent.EXTRA_STREAM, uri)
//                        putExtra(Intent.EXTRA_EMAIL, "")
//                        putExtra(Intent.EXTRA_CC, "")
//                        putExtra(Intent.EXTRA_TITLE, resources.getQuantityString(R.plurals.sharing_num_instruments, numInstruments, numInstruments))
//                        type = "text/plain"
//                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                    }
//                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
//                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        Log.v("Tuner", "InstrumentsFragment.onCreateView: Start creating view")

        val view = inflater.inflate(R.layout.instruments, container, false)

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
            object : InstrumentsAdapter.OnInstrumentClickedListener {
                override fun onInstrumentClicked(instrument: Instrument, stableId: Long) {
//                  Log.v("Tuner", "InstrumentsFragment.onCreateView: new instrument: $instrument")
                    instrumentsViewModel.setInstrument(
                        instrument,
                        InstrumentsViewModel.Section.Predefined
                    )
                    (activity as MainActivity?)?.onBackPressed()
                }

                override fun onEditIconClicked(instrument: Instrument, stableId: Long) {}

                override fun onCopyIconClicked(instrument: Instrument, stableId: Long) {
                    val instrumentCopy = instrument.copy(
                        name = context?.getString(R.string.copy_extension, instrument.getNameString(context)),
                        nameResource = null,
                        stableId = Instrument.NO_STABLE_ID,
                        strings = instrument.strings.copyOf()
                    )
                    instrumentEditorViewModel.setInstrument(instrumentCopy)
                    (requireActivity() as MainActivity).loadTuningEditorFragment()
                }
            }
        instrumentsCustomAdapter.onInstrumentClickedListener =
            object : InstrumentsAdapter.OnInstrumentClickedListener {
                override fun onInstrumentClicked(instrument: Instrument, stableId: Long) {
//            Log.v("Tuner", "InstrumentsFragment.onCreateView: new instrument: $instrument")
                    instrumentsViewModel.setInstrument(instrument,
                        InstrumentsViewModel.Section.Custom
                    )
                    (activity as MainActivity?)?.onBackPressed()
                }

                override fun onEditIconClicked(instrument: Instrument, stableId: Long) {
                    instrumentEditorViewModel.setInstrument(instrument)
                    (requireActivity() as MainActivity).loadTuningEditorFragment()
                }

                override fun onCopyIconClicked(instrument: Instrument, stableId: Long) {
                    val instrumentCopy = instrument.copy(
                        name = context?.getString(R.string.copy_extension, instrument.getNameString(context)),
                        nameResource = null,
                        stableId = Instrument.NO_STABLE_ID,
                        strings = instrument.strings.copyOf()
                    )
                    instrumentEditorViewModel.setInstrument(instrumentCopy)
                    (requireActivity() as MainActivity).loadTuningEditorFragment()
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
                        instrumentsViewModel.customInstrumentDatabase.move(fromPos, toPos)
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
                        instrumentsViewModel.customInstrumentDatabase.remove(
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

        tunerViewModel.noteNames.observe(viewLifecycleOwner) {
            val preferFlat = tunerViewModel.preferFlat.value ?: false
            instrumentsPredefinedAdapter.setNoteNames(it, preferFlat = preferFlat, recyclerView)
            instrumentsCustomAdapter.setNoteNames(it, preferFlat = preferFlat, recyclerView)
        }

        tunerViewModel.preferFlat.observe(viewLifecycleOwner) {
            val noteNames = tunerViewModel.noteNames.value
            instrumentsPredefinedAdapter.setNoteNames(noteNames, preferFlat = it, recyclerView)
            instrumentsCustomAdapter.setNoteNames(noteNames, preferFlat = it, recyclerView)
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

        instrumentsViewModel.uri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                instrumentArchiving.loadInstruments(uri)
                instrumentsViewModel.loadingFileCompleted()
            }
        }

        tuningEditorFab = view.findViewById(R.id.tuning_editor_fab)
        tuningEditorFab?.setOnClickListener {
            instrumentEditorViewModel.clear(0)
            (requireActivity() as MainActivity).loadTuningEditorFragment()
        }

//        Log.v("Tuner", "InstrumentsFragment.onCreateView: Finished creating view")
        return view
    }

    override fun onResume() {
        super.onResume()
        activity?.let {
            it.setTitle(R.string.select_instrument)
            if (it is MainActivity)
                it.setStatusAndNavigationBarColors()
        }
    }
}