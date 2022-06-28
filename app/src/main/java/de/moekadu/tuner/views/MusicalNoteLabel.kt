package de.moekadu.tuner.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.core.graphics.withTranslation
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.temperaments.NoteNameStem
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val baseNoteResourceIds = mapOf(
    BaseNote.C to R.string.c_note_name,
    BaseNote.D to R.string.d_note_name,
    BaseNote.E to R.string.e_note_name,
    BaseNote.F to R.string.f_note_name,
    BaseNote.G to R.string.g_note_name,
    BaseNote.A to R.string.a_note_name,
    BaseNote.B to R.string.b_note_name,
)

private val modifierStrings = mapOf(
    NoteModifier.None to "",
    NoteModifier.Sharp to "\u266F",
    NoteModifier.Flat to "\u266D"
)

private val specialNoteNameResourceIds = mapOf(
    NoteNameStem(BaseNote.B, NoteModifier.Flat, BaseNote.A, NoteModifier.Sharp) to R.string.asharp_bflat_note_name,
    NoteNameStem(BaseNote.A, NoteModifier.Sharp, BaseNote.B, NoteModifier.Flat) to R.string.asharp_bflat_note_name
)

/** Class for creating note labels, where the octave is typed slightly smaller and as superscript. */
class MusicalNoteLabel(val note: MusicalNote, paint: TextPaint, context: Context,
                       backgroundPaint: Paint? = null, cornerRadius: Float = 0f,
                       gravity: LabelGravity = LabelGravity.Center,
                       val enableOctaveIndex: Boolean = true,
                       paddingLeft: Float = 0f, paddingRight: Float = 0f,
                       paddingTop: Float = 0f, paddingBottom: Float = 0f)
    : Label(backgroundPaint, cornerRadius, gravity, paddingLeft, paddingRight, paddingTop, paddingBottom) {

    /** Convenience note class storing the note base part (e.g. C#) and the octave as separate strings. */
    data class LabelSubstrings(val note: String, val octave: String) {
        companion object {
            /** Create string for the note base part (e.g. C#)
             * @param note Musical note .
             * @param context Context for resolving string resources.
             * @return String of base part.
             */
            fun createNoteSubstring(note: MusicalNote, context: Context): String {
                val specialNoteNameResourceId = specialNoteNameResourceIds[NoteNameStem.fromMusicalNote(note)]

                return if (specialNoteNameResourceId == null)
                    context.getString(baseNoteResourceIds[note.base]!!) + modifierStrings[note.modifier]!!
                else
                    context.getString(specialNoteNameResourceId)
            }

            /** Create string for octave number.
             * @param octave Octave number.
             * @return String of octave number.
             */
            fun createOctaveSubstring(octave: Int): String {
                return octave.toString()
            }

            /** Factory class to create the LabelSubstrings class.
             * @param note Musical note.
             * @param context Context for resolving string resources.
             * @return LabelSubstrings class for the given note.
             */
            fun create(note: MusicalNote, context: Context): LabelSubstrings {
                val noteName = createNoteSubstring(note, context)
                val octaveText = createOctaveSubstring(note.octave)
                return LabelSubstrings(noteName, octaveText)
            }
        }
    }

    /** Span for creating superscripts which are typed with a reduce size.
     * @param scaleTextSize Scaling factor for making the superscript smaller.
     * @param moveUpByPartOfAscent Defines how much the superscript should moved up. This is measured
     *   by as parts of ascent of the font, e.g. 0.5f moves the number up by half of the ascent.
     */
    private class SmallSuperScriptSpan(val scaleTextSize: Float = 0.7f, val moveUpByPartOfAscent: Float = 0.4f) : MetricAffectingSpan() {
        override fun updateDrawState(p0: TextPaint?) {
            p0?.let { textPaint ->
                textPaint.baselineShift += (moveUpByPartOfAscent * textPaint.ascent()).roundToInt()
                textPaint.textSize = scaleTextSize * textPaint.textSize
            }
        }

        override fun updateMeasureState(p0: TextPaint) {
            p0.baselineShift += (moveUpByPartOfAscent * p0.ascent()).roundToInt()
            p0.textSize = scaleTextSize * p0.textSize
        }
    }

    private val substrings = LabelSubstrings.create(note, context)

    private val bounds = Rect().apply {
        getBounds(substrings, paint, this, enableOctaveIndex = enableOctaveIndex)
    }

    private val spannableString = getSpannableString()
    private val desiredWidth = StaticLayout.getDesiredWidth(spannableString, paint).roundToInt()
    private val layout = StaticLayout.Builder.obtain(spannableString, 0, spannableString.length, paint, desiredWidth).build()
    private val baselineOfLayout = layout.getLineBaseline(0)

    override val labelWidth: Float
        get() = bounds.width().toFloat()
    override val labelHeight: Float
        get() = bounds.height().toFloat()
    override val labelBaselineBelowTop: Float
        get() = -bounds.top.toFloat()
    override val labelBottomBelowBaseline: Float
        get() = bounds.bottom.toFloat()

    override fun drawToCanvas(positionX: Float, positionY: Float, anchor: LabelAnchor, canvas: Canvas?) {
        if (canvas == null)
            return

        val x = when (anchor) {
            LabelAnchor.North, LabelAnchor.South, LabelAnchor.Center, LabelAnchor.Baseline -> positionX - bounds.centerX()
            LabelAnchor.NorthEast, LabelAnchor.SouthEast, LabelAnchor.East, LabelAnchor.BaselineEast -> positionX - labelWidth
            LabelAnchor.NorthWest, LabelAnchor.SouthWest, LabelAnchor.West, LabelAnchor.BaselineWest -> positionX
        }
        val y = when (anchor) {
                LabelAnchor.North, LabelAnchor.NorthEast, LabelAnchor.NorthWest -> positionY - baselineOfLayout - bounds.top
                LabelAnchor.South, LabelAnchor.SouthEast, LabelAnchor.SouthWest -> positionY - baselineOfLayout - bounds.bottom
                LabelAnchor.Center, LabelAnchor.East, LabelAnchor.West -> positionY - baselineOfLayout - bounds.centerY()
                LabelAnchor.Baseline, LabelAnchor.BaselineEast, LabelAnchor.BaselineWest -> positionY - baselineOfLayout
            }

        canvas.withTranslation(x, y) {
            layout.draw(this)
        }
    }

    private fun getSpannableString(): CharSequence {
        val spannable = SpannableStringBuilder().append(substrings.note)

        if (enableOctaveIndex) {
            spannable.append(SpannableString(substrings.octave).apply {
                setSpan(smallSuperScriptSpan, 0, length, 0)
            })
        }
        return spannable
    }

    companion object {
        private val smallSuperScriptSpan = SmallSuperScriptSpan()

        /** Get bounds of a note, which already is a LabelSubstrings object.
         * Using the substrings object instead of a note avoids extra allocations if this
         * should be called more than one time.
         * @param substrings LabelSubstrings object, created from a note.
         * @param paint Paint which should be used for drawing the label text.
         * @param bounds Rectangle bounds object, where we store the bounds.
         * @param enableOctaveIndex Set this to false if you don't want to print the octave index.
         */
        fun getBounds(substrings: LabelSubstrings, paint: TextPaint, bounds: Rect,
                      enableOctaveIndex: Boolean = true) {
            val noteName = substrings.note

            if (!enableOctaveIndex) {
                paint.getTextBounds(noteName, 0, noteName.length, bounds)
                return
            }

            val octaveText = substrings.octave
            paint.getTextBounds(noteName + octaveText, 0, noteName.length + octaveText.length, bounds)

            val totalWidthNoModifiedOct = bounds.width()
            paint.getTextBounds(octaveText, 0, octaveText.length, bounds)
            val octaveTextWidth = bounds.width()
            val octaveTextTop = bounds.top

            paint.getTextBounds(noteName, 0, noteName.length, bounds)
            val letterSpacing = totalWidthNoModifiedOct - octaveTextWidth - bounds.width()

            val scale = smallSuperScriptSpan.scaleTextSize
            val offset = smallSuperScriptSpan.moveUpByPartOfAscent
            bounds.right += (scale * octaveTextWidth + letterSpacing).roundToInt()
            bounds.top = min(bounds.top, (scale * octaveTextTop + offset * paint.ascent()).roundToInt())
        }

        /** Get bounds of a note.
         * @param note Note which should be drawn.
         * @param paint Paint which should be used for drawing the label text.
         * @param context Context required for accessing string resources.
         * @param bounds Rectangle bounds object, where we store the bounds.
         * @param enableOctaveIndex Set this to false if you don't want to print the octave index.
         */
        fun getBounds(note: MusicalNote, paint: TextPaint, context: Context, bounds: Rect,
                      enableOctaveIndex: Boolean = true) {
            val substrings = LabelSubstrings.create(note, context)
            getBounds(substrings, paint, bounds, enableOctaveIndex = enableOctaveIndex)
        }

        /** Get outer bounds of several labels to be printed.
         * This can be used to compute an common rectangle size for many notes, such that
         * all notes will fit into this rectangle.
         * @param notes Notes, which should fit in the resulting bounds, octave is ignored.
         * @param octaveBegin Smallest octave to be considered.
         * @param octaveEnd End octave to be considered (excluded).
         * @param paint Paint which will be used for drawing the notes.
         * @param context Context required for accessing string resources.
         * @param enableOctaveIndex Set this to false if you don't want to print the octave index.
         * @return Bounds of the rectangle which fits around each single note.
         */
        fun getLabelSetBounds(notes: Array<MusicalNote>, octaveBegin: Int, octaveEnd: Int, paint: TextPaint, context: Context,
                              enableOctaveIndex: Boolean = true): LabelSetBounds {
            val rect = Rect()

            // get letter spacing by measuring MM and M
            paint.getTextBounds("M", 0, 1, rect)
            val widthM = rect.width()
            paint.getTextBounds("MM", 0, 2, rect)
            val widthMM = rect.width()
            val letterSpacing = widthMM - 2 * widthM

            // get maximum measures of note names without octave
            var maxWidth = 0
            var maxHeight = 0
            var maxDistanceBelowBaseline = Int.MIN_VALUE
            var maxDistanceAboveBaseline = Int.MIN_VALUE

            for (n in notes) {
                val noteName = LabelSubstrings.createNoteSubstring(n, context)
                paint.getTextBounds(noteName, 0, noteName.length, rect)
                maxWidth = max(maxWidth, rect.width())
                maxHeight = max(maxHeight, rect.height())
                maxDistanceAboveBaseline = max(maxDistanceAboveBaseline, -rect.top)
                maxDistanceBelowBaseline = max(maxDistanceBelowBaseline, rect.bottom)
            }

            // get maximum measures of octave when neglecting the superscript typing
            var maxOctaveWidth = 0
            var maxOctaveDistanceAboveBaseline = Int.MIN_VALUE

            for (o in octaveBegin until octaveEnd) {
                val octaveText = LabelSubstrings.createOctaveSubstring(o)
                paint.getTextBounds(octaveText, 0, octaveText.length, rect)
                maxOctaveWidth = max(maxOctaveWidth, rect.width())
                maxOctaveDistanceAboveBaseline = max(maxOctaveDistanceAboveBaseline, -rect.top)
            }

            // convert octave measures to measures considering the superscripts
            val scale = smallSuperScriptSpan.scaleTextSize
            val offset = smallSuperScriptSpan.moveUpByPartOfAscent
            val maxOctaveWidthFloat = scale * maxOctaveWidth
            val maxOctaveDistanceAboveBaselineFloat = scale * maxOctaveDistanceAboveBaseline - offset * paint.ascent()

            return if (enableOctaveIndex) {
                LabelSetBounds(
                    maxWidth + letterSpacing + maxOctaveWidthFloat,
                    maxHeight + max(
                        0f,
                        maxOctaveDistanceAboveBaselineFloat - maxDistanceAboveBaseline
                    ),
                    max(maxDistanceAboveBaseline.toFloat(), maxOctaveDistanceAboveBaselineFloat),
                    maxDistanceBelowBaseline.toFloat()
                )
            } else {
                LabelSetBounds(
                    maxWidth.toFloat(), maxHeight.toFloat(),
                    maxDistanceAboveBaseline.toFloat(),
                    maxDistanceBelowBaseline.toFloat()
                )
            }
        }
    }
}