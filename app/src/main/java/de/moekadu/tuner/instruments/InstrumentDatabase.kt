package de.moekadu.tuner.instruments

import android.content.Context
import android.widget.Toast
import de.moekadu.tuner.BuildConfig
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.legacyNoteIndexToNote
import kotlin.math.min

class InstrumentDatabase {
    private val _instruments = mutableListOf<Instrument>()
    val instruments: List<Instrument> get() = _instruments
    val size get() = _instruments.size

    private val stableIds = mutableSetOf<Long>()

    fun interface DatabaseChangedListener {
        fun onChanged(sceneDatabase: InstrumentDatabase)
    }
    var databaseChangedListener: DatabaseChangedListener? = null

    private fun getNewStableId(): Long {
        var newId = 0L
        while (stableIds.contains(newId)) {
            ++newId
        }
        return newId
    }

    fun getInstrument(stableId: Long): Instrument? {
        return instruments.firstOrNull { it.stableId == stableId }
    }

    fun remove(position: Int) : Instrument {
        if (BuildConfig.DEBUG && position >= _instruments.size)
            throw RuntimeException("Invalid position")

        val instrument = _instruments.removeAt(position)
        stableIds.remove(instrument.stableId)
        databaseChangedListener?.onChanged(this)
        return instrument
    }

    fun move(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex >= _instruments.size)
            return
        val instrument = _instruments.removeAt(fromIndex)
        val toIndexCorrected = min(toIndex, _instruments.size)
        _instruments.add(toIndexCorrected, instrument)
        databaseChangedListener?.onChanged(this)
    }

    fun add(instrument: Instrument, callDatabaseChangedListener: Boolean = true): Long {
        //Log.v("Tuner", "InstrumentDatabase.add: Adding: ${instrument.getNameString(null)}")
        return add(instruments.size, instrument, callDatabaseChangedListener)
    }

    fun add(position: Int, instrument: Instrument, callDatabaseChangedListener: Boolean = true) : Long {
//        Log.v("Tuner", "InstrumentDatabase.add: Adding: ${instrument.getNameString(null)}")
        val positionCorrected = min(position, _instruments.size)

        // keep the scene stable id if possible else create a new one
        val stableId = if (instrument.stableId == Instrument.NO_STABLE_ID || stableIds.contains(instrument.stableId)) {
            val newInstrument = instrument.copy(stableId = getNewStableId())
            _instruments.add(positionCorrected, newInstrument)
            stableIds.add(newInstrument.stableId)
            newInstrument.stableId
        }
        else {
            _instruments.add(positionCorrected, instrument)
            stableIds.add(instrument.stableId)
            instrument.stableId
        }

        if (callDatabaseChangedListener)
            databaseChangedListener?.onChanged(this)
        return stableId
    }

    /// Replace when instrument with stable id exists, else add
    fun replaceOrAdd(instrument: Instrument, callDatabaseChangedListener: Boolean = true) {
        val index = instruments.indexOfFirst { it.stableId == instrument.stableId }
        if (index >= 0)
            _instruments[index] = instrument
        else
            add(instrument, false)
        if (callDatabaseChangedListener)
            databaseChangedListener?.onChanged(this)
    }

    /** Get string containing all instruments.
     * @param context Context is only needed, if any of the instruments in the database contain a string
     *   resource as instrument name, instead of an explicit string.
     * @return String representation of instruments list.
     */
    fun getInstrumentsString(context: Context?) : String {
//        Log.v("Metronome", "SceneDatabase.getSceneString: string= ${stringBuilder}")
//        return "Version=${BuildConfig.VERSION_NAME}\n\n" + instruments.joinToString(separator = "\n\n"){ getSingleInstrumentString(context, it)}
        return getInstrumentsString(context, instruments)
    }

    fun loadInstruments(newInstruments: List<Instrument>, mode: InsertMode = InsertMode.Replace): FileCheck {

        when (mode) {
            InsertMode.Replace -> {
                _instruments.clear()
                stableIds.clear()
                newInstruments.forEach { add(it, false) }
            }
            InsertMode.Prepend -> {
                for (i in newInstruments.indices)
                    add(i, newInstruments[i], false)
            }
            InsertMode.Append -> {
                newInstruments.forEach { add(it, false) }
            }
        }

        databaseChangedListener?.onChanged(this)
        return FileCheck.Ok
    }

    fun clear() {
        _instruments.clear()
        stableIds.clear()
        databaseChangedListener?.onChanged(this)
    }

    fun getCopyOfInstrumentsList(): ArrayList<Instrument> {
        val listCopy = ArrayList<Instrument>(size)
        instruments.forEach { listCopy.add(it.copy()) }
        return listCopy
    }

    enum class FileCheck {Ok, Empty, Invalid}
    enum class InsertMode {Replace, Prepend, Append}

    data class InstrumentsAndFileCheckResult(val fileCheck: FileCheck, val instruments: List<Instrument>)

    companion object {
        private val keywords = arrayOf("Version=", "Instrument", "Length of name=", "Name=", "Icon=", "String indices=", "Strings=")
        private enum class Keyword {Version, Instrument, NameLength, Name, Icon, Indices, Strings, Invalid}
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
            /** Read string where the exat number of characters is given. */
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

        /** Get string representation of a single instrument.
         * @param context Context is only needed, if the instrument name is a string resource.
         * @return Instrument string representation.
         */
        private fun getSingleInstrumentString(context: Context?, instrument: Instrument): String {
            val name = instrument.getNameString(context)
            val iconName = instrumentIconId2Name(instrument.iconResource)
            return "Instrument ${instrument.stableId}\n" +
                    "Length of name=${name.length}\n" +
                    "Name=$name\n" +
                    "Icon=$iconName\n" +
                    "Strings=${
                        instrument.strings.joinToString(
                            separator = ";",
                            prefix = "[",
                            postfix = "]"
                        ) { it.asString() }
                    }\n"
        }

        /** Get string containing all instruments.
         * @param context Context is only needed, if any of the instruments contain a string
         *   resource as instrument name, instead of an explicit string.
         * @param instruments List with instruments.
         * @return String representation of instruments list.
         */
        fun getInstrumentsString(context: Context?, instruments: List<Instrument>) : String {
//        Log.v("Metronome", "SceneDatabase.getSceneString: string= ${stringBuilder}")
            return "Version=${BuildConfig.VERSION_NAME}\n\n" + instruments.joinToString(separator = "\n\n"){ getSingleInstrumentString(context, it)}
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
            var iconId = instrumentIcons[0].resourceId
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
                        iconId = instrumentIconName2Id(stream.readString())
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
                        val stringsResolved = strings ?: stringIndices?.map { legacyNoteIndexToNote(it) }?.toTypedArray()
                        stringsResolved?.let {
                            val instrument = Instrument(instrumentName, null, it, iconId, stableId)
                            instruments.add(instrument)
                            numInstrumentsRead += 1
                        }
                        nameLength = -1
                        instrumentName = ""
                        strings = null
                        stringIndices = null
                        iconId = instrumentIcons[0].resourceId
                        stableId = stream.readLong() ?: Instrument.NO_STABLE_ID
//                        Log.v("Tuner", "InstrumentDatabase.stringToInstruments: reading next instrument")
                    }
                    Keyword.Invalid -> {}
                }
                stream.goToNextLine()
            }

            // string indices were used in older versions, we still allow reading them ....
            val stringsResolved = strings ?: stringIndices?.map { legacyNoteIndexToNote(it) }?.toTypedArray()
            stringsResolved?.let {
                val instrument = Instrument(instrumentName, null, it, iconId, stableId)
                instruments.add(instrument)
                numInstrumentsRead += 1
            }

            if (version == null && numInstrumentsRead == 0)
                return InstrumentsAndFileCheckResult(FileCheck.Invalid, instruments)
            return InstrumentsAndFileCheckResult(FileCheck.Ok, instruments)
        }

        fun toastFileCheckString(context: Context, filename: String?, fileCheck: FileCheck) {
            when (fileCheck) {
                FileCheck.Empty -> {
                    Toast.makeText(context, context.getString(R.string.file_empty, filename), Toast.LENGTH_LONG).show()
                }
                FileCheck.Invalid -> {
                    Toast.makeText(context, context.getString(R.string.file_invalid, filename), Toast.LENGTH_LONG).show()
                }
                else -> {
                }
            }
        }
    }
}