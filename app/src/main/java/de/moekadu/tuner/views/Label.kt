package de.moekadu.tuner.views

import android.graphics.Canvas
import android.graphics.Paint

enum class LabelAnchor {
    Center,
    North,
    South,
    West,
    East,
    NorthWest,
    NorthEast,
    SouthWest,
    SouthEast,
    Baseline,
    BaselineWest,
    BaselineEast
}

enum class LabelGravity {
    Center,
    Top,
    Bottom,
    Baseline,
    Left,
    Right,
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
    BaselineLeft,
    BaselineRight
}

/** Abstract label class to plot labels with backgrounds.
 * @param backgroundPaint Paint which should be used to draw the background.
 * @param cornerRadius Corner radius of the rectangle, 0f for no corner radius.
 * @param gravity Gravity of where to place the label, when the drawToCanvasWithFixedSizeBackground
 *   function is used. E.g. the label can be centered within the background, moved to the left,
 *   top, ... . Note that alignment will be note based on the outer bound of the rectangle, but on
 *   the inner region, defined by the padding.
 * @param paddingLeft Padding between left side of rectangle and left side of label.
 * @param paddingRight Padding between right side of rectangle and right side of label.
 * @param paddingTop Padding between upper side of rectangle and upper side of label.
 * @param paddingBottom Padding between lower side of rectangle and lower side of label.
 * */
abstract class Label(
    private val backgroundPaint: Paint? = null, private val cornerRadius: Float = 0f,
    private val gravity: LabelGravity = LabelGravity.Center,
    private val paddingLeft: Float = 0f, private val paddingRight: Float = 0f,
    private val paddingTop: Float = 0f, private val paddingBottom: Float = 0f
) {
    /** Outer bound for a set of labels.
     * @param maxWidth Maximum label width of set of labels.
     * @param maxHeight Maximum label height of a set of labels.
     * @param maxDistanceAboveBaseline Maximum distance between baseline and top of label.
     * @param maxDistanceBelowBaseline Maximum distance between baseline and bottom of label.
     * @param verticalCenterAboveBaseline Vertical center above baseline, which is used for centered
     *   vertical alignment.
     */
    data class LabelSetBounds(val maxWidth: Float, val maxHeight: Float,
                              val maxDistanceAboveBaseline: Float, val maxDistanceBelowBaseline: Float,
                              val verticalCenterAboveBaseline: Float)

    /** Width of label. */
    abstract val labelWidth: Float
    /** Height of label. */
    abstract val labelHeight: Float
    /** Baseline of label as distance between baseline and top. */
    abstract val labelBaselineBelowTop: Float

    /** Distance between baseline and bottom. */
    abstract val labelBottomBelowBaseline: Float

    /** Vertical center above baseline, this is used for centered vertical alignment. */
    abstract val verticalCenterAboveBaseline: Float

    /** Draw label.
     * @param positionX x-position of where to draw.
     * @param positionY y-position of where to draw.
     * @param anchor Anchor, which defines how to use the x,y-position. The label will
     *   then positioned such, that the given anchor matches with the x,y-position.
     * @param canvas Canvas on which we draw.
     */
    abstract fun drawToCanvas(positionX: Float, positionY: Float, anchor: LabelAnchor, canvas: Canvas?)

    /** Draw a fixed size background rectangle and the above draw a label.
     * @param positionX x-position on canvas where to draw
     * @param positionY y-position on canvas where to draw
     * @param backgroundWidth Width of background rectangle.
     * @param backgroundHeight Height of background rectangle.
     * @param baselineBelowTop If anchor is Baseline, BaselineWest, BaselineEast, this value will
     *   be used to define the baseline as distance between top of background and the baseline.
     * @param anchor Anchor, which defines how to use the x,y-position. The background will
     *   then positioned such, that the given anchor matches with the x,y-position.
     * @param canvas Canvas on which we draw.
     */
    fun drawToCanvasWithFixedSizeBackground(
        positionX: Float, positionY: Float,
        backgroundWidth: Float, backgroundHeight: Float, baselineBelowTop: Float = 0f,
        anchor: LabelAnchor, canvas: Canvas?) {
        if (canvas == null)
            return
        val bgLeft = when (anchor) {
            LabelAnchor.North, LabelAnchor.South, LabelAnchor.Center, LabelAnchor.Baseline -> positionX - 0.5f * backgroundWidth
            LabelAnchor.NorthEast, LabelAnchor.SouthEast, LabelAnchor.East, LabelAnchor.BaselineEast -> positionX - backgroundWidth
            LabelAnchor.NorthWest, LabelAnchor.SouthWest, LabelAnchor.West, LabelAnchor.BaselineWest -> positionX
        }
        val bgTop = when (anchor) {
            LabelAnchor.North, LabelAnchor.NorthEast, LabelAnchor.NorthWest -> positionY
            LabelAnchor.South, LabelAnchor.SouthEast, LabelAnchor.SouthWest -> positionY - backgroundHeight
            LabelAnchor.Center, LabelAnchor.East, LabelAnchor.West -> positionY - 0.5f * backgroundHeight
            LabelAnchor.Baseline, LabelAnchor.BaselineEast, LabelAnchor.BaselineWest -> positionY - baselineBelowTop
        }
        val bgRight = bgLeft + backgroundWidth
        val bgBottom = bgTop + backgroundHeight

        if (backgroundPaint != null) {
            if (cornerRadius == 0f)
                canvas.drawRect(bgLeft, bgTop, bgRight, bgBottom, backgroundPaint)
            else
                canvas.drawRoundRect(bgLeft, bgTop, bgRight, bgBottom, cornerRadius, cornerRadius, backgroundPaint)
        }

        val labelX = when (gravity) {
            LabelGravity.Top, LabelGravity.Bottom, LabelGravity.Center, LabelGravity.Baseline -> 0.5f * (bgLeft + paddingLeft + bgRight - paddingRight)
            LabelGravity.TopLeft, LabelGravity.BottomLeft, LabelGravity.Left, LabelGravity.BaselineLeft -> bgLeft + paddingLeft
            LabelGravity.TopRight, LabelGravity.BottomRight, LabelGravity.Right, LabelGravity.BaselineRight -> bgRight - paddingRight
        }
        val labelY = when (gravity) {
            LabelGravity.Top, LabelGravity.TopLeft, LabelGravity.TopRight -> bgTop + paddingTop
            LabelGravity.Bottom, LabelGravity.BottomLeft, LabelGravity.BottomRight -> bgBottom - paddingBottom
            LabelGravity.Center, LabelGravity.Left, LabelGravity.Right -> 0.5f * (bgTop + paddingTop + bgBottom - paddingBottom)
            LabelGravity.Baseline, LabelGravity.BaselineLeft, LabelGravity.BaselineRight -> bgTop + baselineBelowTop
        }
        val labelAnchor = when (gravity) {
            LabelGravity.Center -> LabelAnchor.Center
            LabelGravity.Top -> LabelAnchor.North
            LabelGravity.Bottom -> LabelAnchor.South
            LabelGravity.Left -> LabelAnchor.West
            LabelGravity.Right -> LabelAnchor.East
            LabelGravity.TopLeft -> LabelAnchor.NorthWest
            LabelGravity.TopRight -> LabelAnchor.NorthEast
            LabelGravity.BottomLeft -> LabelAnchor.SouthWest
            LabelGravity.BottomRight -> LabelAnchor.SouthEast
            LabelGravity.Baseline -> LabelAnchor.Baseline
            LabelGravity.BaselineLeft -> LabelAnchor.BaselineWest
            LabelGravity.BaselineRight -> LabelAnchor.BaselineEast
        }
        drawToCanvas(labelX, labelY, labelAnchor, canvas)
    }

    /** Draw a background rectangle and the above draw a label.
     * @param positionX x-Position on canvas where to draw
     * @param positionY y-Position on canvas where to draw
     * @param anchor Anchor, which defines how to use the x,y-position. The background will
     *   then positioned such, that the given anchor matches with the x,y-position.
     * @param canvas Canvas on which we draw.
     */
    fun drawToCanvasWithPaddedBackground(
        positionX: Float, positionY: Float, anchor: LabelAnchor, canvas: Canvas?) {

        if (canvas == null)
            return

        val backgroundWidth = labelWidth + paddingLeft + paddingRight
        val backgroundHeight = labelHeight + paddingTop + paddingBottom
        val backgroundBaselineBelowTop = paddingTop + labelBaselineBelowTop

        drawToCanvasWithFixedSizeBackground(positionX, positionY, backgroundWidth,
            backgroundHeight, backgroundBaselineBelowTop, anchor, canvas)
    }
}