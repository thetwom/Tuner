package de.moekadu.tuner.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.AdapterView
import android.widget.GridView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.instrumentIcons

class IconPickerDialogFragment : DialogFragment() {

    companion object {
        const val REQUEST_KEY = "IconPickerDialogFragment.request_key"
        const val ICON_KEY = "IconPickerDialogFragment.icon_key"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { act ->
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(act)
            val view = requireActivity().layoutInflater.inflate(R.layout.icon_picker_layout, null)
            val icons = IntArray(instrumentIcons.size) {
                instrumentIcons[it].resourceId
            }

            val adapter = IconPickerAdapter(icons)
            //val recyclerView = view.findViewById<RecyclerView>(R.id.icon_list)
            val gridView = view.findViewById<GridView>(R.id.icon_list)
            gridView?.adapter = adapter
            gridView?.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                val bundle = Bundle(1).apply { putInt(ICON_KEY, icons[position]) }
                setFragmentResult(REQUEST_KEY, bundle)
                dismiss()
            }

            builder.setView(view)
                .setMessage(R.string.pick_icon)
                .setNegativeButton(R.string.abort) { _, _ -> dismiss() }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}