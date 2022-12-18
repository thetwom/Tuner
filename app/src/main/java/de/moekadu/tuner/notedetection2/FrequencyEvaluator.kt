package de.moekadu.tuner.notedetection2

import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale

data class FrequencyEvaluationResult(
    val smoothedFrequency: Float,
    val target: TuningTarget?,
    val timeSinceThereIsNoFrequencyDetectionResult: Float
)

class FrequencyEvaluator(
    numMovingAverage: Int,
    toleranceInCents: Float,
    private val maxNoise: Float,
    musicalScale: MusicalScale,
    instrument: Instrument
) {
    private val smoother = OutlierRemovingSmoother(
        numMovingAverage,
        DefaultValues.FREQUENCY_MIN,
        DefaultValues.FREQUENCY_MAX,
        relativeDeviationToBeAnOutlier = 0.1f,
        maxNumSuccessiveOutliers = 2,
        minNumValuesForValidMean = 2,
        numBuffers = 3
    )

    private val tuningTargetComputer = TuningTargetComputer(musicalScale, instrument, toleranceInCents)
    private var currentTargetNote: MusicalNote? = null
    private var timeStepOfLastSuccessfulFrequencyDetection = 0
    private var smoothedFrequency = 0f

    fun evaluate(
        frequencyCollectionResults: FrequencyDetectionCollectedResults?,
        userDefinedNote: MusicalNote?): FrequencyEvaluationResult {
        var frequencyDetectionTimeStep = -1
        var dt = -1f
        val newTarget = frequencyCollectionResults?.let {
            if (it.noise < maxNoise) {
                smoothedFrequency = smoother(it.frequency)
                frequencyDetectionTimeStep = it.timeSeries.framePosition
                dt = it.timeSeries.dt

                if (smoothedFrequency > 0f) {
                    timeStepOfLastSuccessfulFrequencyDetection = frequencyDetectionTimeStep
                    tuningTargetComputer(
                        smoothedFrequency,
                        currentTargetNote,
                        userDefinedNote
                    )
                } else {
                    null
                }
            } else {
                null
            }
        }

        newTarget?.note?.let {
            currentTargetNote = it
        }

        return FrequencyEvaluationResult(
            smoothedFrequency,
            newTarget,
            (frequencyDetectionTimeStep - timeStepOfLastSuccessfulFrequencyDetection) * dt
        )
    }
//    }.collect {
//        ensureActive()
////                Log.v("Tuner", "TunerViewModel: evaluating target: $it, $coroutineContext")
//        it.target?.let{ tuningTarget ->
//            currentTargetNote = tuningTarget.note
//            _tuningTarget.value = tuningTarget
//        }
//        _timeSinceThereIsNoFrequencyDetectionResult.value = it.timeSinceThereIsNoFrequencyDetectionResult
//        if (it.smoothedFrequency > 0f)
//            _currentFrequency.value = it.smoothedFrequency
//    }
}