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

import android.util.Log
import kotlin.math.*

/// Find local maxima based on the slope.
/**
 *  This function will additionally use the average value as threshold to consider a local maximum
 *  really as a local maximum.
 *  @param values Values where we search for the local maxima
 *  @param signalToNoiseRatio For each local maximum, we also search the neighboring minima.
 *    We only keep the local maximum if (local maximum) / max(neigbhoring minima) > signalToNoiseRatio
 *  @param fromIndex First index in the values array where we start search for maxima
 *  @param toIndex Position after the last index in the values where we search local maxima
 *  @return ArrayList with positions of local maxima in descending order.
 */

fun findLocalMaxima(values : FloatArray, signalToNoiseRatio : Float, fromIndex : Int = 0, toIndex : Int = values.size) : ArrayList<Int> {
  val SLOPE_INCREASING = true
  val SLOPE_DECREASING = false

  val maxima = ArrayList<Int>()

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
        maxima.add(localMaximumIndex)
      }
      leftLocalMinimumIndex = i
      if (leftLocalMinimumIndex >= toIndex-1)
        break
      slope = SLOPE_INCREASING
    }
  }

  maxima.sortWith(compareByDescending {values[it]})
  return maxima
}

/// Find maximum values of positive value sections in the values array
/**
 * This function determines the positive sections in the values-array and determines the maximum
 *   value for each of these positive sections.
 *   @param values Array with values for which the maxima should be found.
 *   @param fromIndex First value to be considered inside the values array
 *   @param toIndex Index after the last value to be considered.
 *   @return ArrayList with the maxima in descending order
 */
fun findMaximaOfPositiveSections(values : FloatArray, fromIndex : Int = 0, toIndex : Int = values.size) : ArrayList<Int> {
  val WAIT_FOR_POSITIVE_VALUES = true
  val FIND_MAXIMUM = false
  // val maxima = ArrayList<Int>((toIndex - fromIndex) / 2 + 1 )
  val maxima = ArrayList<Int>()

  var currentFindAction = if (values[0] >= 0) FIND_MAXIMUM else WAIT_FOR_POSITIVE_VALUES
  var currentExtremum = values[fromIndex]
  var currentExtremumIndex = fromIndex

  for (i in fromIndex+1 until toIndex) {
    if (currentFindAction == FIND_MAXIMUM && values[i] > currentExtremum) {
      currentExtremum = values[i]
      currentExtremumIndex = i
    }
    else if (currentFindAction == FIND_MAXIMUM && values[i] < 0f) {
      maxima.add(currentExtremumIndex)
      currentFindAction = WAIT_FOR_POSITIVE_VALUES
    }
    else if (currentFindAction == WAIT_FOR_POSITIVE_VALUES && values[i] >= 0f) {
      currentExtremum = values[i]
      currentExtremumIndex = i
      currentFindAction = FIND_MAXIMUM
    }
  }

  maxima.sortWith(compareByDescending {values[it]})
  return maxima
}
