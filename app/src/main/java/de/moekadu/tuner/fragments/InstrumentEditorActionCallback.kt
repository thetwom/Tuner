package de.moekadu.tuner.fragments

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import de.moekadu.tuner.MainActivity
import de.moekadu.tuner.R

class InstrumentEditorActionCallback(private val activity: MainActivity,
                                     private val addOrReplaceInstrument: () -> Unit
                                     //private val instrument: Instrument,
                                     //private val instrumentResources: InstrumentResources
//                                     private val instrumentsViewModel: InstrumentsViewModel,
//                                     private val instrumentEditorViewModel: InstrumentEditorViewModel
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
                //instrumentsViewModel.replaceOrAddCustomInstrument(instrumentEditorViewModel.getInstrument())
                //instrumentResources.replaceOrAddCustomInstrument(instrument)
                addOrReplaceInstrument()
                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
//        Log.v("Tuner", "InstrumentEditorActionCallback: onDestroyActionMode")
        //activity.onBackPressed()
        activity.handleGoBackCommand()
    }
}