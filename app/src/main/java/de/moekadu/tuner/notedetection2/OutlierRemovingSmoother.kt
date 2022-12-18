package de.moekadu.tuner.notedetection2

import kotlin.math.absoluteValue
import kotlin.math.min

/** Buffer which is used for smoothing.
 * @param maxSize Maximum number of values, which can be stored in the buffer.
 * @param minValue Minimum allowed value to be accepted in the buffer.
 * @param maxValue Maximum allowed value to be accepted in the buffer.
 * @param maxRelativeDeviation Maximum relative deviation from the mean value, that a new value
 *   is accepted in the buffer.
 * @param maxNumSuccessiveOutliers Number of successive outliers which are accepted. If there
 *   are more outliers, the buffer will be cleared.
 */
class OutlierRemovingSmoothingBuffer(
    private val maxSize: Int,
    private val minValue: Float,
    private val maxValue: Float,
    private val maxRelativeDeviation: Float,
    private val maxNumSuccessiveOutliers: Int) {

    /** Underlying memory to store values. */
    private val values = FloatArray(maxSize)
    /** Index in values-array, which refers to index 0 in our buffer class. */
    private var indexZero = 0
    /** Counter for counting the number of successive outliers. */
    var numSuccessiveOutliers = 0
        private set
    /** Current number of values stored in the buffer. */
    var size = 0
        private set
    /** Current mean value of the added values or 0f if there are no values. */
    var mean = 0f
        private set

    /** Delete all buffer values and reset the mean value to 0f. */
    fun clear() {
        indexZero = 0
        size = 0
        mean = 0f
        numSuccessiveOutliers = 0
    }

    /** Append a new value to the buffer.
     * If the given value does not match the constraints (smaller than minValue,
     *   larger than maxValue or it deviates from the mean value to much), it will be
     *   added to the buffer.
     *  @param value Value to be added to the buffer.
     *  @return True, if the value was added, else false.
     */
    fun append(value: Float): Boolean {
        if (maxSize == 0)
            return false

//        Log.v("Tuner", "OutlierRemovingSmootherBuffer.append: value=$value, min=$minValue, max=$maxValue, deviation=${computeDeviation(value)}, maxRelativeDeviation=$maxRelativeDeviation}")
        return if (value < minValue || value > maxValue || computeDeviation(value) > maxRelativeDeviation){
//            Log.v("Tuner", "OutlierRemovingSmootherBuffer.append: incrementOutlierCount")
            incrementOutlierCount()
            false
        } else {
            addValueToBuffer(value)
            numSuccessiveOutliers = 0
            mean = computeMean()
//            Log.v("Tuner", "OutlierRemovingSmootherBuffer.append: addValue, mean=$mean")
            true
        }
    }

    /** Increment outlier count
     * This is the same as calling append() with a value which doesn't match the constraints.
     */
    fun incrementOutlierCount() {
        numSuccessiveOutliers += 1
        if (numSuccessiveOutliers > maxNumSuccessiveOutliers)
            clear()
    }

    /** Get buffer value at given index.
     * @param index Index (must be 0 <= index < size)
     * @return Value at given index.
     */
    private fun get(index: Int): Float {
        require(index < size)
        return values[(indexZero + index) % maxSize]
    }

    /** Add value to the buffer without checking the constraints.
     * This is different to append(), which does check the constraints.
     * @param value Value which will be added to the buffer.
     */
    private fun addValueToBuffer(value: Float) {
        if (maxSize == 0)
            return
        val index = (indexZero + size) % maxSize
        values[index] = value
        val newSize = size + 1
        if (newSize == maxSize + 1) {
            size = maxSize
            indexZero = (indexZero + 1) % maxSize
        } else {
            size = newSize
        }
    }

    /** Compute the mean value of the buffer values.
     * @return Mean value.
     */
    private fun computeMean(): Float {
        require(size > 0)
        var sum = 0f
        for (i in 0 until size)
            sum += get(i)
        return sum / size
    }

    /** Compute the relative deviation of a value from the current mean value.
     * @param value Value for which the deviation should be computed.
     * @return Relative deviation from the mean or 0f if the buffer is empty.
     */
    private fun computeDeviation(value: Float): Float {
        return if (size == 0)
            0f
        else
            (value - mean).absoluteValue / mean.absoluteValue
    }
}

/** Smooth data online.
 * @param size Number of values over which we compute the smoothed value.
 * @param minValue Values below this value will be taken as an outlier.
 * @param maxValue Values above this value will be taken as an outlier.
 * @param relativeDeviationToBeAnOutlier Maximum relative deviation of a value from the mean value
 *   to be not an outlier.
 * @param maxNumSuccessiveOutliers If more than the given number of values are outliers, we will
 *   clear the previous values.
 * @param minNumValuesForValidMean Minimum number of values for computing a smoothed value. This
 *   is different to "size" which is the "ideal" number of values.
 * @param numBuffers Number of buffers which collect values in parallel. So, if there is an outlier
 *   for one buffer, the other buffers can take it and start a new mean value computation.
 */
class OutlierRemovingSmoother(
    val size: Int,
    private val minValue: Float,
    private val maxValue: Float,
    private val relativeDeviationToBeAnOutlier: Float = 0.1f,
    private val maxNumSuccessiveOutliers: Int = 1,
    minNumValuesForValidMean: Int = 2,
    numBuffers: Int = 3) {

    private val minNumValuesForValidMean = min(minNumValuesForValidMean, size)
    /** Buffers which compute the mean value. */
    private val buffers = Array(numBuffers) {
        OutlierRemovingSmoothingBuffer(size, minValue, maxValue, relativeDeviationToBeAnOutlier, maxNumSuccessiveOutliers)
    }

    /** Current smoothed value or 0f if there is no value. */
    var smoothedValue = 0f

    /** Add a value for smoothing.
     * @param value Value to be added
     * @return The new smoothed value or 0f if the value is an outlier such that there is no new
     *   smoothed value.
     */
    operator fun invoke(value: Float): Float {
//        Log.v("Tuner", "OutlierRemovingSmoother.invoke: value=$value")
        var valueAppendedSuccessfully = false

        for (buffer in buffers) {
            if (valueAppendedSuccessfully)
                buffer.incrementOutlierCount()
            else
                valueAppendedSuccessfully = buffer.append(value)
        }
//        Log.v("Tuner", "OutlierRemovingSmoother.invoke: buffers[0].size=${buffers[0].size}, buffers[1].size=${buffers[1].size}")
//        Log.v("Tuner", "OutlierRemovingSmoother.invoke: buffers[0].size=${buffers[0].size}, minNumValuesForValidMean=$minNumValuesForValidMean, maxSize=$size")
        if (buffers[0].size < minNumValuesForValidMean)
            buffers.sortByDescending { it.size }

        return if (buffers[0].numSuccessiveOutliers == 0 && buffers[0].size >= minNumValuesForValidMean) {
            smoothedValue = buffers[0].mean
            smoothedValue
        } else {
            0f
        }
    }
}