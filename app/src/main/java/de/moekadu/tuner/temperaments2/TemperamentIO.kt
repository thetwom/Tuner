package de.moekadu.tuner.temperaments2

import android.content.Context
import android.net.Uri
import de.moekadu.tuner.BuildConfig
import de.moekadu.tuner.misc.StringOrResId
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.temperaments.RationalNumber
import java.io.BufferedReader
import java.io.BufferedWriter

private fun BufferedWriter.writeLine(line: String) {
    write(line)
    newLine()
}

object TemperamentIO {
    data class NoteLineContents(
        val note: MusicalNote?,
        val cent: Double?,
        val ratio: RationalNumber?
    )

    class ParsedTemperament(
        val name: String,
        val abbreviation: String,
        val description: String,
        val noteLines: Array<NoteLineContents?>
    ) {
        fun hasErrors(): Boolean {
            if (noteLines.isEmpty())
                return true
            // check that all lines could be successfully parsed
            if (noteLines.contains(null))
                return true
            // check that there is a valid cent or ratio value
            if (noteLines.firstOrNull { it?.cent == null && it?.ratio == null  } == null)
                return true

            // check that values are increasing
            val orderingError = TemperamentValidityChecks.checkValueOrderingErrors(
                noteLines.size,
                {
                    val ratio = noteLines[it]?.ratio?.toDouble()
                    if (ratio != null) ratioToCents(ratio.toDouble()) else noteLines[it]?.cent
                },
                null
            )
            if (orderingError != TemperamentValidityChecks.ValueOrdering.Increasing)
                return true

            // check if we can use predefined notes
            val predefinedNotes = getSuitableNoteNames(noteLines.size)
            if (predefinedNotes != null)
                return false

            // if predefined notes are note possible, check the validity of the user defined notes
            val noteNameError = TemperamentValidityChecks.checkNoteNameErrors(
                noteLines.size,
                { noteLines[it]?.note },
                null
            )
            if (noteNameError != TemperamentValidityChecks.NoteNameError.None)
                return true

            return false
        }

        fun toTemperamentWithNoteNames(): TemperamentWithNoteNames? {
            if (noteLines.isEmpty())
                return null

            // fill different arrays
            val numberOfNotesPerOctave = noteLines.size
            val ratios = ArrayList<RationalNumber?>()
            val cents = ArrayList<Double>()
            val notes = ArrayList<MusicalNote?>()

            for (line in noteLines) {
                ratios.add(line?.ratio)
                val centFromRatio = if (line?.ratio != null)
                    ratioToCents(line.ratio.toDouble())
                else
                    null
                val cent = line?.cent ?: centFromRatio ?: return null
                cents.add(cent)
                notes.add(line?.note)
            }

            // resolve note names, we try to use predefined note names
            val predefinedNotes = getSuitableNoteNames(noteLines.size)
            if (notes.contains(null) && predefinedNotes == null)
                return null

            val resolvedNotes = if (notes.contains(null)) {
                null // this branch uses the predefined notes, which happens by specifiying null
            } else if (predefinedNotes == null) {
                notes
            } else {
                var usePredefined = true
                for (i in 0 until numberOfNotesPerOctave) {
                    if (!notesEqualCheck(predefinedNotes.notes[i], notes[i])) {
                        usePredefined = false
                        break
                    }
                }
                if (usePredefined) null else notes
            }

            val noteNames = if (resolvedNotes == null) {
                null
            } else {
                val referenceNote = detectReferenceNote(resolvedNotes) ?: return null
                NoteNames(
                    resolvedNotes.map { it!! }.toTypedArray(),
                    referenceNote
                )
            }

            // construct the temperament
            return if (!ratios.contains(null)) {
                TemperamentWithNoteNames(
                    Temperament.create(
                        name = StringOrResId(name ?: ""),
                        abbreviation = StringOrResId(abbreviation ?: ""),
                        description = StringOrResId(description ?: ""),
                        rationalNumbers = ratios.map { it!! }.toTypedArray(),
                        stableId = Temperament.NO_STABLE_ID
                    ),
                    noteNames = noteNames
                )
            } else {
                TemperamentWithNoteNames(
                    Temperament.create(
                        name = StringOrResId(name ?: ""),
                        abbreviation = StringOrResId(abbreviation ?: ""),
                        description = StringOrResId(description ?: ""),
                        cents = cents.toDoubleArray(),
                        stableId = Temperament.NO_STABLE_ID
                    ),
                    noteNames = noteNames
                )
            }
        }

        private fun detectReferenceNote(notes: List<MusicalNote?>): MusicalNote? {
            return notes.firstOrNull{
                (it?.base == BaseNote.A && it.modifier == NoteModifier.None) ||
                        (it?.enharmonicBase == BaseNote.A && it.enharmonicModifier == NoteModifier.None)
            } ?: notes.firstOrNull { it != null }
        }
    }

//    fun temperamentToString(temperament: TemperamentWithNoteNames): String {
//        val result = StringBuilder()
//        val name = temperament.temperament.name.value(null)
//            .replace("\n", " ")
//        val abbreviation = temperament.temperament.abbreviation.value(null)
//            .replace("\n", " ")
//        val description = temperament.temperament.description.value(null)
//            .replace("\n", " ")
//
//        result.append("! ${name.replace(" ","_")}.scl\n")
//        result.append("!\n")
//
//        if (name != "")
//            result.append("! $NAME_KEY=$name\n")
//        if (abbreviation != "")
//            result.append("! $ABBREVIATION_KEY=$abbreviation\n")
//        if (description != "")
//            result.append("! $DESCRIPTION_KEY=$description\n")
//        result.append("!\n")
//        result.append("$name\n")
//        result.append("${temperament.temperament.numberOfNotesPerOctave}\n")
//        result.append("!\n")
//
//        val numberOfNotes = temperament.temperament.numberOfNotesPerOctave
//        val noteNames = temperament.noteNames ?: getSuitableNoteNames(numberOfNotes)
//        val ratios = temperament.temperament.rationalNumbers
//        val cents = temperament.temperament.cents
//
//        for (i in 1 .. numberOfNotes) {
//            if (ratios != null) {
//                val r = ratios[i]
//                result.append(" ${r.numerator}/${r.denominator}\n")
//            } else {
//                val c = cents[i]
//                result.append(" %.2f".format(c))
//            }
//            noteNames?.getOrNull(i % numberOfNotes)?.let { note ->
//                result.append("    ")
//                result.append(noteToString(note))
//            }
//
//            result.append("\n")
//        }
//
//        return result.toString()
//    }

    fun writeTemperaments(
        temperaments: List<TemperamentWithNoteNames>,
        writer: BufferedWriter,
        context: Context) {
        writeVersion(writer)
        temperaments.forEach {
            writer.writeLine("!")
            writeTemperament(it, writer, context)
        }
    }
    fun writeVersion(writer: BufferedWriter) {
        writer.writeLine("! ${VERSION_KEY}=${BuildConfig.VERSION_NAME}")
    }

    fun writeTemperament(
        temperament: TemperamentWithNoteNames,
        writer: BufferedWriter,
        context: Context
    ) {
        val name = temperament.temperament.name.value(context)
            .replace("\n", " ")
        val abbreviation = temperament.temperament.abbreviation.value(context)
            .replace("\n", " ")
        val description = temperament.temperament.description.value(context)
            .replace("\n", " ")

        writer.writeLine("! ${name.replace(" ","_")}.scl")

        if (abbreviation != "")
            writer.writeLine("! $ABBREVIATION_KEY=$abbreviation")
        if (description != "")
            writer.writeLine("! $DESCRIPTION_KEY=$description")

        writer.writeLine("!")
        writer.writeLine(name)
        writer.writeLine("${temperament.temperament.numberOfNotesPerOctave}")
        writer.writeLine("!")

        val numberOfNotes = temperament.temperament.numberOfNotesPerOctave
        val noteNames = temperament.noteNames ?: getSuitableNoteNames(numberOfNotes)
        val ratios = temperament.temperament.rationalNumbers
        val cents = temperament.temperament.cents

        for (i in 1 .. numberOfNotes) {
            if (ratios != null) {
                val r = ratios[i]
                writer.write(" ${r.numerator}/${r.denominator}")
            } else {
                val c = cents[i]
                writer.write(" %.2f".format(c))
            }
            noteNames?.getOrNull(i % numberOfNotes)?.let { note ->
                writer.write("    ${NOTE_KEY}=${noteToString(note)}")
            }
            writer.newLine()
        }
    }

    fun readTemperamentsFromFile(context: Context, uri: Uri): List<ParsedTemperament>  {
        return context.contentResolver?.openInputStream(uri)?.use { reader ->
            parseTemperaments(reader.bufferedReader())
        } ?: listOf()
    }

    fun parseTemperaments(reader: BufferedReader): List<ParsedTemperament> {
        val collectedTemperaments = ArrayList<ParsedTemperament>()

        var version: String? = null
        var name: String? = null
        var abbreviation: String? = null
        var description: String? = null
        var numberOfNotes: Int = -1

        val noteLines = ArrayList<NoteLineContents?>()

        reader.forEachLine { line ->
            if (isCommentLine(line)) {
                val (key, value) = parseCommentLine(line)
                when (key) {
                    VERSION_KEY -> version = value
                    ABBREVIATION_KEY -> abbreviation = value
                    DESCRIPTION_KEY -> description = value
                }
            } else {
                when {
                    name == null -> {
                        name = line.trim()
                    }
                    numberOfNotes < 0 -> {
                        numberOfNotes = line.trim().toIntOrNull() ?: return@forEachLine
                        if (numberOfNotes < 0)
                            return@forEachLine
                    }
                    else -> {
                        noteLines.add(parseNoteLine(line))
                    }
                }
            }

            if (numberOfNotes >= 0 && noteLines.size == numberOfNotes) {
                // we do not support temperaments with only one note, we at least need the octave
                if (numberOfNotes > 0) {
                    noteLines.add(
                        0,
                        noteLines.last()?.copy(
                            cent = 0.0,
                            ratio = RationalNumber(1,1)
                        )
                    )

                    collectedTemperaments.add(ParsedTemperament(
                        name ?: "", abbreviation ?: "", description ?: "",
                        noteLines.toTypedArray()
                    ))
                }

                name = null
                abbreviation = null
                description = null
                numberOfNotes = -1
                noteLines.clear()
            }
        }
        return collectedTemperaments
    }

    private fun notesEqualCheck(note: MusicalNote?, other: MusicalNote?): Boolean {
        return (note?.base == other?.base &&
                note?.modifier == other?.modifier &&
                note?.octaveOffset == other?.octaveOffset &&
                note?.enharmonicBase == other?.enharmonicBase &&
                note?.enharmonicModifier == other?.enharmonicModifier &&
                note?.enharmonicOctaveOffset == other?.enharmonicOctaveOffset
                )
    }
    private fun isCommentLine(string: String): Boolean {
        return string.trimStart().getOrNull(0) == '!'
    }

    private fun parseCommentLine(string: String): Pair<String?, String?> {
        val keys = listOf(VERSION_KEY, DESCRIPTION_KEY, ABBREVIATION_KEY)
        var trimmed = string.trim()
        if (trimmed.getOrNull(0) != '!')
            return Pair(null, null)

        val keyAndValue = trimmed.drop(1).split('=', limit = 2)
        if (keyAndValue.size < 2)
            return Pair(null, null)
        val possibleKey = keyAndValue[0].trim()
        val possibleValue = keyAndValue[1].trim()
        for (key in keys) {
            if (key == possibleKey)
                return Pair(possibleKey, possibleValue)
        }
        return Pair(null, null)
    }

    private fun parseNoteLine(string: String): NoteLineContents? {
        val valueAndMore = string.trim().split("\\s+".toRegex(), limit = 2)
        if (valueAndMore.isEmpty())
            return null
        val cent = parseCent(valueAndMore[0])
        val ratio = if (cent == null) parseRatio(valueAndMore[0]) else null
        if (cent == null && ratio == null)
            return null

        val note = if (valueAndMore.size >= 2) parseNote(valueAndMore[1]) else null
        return NoteLineContents(note, cent, ratio)
    }

    private fun parseRatio(string: String): RationalNumber? {
        // int numbers without slash are also a ratio with denominator 1
        val possibleNumerator = string.trim().toIntOrNull()
        if (possibleNumerator != null)
            return RationalNumber(possibleNumerator, 1)

        // now check for rations with slash
        val values = string.trim().split('/')
        if (values.size != 2)
            return null
        val numerator = values[0].toIntOrNull()
        val denominator = values[1].toIntOrNull()
        return if (numerator != null && denominator != null)
            RationalNumber(numerator, denominator)
        else
            null
    }

    private fun parseCent(string: String): Double? {
        if (!string.contains('.'))
            return null
        return string.trim().toDoubleOrNull()
    }

    private fun parseNote(string: String): MusicalNote? {
        val keyAndValue = string.trim().split('=', limit = 2)
        if (keyAndValue.size < 2)
            return null
        if (keyAndValue[0].trim() != NOTE_KEY)
            return null

        var noteString = keyAndValue[1]
        if (noteString.isEmpty())
            return null

        var baseNote = BaseNote.None
        var noteModifier = NoteModifier.None
        var octaveOffset = 0

        // if string does start with "-/", then the nonenharmonic is None, if not we try to parse
        if (!noteString.matches("-\\s+/.*".toRegex())) {
            // first find base note
            baseNote = BaseNote.entries.firstOrNull {  noteString.startsWith(it.name) }
                ?: return null
            if (baseNote == BaseNote.None)
                return null

            // cut away string responsible for base note
            noteString = noteString.substring(baseNote.name.length)
            // then find note modifier
            noteModifier = stringToNoteModifierMap[
                noteModifierNames.firstOrNull { noteString.startsWith(it) } ?: ""
            ] ?: NoteModifier.None
            // cut away string responsible for modifier
            noteString = noteString.substring(noteModifierToString(noteModifier).length)
            // find octave offset
            val offsetString = "^[-+]\\d+".toRegex().find(noteString)?.value
            octaveOffset = offsetString?.toIntOrNull() ?: 0

            // remove '/' if there is one, afterwards we read the enharmonic
            noteString = noteString.substring(offsetString?.length ?: 0).trim()
            if (noteString[0] == '/')
                noteString = noteString.drop(1).trim()
        }

        // find enharmonic base
        val enharmonicBase = BaseNote.entries.firstOrNull {  noteString.startsWith(it.name) }
            ?: BaseNote.None

        if (enharmonicBase == BaseNote.None)
            return MusicalNote(baseNote, noteModifier, octaveOffset)

        // cut away string responsible for enharmonic base
        noteString = noteString.substring(enharmonicBase.name.length)

        val enharmonicModifier = stringToNoteModifierMap[
            noteModifierNames.firstOrNull { noteString.startsWith(it) } ?: ""
        ] ?: NoteModifier.None
        // cut away string responsible for enharmonic modifier
        noteString = noteString.substring(noteModifierToString(enharmonicModifier).length)
        // find enharmonic octave offset
        val enharmonicOffsetString = "^[-+]\\d+".toRegex().find(noteString)?.value
        val enharmonicOctaveOffset = enharmonicOffsetString?.toIntOrNull() ?: 0
        return MusicalNote(
            baseNote, noteModifier, octaveOffset,
            enharmonicBase = enharmonicBase,
            enharmonicModifier = enharmonicModifier,
            enharmonicOctaveOffset = enharmonicOctaveOffset
        )
    }

    private fun noteToString(note: MusicalNote): String {
        val builder = StringBuilder()
        if (note.base == BaseNote.None) {
            builder.append("-")
        } else {
            builder.append(noteToString(note.base, note.modifier, note.octaveOffset))
        }
        if (note.enharmonicBase != BaseNote.None) {
            builder.append("/")
            builder.append(noteToString(
                note.enharmonicBase,
                note.enharmonicModifier,
                note.enharmonicOctaveOffset
            ))
        }
        return builder.toString()
    }

    private fun noteToString(base: BaseNote, modifier: NoteModifier, offset: Int): String {
        val offsetString = if (offset == 0) "" else "%+d".format(offset)
        return "${base.name}${noteModifierToString(modifier)}$offsetString"
    }

    private fun noteModifierToString(modifier: NoteModifier) = when (modifier) {
        NoteModifier.FlatDownDown -> "vvb"
        NoteModifier.FlatDown -> "vb"
        NoteModifier.Flat -> "b"
        NoteModifier.FlatUp -> "^b"
        NoteModifier.FlatUpUp -> "^^b"
        NoteModifier.NaturalDownDown -> "vv"
        NoteModifier.NaturalDown -> "v"
        NoteModifier.None -> ""
        NoteModifier.NaturalUp -> "^"
        NoteModifier.NaturalUpUp -> "^^"
        NoteModifier.SharpDownDown -> "vv#"
        NoteModifier.SharpDown -> "v#"
        NoteModifier.Sharp -> "#"
        NoteModifier.SharpUp -> "^#"
        NoteModifier.SharpUpUp -> "^^#"
    }

    private val stringToNoteModifierMap =
        NoteModifier.entries.associateBy { noteModifierToString(it) }

    /** Names of note modifiers, where the longer names come first. */
    private val noteModifierNames = NoteModifier.entries
        .map { noteModifierToString(it) }
        .sortedByDescending { it.length }


    const val VERSION_KEY = "version"
    const val ABBREVIATION_KEY = "abbreviation"
    const val DESCRIPTION_KEY = "description"
    const val NOTE_KEY = "note"
}