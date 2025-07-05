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
package de.moekadu.tuner.ui.notes

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import de.moekadu.tuner.R
import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteModifier
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.max

private val modifierPostfixStrings = mapOf(
    NoteModifier.None to "",
    NoteModifier.Sharp to "\uE10C",
    NoteModifier.SharpUp to "\uE10C",
    NoteModifier.SharpUpUp to "\uE10C",
    NoteModifier.SharpUpUpUp to "\uE10C",
    NoteModifier.SharpDown to "\uE10C",
    NoteModifier.SharpDownDown to "\uE10C",
    NoteModifier.SharpDownDownDown to "\uE10C",
    NoteModifier.SharpSharp to "\uE111",
    NoteModifier.SharpSharpUp to "\uE111",
    NoteModifier.SharpSharpUpUp to "\uE111",
    NoteModifier.SharpSharpUpUpUp to "\uE111",
    NoteModifier.SharpSharpDown to "\uE111",
    NoteModifier.SharpSharpDownDown to "\uE111",
    NoteModifier.SharpSharpDownDownDown to "\uE111",
    NoteModifier.SharpSharpSharp to "\uE112",
    NoteModifier.SharpSharpSharpUp to "\uE112",
    NoteModifier.SharpSharpSharpUpUp to "\uE112",
    NoteModifier.SharpSharpSharpUpUpUp to "\uE112",
    NoteModifier.SharpSharpSharpDown to "\uE112",
    NoteModifier.SharpSharpSharpDownDown to "\uE112",
    NoteModifier.SharpSharpSharpDownDownDown to "\uE112",
    NoteModifier.Flat to "\uE100",
    NoteModifier.FlatUp to "\uE100",
    NoteModifier.FlatUpUp to "\uE100",
    NoteModifier.FlatUpUpUp to "\uE100",
    NoteModifier.FlatDown to "\uE100",
    NoteModifier.FlatDownDown to "\uE100",
    NoteModifier.FlatDownDownDown to "\uE100",
    NoteModifier.FlatFlat to "\uE105",
    NoteModifier.FlatFlatUp to "\uE105",
    NoteModifier.FlatFlatUpUp to "\uE105",
    NoteModifier.FlatFlatUpUpUp to "\uE105",
    NoteModifier.FlatFlatDown to "\uE105",
    NoteModifier.FlatFlatDownDown to "\uE105",
    NoteModifier.FlatFlatDownDownDown to "\uE105",
    NoteModifier.FlatFlatFlat to "\uE106",
    NoteModifier.FlatFlatFlatUp to "\uE106",
    NoteModifier.FlatFlatFlatUpUp to "\uE106",
    NoteModifier.FlatFlatFlatUpUpUp to "\uE106",
    NoteModifier.FlatFlatFlatDown to "\uE106",
    NoteModifier.FlatFlatFlatDownDown to "\uE106",
    NoteModifier.FlatFlatFlatDownDownDown to "\uE106",
    NoteModifier.NaturalUp to "",
    NoteModifier.NaturalUpUp to "",
    NoteModifier.NaturalUpUpUp to "",
    NoteModifier.NaturalDown to "",
    NoteModifier.NaturalDownDown to "",
    NoteModifier.NaturalDownDownDown to "",
)

private val modifierPrefixStrings = mapOf(
    NoteModifier.None to "",
    NoteModifier.Sharp to "",
    NoteModifier.SharpUp to "\uE116",
    NoteModifier.SharpUpUp to "\uE117",
    NoteModifier.SharpUpUpUp to "\uE118",
    NoteModifier.SharpDown to "\uE113",
    NoteModifier.SharpDownDown to "\uE114",
    NoteModifier.SharpDownDownDown to "\uE115",
    NoteModifier.SharpSharp to "",
    NoteModifier.SharpSharpUp to "\uE116",
    NoteModifier.SharpSharpUpUp to "\uE117",
    NoteModifier.SharpSharpUpUpUp to "\uE118",
    NoteModifier.SharpSharpDown to "\uE113",
    NoteModifier.SharpSharpDownDown to "\uE114",
    NoteModifier.SharpSharpDownDownDown to "\uE115",
    NoteModifier.SharpSharpSharp to "",
    NoteModifier.SharpSharpSharpUp to "\uE116",
    NoteModifier.SharpSharpSharpUpUp to "\uE117",
    NoteModifier.SharpSharpSharpUpUpUp to "\uE118",
    NoteModifier.SharpSharpSharpDown to "\uE113",
    NoteModifier.SharpSharpSharpDownDown to "\uE114",
    NoteModifier.SharpSharpSharpDownDownDown to "\uE115",
    NoteModifier.Flat to "",
    NoteModifier.FlatUp to "\uE116",
    NoteModifier.FlatUpUp to "\uE117",
    NoteModifier.FlatUpUpUp to "\uE118",
    NoteModifier.FlatDown to "\uE113",
    NoteModifier.FlatDownDown to "\uE114",
    NoteModifier.FlatDownDownDown to "\uE115",
    NoteModifier.FlatFlat to "",
    NoteModifier.FlatFlatUp to "\uE116",
    NoteModifier.FlatFlatUpUp to "\uE117",
    NoteModifier.FlatFlatUpUpUp to "\uE118",
    NoteModifier.FlatFlatDown to "\uE113",
    NoteModifier.FlatFlatDownDown to "\uE114",
    NoteModifier.FlatFlatDownDownDown to "\uE115",
    NoteModifier.FlatFlatFlat to "",
    NoteModifier.FlatFlatFlatUp to "\uE116",
    NoteModifier.FlatFlatFlatUpUp to "\uE117",
    NoteModifier.FlatFlatFlatUpUpUp to "\uE118",
    NoteModifier.FlatFlatFlatDown to "\uE113",
    NoteModifier.FlatFlatFlatDownDown to "\uE114",
    NoteModifier.FlatFlatFlatDownDownDown to "\uE115",
    NoteModifier.NaturalUp to "\uE116",
    NoteModifier.NaturalUpUp to "\uE117",
    NoteModifier.NaturalUpUpUp to "\uE118",
    NoteModifier.NaturalDown to "\uE113",
    NoteModifier.NaturalDownDown to "\uE114",
    NoteModifier.NaturalDownDownDown to "\uE115",
)

/** Stem of note name, which is the including enharmonic info but no octave info.
 * @param baseNote Base note.
 * @param modifier Note modifier.
 */
data class NoteNameStem(
    val baseNote: BaseNote,
    val modifier: NoteModifier = NoteModifier.None
) {
    constructor(note: MusicalNote, useEnharmonic: Boolean) : this(
        if (useEnharmonic) note.enharmonicBase else note.base,
        if (useEnharmonic) note.enharmonicModifier else note.modifier
    )
}

private data class ResolvedNoteProperties(
    val baseName: CharSequence,
    val modifier: NoteModifier,
    val octave: Int
)

private fun resolveNoteProperties(
    note: MusicalNote,
    notePrintOptions: NotePrintOptions,
    resources: Resources
): ResolvedNoteProperties {
    return if (
        (!notePrintOptions.useEnharmonic && note.base != BaseNote.None) ||
        (notePrintOptions.useEnharmonic && note.enharmonicBase == BaseNote.None)
        ) {
        resolveNoteProperties(note, resources = resources, useEnharmonic = false
        ) { notePrintOptions.resourceId(it) }
    } else {
        resolveNoteProperties(note, resources = resources, useEnharmonic = true
        ) { notePrintOptions.resourceId(it) }
    }
}

//private fun resolveNoteProperties(
//    note: MusicalNote,
//    notePrintOptions: NotePrintOptions,
//    resources: Resources
//    ): ResolvedNoteProperties {
//    return if (notePrintOptions.useEnharmonic && note.enharmonicBase != BaseNote.None) {
//        resolveNotePropertiesWithoutEnharmonicCheck(
//            note.switchEnharmonic(switchAlsoForBaseNone = true),
//            resources = resources,
//        ) { notePrintOptions.resourceId(it) }
//    } else {
//        resolveNotePropertiesWithoutEnharmonicCheck(
//            note,
//            resources = resources,
//        ) { notePrintOptions.resourceId(it) }
//    }
//}

//private fun resolveNotePropertiesWithoutEnharmonicCheck(
//    note: MusicalNote,
//    resources: Resources,
//    resourceIdOfStem: (NoteNameStem) -> Int?
//    ): ResolvedNoteProperties {
//    // check if we can directly resolve the note
//    val stem = NoteNameStem(note)
//    val noteName = resourceIdOfStem(stem)?.let {resources.getText(it)}
//    if (noteName != null && noteName != "" && noteName != "-") {
//        return ResolvedNoteProperties(
//            baseName = noteName,
//            modifier = NoteModifier.None,
//            octave = if (note.octave == Int.MAX_VALUE) Int.MAX_VALUE else note.octave + note.octaveOffset
//        )
//    }
//
//    // check if we can directly resolve the enharmonic
//    val noteEnharmonic = note.switchEnharmonic(switchAlsoForBaseNone = true)
//    val stemEnharmonic = NoteNameStem(noteEnharmonic)
//    val noteNameEnharmonic = resourceIdOfStem(stemEnharmonic)?.let { resources.getText(it) }
//
//    if (noteNameEnharmonic != null && noteNameEnharmonic != "" && noteNameEnharmonic != "-") {
//        return ResolvedNoteProperties(
//            baseName = noteNameEnharmonic,
//            modifier = NoteModifier.None,
//            octave = if (note.octave == Int.MAX_VALUE) Int.MAX_VALUE else noteEnharmonic.octave + noteEnharmonic.octaveOffset
//        )
//    }
//
//    // try to resolve note by base name + modifier
//    val stemBase = NoteNameStem(
//        note.base, NoteModifier.None, BaseNote.None, NoteModifier.None
//    )
//    val noteNameBase = resourceIdOfStem(stemBase)?.let { resources.getText(it) }
//    if (noteNameBase != null && noteNameBase != "" && noteNameBase != "-") {
//        return ResolvedNoteProperties(
//            baseName = noteNameBase,
//            modifier = note.modifier,
//            octave = if (note.octave == Int.MAX_VALUE) Int.MAX_VALUE else note.octave + note.octaveOffset
//        )
//    }
//
//    // try to resolve note by enharmonic base name + modifier
//    val stemEnharmonicBase = NoteNameStem(
//        noteEnharmonic.base, NoteModifier.None, BaseNote.None, NoteModifier.None
//    )
//    val noteNameEnharmonicBase = resourceIdOfStem(stemEnharmonicBase)?.let { resources.getText(it) }
//    if (noteNameEnharmonicBase != null && noteNameEnharmonicBase != "" && noteNameEnharmonicBase != "-") {
//        return ResolvedNoteProperties(
//            baseName = noteNameEnharmonicBase,
//            modifier = noteEnharmonic.modifier,
//            octave = if (note.octave == Int.MAX_VALUE) Int.MAX_VALUE else noteEnharmonic.octave + noteEnharmonic.octaveOffset
//        )
//    }
//    return ResolvedNoteProperties("O", note.modifier, note.octave)
//}

private fun resolveNoteProperties(
    note: MusicalNote,
    resources: Resources,
    useEnharmonic: Boolean,
    resourceIdOfStem: (NoteNameStem) -> Int?
): ResolvedNoteProperties {
    // check if we can directly resolve the note
    val stem = NoteNameStem(note, useEnharmonic)
    val noteName = resourceIdOfStem(stem)?.let {resources.getText(it)}
    val octaveOffset = if (useEnharmonic) note.enharmonicOctaveOffset else note.octaveOffset

    if (noteName != null && noteName != "" && noteName != "-") {
        return ResolvedNoteProperties(
            baseName = noteName,
            modifier = NoteModifier.None,
            octave = if (note.octave == Int.MAX_VALUE) Int.MAX_VALUE else note.octave + octaveOffset
        )
    }

    // try to resolve note by base name + modifier
    val stemBase = NoteNameStem(
        if (useEnharmonic) note.enharmonicBase else note.base,
        NoteModifier.None
    )
    val noteNameBase = resourceIdOfStem(stemBase)?.let { resources.getText(it) }
    if (noteNameBase != null && noteNameBase != "" && noteNameBase != "-") {
        return ResolvedNoteProperties(
            baseName = noteNameBase,
            modifier = if (useEnharmonic) note.enharmonicModifier else note.modifier,
            octave = if (note.octave == Int.MAX_VALUE) Int.MAX_VALUE else note.octave + octaveOffset
        )
    }
    return ResolvedNoteProperties(
        "O",
        if (useEnharmonic) note.enharmonicModifier else note.modifier,
        note.octave + octaveOffset
    )
}

/** Options for note printing.
 * @param useEnharmonic Tells if the enharmonic should be used for printing.
 * @param helmholtzNotation Set this to true, to use Helmholtz notation (this uses small letters
 *   for higher octaves and capital letters for lower octaves. Also for octaves around 2 and 3, no
 *   octaves numbers are printed but instead , and '.
 * @param notationType Notation type used for printing.
 */
@Serializable
@Stable
data class NotePrintOptions(
    val useEnharmonic: Boolean = false,
    val helmholtzNotation: Boolean = false,
    val notationType: NotationType = NotationType.Standard
) {
    @Transient
    private val resourceIds = notationType.resourceIds()
    fun resourceId(noteNameStem: NoteNameStem) = resourceIds[noteNameStem]
}

private val musicalSymbolFont = FontFamily(Font(R.font.gonville))

private fun createAnnotatedStringOfNote(
    properties: ResolvedNoteProperties,
    notePrintOptions: NotePrintOptions,
    fontSize: TextUnit,
    fontWeight: FontWeight?,
    withOctave: Boolean
): AnnotatedString {
    return buildAnnotatedString {
        if (properties.modifier != NoteModifier.None && modifierPrefixStrings[properties.modifier] != "") {
            withStyle(
                SpanStyle(
                    fontFamily = musicalSymbolFont,
                    fontSize = fontSize,
                    fontWeight = fontWeight
                )
            ) {
                append(modifierPrefixStrings[properties.modifier])
                append("\u200a")
            }
        }

        withStyle(
            SpanStyle(
                fontSize = fontSize,
                fontWeight = fontWeight
            )
        ) {
            append(
                if (notePrintOptions.helmholtzNotation && withOctave && properties.octave >= 3)
                    properties.baseName.toString().lowercase()
                else
                    properties.baseName
            )
        }

        // postfix with spaces (\u200a is a narrow space)
        if (properties.modifier != NoteModifier.None && modifierPostfixStrings[properties.modifier] != "") {
            withStyle(
                SpanStyle(
                    fontFamily = musicalSymbolFont,
                    fontSize = fontSize,
                    fontWeight = fontWeight
                )
            ) {
                append("\u200a")
                append(modifierPostfixStrings[properties.modifier])
                if (withOctave && properties.octave != Int.MAX_VALUE)
                    append("\u200a")
            }
        } else {
            // append a zero-width space, such that the symbol-typeface is used
            // this ensures, that the total text height is the same unregarding if a note has a modifier
            // or not.
            withStyle(
                SpanStyle(
                    fontFamily = musicalSymbolFont,
                    fontSize = fontSize,
                    fontWeight = fontWeight
                )
            ) {
                append("\u200b")
            }
        }

        if (withOctave && properties.octave != Int.MAX_VALUE) {
            if (notePrintOptions.helmholtzNotation) {
                when {
                    properties.octave <= -1 -> {
                        withStyle(SpanStyle(fontSize = fontSize * 0.7f)) {
                            withStyle(SpanStyle(baselineShift = BaselineShift(-0.2f))) {
                                append((-properties.octave + 2).toString())
                            }
                        }
                    }

                    properties.octave == 0 -> {
                        withStyle(SpanStyle(fontSize = fontSize, fontWeight = fontWeight)) {
                            append(",,")
                        }
                    }

                    properties.octave == 1 -> {
                        withStyle(SpanStyle(fontSize = fontSize, fontWeight = fontWeight)) {
                            append(",")
                        }
                    }

                    properties.octave == 2 || properties.octave == 3 -> {
                        withStyle(SpanStyle(fontSize = fontSize, fontWeight = fontWeight)) {
                            append("")
                        }
                    }

                    properties.octave == 4 -> {
                        withStyle(SpanStyle(fontSize = fontSize, fontWeight = fontWeight)) {
                            append("'")
                        }
                    }

                    properties.octave == 5 -> {
                        withStyle(SpanStyle(fontSize = fontSize, fontWeight = fontWeight)) {
                            append("''")
                        }
                    }

                    else -> {  // -> octave >= 6
                        withStyle(SpanStyle(fontSize = fontSize * 0.7f, fontWeight = fontWeight)) {
                            withStyle(SpanStyle(baselineShift = BaselineShift(0.5f))) {
                                append((properties.octave - 3).toString())
                            }
                        }
                    }
                }
            } else {
                withStyle(SpanStyle(fontSize = fontSize * 0.7f, fontWeight = fontWeight)) {
                    withStyle(SpanStyle(baselineShift = BaselineShift(0.5f))) {
                        append(properties.octave.toString())
                    }
                }
            }
        }
    }
}

fun MusicalNote.asAnnotatedString(
    notePrintOptions: NotePrintOptions,
    fontSize: TextUnit,
    fontWeight: FontWeight?,
    withOctave: Boolean,
    resources: Resources
) : AnnotatedString {
    val properties = resolveNoteProperties(
        this,
        notePrintOptions,
        resources
    )
    return createAnnotatedStringOfNote(properties, notePrintOptions, fontSize, fontWeight, withOctave)
}

/** Note.
 * @param musicalNote Note which should be printed.
 * @param modifier Modifier.
 * @param notePrintOptions Options of how to print the note.
 * @param withOctave True to print with octave index, false to print without octave index.
 * @param fontSize Font size or Unspecified to get it from the context.
 */
@Composable
fun Note(
    musicalNote: MusicalNote,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    withOctave: Boolean = true,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    style: TextStyle? = null
) {
    val resources = LocalContext.current.resources
    val fontSizeResolved = fontSize.takeOrElse {
        (style?.fontSize ?: TextUnit.Unspecified).takeOrElse {
            LocalTextStyle.current.fontSize.takeOrElse { 12.sp }
        }
    }
    val fontWeightResolved = fontWeight ?: style?.fontWeight
    val colorResolved = color.takeOrElse {
        style?.color ?: Color.Unspecified
    }

    val properties = remember(musicalNote, notePrintOptions, resources) {
        resolveNoteProperties(
            note = musicalNote,
            notePrintOptions = notePrintOptions,
            resources = resources
        )
    }

    val noteAsString = remember(musicalNote, notePrintOptions, fontSizeResolved, fontWeightResolved, withOctave) {
        createAnnotatedStringOfNote(properties, notePrintOptions, fontSizeResolved, fontWeightResolved, withOctave)
    }

    Text(text = noteAsString, modifier = modifier, color = colorResolved)
}

/** Note with enharmonic if it exists (prints something like e.g. C#/Db).
 * @param musicalNote Note which should be printed.
 * @param modifier Modifier.
 * @param notePrintOptions Options of how to print the note.
 * @param withOctave True to print with octave index, false to print without octave index.
 * @param fontSize Font size or Unspecified to get it from the context.
 */
@Composable
fun NoteWithEnharmonic(
    musicalNote: MusicalNote,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    withOctave: Boolean = true,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    style: TextStyle? = null
) {
    val resources = LocalContext.current.resources
    val fontSizeResolved = fontSize.takeOrElse {
        (style?.fontSize ?: TextUnit.Unspecified).takeOrElse {
            LocalTextStyle.current.fontSize.takeOrElse { 12.sp }
        }
    }
    val fontWeightResolved = fontWeight ?: style?.fontWeight
    val colorResolved = color.takeOrElse {
        style?.color ?: Color.Unspecified
    }

    val properties = remember(musicalNote, notePrintOptions, resources) {
        if (musicalNote.base != BaseNote.None) {
            resolveNoteProperties(
                note = musicalNote,
                resources = resources,
                useEnharmonic = false
            ) { notePrintOptions.resourceId(it) }
        } else {
            null
        }
    }
    val propertiesEnharmonic = remember(musicalNote, notePrintOptions, resources) {
        if (musicalNote.base != BaseNote.None) {
            resolveNoteProperties(
                note = musicalNote,
                resources = resources,
                useEnharmonic = true
            ) { notePrintOptions.resourceId(it) }
        } else {
            null
        }
    }

    val noteAsString = remember(properties, notePrintOptions, fontSizeResolved, fontWeightResolved, withOctave) {
        properties?.let {
            createAnnotatedStringOfNote(
                it,
                notePrintOptions,
                fontSizeResolved,
                fontWeightResolved,
                withOctave
            )
        }
    }

    val noteAsStringEnharmonic = remember(propertiesEnharmonic, notePrintOptions, fontSizeResolved, fontWeightResolved, withOctave) {
        propertiesEnharmonic?.let {
            createAnnotatedStringOfNote(
                it,
                notePrintOptions,
                fontSizeResolved,
                fontWeightResolved,
                withOctave
            )
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        val printDefault = musicalNote.base != BaseNote.None && noteAsString != null
        val printEnharmonic = (
                musicalNote.enharmonicBase != BaseNote.None &&
                        noteAsStringEnharmonic != null &&
                        properties != propertiesEnharmonic
        )
        if (printDefault) {
            Text(text = noteAsString!!, color = colorResolved)
        }
        if (printDefault && printEnharmonic) {
            Text(
                text = "/",
                Modifier.padding(horizontal = 4.dp),
                fontWeight = fontWeight,
                fontSize = fontSize,
                style = style ?: LocalTextStyle.current
            )
        }
        if (printEnharmonic) {
            Text(text = noteAsStringEnharmonic!!, color = colorResolved)
        }
    }
}

/** Compute the size of the biggest note within a scale.
 * @param notes Notes within one octave of the notes to print.
 * @param notePrintOptions Options which tell how to print notes.
 * @param fontSize Font size.
 * @param octaveRange Range of possible octaves or null if not octaves should be printed.
 * @param measurer Instance to measure text. This is normally created via
 *   rememberTextMeasurer()
 * @param resources Resources which we need to obtain text resources (use e.g.
 *   LocalContext.current.resources)
 * @return Size of largest note.
 */
fun computeMaxNoteSize(
    notes: Array<MusicalNote>,
    notePrintOptions: NotePrintOptions,
    fontSize: TextUnit,
    fontWeight: FontWeight?,
    octaveRange: IntRange?,
    measurer:TextMeasurer,
    resources: Resources
): IntSize {
//    Log.v("Tuner", "Note.computeMaxNoteSize, $measurer")

    // find the widest note when ignoring the octave index
    val widestNoteNoOctave = notes.maxByOrNull { musicalNote ->
        val properties = resolveNoteProperties(
            note = musicalNote,
            notePrintOptions = notePrintOptions,
            resources = resources
        )
        val noteString = createAnnotatedStringOfNote(
            properties, notePrintOptions, fontSize, fontWeight, false
        )
        measurer.measure(noteString).size.width
    } ?: return IntSize.Zero

    val octaveOfWidestNote = octaveRange?.maxByOrNull { octave ->
        val properties = resolveNoteProperties(
            note = widestNoteNoOctave.copy(octave = octave),
            notePrintOptions = notePrintOptions,
            resources = resources
        )
        val noteString = createAnnotatedStringOfNote(
            properties, notePrintOptions, fontSize, fontWeight, true
        )
        measurer.measure(noteString).size.width
    } ?: Int.MAX_VALUE

    val properties = resolveNoteProperties(
        note = widestNoteNoOctave.copy(octave = octaveOfWidestNote),
        notePrintOptions = notePrintOptions,
        resources = resources
    )
    val noteString = createAnnotatedStringOfNote(
        properties, notePrintOptions, fontSize, fontWeight, withOctave = (octaveOfWidestNote != Int.MAX_VALUE)
    )
    val measuredWidestNote = measurer.measure(noteString)

    return measuredWidestNote.size
}

/** Compute and remember the size of the biggest note within a scale.
 * @param notes Notes within one octave.
 * @param notePrintOptions Options which tell how to print notes.
 * @param fontSize Font size.
 * @param octaveRange Range of possible octaves or null if not octaves should be printed.
 * @param textMeasurer Text measurer used to measure text.
 * @return Size of largest note.
 */
@Composable
fun rememberMaxNoteSize(
    notes: Array<MusicalNote>,
    notePrintOptions: NotePrintOptions,
    fontSize: TextUnit,
    octaveRange: IntRange?,
    fontWeight: FontWeight? = null,
    textMeasurer: TextMeasurer = rememberTextMeasurer()
): DpSize {
    val resources = LocalContext.current.resources
    val density = LocalDensity.current
    return remember(notes, notePrintOptions, fontSize, octaveRange, textMeasurer, resources, density) {
        val sizePx1 = computeMaxNoteSize(
            notes, notePrintOptions.copy(useEnharmonic = false),
            fontSize, fontWeight, octaveRange, textMeasurer, resources
        )
        val sizePx2 = computeMaxNoteSize(
            notes, notePrintOptions.copy(useEnharmonic = true),
            fontSize, fontWeight, octaveRange, textMeasurer, resources
        )
        val sizePx = IntSize(
            max(sizePx1.width, sizePx2.width),
            max(sizePx1.height, sizePx2.height)
        )
        DpSize(with(density) { sizePx.width.toDp() }, with(density) { sizePx.height.toDp() }
        )
    }
}

@Preview(showBackground = true, widthDp = 500, heightDp = 1200)
@Composable
private fun NotePreview() {
    val fontSize = 100.sp
    val fontWeight = null // FontWeight.Bold
    val notePrintOptions = remember {
        NotePrintOptions(
            useEnharmonic = false,
            helmholtzNotation = false,
            notationType = NotationType.Solfege
        )
    }
    val maxLabelSize = rememberMaxNoteSize(
        notes = arrayOf(MusicalNote(BaseNote.A, NoteModifier.None)),
        notePrintOptions = notePrintOptions,
        fontSize = fontSize,
        fontWeight = fontWeight,
        octaveRange = 0 .. 12
    )

    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    ){
//        Box(
//            modifier = Modifier.size(maxLabelSize).background(MaterialTheme.colorScheme.surfaceVariant),
//        ) {
            Note(
                MusicalNote(BaseNote.A, NoteModifier.NaturalUp),
                withOctave = true,
                notePrintOptions = notePrintOptions,
                fontSize = fontSize,
                fontWeight = fontWeight,
                //style = MaterialTheme.typography.displayLarge
            )
        Note(
            MusicalNote(BaseNote.A, NoteModifier.FlatDown),
            withOctave = true,
            notePrintOptions = notePrintOptions,
            fontSize = fontSize,
            fontWeight = fontWeight,
            //style = MaterialTheme.typography.displayLarge
        )
        Note(
            MusicalNote(BaseNote.A, NoteModifier.FlatDownDown),
            withOctave = true,
            notePrintOptions = notePrintOptions,
            fontSize = fontSize,
            fontWeight = fontWeight,
            //style = MaterialTheme.typography.displayLarge
        )
        Note(
            MusicalNote(BaseNote.A, NoteModifier.SharpUp),
            withOctave = true,
            notePrintOptions = notePrintOptions,
            fontSize = fontSize,
            fontWeight = fontWeight,
            //style = MaterialTheme.typography.displayLarge
        )
        Note(
            MusicalNote(BaseNote.A, NoteModifier.SharpSharpSharpUpUp),
            withOctave = true,
            notePrintOptions = notePrintOptions,
            fontSize = fontSize,
            fontWeight = fontWeight,
            //style = MaterialTheme.typography.displayLarge
        )
        Note(
            MusicalNote(BaseNote.A, NoteModifier.SharpSharpUpUpUp),
            withOctave = true,
            notePrintOptions = notePrintOptions,
            fontSize = fontSize,
            fontWeight = fontWeight,
            //style = MaterialTheme.typography.displayLarge
        )
        NoteWithEnharmonic(
            MusicalNote(BaseNote.A, NoteModifier.FlatFlatDownDownDown),
            withOctave = true,
            notePrintOptions = notePrintOptions,
            fontSize = fontSize,
            fontWeight = fontWeight,
            //style = MaterialTheme.typography.displayLarge
        )

        NoteWithEnharmonic(
            MusicalNote(
                BaseNote.A, NoteModifier.FlatFlatFlatDownDown,
                enharmonicBase = BaseNote.G,
                enharmonicModifier = NoteModifier.FlatDown),
            withOctave = true,
            notePrintOptions = notePrintOptions,
            fontSize = fontSize,
            fontWeight = fontWeight,
            //style = MaterialTheme.typography.displayLarge
        )
//        }
    }
}