package de.moekadu.tuner.fragments

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import de.moekadu.tuner.MainActivity
import de.moekadu.tuner.R
import de.moekadu.tuner.viewmodels.InstrumentEditorViewModel
import de.moekadu.tuner.viewmodels.InstrumentsViewModel

class InstrumentEditorActionCallback(private val activity: MainActivity,
                                     private val instrumentsViewModel: InstrumentsViewModel,
                                     private val instrumentEditorViewModel: InstrumentEditorViewModel
)
    : ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        val inflater = mode?.menuInflater
        inflater?.inflate(R.menu.instrument_editor, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_edit_done -> {
                mode?.finish()
                //instrumentsViewModel.customInstrumentDatabase.add(instrumentEditorViewModel.getInstrument())
                instrumentsViewModel.customInstrumentDatabase.replaceOrAdd(instrumentEditorViewModel.getInstrument())
                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
//        Log.v("Tuner", "InstrumentEditorActionCallback: onDestroyActionMode")
        activity.onBackPressed()
    }
}