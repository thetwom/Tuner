package de.moekadu.tuner.views

import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import kotlin.math.roundToInt

/** Span for creating subscripts which are typed with a reduce size.
 * @param scaleTextSize Scaling factor for making the subscript smaller.
 * @param moveUpByPartOfAscent Defines how much the subscript should moved up. This is measured
 *   by as parts of ascent of the font, e.g. 0.5f moves the number up by half of the ascent.
 */
class SmallSubScriptSpan(val scaleTextSize: Float = 0.7f, val moveDownByPartOfAscent: Float = 0.2f) : MetricAffectingSpan() {
    override fun updateDrawState(p0: TextPaint?) {
        p0?.let { textPaint ->
            textPaint.baselineShift -= (moveDownByPartOfAscent * textPaint.ascent()).roundToInt()
            textPaint.textSize = scaleTextSize * textPaint.textSize
        }
    }

    override fun updateMeasureState(p0: TextPaint) {
        p0.baselineShift -= (moveDownByPartOfAscent * p0.ascent()).roundToInt()
        p0.textSize = scaleTextSize * p0.textSize
    }
}