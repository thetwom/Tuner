package de.moekadu.tuner.views

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withTranslation
import de.moekadu.tuner.temperaments.*
import kotlin.math.max

/** Class for creating note labels, where the octave is typed slightly smaller and as superscript. */
class MusicalNoteLabel(val note: MusicalNote, paint: TextPaint,
                       noteNamePrinter: NoteNamePrinter,
                       backgroundPaint: Paint? = null, cornerRadius: Float = 0f,
                       gravity: LabelGravity = LabelGravity.Center,
                       printOptions: MusicalNotePrintOptions = MusicalNotePrintOptions.None,
                       val enableOctaveIndex: Boolean = true,
                       paddingLeft: Float = 0f, paddingRight: Float = 0f,
                       paddingTop: Float = 0f, paddingBottom: Float = 0f)
    : Label(backgroundPaint, cornerRadius, gravity, paddingLeft, paddingRight, paddingTop, paddingBottom) {

    private val capitalLetterBounds = Rect().apply {
        paint.getTextBounds("M", 0, 1, this)
    }

    private val bounds = noteNamePrinter.measure(paint, note, printOptions, enableOctaveIndex)
    private val spannableString = noteNamePrinter.noteToCharSequence(note, printOptions, enableOctaveIndex)
    // make sure that the width is wide enough to not have line breaks
    // theoretically, bounds.width() would be enough, but it might be not fully exact ...
    private val desiredWidth = (2 * bounds.width()).toInt() + 10
    private val layout = StaticLayout.Builder.obtain(spannableString, 0, spannableString.length, paint, desiredWidth).build()
    private val baselineOfLayout = layout.getLineBaseline(0)

    override val labelWidth: Float
        get() = bounds.width()
    override val labelHeight: Float
        get() = 2 * max(labelBottomBelowBaseline + verticalCenterAboveBaseline, labelBaselineBelowTop - verticalCenterAboveBaseline)
        //get() = bounds.height()
    override val labelBaselineBelowTop: Float
        get() = -bounds.top
    override val labelBottomBelowBaseline: Float
        get() = bounds.bottom
    override val verticalCenterAboveBaseline: Float
        get() = -capitalLetterBounds.exactCenterY()

    override fun drawToCanvas(positionX: Float, positionY: Float, anchor: LabelAnchor, canvas: Canvas?) {
        if (canvas == null)
            return

        val x = when (anchor) {
            LabelAnchor.North, LabelAnchor.South, LabelAnchor.Center, LabelAnchor.Baseline -> positionX - bounds.centerX()
            LabelAnchor.NorthEast, LabelAnchor.SouthEast, LabelAnchor.East, LabelAnchor.BaselineEast -> positionX - bounds.right
            LabelAnchor.NorthWest, LabelAnchor.SouthWest, LabelAnchor.West, LabelAnchor.BaselineWest -> positionX - bounds.left
        }
        val y = when (anchor) {
                LabelAnchor.North, LabelAnchor.NorthEast, LabelAnchor.NorthWest -> positionY - baselineOfLayout - bounds.top
                LabelAnchor.South, LabelAnchor.SouthEast, LabelAnchor.SouthWest -> positionY - baselineOfLayout - bounds.bottom
                LabelAnchor.Center, LabelAnchor.East, LabelAnchor.West -> positionY - baselineOfLayout + verticalCenterAboveBaseline//- bounds.centerY()
                LabelAnchor.Baseline, LabelAnchor.BaselineEast, LabelAnchor.BaselineWest -> positionY - baselineOfLayout
            }

        canvas.withTranslation(x, y) {
            layout.draw(this)
        }
    }

    companion object {
        /** Get outer bounds of several labels to be printed.
         * This can be used to compute an common rectangle size for many notes, such that
         * all notes will fit into this rectangle.
         * @param notes Notes, which should fit in the resulting bounds, octave is ignored.
         * @param octaveBegin Smallest octave to be considered.
         * @param octaveEnd End octave to be considered (excluded).
         * @param paint Paint which will be used for drawing the notes.
         * @param context Context required for accessing string resources.
         * @param printOptions Options for printing the note (prefer flat/sharp, ...)
         * @param enableOctaveIndex Set this to false if you don't want to print the octave index.
         * @return Bounds of the rectangle which fits around each single note.
         */
        fun getLabelSetBounds(notes: Array<MusicalNote>, octaveBegin: Int, octaveEnd: Int, paint: TextPaint, noteNamePrinter: NoteNamePrinter,
                              printOptions: MusicalNotePrintOptions = MusicalNotePrintOptions.None, enableOctaveIndex: Boolean = true): LabelSetBounds {
            val capitalLetterBounds = Rect().apply {
                paint.getTextBounds("M", 0, 1, this)
            }

            // get maximum measures of note names without octave
            var maxWidth = 0f
            var maxHeight = 0f
            var maxDistanceBelowBaseline = Float.NEGATIVE_INFINITY
            var maxDistanceAboveBaseline = Float.NEGATIVE_INFINITY

            for (n in notes) {
                val noteBounds = noteNamePrinter.measure(paint, n, printOptions, withOctave = false)

                maxWidth = max(maxWidth, noteBounds.width())
                maxHeight = max(maxHeight, noteBounds.height())
                maxDistanceAboveBaseline = max(maxDistanceAboveBaseline, -noteBounds.top)
                maxDistanceBelowBaseline = max(maxDistanceBelowBaseline, noteBounds.bottom)
            }

            return if (enableOctaveIndex) {
                // get maximum measures of octave when neglecting the superscript typing
                var maxOctaveWidth = 0f
                var maxOctaveDistanceAboveBaseline = Float.NEGATIVE_INFINITY

                for (o in octaveBegin until octaveEnd) {
                    val octaveBounds = noteNamePrinter.measureOctaveIndex(paint, o)
                    maxOctaveWidth = max(maxOctaveWidth, octaveBounds.width())
                    maxOctaveDistanceAboveBaseline = max(maxOctaveDistanceAboveBaseline, -octaveBounds.top)
                }

                val spaceWidth = noteNamePrinter.measureOctaveIndexLeadingSpace(paint)

                LabelSetBounds(
                    maxWidth + maxOctaveWidth + spaceWidth,
                    maxHeight + max(
                        0f,
                        maxOctaveDistanceAboveBaseline - maxDistanceAboveBaseline
                    ),
                    max(maxDistanceAboveBaseline, maxOctaveDistanceAboveBaseline),
                    maxDistanceBelowBaseline,
                    -capitalLetterBounds.exactCenterY()
                )
            } else {
                LabelSetBounds(
                    maxWidth, maxHeight,
                    maxDistanceAboveBaseline,
                    maxDistanceBelowBaseline,
                    -capitalLetterBounds.exactCenterY()
                )
            }
        }
    }
}
