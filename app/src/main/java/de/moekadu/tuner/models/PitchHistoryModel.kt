package de.moekadu.tuner.models

import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.notedetection.TuningState
import de.moekadu.tuner.notedetection.TuningTarget
import de.moekadu.tuner.notedetection.checkTuning
import de.moekadu.tuner.temperaments.*
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private fun ratioToCents(ratio: Float): Float {
    return (1200.0 * log(ratio.toDouble(), 2.0)).toFloat()
}

private fun centsToRatio(cents: Float): Float {
    return (2.0.pow(cents / 1200.0)).toFloat()
}

class PitchHistoryModel {
    var changeId = 0
        private set

    var numHistoryValues = 0
        private set
    var maxNumHistoryValues = 0
        private set

    var historyValues = FloatArray(50)
        private set
    var historyValuesChangeId = 0
        private set
    val currentFrequency get() = if (numHistoryValues == 0) 0f else historyValues[numHistoryValues - 1]

    private var cachedHistoryValues = FloatArray(50)
    private var numCachedHistoryValues = 0

    var yRangeAuto = FloatArray(2)
    var yRangeChangeId = 0
        private set

    private var tuningState = TuningState.Unknown

    var targetNote: MusicalNote? = null
        private set
    var targetNoteFrequency = 0f
        private set
    var targetNoteChangeId = 0
        private set

    var notePrintOptions = MusicalNotePrintOptions.None
        private set
    var notePrintOptionsChangeId = 0
        private set

    /** Tolerance in cents. */
    var toleranceInCents = 0
        private set
    /** Lower tolerance frequency value. */
    var lowerToleranceFrequency = 0f
        private set
    /** Upper tolerance frequency value. */
    var upperToleranceFrequency = 0f
        private set
    var toleranceChangeId = 0
        private set

    /** Scale is needed for ticks. */
    var musicalScale = MusicalScaleFactory.create(DefaultValues.TEMPERAMENT)
        private set
    var musicalScaleFrequencies = computeMusicalScaleFrequencies(musicalScale)
        private set
    var musicalScaleChangeId = 0
        private set

    /** Define if currently we are detecting notes or not. */
    var isCurrentlyDetectingNotes = true
        private set

    var useExtraPadding = false
        private set

    var historyLineStyle = HISTORY_LINE_STYLE_ACTIVE
        private set
    var currentFrequencyPointStyle = CURRENT_FREQUENCY_POINT_STYLE_INTUNE
        private set
    var tuningDirectionPointStyle = TUNING_DIRECTION_STYLE_TOOLOW_ACTIVE
        private set
    var tuningDirectionPointRelativeOffset = TUNING_DIRECTION_OFFSET_TOOLOW
        private set
    var tuningDirectionPointVisible = false
        private set
    var targetNoteMarkStyle = TARGET_NOTE_MARK_STYLE_INTUNE
        private set

    fun changeSettings(
        musicalScale: MusicalScale? = null,
        tuningTarget: TuningTarget? = null,
        maxNumHistoryValues: Int = -1,
        toleranceInCents: Int = -1,
        notePrintOptions: MusicalNotePrintOptions? = null,
        isCurrentlyDetectingNotes: Boolean = this.isCurrentlyDetectingNotes
    ) {
        changeId++
        var recomputeYRange = false
        var recomputeToleranceBounds = false
        var recomputeTuning = false

        if (musicalScale != null) {
            this.musicalScale = musicalScale
            musicalScaleFrequencies = computeMusicalScaleFrequencies(musicalScale)
            recomputeYRange = true
            musicalScaleChangeId = changeId
        }

        if (notePrintOptions != null) {
            this.notePrintOptions = notePrintOptions
            notePrintOptionsChangeId = changeId
            useExtraPadding = (notePrintOptions.notation == Notation.solfege)
        }

        // handle tuning target
        if (tuningTarget != null) {
//            Log.v("Tuner", "PitchHistoryModel.changeSettings: tuningTarget=$tuningTarget")
            targetNote = if (tuningTarget.isPartOfInstrument) tuningTarget.note else null
            targetNoteFrequency = tuningTarget.frequency
            recomputeYRange = true
            recomputeToleranceBounds = true
            recomputeTuning = true
            targetNoteChangeId = changeId
        }

        // reset pitch history array
        if (maxNumHistoryValues >= 0 && maxNumHistoryValues != this.maxNumHistoryValues) {
//            Log.v("Tuner", "PitchHistoryModel.changeSettings: numHistoryValues=$maxNumHistoryValues")
            moveCachedValuesToHistory()
            cachedHistoryValues = FloatArray(maxNumHistoryValues)

            this.maxNumHistoryValues = maxNumHistoryValues
            val oldNumHistoryValues = numHistoryValues
            val oldHistoryValues = historyValues
            historyValues = FloatArray(maxNumHistoryValues)
            val startIndex = max(0, oldNumHistoryValues - maxNumHistoryValues)
            oldHistoryValues.copyInto(historyValues, 0, startIndex, oldNumHistoryValues)
            numHistoryValues = oldNumHistoryValues - startIndex
            recomputeTuning = true
            recomputeYRange = true
            historyValuesChangeId = changeId
        }

        // set toleranceInCents
        if (toleranceInCents >= 0 && toleranceInCents != this.toleranceInCents) {
//            Log.v("Tuner", "PitchHistoryModel.changeSettings: toleranceInCents=$toleranceInCents")
            this.toleranceInCents = toleranceInCents
            recomputeToleranceBounds = true
            recomputeTuning = true
        }

//        // set isCurrentlyDetectingNotes
//        if (isCurrentlyDetectingNotes != null && isCurrentlyDetectingNotes != this.isCurrentlyDetectingNotes) {
        this.isCurrentlyDetectingNotes = isCurrentlyDetectingNotes
//            isCurrentlyDetectingNotesChangeId = changeId
//        }

        if (recomputeToleranceBounds) {
            val ratio = centsToRatio(this.toleranceInCents.toFloat())
//            Log.v("Tuner", "PitchHistoryModel.changeSettings: toleranceRatio = $ratio, toleranceInCents=$this.toleranceInCents, this=$this")
            if (targetNote == null || targetNoteFrequency <= 0f) {
                lowerToleranceFrequency = -1f
                upperToleranceFrequency = -1f
            } else {
                lowerToleranceFrequency = targetNoteFrequency / ratio
                upperToleranceFrequency = targetNoteFrequency * ratio
            }
//            Log.v("Tuner", "PitchHistoryModel.changeSettings: tolerance $lowerToleranceFrequency -- $upperToleranceFrequency, current target = $targetNoteFrequency")
            toleranceChangeId = changeId
        }

        if (recomputeYRange) {
            val frequency = if (numHistoryValues > 0)
                historyValues[numHistoryValues - 1]
            else if (targetNoteFrequency > 0f)
                targetNoteFrequency
            else
                this.musicalScale.referenceFrequency
            computeYRange(frequency, targetNote, this.musicalScale)
        }

        if (recomputeTuning) {
            tuningState = checkTuning(
                if (numHistoryValues > 0) historyValues[numHistoryValues - 1] else -1f,
                targetNoteFrequency,
                this.toleranceInCents.toFloat()
            )
//            Log.v("Tuner", "PitchHistoryModel.changeSettings: recomputing tuningState=$tuningState")
        }

        setStyles(tuningState, this.isCurrentlyDetectingNotes)
    }

    private fun addFrequencyToCache(frequency: Float) {
        if (numCachedHistoryValues == cachedHistoryValues.size) {
            cachedHistoryValues.copyInto(cachedHistoryValues, 0, 1, numHistoryValues)
            --numCachedHistoryValues
        }
        cachedHistoryValues[numCachedHistoryValues] = frequency
        ++numCachedHistoryValues
    }

    private fun moveCachedValuesToHistory() {
        val numFree = historyValues.size - numHistoryValues
        val numDelete = max(0, numCachedHistoryValues - numFree)
        if (numDelete > 0) {
            historyValues.copyInto(historyValues, 0, numDelete, numHistoryValues)
            numHistoryValues -= numDelete
        }
        cachedHistoryValues.copyInto(historyValues, numHistoryValues, 0, numCachedHistoryValues)
        numHistoryValues += numCachedHistoryValues
        numCachedHistoryValues = 0
    }

    /** Add a new frequency
     * @param frequency Frequency to ass
     * @param cacheOnly Just store the frequency, but don't update the model. This can be used
     *   to suppress too frequent view updates.
     */
    fun addFrequency(frequency: Float, cacheOnly: Boolean) {
        addFrequencyToCache(frequency)

        if (cacheOnly)
            return

        changeId++

        moveCachedValuesToHistory()
//        if (numHistoryValues == historyValues.size) {
//            for (i in 1 until numHistoryValues)
//                historyValues[i - 1] = historyValues[i]
//            --numHistoryValues
//        }
//        historyValues[numHistoryValues] = frequency
//        ++numHistoryValues

        tuningState = checkTuning(
            frequency,
            targetNoteFrequency,
            toleranceInCents.toFloat()
        )
        computeYRange(frequency, targetNote, musicalScale)
        setStyles(tuningState, isCurrentlyDetectingNotes)

        historyValuesChangeId = changeId
    }

    private fun computeYRange(
        currentFrequency: Float,
        targetNote: MusicalNote?,
        musicalScale: MusicalScale) {

        val targetNoteIndex = if (targetNote == null) Int.MAX_VALUE else musicalScale.getNoteIndex(targetNote)
        val closestNoteIndex = musicalScale.getClosestNoteIndex(currentFrequency)

        val minIndex = if (targetNoteIndex == Int.MAX_VALUE) {
            closestNoteIndex - YRANGE_IN_NOTE_INDICES_AT_TARGET_NOTE // not intuitive that we use here the TARGET NOTE, but it looks better to have a slightly bigger range in this rare case
        } else {
            min(
                closestNoteIndex - YRANGE_IN_NOTE_INDICES_AT_CLOSEST_NOTE,
                targetNoteIndex - YRANGE_IN_NOTE_INDICES_AT_TARGET_NOTE
            )
        }

        val maxIndex =  if (targetNoteIndex == Int.MAX_VALUE) {
            closestNoteIndex + YRANGE_IN_NOTE_INDICES_AT_TARGET_NOTE // not intuitive that we use here the TARGET NOTE, but it looks better to have a slightly bigger range in this rare case
        } else {
            max(
                closestNoteIndex + YRANGE_IN_NOTE_INDICES_AT_CLOSEST_NOTE,
                targetNoteIndex + YRANGE_IN_NOTE_INDICES_AT_TARGET_NOTE
            )
        }

        val yRangeMin = musicalScale.getNoteFrequency(minIndex)
        val yRangeMax = musicalScale.getNoteFrequency(maxIndex)
        if (yRangeMin != yRangeAuto[0] || yRangeMax != yRangeAuto[1]) {
            yRangeAuto[0] = yRangeMin
            yRangeAuto[1] = yRangeMax
            yRangeChangeId = changeId
        }
//        Log.v("Tuner", "PitchHistoryModel.computeYRange: ${yRangeAuto[0]} -- ${yRangeAuto[1]}, targetNoteIndex=$targetNoteIndex, targetNote=$targetNote, closestNoteIndex=$closestNoteIndex")
    }

    private fun setStyles(tuningState: TuningState = this.tuningState, isActive: Boolean = isCurrentlyDetectingNotes) {
//        Log.v("Tuner", "PitchHistoryModel.setStyles: tuningState=$tuningState")
        historyLineStyle = if (isActive) HISTORY_LINE_STYLE_ACTIVE else HISTORY_LINE_STYLE_INACTIVE
        currentFrequencyPointStyle = if (!isActive)
            CURRENT_FREQUENCY_POINT_STYLE_INACTIVE
        else if (tuningState == TuningState.InTune)
            CURRENT_FREQUENCY_POINT_STYLE_INTUNE
        else
            CURRENT_FREQUENCY_POINT_STYLE_OUTOFTUNE
        tuningDirectionPointStyle = if (isActive && tuningState == TuningState.TooHigh)
            TUNING_DIRECTION_STYLE_TOOHIGH_ACTIVE
        else if (!isActive && tuningState == TuningState.TooHigh)
            TUNING_DIRECTION_STYLE_TOOHIGH_INACTIVE
        else if (isActive && tuningState == TuningState.TooLow)
            TUNING_DIRECTION_STYLE_TOOLOW_ACTIVE
        else if (!isActive && tuningState == TuningState.TooLow)
            TUNING_DIRECTION_STYLE_TOOLOW_INACTIVE
        else
            0

        tuningDirectionPointRelativeOffset = if (tuningState == TuningState.TooHigh)
            TUNING_DIRECTION_OFFSET_TOOHIGH
        else
            TUNING_DIRECTION_OFFSET_TOOLOW
        tuningDirectionPointVisible = tuningState != TuningState.InTune && tuningState != TuningState.Unknown
        targetNoteMarkStyle = if (tuningState == TuningState.InTune)
            TARGET_NOTE_MARK_STYLE_INTUNE
        else
            TARGET_NOTE_MARK_STYLE_OUTOFTUNE
    }

    private fun computeMusicalScaleFrequencies(musicalScale: MusicalScale): FloatArray {
        val numNotes = musicalScale.noteIndexEnd - musicalScale.noteIndexBegin
        return FloatArray(numNotes) {
            musicalScale.getNoteFrequency(musicalScale.noteIndexBegin + it)
        }
    }

    companion object {
        const val YRANGE_IN_NOTE_INDICES_AT_TARGET_NOTE = 1.55f
        const val YRANGE_IN_NOTE_INDICES_AT_CLOSEST_NOTE = 0.55f

        const val HISTORY_LINE_TAG = 1L
        const val CURRENT_FREQUENCY_POINT_TAG = 2L
        const val TUNING_DIRECTION_POINT_TAG = 3L
        const val TARGET_NOTE_MARK_TAG = 4L
        const val TOLERANCE_MARK_TAG = 5L

        const val HISTORY_LINE_STYLE_ACTIVE = 0
        const val HISTORY_LINE_STYLE_INACTIVE = 1

        const val CURRENT_FREQUENCY_POINT_STYLE_INTUNE = 0
        const val CURRENT_FREQUENCY_POINT_STYLE_OUTOFTUNE = 2
        const val CURRENT_FREQUENCY_POINT_STYLE_INACTIVE = 1

        const val TUNING_DIRECTION_STYLE_TOOLOW_ACTIVE = 4
        const val TUNING_DIRECTION_STYLE_TOOHIGH_ACTIVE = 3
        const val TUNING_DIRECTION_STYLE_TOOLOW_INACTIVE = 6
        const val TUNING_DIRECTION_STYLE_TOOHIGH_INACTIVE = 5

        const val TUNING_DIRECTION_OFFSET_TOOLOW = -1.5f
        const val TUNING_DIRECTION_OFFSET_TOOHIGH = 1.5f

        const val TARGET_NOTE_MARK_STYLE_INTUNE = 0
        const val TARGET_NOTE_MARK_STYLE_OUTOFTUNE = 2

        const val TOLERANCE_STYLE = 1
    }
}