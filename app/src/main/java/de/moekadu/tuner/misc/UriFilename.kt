package de.moekadu.tuner.misc

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.database.getStringOrNull

fun getFilenameFromUri(context: Context, uri: Uri): String? {
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