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

fun bitReverse(value : Int, num_bits : Int) : Int {
    var myValue = value
    var rev = 0

    for (i in 0 until num_bits) {
        rev = rev shl (1)
        rev = rev or (myValue and 1)
        myValue = myValue shr (1)
    }

    return rev
}

/// Number of frequencies, for which we get a result from the RealFFT.
/**
 * @param size Size as passed to the constructor of the RealFFT.
 * @return Number of frequencies for which we get a RealFFT result.
 */
fun RealFFT.Companion.numFrequencies(size: Int) : Int {
    return size / 2 + 1
}

/// Frequency for a specific index computed by the RealFFT.
/**
 * @param idx Index for which the frequency should be computed.
 * @param size Size as passed to the constructor of the RealFFT.
 * @param dt Time step width between two input samples.
 * @return Frequency value for the given value.
 */
fun RealFFT.Companion.getFrequency(idx : Int, size: Int, dt : Float) : Float {
    return idx / (dt * size)
}

/// Frequency index for a specific frequency value.
/**
 * @param frequency Frequency for which the index is requested.
 * @param size Size as passed to the constructor of the RealFFT.
 * @param dt Time step width between two input samples.
 * @return Closest index which fits to the given frequency.
 */
fun RealFFT.Companion.closestFrequencyIndex(frequency : Float, size: Int, dt : Float) : Int {
    return (frequency * dt * size).roundToInt()
}

class RealFFT(val size : Int, private val windowType : WindowingFunction = WindowingFunction.Tophat) {
    companion object {}

    private val cosTable = FloatArray(size)
    private val sinTable = FloatArray(size)
    private val cosTableH = FloatArray(size)
    private val sinTableH = FloatArray(size)
    val bitReverseTable = IntArray(size/2)
    private val nBits = log2(size.toFloat()).roundToInt()

    private val window = FloatArray(size)

    init {
        val sizeCheck = 1 shl nBits
        if (size != sizeCheck) {
            throw RuntimeException("RealFFT size must be a power of 2 but $size given.")
        }
        val halfSize = size / 2

        val fac: Float = -2.0f * PI.toFloat() / size
        for (i in 0 until halfSize) {
            sinTable[i] = sin(2.0f * i * fac)
            cosTable[i] = cos(2.0f * i * fac)

            sinTableH[i] = sin(i * fac)
            cosTableH[i] = cos(i * fac)

            bitReverseTable[i] = bitReverse(i, nBits - 1)
        }

        getWindow(windowType, size).copyInto(window)
    }

    fun fft(input: FloatArray, output: FloatArray, disableWindow : Boolean = false) {
        require(size == input.size) {"FFT input is of invalid size"}
        require(size == output.size-2) {"FFT output is of invalid size"}
      
        val halfSize = size / 2

        if(windowType == WindowingFunction.Tophat || disableWindow) {
            for (i in 0 until halfSize) {
                val ir2 = 2 * bitReverseTable[i]
                val i2 = 2 * i
                if (i2 >= ir2) {
                    output[i2] = input[ir2]
                    output[i2 + 1] = input[ir2 + 1]
                    output[ir2] = input[i2]
                    output[ir2 + 1] = input[i2 + 1]
                }
            }
        }
        else {
            for (i in 0 until halfSize) {
                val ir2 = 2 * bitReverseTable[i]
                val i2 = 2 * i
                if (i2 >= ir2) {
                    output[i2] = window[ir2] * input[ir2]
                    output[i2 + 1] = window[ir2 + 1] * input[ir2 + 1]
                    output[ir2] = window[i2] * input[i2]
                    output[ir2 + 1] = window[i2 + 1] * input[i2 + 1]
                }
            }
        }
        fftBitreversed(output)
    }

    /// Transform already bitreversed data in-place
    fun fftBitreversed(output: FloatArray) {
        require(size == output.size-2) {"size of output must be fftSize + 2"}
        val halfSize = size / 2
        var numInner = 1
        var wStep = halfSize / 2

        for (iOuter in 0 until nBits - 1) {
            var idx1 = 0
            var idx2 = halfSize / 2

            for (i in 0 until numInner) {
                val cos1 = cosTable[idx1]
                val sin1 = sinTable[idx1]
                val cos2 = cosTable[idx2]
                val sin2 = sinTable[idx2]

                var k1re = 2 * i

                for (j in 0 until wStep) {
                    val k1im = k1re + 1
                    val k2re = k1re + 2 * numInner
                    val k2im = k2re + 1

                    val tmp2Re = output[k2re]
                    val tmp2Im = output[k2im]

                    output[k2re] = output[k1re] + cos2 * tmp2Re - sin2 * tmp2Im
                    output[k2im] = output[k1im] + cos2 * tmp2Im + sin2 * tmp2Re
                    output[k1re] += cos1 * tmp2Re - sin1 * tmp2Im
                    output[k1im] += cos1 * tmp2Im + sin1 * tmp2Re

                    k1re += 4 * numInner
                }
                idx1 += wStep
                idx2 += wStep
            }
            numInner *= 2
            wStep /= 2
        }

        var k1re = 0
        var k1im = 1
        var k2re = 2 * halfSize
        var k2im = k2re + 1

        for (i in 1 until halfSize / 2) {
            k1re += 2
            k1im += 2
            k2re -= 2
            k2im -= 2

            val cos1 = cosTableH[i]
            val sin1 = sinTableH[i]
            val cos2 = cosTableH[halfSize - i]
            val sin2 = sinTableH[halfSize - i]

            val frRe = 0.5f * (output[k1re] + output[k2re])
            val frIm = 0.5f * (output[k1im] - output[k2im])
            val grRe = 0.5f * (output[k1im] + output[k2im])
            val grIm = 0.5f * (output[k2re] - output[k1re])

            output[k1re] = frRe + cos1 * grRe - sin1 * grIm
            output[k1im] = frIm + sin1 * grRe + cos1 * grIm
            output[k2re] = frRe + cos2 * grRe + sin2 * grIm
            output[k2im] = -frIm + sin2 * grRe - cos2 * grIm
        }

        output[size] = output[0] - output[1]
        output[0] = output[0] + output[1]
        output[1] = 0.0f
        output[size+1] = 0.0f
    }

//    fun getFreq(idx : Int, dt : Float) : Float {
//        return idx / (dt * size)
//    }
}
