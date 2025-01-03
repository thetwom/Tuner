/*
* Copyright 2024 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.moekadu.tuner.misc

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.FileProvider
import de.moekadu.tuner.R
import java.io.File

object ShareData {
    class Contract : ActivityResultContract<Intent, Unit>() {
        override fun createIntent(context: Context, input: Intent): Intent {
            val title = input.getStringExtra(Intent.EXTRA_TITLE)
            return Intent.createChooser(input, title)
        }

        override fun parseResult(resultCode: Int, intent: Intent?) {
            return
        }

    }

    private fun writeDataToCacheFile(context: Context, filename: String, dataAsString: String): Uri {
        val sharePath = File(context.cacheDir, "share").also { it.mkdir() }
        val sharedFile = File(sharePath.path, filename)
        // val fileContent = InstrumentIO.instrumentsListToString(context, instruments)
        sharedFile.writeBytes(dataAsString.toByteArray())
        return FileProvider.getUriForFile(context, context.packageName, sharedFile)
    }

    /** Create an intent to share data.
     * @param context Context.
     * @param filename Name of file, used for sharing. This file will stored in the internal
     *   "share"-folder.
     * @param dataAsString Data to be shared as string.
     * @param numberOfItems Number of items to be shared. Just needed for writing a toast message.
     * @return Intent.
     */
    fun createShareDataIntent(
        context: Context,
        filename: String,
        dataAsString: String,
        numberOfItems: Int
    ): Intent {
        val uri = writeDataToCacheFile(context, filename, dataAsString)

        return Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_EMAIL, "")
            putExtra(Intent.EXTRA_CC, "")
            putExtra(
                Intent.EXTRA_TITLE,
                context.resources.getQuantityString(
                    R.plurals.sharing_num_items, numberOfItems, numberOfItems
                )
            )
            type = "text/plain"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
