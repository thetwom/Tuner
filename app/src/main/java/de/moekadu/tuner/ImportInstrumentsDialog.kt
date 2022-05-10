package de.moekadu.tuner

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult

class ImportInstrumentsDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val instrumentsString = arguments?.getString(INSTRUMENTS_KEY, "") ?: ""
        val instruments = InstrumentDatabase.stringToInstruments(instrumentsString).instruments

        val dialog = AlertDialog.Builder(requireContext()).apply {
            setTitle(context.resources.getQuantityString(R.plurals.load_instruments, instruments.size, instruments.size))
            setNegativeButton(R.string.abort) { _, _ -> dismiss() }
            setItems(R.array.load_instruments_list) { _, which ->
                val array = context.resources.getStringArray(R.array.load_instruments_list)
                val task = when (array[which]) {
                    context.getString(R.string.prepend_current_list) -> InstrumentDatabase.InsertMode.Prepend
                    context.getString(R.string.append_current_list) -> InstrumentDatabase.InsertMode.Append
                    else -> InstrumentDatabase.InsertMode.Replace
                }
                //database.loadInstruments(instruments, task)

                val bundle = Bundle(2)
                bundle.putString(INSERT_MODE_KEY, task.toString())
                bundle.putString(INSTRUMENTS_KEY, instrumentsString)
                setFragmentResult(REQUEST_KEY, bundle)
                //scenesFragment.loadScenes(scenes, task)
            }
        }.create()
        return dialog
    }

    companion object {
        const val REQUEST_KEY = "ImportScenesDialog: import scenes"
        const val INSERT_MODE_KEY = "insert mode"
        const val INSTRUMENTS_KEY = "scenes key"

        fun createInstance(scenesString: String): ImportInstrumentsDialog {
            val dialog = ImportInstrumentsDialog()
            val bundle = Bundle(1)
            bundle.putString(INSTRUMENTS_KEY, scenesString)
            dialog.arguments = bundle
            return dialog
        }
    }
}