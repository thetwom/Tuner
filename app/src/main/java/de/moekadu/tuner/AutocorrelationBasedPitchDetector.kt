package de.moekadu.tuner

import android.util.Log
import kotlin.math.*

class AutocorrelationBasedPitchDetectorPrep(val size : Int, private val dt : Float, private val minimumFrequency : Float, private val maximumFrequency : Float) {

  class Results(size : Int) {
    val correlation = FloatArray(size+1)
    var idxMaxPitch = 0
    var idxMaxFreq = 0
    var numLocalMaxima = 0
    val localMaxima = IntArray(NUM_MAX_HARMONIC)
  }

  companion object {
    const val NUM_MAX_HARMONIC = 3
  }

  private val correlation = Correlation(size)
  private val localMaxima = IntArray(size/2+1)
  private val signalToNoiseRatio = 10.0f

  fun run(readBuffer : CircularRecordData.ReadBuffer, results : Results) {
    Log.v("Tuner", "AutocorrelationBasedPitchDetectorPrep.run")
    require(readBuffer.size == size) { "Incorrect size for preprocessing autocorrelation based pitch detection" }
    require(results.correlation.size == size + 1) { "Incorrect size for preprocessing autocorrelation based pitch detection" }

    correlation.correlate(readBuffer, results.correlation)

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

class AutocorrelationBasedPitchDetectorPost(private val dt : Float) {
  class Results {
        var frequency = 0.0f
    }

  fun run(preprocessingResults: Array<AutocorrelationBasedPitchDetectorPrep.Results?>,
          postprocessingResults: Results) {
    Log.v("Tuner", "AutocorrelationBasedPitchDetectorPost.run")
    postprocessingResults.frequency = 0.0f
    //val corr = preprocessingResults[0]?.correlation
    preprocessingResults.last()?.let { prep ->
      val maxPitchIndex = prep.idxMaxPitch
      postprocessingResults.frequency = 1.0f / (maxPitchIndex * dt)
    }
  }
}