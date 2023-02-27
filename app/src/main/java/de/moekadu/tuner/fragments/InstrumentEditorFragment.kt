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
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import de.moekadu.tuner.MainActivity
import de.moekadu.tuner.R
import de.moekadu.tuner.dialogs.IconPickerDialogFragment
import de.moekadu.tuner.instrumentResources
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.preferenceResources
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.viewmodels.InstrumentEditorViewModel
import de.moekadu.tuner.views.DetectedNoteViewer
import de.moekadu.tuner.views.NoteSelector
import de.moekadu.tuner.views.StringView

class InstrumentEditorFragment : Fragment() {

    private val viewModel: InstrumentEditorViewModel by viewModels {
        InstrumentEditorViewModel.Factory(requireActivity().preferenceResources)
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

    private var stringViewChangeId = -1
    private var noteSelectorChangeId = -1
    private var detectedNoteViewChangeId = -1

    /// Instance for requesting audio recording permission.
    /**
     * This will create the sourceJob as soon as the permissions are granted.
     */
    private val askForPermissionAndNotifyViewModel = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        if (result) {
            //tunerViewModel.startSampling()
            viewModel.startSampling()
        } else {
            Toast.makeText(activity, getString(R.string.no_audio_recording_permission), Toast.LENGTH_LONG)
                .show()
            Log.v("Tuner", "InstrumentEditorFragment.askForPermissionAnNotifyViewModel: No audio recording permission is granted."
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.instrument_editor, container, false)
//        Log.v("Tuner", "InstrumentEditorFragment: onCreateView")
        val initialInstrument = arguments?.getParcelable<Instrument?>(INSTRUMENT_KEY)
        if (initialInstrument == null) {
            viewModel.setDefaultInstrument()
        } else {
//            Log.v("Tuner", "InstrumentEditorFragment: onCreateView: initial instrument = $it")
            viewModel.setInstrument(initialInstrument, context)
        }

        parentFragmentManager.setFragmentResultListener(IconPickerDialogFragment.REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            viewModel.setInstrumentIcon(bundle.getInt(IconPickerDialogFragment.ICON_KEY))
        }

        stringView = view.findViewById(R.id.string_view)
        addButton = view.findViewById(R.id.button_add_note)
        deleteButton = view.findViewById(R.id.button_delete_note)
        noteSelector = view.findViewById(R.id.note_selector)
        detectedNoteViewer = view.findViewById(R.id.detected_note_viewer)

        instrumentNameLayout = view.findViewById(R.id.instrument_title)
        instrumentNameEditText = view.findViewById(R.id.instrument_title_edit_text)

        instrumentNameLayout?.setStartIconOnClickListener {
            val dialog = IconPickerDialogFragment()
            dialog.show(parentFragmentManager, null)
        }

        instrumentNameEditText?.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
//            Log.v("Tuner", "InstrumentEditorFragment: instrumentNameEditText -> onFocusChanged: hasFocus=$hasFocus")
            if (v.id == R.id.instrument_title_edit_text && !hasFocus) {
                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                imm?.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }

//        INFO: the following lines have been temporarily switched off
//        tunerViewModel.pitchHistoryUpdateInterval.observe(viewLifecycleOwner) {
//            detectedNoteViewer?.setApproximateHitNoteUpdateInterval(it)
//        }
//
//        tunerViewModel.pitchHistory.historyAveraged.observe(viewLifecycleOwner) {
//            tunerViewModel.targetNote.value?.let { targetNote ->
//                detectedNoteViewer?.hitNote(targetNote.note)
//            }
//        }
//
//        tunerViewModel.musicalScale.observe(viewLifecycleOwner) {
//            updateNoteNamesInAllViews()
//        }
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                requireContext().preferenceResources.notePrintOptions.collect {
//                    updateNoteNamesInAllViews()
//                }
//            }
//        }

        viewModel.instrumentNameModel.observe(viewLifecycleOwner) {
//            Log.v("Tuner", "InstrumentEditorFragment: observe instrument name: new = |$it|, before = |${instrumentNameEditText?.text?.trim()}|, different? = ${instrumentNameEditText?.text?.trim()?.contentEquals(it)}")
            //if (instrumentNameEditText?.text?.trim()?.contentEquals(it) == false)
            if (instrumentNameEditText?.text?.contentEquals(it.name) == false)
                instrumentNameEditText?.setText(it.name)
            instrumentNameLayout?.setStartIconDrawable(it.iconResourceId)
        }

//        viewModel.iconResourceId.observe(viewLifecycleOwner) {
//            instrumentNameLayout?.setStartIconDrawable(it)
//        }

//        INFO: The following lines have been temporarily switched off
//        viewModel.strings.observe(viewLifecycleOwner) { strings ->
//            //val noteNames = tunerViewModel.noteNames.value
//            tunerViewModel.musicalScale.value?.let { musicalScale ->
//                stringView?.setStrings(
//                    strings,
//                    isChromatic = false,
//                    musicalScale.noteNameScale,
//                    musicalScale.noteIndexBegin,
//                    musicalScale.noteIndexEnd,
//                    requireContext().preferenceResources.notePrintOptions.value
//                )
//                val selectedStringIndex = viewModel.selectedStringIndex.value ?: -1
//                if (selectedStringIndex in strings.indices)
//                    noteSelector?.setActiveNote(strings[selectedStringIndex], 150L)
//                else
//                    noteSelector?.setActiveNote(strings.lastOrNull(), 150L)
//            }
//        }

//        viewModel.selectedStringIndex.observe(viewLifecycleOwner) { selectedStringIndex ->
//            val strings = viewModel.strings.value
//            if (strings != null && selectedStringIndex in strings.indices) {
//                val note = strings[selectedStringIndex]
//                stringView?.highlightSingleString(selectedStringIndex, 300L)
//                noteSelector?.setActiveNote(note, 150L)
//            }
//        }
        viewModel.stringViewModel.observe(viewLifecycleOwner) { model ->
            if (model.changeId < stringViewChangeId)
                stringViewChangeId = -1

            val printer = model.noteNamePrinter
            if (model.stringChangedId > stringViewChangeId && printer != null) {
                stringView?.setStrings(
                    model.strings,
                    isChromatic = false,
                    model.musicalScale.noteNameScale,
                    model.musicalScale.noteIndexBegin,
                    model.musicalScale.noteIndexEnd,
                    printer
                )
            }

            if (model.settingsChangedId > stringViewChangeId && model.selectedStringIndex in model.strings.indices) {
                stringView?.highlightSingleString(model.selectedStringIndex, 300L)
            }
                //            if (strings != null && selectedStringIndex in strings.indices) {
//                val note = strings[selectedStringIndex]
//                stringView?.highlightSingleString(selectedStringIndex, 300L)
//                noteSelector?.setActiveNote(note, 150L)
//            }

            stringViewChangeId = model.changeId
        }

        stringView?.stringClickedListener = object : StringView.StringClickedListener {
            override fun onStringClicked(stringIndex: Int, note: MusicalNote) {
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

        viewModel.noteSelectorModel.observe(viewLifecycleOwner) { model ->
            if (model.changeId < noteSelectorChangeId)
                noteSelectorChangeId = -1

            val printer = model.noteNamePrinter
            if (model.scaleChangeId > noteSelectorChangeId && printer != null) {
                noteSelector?.setNotes(
                    model.musicalScale.noteIndexBegin, model.musicalScale.noteIndexEnd,
                    model.musicalScale.noteNameScale, null, printer)
            }
            if (model.selectedNoteId > noteSelectorChangeId) {
                noteSelector?.setActiveNote(model.selectedNote, 150L)
            }
            noteSelectorChangeId = model.changeId
        }
        noteSelector?.noteChangedListener = NoteSelector.NoteChangedListener {
            viewModel.setSelectedStringTo(it)
        }

        viewModel.detectedNoteModel.observe(viewLifecycleOwner) { model ->
            if (model.changeId < detectedNoteViewChangeId)
                detectedNoteViewChangeId = -1

            val printer = model.noteNamePrinter
            if (model.scaleChangeId > detectedNoteViewChangeId && printer != null) {
                detectedNoteViewer?.setNotes(
                    model.musicalScale.noteNameScale,
                    model.musicalScale.noteIndexBegin, model.musicalScale.noteIndexEnd,
                    printer
                )
            }
            if (model.noteUpdateIntervalChangedId > detectedNoteViewChangeId) {
                detectedNoteViewer?.setApproximateHitNoteUpdateInterval(model.noteUpdateInterval)
            }
            if (model.noteChangedId > detectedNoteViewChangeId) {
                detectedNoteViewer?.hitNote(model.note)
            }
            detectedNoteViewChangeId = model.changeId
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
            //InstrumentEditorActionCallback(requireActivity() as MainActivity, instrumentsViewModel, viewModel)
            InstrumentEditorActionCallback(
                requireActivity() as MainActivity,
                addOrReplaceInstrument = {
                    activity?.instrumentResources?.replaceOrAddCustomInstrument(viewModel.getInstrument())
                }
            )
        )
        actionMode?.setTitle(R.string.edit_instrument)

        return view
    }

    override fun onStart() {
        super.onStart()
//        Log.v("Tuner", "InstrumentEditorFragment.onStart()")
        askForPermissionAndNotifyViewModel.launch(Manifest.permission.RECORD_AUDIO)
    }

    override fun onStop() {
//        Log.v("Tuner", "InstrumentEditorFragment.onStop()")
        //tunerViewModel.stopSampling()
        viewModel.stopSampling()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        activity?.let {
            if (it is MainActivity) {
                it.setStatusAndNavigationBarColors()
                it.setPreferenceBarVisibilty(View.VISIBLE)
            }
        }
    }

//    INFO: The following lines have been temporarily switched off
//    private fun updateNoteNamesInAllViews() {
//        //val noteNames = tunerViewModel.noteNames.value ?: return
//        val notePrintOptions = requireContext().preferenceResources.notePrintOptions.value
//        val musicalScale = tunerViewModel.musicalScale.value ?: return
//
//        noteSelector?.setNotes(musicalScale.noteIndexBegin, musicalScale.noteIndexEnd,
//            musicalScale.noteNameScale, null, notePrintOptions)
//        detectedNoteViewer?.setNotes(
//            musicalScale.noteNameScale, musicalScale.noteIndexBegin, musicalScale.noteIndexEnd, notePrintOptions)
//        viewModel.strings.value?.let { strings ->
//            stringView?.setStrings(strings, false, musicalScale.noteNameScale,
//                musicalScale.noteIndexBegin, musicalScale.noteIndexEnd, notePrintOptions)
//        }
//    }

    companion object {
        const val INSTRUMENT_KEY = "instrument"
    }
}
