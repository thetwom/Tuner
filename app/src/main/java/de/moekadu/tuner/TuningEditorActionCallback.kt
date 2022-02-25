package de.moekadu.tuner

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode

class TuningEditorActionCallback(private val activity: MainActivity,
                                 private val instrumentsViewModel: InstrumentsViewModel,
                                 private val tuningEditorViewModel: TuningEditorViewModel)
    : ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        //activity.menuInflater.inflate(R.menu.tuning_editor, menu)
        val inflater = mode?.menuInflater
        inflater?.inflate(R.menu.tuning_editor, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_edit_done -> {
                mode?.finish()
                instrumentsViewModel.customInstrumentDatabase.add(tuningEditorViewModel.getInstrument())
                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        activity.onBackPressed()
    }
}