package de.moekadu.tuner

import kotlin.math.*

class Correlation (val size : Int, private val windowType : Int = NO_WINDOW) {
  companion object {
        const val NO_WINDOW = 0
        const val HAMMING_WINDOW = 1
    }

  private val fft = RealFFT(2 * size)
  private val inputBitreversed = FloatArray(2 * size + 2)
  private val window = FloatArray(size)

  init {
    if (windowType == HAMMING_WINDOW) {
      for (i in 0 until size)
        window[i] = 0.54f - 0.46f * cos(2.0f * PI.toFloat() * i.toFloat() / size.toFloat())
    }
    else if (windowType != NO_WINDOW) {
      throw RuntimeException("Invalid window type")
    }
  }

  fun correlate(input : CircularRecordData.ReadBuffer, output : FloatArray, disableWindow : Boolean = false) {
    require(input.size == size) {"input size must be equal to the size of the correlation size"}
    require(output.size - 1 == size) {"output  size must be correlation size + 1"}

    if(windowType == NO_WINDOW || disableWindow) {
      for (i in 0 until size) {
        val ir2 = 2 * fft.bitReverseTable[i]
        val i2 = 2 * i
        if (i2 >= ir2) {
          inputBitreversed[i2] = if (ir2 < size) input[ir2] else 0f
          inputBitreversed[i2 + 1] = if (ir2 + 1 < size) input[ir2 + 1] else 0f
          inputBitreversed[ir2] = if (i2 < size) input[i2] else 0f
          inputBitreversed[ir2 + 1] = if (i2 + 1 < size) input[i2 + 1] else 0f
        }
      }
    }
    else {
      for (i in 0 until size) {
        val ir2 = 2 * fft.bitReverseTable[i]
        val i2 = 2 * i
        if (i2 >= ir2) {
          inputBitreversed[i2] = if (ir2 < size) window[ir2] * input[ir2] else 0f
          inputBitreversed[i2 + 1] = if (ir2 + 1 < size) window[ir2 + 1] * input[ir2 + 1] else 0f
          inputBitreversed[ir2] = if (i2 < size) window[i2] * input[i2] else 0f
          inputBitreversed[ir2 + 1] = if (i2 + 1 < size) window[i2 + 1] * input[i2 + 1] else 0f
        }
      }
    }
    fft.fftBitreversed(inputBitreversed)

    // compute amplitudes
    val ampMaxFreq = inputBitreversed[1]
    inputBitreversed[1] = 0f
    for(i in 0 until size) {
      inputBitreversed[i] = inputBitreversed[2*i].pow(2) + inputBitreversed[2*i+1].pow(2)
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