package de.moekadu.tuner

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.withTranslation
import androidx.core.view.ViewCompat
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatValueHolder
import kotlin.math.*

class StringView(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    : View(context, attrs, defStyleAttr)
{
    // TODO: allow manual/auto control
    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, R.attr.stringViewStyle)

    class StringInfo (val toneIndex: Int, val label: CharSequence?) {
        var layout: StaticLayout? = null
        var layoutHighlight: StaticLayout? = null
    }

    private val stringPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val stringPaintHighlight = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val labelBackgroundPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val labelBackgroundPaintHighlight = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val labelPaint = TextPaint().apply {
        color = Color.GREEN
        textSize = 12f
        isAntiAlias = true
    }

    private val labelPaintHighlight = TextPaint().apply {
        color = Color.GREEN
        textSize = 12f
        isAntiAlias = true
    }

    private val framePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private var frameColor = Color.BLACK
    private var frameColorOnTouch = Color.RED

    private var labelBackgroundPadding = 2f
    private var labelSpacing = 2f

    private var labelWidth = 0f
    private var labelWidthExpanded = 0f ///< Expanded label with so that it fills the columns
    private var labelHeight = 0f

    private val strings = ArrayList<StringInfo>()

    var activeToneIndex: Int = NO_ACTIVE_TONE_INDEX
        set(value) {
            if (value != field) {
                field = value
                updateActiveStringIndex(value)
                if (field != NO_ACTIVE_TONE_INDEX && automaticScrollToHighlight)
                    scrollToString(value, 200L)
                else
                    invalidate()
            }
        }

    private var activeStringIndex: Int = NO_ACTIVE_TONE_INDEX

    private var yOffset = 0f

    private val offsetAnimator = ValueAnimator().apply {
        addUpdateListener {
            val offset = it.animatedValue as Float
            yOffset = offset
//            Log.v("Tuner", "StringView.offsetAnimator: yOffset = $yOffset")
            updateStringPositionVariables(width, height)
            ViewCompat.postInvalidateOnAnimation(this@StringView)
        }
    }

    private val flingAnimation = FlingAnimation(FloatValueHolder()).apply {
        addUpdateListener { _, value, _ ->
            val distance = lastFlingValue - value
            lastFlingValue = value
            scrollDistance(distance)
        }
    }

    /// Temporary storage needed for fling animations
    var lastFlingValue = 0f

    /// Number of labels which can be placed next to each other
    private var numCols = 1
    /// The total horizontal space each label + equidistant spacing takes
    private var colWidth = 1f
    /// Vertical space for needed for each additional string
    private var rowHeight = 1f
    /// Current start index of string which is actually visible
    private var stringStartIndex = 0
    /// Current end index (index is included) of string which is actually visible
    private var stringEndIndex = 0

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent?): Boolean {
//            Log.v("Tuner", "PlotView: gestureListener.OnDown")
            flingAnimation.cancel()
            return true
        }
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
//            Log.v("Tuner", "PlotView: gestureListener.OnScroll x=$distanceX, y=$distanceY")
            setManualControl()
            scrollDistance(distanceY)
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            lastFlingValue = 0f
            offsetAnimator.cancel()
            flingAnimation.cancel()
            flingAnimation.setStartValue(0f)
            flingAnimation.setStartVelocity(velocityY)
            flingAnimation.start()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            if (e == null)
                return true

            val x = e.x
            val y = e.y
            val halfSizeX = 0.5f * labelWidthExpanded
            val halfSizeY = rowHeight
            var toneIndex = NO_ACTIVE_TONE_INDEX
            for (i in stringStartIndex .. stringEndIndex) {
                val xPos = getStringDrawingPositionX(i)
                val yPos = getStringDrawingPositionY(i)
                if (x >= xPos - halfSizeX && x < xPos + halfSizeX
                    && y >= yPos - halfSizeY && y < yPos + halfSizeY) {
                    toneIndex = strings[i].toneIndex
                    break
                }
            }
//            Log.v("Tuner", "StringView..onSingleTapUp: toneIndex=$toneIndex")
            stringClickedListener?.onStringClicked(toneIndex)
            if (toneIndex == NO_ACTIVE_TONE_INDEX)
                setAutomaticControl()
            performClick()
            return true
        }
    }

    private val gestureDetector = GestureDetector(context, gestureListener)

    fun interface StringClickedListener {
        fun onStringClicked(toneIndex: Int)
    }

    var stringClickedListener : StringClickedListener? = null

    private var automaticScrollToHighlight = true

    private var touchManualControlDrawable: TouchControlDrawable
    private var anchorDrawable: TouchControlDrawable
    var showAnchor = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }


    init {
        var touchDrawableId = R.drawable.ic_manual
        var touchManualControlDrawableWidth = 10f
        var touchDrawableBackgoundTint = Color.WHITE

        var anchorDrawableId = R.drawable.ic_anchor_inv
        var anchorDrawableWidth = 10f
        var anchorDrawableBackgoundTint = Color.WHITE

        attrs?.let {
            val ta = context.obtainStyledAttributes(
                attrs,
                R.styleable.StringView,
                defStyleAttr,
                R.style.StringViewStyle
            )
            labelBackgroundPadding = ta.getDimension(R.styleable.StringView_labelPadding, labelBackgroundPadding)
            labelSpacing = ta.getDimension(R.styleable.StringView_labelSpacing, labelSpacing)

            labelPaint.textSize = ta.getDimension(R.styleable.StringView_labelTextSize, labelPaint.textSize)
            labelPaint.color = ta.getColor(R.styleable.StringView_labelTextColor, labelPaint.color)

            labelPaintHighlight.textSize = labelPaint.textSize
            labelPaintHighlight.color = ta.getColor(R.styleable.StringView_labelTextColorHighlight, labelPaintHighlight.color)

            stringPaint.color = ta.getColor(R.styleable.StringView_stringColor, stringPaint.color)
            stringPaint.strokeWidth = ta.getDimension(R.styleable.StringView_stringLineWidth, stringPaint.strokeWidth)

            stringPaintHighlight.color = ta.getColor(R.styleable.StringView_stringColorHighlight, stringPaintHighlight.color)
            stringPaintHighlight.strokeWidth = ta.getDimension(R.styleable.StringView_stringLineWidthHighlight, stringPaintHighlight.strokeWidth)

            labelBackgroundPaint.color = stringPaint.color
            labelBackgroundPaintHighlight.color = stringPaintHighlight.color

            frameColor = ta.getColor(R.styleable.StringView_frameColor, frameColor)
            frameColorOnTouch = ta.getColor(R.styleable.StringView_frameColorOnTouch, frameColorOnTouch)
            framePaint.strokeWidth = ta.getDimension(R.styleable.StringView_frameStrokeWidth, framePaint.strokeWidth)
            framePaint.color = frameColor

            touchDrawableId = ta.getResourceId(R.styleable.StringView_touchDrawable, touchDrawableId)
            touchManualControlDrawableWidth = ta.getDimension(R.styleable.StringView_touchDrawableWidth, touchManualControlDrawableWidth)
            touchDrawableBackgoundTint = ta.getColor(R.styleable.StringView_touchDrawableBackgroundTint, touchDrawableBackgoundTint)

            anchorDrawableId = ta.getResourceId(R.styleable.StringView_anchorDrawable, anchorDrawableId)
            anchorDrawableWidth = ta.getDimension(R.styleable.StringView_anchorDrawableWidth, anchorDrawableWidth)
            anchorDrawableBackgoundTint = ta.getColor(R.styleable.StringView_anchorDrawableBackgroundTint, anchorDrawableBackgoundTint)
            ta.recycle()
        }

        touchManualControlDrawable = TouchControlDrawable(context, frameColorOnTouch, touchDrawableBackgoundTint, touchDrawableId)
        touchManualControlDrawable.setSize(width = touchManualControlDrawableWidth)

        anchorDrawable = TouchControlDrawable(context, stringPaintHighlight.color, anchorDrawableBackgoundTint, anchorDrawableId)
        anchorDrawable.setSize(width = anchorDrawableWidth)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var proposedWidth = max(MeasureSpec.getSize(widthMeasureSpec), suggestedMinimumWidth)
        if (widthMode == MeasureSpec.UNSPECIFIED)
            proposedWidth = max(6 * ((labelWidth + labelSpacing) + labelSpacing + paddingLeft + paddingRight).toInt(), suggestedMinimumWidth)
        val w = resolveSize(proposedWidth, widthMeasureSpec)

        val rowHeight = 0.5f * (labelHeight + stringPaint.strokeWidth) + labelSpacing
        val desiredHeight = max((paddingTop + labelHeight + (strings.size - 1) * rowHeight + paddingBottom).roundToInt() + 1, suggestedMinimumHeight)
        val h = resolveSize(desiredHeight, heightMeasureSpec)
//        Log.v("Tuner", "StringView.onMeasure: width=${MeasureSpec.toString(widthMeasureSpec)}, height=${MeasureSpec.toString(heightMeasureSpec)}, desiredHeight=$desiredHeight, resolvedHeight=$h")
        setMeasuredDimension(w, h)
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val g = gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event) || g
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        updateStringPositionVariables(w, h)
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas?) {
//        Log.v("Tuner", "StringView.onDraw: yOffset = $yOffset")
        if (canvas == null)
            return
        canvas.drawRect(paddingLeft.toFloat(), paddingTop.toFloat(),
            width - paddingRight.toFloat(), height - paddingBottom.toFloat(), framePaint)
        //canvas.save()
        //canvas.clipRect(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom)
        canvas.clipRect(0, paddingTop, width, height - paddingBottom)
        var anchorYPos = NO_ANCHOR

        for (i in stringStartIndex .. stringEndIndex) {
            val xPos = getStringDrawingPositionX(i)
            val yPos = getStringDrawingPositionY(i)
            drawString(xPos, yPos, strings[i], strings[i].toneIndex == activeToneIndex, canvas)

            if (strings[i].toneIndex == activeToneIndex)
                anchorYPos = yPos
        }

        if (!automaticScrollToHighlight) {
            touchManualControlDrawable.drawToCanvas(
                width - paddingRight.toFloat() - 0.5f * framePaint.strokeWidth + 1,
                paddingTop.toFloat() + 0.5f * framePaint.strokeWidth - 1,
                MarkAnchor.NorthEast,
                canvas
            )
        }

        //canvas.restore()
        if (showAnchor) {
            val minYPos = paddingTop + 0.5f * anchorDrawable.height - 0.5f * framePaint.strokeWidth
            val maxYPos = height - paddingBottom - 0.5f * anchorDrawable.height + 0.5f * framePaint.strokeWidth

            if (anchorYPos == NO_ANCHOR){
                anchorYPos = if (activeStringIndex < stringStartIndex)
                    minYPos
                else
                    maxYPos
            }
            anchorYPos = min(anchorYPos, maxYPos)
            anchorYPos = max(anchorYPos, minYPos)
            anchorDrawable.drawToCanvas(
                width - paddingRight.toFloat() - 0.5f * framePaint.strokeWidth,
                anchorYPos,
                MarkAnchor.West, canvas
            )
        }
    }

    fun setStrings(toneIndices: IntArray, labels: (Int) -> CharSequence?) {
        strings.clear()
        var labelWidth = 0
        var labelHeight = 0

        for (toneIndex in toneIndices) {
            strings.add(StringInfo(toneIndex, labels(toneIndex)))
            strings.last().layout = buildLabelLayout(strings.last().label, highlight = false)
            strings.last().layoutHighlight = buildLabelLayout(strings.last().label, highlight = true)

            labelWidth = max(labelWidth, strings.last().layout?.width ?: 0)
            labelHeight = max(labelHeight, strings.last().layout?.height ?: 0)
        }
        this.labelWidth = labelWidth.toFloat() + 2 * labelBackgroundPadding
        this.labelHeight = labelHeight.toFloat() + 2 * labelBackgroundPadding

        if (!(activeToneIndex in toneIndices))
            activeToneIndex = NO_ACTIVE_TONE_INDEX
        updateActiveStringIndex(activeToneIndex)

        requestLayout()
        invalidate()
        post {
            yOffset = 0f
            updateStringPositionVariables(width, height)
            setAutomaticControl(0L)
//            Log.v("Tuner", "StringView.setStrings (post): yOffset = $yOffset")
        }
        // TODO: in theory we should call updateStringPositionVariables( .... )
        //       but maybe we should also adapt the yOffset

    }

    fun scrollToString(toneIndex: Int, animationDuration: Long) {
        val stringIndex = strings.indexOfFirst { it.toneIndex == toneIndex }
        if (stringIndex >= 0) {
            flingAnimation.cancel()

            val yPosOfString = paddingTop + 0.5f * height
            // yPos = yOffset + paddingTop + 0.5f * labelHeight + stringIndex * rowHeight
            var yOffsetTarget = yPosOfString - (paddingTop + 0.5f * labelHeight + stringIndex * rowHeight)
            yOffsetTarget = min(yOffsetTarget, computeOffsetMax())
            yOffsetTarget = max(yOffsetTarget, computeOffsetMin())
//            Log.v("Tuner", "StringView.scrollToString yOffsetMin = ${computeOffsetMin()}, yOffsetMax=${computeOffsetMax()}, yOffsetTarget=$yOffsetTarget")
            offsetAnimator.cancel()

            if (animationDuration == 0L) {
                yOffset = yOffsetTarget
                updateStringPositionVariables(width, height)
//                Log.v("Tuner", "StringView.scrollToString (animDur=0): yOffset = $yOffset")
                invalidate()
            } else {
//                Log.v("Tuner", "StringView.scrollToString (animDur>0): yOffset = $yOffset")
                offsetAnimator.duration = animationDuration
                offsetAnimator.setFloatValues(yOffset, yOffsetTarget)
                offsetAnimator.start()
            }
        }
    }

    fun setManualControl() {
        // no manual control if no scrolling is possible
        if (computeOffsetMax() == computeOffsetMin())
            return
        automaticScrollToHighlight = false
        framePaint.color = frameColorOnTouch
        invalidate()
    }

    fun setAutomaticControl(animationDuration: Long = 200L) {
        automaticScrollToHighlight = true
        framePaint.color = frameColor
        if (activeToneIndex != NO_ACTIVE_TONE_INDEX)
            scrollToString(activeToneIndex, animationDuration)
        invalidate()
    }

    private fun updateActiveStringIndex(toneIndex: Int) {
        activeStringIndex = if (toneIndex == NO_ACTIVE_TONE_INDEX)
            -1
        else
            strings.indexOfFirst { it.toneIndex == toneIndex }
    }

    private fun scrollDistance(distance: Float) {
        yOffset -= distance
//            Log.v("Tuner", "StringView.scrollDistance: distance: offsetY=$yOffset, distance=$distance, min=${computeOffsetMin()}, max=${computeOffsetMax()}")
        yOffset = min(yOffset, computeOffsetMax())
        yOffset = max(yOffset, computeOffsetMin())
        updateStringPositionVariables(width, height)
        ViewCompat.postInvalidateOnAnimation(this@StringView)
    }

    private fun drawString(xPos: Float, yPos: Float, stringInfo: StringInfo, highlight: Boolean, canvas: Canvas) {
        canvas.drawLine(paddingLeft.toFloat(), yPos, width.toFloat() - paddingRight, yPos, if (highlight) stringPaintHighlight else stringPaint)
        canvas.drawRect(xPos - 0.5f * labelWidthExpanded, yPos - 0.5f * labelHeight, xPos + 0.5f * labelWidthExpanded,
            yPos + 0.5f * labelHeight, if (highlight) labelBackgroundPaintHighlight else labelBackgroundPaint)

        (if (highlight) stringInfo.layoutHighlight else stringInfo.layout)?.let { layout ->
            canvas.withTranslation(
                xPos - 0.5f * layout.width,yPos - 0.5f * layout.height
            ) {
                layout.draw(this)
            }
        }
    }

    private fun buildLabelLayout(label: CharSequence?, highlight: Boolean): StaticLayout? {
        return if (label != null) {
            val desiredWidth = ceil(StaticLayout.getDesiredWidth(label, labelPaint)).toInt()
            val builder = StaticLayout.Builder.obtain(label,0, label.length,
                if (highlight) labelPaintHighlight else labelPaint, desiredWidth)
            builder.build()
        }
        else {
            null
        }
    }

    private fun getStringDrawingPositionX(stringIndex: Int): Float {
        val col = stringIndex % numCols
        return paddingLeft + 0.5f * labelSpacing + (0.5f + col) * colWidth
    }

    private fun getStringDrawingPositionY(stringIndex: Int): Float {
        return yOffset + paddingTop + 0.5f * labelHeight + stringIndex * rowHeight
    }

    private fun updateStringPositionVariables(w: Int, h: Int) {
        val effectiveWidth = w - paddingLeft - paddingRight
        numCols = (floor((effectiveWidth - labelSpacing) / (labelWidth + labelSpacing))).toInt()
        numCols = max(1, min(numCols, strings.size))
        labelWidthExpanded = (effectiveWidth - labelSpacing) / numCols - labelSpacing
        colWidth = (effectiveWidth - labelSpacing) / numCols
        rowHeight = 0.5f * (labelHeight + stringPaint.strokeWidth) + labelSpacing

        // visible if
        // yOffset + paddingTop + 0.5f * labelHeight + i * rowHeight + 0.5f * labelHeight > paddingTop
        // i * rowHeight > -yOffset - labelHeight
        // i > - (yOffset + labelHeight) / rowHeight
        //
        // yOffset + paddingTop + 0.5f * labelHeight + i * rowHeight - 0.5f * labelHeight < height - paddingBottom
        // i * rowHeight < height - paddingBottom - paddingTop - yOffset
        // i > (height - paddingBottom - paddingTop - yOffset) / rowHeight
        stringStartIndex = max(0, floor(-(yOffset + labelHeight) / rowHeight).toInt())
        stringEndIndex = min(strings.size-1, ceil((h - paddingBottom - paddingTop - yOffset) / rowHeight).toInt())
    }

    private fun computeOffsetMin(): Float {
        val effectiveHeight = height - paddingTop - paddingBottom
        val rowHeight = 0.5f * (labelHeight + stringPaint.strokeWidth) + labelSpacing
        val stringTotalHeight = strings.size * rowHeight + 0.5f * labelHeight - labelSpacing // check
//        Log.v("Tuner", "StringView.computeOffsetMax: stringTotalHeight=$stringTotalHeight, effectiveHeight=$effectiveHeight")
        return min(0f, effectiveHeight - stringTotalHeight)
    }

    private fun computeOffsetMax() = 0f


    companion object {
        const val NO_ACTIVE_TONE_INDEX = Int.MAX_VALUE
        const val NO_ANCHOR = Float.MAX_VALUE
    }
}
