package de.moekadu.tuner

import kotlin.math.*

/// Increase the frequency accuracy of maximum SPL if two spectra captured one after another are given.
/**
 * @param spectrum1 First spectrum, computed from a time series.
 * @param spectrum2 Second spectrum, computed from the same time series as spectrum1 but at a later point in time
 *   as spectrum1. Must have the same size as spectrum1.
 * @param frequencyIndex Frequency index in spectrum1 for the estimated maximum SPL.
 * @param df Frequency resolution in Hz of the two spectra
 * @param spectrumShift Time in seconds telling how far the time series of the second spectrum comes after the first spectrum
 * @return Corrected frequency for maximum SPL.
 */
fun increaseFrequencyAccuracy(spectrum1 : FloatArray, spectrum2 : FloatArray, frequencyIndex : Int, df : Float, spectrumShift : Float) : Float {
  require(spectrum1.size == spectrum2.size) {"The two spectra must be of equal size."}

  val frequency = frequencyIndex * df
  val phase1 = atan2(spectrum1[2 * frequencyIndex + 1], spectrum1[2 * frequencyIndex])
  val phase2 = atan2(spectrum2[2 * frequencyIndex + 1], spectrum2[2 * frequencyIndex])
  val phaseErrRaw =
    phase2 - phase1 - 2.0f * PI.toFloat() * frequency * spectrumShift
  val phaseErr = phaseErrRaw - 2.0f * PI.toFloat() * round(phaseErrRaw / (2.0f * PI.toFloat()))
  val result = spectrumShift * frequency / (spectrumShift - phaseErr / (2.0f * PI.toFloat() * frequency))
  return result
}