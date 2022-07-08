package de.moekadu.tuner.views

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withTranslation

class StringLabel(string: String, paint: TextPaint,
                  backgroundPaint: Paint? = null, cornerRadius: Float = 0f,
                  gravity: LabelGravity = LabelGravity.Center,
                  paddingLeft: Float = 0f, paddingRight: Float = 0f,
                  paddingTop: Float = 0f, paddingBottom: Float = 0f)
    : Label(backgroundPaint, cornerRadius, gravity, paddingLeft, paddingRight, paddingTop, paddingBottom) {
    private val bounds = Rect().apply {
        paint.getTextBounds(string, 0, string.length, this)
    }

    /** Desired width of layout, but add +1 in order to avoid line breaks. */
    private val desiredWidth = StaticLayout.getDesiredWidth(string, paint).toInt() + 1
    private val layout = StaticLayout.Builder.obtain(string, 0, string.length, paint, desiredWidth).build()
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

    companion object {
        fun getBounds(string: String, paint: TextPaint, rect: Rect) {
            paint.getTextBounds(string, 0, string.length, rect)
        }
    }
}