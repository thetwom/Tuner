package de.moekadu.tuner

import android.Manifest
import android.content.Context
import android.os.Bundle
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
    // TODO: toolbar should be adapted, removing the back button, but having an "accept"/"abort" button
    // TODO: we must be able to handle two tones, which are equal
    private val tunerViewModel: TunerViewModel by activityViewModels()
    private val viewModel: TuningEditorViewModel by activityViewModels()

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
            instrumentNameLayout?.setStartIconDrawable(R.drawable.ic_piano)
        }
        instrumentNameEditText?.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (v.id == R.id.instrument_title_edit_text && !hasFocus) {
                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                imm?.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }

        // viewModel.pitchHistoryUpdateInterval must go to DetectedNoteViewer
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

        // TODO: connect view model name and icon with TextInputLayout

        viewModel.strings.observe(viewLifecycleOwner) { strings ->
            val tuningFrequencies = tunerViewModel.tuningFrequencies.value
            stringView?.setStrings(strings) { i ->
                tuningFrequencies?.getNoteName(requireContext(), i, preferFlat = false) ?: i.toString()
            }
        }

        viewModel.selectedStringIndex.observe(viewLifecycleOwner) { selectedStringIndex ->
            val numStrings = viewModel.strings.value?.size ?: 0
            if (selectedStringIndex < numStrings) {
                val toneIndex = viewModel.strings.value?.get(selectedStringIndex) ?: 0
                // TODO: check how string view would handle the situation that the toneIndex of a string changes
                stringView?.activeToneIndex = toneIndex
                noteSelector?.setActiveTone(toneIndex, 150L)
            }
        }

        stringView?.stringClickedListener = object : StringView.StringClickedListener {
            override fun onStringClicked(toneIndex: Int) {
                // TODO: this function should better take the array index (at least additionally)
                //       since we could have two times the same tone
                viewModel.selectString(toneIndex)
            }

            override fun onAnchorClicked() { }
            override fun onBackgroundClicked() { }
        }

        addButton?.setOnClickListener {
            viewModel.addStringBelowSelectedAndSelectNewString()
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
        return view
    }


    override fun onStart() {
        super.onStart()
        askForPermissionAndNotifyViewModel.launch(Manifest.permission.RECORD_AUDIO)
        tunerViewModel.setInstrument(instrumentDatabase[0])
        tunerViewModel.setTargetNote()
    }

    override fun onStop() {
        tunerViewModel.stopSampling()
        super.onStop()
    }
}