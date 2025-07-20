/*
* Copyright 2024 Michael Moessner
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

import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.musicalscale.MusicalScale2
import kotlin.math.log10

/** Postprocess frequency.
 * Compared to the FrequencyEvaluator, this does not find target notes.
 * @param numMovingAverage Number of values used for computing moving average for frequency
 *   smoothing.
 * @param maxNumFaultyValues How many values can be of before the smoother discards the history.
 * @param maxNoise If noise in signal is higher than this value, we will not use this value.
 * @param minHarmonicEnergyContent Minimum required harmonic content in signal. If below, we will
 *   ignore the sample.
 * @param sensitivity Another measure to sort out bad samples. A value from 0 to 100. If 0, we
 *   will never use a signal. If 100 we use every signal. If somewhere between we evaluate the
 *   harmonic energy of the signal an require its absolute values to be high enough.
 */
class FrequencyEvaluatorSimple(
    numMovingAverage: Int,
    maxNumFaultyValues: Int,
    private val maxNoise: Float,
    private val minHarmonicEnergyContent: Float,
    private val sensitivity: Float
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

    /** Evaluate results.
     * @param frequencyCollectionResults Input from frequency detector.
     * @return Postprocessed (smoothed and filtered) frequency or 0f if sample should be discarded.
     */
    fun evaluate(frequencyCollectionResults: FrequencyDetectionCollectedResults?): Float {
        var smoothedFrequency = 0f
//        Log.v("Tuner", "FrequencyEvaluator.evaluate: frequencyCollectionResults = $frequencyCollectionResults")
        frequencyCollectionResults?.let {
//            Log.v("Tuner", "FrequencyEvaluator.evaluate: noise = ${it.noise}, maxNoise=$maxNoise, f=${it.frequency}")
            val requiredEnergyLevel =
                100 - sensitivity - 0.0001f // minus a very small number, to make sure, that a level of 0 always enables evaluation for sensitivity 100
//            Log.v("Tuner", "FrequencyEvaluator.evaluate: energy = ${it.harmonicEnergyAbsolute} signalLevel = ${transformEnergyToLevelFrom0To100(it.harmonicEnergyAbsolute)}, required = $requiredEnergyLevel")
            if (it.noise < maxNoise
                && it.harmonicEnergyContentRelative >= minHarmonicEnergyContent
                && transformEnergyToLevelFrom0To100(it.harmonicEnergyAbsolute) >= requiredEnergyLevel
            ) {
                smoothedFrequency = smoother(it.frequency)
            }
        }
        return smoothedFrequency
    }

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