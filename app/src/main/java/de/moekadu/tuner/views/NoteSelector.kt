package de.moekadu.tuner.views

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatValueHolder
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.*
import kotlin.math.*

class NoteSelector(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    : View(context, attrs, defStyleAttr) {
    companion object {
        private const val NUM_STYLES = 2
    }

    private class LabelSetSize(var width: Float, var height: Float)

    fun interface NoteChangedListener {
        fun onNoteChanged(newNote: MusicalNote)
    }

    var noteChangedListener: NoteChangedListener? = null

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

        override fun onDown(e: MotionEvent): Boolean {
//            Log.v("Tuner", "NoteSelector: gestureListener.OnDown")
            flingAnimation.cancel()
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
//            Log.v("Tuner", "NoteSelector: gestureListener.OnScroll x=$distanceX, y=$distanceY")
            scrollDistance(distanceX)
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
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

        override fun onSingleTapUp(e: MotionEvent): Boolean {
//            if (e == null || stringClickedListener == null)
            if (e == null)
                return true

            val x = e.x
            val arrayIndex = arrayIndexFromX(x)
            if (arrayIndex < 0)
                return false

            if (arrayIndex != activeNoteArrayIndex) {
                activeNoteArrayIndex = arrayIndex
                noteNameScale?.let {
                    val noteIndex = arrayIndex + noteIndexBegin
                    noteChangedListener?.onNoteChanged(it.getNoteOfIndex(noteIndex))
                }
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

    /** Class for measuring and printing notes. */
    private var noteNamePrinter = createNoteNamePrinter(context, NotationType.Standard, NoteNamePrinter.SharpFlatPreference.None)

    /** For each style we store the label of all notes.
     *  We use lazy creation, so they are only non-null if needed.
     */
    private val noteLabels = Array<Array<MusicalNoteLabel?> >(NUM_STYLES) { arrayOf(null) }

    /** Paint for drawing the labels.
     *  Index 0 is the paint which draws the active note (the one inside the rectangle window)
     *  Index 1 is the paint which draws the notes outside the rectangle window.
     */
    private val labelPaint = Array(NUM_STYLES) {
        TextPaint().apply {
            isAntiAlias = true
        }
    }

    /** The text size for the labelPaints.
     * 0f will determine the text size based on the view height.
     * Other values will define the actual text size of the label.
     */
    private var textSize = 0f

    private var maxLabelWidth = 0f
    private var maxDistanceAboveBaseline = 0f

    /** The total width of a total entry (including label width, padding, stroke width) */
    private val singleEntryWidth get() = maxLabelWidth + 2 * (textPadding + windowPaint.strokeWidth)

    /** Minimum space between text label and window rectangle */
    private var textPadding = 0f
    /** Paint to draw the window rectangle. */
    private val windowPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    /** Corner radius of the window rectangle. */
    private var rectangleRadius = 0f

    /** Array index of the active note. */
    private var activeNoteArrayIndex = -1
    /** Get active musical note. */
    val activeNote: MusicalNote?
        get() {
            return if (activeNoteArrayIndex in 0 until numNotes)
                noteNameScale?.getNoteOfIndex(activeNoteArrayIndex + noteIndexBegin)
            else
                null
        }

    /** The class which defines which notes should be shown. */
    private var noteNameScale: NoteNameScale? = null
    /** First note index of the selector. */
    private var noteIndexBegin = Int.MAX_VALUE
    /** End note index of the selector (excluded. */
    private var noteIndexEnd = Int.MAX_VALUE
    /** Total number of notes which will be shown by the note selector. */
    private val numNotes get() = noteIndexEnd - noteIndexBegin

    /** Defines the scroll position of the note selector. */
    private var horizontalScrollPosition = 0f

    /** If true, octave index will also be printed, if false, only the notes itself will be printed. */
    private var enableOctaveIndex = true

    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs,
        R.attr.noteSelectorStyle
    )

    init {
        val typefaceValues = Array(NUM_STYLES) {0}
        attrs?.let {
            val ta = context.obtainStyledAttributes(attrs,
                R.styleable.NoteSelector, defStyleAttr,
                R.style.NoteSelectorStyle
            )

            labelPaint[0].color = ta.getColor(R.styleable.NoteSelector_labelTextColor, Color.BLACK)
            labelPaint[1].color = ta.getColor(R.styleable.NoteSelector_labelTextColor2, Color.GREEN)

            textSize = ta.getDimension(R.styleable.NoteSelector_labelTextSize, textSize)

            typefaceValues[0] = ta.getInt(R.styleable.NoteSelector_textStyle, 0)
            typefaceValues[1] = ta.getInt(R.styleable.NoteSelector_textStyle2, 0)

            windowPaint.color = ta.getColor(R.styleable.NoteSelector_windowColor, Color.RED)
            windowPaint.strokeWidth = ta.getDimension(R.styleable.NoteSelector_windowStrokeWidth, 3f)

            textPadding = ta.getDimension(R.styleable.NoteSelector_textPadding, 4f)
            rectangleRadius = ta.getDimension(R.styleable.NoteSelector_rectangleRadius, rectangleRadius)

            enableOctaveIndex = ta.getBoolean(R.styleable.NoteSelector_enableOctaveIndex, enableOctaveIndex)

            ta.recycle()
        }
        for (i in 0 until NUM_STYLES) {
            when (typefaceValues[i]) {
                0 -> labelPaint[i].typeface = Typeface.DEFAULT
                1 -> labelPaint[i].typeface = Typeface.DEFAULT_BOLD
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
//        Log.v("StaticLayoutTest", "NoteSelector.onMeasure: heightMode = $heightMode, widthMode = $widthMode")
//        Log.v("StaticLayoutTest", "NoteSelector.onMeasure: unspec = ${MeasureSpec.UNSPECIFIED}, most = ${MeasureSpec.AT_MOST}, exact = ${MeasureSpec.EXACTLY}")
        var proposedHeight = max(MeasureSpec.getSize(heightMeasureSpec), suggestedMinimumHeight)
        var proposedWidth = max(MeasureSpec.getSize(widthMeasureSpec), suggestedMinimumWidth)

        if (heightMode != MeasureSpec.EXACTLY || widthMode != MeasureSpec.EXACTLY) {
            val maxSize = getMaximumLabelSize()
            if (heightMode != MeasureSpec.EXACTLY) {
//                Log.v("StaticLayoutTest", "NoteSelector.onMeasure: setting proposed height to $proposedHeight")
                proposedHeight =
                    (maxSize.height + paddingBottom + paddingTop + 2 * textPadding + 2 * windowPaint.strokeWidth).roundToInt()
            }
            if (widthMode != MeasureSpec.EXACTLY)
                proposedWidth = (4 * (maxSize.width + 2 * textPadding + 2 * windowPaint.strokeWidth) + paddingLeft + paddingRight).roundToInt()
        }
//        Log.v("StaticLayoutTest", "NoteSelector.onMeasure: proposedHeight=$proposedHeight")
        val h = resolveSize(proposedHeight, heightMeasureSpec)
        val w = resolveSize(proposedWidth, widthMeasureSpec)
        setMeasuredDimension(w, h)
    }

    private fun getMaximumLabelSize(): LabelSetSize {
        val noteNameScaleLocal = noteNameScale ?: return LabelSetSize(60f, 30f)
        val octaveBegin = noteNameScaleLocal.getNoteOfIndex(noteIndexBegin).octave
        val octaveEnd = noteNameScaleLocal.getNoteOfIndex(noteIndexEnd - 1).octave + 1

        var maxWidth = 0f
        var maxHeight = 0f
        labelPaint.forEach { textPaint ->
            textPaint.textSize = if (textSize == 0f) 30f else textSize
            val measures = MusicalNoteLabel.getLabelSetBounds(
                noteNameScaleLocal.notes,
                octaveBegin,
                octaveEnd,
                textPaint,
                noteNamePrinter,
                enableOctaveIndex = enableOctaveIndex
            )
            maxHeight = max(maxHeight, measures.maxDistanceAboveBaseline + measures.maxDistanceBelowBaseline)
            maxWidth = max(maxWidth, measures.maxWidth)
        }
        return LabelSetSize(maxWidth, maxHeight)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null)
            return super.onTouchEvent(null)

        val g = gestureDetector.onTouchEvent(event)
//        Log.v("Tuner", "NoteSelector.onTouchEvent: $g")
        val u = if (!g && event.actionMasked == MotionEvent.ACTION_UP) {
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
        if (h != oldh) {
            val s = computeLabelHeight(h)
            noteLabels.forEach { it.forEachIndexed { index, _ -> it[index] = null  } }
            noteNameScale?.let {
                setLabelPaintSizeAndMaximumLabelWidthAndBaseline(s, it)
            }
            scrollToActiveNote(0L)
        }

        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val rectangleCenter = getRectangleCenter()
        canvas.clipRect(paddingLeft, 0, width - paddingRight, height)

        val textTop = paddingTop + windowPaint.strokeWidth + textPadding
        val textBaseline = textTop + maxDistanceAboveBaseline
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

        if (numNotes > 0) {
            val arrayIndexMin = getVisibleArrayIndexMin()
            val arrayIndexMax = getVisibleArrayIndexMax()
//            Log.v("Tuner", "NoteSelector.onDraw: arrayIndexMin=$arrayIndexMin, arrayIndexMax=$arrayIndexMax")
            for (arrayIndex in arrayIndexMin .. arrayIndexMax) {
                val styleIndex = if (arrayIndex == activeNoteArrayIndex) 0 else 1
                val existingLabel = noteLabels[styleIndex][arrayIndex]
                val label = if (existingLabel != null) {
                    existingLabel
                } else {
                    val newLabel = noteNameScale?.let { scale ->
                        MusicalNoteLabel(
                            scale.getNoteOfIndex(arrayIndex + noteIndexBegin),
                            labelPaint[styleIndex], noteNamePrinter,
                            enableOctaveIndex = enableOctaveIndex
                        )
                    }
                    noteLabels[styleIndex][arrayIndex] = newLabel
                    newLabel
                }

                val xPos = rectangleCenter + arrayIndex * singleEntryWidth + horizontalScrollPosition
                label?.drawToCanvas(xPos, textBaseline, LabelAnchor.Baseline, canvas)
            }
        }

        canvas.drawRoundRect(
            rectangleCenter - 0.5f * singleEntryWidth + 0.5f * windowPaint.strokeWidth,
            paddingTop + 0.5f * windowPaint.strokeWidth,
            rectangleCenter + 0.5f * singleEntryWidth - 0.5f * windowPaint.strokeWidth,
            height - paddingBottom - 0.5f * windowPaint.strokeWidth,
            rectangleRadius, rectangleRadius,
            windowPaint
        )
    }

    /** Set notes of selector.
     * @param noteIndexBegin Index which is used to extract the first note of the selector from
     *   the note name scale.
     * @param noteIndexEnd Index after (excluded) which is used to extract the last note of the
     *   selector from the note name scale.
     * @param noteNameScale Note names.
     * @param newActiveNote Set the new note to this value if possible. If this is null, we
     *   keep the selected value. If it is not part of the new scale, we choose something.
     * @param noteNamePrinter Object which does note name printing.
     */
    fun setNotes(noteIndexBegin: Int, noteIndexEnd: Int, noteNameScale: NoteNameScale,
                 newActiveNote: MusicalNote?, noteNamePrinter: NoteNamePrinter) {
        this.noteNamePrinter = noteNamePrinter
        val activeNoteBackup = activeNote
        val activeNoteIndexBackupPercent = if (numNotes > 0)
            (activeNoteArrayIndex).toDouble() / (numNotes).toDouble()
        else
            0.5

        maxLabelWidth = 0f
        this.noteNameScale = noteNameScale
        this.noteIndexBegin = noteIndexBegin
        this.noteIndexEnd = noteIndexEnd

        for (i in 0 until NUM_STYLES)
            noteLabels[i] = Array(numNotes) {null}

        if (isLaidOut) {
            val s = computeLabelHeight(height)
//            Log.v("Tuner", "NoteSelector.setNotes: s=$s, textSize=${labelPaint[0].textSize}")
            setLabelPaintSizeAndMaximumLabelWidthAndBaseline(s, noteNameScale)
        }

        activeNoteArrayIndex = if (newActiveNote != null
            && noteNameScale.getIndexOfNote(newActiveNote) in this.noteIndexBegin until this.noteIndexEnd) {
            noteNameScale.getIndexOfNote(newActiveNote) - noteIndexBegin
        } else if (activeNoteBackup != null
            && noteNameScale.getIndexOfNote(activeNoteBackup) in this.noteIndexBegin until this.noteIndexEnd) {
            noteNameScale.getIndexOfNote(activeNoteBackup) - noteIndexBegin
        } else if (activeNoteBackup != null) {
            min((activeNoteIndexBackupPercent * numNotes).roundToInt(), numNotes - 1)
        } else {
            (numNotes) / 2
        }

        if (isLaidOut)
            scrollToActiveNote(0L)
        requestLayout()
    }

    /** Set new active note.
     * @param note Note which should become active, or null for no active note.
     * @param animationDuration Animation duration for scrolling to note in ms.
     * @return True if scrolling successful (or rather if given note is part of the selector)
     *   or false otherwise (meaning, that the given note is not part of the selector)
     */
    fun setActiveNote(note: MusicalNote?, animationDuration: Long): Boolean {
        if (note == null) {
            activeNoteArrayIndex = -1
            return false
        }

        if (numNotes <= 0) // this might not be needed anymore due to the next check of note_index < 0
            return false

        val noteIndex = noteNameScale?.getIndexOfNote(note) ?: Int.MAX_VALUE
        if (noteIndex == Int.MAX_VALUE)
            return false

        val arrayIndex = noteIndex - noteIndexBegin
        if (arrayIndex != activeNoteArrayIndex && arrayIndex in 0 until numNotes) {
            activeNoteArrayIndex = arrayIndex
            scrollToActiveNote(animationDuration)
            return true
        }
        return false
    }

    private fun computeLabelHeight(totalHeight: Int): Float {
        return totalHeight - 2 * (textPadding + windowPaint.strokeWidth) - paddingTop - paddingBottom
    }

    private fun setLabelPaintSizeAndMaximumLabelWidthAndBaseline(totalTextHeight: Float, noteNameScale: NoteNameScale) {
        var maxLabelWidth = 0f
        var maxDistanceAboveBaseline = 0f
        var maxDistanceBelowBaseline = 0f

        val octaveBegin = noteNameScale.getNoteOfIndex(noteIndexBegin).octave
        val octaveEnd = noteNameScale.getNoteOfIndex(noteIndexEnd - 1).octave + 1

        labelPaint.forEach { textPaint ->
            textPaint.textSize = if (textSize == 0f) totalTextHeight else textSize

            val measures = MusicalNoteLabel.getLabelSetBounds(
                noteNameScale.notes,
                octaveBegin,
                octaveEnd,
                textPaint,
                noteNamePrinter,
                enableOctaveIndex = enableOctaveIndex
            )

            maxDistanceAboveBaseline = max(maxDistanceAboveBaseline, measures.maxDistanceAboveBaseline)
            maxDistanceBelowBaseline = max(maxDistanceBelowBaseline, measures.maxDistanceBelowBaseline)
            maxLabelWidth = max(maxLabelWidth, measures.maxWidth)
        }

        val maxLabelHeight = maxDistanceAboveBaseline + maxDistanceBelowBaseline

        if (textSize == 0f) {
            val fontScaling = totalTextHeight / maxLabelHeight
            labelPaint.forEach { it.textSize = fontScaling * totalTextHeight }
            this.maxDistanceAboveBaseline = maxDistanceAboveBaseline * fontScaling
            this.maxLabelWidth = fontScaling * maxLabelWidth
        } else {
            labelPaint.forEach { it.textSize = textSize }
            val labelTop = 0.5f * (totalTextHeight - maxLabelHeight)
            this.maxDistanceAboveBaseline = labelTop + maxDistanceAboveBaseline
            this.maxLabelWidth = maxLabelWidth
        }
    }

    private fun getScrollPositionByArrayIndex(arrayIndex: Int): Float {
        return -arrayIndex * singleEntryWidth
    }

    private fun getActiveArrayIndexByScrollPosition(position: Float): Int {
        return (-position / singleEntryWidth).roundToInt()
    }

    private fun scrollDistance(distance: Float) {
        horizontalScrollPosition -= distance
        horizontalScrollPosition = min(horizontalScrollPosition, computeMaxScrollPosition())
        horizontalScrollPosition = max(horizontalScrollPosition, computeMinScrollPosition())
        val arrayIndex = getActiveArrayIndexByScrollPosition(horizontalScrollPosition)
        if (arrayIndex != activeNoteArrayIndex) {
            activeNoteArrayIndex = arrayIndex
            noteNameScale?.let {
                val noteIndex = arrayIndex + noteIndexBegin
                val note = it.getNoteOfIndex(noteIndex)
                noteChangedListener?.onNoteChanged(note)
            }
        }
        ViewCompat.postInvalidateOnAnimation(this@NoteSelector)
    }

    private fun computeMinScrollPosition(): Float {
        return -(numNotes - 1) * singleEntryWidth
    }
    private fun computeMaxScrollPosition(): Float {
        return 0f
    }

    private fun getRectangleCenter(): Float {
        return 0.5f * (paddingLeft + width - paddingRight)
    }

    private fun getVisibleArrayIndexMin(): Int {
        val rectangleCenter = getRectangleCenter()
        val index = ceil(-(rectangleCenter + 0.5f * singleEntryWidth - paddingLeft + horizontalScrollPosition) / singleEntryWidth).toInt()
        return max(0, index)
    }

    private fun getVisibleArrayIndexMax(): Int {
        val rectangleCenter = getRectangleCenter()
        val index = floor(-(rectangleCenter - 0.5f * singleEntryWidth - width + paddingRight + horizontalScrollPosition) / singleEntryWidth).toInt()
        return min(numNotes - 1, index)
    }

    /** Return array index from given x-position.
     *
     * @param x x-position.
     * @return Array index or -1 if x-position is out of range.
     */
    private fun arrayIndexFromX(x: Float): Int {
        val rectangleCenter = getRectangleCenter()
        // notePositionCenter = rectangleCenter + i * (widthFactor * maxLabelWidth + textPadding) + horizontalScrollPosition
        // | x - notePositionCenter | = min
        val i = ((x - rectangleCenter - horizontalScrollPosition) / singleEntryWidth).roundToInt()

//        Log.v("Tuner", "NoteSelector.noteIndexFromX: $i")
        return if (i in 0 until numNotes)
            i
        else
            -1
    }

    private fun scrollToActiveNote(animationDuration: Long) {
        if (activeNoteArrayIndex < 0)
            return

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
