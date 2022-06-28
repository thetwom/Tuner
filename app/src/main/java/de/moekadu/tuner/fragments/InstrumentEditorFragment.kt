package de.moekadu.tuner.fragments

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import de.moekadu.tuner.MainActivity
import de.moekadu.tuner.R
import de.moekadu.tuner.dialogs.IconPickerDialogFragment
import de.moekadu.tuner.instruments.instrumentDatabase
import de.moekadu.tuner.preferences.AppPreferences
import de.moekadu.tuner.viewmodels.InstrumentEditorViewModel
import de.moekadu.tuner.viewmodels.InstrumentsViewModel
import de.moekadu.tuner.viewmodels.TunerViewModel
import de.moekadu.tuner.views.DetectedNoteViewer
import de.moekadu.tuner.views.NoteSelector
import de.moekadu.tuner.views.StringView

class InstrumentEditorFragment : Fragment() {
    private val tunerViewModel: TunerViewModel by activityViewModels()
    private val viewModel: InstrumentEditorViewModel by activityViewModels()
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
    // TODO: the clear-icon only appears after typing, but not when getting focus.
    //       This seems an issue with the TextInputLayout???
    private var instrumentNameLayout: TextInputLayout? = null
    private var instrumentNameEditText: TextInputEditText? = null

    private var stringView: StringView? = null
    private var addButton: MaterialButton? = null
    private var deleteButton: MaterialButton? = null
    private var noteSelector: NoteSelector? = null
    private var detectedNoteViewer: DetectedNoteViewer? = null

    /// Instance for requesting audio recording permission.
    /**
     * This will create the sourceJob as soon as the permissions are granted.
     */
    private val askForPermissionAndNotifyViewModel = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        if (result) {
            tunerViewModel.startSampling()
        } else {
            Toast.makeText(activity, getString(R.string.no_audio_recording_permission), Toast.LENGTH_LONG)
                .show()
            Log.v("Tuner", "InstrumentEditorFragment.askForPermissionAnNotifyViewModel: No audio recording permission is granted."
            )
        }
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        setHasOptionsMenu(true)
//        super.onCreate(savedInstanceState)
//    }

//    override fun onPrepareOptionsMenu(menu : Menu) {
//        menu.findItem(R.id.action_settings)?.isVisible = false
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.instrument_editor, container, false)

        stringView = view.findViewById(R.id.string_view)
        addButton = view.findViewById(R.id.button_add_note)
        deleteButton = view.findViewById(R.id.button_delete_note)
        noteSelector = view.findViewById(R.id.note_selector)
        detectedNoteViewer = view.findViewById(R.id.detected_note_viewer)

        instrumentNameLayout = view.findViewById(R.id.instrument_title)
        instrumentNameEditText = view.findViewById(R.id.instrument_title_edit_text)

        instrumentNameLayout?.setStartIconOnClickListener {
            val dialog = IconPickerDialogFragment {
                viewModel.setInstrumentIcon(it)
            }
            dialog.show(childFragmentManager, null)
        }

        instrumentNameEditText?.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
//            Log.v("Tuner", "InstrumentEditorFragment: instrumentNameEditText -> onFocusChanged: hasFocus=$hasFocus")
            if (v.id == R.id.instrument_title_edit_text && !hasFocus) {
                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                imm?.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }

        tunerViewModel.pitchHistoryUpdateInterval.observe(viewLifecycleOwner) {
            detectedNoteViewer?.setApproximateHitNoteUpdateInterval(it)
        }

        tunerViewModel.pitchHistory.historyAveraged.observe(viewLifecycleOwner) {
            tunerViewModel.targetNote.value?.let { targetNote ->
                detectedNoteViewer?.hitNote(targetNote.note)
            }
        }

        tunerViewModel.noteNames.observe(viewLifecycleOwner) {
            updateNoteNamesInAllViews()
        }

        tunerViewModel.preferFlat.observe(viewLifecycleOwner) {
            updateNoteNamesInAllViews()
        }

        viewModel.instrumentName.observe(viewLifecycleOwner) {
//            Log.v("Tuner", "InstrumentEditorFragment: observe instrument name: new = |$it|, before = |${instrumentNameEditText?.text?.trim()}|, different? = ${instrumentNameEditText?.text?.trim()?.contentEquals(it)}")
            //if (instrumentNameEditText?.text?.trim()?.contentEquals(it) == false)
            if (instrumentNameEditText?.text?.contentEquals(it) == false)
                instrumentNameEditText?.setText(it)
        }

        viewModel.iconResourceId.observe(viewLifecycleOwner) {
            instrumentNameLayout?.setStartIconDrawable(it)
        }

        viewModel.strings.observe(viewLifecycleOwner) { strings ->
            val noteNames = tunerViewModel.noteNames.value
            val preferFlat = tunerViewModel.preferFlat.value ?: false
            stringView?.setStrings(strings) { i ->
                noteNames?.getNoteName(requireContext(), i, preferFlat = preferFlat) ?: i.toString()
            }
            val selectedStringIndex = viewModel.selectedStringIndex.value ?: -1
            if (selectedStringIndex in strings.indices)
                noteSelector?.setActiveNote(strings[selectedStringIndex], 150L)
            else
                noteSelector?.setActiveNote(strings.lastOrNull(), 150L)
        }

        viewModel.selectedStringIndex.observe(viewLifecycleOwner) { selectedStringIndex ->
            val strings = viewModel.strings.value
            if (strings != null && selectedStringIndex in strings.indices) {
                val note = strings[selectedStringIndex]
                stringView?.highlightSingleString(selectedStringIndex, 300L)
                noteSelector?.setActiveNote(note, 150L)
            }
        }

        stringView?.stringClickedListener = object : StringView.StringClickedListener {
            override fun onStringClicked(stringIndex: Int, toneIndex: Int) {
                viewModel.selectString(stringIndex)
            }

            override fun onAnchorClicked() { }
            override fun onBackgroundClicked() { }
        }

        addButton?.setOnClickListener {
            viewModel.addStringBelowSelectedAndSelectNewString(noteSelector?.activeNote)
        }

        deleteButton?.setOnClickListener {
            viewModel.deleteSelectedString()
        }

        noteSelector?.noteChangedListener = NoteSelector.NoteChangedListener {
            viewModel.setSelectedStringTo(it)
        }

        detectedNoteViewer?.noteClickedListener = DetectedNoteViewer.NoteClickedListener {
            viewModel.setSelectedStringTo(it)
        }

        instrumentNameEditText?.addTextChangedListener(object :TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
            override fun afterTextChanged(s: Editable?) {
                //viewModel.setInstrumentName(s?.trim())
                viewModel.setInstrumentName(s)
            }
        })

        val actionMode = (requireActivity() as MainActivity).startSupportActionMode(
            InstrumentEditorActionCallback(requireActivity() as MainActivity, instrumentsViewModel, viewModel)
        )
        actionMode?.setTitle(R.string.edit_instrument)

        return view
    }

    override fun onStart() {
        super.onStart()
//        Log.v("Tuner", "InstrumentEditorFragment.onStart()")
        askForPermissionAndNotifyViewModel.launch(Manifest.permission.RECORD_AUDIO)
        tunerViewModel.setInstrument(instrumentDatabase[0])
        tunerViewModel.setTargetNote(-1, TunerViewModel.AUTOMATIC_TARGET_NOTE_DETECTION)
    }

    override fun onStop() {
//        Log.v("Tuner", "InstrumentEditorFragment.onStop()")
        tunerViewModel.stopSampling()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        activity?.let {
            if (it is MainActivity)
                it.setStatusAndNavigationBarColors()
        }
    }

    private fun updateNoteNamesInAllViews() {
        val noteNames = tunerViewModel.noteNames.value ?: return
        val preferFlat = tunerViewModel.preferFlat.value ?: false

        noteSelector?.setNotes(-50, 50) { i ->
            noteNames.getNoteName(requireContext(), i, preferFlat = preferFlat)
        }
        detectedNoteViewer?.setNotes(-50, 50) { i ->
            noteNames.getNoteName(requireContext(), i, preferFlat = preferFlat)
        }
        viewModel.strings.value?.let { strings ->
            stringView?.setStrings(strings) { i ->
                noteNames.getNoteName(requireContext(), i, preferFlat = preferFlat)
            }
        }
    }
}