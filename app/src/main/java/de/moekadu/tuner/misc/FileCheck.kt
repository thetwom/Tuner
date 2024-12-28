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
