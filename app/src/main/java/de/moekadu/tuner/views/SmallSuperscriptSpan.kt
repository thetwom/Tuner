package de.moekadu.tuner.views

import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import kotlin.math.roundToInt

/** Span for creating superscripts which are typed with a reduce size.
 * @param scaleTextSize Scaling factor for making the superscript smaller.
 * @param moveUpByPartOfAscent Defines how much the superscript should moved up. This is measured
 *   by as parts of ascent of the font, e.g. 0.5f moves the number up by half of the ascent.
 */
class SmallSuperScriptSpan(val scaleTextSize: Float = 0.7f, val moveUpByPartOfAscent: Float = 0.4f) : MetricAffectingSpan() {
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