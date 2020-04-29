package de.moekadu.tuner

import android.util.Log
import kotlin.math.*

fun getClosestIntArrayIndex(element : Int, array : IntArray, fromIndex : Int = 0, toIndex : Int = array.size) : Int {
    var closestIndex = array.binarySearch(element, fromIndex, toIndex)

    if(closestIndex >= 0)
        return closestIndex
    else
        closestIndex = - (closestIndex + 1)
    //Log.v("Tuner", "BLBB: $closestIndex from $fromIndex to $toIndex")
    when {
      closestIndex == 0 -> return closestIndex
      closestIndex == toIndex -> return toIndex-1
      element - array[closestIndex-1] < array[closestIndex] - element -> return closestIndex-1
      else -> return closestIndex
    }
}


class FrequencyBasedPitchDetectorPrep(val size : Int, private val dt : Float, minimumFrequency : Float, maximumFrequency : Float) {

    class Results(size : Int) {
        val spectrum = FloatArray(size+2)
        val ampSpec = FloatArray(spectrum.size/2)
        var idxMaxFreq = 0
        var idxMaxPitch = 0
        var numLocalMaxima = 0
        val localMaxima = IntArray(NUM_MAX_HARMONIC)
    }

    companion object {
        const val NUM_MAX_HARMONIC = 3
    }

    private val fft = RealFFT(size, RealFFT.HAMMING_WINDOW)
    private val localMaximaSNR = 10.0f
    private val localMaxima = IntArray(size/2+1)
    private val markedLocalMaxima = IntArray(NUM_MAX_HARMONIC)

    /// Minimum index in transformed spectrum, which should be considered
    private val startIndex = RealFFT.closestFrequencyIndex(minimumFrequency, fft.size, dt)
    /// Maximum index in transformed spectrum, which should be considered (endIndex is included in range)
    private val endIndex = RealFFT.closestFrequencyIndex(maximumFrequency, fft.size, dt)

    fun run(readBuffer : CircularRecordData.ReadBuffer, results : Results) {

        require(readBuffer.size == size)
        require(results.spectrum.size == size+2)

        //------------------------------------------------------------------------------------------
        // Transform data zu frequency domain
        //------------------------------------------------------------------------------------------
        fft.fft(readBuffer, results.spectrum)

        //------------------------------------------------------------------------------------------
        // Create amplitude spectrum and index where spectrum amplitude is maximal
        //------------------------------------------------------------------------------------------
        results.idxMaxFreq = 0
        var maxAmp = 0.0f
        val ampSpec = results.ampSpec

        for(i in ampSpec.indices)
            ampSpec[i] = results.spectrum[2*i].pow(2) + results.spectrum[2*i+1].pow(2)

        for(i in startIndex until endIndex) {
            if(ampSpec[i] > maxAmp){
                maxAmp = ampSpec[i]
                results.idxMaxFreq = i
            }
        }

        //------------------------------------------------------------------------------------------
        // Find local maxima
        //------------------------------------------------------------------------------------------
        val numLocalMaxima = findLocalMaxima(ampSpec, localMaximaSNR, startIndex, endIndex+1, localMaxima)

        //------------------------------------------------------------------------------------------
        // We have found all indices of our local maxima, now we have choose a pitch
        //------------------------------------------------------------------------------------------
        if(numLocalMaxima > 0) {
            // Find the index of the largest local maximum, this will always be part of our pitch computation
            var largestLocalMaximum = localMaxima[0]
            for (i in 1 until numLocalMaxima) {
                if (ampSpec[localMaxima[i]] > ampSpec[largestLocalMaximum])
                    largestLocalMaximum = localMaxima[i]
            }

            if (largestLocalMaximum != results.idxMaxFreq)
                Log.v(
                    "Tuner",
                    "PreprocessorThread.preprocessData : largestLocalMaxima($largestLocalMaximum) != idxMaxFreq(" + results.idxMaxFreq + ")"
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
                        results.localMaxima,
                        0,
                        0,
                        numMarkedLocalMaxima
                    )
                    results.numLocalMaxima = numMarkedLocalMaxima
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
            results.idxMaxPitch = pitchIndex
            //Log.v("Tuner", "PreprocessorThread.preprocessData : idxMaxFreq=" + preprocessingResults.idxMaxFreq + " idxMaxPitch=" + preprocessingResults.idxMaxPitch)
            Log.v(
                "Tuner",
                "PreprocessorThread.preprocessData : numLocalMaxima=$numLocalMaxima, marked=" + results.numLocalMaxima
            )

//        var numLocalMaximaNew = 0
//        for(i in 0 .. preprocessingResults.numLocalMaxima) {
//            val harmonicFP = preprocessingResults.localMaxima[i] / approximatePitchIndexFloat
//            val harmonicRound = harmonicFP.roundToInt()
//            if(abs(harmonicFP-harmonicRound)/harmonicFP < 0.01f ||  )
//        }
        }
        else {
            results.numLocalMaxima = 1
            results.localMaxima[0] = results.idxMaxFreq
            results.idxMaxPitch = results.idxMaxFreq
        }
    }
}

class FrequencyBasedPitchDetectorPost(private val dt : Float, private val processingInterval : Int) {
    class Results {
        var frequency = 0.0f
    }

    fun run(preprocessingResults: Array<FrequencyBasedPitchDetectorPrep.Results?>,
            postprocessingResults: Results) {
        //Log.v("Tuner", "PostprocessorThread:postprocessData")
        val freqIdx = preprocessingResults[0]?.idxMaxPitch ?: 0
        val spec1 = preprocessingResults[0]?.spectrum
        val spec2 = preprocessingResults[1]?.spectrum

        if(freqIdx == 0 || spec1 == null || spec2 ==  null) {
            postprocessingResults.frequency = 0f
            return
        }

        // we need the fft input size for computing df, spec1 is the output size, which is two entries larger.
        val df = 1.0f/ (dt * (spec1.size-2))
        postprocessingResults.frequency = increaseFrequencyAccuracy(spec1, spec2, freqIdx, df, processingInterval * dt)
    }
}