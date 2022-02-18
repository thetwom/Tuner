package de.moekadu.tuner

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.AdapterView
import android.widget.GridView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class IconPickerDialogFragment(private val iconResourceSelectedListener: IconResourceSelectedListener)
    : DialogFragment() {

    fun interface IconResourceSelectedListener {
        fun onResourceSelected(resourceId: Int)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)

            val view = requireActivity().layoutInflater.inflate(R.layout.icon_picker_layout, null)

            val icons = intArrayOf(
                R.drawable.ic_piano,
                R.drawable.ic_guitar,
                R.drawable.ic_ukulele,
                R.drawable.ic_bass,
                R.drawable.ic_violin
            )
            
            val adapter = IconPickerAdapter(icons)
            //val recyclerView = view.findViewById<RecyclerView>(R.id.icon_list)
            val gridView = view.findViewById<GridView>(R.id.icon_list)
            gridView?.adapter = adapter
            gridView?.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                iconResourceSelectedListener.onResourceSelected(icons[position])
                dismiss()
            }

            //recyclerView?.layoutManager = GridLayoutManager(requireContext(), 3)
            //recyclerView?.adapter = adapter

            builder.setView(view)
                .setMessage(R.string.pick_icon)
                .setNegativeButton(R.string.abort) { _, _ -> dismiss() }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}