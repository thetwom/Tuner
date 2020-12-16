package de.moekadu.tuner

import android.util.Log
import kotlin.math.max
import kotlin.math.min

/// Class which collects sample data.
/**
 * @param size Sample data buffer size
 * @param framePosition Position of first frame in data
 */
class SampleData(val size: Int, val framePosition: Int, val sampleRate: Int) {
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
