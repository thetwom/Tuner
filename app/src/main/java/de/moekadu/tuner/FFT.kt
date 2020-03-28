package de.moekadu.tuner

import kotlin.math.roundToInt

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

fun RealFFT.Companion.numFrequencies(size: Int) : Int {
    return size/2
}

fun RealFFT.Companion.getFrequency(idx : Int, size: Int, dt : Float) : Float {
    return idx / (dt * size)
}

fun RealFFT.Companion.closestFrequencyIndex(frequency : Float, size: Int, dt : Float) : Int {
    return (frequency * dt * size).roundToInt()
}



class RealFFT(val size : Int, val windowType : Int = NO_WINDOW) {
    companion object {
        const val NO_WINDOW = 0
        const val HAMMING_WINDOW = 1

    }
    private val cosTable = FloatArray(size)
    private val sinTable = FloatArray(size)
    private val cosTableH = FloatArray(size)
    private val sinTableH = FloatArray(size)
    private val bitReverseTable = IntArray(size)
    private val nBits = kotlin.math.round(kotlin.math.log2(size.toFloat())).toInt()

    private val window = FloatArray(size)

    init {
        val sizeCheck = 1 shl nBits
        if (size != sizeCheck) {
            throw RuntimeException("RealFFT size must be a power of 2 but " + size + " given.")
        }
        val halfSize = size / 2

        val fac: Float = -2.0f * kotlin.math.PI.toFloat() / size
        for (i in 0 until halfSize) {
            sinTable[i] = kotlin.math.sin(2.0f * i * fac)
            cosTable[i] = kotlin.math.cos(2.0f * i * fac)

            sinTableH[i] = kotlin.math.sin(i * fac)
            cosTableH[i] = kotlin.math.cos(i * fac)

            bitReverseTable[i] = bitReverse(i, nBits - 1)
        }

        if (windowType == HAMMING_WINDOW) {
            for (i in 0 until size)
                window[i] =
                    0.54f - 0.46f * kotlin.math.cos(2.0f * kotlin.math.PI.toFloat() * i.toFloat() / size.toFloat())
        }
        else if (windowType != NO_WINDOW) {
            throw RuntimeException("Invalid window type")
        }
    }

    fun fft(input: CircularRecordData.ReadBuffer, output: FloatArray) {
        if (size != input.size) {
            throw RuntimeException("FFT input is of invalid size")
        }

        if (size != output.size) {
            throw RuntimeException("FFT output is of invalid size")
        }

        val halfSize = size / 2

        if(windowType == NO_WINDOW) {
            for (i in 0 until halfSize) {
                val ir2 = 2 * bitReverseTable[i]
                val i2 = 2 * i
                output[i2] = input[ir2]
                output[i2 + 1] = input[ir2 + 1]
                output[ir2] = input[i2]
                output[ir2 + 1] = input[i2 + 1]
            }
        }
        else {
            for (i in 0 until halfSize) {
                val ir2 = 2 * bitReverseTable[i]
                val i2 = 2 * i
                output[i2] = window[ir2] * input[ir2]
                output[i2 + 1] = window[ir2 + 1] * input[ir2 + 1]
                output[ir2] = window[i2] * input[i2]
                output[ir2 + 1] = window[i2 + 1] * input[i2 + 1]
            }
        }
        fft(output)
    }

    private fun fft(output: FloatArray) {

        if (size != output.size) {
            throw RuntimeException("FFT output is of invalid size")
        }

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

        output[0] = 2 * output[0]
        output[1] = 0.0f

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
    }

//    fun getFreq(idx : Int, dt : Float) : Float {
//        return idx / (dt * size)
//    }
}
