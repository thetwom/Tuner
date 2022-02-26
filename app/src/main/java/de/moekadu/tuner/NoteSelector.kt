package de.moekadu.tuner

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
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

class NoteSelector(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    : View(context, attrs, defStyleAttr) {
    companion object {
        private const val NUM_STYLES = 2
    }

    fun interface ToneChangedListener {
        fun onToneChanged(newTone: Int)
    }

    var toneChangedListener: ToneChangedListener? = null

    class NoteLabel(val toneIndex: Int, private val label: CharSequence, labelPaint: Array<TextPaint>) {
        val layouts = Array<StaticLayout?>(NUM_STYLES) {null}
        var maxLabelWidth = 0
            private set
        private val textSizes = Array(NUM_STYLES) {0f}

        init {
            maxLabelWidth = 0
            for (i in 0 until NUM_STYLES) {
                textSizes[i] = labelPaint[i].textSize
                layouts[i] = buildLabelLayout(labelPaint[i])
//                Log.v("Tuner", "NoteSelector.NoteLabel.init: label=$label, i=$i, layouts[i]=${layouts[i]}, labelPaint[i].textSize=${labelPaint[i].textSize}, w=${layouts[i]?.width}, h=${layouts[i]?.height}, a=${layouts[i]?.alignment}, bP=${layouts[i]?.bottomPadding}")
                maxLabelWidth = max(maxLabelWidth, layouts[i]?.width ?: 0)
            }
        }

        fun updateSize(labelPaint: Array<TextPaint>): Boolean {
            var changed = false
//            Log.v("Tuner", "NoteSelector.NoteLabel.updateSize: textSizes[0]=${textSizes[0]}, labelPaint[0].textSize=${labelPaint[0].textSize}")
            for (i in 0 until NUM_STYLES) {
                if (labelPaint[i].textSize != textSizes[i]) {
                    textSizes[i] = labelPaint[i].textSize
                    layouts[i] = buildLabelLayout(labelPaint[i])
//                    Log.v("Tuner", "NoteSelector.NoteLabel.updateSize: label=$label, i=$i, layouts[i]=${layouts[i]}, labelPaint[i].textSize=${labelPaint[i].textSize}, w=${layouts[i]?.width}, h=${layouts[i]?.height}, a=${layouts[i]?.alignment}, bP=${layouts[i]?.bottomPadding}")
                    changed = true
                }
            }
            if (changed)
                maxLabelWidth = layouts.maxOf { it?.width ?: 0 }
            return changed
        }

        private fun buildLabelLayout(labelPaint: TextPaint): StaticLayout {
            val desiredWidth = ceil(StaticLayout.getDesiredWidth(label, labelPaint)).toInt()
            val builder =
                StaticLayout.Builder.obtain(label, 0, label.length, labelPaint, desiredWidth)
            return builder.build()
        }
    }

    private val offsetAnimator = ValueAnimator().apply {
        addUpdateListener {
            val offset = it.animatedValue as Float
            horizontalScrollPosition = offset
//            Log.v("Tuner", "NoteSelector.offsetAnimator: yOffset = $yOffset")
            ViewCompat.postInvalidateOnAnimation(this@NoteSelector)
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

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent?): Boolean {
//            Log.v("Tuner", "NoteSelector: gestureListener.OnDown")
            flingAnimation.cancel()
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
//            Log.v("Tuner", "NoteSelector: gestureListener.OnScroll x=$distanceX, y=$distanceY")
            scrollDistance(distanceX)
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            lastFlingValue = 0f
            offsetAnimator.cancel()
            flingAnimation.cancel()
            flingAnimation.setStartValue(0f)
            flingAnimation.setStartVelocity(velocityX)
            flingAnimation.addEndListener { _, canceled, _, _ ->
                if (!canceled)
                    scrollToActiveNote(150L)
            }
            flingAnimation.start()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
//            if (e == null || stringClickedListener == null)
            if (e == null)
                return true

            val x = e.x
            val noteIndex = noteIndexFromX(x)
            if (noteIndex < 0)
                return false

            if (noteIndex != activeNoteArrayIndex) {
                activeNoteArrayIndex = noteIndex
                toneChangedListener?.onToneChanged(noteLabels[noteIndex].toneIndex)
                scrollToActiveNote(150L)
//                Log.v("Tuner", "NoteSelector.onSingleTapUp: performingClick")
                if (isSoundEffectsEnabled)
                    playSoundEffect(android.view.SoundEffectConstants.CLICK)
                performClick()
            }
            return true
        }
    }

    private val gestureDetector = GestureDetector(context, gestureListener)

    private val noteLabels = ArrayList<NoteLabel>()
    private val labelPaint = Array(NUM_STYLES) {
        TextPaint().apply {
            isAntiAlias = true
        }
    }
    private val fontMetrics = Array(NUM_STYLES) {Paint.FontMetrics()}

    private var maxLabelWidth = 0

    private var textPadding = 0f
    private val windowPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private var rectangleRadius = 0f

    private val widthFactor = 1.2f

    private var activeNoteArrayIndex = 0
    val activeToneIndex: Int
        get() {
            return if (activeNoteArrayIndex in noteLabels.indices)
                noteLabels[activeNoteArrayIndex].toneIndex
            else
                Int.MAX_VALUE
        }
    private var horizontalScrollPosition = 0f

    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, R.attr.noteSelectorStyle)

    init {
        val typefaceValues = Array(NUM_STYLES) {0}
        attrs?.let {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.NoteSelector, defStyleAttr, R.style.NoteSelectorStyle)

            labelPaint[0].color = ta.getColor(R.styleable.NoteSelector_labelTextColor, Color.BLACK)
            labelPaint[1].color = ta.getColor(R.styleable.NoteSelector_labelTextColor2, Color.GREEN)

            typefaceValues[0] = ta.getInt(R.styleable.NoteSelector_textStyle, 0)
            typefaceValues[1] = ta.getInt(R.styleable.NoteSelector_textStyle2, 0)

            windowPaint.color = ta.getColor(R.styleable.NoteSelector_windowColor, Color.RED)
            windowPaint.strokeWidth = ta.getDimension(R.styleable.NoteSelector_windowStrokeWidth, 3f)

            textPadding = ta.getDimension(R.styleable.NoteSelector_textPadding, 4f)

            rectangleRadius = ta.getDimension(R.styleable.NoteSelector_rectangleRadius, rectangleRadius)

            ta.recycle()
        }
        for (i in 0 until NUM_STYLES) {
            when (typefaceValues[i]) {
                0 -> labelPaint[i].typeface = Typeface.DEFAULT
                1 -> labelPaint[i].typeface = Typeface.DEFAULT_BOLD
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val g = gestureDetector.onTouchEvent(event)
//        Log.v("Tuner", "NoteSelector.onTouchEvent: $g")
        val u = if (!g && event?.actionMasked == MotionEvent.ACTION_UP) {
//            Log.v("Tuner", "NoteSelector.onTouchEvent: action up")
            scrollToActiveNote(150L)
            //performClick()
            true
        } else {
            false
        }
        return super.onTouchEvent(event) || g || u
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val s = computeLabelTextSize(h)
        setLabelPaintSizeAndFontMetrics(s)
//        Log.v("Tuner", "NoteSelector.onSizeChanged: s=$s, textSize=${labelPaint[0].textSize}")
//        Log.v("Tuner", "NoteSelector.onSizeChanged: s=$s, ${labelPaint[0].textSize}, ${labelPaint[0].descent() - labelPaint[0].ascent()}")
//        Log.v("Tuner", "NoteSelector.onSizeChanged: after: numLabels=${noteLabels.size}, s=$s, textSize=${labelPaint[0].textSize}")
        var changed = false
        noteLabels.forEach {
            changed = it.updateSize(labelPaint) || changed
        }
        if (changed) {
            maxLabelWidth = noteLabels.maxOf { it.maxLabelWidth }
            scrollToActiveNote(0L)
        }

        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null)
            return

        val rectangleCenter = getRectangleCenter()
        canvas.clipRect(paddingLeft, 0, width - paddingRight, height)

        val textTop = paddingTop + windowPaint.strokeWidth + textPadding
        val textBottom = height - (paddingBottom + windowPaint.strokeWidth + textPadding)
        val textBaseline = textTop - fontMetrics[0].ascent / (fontMetrics[0].descent - fontMetrics[0].ascent) * (textBottom - textTop)
//        Log.v("Tuner", "NoteSelector.onDraw: textTop=$textTop, textBottom=$textBottom, textBaseline=$textBaseline, fm.ascent=${fontMetrics[0].ascent}, fm.descent=${fontMetrics[0].descent}")

        // find smallest i which must be drawn
        // notePositionRight < rectangleCenter + i * (widthFactor * maxLabelWidth + textPadding) + 0.5f * maxLabelWidth + horizontalScrollPosition
        // => i > -(rectangleCenter + 0.5f * maxLabelWidth - notePositionRight + horizontalScrollPosition) / (widthFactor * maxLabelWidth + textPadding)
        // with notePositionRight = paddingLeft
        // => i > -(rectangleCenter + 0.5f * maxLabelWidth - paddingLeft + horizontalScrollPosition) / (widthFactor * maxLabelWidth + textPadding)

        // find largest i which must be drawn
        // notePositionLeft > rectangleCenter + i * (widthFactor * maximumLabelWidth + textPadding) - 0.5f * maxLabelWidth + horizontalScrollPosition
        // => i < -(rectangleCenter - 0.5f * maxLabelWidth - notePositionLeft + horizontalScrollPosition) / (widthFactor * maxLabelWidth + textPadding)
        // with notePositionLeft = width - paddingRight
        // => i < -(rectangleCenter - 0.5f * maxLabelWidth - width + paddingRight + horizontalScrollPosition) / (widthFactor * maxLabelWidth + textPadding)

        if (noteLabels.size > 0) {
            val arrayIndexMin = getVisibleArrayIndexMin()
            val arrayIndexMax = getVisibleArrayIndexMax()
//            Log.v("Tuner", "NoteSelector.onDraw: arrayIndexMin=$arrayIndexMin, arrayIndexMax=$arrayIndexMax")
            for (arrayIndex in arrayIndexMin .. arrayIndexMax) {
//                Log.v("Tuner", "NoteSelector.onDraw: arrayIndex=$arrayIndex, activeNoteArrayIndex=$activeNoteArrayIndex")
                val noteLayout = noteLabels[arrayIndex].layouts[if (arrayIndex == activeNoteArrayIndex) 0 else 1]
                if (noteLayout != null) {
                    val xPos = rectangleCenter + arrayIndex * (widthFactor * maxLabelWidth + textPadding) + horizontalScrollPosition - 0.5f * noteLayout.width
                    //val yPos = paddingTop + windowPaint.strokeWidth + textPadding
                    val yPos = textBaseline + noteLayout.getLineDescent(0) - noteLayout.height
                    //val yPos = height - paddingBottom - windowPaint.strokeWidth - textPadding + noteLayout.getLineAscent(0)
//                    Log.v("Tuner", "NoteSelector.onDraw: xPos=$xPos, yPos=$yPos, center=${rectangleCenter + arrayIndex * (widthFactor * maxLabelWidth + textPadding)}, center-0.5maxWidth=${rectangleCenter + arrayIndex * (widthFactor * maxLabelWidth + textPadding)-0.5f*maxLabelWidth}")
                    canvas.withTranslation(xPos, yPos) {
                        noteLayout.draw(canvas)
                        //canvas.drawRect(0f, 0f, 10f, 10f, windowPaint)
                    }
                }
            }
        }

        canvas.drawRoundRect(
            rectangleCenter - 0.5f * maxLabelWidth - 0.5f * windowPaint.strokeWidth - textPadding,
            paddingTop + 0.5f * windowPaint.strokeWidth,
            rectangleCenter + 0.5f * maxLabelWidth + 0.5f * windowPaint.strokeWidth + textPadding,
            height - paddingBottom - 0.5f * windowPaint.strokeWidth,
            rectangleRadius, rectangleRadius,
            windowPaint
        )
    }

    fun setNotes(toneIndexBegin: Int, toneIndexEnd: Int, labels: (Int) -> CharSequence) {
        val oldToneIndex = if (activeNoteArrayIndex in noteLabels.indices)
            noteLabels[activeNoteArrayIndex].toneIndex
        else
            0

        noteLabels.clear()
        val numLabels = toneIndexEnd - toneIndexBegin
        maxLabelWidth = 0

        if (isLaidOut) {
            val s = computeLabelTextSize(height)
//            Log.v("Tuner", "NoteSelector.setNotes: s=$s, textSize=${labelPaint[0].textSize}")
            setLabelPaintSizeAndFontMetrics(s)
        }

        for (i in 0 until numLabels) {
            val toneIndex = toneIndexBegin + i
            noteLabels.add(NoteLabel(toneIndex, labels(toneIndex), labelPaint))
            maxLabelWidth = max(maxLabelWidth, noteLabels.last().maxLabelWidth)
        }

        activeNoteArrayIndex = if (oldToneIndex in toneIndexBegin until toneIndexEnd)
            oldToneIndex - noteLabels[0].toneIndex
        else
            0
        if (isLaidOut)
            scrollToActiveNote(0L)
    }

    fun setActiveTone(toneIndex: Int, animationDuration: Long) {
        if (noteLabels.size == 0)
            return
        val index = toneIndex - noteLabels[0].toneIndex
        if (index != activeNoteArrayIndex && index in 0 until noteLabels.size) {
            activeNoteArrayIndex = index
            scrollToActiveNote(animationDuration)
        }
    }

    private fun computeLabelTextSize(totalHeight: Int): Float {
        return totalHeight - 2 * (textPadding + windowPaint.strokeWidth) - paddingTop - paddingBottom
    }

    private fun setLabelPaintSizeAndFontMetrics(totalTextHeight: Float) {
        labelPaint.forEachIndexed { index, textPaint ->
            textPaint.textSize = totalTextHeight
            val fm = fontMetrics[index]
            textPaint.getFontMetrics(fm)
            textPaint.textSize = floor(0.7f * totalTextHeight * totalTextHeight / (fm.bottom - fm.top))
            //textPaint.textSize = floor(s * s / (labelPaint[0].descent() - labelPaint[0].ascent()))
            textPaint.getFontMetrics(fm)
        }
    }

    private fun getScrollPositionByArrayIndex(index: Int): Float {
        return -index * (widthFactor * maxLabelWidth + textPadding)
    }

    private fun getActiveIndexByScrollPosition(position: Float): Int {
        return (-position / (widthFactor * maxLabelWidth + textPadding)).roundToInt()
    }

    private fun scrollDistance(distance: Float) {
        horizontalScrollPosition -= distance
        horizontalScrollPosition = min(horizontalScrollPosition, computeMaxScrollPosition())
        horizontalScrollPosition = max(horizontalScrollPosition, computeMinScrollPosition())
        val noteIndex = getActiveIndexByScrollPosition(horizontalScrollPosition)
        if (noteIndex != activeNoteArrayIndex) {
            activeNoteArrayIndex = noteIndex
            toneChangedListener?.onToneChanged(noteLabels[noteIndex].toneIndex)
        }
        ViewCompat.postInvalidateOnAnimation(this@NoteSelector)
    }

    private fun computeMinScrollPosition(): Float {
        return -(noteLabels.size - 1) * (widthFactor * maxLabelWidth + textPadding)
    }
    private fun computeMaxScrollPosition(): Float {
        return 0f
    }

    private fun getRectangleCenter(): Float {
        return 0.5f * (paddingLeft + width - paddingRight)
    }

    private fun getVisibleArrayIndexMin(): Int {
        val rectangleCenter = getRectangleCenter()
        val index = ceil(-(rectangleCenter + 0.5f * maxLabelWidth - paddingLeft + horizontalScrollPosition) / (widthFactor * maxLabelWidth + textPadding)).toInt()
        return max(0, index)
    }

    private fun getVisibleArrayIndexMax(): Int {
        val rectangleCenter = getRectangleCenter()
        val index = floor(-(rectangleCenter - 0.5f * maxLabelWidth - width + paddingRight + horizontalScrollPosition) / (widthFactor * maxLabelWidth + textPadding)).toInt()
        return min(noteLabels.size - 1, index)
    }

    private fun noteIndexFromX(x: Float): Int {
        val rectangleCenter = getRectangleCenter()
        // notePositionCenter = rectangleCenter + i * (widthFactor * maxLabelWidth + textPadding) + horizontalScrollPosition
        // | x - notePositionCenter | = min
        val i = ((x - rectangleCenter - horizontalScrollPosition) / (widthFactor * maxLabelWidth + textPadding)).roundToInt()

//        Log.v("Tuner", "NoteSelector.noteIndexFromX: $i")
        return if (i in 0 until noteLabels.size)
            i
        else
            -1
    }

    private fun scrollToActiveNote(animationDuration: Long) {
        offsetAnimator.cancel()
        flingAnimation.cancel()

        val targetScrollPosition = getScrollPositionByArrayIndex(activeNoteArrayIndex)
        if (animationDuration == 0L) {
            horizontalScrollPosition = targetScrollPosition
            invalidate()
        } else {
            offsetAnimator.duration = animationDuration
            offsetAnimator.setFloatValues(horizontalScrollPosition, targetScrollPosition)
            offsetAnimator.start()
        }
    }
}