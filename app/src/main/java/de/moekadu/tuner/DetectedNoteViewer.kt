package de.moekadu.tuner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.withTranslation
import kotlin.math.*

class DetectedNoteViewer(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    : View(context, attrs, defStyleAttr) {
    companion object {
        private val NO_TONE_INDEX = Int.MAX_VALUE
    }
    fun interface NoteClickedListener {
        fun onNoteClicked(toneIndex: Int)
    }

    var noteClickedListener: NoteClickedListener? = null

    class DetectedNote(private var hitCountMin: Int, private var hitCountMax: Int) {
        var toneIndex = 0
            private set
        var isEnabled = false
            private set

        private var layout: StaticLayout? = null
        private var label: CharSequence? = null

        var hitCount = hitCountMax
            private set

        private var textSize = -1f
        private var textStyle = -1
        private var textColor = Color.BLACK

        fun setNewTone(toneIndex: Int, label: CharSequence) {
            this.toneIndex = toneIndex
            this.label = label
            hitCount = hitCountMax
            isEnabled = true
        }
        fun clear() {
            isEnabled = false
        }

        fun setHitCountRange(minValue: Int, maxValue: Int) {
            hitCountMin = minValue
            hitCountMax = maxValue
            hitCount = max(hitCountMin, hitCount)
            hitCount = min(hitCountMax, hitCount)
        }

        fun hit(count: Int = 1) {
            hitCount += count
            hitCount = max(hitCountMin, hitCount)
            hitCount = min(hitCountMax, hitCount)
        }

        fun drawToCanvas(canvas: Canvas, x: Float, y: Float, labelPaint: TextPaint) {
            if (!isEnabled)
                return
            val newTextStyle = labelPaint.typeface?.style ?: -1
            if (labelPaint.textSize != textSize || newTextStyle != textStyle || labelPaint.color != textColor) {
//                Log.v("Tuner", "DetectedNoteViewer.DetectedNote.drawToCanvas: create label for note with toneIndex $toneIndex")
                layout = buildLabelLayout(labelPaint)
                textSize = labelPaint.textSize
                textStyle = newTextStyle
                textColor = labelPaint.color
            }
            val layoutLocal = layout ?: return
            canvas.withTranslation(
                x - 0.5f * layoutLocal.width,
                //y - 0.5f * layoutLocal.height + 0.5f * (layoutLocal.topPadding + layoutLocal.bottomPadding)) {
                y - 0.5f * layoutLocal.height + layoutLocal.topPadding
            ) {
//                Log.v("Tuner", "DetectedNoteViewer.DetectedNote.drawToCanvas: toneIndex $toneIndex, drawing label=$label, x=$x, y=$y")
                //labelPaint.alpha = 100
                //canvas.drawRect(0f, 0f, layoutLocal.width.toFloat(), layoutLocal.height.toFloat(), labelPaint)
                //canvas.drawRect(0f, -layoutLocal.topPadding.toFloat(), layoutLocal.width.toFloat(), layoutLocal.height.toFloat() - layoutLocal.bottomPadding, labelPaint)
                //canvas.drawRect(0f, -layoutLocal.topPadding.toFloat(), 10f, -layoutLocal.topPadding + 3f, labelPaint)
                //canvas.drawRect(0f, -layoutLocal.getLineTop(0).toFloat(), 10f, layoutLocal.getLineTop(0) + 3f, labelPaint)
                //canvas.drawRect(0f, layoutLocal.height - layoutLocal.bottomPadding.toFloat(), 10f, layoutLocal.height - layoutLocal.bottomPadding.toFloat()- 3f, labelPaint)
                //labelPaint.alpha = 255
                layoutLocal.draw(canvas)
            }
        }

        fun getTextSizeInPercentOfMax(): Float {
            return hitCount.toFloat() / hitCountMax.toFloat()
        }

        private fun buildLabelLayout(labelPaint: TextPaint): StaticLayout? {
            label?.let { l ->
                val desiredWidth = ceil(StaticLayout.getDesiredWidth(l, labelPaint)).toInt()
                return StaticLayout.Builder.obtain(l, 0, l.length, labelPaint, desiredWidth).build()
            }
            return null
        }
    }

    private val labelPaint = TextPaint().apply {
        isAntiAlias = true
    }
    private val fontMetrics = Paint.FontMetrics()
    private var maximumTextSize = 0f

    private val minimumAspectPerNote = 1.2f

    private var aspectRatioMin = -1f
    private var aspectRatioMax = -1f

    private var toneIndexBegin = 0
    private var toneIndexEnd = 0

    private var toneIndexToLabel: ((Int) -> CharSequence)? = null

    private var notes = Array(1) {DetectedNote((hitCountMax * ratioMinSizeToMaxSize).roundToInt(), hitCountMax)}
    private var leastRecentlyUsedTones = Array(1){ NO_TONE_INDEX }

    private var minimumLabelSpace = 0f

    private var clickedToneIndex = NO_TONE_INDEX

    private var title: String? = null
    private var titleLayout: StaticLayout? = null
    private val titlePaint = TextPaint().apply {
        isAntiAlias = true
        textSize = 10f
        color = Color.BLACK
    }
    private val titleBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    var titlePadding = 4f

    private var boxCornerRadius = 0f
    private var notePadding = 4f // minimum space between note top box lines
    private val boxPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 1f
        color = Color.BLACK
        style = Paint.Style.STROKE
    }

    private var hitCountMax = 100
    private val ratioMinSizeToMaxSize = 0.5f
    private val durationToGrowToneSizeInSeconds = 1.0f

    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, R.attr.detectedNoteViewerStyle)

    init {
        // var numNotes = 4
        attrs?.let {
            val ta = context.obtainStyledAttributes(
                attrs,
                R.styleable.DetectedNoteViewer,
                defStyleAttr,
                R.style.DetectedNoteViewerStyle
            )

            labelPaint.color = ta.getColor(R.styleable.DetectedNoteViewer_labelTextColor, Color.BLACK)
            // numNotes = ta.getInt(R.styleable.DetectedNoteViewer_noteNumber, numNotes)
            minimumLabelSpace = ta.getDimension(R.styleable.DetectedNoteViewer_minimumLabelSpace, minimumLabelSpace)

            title = ta.getString(R.styleable.DetectedNoteViewer_title)
            titlePaint.textSize = ta.getDimension(R.styleable.DetectedNoteViewer_titleSize, titlePaint.textSize)
            titlePaint.color = ta.getColor(R.styleable.DetectedNoteViewer_titleColor, titlePaint.color)
            titlePaint.alpha = (255 * ta.getFloat(R.styleable.DetectedNoteViewer_titleOpacity, 1.0f)).roundToInt()
            titleBackgroundPaint.color = ta.getColor(R.styleable.DetectedNoteViewer_titleBackgroundColor, titleBackgroundPaint.color)
            titlePadding = ta.getDimension(R.styleable.DetectedNoteViewer_titlePadding, titlePadding)
            boxPaint.color = ta.getColor(R.styleable.DetectedNoteViewer_boxStrokeColor, boxPaint.color)
            boxPaint.strokeWidth = ta.getDimension(R.styleable.DetectedNoteViewer_boxStrokeWidth, boxPaint.strokeWidth)
            boxPaint.alpha = (255 * ta.getFloat(R.styleable.DetectedNoteViewer_boxStrokeOpacity, 1.0f)).roundToInt()
            boxCornerRadius = ta.getDimension(R.styleable.DetectedNoteViewer_boxCornerRadius, boxCornerRadius)
            notePadding = ta.getDimension(R.styleable.DetectedNoteViewer_notePadding, notePadding)
            ta.recycle()
        }

        title?.let {
            val desiredWidth = ceil(StaticLayout.getDesiredWidth(it, titlePaint)).toInt()
            titleLayout = StaticLayout.Builder.obtain(it, 0, it.length, titlePaint, desiredWidth).build()
        }
//        notes = Array(numNotes) { DetectedNote() }
//        leastRecentlyUsedTones = Array(numNotes) {-1}
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val numNotes = computeNumNotes(w.toFloat(), h.toFloat())
        if (notes.size != numNotes) {
            notes = Array(numNotes) { DetectedNote((hitCountMax * ratioMinSizeToMaxSize).roundToInt(), hitCountMax) }
            leastRecentlyUsedTones = Array(numNotes) { NO_TONE_INDEX }
        }
        maximumTextSize = computePaintTextSize(width, height, aspectRatioMax)

        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas == null)
            return

        //val yPosCenter = 0.5f * (paddingTop + height - paddingBottom)
        val yPosCenter = 0.5f * (computeBoxContentTop() + computeBoxContentBottom(height))
        val horizontalSpacePerNote = computeHorizontalSpacePerNote()
        notes.forEachIndexed { index, note ->
            if (note.isEnabled) {
                val xPosCenter = paddingLeft + boxPaint.strokeWidth + 0.5f * minimumLabelSpace + (0.5f + index) * horizontalSpacePerNote
                //labelPaint.textSize = if (note.toneIndex == clickedToneIndex) maximumTextSize else note.getTextSizeInPercentOfMax() * maximumTextSize
                var textHeightPercent = note.getTextSizeInPercentOfMax()
                if (note.toneIndex == clickedToneIndex)
                    textHeightPercent = min(1f, textHeightPercent + 0.2f)

                labelPaint.textSize = textHeightPercent * maximumTextSize
//                Log.v("Tuner", "DetectedNoteViewer.onDraw: drawing note ${note.toneIndex}, clicked=$clickedToneIndex, x=$xPosCenter, y=$yPosCenter, w=$width, h=$height, maxText=$maximumTextSize")
                labelPaint.typeface = if (note.toneIndex == clickedToneIndex) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                note.drawToCanvas(canvas, xPosCenter, yPosCenter, labelPaint)
            }
        }

        canvas.drawRoundRect(paddingLeft + 0.5f * boxPaint.strokeWidth, computeBoxTop(),
        width - paddingRight - 0.5f * boxPaint.strokeWidth, computeBoxBottom(height),
            boxCornerRadius, boxCornerRadius, boxPaint)
        titleLayout?.let { layout ->
            canvas.withTranslation(
                paddingLeft + 0.5f * boxPaint.strokeWidth + 3 * boxCornerRadius + titlePadding,
                computeBoxTop() - 0.5f * layout.height
            ) {
                canvas.drawRect(-titlePadding, 0f, layout.width + titlePadding, layout.height.toFloat(), titleBackgroundPaint)
                layout.draw(canvas)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null)
            return super.onTouchEvent(event)
        val action = event.actionMasked
//        Log.v("Tuner", "DetectedNoteViewer.onTouchEvent: action=$action")
        val toneIndex = xPositionToToneIndex(event.x)
        val clickedToneIndexOld = clickedToneIndex

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                clickedToneIndex = toneIndex
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                clickedToneIndex = NO_TONE_INDEX
//                Log.v("Tuner", "DetectedNoteViewer.onTouchEvent: ACTION_UP, toneIndex= $toneIndex, x=${event.x}")
                if (toneIndex != NO_TONE_INDEX) {
                    performClick()
                    noteClickedListener?.onNoteClicked(toneIndex)
                }
            }
        }

//        Log.v("Tuner", "DetectedNoteViewer.onTouchEvent: clickedToneIndex= $clickedToneIndex")
        if (clickedToneIndex != clickedToneIndexOld)
            invalidate()

        super.onTouchEvent(event)
        return true
    }

    fun setNotes(toneIndexBegin: Int, toneIndexEnd: Int, toneIndexToLabel: (Int) -> CharSequence) {
        this.toneIndexToLabel = toneIndexToLabel
        this.toneIndexBegin = toneIndexBegin
        this.toneIndexEnd = toneIndexEnd
        computeAspectRatioMinMax()
//        Log.v("Tuner", "DetectedNoteViewer.setNotes: Check toneIndex 0: label=${this.toneIndexToLabel?.let { it(0) }}")
        if (isLaidOut) {
            maximumTextSize = computePaintTextSize(width, height, aspectRatioMax)
            notes.forEach { it.clear() }
        }
    }

    /// Set approximate update interval with which you are going to call hitNote()
    /**
     * @param durationInSeconds Update duration in seconds
     */
    fun setApproximateHitNoteUpdateInterval(durationInSeconds: Int) {
        hitCountMax = if (durationInSeconds == 0)
            50
        else
            (durationToGrowToneSizeInSeconds / (durationInSeconds * ratioMinSizeToMaxSize)).roundToInt()

        val hitCountMin = (hitCountMax * ratioMinSizeToMaxSize).roundToInt()

        for (n in notes)
            n.setHitCountRange(hitCountMin, hitCountMax)
        invalidate()
    }

    fun hitNote(toneIndex: Int) {
        if (notes.isEmpty())
            return

        val index = notes.indexOfFirst { it.toneIndex == toneIndex && it.isEnabled}

        if (index >= 0) {
            val lruIndex = leastRecentlyUsedTones.indexOfFirst { it == toneIndex }
            require(lruIndex >= 0)
            for (i in lruIndex until leastRecentlyUsedTones.size - 1)
                leastRecentlyUsedTones[i] = leastRecentlyUsedTones[i + 1]
            notes[index].hit(2)
        } else {
            val leastRecentlyToneIndex = leastRecentlyUsedTones[0]
//            Log.v("Tuner", "DetectedNoteViewer.hitTone: Creating new label label=${toneIndexToLabel?.let { it(toneIndex) }}")
            val indexOfLeastRecentlyTone = notes.indexOfFirst { it.toneIndex == leastRecentlyToneIndex || !it.isEnabled}
            require(indexOfLeastRecentlyTone >= 0)
            notes[indexOfLeastRecentlyTone].setNewTone(toneIndex, toneIndexToLabel?.let { it(toneIndex) } ?: "")
            for (i in 0 until leastRecentlyUsedTones.size - 1)
                leastRecentlyUsedTones[i] = leastRecentlyUsedTones[i + 1]
        }

        leastRecentlyUsedTones[leastRecentlyUsedTones.size - 1] = toneIndex
        notes.forEach { if (it.toneIndex != toneIndex) it.hit(-2)}
        invalidate()
    }

    private fun computeAspectRatioMinMax() {
        val toneIndexToLabelLocal = toneIndexToLabel ?: return

        aspectRatioMin = Float.MAX_VALUE
        aspectRatioMax = 0f
        labelPaint.textSize = 10f
        for (toneIndex in toneIndexBegin until toneIndexEnd) {
            val label = toneIndexToLabelLocal(toneIndex)
            val desiredWidth = ceil(StaticLayout.getDesiredWidth(label, labelPaint)).toInt()
            val layout = StaticLayout.Builder.obtain(label, 0, label.length, labelPaint, desiredWidth).build()
            if (layout.height > 0f) {
                // topPadding is a negative number, thats why we add it, ...
                val aspect = layout.width.toFloat() / (layout.height.toFloat() + layout.topPadding - layout.bottomPadding)
                aspectRatioMin = min(aspect, aspectRatioMin)
                aspectRatioMax = max(aspect, aspectRatioMax)
            }
        }
    }

    private fun computePaintTextSize(w: Int, h: Int, aspectMax: Float): Float {
        val effectiveLabelWidth = (w - paddingLeft - paddingRight
                - (notes.size + 1) * minimumLabelSpace
                - 2 * boxPaint.strokeWidth )
//        val effectiveLabelHeight = (h - paddingBottom - paddingTop
//                - 2 * (boxPaint.strokeWidth + notePadding))
        val effectiveLabelHeight = computeBoxContentBottom(h) - computeBoxContentTop()
        if (effectiveLabelHeight <= 0f || effectiveLabelWidth <= 0f)
            return 0f

        val heightFromAspectMax = effectiveLabelWidth / aspectMax
        val maximumAllowedHeight = min(effectiveLabelHeight, heightFromAspectMax)

        labelPaint.textSize = maximumAllowedHeight
        labelPaint.getFontMetrics(fontMetrics)
        return floor(0.8f * maximumAllowedHeight * maximumAllowedHeight / (fontMetrics.bottom - fontMetrics.top))
    }

    private fun xPositionToToneIndex(x: Float): Int {
        val horizontalSpacePerNote = computeHorizontalSpacePerNote()
        // x  = paddingLeft + boxStrokeWidth +  0.5f * minimumLabelSpace + (0.5f + index) * horizontalSpacePerNote
        val index = ((x - paddingLeft - boxPaint.strokeWidth - 0.5f * minimumLabelSpace) / horizontalSpacePerNote - 0.5f).roundToInt()
        if (index in notes.indices && notes[index].isEnabled) {
            return notes[index].toneIndex
        }
        return NO_TONE_INDEX
    }

    private fun computeBoxTop(): Float {
        val titleHeight = titleLayout?.height?.toFloat() ?: 0f
        return paddingTop + max(0.5f * titleHeight, 0.5f * boxPaint.strokeWidth)
    }

    private fun computeBoxBottom(h: Int): Float {
        return h - paddingBottom - 0.5f * boxPaint.strokeWidth
    }

    private fun computeBoxContentTop(): Float {
        val titleHeight = titleLayout?.height?.toFloat() ?: 0f
        return computeBoxTop() + max(0.5f * titleHeight, 0.5f * boxPaint.strokeWidth) + notePadding
    }

    private fun computeBoxContentBottom(h: Int): Float {
        return computeBoxBottom(h) - 0.5f * boxPaint.strokeWidth - notePadding
    }

    private fun computeHorizontalSpacePerNote(): Float {
        return (width - paddingLeft - paddingRight - 2 * boxPaint.strokeWidth - minimumLabelSpace) / notes.size
    }

    private fun computeNumNotes(w: Float, h: Float): Int {
        val minimumWidthPerNote = minimumAspectPerNote * h
        return if (minimumWidthPerNote == 0f)
            1
        else
            floor(w / minimumWidthPerNote).toInt()
    }
}