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

import kotlin.math.*

class PitchHistory(val size : Int, private val tuningFrequencies : TuningFrequencies) {
    /// Current estimate of the tone index based on the pitch history
    var currentEstimatedToneIndex = 0
        private set

    private val pitchArray = FloatArray(size) { tuningFrequencies.getNoteFrequency(currentEstimatedToneIndex) }

    private var numFaultyValues = 0
    private val maybeFaultyValues = FloatArray(3) { 0.0f }

    private var allowedDeltaNoteToBeValid = 0.5f

    /// We need some hysteresis effect, before we changing our current tone estimate
    /**
     * A value of 0.5f would mean that if we exactly between two half tone, we change our pitch, but
     * this would have no hysteresis effect. So better give a value somewhere between 0.5f and 1.0f
     */
    private val allowedHalfToneDeviationBeforeChangingTarget = 0.8f

    /// This describes the frequency range, inside which we won't change our current tone estimate.
    private val currentRangeBeforeChangingPitch = floatArrayOf(-1.0f, -1.0f)

    init {
        updateCurrentPitch(pitchArray.last())
    }

    fun getCurrentValue() : Float {
        return pitchArray.last()
    }

    fun getHistory() : FloatArray {
        return pitchArray
    }

    fun addValue(value : Float) : Int {
        val lastValue = pitchArray.last()

        val allowedRatioToBeValid = (
            tuningFrequencies.getNoteFrequency(currentEstimatedToneIndex+allowedDeltaNoteToBeValid) /
            tuningFrequencies.getNoteFrequency(currentEstimatedToneIndex)
            )

        if(value < allowedRatioToBeValid * lastValue && value > lastValue / allowedRatioToBeValid) {
            pitchArray.copyInto(pitchArray, 0, 1)
            pitchArray[pitchArray.lastIndex] = value
            updateCurrentPitch(value)
            return currentEstimatedToneIndex
        }

        if(numFaultyValues == 0) {
            maybeFaultyValues[0] = value
            ++numFaultyValues
            return currentEstimatedToneIndex
        }

        val lastFaultyValue = maybeFaultyValues[numFaultyValues-1]
        if(value < allowedRatioToBeValid * lastFaultyValue && value > lastFaultyValue / allowedRatioToBeValid) {
            maybeFaultyValues[numFaultyValues] = value
            ++numFaultyValues
        }
        else {
            numFaultyValues = 0
            return currentEstimatedToneIndex
        }

        if(numFaultyValues == maybeFaultyValues.size) {
            pitchArray.copyInto(pitchArray, 0, maybeFaultyValues.size)
            maybeFaultyValues.copyInto(pitchArray, pitchArray.size-maybeFaultyValues.size, 0)
            numFaultyValues = 0
            updateCurrentPitch(pitchArray.last())
        }
        return currentEstimatedToneIndex
    }

    /// Returns true, if the current pitch was changed, else false
    private fun updateCurrentPitch(latestPitchValue : Float) : Boolean {
        if(latestPitchValue < currentRangeBeforeChangingPitch[0] || latestPitchValue > currentRangeBeforeChangingPitch[1]) {
            currentEstimatedToneIndex = tuningFrequencies.getClosestToneIndex(latestPitchValue)
            currentRangeBeforeChangingPitch[0] = tuningFrequencies.getNoteFrequency(currentEstimatedToneIndex - allowedHalfToneDeviationBeforeChangingTarget)
            currentRangeBeforeChangingPitch[1] = tuningFrequencies.getNoteFrequency(currentEstimatedToneIndex + allowedHalfToneDeviationBeforeChangingTarget)
            return true
        }
        return false
    }
}