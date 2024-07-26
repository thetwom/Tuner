package de.moekadu.tuner.instruments

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.FileProvider
import de.moekadu.tuner.R
import java.io.File


object ShareInstruments {
    class Contract : ActivityResultContract<Intent, Unit>() {
        override fun createIntent(context: Context, input: Intent): Intent {
            val title = input.getStringExtra(Intent.EXTRA_TITLE)
            return Intent.createChooser(input, title)
        }

        override fun parseResult(resultCode: Int, intent: Intent?) {
            return
        }

    }

    private fun writeInstrumentsToCacheFile(context: Context, instruments: List<Instrument>): Uri {
        val sharePath = File(context.cacheDir, "share").also { it.mkdir() }
        val sharedFile = File(sharePath.path, "tuner.txt")
        val fileContent = InstrumentIO.instrumentsListToString(context, instruments)
        sharedFile.writeBytes(fileContent.toByteArray())

        return FileProvider.getUriForFile(context, context.packageName, sharedFile)
    }

    fun createShareInstrumentsIntent(context: Context, instruments: List<Instrument>): Intent {
        val uri = writeInstrumentsToCacheFile(context, instruments)

        return Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_EMAIL, "")
            putExtra(Intent.EXTRA_CC, "")
            putExtra(
                Intent.EXTRA_TITLE,
                context.resources.getQuantityString(
                    R.plurals.sharing_num_instruments,
                    instruments.size,
                    instruments.size
                )
            )
            type = "text/plain"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}