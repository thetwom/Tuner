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
import android.net.Uri
import android.widget.Toast
import de.moekadu.tuner.BuildConfig
import de.moekadu.tuner.R
import de.moekadu.tuner.misc.getFilenameFromUri
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.legacyNoteIndexToNote
import kotlin.math.min

object InstrumentIO {
    /** Get string containing all instruments.
     * @param context Context is only needed, if any of the instruments contain a string
     *   resource as instrument name, instead of an explicit string.
     * @param instruments List with instruments.
     * @return String representation of instruments list.
     */
    fun instrumentsListToString(context: Context?, instruments: List<Instrument>): String {
        return "Version=${BuildConfig.VERSION_NAME}\n\n" + instruments.joinToString(separator = "\n\n") {
            getSingleInstrumentString(
                context,
                it
            )
        }
    }

    enum class FileCheck { Ok, Empty, Invalid }
    enum class InsertMode { Replace, Prepend, Append }

    data class InstrumentsAndFileCheckResult(
        val fileCheck: FileCheck,
        val instruments: List<Instrument>
    )

    fun readInstrumentsFromFile(context: Context, uri: Uri): InstrumentsAndFileCheckResult  {
//        val filename = getFilenameFromUri(context, uri)
//            Log.v("Tuner", "InstrumentArchiving.loadInstruments: $filename")
        val instrumentsString = context.contentResolver?.openInputStream(uri)?.use { stream ->
            stream.reader().use {
                it.readText()
            }
        }
        return if (instrumentsString == null) {
            InstrumentsAndFileCheckResult(FileCheck.Invalid, listOf())
        } else {
            stringToInstruments(instrumentsString)
        }
    }

    fun toastFileLoadingResult(context: Context, readState: FileCheck, uri: Uri) {
        when (readState) {
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

    fun stringToInstruments(instrumentsString: String): InstrumentsAndFileCheckResult {
        val instruments = mutableListOf<Instrument>()

        if (instrumentsString == "")
            return InstrumentsAndFileCheckResult(FileCheck.Empty, instruments)

        val stream = SimpleStream(instrumentsString, 0)

        var version: String? = null
        var numInstrumentsRead = 0
        var nameLength = -1
        var instrumentName = ""
        var stringIndices: IntArray? = null
        var strings: Array<MusicalNote>? = null
        var icon = InstrumentIcon.entries[0]
        var stableId = Instrument.NO_STABLE_ID

        while (!stream.isEos()) {
            stream.advance()
            //                Log.v("Tuner", "InstrumentDatabase.stringToInstruments: found keyword: $k")
            when (readKeyword(stream)) {
                Keyword.Version -> {
                    version = stream.readString()
//                        Log.v("Tuner", "InstrumentDatabase.stringToInstruments: reading version: $version")
                }

                Keyword.NameLength -> {
                    nameLength = stream.readInt() ?: -1
//                        Log.v("Tuner", "InstrumentDatabase.stringToInstruments: reading name length: $nameLength")
                }

                Keyword.Name -> {
                    instrumentName = stream.readString(nameLength)
//                        Log.v("Tuner", "InstrumentDatabase.stringToInstruments: reading name: $instrumentName")
                }

                Keyword.Icon -> {
                    val iconString = stream.readString()
//                    Log.v("Tuner", "InstrumentDatabase.stringToInstruments: reading icon: $iconString, ${iconString.replace(" ", "_")}")
                    icon = iconString.toInstrumentIcon()
                    //iconId = instrumentIconName2Id(stream.readString())
//                        Log.v("Tuner", "InstrumentDatabase.stringToInstruments: reading icon id: $iconId")
                }

                Keyword.Strings -> {
                    strings = stream.readMusicalNoteArray()
//                        Log.v("Tuner", "InstrumentDatabase.stringToInstruments: reading strings: ${strings?.joinToString(separator=";", prefix="[", postfix="]"){it.asString()}}")
                }

                Keyword.Indices -> {
                    stringIndices = stream.readIntArray()
//                        Log.v("Tuner", "InstrumentDatabase.stringToInstruments: reading string indices: ${stringIndices?.joinToString(separator=",", prefix="[", postfix="]")}")
                }

                Keyword.Instrument -> {
                    // string indices were used in older versions, we still allow reading them ....
                    val stringsResolved =
                        strings ?: stringIndices?.map { legacyNoteIndexToNote(it) }?.toTypedArray()
                    stringsResolved?.let {
                        val instrument = Instrument(instrumentName, null, it, icon, stableId)
                        instruments.add(instrument)
                        numInstrumentsRead += 1
                    }
                    nameLength = -1
                    instrumentName = ""
                    strings = null
                    stringIndices = null
                    icon = InstrumentIcon.entries[0]
                    stableId = stream.readLong() ?: Instrument.NO_STABLE_ID
//                        Log.v("Tuner", "InstrumentDatabase.stringToInstruments: reading next instrument")
                }

                Keyword.Invalid -> {}
            }
            stream.goToNextLine()
        }

        // string indices were used in older versions, we still allow reading them ....
        val stringsResolved =
            strings ?: stringIndices?.map { legacyNoteIndexToNote(it) }?.toTypedArray()
        stringsResolved?.let {
            val instrument = Instrument(instrumentName, null, it, icon, stableId)
            instruments.add(instrument)
            numInstrumentsRead += 1
        }

        if (version == null && numInstrumentsRead == 0)
            return InstrumentsAndFileCheckResult(FileCheck.Invalid, instruments)
        return InstrumentsAndFileCheckResult(FileCheck.Ok, instruments)
    }

    private val keywords = arrayOf("Version=", "Instrument", "Length of name=", "Name=", "Icon=", "String indices=", "Strings=")

    private enum class Keyword {Version, Instrument, NameLength, Name, Icon, Indices, Strings, Invalid}

    /** Get string representation of a single instrument.
     * @param context Context is only needed, if the instrument name is a string resource.
     * @return Instrument string representation.
     */
    private fun getSingleInstrumentString(context: Context?, instrument: Instrument): String {
        val name = instrument.getNameString(context)
        return "Instrument ${instrument.stableId}\n" +
                "Length of name=${name.length}\n" +
                "Name=$name\n" +
                "Icon=${instrument.icon.name}\n" +
                "Strings=${
                    instrument.strings.joinToString(
                        separator = ";",
                        prefix = "[",
                        postfix = "]"
                    ) { it.asString() }
                }\n"
    }

    private class SimpleStream(val string: String, var pos: Int) {
        /** Advance to next character which is not a white space. */
        fun advance() {
            while (pos < string.length && string[pos].isWhitespace())
                ++pos
        }
        fun goToNextLine() {
            while (pos < string.length && string[pos] != '\n')
                ++pos
            if (pos < string.length)
                ++pos
        }
        fun isEos() = pos >= string.length

        /** Read next coming string ended by a whitespace character. */
        fun readString(): String {
            advance()
            val posStart = pos
            while (pos < string.length && !string[pos].isWhitespace())
                ++pos
            return if (pos > posStart)
                string.substring(posStart, pos)
            else
                ""
        }
        /** Read string where the exact number of characters is given. */
        fun readString(numCharacters: Int): String {
            val posStart = pos
            val res = if (numCharacters >= 0) {
                pos += numCharacters
                pos = min(pos, string.length)
                if (pos < string.length)
                    string.substring(posStart, pos)
                else
                    ""
            } else {
                while (pos < string.length && string[pos] != '\n')
                    ++pos
                if (pos < string.length)
                    string.substring(posStart, pos).trim()
                else
                    ""
            }
            return res
        }

        fun readInt(): Int? {
            val intAsString = readString()
            return intAsString.toIntOrNull()
        }
        fun readLong(): Long? {
            val longAsString = readString()
            return longAsString.toLongOrNull()
        }

        /** Read an array of integers.
         * The array is expected to be enclosed in "[" and "]" and the int numbers should be
         * separated by ",".
         * * @return Int array of numbers or null if no array available.
         */
        fun readIntArray(): IntArray? {
            advance()
            if (pos >= string.length || string[pos] != '[')
                return null
//                Log.v("Tuner", "InstrumentDatabase.readIntArray. string[pos]=${string[pos]}")
            val posStart = pos
            while (pos < string.length && string[pos] != '\n' && string[pos] != ']')
                ++pos
            if (string[pos] != ']')
                return null
            ++pos
            if (pos - 1 <= posStart + 1) // allow empty int lists
                return intArrayOf()
            val intArrayAsString = string.substring(posStart + 1, pos - 1)
            return try {
                intArrayAsString.split(",").map { it.trim().toInt() }.toIntArray()
            } catch (e: NumberFormatException) {
                null
            }
        }
        /** Read an array of musical notes.
         * The array is expected to be enclosed in "[" and "]", the string representation
         * of the notes must be created with MusicalNote.asString(), and the different
         * notes must be separated by ";".
         * @return Array of musical notes or null if no array available.
         */
        fun readMusicalNoteArray(): Array<MusicalNote>? {
            advance()
            if (pos >= string.length || string[pos] != '[')
                return null
//                Log.v("Tuner", "InstrumentDatabase.readIntArray. string[pos]=${string[pos]}")
            val posStart = pos
            while (pos < string.length && string[pos] != '\n' && string[pos] != ']')
                ++pos
            if (string[pos] != ']')
                return null
            ++pos
            if (pos - 1 <= posStart + 1) // allow empty int lists
                return arrayOf()
            val musicalNoteArrayAsString = string.substring(posStart + 1, pos - 1)
            return try {
                musicalNoteArrayAsString.split(";").map { MusicalNote.fromString(it.trim()) }.toTypedArray()
            } catch (e: NumberFormatException) {
                null
            }
        }
    }

    private fun readKeyword(input: SimpleStream): Keyword {
        var res: String? = null
        for (k in keywords) {
            if (input.string.startsWith(k, input.pos)) {
                res = k
                break
            }
        }
        res?.let { input.pos += it.length }
//            Log.v("Tuner", "InstrumentDatabase.SimpleStream.readKeyword: res=$res")
        return when (res) {
            "Version=" -> Keyword.Version
            "Instrument" -> Keyword.Instrument
            "Length of name=" -> Keyword.NameLength
            "Name=" -> Keyword.Name
            "Icon=" -> Keyword.Icon
            "Strings=" -> Keyword.Strings
            "String indices=" -> Keyword.Indices
            else -> Keyword.Invalid
        }
    }

}