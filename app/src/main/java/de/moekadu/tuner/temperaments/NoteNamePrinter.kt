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

private val baseNoteResourceIds = mapOf(
    BaseNote.C to R.string.c_note_name,
    BaseNote.D to R.string.d_note_name,
    BaseNote.E to R.string.e_note_name,
    BaseNote.F to R.string.f_note_name,
    BaseNote.G to R.string.g_note_name,
    BaseNote.A to R.string.a_note_name,
    BaseNote.B to R.string.b_note_name,
)

private val baseNoteSolfegeResourceIds = mapOf(
    BaseNote.C to R.string.c_solfege,
    BaseNote.D to R.string.d_solfege,
    BaseNote.E to R.string.e_solfege,
    BaseNote.F to R.string.f_solfege,
    BaseNote.G to R.string.g_solfege,
    BaseNote.A to R.string.a_solfege,
    BaseNote.B to R.string.b_solfege,
)

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

private val specialNoteNameResourceIds = mapOf(
    NoteNameStem(BaseNote.B, NoteModifier.Flat, BaseNote.A, NoteModifier.Sharp) to R.string.asharp_bflat_note_name,
    NoteNameStem(BaseNote.A, NoteModifier.Sharp, BaseNote.B, NoteModifier.Flat) to R.string.asharp_bflat_note_name
)

enum class MusicalNotePrintOptions {
    None {
        override fun contains(modifier: NoteModifier): Boolean {
            return true
        }
        override val isSolfege = false
        override val isPreferringFlat = false
    },
    PreferFlat {
        override fun contains(modifier: NoteModifier): Boolean {
            return when (modifier) {
                NoteModifier.Flat, NoteModifier.FlatUp, NoteModifier.FlatUpUp, NoteModifier.FlatDown, NoteModifier.FlatDownDown -> true
                else -> false
            }
        }
        override val isSolfege = false
        override val isPreferringFlat = true
    },
    PreferSharp {
        override fun contains(modifier: NoteModifier): Boolean {
            return when (modifier) {
                NoteModifier.Sharp, NoteModifier.SharpUp, NoteModifier.SharpUpUp, NoteModifier.SharpDown, NoteModifier.SharpDownDown -> true
                else -> false
            }
        }
        override val isSolfege = false
        override val isPreferringFlat = false
    },
    SolfegePreferFlat {
        override fun contains(modifier: NoteModifier): Boolean {
            return when (modifier) {
                NoteModifier.Flat, NoteModifier.FlatUp, NoteModifier.FlatUpUp, NoteModifier.FlatDown, NoteModifier.FlatDownDown -> true
                else -> false
            }
        }
        override val isSolfege = true
        override val isPreferringFlat = true
    },
    SolfegePreferSharp {
        override fun contains(modifier: NoteModifier): Boolean {
            return when (modifier) {
                NoteModifier.Sharp, NoteModifier.SharpUp, NoteModifier.SharpUpUp, NoteModifier.SharpDown, NoteModifier.SharpDownDown -> true
                else -> false
            }
        }
        override val isSolfege = true
        override val isPreferringFlat = false
    };
    abstract fun contains(modifier: NoteModifier): Boolean
    abstract val isSolfege: Boolean
    abstract val isPreferringFlat: Boolean
}

//fun NoteModifier.isOfCategory(printOption: MusicalNotePrintOptions): Boolean {
//    return when (printOption) {
//        MusicalNotePrintOptions.None -> {
//            true
//        }
//        MusicalNotePrintOptions.PreferFlat -> {
//            when (this) {
//                NoteModifier.Flat, NoteModifier.FlatUp, NoteModifier.FlatUpUp, NoteModifier.FlatDown, NoteModifier.FlatDownDown -> true
//                else -> false
//            }
//        }
//        MusicalNotePrintOptions.PreferSharp -> {
//            when (this) {
//                NoteModifier.Sharp, NoteModifier.SharpUp, NoteModifier.SharpUpUp, NoteModifier.SharpDown, NoteModifier.SharpDownDown -> true
//                else -> false
//            }
//        }
//    }
//}

class NoteNamePrinter(private val context: Context) {
    private val typeface = ResourcesCompat.getFont(context, R.font.gonville) ?: throw RuntimeException("Error")
    private val octaveSpan = SmallSuperScriptSpan()
    private class ResolvedNoteProperties(val baseName: String, val modifier: NoteModifier, val octave: Int)

    fun noteToCharSequence(note: MusicalNote, printOption: MusicalNotePrintOptions, withOctave: Boolean): CharSequence {
        val properties = resolveNoteProperties(note, printOption)

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
                    setSpan(SmallSuperScriptSpan(),0, length,Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                }
            )
        }
        return spannable
    }

    fun measure(paint: Paint, note: MusicalNote, printOption: MusicalNotePrintOptions, withOctave: Boolean): RectF {
        val properties = resolveNoteProperties(note, printOption)
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

    private fun resolveNoteProperties(note: MusicalNote, printOption: MusicalNotePrintOptions): ResolvedNoteProperties {
        val stem = NoteNameStem.fromMusicalNote(note)

        val specialNoteName = specialNoteNameResourceIds[stem]?.let { context.getString(it) }
        val baseNote: String
        val octaveIndex: Int
        val modifier: NoteModifier

        if (specialNoteName != null && specialNoteName != "" && specialNoteName != "-" && !printOption.isSolfege) {
            octaveIndex = note.octave + note.octaveOffset
            baseNote = specialNoteName
            modifier = NoteModifier.None
        } else if (preferEnharmonic(note, printOption)) {
            baseNote = if (printOption.isSolfege)
                context.getString(baseNoteSolfegeResourceIds[note.enharmonicBase]!!)
            else
                context.getString(baseNoteResourceIds[note.enharmonicBase]!!)
            modifier = note.enharmonicModifier
            octaveIndex =
                if (note.octave == Int.MAX_VALUE) Int.MAX_VALUE else note.octave + note.enharmonicOctaveOffset
        } else {
            baseNote = if (printOption.isSolfege)
                context.getString(baseNoteSolfegeResourceIds[note.base]!!)
            else
                context.getString(baseNoteResourceIds[note.base]!!)
            modifier = note.modifier
            octaveIndex = note.octave + note.octaveOffset
        }
        return ResolvedNoteProperties(baseNote, modifier, octaveIndex)
    }

    private fun preferEnharmonic(note: MusicalNote, printOption: MusicalNotePrintOptions): Boolean {
        if (note.enharmonicBase == BaseNote.None)
            return false

        return when (printOption) {
            MusicalNotePrintOptions.None -> {
                false
            }
            MusicalNotePrintOptions.PreferSharp, MusicalNotePrintOptions.SolfegePreferSharp -> {
                note.enharmonicModifier.flatSharpIndex() > note.modifier.flatSharpIndex()
            }
            MusicalNotePrintOptions.PreferFlat, MusicalNotePrintOptions.SolfegePreferFlat -> {
                note.enharmonicModifier.flatSharpIndex() < note.modifier.flatSharpIndex()
            }
        }
    }
}
