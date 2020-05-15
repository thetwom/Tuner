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

class Correlation (val size : Int, private val windowType : Int = RealFFT.NO_WINDOW) {

  private val fft = RealFFT(2 * size)
  private val inputBitreversed = FloatArray(2 * size + 2)
  private val window = FloatArray(size)

  init {
    if (windowType == RealFFT.HAMMING_WINDOW) {
      for (i in 0 until size)
        window[i] = 0.54f - 0.46f * cos(2.0f * PI.toFloat() * i.toFloat() / size.toFloat())
    }
    else if (windowType != RealFFT.NO_WINDOW) {
      throw RuntimeException("Invalid window type")
    }
  }

  /// Autocorrelation of input.
  /**
   * @param input Input data which should be correlated (required size: size)
   * @param output Output array where we store the autocorrelation, (required size: size+1)
   * @param disableWindow if true, we disable windowing, even when it is defined in the constructor.
   * @param spectrum If a non-null array is given, we will store here to spectrum of zero-padded input (input is
   *   zero-padded to become twice the size, before we start correlating). If it is null, we will use the internal class
   *   storage. If it is non-null, the size of the spectrum must be 2*size+2.
   */
  fun correlate(input : CircularRecordData.ReadBuffer, output : FloatArray, disableWindow : Boolean = false, spectrum : FloatArray? = null) {
    require(input.size == size) {"input size must be equal to the size of the correlation size"}
    require(output.size - 1 == size) {"output  size must be correlation size + 1"}
    if(spectrum != null) {
      require(spectrum.size == 2 * size + 2) { "output spectrum size must be 2*size+2" }
    }
    val spectrumStorage = spectrum ?: inputBitreversed

    if(windowType == RealFFT.NO_WINDOW || disableWindow) {
      for (i in 0 until size) {
        val ir2 = 2 * fft.bitReverseTable[i]
        val i2 = 2 * i
        if (i2 >= ir2) {
          spectrumStorage[i2] = if (ir2 < size) input[ir2] else 0f
          spectrumStorage[i2 + 1] = if (ir2 + 1 < size) input[ir2 + 1] else 0f
          spectrumStorage[ir2] = if (i2 < size) input[i2] else 0f
          spectrumStorage[ir2 + 1] = if (i2 + 1 < size) input[i2 + 1] else 0f
        }
      }
    }
    else {
      for (i in 0 until size) {
        val ir2 = 2 * fft.bitReverseTable[i]
        val i2 = 2 * i
        if (i2 >= ir2) {
          spectrumStorage[i2] = if (ir2 < size) window[ir2] * input[ir2] else 0f
          spectrumStorage[i2 + 1] = if (ir2 + 1 < size) window[ir2 + 1] * input[ir2 + 1] else 0f
          spectrumStorage[ir2] = if (i2 < size) window[i2] * input[i2] else 0f
          spectrumStorage[ir2 + 1] = if (i2 + 1 < size) window[i2 + 1] * input[i2 + 1] else 0f
        }
      }
    }
    fft.fftBitreversed(spectrumStorage)

    // compute amplitudes, we have to be careful ere since spectrumStorage and inputBitreversed can be the same storage!
    val ampMaxFreq = spectrumStorage[1]
    inputBitreversed[1] = 0f
    for(i in 0 until size) {
      inputBitreversed[i] = spectrumStorage[2*i].pow(2) + spectrumStorage[2*i+1].pow(2)
    }
    inputBitreversed[size] = ampMaxFreq
    // copy amplitudes
    for(i in 1 until size)
      inputBitreversed[size + i] = inputBitreversed[size - i]

    for (i in 0 until size) {
      val ir2 = 2 * fft.bitReverseTable[i]
      val i2 = 2 * i
      if (i2 > ir2) {
        val tmp1 = inputBitreversed[i2]
        inputBitreversed[i2] = inputBitreversed[ir2]
        inputBitreversed[ir2] = tmp1
        val tmp2 = inputBitreversed[i2 + 1]
        inputBitreversed[i2 + 1] = inputBitreversed[ir2 + 1]
        inputBitreversed[ir2 + 1] = tmp2
      }
    }
    fft.fftBitreversed(inputBitreversed)

    for(i in 0 until size)
      output[i] = inputBitreversed[2*i]
    output[size] = inputBitreversed[1]
  }
}