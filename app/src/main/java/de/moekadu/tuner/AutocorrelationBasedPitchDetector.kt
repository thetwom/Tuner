package de.moekadu.tuner

import android.util.Log
import kotlin.math.*

class AutocorrelationBasedPitchDetectorPrep(val size : Int, private val dt : Float, private val minimumFrequency : Float, private val maximumFrequency : Float) {

  class Results(size : Int) {
    val correlation = FloatArray(size+1)
    val spectrum = FloatArray(2 * size + 2)
    var idxMaxPitch = 0
    var idxMaxFreq = 0
    //var numLocalMaxima = 0
    //val localMaxima = IntArray(NUM_MAX_HARMONIC)
  }

//  companion object {
//    const val NUM_MAX_HARMONIC = 3
//  }

  private val correlation = Correlation(size)
  private val localMaxima = IntArray(size/2+1)

  fun run(readBuffer : CircularRecordData.ReadBuffer, results : Results) {
    //Log.v("Tuner", "AutocorrelationBasedPitchDetectorPrep.run")
    require(readBuffer.size == size) { "Incorrect size for preprocessing autocorrelation based pitch detection" }
    require(results.correlation.size == size + 1) { "Incorrect size for preprocessing autocorrelation based pitch detection" }

    correlation.correlate(readBuffer, results.correlation, false, results.spectrum)

    val indexMinimumFrequency = (ceil((1.0f / minimumFrequency) / dt)).toInt()
    val indexMaximumFrequency = (ceil((1.0f / maximumFrequency) / dt)).toInt()

    // index of maximum frequency is the smaller index ..., we have to start from zero, otherwise its hard to interpret the first maximum
    val numLocalMaxima = findLocalMaximaPosNeg(results.correlation, 0, indexMinimumFrequency, localMaxima)

    //------------------------------------------------------------------------------------------
    // Find largest maximum
    //------------------------------------------------------------------------------------------
    var largestMaximum = 0f
    var pitchIndex = 0

    for (i in 0 until numLocalMaxima) {
      val currentMaximumIndex = localMaxima[i]
      if (currentMaximumIndex >= indexMaximumFrequency && results.correlation[currentMaximumIndex] > largestMaximum) {
        pitchIndex = currentMaximumIndex
        largestMaximum = results.correlation[currentMaximumIndex]
      }
    }

    results.idxMaxFreq = pitchIndex
    results.idxMaxPitch = results.idxMaxFreq
  }
}

class AutocorrelationBasedPitchDetectorPost(private val dt : Float, private val processingInterval : Int) {
  class Results {
        var frequency = 0.0f
    }

  fun run(preprocessingResults: Array<AutocorrelationBasedPitchDetectorPrep.Results?>,
          postprocessingResults: Results) {
//    Log.v("Tuner", "AutocorrelationBasedPitchDetectorPost.run")

    // This index defines the shift between two time series where the correlation is maximal
    val maxPitchIndex = preprocessingResults[0]?.idxMaxPitch ?: 0

    val spec1 = preprocessingResults[0]?.spectrum
    val spec2 = preprocessingResults[1]?.spectrum

    if(maxPitchIndex == 0 || spec1 == null || spec2 == null) {
      postprocessingResults.frequency = 0.0f
      return
    }

    // this is the frequency computed from the shift at the maximum correlation, however, this the frequency
    // resolution is not high enough for our needs.
    val approximateFrequency = 1.0f / (maxPitchIndex * dt)
    // this is the frequency resolution
    val df = 1.0f / (dt * (spec1.size-2))
    val freqIdx = (approximateFrequency / df).roundToInt()
    // by comparing the phase of two successive spectra we can increase the accuracy.
    postprocessingResults.frequency = increaseFrequencyAccuracy(spec1, spec2, freqIdx, df, processingInterval * dt)
  }
}