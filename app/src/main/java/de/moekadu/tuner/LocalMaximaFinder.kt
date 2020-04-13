package de.moekadu.tuner

import android.util.Log
import kotlin.math.*

/// This function will additionally use the average value as threshold to treat consider a local maximum really as a local maximum
fun findLocalMaxima(values : FloatArray, signalToNoiseRatio : Float, fromIndex : Int = 0, toIndex : Int = values.size, maxima : IntArray) : Int {
  val SLOPE_INCREASING = true
  val SLOPE_DECREASING = false
  require(maxima.size >= (toIndex - fromIndex) / 2 + 1 )

  //------------------------------------------------------------------------------------------
  // Find average value of our spectrum
  //------------------------------------------------------------------------------------------
  var avgValue = 0.0f
  for(i in fromIndex until toIndex)
    avgValue += values[i] // ampSpec[i].pow(2)
  //avgValue = sqrt(avgValue / (endIndex-startIndex+1))
  avgValue /= (toIndex - fromIndex)

  // Find local minimum left of our starting value
  var leftLocalMinimumIndex = fromIndex
  if(values[fromIndex+1] < values[fromIndex]) {
    for(i in fromIndex+2 until toIndex) {
      if(values[i] > values[i-1]) {
        leftLocalMinimumIndex = i-1
        break
      }
    }
  }
  else {
    for(i in fromIndex downTo 1){
      if(values[i-1] > values[i]) {
        leftLocalMinimumIndex = i
        break
      }
    }
  }
  // correct the start index such that we start at left local minimum
  val startIndexLocalMaxima = max(fromIndex, leftLocalMinimumIndex)
  var slope = SLOPE_INCREASING
  var localMaximumIndex = 0
  var numLocalMaxima = 0

  for(i in startIndexLocalMaxima until values.size-2 ) {
    // We found a maximum, so store its position
    if (slope == SLOPE_INCREASING && values[i + 1] < values[i]) {
      localMaximumIndex = i
      slope = SLOPE_DECREASING
    }
    // We found a local minimum, that means that we now can evaluate our latest stored local maximum
    else if (slope == SLOPE_DECREASING && values[i + 1] > values[i]) {
      // Signal to noise ratio is based on the larger local minimum (the minimum left or right to our maximum)
      val largerMinimum = max(values[leftLocalMinimumIndex], values[i])
      // Here is the condition when we consider a local maximum worth to be stored
      if (values[localMaximumIndex] >= largerMinimum * signalToNoiseRatio && values[localMaximumIndex] > avgValue) {
        maxima[numLocalMaxima] = localMaximumIndex
        ++numLocalMaxima
      }
      leftLocalMinimumIndex = i
      if (leftLocalMinimumIndex >= toIndex-1)
        break
      slope = SLOPE_INCREASING
    }
  }
  return numLocalMaxima
}

fun findLocalMaximaPosNeg(values : FloatArray, fromIndex : Int = 0, toIndex : Int = values.size, maxima : IntArray) : Int {
  val WAIT_FOR_POSITIVE_VALUES = true
  val FIND_MAXIMUM = false
  require(maxima.size >= (toIndex - fromIndex) / 2 + 1 )

  var numLocalMaxima = 0

  var currentFindAction = if (values[0] >= 0) FIND_MAXIMUM else WAIT_FOR_POSITIVE_VALUES
  var currentExtremum = values[fromIndex]
  var currentExtremumIndex = fromIndex

  for (i in fromIndex+1 until toIndex) {
    if (currentFindAction == FIND_MAXIMUM && values[i] > currentExtremum) {
      currentExtremum = values[i]
      currentExtremumIndex = i
    }
    else if (currentFindAction == FIND_MAXIMUM && values[i] < 0f) {
      maxima[numLocalMaxima] = currentExtremumIndex
      ++numLocalMaxima
      currentFindAction = WAIT_FOR_POSITIVE_VALUES
    }
    else if (currentFindAction == WAIT_FOR_POSITIVE_VALUES && values[i] >= 0f) {
      currentExtremum = values[i]
      currentExtremumIndex = i
      currentFindAction = FIND_MAXIMUM
    }
  }

  return numLocalMaxima
}