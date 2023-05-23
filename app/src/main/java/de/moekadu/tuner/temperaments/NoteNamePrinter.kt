package de.moekadu.tuner.temperaments

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import androidx.core.content.res.ResourcesCompat
import de.moekadu.tuner.R
import de.moekadu.tuner.views.CustomTypefaceSpan
import de.moekadu.tuner.views.SmallSuperScriptSpan
import kotlin.math.max
import kotlin.math.min

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

class NoteNamePrinter(
    private val context: Context,
    val sharpFlatPreference: SharpFlatPreference,
    private val noteResourceIds: Map<NoteNameStem, Int>,
    val noteNameWidth: MaxNoteNameWidth,
    val hasSharpFlatCounterpart: Boolean = true
) {
    enum class MaxNoteNameWidth {
        SingleLetter, // C, D, E, ... or similar
        MultipleLetters // Do, Re, Mi, ... or similar
    }
    enum class SharpFlatPreference {
        Sharp, Flat, None
    }
    private class ResolvedNoteProperties(val baseName: String, val modifier: NoteModifier, val octave: Int)

    private val typeface = ResourcesCompat.getFont(context, R.font.gonville) ?: throw RuntimeException("Error")
    private val octaveSpan = SmallSuperScriptSpan()

    fun noteToCharSequence(note: MusicalNote, withOctave: Boolean): CharSequence {
        val properties = resolveNoteProperties(note)

        val spannable = SpannableStringBuilder()

        // prefix (i.e. ups/downs)
        if (properties.modifier != NoteModifier.None && modifierPrefixStrings[properties.modifier] != "") {
            val spannableSource = modifierPrefixStrings[properties.modifier] + "\u200a"
            spannable.append(
                SpannableString(spannableSource).apply {
                    setSpan(CustomTypefaceSpan(typeface), 0, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                }
            )
        }

        // base note name
        spannable.append(properties.baseName)

        // postfix with spaces (\u200a is a narrow space)
        if (properties.modifier != NoteModifier.None && modifierPostfixStrings[properties.modifier] != "") {
            val spannableSource = if (withOctave && properties.octave != Int.MAX_VALUE)
                "\u200a" + modifierPostfixStrings[properties.modifier] + "\u200a"
            else
                "\u200a" + modifierPostfixStrings[properties.modifier]

            spannable.append(
                SpannableString(spannableSource).apply {
                    setSpan(CustomTypefaceSpan(typeface), 0, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                }
            )
        } else {
            // append a zero-width space, such that the symbol-typeface is used
            // this ensures, that the total text height is the same unregarding if a note has a modifier
            // or not.
            spannable.append(
                SpannableString("\u200b").apply {
                    setSpan(CustomTypefaceSpan(typeface), 0, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                }
            )
        }

        if (withOctave && properties.octave != Int.MAX_VALUE) {
            spannable.append(
                SpannableString(properties.octave.toString()).apply {
                    setSpan(SmallSuperScriptSpan(),0, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                }
            )
        }
        return spannable
    }

    fun measure(paint: Paint, note: MusicalNote, withOctave: Boolean): RectF {
        val properties = resolveNoteProperties(note)
        val bounds = Rect()
        paint.getTextBounds(properties.baseName, 0, properties.baseName.length, bounds)
        val result = RectF()
        result.top = bounds.top.toFloat()
        result.bottom = bounds.bottom.toFloat()
        result.left = bounds.left.toFloat()
        result.right = bounds.right.toFloat()

        if (withOctave && properties.octave != Int.MAX_VALUE) {
            val octaveBounds = measureOctaveIndex(paint, properties.octave)
            result.right += octaveBounds.width()
            result.top = min(result.top, octaveBounds.top)
            result.bottom = max(result.bottom, octaveBounds.bottom)
        }

        if (properties.modifier != NoteModifier.None) {
            var modifierString = modifierPrefixStrings[properties.modifier]
            if (modifierPrefixStrings[properties.modifier] != null && modifierPrefixStrings[properties.modifier] != "")
                modifierString += "\u200a"
            if (modifierPostfixStrings[properties.modifier] != null && modifierPostfixStrings[properties.modifier] != "")
                modifierString += modifierPostfixStrings[properties.modifier] + "\u200a"
            if (withOctave && properties.octave != Int.MAX_VALUE)
                modifierString += "\u200a"

            if (modifierString != "") {
                //val modifierStringWithSpaces = "\u200a" + modifierString + "\u200a"
                val oldTypeface = paint.typeface
                paint.typeface = typeface

                // once measure only something (we use a sharp-character here, but it could be anything
                // and once measure the modifier with two spaces inbetween and ended by the sharp
                // the modifier width with the spaces is then computed by subtracting the width of
                // only the sharp sign. This strategy is needed, since leading and trailing spaces
                // are ignored by the measuring procedure.
                val sharpString = "\uE10A"
                paint.getTextBounds(sharpString, 0, sharpString.length, bounds)
                val widthSharp = bounds.width()

                val modifierStringWithSpacesAndSharp = modifierString + sharpString
                paint.getTextBounds(modifierStringWithSpacesAndSharp, 0, modifierStringWithSpacesAndSharp.length, bounds)
                paint.typeface = oldTypeface
                result.right += bounds.width() - widthSharp
                result.top = min(bounds.top.toFloat(), result.top)
                result.bottom = max(bounds.bottom.toFloat(), result.bottom)
            }
        }
        return result
    }

    fun measureOctaveIndex(paint: Paint, octaveIndex: Int): RectF {
        val boundsInt = Rect()
        val octaveString = octaveIndex.toString()
        paint.getTextBounds(octaveString, 0, octaveString.length, boundsInt)
        return RectF(
            octaveSpan.scaleTextSize * boundsInt.left,
            octaveSpan.scaleTextSize * boundsInt.top + octaveSpan.moveUpByPartOfAscent * paint.ascent(),
            octaveSpan.scaleTextSize * boundsInt.right,
            octaveSpan.scaleTextSize * boundsInt.bottom + octaveSpan.moveUpByPartOfAscent * paint.ascent(),
        )
    }

    fun measureOctaveIndexLeadingSpace(paint: Paint): Int {
        val oldTypeface = paint.typeface
        paint.typeface = typeface
        val boundsInt = Rect()
        val sharpString = "\uE10A"
        val sharpsEnclosingSpace = sharpString + "\u200a" + sharpString
        val sharpsNoSpace = sharpString + sharpString
        paint.getTextBounds(sharpsEnclosingSpace, 0, sharpsEnclosingSpace.length, boundsInt)
        val w = boundsInt.width()
        paint.getTextBounds(sharpsNoSpace, 0, sharpsNoSpace.length, boundsInt)
        val v = boundsInt.width()
        paint.typeface = oldTypeface
        return w - v
    }

    private fun resolveNoteProperties(note: MusicalNote): ResolvedNoteProperties {
        return if (preferEnharmonic(note)) {
            resolveNotePropertiesWithoutEnharmonicCheck(note.switchEnharmonic(switchAlsoForBaseNone = true))
        } else {
            resolveNotePropertiesWithoutEnharmonicCheck(note)
        }
    }

    private fun resolveNotePropertiesWithoutEnharmonicCheck(note: MusicalNote): ResolvedNoteProperties {
        // check if we can directly resolve the note
        val stem = NoteNameStem.fromMusicalNote(note)
        val noteName = noteResourceIds[stem]?.let { context.getString(it) }
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
        val noteNameEnharmonic = noteResourceIds[stemEnharmonic]?.let { context.getString(it) }

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
        val noteNameBase = noteResourceIds[stemBase]?.let { context.getString(it) }
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
        val noteNameEnharmonicBase = noteResourceIds[stemEnharmonicBase]?.let { context.getString(it) }
        if (noteNameEnharmonicBase != null && noteNameEnharmonicBase != "" && noteNameEnharmonicBase != "-") {
            return ResolvedNoteProperties(
                baseName = noteNameEnharmonicBase,
                modifier = noteEnharmonic.modifier,
                octave = if (note.octave == Int.MAX_VALUE) Int.MAX_VALUE else noteEnharmonic.octave + noteEnharmonic.octaveOffset
            )
        }
        return ResolvedNoteProperties("X", NoteModifier.None, note.octave)
    }

    private fun preferEnharmonic(note: MusicalNote): Boolean {
        if (note.enharmonicBase == BaseNote.None)
            return sharpFlatPreference == SharpFlatPreference.Flat

        return when (sharpFlatPreference) {
            SharpFlatPreference.None -> {
                false
            }
            SharpFlatPreference.Sharp -> {
                if (note.enharmonicModifier.flatSharpIndex() == note.modifier.flatSharpIndex())
                    false
                else
                    note.enharmonicModifier.flatSharpIndex() > note.modifier.flatSharpIndex()
            }
            SharpFlatPreference.Flat -> {
                if (note.enharmonicModifier.flatSharpIndex() == note.modifier.flatSharpIndex())
                    true
                else
                    note.enharmonicModifier.flatSharpIndex() < note.modifier.flatSharpIndex()
            }
        }
    }
}

