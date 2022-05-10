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

package de.moekadu.tuner.notedetection

import kotlin.math.max
import kotlin.math.min

/// Class which collects sample data.
/**
 * @param size Sample data buffer size
 * @param sampleRate Sample rate in Hertz
 * @param framePosition Position of first frame in data
 */
class SampleData(val size: Int, val sampleRate: Int, var framePosition: Int) {
    /// Here we store the data.
    val data = FloatArray(size)

    /// Smallest index in data, where data was written.
    private var minLevel = Int.MAX_VALUE
    /// Largest index in data, where data was written.
    private var maxLevel = 0

    /// Flag telling if our data buffer is completely filled.
    /** @note We only check that some data was written to min and max position of our data object. */
    val isFull
            get() = (maxLevel == size && minLevel == 0)

    fun reset(framePosition: Int) {
        this.framePosition = framePosition
        minLevel = Int.MAX_VALUE
        maxLevel = 0
    }

    /// Add some data to our data object.
    /**
     * @param inputFramePosition Frame position of first entry in input-array
     * @param input Input data, which should be copied to our local data object.
     */
    fun addData(inputFramePosition: Int, input: FloatArray) {
        val startIndexData = max(0, inputFramePosition - framePosition)
        val startIndexInput = max(0, framePosition - inputFramePosition)

        val numCopy = min(size - startIndexData, input.size - startIndexInput)
        if (numCopy > 0) {
            input.copyInto(data, startIndexData, startIndexInput, startIndexInput + numCopy)
            maxLevel = max(maxLevel, startIndexData + numCopy)
            if (numCopy > 0)
                minLevel = min(minLevel, startIndexData)
        }
        //Log.v("TestRecordFlow", "SampleData.addData: inputFramePosition = $inputFramePosition, input.size=${input.size}, numCopy=$numCopy, startIndexData=$startIndexData, startIndexInput=$startIndexInput")
        require(maxLevel <= size)
    }
}
