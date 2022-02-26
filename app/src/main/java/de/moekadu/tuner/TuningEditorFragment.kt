package de.moekadu.tuner

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
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

class TuningEditorFragment : Fragment() {
    // TODO: more instrument icon

    private val tunerViewModel: TunerViewModel by activityViewModels()
    private val viewModel: TuningEditorViewModel by activityViewModels()
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
            Log.v("Tuner", "TuningEditorFragment.askForPermissionAnNotifyViewModel: No audio recording permission is granted."
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onPrepareOptionsMenu(menu : Menu) {
        menu.findItem(R.id.action_settings)?.isVisible = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.tuning_editor, container, false)

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
                detectedNoteViewer?.hitNote(targetNote.toneIndex)
            }
        }

        tunerViewModel.tuningFrequencies.observe(viewLifecycleOwner) { tuningFrequencies ->
            noteSelector?.setNotes(-50, 50) { i ->
                tuningFrequencies.getNoteName(requireContext(), i, preferFlat = false)
            }
            detectedNoteViewer?.setNotes(-50, 50) { i ->
                tuningFrequencies.getNoteName(requireContext(), i, preferFlat = false)
            }
        }

        viewModel.instrumentName.observe(viewLifecycleOwner) {
//            Log.v("Tuner", "TuningEditorFragment: observe instrument name: new = |$it|, before = |${instrumentNameEditText?.text?.trim()}|, different? = ${instrumentNameEditText?.text?.trim()?.contentEquals(it)}")
            //if (instrumentNameEditText?.text?.trim()?.contentEquals(it) == false)
            if (instrumentNameEditText?.text?.contentEquals(it) == false)
                instrumentNameEditText?.setText(it)
        }

        viewModel.iconResourceId.observe(viewLifecycleOwner) {
            instrumentNameLayout?.setStartIconDrawable(it)
        }

        viewModel.strings.observe(viewLifecycleOwner) { strings ->
            val tuningFrequencies = tunerViewModel.tuningFrequencies.value
            stringView?.setStrings(strings) { i ->
                tuningFrequencies?.getNoteName(requireContext(), i, preferFlat = false) ?: i.toString()
            }
            val selectedStringIndex = viewModel.selectedStringIndex.value ?: -1
            if (selectedStringIndex in strings.indices)
                noteSelector?.setActiveTone(strings[selectedStringIndex], 150L)
            else
                noteSelector?.setActiveTone(strings.lastOrNull() ?: 0, 150L)
        }

        viewModel.selectedStringIndex.observe(viewLifecycleOwner) { selectedStringIndex ->
            val strings = viewModel.strings.value
            if (strings != null && selectedStringIndex in strings.indices) {
                val toneIndex = strings[selectedStringIndex]
                stringView?.highlightSingleString(selectedStringIndex, 300L)
                noteSelector?.setActiveTone(toneIndex, 150L)
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
            viewModel.addStringBelowSelectedAndSelectNewString(noteSelector?.activeToneIndex ?: Int.MAX_VALUE)
        }

        deleteButton?.setOnClickListener {
            viewModel.deleteSelectedString()
        }

        noteSelector?.toneChangedListener = NoteSelector.ToneChangedListener {
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

        return view
    }


    override fun onStart() {
        super.onStart()
        askForPermissionAndNotifyViewModel.launch(Manifest.permission.RECORD_AUDIO)
        tunerViewModel.setInstrument(instrumentDatabase[0])
        tunerViewModel.setTargetNote(-1, TunerViewModel.AUTOMATIC_TARGET_NOTE_DETECTION)

        val actionMode = (requireActivity() as MainActivity).startSupportActionMode(
            TuningEditorActionCallback(requireActivity() as MainActivity, instrumentsViewModel, viewModel))
        actionMode?.setTitle(R.string.edit_instrument)
    }

    override fun onStop() {
        tunerViewModel.stopSampling()
        super.onStop()
    }
}