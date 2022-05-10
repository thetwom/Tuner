package de.moekadu.tuner.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import de.moekadu.tuner.R

class ResetSettingsDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AlertDialog.Builder(requireActivity()).apply {
            setTitle(R.string.reset_settings_prompt)
            setPositiveButton(R.string.yes) { _, _ ->
                val preferenceEditor =
                    PreferenceManager.getDefaultSharedPreferences(requireActivity()).edit()
                preferenceEditor.clear()
                PreferenceManager.setDefaultValues(requireActivity(), R.xml.preferences, true)
                preferenceEditor.apply()
                // if we don't dismiss the dialog before recreating the activity, it will be
                // reappear directly after activity restart.
                dismiss()
                requireActivity().recreate()
            }
            setNegativeButton(R.string.no) { _, _ -> dismiss() }
        }.create()
        return dialog
    }
}