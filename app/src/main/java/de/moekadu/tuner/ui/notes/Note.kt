package de.moekadu.tuner.ui.notes

import android.content.res.Resources
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.temperaments.NoteNameScale
import de.moekadu.tuner.temperaments.NoteNameStem
import de.moekadu.tuner.temperaments.createNoteNameScale53Tone
import de.moekadu.tuner.temperaments.flatSharpIndex

private val modifierPostfixStrings = mapOf(
    NoteModifier.None to "",
    NoteModifier.Sharp to "\uE10A",
    NoteModifier.SharpUp to "\uE10A",
    NoteModifier.SharpUpUp to "\uE10A",
    NoteModifier.SharpDown to "\uE10A",
    NoteModifier.SharpDownDown to "\uE10A",
    NoteModifier.Flat to "\uE100",
    NoteModifier.FlatUp to "\uE100",
    NoteModifier.FlatUpUp to "\uE100",
    NoteModifier.FlatDown to "\uE100",
    NoteModifier.FlatDownDown to "\uE100",
    NoteModifier.NaturalUp to "",
    NoteModifier.NaturalUpUp to "",
    NoteModifier.NaturalDown to "",
    NoteModifier.NaturalDownDown to "",
)

private val modifierPrefixStrings = mapOf(
    NoteModifier.None to "",
    NoteModifier.Sharp to "",
    NoteModifier.SharpUp to "\uE111",
    NoteModifier.SharpUpUp to "\uE112",
    NoteModifier.SharpDown to "\uE10F",
    NoteModifier.SharpDownDown to "\uE110",
    NoteModifier.Flat to "",
    NoteModifier.FlatUp to "\uE111",
    NoteModifier.FlatUpUp to "\uE112",
    NoteModifier.FlatDown to "\uE10F",
    NoteModifier.FlatDownDown to "\uE110",
    NoteModifier.NaturalUp to "\uE111",
    NoteModifier.NaturalUpUp to "\uE112",
    NoteModifier.NaturalDown to "\uE10F",
    NoteModifier.NaturalDownDown to "\uE110",
)


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
    return if (preferEnharmonic(note, notePrintOptions.sharpFlatPreference)) {
        resolveNotePropertiesWithoutEnharmonicCheck(
            note.switchEnharmonic(switchAlsoForBaseNone = true),
            resources = resources,
        ) { notePrintOptions.resourceId(it) }
    } else {
        resolveNotePropertiesWithoutEnharmonicCheck(
            note,
            resources = resources,
        ) { notePrintOptions.resourceId(it) }
    }
}

private fun resolveNotePropertiesWithoutEnharmonicCheck(
    note: MusicalNote,
    resources: Resources,
    resourceIdOfStem: (NoteNameStem) -> Int?
    ): ResolvedNoteProperties {
    // check if we can directly resolve the note
    val stem = NoteNameStem.fromMusicalNote(note)
    val noteName = resourceIdOfStem(stem)?.let {resources.getText(it)}
    if (noteName != null && noteName != "" && noteName != "-") {
        return ResolvedNoteProperties(
            baseName = noteName,
            modifier = NoteModifier.None,
            octave = if (note.octave == Int.MAX_VALUE) Int.MAX_VALUE else note.octave + note.octaveOffset
        )
    }

    // check if we can directly resolve the enharmonic
    val noteEnharmonic = note.switchEnharmonic(switchAlsoForBaseNone = true)
    val stemEnharmonic = NoteNameStem.fromMusicalNote(noteEnharmonic)
    val noteNameEnharmonic = resourceIdOfStem(stemEnharmonic)?.let { resources.getText(it) }

    if (noteNameEnharmonic != null && noteNameEnharmonic != "" && noteNameEnharmonic != "-") {
        return ResolvedNoteProperties(
            baseName = noteNameEnharmonic,
            modifier = NoteModifier.None,
            octave = if (note.octave == Int.MAX_VALUE) Int.MAX_VALUE else noteEnharmonic.octave + noteEnharmonic.octaveOffset
        )
    }

    // try to resolve note by base name + modifier
    val stemBase = NoteNameStem(
        note.base, NoteModifier.None, BaseNote.None, NoteModifier.None
    )
    val noteNameBase = resourceIdOfStem(stemBase)?.let { resources.getText(it) }
    if (noteNameBase != null && noteNameBase != "" && noteNameBase != "-") {
        return ResolvedNoteProperties(
            baseName = noteNameBase,
            modifier = note.modifier,
            octave = if (note.octave == Int.MAX_VALUE) Int.MAX_VALUE else note.octave + note.octaveOffset
        )
    }

    // try to resolve note by enharmonic base name + modifier
    val stemEnharmonicBase = NoteNameStem(
        noteEnharmonic.base, NoteModifier.None, BaseNote.None, NoteModifier.None
    )
    val noteNameEnharmonicBase = resourceIdOfStem(stemEnharmonicBase)?.let { resources.getText(it) }
    if (noteNameEnharmonicBase != null && noteNameEnharmonicBase != "" && noteNameEnharmonicBase != "-") {
        return ResolvedNoteProperties(
            baseName = noteNameEnharmonicBase,
            modifier = noteEnharmonic.modifier,
            octave = if (note.octave == Int.MAX_VALUE) Int.MAX_VALUE else noteEnharmonic.octave + noteEnharmonic.octaveOffset
        )
    }
    return ResolvedNoteProperties("X", NoteModifier.None, note.octave)
}

private fun preferEnharmonic(
    note: MusicalNote,
    sharpFlatPreference: NotePrintOptions.SharpFlatPreference): Boolean {
    if (note.enharmonicBase == BaseNote.None)
        return sharpFlatPreference == NotePrintOptions.SharpFlatPreference.Flat

    return when (sharpFlatPreference) {
        NotePrintOptions.SharpFlatPreference.None -> {
            false
        }
        NotePrintOptions.SharpFlatPreference.Sharp -> {
            if (note.enharmonicModifier.flatSharpIndex() == note.modifier.flatSharpIndex())
                false
            else
                note.enharmonicModifier.flatSharpIndex() > note.modifier.flatSharpIndex()
        }
        NotePrintOptions.SharpFlatPreference.Flat -> {
            if (note.enharmonicModifier.flatSharpIndex() == note.modifier.flatSharpIndex())
                true
            else
                note.enharmonicModifier.flatSharpIndex() < note.modifier.flatSharpIndex()
        }
    }
}

/** Options for note printing.
 * @param sharpFlatPreference Tells if sharp or flat signs should be preferred for enharmonics.
 * @param helmholtzNotation Set this to true, to use Helmholtz notation (this uses small letters
 *   for higher octaves and capital letters for lower octaves. Also for octaves oround 2 and 3, no
 *   octaves numbers are printed but instead , and '.
 * @param notationType Notation type used for printing.
 */
@Stable
data class NotePrintOptions(
    val sharpFlatPreference: SharpFlatPreference = SharpFlatPreference.None,
    val helmholtzNotation: Boolean = false,
    val notationType: NotationType = NotationType.Standard
) {
    enum class SharpFlatPreference {
        Sharp, Flat, None
    }

    private val resourceIds = notationType.resourceIds()
    fun resourceId(noteNameStem: NoteNameStem) = resourceIds[noteNameStem]
}

private val musicalSymbolFont = FontFamily(Font(R.font.gonville))

private fun createAnnotatedStringOfNote(
    properties: ResolvedNoteProperties,
    notePrintOptions: NotePrintOptions,
    fontSize: TextUnit,
    withOctave: Boolean
): AnnotatedString {
    return buildAnnotatedString {
        if (properties.modifier != NoteModifier.None && modifierPrefixStrings[properties.modifier] != "") {
            withStyle(
                SpanStyle(
                    fontFamily = musicalSymbolFont,
                    fontSize = fontSize
                )
            ) {
                append(modifierPrefixStrings[properties.modifier])
                append("\u200a")
            }
        }

        withStyle(
            SpanStyle(
                fontSize = fontSize
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
                    fontSize = fontSize
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
                    fontSize = fontSize
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
                        withStyle(SpanStyle(fontSize = fontSize)) {
                            append(",,")
                        }
                    }

                    properties.octave == 1 -> {
                        withStyle(SpanStyle(fontSize = fontSize)) {
                            append(",")
                        }
                    }

                    properties.octave == 2 || properties.octave == 3 -> {
                        withStyle(SpanStyle(fontSize = fontSize)) {
                            append("")
                        }
                    }

                    properties.octave == 4 -> {
                        withStyle(SpanStyle(fontSize = fontSize)) {
                            append("'")
                        }
                    }

                    properties.octave == 5 -> {
                        withStyle(SpanStyle(fontSize = fontSize)) {
                            append("''")
                        }
                    }

                    else -> {  // -> octave >= 6
                        withStyle(SpanStyle(fontSize = fontSize * 0.7f)) {
                            withStyle(SpanStyle(baselineShift = BaselineShift(0.5f))) {
                                append((properties.octave - 3).toString())
                            }
                        }
                    }
                }
            } else {
                withStyle(SpanStyle(fontSize = fontSize * 0.7f)) {
                    withStyle(SpanStyle(baselineShift = BaselineShift(0.5f))) {
                        append(properties.octave.toString())
                    }
                }
            }
        }
    }
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
    color: Color = Color.Unspecified
) {
    val fontSizeResolved = fontSize.takeOrElse {
        LocalTextStyle.current.fontSize.takeOrElse { 12.sp }
    }
    val colorResolved = color.takeOrElse {
        LocalTextStyle.current.color.takeOrElse { Color.Black }
    }

    val properties = resolveNoteProperties(
        note = musicalNote,
        notePrintOptions = notePrintOptions,
        resources = LocalContext.current.resources
    )


    val noteAsString = remember(musicalNote, notePrintOptions, fontSize, withOctave) {
        createAnnotatedStringOfNote(properties, notePrintOptions, fontSizeResolved, withOctave)
    }

    Text(text = noteAsString, modifier = modifier, color = colorResolved)
}

/** Compute the size of the biggest note within a scale.
 * @param noteNameScale Scale which tells which notes to print.
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
    noteNameScale: NoteNameScale,
    notePrintOptions: NotePrintOptions,
    fontSize: TextUnit,
    octaveRange: IntRange?,
    measurer:TextMeasurer,
    resources: Resources
): IntSize {
//    Log.v("Tuner", "Note.computeMaxNoteSize, $measurer")

    // find the widest note when ignoring the octave index
    val widestNoteNoOctave = noteNameScale.notes.maxByOrNull { musicalNote ->
        val properties = resolveNoteProperties(
            note = musicalNote,
            notePrintOptions = notePrintOptions,
            resources = resources
        )
        val noteString = createAnnotatedStringOfNote(
            properties, notePrintOptions, fontSize, false
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
            properties, notePrintOptions, fontSize, true
        )
        measurer.measure(noteString).size.width
    } ?: Int.MAX_VALUE

    val properties = resolveNoteProperties(
        note = widestNoteNoOctave.copy(octave = octaveOfWidestNote),
        notePrintOptions = notePrintOptions,
        resources = resources
    )
    val noteString = createAnnotatedStringOfNote(
        properties, notePrintOptions, fontSize, withOctave = (octaveOfWidestNote != Int.MAX_VALUE)
    )
    val measuredWidestNote = measurer.measure(noteString)

    return measuredWidestNote.size
}

/** Compute and remember the size of the biggest note within a scale.
 * @param noteNameScale Scale which tells which notes to print.
 * @param notePrintOptions Options which tell how to print notes.
 * @param fontSize Font size.
 * @param octaveRange Range of possible octaves or null if not octaves should be printed.
 * @param textMeasurer Text measurer used to measure text.
 * @return Size of largest note.
 */
@Composable
fun rememberMaxNoteSize(
    noteNameScale: NoteNameScale,
    notePrintOptions: NotePrintOptions,
    fontSize: TextUnit,
    octaveRange: IntRange?,
    textMeasurer: TextMeasurer = rememberTextMeasurer()
): DpSize {
    val resources = LocalContext.current.resources
    val density = LocalDensity.current
    return remember(noteNameScale, notePrintOptions, fontSize, octaveRange, textMeasurer, resources, density) {
        val sizePx = computeMaxNoteSize(noteNameScale, notePrintOptions, fontSize, octaveRange, textMeasurer, resources)
        DpSize(with(density) { sizePx.width.toDp() }, with(density) { sizePx.height.toDp() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotePreview() {
    val noteNameScale = remember { createNoteNameScale53Tone(null) }
    val musicalNote = noteNameScale.notes[12].copy(octave = 9)
    val fontSize = 100.sp
    val notePrintOptions = remember {
        NotePrintOptions(
            sharpFlatPreference = NotePrintOptions.SharpFlatPreference.Sharp,
            helmholtzNotation = false,
            notationType = NotationType.Standard
        )
    }
    val maxLabelSizePx = computeMaxNoteSize(
        noteNameScale = createNoteNameScale53Tone(null),
        notePrintOptions = notePrintOptions,
        fontSize = fontSize,
        octaveRange = 0 .. 12,
        measurer = rememberTextMeasurer(),
        resources = LocalContext.current.resources
    )
    val density = LocalDensity.current
    val maxLabelSize = DpSize(
            with(density) { maxLabelSizePx.width.toDp() },
            with(density) { maxLabelSizePx.height.toDp() }
        )


    Box(
      contentAlignment = Alignment.Center,
        modifier = Modifier.size(maxLabelSize),

    ) {

        Note(musicalNote,
            fontSize = fontSize,
            withOctave = true,
            notePrintOptions = notePrintOptions
        )
    }
}