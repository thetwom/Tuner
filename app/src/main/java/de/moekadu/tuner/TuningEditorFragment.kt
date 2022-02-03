package de.moekadu.tuner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

class TuningEditorFragment : Fragment() {
    private val viewModel: TunerViewModel by activityViewModels()

    private var noteSelector: NoteSelector? = null

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

        noteSelector = view.findViewById(R.id.note_selector)

        viewModel.tuningFrequencies.observe(viewLifecycleOwner) { tuningFrequencies ->
            noteSelector?.setNotes(-50, 50) { i ->
                tuningFrequencies.getNoteName(requireContext(), i, preferFlat = false)
            }
        }
        return view
    }

}