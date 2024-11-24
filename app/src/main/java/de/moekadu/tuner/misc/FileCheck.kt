package de.moekadu.tuner.misc

import android.content.Context
import android.net.Uri
import android.widget.Toast
import de.moekadu.tuner.R

enum class FileCheck { Ok, Empty, Invalid }

fun FileCheck.toastPotentialFileCheckError(context: Context, uri: Uri) {
    when (this) {
        FileCheck.Empty -> {
            val filename = getFilenameFromUri(context, uri)
            Toast.makeText(
                context,
                context.getString(R.string.file_empty, filename),
                Toast.LENGTH_LONG
            ).show()
        }

        FileCheck.Invalid -> {
            val filename = getFilenameFromUri(context, uri)
            Toast.makeText(
                context,
                context.getString(R.string.file_invalid, filename),
                Toast.LENGTH_LONG
            ).show()
        }
        else -> { }
    }
}
