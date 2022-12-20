package de.moekadu.tuner.models

import de.moekadu.tuner.notedetection.AutoCorrelation
import de.moekadu.tuner.temperaments.MusicalNotePrintOptions

class CorrelationPlotModel {
    var changeId = 0
        private set

    // the following values are changed with noteDetectionChangeId
    var timeShifts = FloatArray(0)
        private set
    var correlationValues = FloatArray(0)
        private set
    var detectedFrequency = -1f
        private set
    var noteDetectionChangeId = changeId
        private set

    // the following values are changed with targetChangeId
    var targetFrequency = -1f
        private set
    var timeShiftRange = FloatArray(2) { 0f }
        private set
    var targetChangeId = changeId
        private set

    // this is independent on change id
    var useExtraPadding = false
        private set

    fun changeSettings(
        autoCorrelation: AutoCorrelation? = null,
        detectedFrequency: Float = -1f,
        targetFrequency: Float = -1f,
        notePrintOptions: MusicalNotePrintOptions? = null
    ) {
        ++changeId
        autoCorrelation?.let {
            if (timeShifts.size != it.size) {
                timeShifts = FloatArray(it.size)
                correlationValues = FloatArray(it.size)
            }
            it.times.copyInto(timeShifts)
            it.values.copyInto(correlationValues)
            noteDetectionChangeId = changeId
        }

        if (detectedFrequency > 0f) {
            this.detectedFrequency = detectedFrequency
            noteDetectionChangeId = changeId
        }

        if (targetFrequency > 0f) {
            this.targetFrequency = targetFrequency
            timeShiftRange[1] = 3.0f / targetFrequency
            targetChangeId = changeId
        }

        notePrintOptions?.let {
            useExtraPadding = notePrintOptions.isSolfege
        }

    }
}