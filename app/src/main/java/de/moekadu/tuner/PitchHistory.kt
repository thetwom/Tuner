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

package de.moekadu.tuner

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.math.max
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

class PitchHistory(size : Int, tuningFrequencies : TuningFrequencies) {

    var size = size
        set(value) {
            if (value != field) {
                field = value
                _sizeAsLiveData.value = field
                if (pitchArray.size > field)
                    pitchArray.subList(0, pitchArray.size - field).clear()
            }
        }

    private val _sizeAsLiveData = MutableLiveData<Int>().apply { value = size }
    val sizeAsLiveData: LiveData<Int>
        get() = _sizeAsLiveData

    /// TuningFrequencies.
    var tuningFrequencies = tuningFrequencies
        set(value)  {
            field = value
            // invalidate range which then will force to update the current estimated tone index
            currentRangeBeforeChangingPitch[0] = 0.0f
            currentRangeBeforeChangingPitch[1] = -1.0f
            updateCurrentEstimatedToneIndex()
        }
    /// Current estimate of the tone index based on the pitch history
    private val _currentEstimatedToneIndex = MutableLiveData<Int>()

    /// Here we allow the observers to take track of the current estimated tone index
    val currentEstimatedToneIndex: LiveData<Int>
        get() = _currentEstimatedToneIndex

    /// Here we store our pitch history
    private val pitchArray = ArrayList<Float>(size)

    /// Number of values which would lead to another detected pitch
    var maxNumFaultyValues = 3

    /// Last appended values which would lead to another detected pitch
    private val maybeFaultyValues = ArrayList<Float>(maxNumFaultyValues)

    /// We only allow values which don't differ from the previous value too much,
    /** This is the maximum allowed difference in dimensions of note indices. */
    private var allowedDeltaNoteToBeValid = 0.5f

    /// We need some hysteresis effect, before we changing our current tone estimate
    /**
     * A value of 0.5f would mean that if we exactly between two half tone, we change our pitch, but
     * this would have no hysteresis effect. So better give a value somewhere between 0.5f and 1.0f
     */
    private val allowedHalfToneDeviationBeforeChangingTarget = 0.8f

    /// This describes the frequency range, inside which we won't change our current tone estimate.
    /** first value is min, second value is max value of range. */
    private val currentRangeBeforeChangingPitch = floatArrayOf(0.0f, -1.0f)

    /// Backing LiveData field for the current frequency
    private val _history = MutableLiveData<ArrayList<Float> >()

    /// Here we allow the user to observe history changes
    val history: LiveData<ArrayList<Float> >
        get() = _history

    /// Defines the frequency limits which can be used for plots
    var plotRangeInToneIndices = 3f
        set(value) {
            field = value
            updatePlotRange()
        }

    /// Backing field for minimum and maximum limit of plot range
    private val frequencyPlotRangeValues = floatArrayOf(400f, 500f)
    /// Backing LiveData field for plot range
    private val _frequencyPlotRange = MutableLiveData<FloatArray>()
    /// LiveData field which contains the current frequency limits, when plotting the current tones
    val frequencyPlotRange: LiveData<FloatArray>
        get() = _frequencyPlotRange

    /// Append a new frequency value to our history.
    /**
     * @note This will also update the currentEstimatedPitch and the history live data.
     * @param value Latest frequency value
     */
    fun appendValue(value : Float) {
//        Log.v("Tuner", "PitchHistory.appendValue: $value")
//        Log.v("TestRecordFlow", "PitchHistory.appendValue: value=$value")
        var pitchArrayUpdated = false

        if (pitchArray.size == 0) {
            pitchArray.add(value)
            pitchArrayUpdated = true
        }
        else {
            val lastValue = pitchArray.last()
            val toneIndex = tuningFrequencies.getToneIndex(lastValue)
            val validFrequencyMin = tuningFrequencies.getNoteFrequency(toneIndex - allowedDeltaNoteToBeValid)
            val validFrequencyMax = tuningFrequencies.getNoteFrequency(toneIndex + allowedDeltaNoteToBeValid)

            if(value < validFrequencyMax && value >= validFrequencyMin) {
                if (pitchArray.size >= size)
                    pitchArray.subList(0, pitchArray.size - size + 1).clear()
                pitchArray.add(value)
                pitchArrayUpdated = true
            }
        }

        if (!pitchArrayUpdated) {
            if (maybeFaultyValues.size == 0) {
                maybeFaultyValues.add(value)
            }
            else {
                val lastFaultyValue = maybeFaultyValues.last()
                val toneIndex = tuningFrequencies.getToneIndex(lastFaultyValue)
                val validFrequencyMin = tuningFrequencies.getNoteFrequency(toneIndex - allowedDeltaNoteToBeValid)
                val validFrequencyMax = tuningFrequencies.getNoteFrequency(toneIndex + allowedDeltaNoteToBeValid)

                if(value >= validFrequencyMax || value < validFrequencyMin)
                    maybeFaultyValues.clear()

                maybeFaultyValues.add(value)
            }

            // we have completely filled our maybeFaultyValues-array. We are convinced now, that
            // these values are not faulty but rather indicate a pitch change. So we take these
            // values now.
            if(maybeFaultyValues.size >= maxNumFaultyValues) {
                val freeSpace = size - pitchArray.size
                if (maxNumFaultyValues > freeSpace) {
                    val numDelete = min(maxNumFaultyValues - freeSpace, pitchArray.size)
                    pitchArray.subList(0, numDelete).clear()
                }
                val numCopy = min(maxNumFaultyValues, size)
                pitchArray.addAll(maybeFaultyValues.subList(maybeFaultyValues.size - numCopy, maybeFaultyValues.size))
                pitchArrayUpdated = true
            }
        }

        if (pitchArrayUpdated) {
            require(pitchArray.size > 0)
            maybeFaultyValues.clear()
            _history.value = pitchArray
            updateCurrentEstimatedToneIndex()
            updatePlotRange()
        }
    }

    /// Update currentEstimatedToneIndex if the last frequency in the history tells us so.
    private fun updateCurrentEstimatedToneIndex() {
        val currentFrequency = if (pitchArray.size == 0) return else pitchArray.last()

        if (currentFrequency <= 0f)
            return
//        Log.v("TestRecordFlow", "PitchHistory.updateCurrentEstimatedToneIndex: currentFrequency=$currentFrequency")
        if(currentFrequency < currentRangeBeforeChangingPitch[0] || currentFrequency >= currentRangeBeforeChangingPitch[1]) {
            val toneIndex = tuningFrequencies.getClosestToneIndex(currentFrequency)
            currentRangeBeforeChangingPitch[0] = tuningFrequencies.getNoteFrequency(toneIndex - allowedHalfToneDeviationBeforeChangingTarget)
            currentRangeBeforeChangingPitch[1] = tuningFrequencies.getNoteFrequency(toneIndex + allowedHalfToneDeviationBeforeChangingTarget)
            _currentEstimatedToneIndex.value = toneIndex
            //updatePlotRange()
        }
    }

    private fun updatePlotRange() {
//        Log.v("Tuner", "PitchHistory.updatePlotRange")
        val pitchMin = pitchArray.minOrNull() ?: return
        val pitchMax = pitchArray.maxOrNull() ?: return

        val toneIndexMin = tuningFrequencies.getClosestToneIndex(pitchMin)
        val toneIndexMax = tuningFrequencies.getClosestToneIndex(pitchMax)

        val lowerBound = tuningFrequencies.getNoteFrequency(toneIndexMin - 0.5f * plotRangeInToneIndices)
        val upperBound = tuningFrequencies.getNoteFrequency(toneIndexMax + 0.5f * plotRangeInToneIndices)
//        Log.v("TestRecordFlow", "PitchHistory, updatePlotRange: lowerBound=$lowerBound, upperBound=$upperBound")
        // only update after something changed
        if (lowerBound != frequencyPlotRangeValues[0] || upperBound != frequencyPlotRangeValues[1]) {
            frequencyPlotRangeValues[0] = lowerBound
            frequencyPlotRangeValues[1] = upperBound
            _frequencyPlotRange.value = frequencyPlotRangeValues
        }
//        Log.v("TestRecordFlow", "PitchHistory.updatePlotRange: lowerBound=$lowerBound, upperBound=$upperBound")
    }
}
