package de.moekadu.tuner.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class DetectedNoteViewer(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    : View(context, attrs, defStyleAttr) {

    fun interface NoteClickedListener {
        fun onNoteClicked(note: MusicalNote)
    }

    private class LabelSetSize(var width: Float, var height: Float)

    var noteClickedListener: NoteClickedListener? = null

    class DetectedNote(private var hitCountMin: Int, private var hitCountMax: Int, private val printOption: MusicalNotePrintOptions) {
        var note = MusicalNote(BaseNote.A, NoteModifier.None, 4)
            private set
        var isEnabled = false
            private set

        private var label: MusicalNoteLabel? = null

        private var hitCount = hitCountMax

        private var textSize = -1f
        private var textStyle = -1
        private var textColor = Color.BLACK

        fun setNewNote(note: MusicalNote) {
            this.note = note
            this.label = null
            hitCount = hitCountMax
            isEnabled = true
        }
//        fun clear() {
//            isEnabled = false
//        }

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

        fun drawToCanvas(canvas: Canvas, x: Float, y: Float, labelPaint: TextPaint, noteNamePrinter: NoteNamePrinter) {
            if (!isEnabled)
                return
            val newTextStyle = labelPaint.typeface?.style ?: -1
            if (labelPaint.textSize != textSize || newTextStyle != textStyle || labelPaint.color != textColor) {
                label = null
//                Log.v("Tuner", "DetectedNoteViewer.DetectedNote.drawToCanvas: create label for note with toneIndex $toneIndex")
                textSize = labelPaint.textSize
                textStyle = newTextStyle
                textColor = labelPaint.color
            }

            if (label == null) {
                label = MusicalNoteLabel(note, labelPaint, noteNamePrinter, printOptions = printOption)
            }

            label?.drawToCanvas(x, y, LabelAnchor.Center, canvas)
        }

        fun getTextSizeInPercentOfMax(): Float {
            return hitCount.toFloat() / hitCountMax.toFloat()
        }

    }

    /** Class for measuring and printing notes. */
    private val noteNamePrinter = NoteNamePrinter(context)

    /** Paint for drawing the notes. */
    private val labelPaint = TextPaint().apply {
        isAntiAlias = true
    }

    /** Maximum text size of labels. */
    private var maximumTextSize = 0f

    /** Note name scale of the notes which can be hit (for measuring the max label size). */
    private var noteNameScale: NoteNameScale? = null

    /** Options for printing the note labels (prefer flat/sharp). */
    private var notePrintOptions = MusicalNotePrintOptions.None

    /** First possible note index. */
    private var noteIndexBegin = 0
    /** End note index (excluded). */
    private var noteIndexEnd = 0

    /** Notes for the available spaces of this viewer. */
    private var notes = Array(1) { DetectedNote((hitCountMax * ratioMinSizeToMaxSize).roundToInt(), hitCountMax, notePrintOptions) }
    /** Array of notes which were recently hit in the according order (least hit note is the last one). */
    private var leastRecentlyUsedNotes = Array<MusicalNote?>(1){ null }

    /** Minimum space between two neighboring notes. */
    private var minimumLabelSpace = 0f

    /** Note which is currently pressed or null if now note ist pressed. */
    private var clickedNote: MusicalNote? = null

    /** Title label. */
    private var title: StringLabel? = null
    /** Paint for drawing the title. */
    private val titlePaint = TextPaint().apply {
        isAntiAlias = true
        textSize = 10f
        color = Color.BLACK
    }

    /** Paint for drawing the background of the title.
     * This would normally use the background color.
     */
    private val titleBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    /** Left and right padding around the title. */
    private var titlePadding = 4f

    /** Text size of the notes or 0f if it should be determined such that it fits into the box. */
    private var textSize = 0f
    /** Corner radius of the surrounding rectangle. */
    private var boxCornerRadius = 0f
    private var notePadding = 4f // minimum space between note top/bottom and title (or top line if title does not exist) and bottom line
    /** Paint for drawing the surrounding rectangle. */
    private val boxPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 1f
        color = Color.BLACK
        style = Paint.Style.STROKE
    }

    private var hitCountMax = 100
    private val ratioMinSizeToMaxSize = 0.5f
    private val durationToGrowToneSizeInSeconds = 1.0f

    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs,
        R.attr.detectedNoteViewerStyle
    )

    init {
        var titleString: String? = null
        // var numNotes = 4
        attrs?.let {
            val ta = context.obtainStyledAttributes(
                attrs,
                R.styleable.DetectedNoteViewer,
                defStyleAttr,
                R.style.DetectedNoteViewerStyle
            )

            labelPaint.color = ta.getColor(R.styleable.DetectedNoteViewer_labelTextColor, Color.BLACK)
            minimumLabelSpace = ta.getDimension(R.styleable.DetectedNoteViewer_minimumLabelSpace, minimumLabelSpace)

            titleString = ta.getString(R.styleable.DetectedNoteViewer_title)
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
            textSize = ta.getDimension(R.styleable.DetectedNoteViewer_textSize, textSize)
            ta.recycle()
        }

        titleString?.let {
            title = StringLabel(it, titlePaint, titleBackgroundPaint, paddingLeft = titlePadding, paddingRight = titlePadding)
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
            val titleHeight = title?.labelHeight ?: 0f
//            Log.v("StaticLayoutTest", "DetectedNoteViewer.onMeasure: maxLabelHeight = ${maxSize.height}")
            if (heightMode != MeasureSpec.EXACTLY) {
//                Log.v("StaticLayoutTest", "NoteSelector.onMeasure: setting proposed height to $proposedHeight")
                proposedHeight =
                    (maxSize.height + paddingBottom + paddingTop
                            + 2 * notePadding + boxPaint.strokeWidth
                            + max(boxPaint.strokeWidth, titleHeight)).roundToInt()
            }
            if (widthMode != MeasureSpec.EXACTLY)
                proposedWidth = ceil(4 * (maxSize.width + minimumLabelSpace + 2 * boxPaint.strokeWidth) + 2 * notePadding + notePadding + paddingLeft + paddingRight - minimumLabelSpace).toInt()
        }
//        Log.v("StaticLayoutTest", "NoteSelector.onMeasure: proposedHeight=$proposedHeight")
        val h = resolveSize(proposedHeight, heightMeasureSpec)
        val w = resolveSize(proposedWidth, widthMeasureSpec)
        setMeasuredDimension(w, h)
    }

    /** Compute maximum size of labels for use during onMeasure.
     * If no noteNameScale is set, this will be set to an arbitrary value.
     * @return Class with maximum width and height of the labels.
     */
    private fun getMaximumLabelSize(): LabelSetSize {
        val noteNameScaleLocal = noteNameScale ?: return LabelSetSize(60f, 30f)
        val octaveBegin = noteNameScaleLocal.getNoteOfIndex(noteIndexBegin).octave
        val octaveEnd = noteNameScaleLocal.getNoteOfIndex(noteIndexEnd - 1).octave + 1

        labelPaint.textSize = if (textSize == 0f) 30f else textSize
            val measures = MusicalNoteLabel.getLabelSetBounds(
                noteNameScaleLocal.notes,
                octaveBegin,
                octaveEnd,
                labelPaint,
                noteNamePrinter,
                notePrintOptions,
                enableOctaveIndex = true
            )

        return LabelSetSize(measures.maxWidth, measures.maxHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val numNotes = setMaximumTextSizeAndReturnNumNotes(w, h)

        if (notes.size != numNotes) {
            notes = Array(numNotes) { DetectedNote((hitCountMax * ratioMinSizeToMaxSize).roundToInt(), hitCountMax, notePrintOptions) }
            leastRecentlyUsedNotes = Array(numNotes) { null }
        }

        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas == null)
            return

        val yPosCenter = 0.5f * (computeBoxContentTop() + computeBoxContentBottom(height))
        val horizontalSpacePerNote = computeHorizontalSpacePerNote()
        notes.forEachIndexed { index, note ->
            if (note.isEnabled) {
                val xPosCenter = paddingLeft + boxPaint.strokeWidth + notePadding - 0.5f * minimumLabelSpace + (0.5f + index) * (horizontalSpacePerNote + minimumLabelSpace)
                var textHeightPercent = note.getTextSizeInPercentOfMax()
                if (note.note == clickedNote)
                    textHeightPercent = min(1f, textHeightPercent + 0.2f)

                labelPaint.textSize = textHeightPercent * maximumTextSize
//                Log.v("Tuner", "DetectedNoteViewer.onDraw: drawing note ${note.toneIndex}, clicked=$clickedToneIndex, x=$xPosCenter, y=$yPosCenter, w=$width, h=$height, maxText=$maximumTextSize")
                labelPaint.typeface = if (note.note == clickedNote) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
//                Log.v("StaticLayoutTest", "DetectedNoteViewer.onDraw: drawing note to $xPosCenter, $yPosCenter")
                note.drawToCanvas(canvas, xPosCenter, yPosCenter, labelPaint, noteNamePrinter)
            }
        }

        canvas.drawRoundRect(
            paddingLeft + 0.5f * boxPaint.strokeWidth, computeBoxTop(),
        width - paddingRight - 0.5f * boxPaint.strokeWidth, computeBoxBottom(height),
            boxCornerRadius, boxCornerRadius, boxPaint)
        title?.drawToCanvasWithPaddedBackground(
            paddingLeft + 0.5f * boxPaint.strokeWidth + 3 * boxCornerRadius,
            computeBoxTop(),
            LabelAnchor.West,
            canvas
        )
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null)
            return super.onTouchEvent(event)
        val action = event.actionMasked
//        Log.v("Tuner", "DetectedNoteViewer.onTouchEvent: action=$action")
        val note = xPositionToNote(event.x)
        val clickedNoteOld = clickedNote

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                clickedNote = note
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                clickedNote = null
//                Log.v("Tuner", "DetectedNoteViewer.onTouchEvent: ACTION_UP, toneIndex= $toneIndex, x=${event.x}")
                if (note != null) {
                    if (isSoundEffectsEnabled)
                        playSoundEffect(android.view.SoundEffectConstants.CLICK)
                    performClick()
                    noteClickedListener?.onNoteClicked(note)
                }
            }
        }

//        Log.v("Tuner", "DetectedNoteViewer.onTouchEvent: clickedToneIndex= $clickedToneIndex")
        if (clickedNote != clickedNoteOld)
            invalidate()

        super.onTouchEvent(event)
        return true
    }

    /** Set the possible notes.
     * This is needed to compute the maximum extents of the note labels.
     * @param noteNameScale Note name scale of notes which can be shown.
     * @param noteIndexBegin Index of first note.
     * @param noteIndexEnd End index of note (excluded).
     * @param notePrintOptions Options for printing the note labels (prefer flat/sharp)
     */
    fun setNotes(noteNameScale: NoteNameScale, noteIndexBegin: Int, noteIndexEnd: Int, notePrintOptions: MusicalNotePrintOptions) {
        this.noteNameScale = noteNameScale
        this.noteIndexBegin = noteIndexBegin
        this.noteIndexEnd = noteIndexEnd
        this.notePrintOptions = notePrintOptions

        if (isLaidOut) { // if it is not laid out, all the following functions will be called in onSizeChanged
            val numNotes = setMaximumTextSizeAndReturnNumNotes(width, height)
            notes = Array(numNotes) { DetectedNote((hitCountMax * ratioMinSizeToMaxSize).roundToInt(), hitCountMax, notePrintOptions) }
            leastRecentlyUsedNotes = Array(numNotes) { null }
        }
        requestLayout()
    }

    /** Set approximate update interval with which you are going to call hitNote()
     * @param durationInSeconds Update duration in seconds
     */
    fun setApproximateHitNoteUpdateInterval(durationInSeconds: Float) {
        hitCountMax = if (durationInSeconds == 0f)
            50
        else
            (durationToGrowToneSizeInSeconds / (durationInSeconds * ratioMinSizeToMaxSize)).roundToInt()

        val hitCountMin = (hitCountMax * ratioMinSizeToMaxSize).roundToInt()

        for (n in notes)
            n.setHitCountRange(hitCountMin, hitCountMax)
        invalidate()
    }

    /** Tell that a note was hit.
     * This will either show the note in the viewer or increase size. Also the size of the
     * other notes will be decreased.
     * @param note Note which was hit.
     */
    fun hitNote(note: MusicalNote?) {
        if (notes.isEmpty() || note == null)
            return

        val index = notes.indexOfFirst { it.note == note && it.isEnabled}

        if (index >= 0) {
            val lruIndex = leastRecentlyUsedNotes.indexOfFirst { it == note }
            require(lruIndex >= 0)
            for (i in lruIndex until leastRecentlyUsedNotes.size - 1)
                leastRecentlyUsedNotes[i] = leastRecentlyUsedNotes[i + 1]
            notes[index].hit(2)
        } else {
            val leastRecentlyNote = leastRecentlyUsedNotes[0]
//            Log.v("Tuner", "DetectedNoteViewer.hitTone: Creating new label label=${toneIndexToLabel?.let { it(toneIndex) }}")
            val indexOfLeastRecentlyNote = notes.indexOfFirst { it.note == leastRecentlyNote || !it.isEnabled}
            require(indexOfLeastRecentlyNote >= 0)
            notes[indexOfLeastRecentlyNote].setNewNote(note)
            for (i in 0 until leastRecentlyUsedNotes.size - 1)
                leastRecentlyUsedNotes[i] = leastRecentlyUsedNotes[i + 1]
        }

        leastRecentlyUsedNotes[leastRecentlyUsedNotes.size - 1] = note
        notes.forEach { if (it.note != note) it.hit(-2)}
//        Log.v("StaticLayoutText", "DetectedNoteViewer.hitNote")
        invalidate()
    }

    /** With the x-position within the view given, this tells the underlying note.
     * @param x x-position inside the view.
     * @return Underlying musical note or null if no note available.
     */
    private fun xPositionToNote(x: Float): MusicalNote? {
        val horizontalSpacePerNote = computeHorizontalSpacePerNote()
        val index = ((x - paddingLeft - notePadding - boxPaint.strokeWidth - 0.5f * minimumLabelSpace) / (horizontalSpacePerNote + minimumLabelSpace) - 0.5f).roundToInt()
        if (index in notes.indices && notes[index].isEnabled) {
            return notes[index].note
        }
        return null
    }

    /** Compute top of surrounding rectangle.
     * The top is defined at the center of stroke.
     * @return Box top.
     */
    private fun computeBoxTop(): Float {
        val titleHeight = title?.labelHeight ?: 0f
        return paddingTop + max(0.5f * titleHeight,0.5f * boxPaint.strokeWidth)
    }

    /** Compute bottom of surrounding rectangle.
     * The bottom is defined at the center of stroke.
     * @param h Total height of the view.
     * @return Box bottom.
     */
    private fun computeBoxBottom(h: Int): Float {
        return h - paddingBottom - 0.5f * boxPaint.strokeWidth
    }

    /** Compute the top of the content (note labels) inside the box.
     * This takes into account the box stroke width, the note padding and the title size
     * if it exists.
     * @return Top of box content.
     */
    private fun computeBoxContentTop(): Float {
        val titleHeight = title?.labelHeight ?: 0f
        return computeBoxTop() + max(0.5f * titleHeight, 0.5f * boxPaint.strokeWidth) + notePadding
    }

    /** Compute the bottom of the content (note labels) inside the box.
     * This takes into account the box stroke width and the note padding.
     * @return Bottom of box content.
     */
    private fun computeBoxContentBottom(h: Int): Float {
        return computeBoxBottom(h) - 0.5f * boxPaint.strokeWidth - notePadding
    }

    /** Compute the width of each rectangle where a note can be printed.
     *  This is the total note space within the box divided by the number of notes.
     *  @return Width of note rectangle.
     */
    private fun computeHorizontalSpacePerNote(): Float {
        return (width - paddingLeft - paddingRight - 2 * (boxPaint.strokeWidth + notePadding) + minimumLabelSpace) / notes.size - minimumLabelSpace
    }

    /** Compute and set the maximumTextSize and return the number of notes.
     * If the input textSize is not 0f, this will be the maximumTextSize, otherwise we
     * compute the text size based on the available space.
     * @note The maximumTextSize will be set within this function.
     * @param w Total height of the view.
     * @param h Total width of the view.
     * @return Number of notes
     */
    private fun setMaximumTextSizeAndReturnNumNotes(w: Int, h: Int): Int {

        val allowedMaxHeight = computeBoxContentBottom(h) - computeBoxContentTop()

        val noteNameScaleLocal = noteNameScale
        if (noteNameScaleLocal == null) {
            maximumTextSize = allowedMaxHeight
            return 1
        }

        val octaveBegin = noteNameScaleLocal.getNoteOfIndex(noteIndexBegin).octave
        val octaveEnd = noteNameScaleLocal.getNoteOfIndex(noteIndexEnd - 1).octave + 1

        labelPaint.textSize = if (textSize == 0f) allowedMaxHeight else textSize

        val measures = MusicalNoteLabel.getLabelSetBounds(
            noteNameScaleLocal.notes,
            octaveBegin,
            octaveEnd,
            labelPaint,
            noteNamePrinter,
        )

        val maximumLabelWidth: Float
        if (textSize == 0f) {
            val fontScaling = allowedMaxHeight / measures.maxHeight
            maximumTextSize = fontScaling * allowedMaxHeight
            maximumLabelWidth = fontScaling * measures.maxWidth
        } else {
            maximumTextSize = textSize
            maximumLabelWidth = measures.maxWidth
//            Log.v("StaticLayoutTest", "DetectedNoteViewer.setMaximumTextSizeAndReturnNumNotes: maxLabelHeight = ${measures.maxHeight}")
        }
        val numNotes = ((w - paddingLeft - paddingRight - 2 * (labelPaint.strokeWidth + notePadding) + minimumLabelSpace) / (maximumLabelWidth + minimumLabelSpace)).toInt()
//        Log.v("StaticLayoutTest", "DetectedNoteViewer.setMaximumTextSizeAndReturnNumNotes: maximumTextSize = $maximumTextSize, numNotes = $numNotes")
        return numNotes
    }
}
