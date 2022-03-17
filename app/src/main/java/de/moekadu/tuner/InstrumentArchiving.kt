/*
 * Copyright 2020 Michael Moessner
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

package de.moekadu.tuner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AlertDialog
import androidx.core.database.getStringOrNull

class InstrumentArchiving(private val instrumentsFragment: InstrumentsFragment) {
    private val database get() = instrumentsFragment.instrumentsViewModel.customInstrumentDatabase

    private inner class FileWriterContract : ActivityResultContract<String, String?>() {

        override fun createIntent(context: Context, input: String): Intent {
            return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, "tuner.txt")
                // default path
                // putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            val uri = intent?.data
            return saveInstruments(uri)
        }
    }

    private class FileReaderContract : ActivityResultContract<String, Uri?>() {
        override fun createIntent(context: Context, input: String): Intent {
            return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
                // default path
                // putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
//            Log.v("Tuner", "InstrumentsArchiving.FileReaderContract.parseResult: $intent, $resultCode, ${resultCode== Activity.RESULT_OK}")
            return intent?.data
        }
    }

    private val _archiveInstruments = instrumentsFragment.registerForActivityResult(FileWriterContract()) { filename ->
        if (filename == null) {
            Toast.makeText(instrumentsFragment.requireContext(), R.string.failed_to_archive_instruments, Toast.LENGTH_LONG).show()
        } else {
            instrumentsFragment.context?.let { context ->
                Toast.makeText(context, context.getString(R.string.database_saved, filename), Toast.LENGTH_LONG).show()
            }
        }
    }

    private val _unarchiveInstruments = instrumentsFragment.registerForActivityResult(FileReaderContract()) { uri ->
//        Log.v("Tuner", "InstrumentsArchiving._unarchiveInstruments: uri=$uri")
        loadInstruments(uri)
    }

    fun archiveInstruments(instrumentDatabase: InstrumentDatabase?) {
        if (instrumentDatabase?.size ?: 0 == 0) {
            Toast.makeText(instrumentsFragment.requireContext(), R.string.database_empty, Toast.LENGTH_LONG).show()
        } else {
            _archiveInstruments.launch("")
        }
    }

    fun unarchiveInstruments() {
        _unarchiveInstruments.launch("")
    }

    fun saveInstruments(uri: Uri?) : String? {
        val context = instrumentsFragment.context

        if (uri == null || context == null)
            return null

        val fileData = database.getInstrumentsString(context)

        context.contentResolver?.openOutputStream(uri)?.use { stream ->
            stream.write(fileData.toByteArray())
        }
        return getFilenameFromUri(context, uri)
    }

    fun loadInstruments(uri: Uri?){
//        Log.v("Tuner", "InstrumentsArchiving.loadInstruments: uri=$uri")
        val context = instrumentsFragment.context

        if (context != null && uri != null) {
            val filename = getFilenameFromUri(context, uri)
//            Log.v("Tuner", "InstrmentArchiving.loadInstruments: $filename")
            val databaseString = context.contentResolver?.openInputStream(uri)?.use { stream ->
                stream.reader().use {
                    it.readText()
                }
            } ?: return

            val (check, instruments) = InstrumentDatabase.stringToInstruments(databaseString)
            InstrumentDatabase.toastFileCheckString(context, filename, check)

            when {
                check != InstrumentDatabase.FileCheck.Ok -> {
                    return
                }
                database.size == 0 -> {
                    database.loadInstruments(instruments, InstrumentDatabase.InsertMode.Replace)
                }
                else -> {
        //            Log.v("Tuner", "InstrumentArchiving.loadInstruments: filename = $filename")
                    val builder = AlertDialog.Builder(context).apply {
                        setTitle(context.resources.getQuantityString(R.plurals.load_instruments, instruments.size, instruments.size))
                        setNegativeButton(R.string.abort) { dialog, _ -> dialog.dismiss() }
                        setItems(R.array.load_instruments_list) { _, which ->
                            val array = context.resources.getStringArray(R.array.load_instruments_list)
                            val task = when (array[which]) {
                                context.getString(R.string.prepend_current_list) -> InstrumentDatabase.InsertMode.Prepend
                                context.getString(R.string.append_current_list) -> InstrumentDatabase.InsertMode.Append
                                else -> InstrumentDatabase.InsertMode.Replace
                            }
                            database.loadInstruments(instruments, task)
                        }
                    }
                    builder.show()
                }
            }
        }
    }

    private fun getFilenameFromUri(context: Context, uri: Uri): String? {
        var filename: String? = null
        context.contentResolver?.query(
                uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            filename = cursor.getStringOrNull(nameIndex)
            cursor.close()
        }
        return filename
    }
}