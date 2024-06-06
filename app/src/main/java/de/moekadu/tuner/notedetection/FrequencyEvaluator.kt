package de.moekadu.tuner.notedetection

import android.util.Log
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale
import kotlin.math.log10

data class FrequencyEvaluationResult(
    val smoothedFrequency: Float,
    val target: TuningTarget?,
    val timeSinceThereIsNoFrequencyDetectionResult: Float,
    val framePosition: Int
)

class FrequencyEvaluator(
    numMovingAverage: Int,
    toleranceInCents: Float,
    maxNumFaultyValues: Int,
    private val maxNoise: Float,
    private val minHarmonicEnergyContent: Float,
    private val sensitivity: Float,
    musicalScale: MusicalScale,
    instrument: Instrument
) {
    private val smoother = OutlierRemovingSmoother(
        numMovingAverage,
        DefaultValues.FREQUENCY_MIN,
        DefaultValues.FREQUENCY_MAX,
        relativeDeviationToBeAnOutlier = 0.1f,
        maxNumSuccessiveOutliers = maxNumFaultyValues,
        minNumValuesForValidMean = 2,
        numBuffers = 3
    )

    private val tuningTargetComputer = TuningTargetComputer(musicalScale, instrument, toleranceInCents)
    private var currentTargetNote: MusicalNote? = null
    private var timeStepOfLastSuccessfulFrequencyDetection = 0

    private var lastTime = 0

    fun evaluate(
        frequencyCollectionResults: FrequencyDetectionCollectedResults?,
        userDefinedNote: MusicalNote?
    ): FrequencyEvaluationResult {
        var smoothedFrequency = 0f
        var frequencyDetectionTimeStep = -1
        var dt = -1f
//        Log.v("Tuner", "FrequencyEvaluator.evaluate: frequencyCollectionResults = $frequencyCollectionResults")
        val newTarget = frequencyCollectionResults?.let {
//            Log.v("Tuner", "FrequencyEvaluator.evaluate: noise = ${it.noise}, maxNoise=$maxNoise, f=${it.frequency}")
            val requiredEnergyLevel = 100 - sensitivity - 0.0001f // minus a very small number, to make sure, that a level of 0 always enables evaluation for sensitivity 100
//            Log.v("Tuner", "FrequencyEvaluator.evaluate: energy = ${it.harmonicEnergyAbsolute} signalLevel = ${transformEnergyToLevelFrom0To100(it.harmonicEnergyAbsolute)}, required = $requiredEnergyLevel")
            if (it.noise < maxNoise
                && it.harmonicEnergyContentRelative >= minHarmonicEnergyContent
                && transformEnergyToLevelFrom0To100(it.harmonicEnergyAbsolute) >= requiredEnergyLevel
                ) {
                smoothedFrequency = smoother(it.frequency)
                frequencyDetectionTimeStep = it.timeSeries.framePosition
                dt = it.timeSeries.dt

//                Log.v("Tuner", "FrequencyEvaluator.evaluate: smoothedFrequency=$smoothedFrequency")
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

        val time = frequencyCollectionResults?.timeSeries?.framePosition ?: 0
        val diff = time - lastTime
        lastTime = time
//        Log.v("Tuner", "FrequencyEvaluator.evaluate: time since last update = $diff")
        return FrequencyEvaluationResult(
            smoothedFrequency,
            newTarget,
            (frequencyDetectionTimeStep - timeStepOfLastSuccessfulFrequencyDetection) * dt,
            frequencyDetectionTimeStep
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

    private fun transformEnergyToLevelFrom0To100(energy: Float): Float {
        // sine waves of maximum amplitude (1f) would have a level of log10(1f)
        // but normal levels are normally much below 1, 1e-3 seems good enough

        val minValue = 1e-7f
        val maxValue = 1e-2f
        val minLevel = log10(minValue)
        val maxLevel = log10(maxValue)
        val energyLevel = log10(energy.coerceAtLeast(minValue)) // make sure, that we don't use zero
        return (100 * (energyLevel - minLevel) / (maxLevel - minLevel)).coerceIn(0f, 100f)
    }
}