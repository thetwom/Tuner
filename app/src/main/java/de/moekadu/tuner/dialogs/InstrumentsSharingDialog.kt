/*
 * Copyright 2022 Michael Moessner
 *
 * This file is part of Metronome.
 *
 * Metronome is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metronome is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Metronome.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.moekadu.tuner.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentDatabase
import kotlinx.parcelize.Parcelize
import java.io.File

class InstrumentsSharingDialog() : DialogFragment() {
    private var adapter: InstrumentsSharingDialogAdapter? = null
    private var instrumentsString: String? = null

    @Parcelize
    private class SavedState(val checkedList: List<Boolean>?, val instrumentsString: String?) : Parcelable

    constructor(context: Context, instruments: List<Instrument>) : this() {
        adapter = InstrumentsSharingDialogAdapter(instruments)
        instrumentsString = InstrumentDatabase.getInstrumentsString(context, instruments)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val state = SavedState(adapter?.getStateOfEachInstrument(), instrumentsString)
        outState.putParcelable("instruments sharing dialog fragment state", state)
        super.onSaveInstanceState(outState)
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.let {
            it.getParcelable<SavedState>("instruments sharing dialog fragment state")?.let { storedState ->
                if (storedState.checkedList != null && storedState.instrumentsString != null) {
                    instrumentsString = storedState.instrumentsString
                    val instruments = InstrumentDatabase.stringToInstruments(storedState.instrumentsString).instruments
                    adapter = InstrumentsSharingDialogAdapter(instruments)
                    adapter?.setStateOfEachInstrument(storedState.checkedList)
                }
            }
        }

        return AlertDialog.Builder(requireContext()).apply {
            // root must be null here since alert dialog does not provide a root view
            val v = layoutInflater.inflate(R.layout.select_instruments_dialog, null)
            val r = v.findViewById<RecyclerView>(R.id.instruments_list)
            r?.layoutManager = LinearLayoutManager(v.context)
            r?.adapter = adapter
            setTitle(R.string.select_instruments)
            setView(v)
            setPositiveButton(R.string.share2) { _, _ ->
                val instrumentsForSharing = adapter?.getInstrumentsToBeShared()
                val numInstruments = instrumentsForSharing?.size ?: 0
                if (numInstruments == 0 || instrumentsForSharing == null) {
                    Toast.makeText(requireContext(), R.string.no_instruments_selected, Toast.LENGTH_LONG).show()
                } else {
                    val content = InstrumentDatabase.getInstrumentsString(requireContext(), instrumentsForSharing)

                    val sharePath = File(requireContext().cacheDir, "share").also { it.mkdir() }
                    val sharedFile = File(sharePath.path, "tuner.txt")
                    sharedFile.writeBytes(content.toByteArray())

                    val uri = FileProvider.getUriForFile(requireContext(), requireContext().packageName, sharedFile)

                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_EMAIL, "")
                        putExtra(Intent.EXTRA_CC, "")
                        putExtra(Intent.EXTRA_TITLE, resources.getQuantityString(R.plurals.sharing_num_instruments, numInstruments, numInstruments))
                        type = "text/plain"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
                }
            }
            setNegativeButton(R.string.abort) { _, _ -> dismiss()}
        }.create()
    }
}