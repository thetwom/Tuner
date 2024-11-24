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
package de.moekadu.tuner.instruments

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.FileProvider
import de.moekadu.tuner.R
import java.io.File


//object ShareInstruments {
//    class Contract : ActivityResultContract<Intent, Unit>() {
//        override fun createIntent(context: Context, input: Intent): Intent {
//            val title = input.getStringExtra(Intent.EXTRA_TITLE)
//            return Intent.createChooser(input, title)
//        }
//
//        override fun parseResult(resultCode: Int, intent: Intent?) {
//            return
//        }
//
//    }
//
//    private fun writeInstrumentsToCacheFile(context: Context, instruments: List<Instrument>): Uri {
//        val sharePath = File(context.cacheDir, "share").also { it.mkdir() }
//        val sharedFile = File(sharePath.path, "tuner.txt")
//        val fileContent = InstrumentIO.instrumentsListToString(context, instruments)
//        sharedFile.writeBytes(fileContent.toByteArray())
//        return FileProvider.getUriForFile(context, context.packageName, sharedFile)
//    }
//
//    fun createShareInstrumentsIntent(context: Context, instruments: List<Instrument>): Intent {
//        val uri = writeInstrumentsToCacheFile(context, instruments)
//
//        return Intent(Intent.ACTION_SEND).apply {
//            putExtra(Intent.EXTRA_STREAM, uri)
//            putExtra(Intent.EXTRA_EMAIL, "")
//            putExtra(Intent.EXTRA_CC, "")
//            putExtra(
//                Intent.EXTRA_TITLE,
//                context.resources.getQuantityString(
//                    R.plurals.sharing_num_items,
//                    instruments.size,
//                    instruments.size
//                )
//            )
//            type = "text/plain"
//            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        }
//    }
//}