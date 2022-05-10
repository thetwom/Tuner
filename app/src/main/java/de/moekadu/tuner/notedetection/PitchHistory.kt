/*
 * Copyright 2020 Michael Moessner
 *
 * This file is part of Tuner.
 *
 * Tuner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tuner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.moekadu.tuner.notedetection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.moekadu.tuner.temperaments.TemperamentFrequencies
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/// Compute pitch history duration in seconds based on a percent value.
/**
 * This uses a exponential scale for setting the duration.
 * @param percent Percentage value where 0 stands for the minimum duration and 100 for the maximum.
 * @param durationAtFiftyPercent Duration in seconds at fifty percent.
 * @return Pitch history duration in seconds.
 */
fun percentToPitchHistoryDuration(percent: Int, durationAtFiftyPercent: Float = 3.0f) : Float {
    return durationAtFiftyPercent * 2.0f.pow(0.05f * (percent - 50))
}

/// Convert pitch history duration to the number of samples which have to be stored.
/**
 * @param duration Duration of pitch history in seconds.
 * @param sampleRate Sample rate of audio signal in Hertz
 * @param windowSize Number of samples for one chunk of data which is used for evaluation.
 * @param overlap Overlap between to succeeding data chunks, where 0 is no overlap and 1 is
 *   100% overlap (1.0 is of course not allowed).
 * @return Number of samples, which must be stored in the pitch history, so that the we match
 *   the given duration.
 */
fun pitchHistoryDurationToPitchSamples(duration: Float, sampleRate: Int, windowSize: Int, overlap: Float): Int {
    return (duration / (windowSize.toFloat() / sampleRate.toFloat() * (1.0f - overlap))).roundToInt()
}

class PitchHistory(size : Int, var temperamentFrequencies : TemperamentFrequencies) {

    var size = size
        set(value) {
            if (value != field) {
                field = value
                _sizeAsLiveData.value = field
                if (pitchArray.size > field)
                    pitchArray.subList(0, pitchArray.size - field).clear()
                if (pitchArrayMovingAverage.size > field)
                    pitchArrayMovingAverage.subList(0, pitchArrayMovingAverage.size - field).clear()
            }
        }

    private val _sizeAsLiveData = MutableLiveData<Int>().apply { value = size }
    val sizeAsLiveData: LiveData<Int>
        get() = _sizeAsLiveData

    /// Current tone estimated tone index based on pitch history
    var currentEstimatedToneIndex = 0
        private set

    /// Here we store our pitch history
    private val pitchArray = ArrayList<Float>(size)

    /// Pitch array storing a moving average value
    private val pitchArrayMovingAverage = ArrayList<Float>(size)

    /// Number of samples used for a moving average computation
    var numMovingAverage = 5

    /** We only compute the moving average with values after a pitch change. Here we count
     * how many samples we can currently use for the average computation.
     */
    private var numberOfValuesWhichCanBeUsedForAveraging = 0

    /// Number of values which would lead to another detected pitch
    var maxNumFaultyValues = 3

    /// Maximum noise value for a value to be accepted (0f->no noise, 1f->signal is only noise)
    var maxNoise = 0.1f

    /// Last appended values which would lead to another detected pitch
    private val maybeFaultyValues = ArrayList<Float>(maxNumFaultyValues)

    /// We allow one exception inside the faulty values before throwing them away completely
    private var faultyValueException = 0f

    /// Defines if the faultyValueException is set.
    private var faultyValueExceptionSet = false

    /// We only allow values which don't differ from the previous value too much,
    /** This is the maximum allowed difference in dimensions of note indices. */
    private var allowedDeltaNoteToBeValid = 0.5f

//    /// We need some hysteresis effect, before we changing our current tone estimate
//    /**
//     * A value of 0.5f would mean that if we exactly between two half tone, we change our pitch, but
//     * this would have no hysteresis effect. So better give a value somewhere between 0.5f and 1.0f
//     */
//    private val allowedHalfToneDeviationBeforeChangingTarget = 0.8f

//    /// This describes the frequency range, inside which we won't change our current tone estimate.
//    /** first value is min, second value is max value of range. */
//    private val currentRangeBeforeChangingPitch = floatArrayOf(0.0f, -1.0f)

    /// Backing LiveData field for the current frequency
    private val _history = MutableLiveData<ArrayList<Float> >()

    /// Here we allow the user to observe history changes
    val history: LiveData<ArrayList<Float> >
        get() = _history

    /// Backing LiveData field for the current frequency of averaged data
    private val _historyAveraged = MutableLiveData<ArrayList<Float> >()

    /// Here we allow the user to observe history changes of averaged data
    val historyAveraged: LiveData<ArrayList<Float> >
        get() = _historyAveraged

//    /// Defines the frequency limits which can be used for plots
//    var plotRangeInToneIndices = 3f
//        set(value) {
//            field = value
//            updatePlotRange2()
//        }

//    /// Backing field for minimum and maximum limit of plot range
//    private val frequencyPlotRangeValues = floatArrayOf(400f, 500f)
//    /// Backing LiveData field for plot range
//    private val _frequencyPlotRange = MutableLiveData<FloatArray>()
//    /// LiveData field which contains the current frequency limits, when plotting the current tones
//    val frequencyPlotRange: LiveData<FloatArray>
//        get() = _frequencyPlotRange

//    /// Backing field for minimum and maximum limit of plot range of averaged data
//    private val frequencyPlotRangeAveragedValues = floatArrayOf(400f, 500f)
//    /// Backing LiveData field for plot range of averaged data
//    private val _frequencyPlotRangeAveraged = MutableLiveData<FloatArray>()
//    /// LiveData field which contains the current frequency limits of averaged values, when plotting the current tones
//    val frequencyPlotRangeAveraged: LiveData<FloatArray>
//        get() = _frequencyPlotRangeAveraged

    /** Backing live data where store the number of values which have been append to the plot
     * after the lines were updated. */
    private val _numValuesSinceLastLineUpdate = MutableLiveData(0L)
    /** Live data where store the number of values which have been append to the plot
     * after the lines were updated. */
    val numValuesSinceLastLineUpdate: LiveData<Long> = _numValuesSinceLastLineUpdate

    /// Append a new frequency value to our history.
    /**
     * @note This will also update the currentEstimatedPitch and the history live data.
     * @param value Latest frequency value
     */
    fun appendValue(value: Float, noise: Float) {
//        Log.v("Tuner", "PitchHistory.appendValue: $value")
//        Log.v("TestRecordFlow", "PitchHistory.appendValue: value=$value")
        if (noise > maxNoise) {
            _numValuesSinceLastLineUpdate.value = (_numValuesSinceLastLineUpdate.value ?: 0L) + 1L
            return
        }

        var pitchArrayUpdated = false

        if (pitchArray.size == 0) {
            addValueToPitchArrayAndComputeMovingAverage(value)
            pitchArrayUpdated = true
        }
        else {
            val lastValue = pitchArray.last()
            if (checkIfValueIsWithinAllowedRange(value, lastValue)) {
                addValueToPitchArrayAndComputeMovingAverage(value)
                pitchArrayUpdated = true
            }
        }

        if (!pitchArrayUpdated) {
            if (maybeFaultyValues.size == 0) {
                maybeFaultyValues.add(value)
            }
            else {
                val lastFaultyValue = maybeFaultyValues.last()

                if (checkIfValueIsWithinAllowedRange(value, lastFaultyValue)) {
                    maybeFaultyValues.add(value)
                    faultyValueExceptionSet = false
                }
                else {
                    // we do not directly delete the faulty values, but allow one exception
                    if (!faultyValueExceptionSet) {
                        faultyValueException = value
                        faultyValueExceptionSet = true
                    }
                    else {
                        maybeFaultyValues.clear()

                        // is our exception matches our new value, we also keep it,
                        if (checkIfValueIsWithinAllowedRange(value, faultyValueException))
                            maybeFaultyValues.add(faultyValueException)
                        maybeFaultyValues.add(value)
                        faultyValueExceptionSet = false
                    }
                }
            }

            // we have completely filled our maybeFaultyValues-array. We are convinced now, that
            // these values are not faulty but rather indicate a pitch change. So we take these
            // values now.
            if(maybeFaultyValues.size >= maxNumFaultyValues) {
                numberOfValuesWhichCanBeUsedForAveraging = 0
                for (v in maybeFaultyValues)
                    addValueToPitchArrayAndComputeMovingAverage(v)
                pitchArrayUpdated = true
            }
        }

        if (pitchArrayUpdated) {
            require(pitchArray.size > 0)
            maybeFaultyValues.clear()
            faultyValueExceptionSet = false
            _history.value = pitchArray
            _historyAveraged.value = pitchArrayMovingAverage
            // updateCurrentEstimatedToneIndex()
            // updatePlotRange2()
            if (_numValuesSinceLastLineUpdate.value != null)
                _numValuesSinceLastLineUpdate.value = 0L
        } else {
            _numValuesSinceLastLineUpdate.value = (_numValuesSinceLastLineUpdate.value ?: 0L) + 1L
        }
    }

    private fun addValueToPitchArrayAndComputeMovingAverage(value: Float) {
        if (pitchArray.size >= size)
            pitchArray.subList(0, pitchArray.size - size + 1).clear()
        if (pitchArrayMovingAverage.size >= size)
            pitchArrayMovingAverage.subList(0, pitchArrayMovingAverage.size - size + 1).clear()

        pitchArray.add(value)
        ++numberOfValuesWhichCanBeUsedForAveraging

        var newAverage = 0.0f
        val numAverage = min(numMovingAverage, min(numberOfValuesWhichCanBeUsedForAveraging, pitchArray.size))
        for (i in 1 .. numAverage)
            newAverage += pitchArray[pitchArray.size - i]
        pitchArrayMovingAverage.add(newAverage / numAverage)
    }

//    /// Update currentEstimatedToneIndex if the last frequency in the history tells us so.
//    private fun updateCurrentEstimatedToneIndex() {
//        val currentFrequency = if (pitchArray.size == 0) return else pitchArray.last()
//
//        if (currentFrequency <= 0f)
//            return
////        Log.v("TestRecordFlow", "PitchHistory.updateCurrentEstimatedToneIndex: currentFrequency=$currentFrequency")
//        if(currentFrequency < currentRangeBeforeChangingPitch[0] || currentFrequency >= currentRangeBeforeChangingPitch[1]) {
//            val toneIndex = tuningFrequencies.getClosestToneIndex(currentFrequency)
//            currentRangeBeforeChangingPitch[0] = tuningFrequencies.getNoteFrequency(toneIndex - allowedHalfToneDeviationBeforeChangingTarget)
//            currentRangeBeforeChangingPitch[1] = tuningFrequencies.getNoteFrequency(toneIndex + allowedHalfToneDeviationBeforeChangingTarget)
//            currentEstimatedToneIndex = toneIndex
//            //updatePlotRange()
//        }
//    }

//    private fun updatePlotRange() {
//        updatePlotRange(frequencyPlotRangeValues, _frequencyPlotRange, pitchArray)
//        updatePlotRange(frequencyPlotRangeAveragedValues, _frequencyPlotRangeAveraged, pitchArrayMovingAverage)
//    }
//
//    private fun updatePlotRange2() {
//        val lowerBound = tuningFrequencies.getNoteFrequency(currentEstimatedToneIndex - 0.5f * plotRangeInToneIndices)
//        val upperBound = tuningFrequencies.getNoteFrequency(currentEstimatedToneIndex + 0.5f * plotRangeInToneIndices)
//
//        // only update after something changed
//        if (lowerBound != frequencyPlotRangeValues[0] || upperBound != frequencyPlotRangeValues[1]) {
//            frequencyPlotRangeValues[0] = lowerBound
//            frequencyPlotRangeValues[1] = upperBound
//            _frequencyPlotRange.value = frequencyPlotRangeValues
//        }
//
//        if (lowerBound != frequencyPlotRangeAveragedValues[0] || upperBound != frequencyPlotRangeAveragedValues[1]) {
//            frequencyPlotRangeAveragedValues[0] = lowerBound
//            frequencyPlotRangeAveragedValues[1] = upperBound
//            _frequencyPlotRangeAveraged.value = frequencyPlotRangeAveragedValues
//        }
//    }
//
//    private fun updatePlotRange(plotRange: FloatArray, plotRangeLiveData: MutableLiveData<FloatArray>,
//                                plotValues: ArrayList<Float>) {
//        val plotMin = plotValues.minOrNull() ?: return
//        val plotMax = plotValues.maxOrNull() ?: return
//
//        val toneIndexMin = tuningFrequencies.getClosestToneIndex(plotMin)
//        val toneIndexMax = tuningFrequencies.getClosestToneIndex(plotMax)
//
//        val lowerBound = tuningFrequencies.getNoteFrequency(toneIndexMin - 0.5f * plotRangeInToneIndices)
//        val upperBound = tuningFrequencies.getNoteFrequency(toneIndexMax + 0.5f * plotRangeInToneIndices)
//
//        // only update after something changed
//        if (lowerBound != plotRange[0] || upperBound != plotRange[1]) {
//            plotRange[0] = lowerBound
//            plotRange[1] = upperBound
//            plotRangeLiveData.value = plotRange
//        }
//    }

    private fun checkIfValueIsWithinAllowedRange(value: Float, previousValue: Float) : Boolean {
        val toneIndex = temperamentFrequencies.getToneIndex(previousValue)
        val validFrequencyMin = temperamentFrequencies.getNoteFrequency(toneIndex - allowedDeltaNoteToBeValid)
        val validFrequencyMax = temperamentFrequencies.getNoteFrequency(toneIndex + allowedDeltaNoteToBeValid)
        return !(value >= validFrequencyMax || value < validFrequencyMin)
    }
}
