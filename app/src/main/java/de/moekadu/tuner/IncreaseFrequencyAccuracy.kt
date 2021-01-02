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

/// Increase the frequency accuracy of maximum SPL if two spectra captured one after another are given.
/**
 * This ensures that the spectrumShift fits to the number of waves + phase:
 *   spectrumShift = periodicDuration * (numWaves + phaseDifference / (2 * pi))
 *                 = (numWaves + phaseDifference / (2 * pi)) / (high-accuracy frequency)
 * If we assumes that the phaseDifference is correct, we can compute the high accuracy frequency
 * by reformulating this equation:
 *    high accuracy frequency = (numWaves + phaseDifference / (2*pi))
  * and the number of waves is also computed with the above formula, but with the low accuracy frequency:
 *    numWaves = round(spectrumShift * frequency - phaseDifference / (2 * pi))
 *
 * @param spectrum1 First spectrum, computed from a time series.
 * @param spectrum2 Second spectrum, computed from the same time series as spectrum1 but at a later point in time
 *   as spectrum1. Must have the same size as spectrum1.
 * @param frequencyIndex Frequency index in spectrum1 for the estimated maximum SPL.
 * @param df Frequency resolution in Hz of the two spectra
 * @param spectrumShift Time in seconds telling how far the time series of the second spectrum comes after the first spectrum
 * @return Corrected frequency for maximum SPL.
 */
fun increaseFrequencyAccuracy(spectrum1: FloatArray, spectrum2: FloatArray, frequencyIndex: Int, df: Float, spectrumShift: Float) : Float {
  require(spectrum1.size == spectrum2.size) {"The two spectra must be of equal size."}

  val frequency = frequencyIndex * df
  val phase1 = atan2(spectrum1[2 * frequencyIndex + 1], spectrum1[2 * frequencyIndex])
  val phase2 = atan2(spectrum2[2 * frequencyIndex + 1], spectrum2[2 * frequencyIndex])
  val phaseDiff = when {
    phase2 - phase1 > PI -> phase2 - phase1 - 2f * PI.toFloat()
    phase2 - phase1 < -PI -> phase2 - phase1 + 2f * PI.toFloat()
    else -> phase2 - phase1
  }
  val numWaves = (spectrumShift * frequency - phaseDiff / (2f * PI.toFloat())).roundToInt()
  return (numWaves + phaseDiff / (2f * PI.toFloat())) / spectrumShift
//  val phaseErrRaw =
//    phase2 - phase1 - 2.0f * PI.toFloat() * frequency * spectrumShift
//  val phaseErr = phaseErrRaw - 2.0f * PI.toFloat() * round(phaseErrRaw / (2.0f * PI.toFloat()))
//  return spectrumShift * frequency / (spectrumShift - phaseErr / (2.0f * PI.toFloat() * frequency))
}

/// Increase time shift accuracy by fitting a second order polynomial over three points.
/**
 * @param correlation Array with autocorrelation values.
 * @param maximumIndex Index of a local maximum within the correlation-array
 * @param dt Time shift between two neighboring indices.
 * @return Time shift obtained by finding the maximum of the second order polynomial going through
 *   the point at the maximumIndex and the left and right neighbors.
 */
fun increaseTimeShiftAccuracy(correlation: FloatArray, maximumIndex: Int, dt : Float): Float {
  require(maximumIndex > 0) // zero shift doesn't make sense for obtaining a frequency
  if (maximumIndex >= correlation.size - 1)
    return maximumIndex * dt

  val t0 = (maximumIndex - 1) * dt
  val t1 = maximumIndex * dt
  val t2 = (maximumIndex + 1) * dt
  val c0 = correlation[maximumIndex - 1]
  val c1 = correlation[maximumIndex]
  val c2 = correlation[maximumIndex + 1]
  require(c0 <= c1 && c2 <= c1)

  return (0.5f * ((t1*t1 - t0*t0) * c2 + (t0*t0 - t2*t2) * c1 + (t2*t2 - t1*t1) * c0)
          / ((t1 - t0) * c2 + (t0 - t2) * c1 + (t2 - t1) * c0))
}