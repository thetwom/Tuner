package de.moekadu.tuner

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import kotlin.math.*

fun getClosestIntArrayIndex(element : Int, array : IntArray, fromIndex : Int = 0, toIndex : Int = array.size) : Int {
    var closestIndex = array.binarySearch(element, fromIndex, toIndex)

    if(closestIndex >= 0)
        return closestIndex
    else
        closestIndex = - (closestIndex + 1)
    //Log.v("Tuner", "BLBB: $closestIndex from $fromIndex to $toIndex")
    if(closestIndex == 0)
        return closestIndex
    else if(closestIndex == toIndex)
        return toIndex-1
    else if(element - array[closestIndex-1] < array[closestIndex] - element)
        return closestIndex-1
    return closestIndex
}

/// Preprocess a time spectrum
/**
 * @param size Number of audio data points which are processed here
 * @param dt Time step width between to two input data points
 * @param minimumFrequency Minimum allowed frequency which we sould detect
 * @param maximumFrequency Maximum allowd frequency which we should detect
 * @param uiHandler handler of ui thread, where we send oure results
 */
class PreprocessorThread(val size : Int, val dt : Float, val minimumFrequency : Float, val maximumFrequency : Float,
                         private val uiHandler : Handler) : HandlerThread("Tuner:PreprocessorThread") {
    lateinit var handler: Handler

    private val fft = RealFFT(size, RealFFT.HAMMING_WINDOW)
    private val localMaximaSNR = 5.0f
    val localMaxima = IntArray(size/2)
    val markedLocalMaxima = IntArray(NUM_MAX_HARMONIC)

    /// Minimum index in transformed spectrum, which should be considered
    private val startIndex = RealFFT.closestFrequencyIndex(minimumFrequency, fft.size, dt)
    /// Maximum index in transformed spectrum, which should be considered (endIndex is included in range)
    private val endIndex = RealFFT.closestFrequencyIndex(maximumFrequency, fft.size, dt)

    companion object {
        const val PREPROCESS_AUDIO = 200001
        const val PREPROCESSING_FINISHED = 200002
        const val NUM_MAX_HARMONIC = 3
        private const val SLOPE_INCREASING = true
        private const val SLOPE_DECREASING = false
    }

    class PreprocessingResults(size : Int) {
        var maxValue = 0.0f
        val spectrum = FloatArray(size)
        val ampSpec = FloatArray(size/2)
        var idxMaxFreq = 0
        var idxMaxPitch = 0
        var numLocalMaxima = 0
        val localMaxima = IntArray(NUM_MAX_HARMONIC)
    }

    class ReadBufferAndProcessingResults(val readBuffer: CircularRecordData.ReadBuffer, val preprocessingResults: PreprocessingResults)

    override fun onLooperPrepared() {
        super.onLooperPrepared()

        handler = object : Handler(looper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                if (msg.what == PREPROCESS_AUDIO) {
                    //                    Log.v("Tuner", "test")
                    when (val obj = msg.obj) {
                        is ReadBufferAndProcessingResults -> preprocessData(obj)
                    }
                }
            }
        }
    }

    private fun preprocessData(readBufferAndProcessingResults: ReadBufferAndProcessingResults) {
        val readBuffer = readBufferAndProcessingResults.readBuffer
        val preprocessingResults = readBufferAndProcessingResults.preprocessingResults

        require(readBuffer.size == size)
        require(preprocessingResults.spectrum.size == size)

        //------------------------------------------------------------------------------------------
        // Find maximum value in audio buffer
        //------------------------------------------------------------------------------------------
        preprocessingResults.maxValue = 0.0f
        for(i in 0 until readBuffer.size)
            preprocessingResults.maxValue = max(preprocessingResults.maxValue, readBuffer[i].absoluteValue)
        //val processingResults = ProcessingResults(tmpData.max() ?: 0.0f, spectrum)

        //------------------------------------------------------------------------------------------
        // Transform data zu frequency domain
        //------------------------------------------------------------------------------------------
        fft.fft(readBuffer, preprocessingResults.spectrum)

        //------------------------------------------------------------------------------------------
        // Create amplitude spectrum and index where spectrum amplitude is maximal
        //------------------------------------------------------------------------------------------
        preprocessingResults.idxMaxFreq = 0
        var maxAmp = 0.0f
        val ampSpec = preprocessingResults.ampSpec

        for(i in startIndex until endIndex) {
            val amp = preprocessingResults.spectrum[2*i].pow(2) + preprocessingResults.spectrum[2*i+1].pow(2)
            if(amp > maxAmp){
                maxAmp = amp
                preprocessingResults.idxMaxFreq = i
            }
            ampSpec[i] = amp
        }

        //------------------------------------------------------------------------------------------
        // Find average value of our spectrum
        //------------------------------------------------------------------------------------------
        require(startIndex < ampSpec.size-2)
        var avgValue = 0.0f
        for(i in startIndex .. endIndex)
            avgValue += ampSpec[i] // ampSpec[i].pow(2)
        //avgValue = sqrt(avgValue / (endIndex-startIndex+1))
        avgValue /= (endIndex-startIndex+1)

        //------------------------------------------------------------------------------------------
        // Find all local maxima in our spectrum which have a signal to noise ratio greater than
        //   localMaximaSNR and are greater than the average
        //--------------------------------------------------
        // Find local minium left of our starting value
        var leftLocalMinimumIndex = startIndex
        if(ampSpec[startIndex+1] < ampSpec[startIndex]) {
            for(i in startIndex+2 .. endIndex) {
                if(ampSpec[i] > ampSpec[i-1]) {
                    leftLocalMinimumIndex = i-1
                    break
                }
            }
        }
        else {
            for(i in startIndex downTo 1){
                if(ampSpec[i-1] > ampSpec[i]) {
                    leftLocalMinimumIndex = i
                    break
                }
            }
        }
        // correct the start index such that we start at left local minimum
        val startIndexLocalMaxima = max(startIndex, leftLocalMinimumIndex)
        var slope = SLOPE_INCREASING
        var localMaximumIndex = 0
        var numLocalMaxima = 0

        for(i in startIndexLocalMaxima until ampSpec.size-2 ) {
            // We found a maximum, so store its position
            if (slope == SLOPE_INCREASING && ampSpec[i + 1] < ampSpec[i]) {
                localMaximumIndex = i
                slope = SLOPE_DECREASING

            }
            // We found a local minimum, that means that we now can evaluate our latest stored local maximum
            else if (slope == SLOPE_DECREASING && ampSpec[i + 1] > ampSpec[i]) {
                // Signal to noise ratio is based on the larger local minimum (the minimum left or right to our maximum)
                val largerMinimum = max(ampSpec[leftLocalMinimumIndex], ampSpec[i])
                // Here is the condition when we consider a local maximum worth to be stored
                if (ampSpec[localMaximumIndex] >= largerMinimum * localMaximaSNR && ampSpec[localMaximumIndex] > avgValue) {
                    localMaxima[numLocalMaxima] = localMaximumIndex
                    ++numLocalMaxima
                }
                leftLocalMinimumIndex = i
                if (leftLocalMinimumIndex >= endIndex)
                    break
                slope = SLOPE_INCREASING
            }
        }

        //------------------------------------------------------------------------------------------
        // We have found all inidices of our local maxima, now we have choose a pitch
        //------------------------------------------------------------------------------------------
        if(numLocalMaxima > 0) {
            // Find the index of the largest local maximum, this will always be part of our pitch computation
            var largestLocalMaximum = localMaxima[0]
            for (i in 1 until numLocalMaxima) {
                if (ampSpec[localMaxima[i]] > ampSpec[largestLocalMaximum])
                    largestLocalMaximum = localMaxima[i]
            }

            if (largestLocalMaximum != preprocessingResults.idxMaxFreq)
                Log.v(
                    "Tuner",
                    "PreprocessorThread.preprocessData : largestLocalMaxima($largestLocalMaximum) != idxMaxFreq(" + preprocessingResults.idxMaxFreq + ")"
                )

            Log.v(
                "Tuner",
                "PreprocessorThread.preprocessData: frequency of max local maximum=" + RealFFT.getFrequency(
                    largestLocalMaximum,
                    fft.size,
                    dt
                )
            )
            var maximaSum = 0.0f
            var maximaSumHarmonic = 1

            for (iHarmonic in 1..NUM_MAX_HARMONIC) {
                var currentMaximaSum = 0.0f
                var numMarkedLocalMaxima = 0

                for (harmonicCounter in 1..NUM_MAX_HARMONIC) {
                    val theoreticalHarmonicIndex =
                        ((harmonicCounter * largestLocalMaximum) / iHarmonic.toFloat()).roundToInt()
                    val closestIndexInLocalMaxima = getClosestIntArrayIndex(
                        theoreticalHarmonicIndex,
                        localMaxima,
                        0,
                        numLocalMaxima
                    )
                    val potentialHarmonicIndex = localMaxima[closestIndexInLocalMaxima]
                    val difference =
                        (potentialHarmonicIndex - theoreticalHarmonicIndex).absoluteValue

                    Log.v(
                        "Tuner", "PreprocessorThread.preprocessData : "
                                + "numLocalMaxima=$numLocalMaxima "
                                + "index of largestLocalMaximum=$largestLocalMaximum "
                                + "theoreticalHarmonicIndex=$theoreticalHarmonicIndex (f="
                                + RealFFT.getFrequency(theoreticalHarmonicIndex, fft.size, dt)
                                + "), potentialHarmonicIndex=$potentialHarmonicIndex, (f="
                                + RealFFT.getFrequency(potentialHarmonicIndex, fft.size, dt)
                                + "), closestIndexInLocalMaxima=$closestIndexInLocalMaxima"
                    )

                    if (iHarmonic == harmonicCounter) {
                        require(difference == 0) { "Something when wrong with search the maxima index" }
                    }

                    //if(searchIndex>1000)
                    //    throw RuntimeException("blub")
//                Log.v("Tuner", "PreprocessorThread.preprocessData : blub " + (abs(closestIndex-searchIndex) / searchIndex.toFloat()))

                    // The index of the potential harmonic must be close enough to the index ouf the therotical harmonic
                    if (difference <= 1 || difference / theoreticalHarmonicIndex.toFloat() < 0.02f) {
                        // we define a slight weight when adding the energies, such that we prefer smaller harmonic
                        val weight = 0.9f.pow(harmonicCounter - 1)
                        currentMaximaSum += weight * ampSpec[potentialHarmonicIndex]

                        // add also the neighbors of the local maximum to our sum, since this might give a more robust result
                        if (potentialHarmonicIndex > 0)
                            currentMaximaSum += weight * ampSpec[potentialHarmonicIndex - 1]
                        if (potentialHarmonicIndex < ampSpec.size - 1)
                            currentMaximaSum += weight * ampSpec[potentialHarmonicIndex + 1]

                        markedLocalMaxima[numMarkedLocalMaxima] = potentialHarmonicIndex
                        ++numMarkedLocalMaxima
                    }
                }

                // Store the chosen local maxima if the containing energy is the largest
                if (currentMaximaSum > maximaSum) {
                    maximaSum = currentMaximaSum
                    maximaSumHarmonic = iHarmonic
                    markedLocalMaxima.copyInto(
                        preprocessingResults.localMaxima,
                        0,
                        0,
                        numMarkedLocalMaxima
                    )
                    preprocessingResults.numLocalMaxima = numMarkedLocalMaxima
                    Log.v(
                        "Tuner",
                        "PreprocessorThread.preprocessData : numMarkedLocalMaxima=$numMarkedLocalMaxima, markedLocalMaxima[0]=" + markedLocalMaxima[0]
                    )
                    //localMaxima.copyInto(preprocessingResults.localMaxima)
                    //preprocessingResults.numLocalMaxima = numLocalMaxima
                }
            }

            // Determine the pitch based on our previously chosen local maxima
            // Note, that the pitch might be not part of our previously found local maxima do to the search conditions
            //  still we assume, that we have at least a small local maxima at the pitch frequency, and thats what we
            //  are search for here
            val approximatePitchIndexFloat = largestLocalMaximum / maximaSumHarmonic.toFloat()
            val approximatePitchIndex = approximatePitchIndexFloat.roundToInt()
            var pitchIndex = approximatePitchIndex
            for (i in approximatePitchIndex + 1 until ampSpec.size) {
                if (ampSpec[i] > ampSpec[i - 1])
                    pitchIndex = i
                else
                    break
            }
            for (i in approximatePitchIndex - 1 downTo 0) {
                if (ampSpec[i] > ampSpec[i + 1] && ampSpec[i] > ampSpec[pitchIndex]) {
                    pitchIndex = i
                } else if (ampSpec[i] <= ampSpec[i + 1])
                    break
            }
            preprocessingResults.idxMaxPitch = pitchIndex
            //Log.v("Tuner", "PreprocessorThread.preprocessData : idxMaxFreq=" + preprocessingResults.idxMaxFreq + " idxMaxPitch=" + preprocessingResults.idxMaxPitch)
            Log.v(
                "Tuner",
                "PreprocessorThread.preprocessData : numLocalMaxima=$numLocalMaxima, marked=" + preprocessingResults.numLocalMaxima
            )

//        var numLocalMaximaNew = 0
//        for(i in 0 .. preprocessingResults.numLocalMaxima) {
//            val harmonicFP = preprocessingResults.localMaxima[i] / approximatePitchIndexFloat
//            val harmonicRound = harmonicFP.roundToInt()
//            if(abs(harmonicFP-harmonicRound)/harmonicFP < 0.01f ||  )
//        }
        }
        else {
            preprocessingResults.numLocalMaxima = 1
            preprocessingResults.localMaxima[0] = preprocessingResults.idxMaxFreq
            preprocessingResults.idxMaxPitch = preprocessingResults.idxMaxFreq
        }

        val message =
            uiHandler.obtainMessage(PREPROCESSING_FINISHED, readBufferAndProcessingResults)
        uiHandler.sendMessage(message)
    }
}