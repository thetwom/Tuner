package de.moekadu.tuner

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class AboutDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.about)
            setMessage(getString(R.string.about_message, BuildConfig.VERSION_NAME))
            setNegativeButton(R.string.acknowledged) { _, _ -> dismiss()}
        }.create()
        return dialog
    }
}