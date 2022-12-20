package de.moekadu.tuner

import de.moekadu.tuner.notedetection.OutlierRemovingSmoother
import de.moekadu.tuner.notedetection.OutlierRemovingSmoothingBuffer
import org.junit.Assert.*
import org.junit.Test

class OutlierRemovingSmootherTest {

    @Test
    fun testBuffer() {
        val buffer = OutlierRemovingSmoothingBuffer(3, 10f, 100f, 0.3f, 2)

        // out of bounds check 1
        var success = buffer.append(150f)
        assertFalse(success)
        assertEquals(0f, buffer.mean)
        assertEquals(1, buffer.numSuccessiveOutliers)
        assertEquals(0, buffer.size)

        // out of bounds check 2
        success = buffer.append(1f)
        assertFalse(success)
        assertEquals(0f, buffer.mean)
        assertEquals(2, buffer.numSuccessiveOutliers)
        assertEquals(0, buffer.size)

        // add first value 50
        success = buffer.append(50f)
        assertTrue(success)
        assertEquals(50f, buffer.mean)
        assertEquals(0, buffer.numSuccessiveOutliers)
        assertEquals(1, buffer.size)

        // mean of 50, 60
        success = buffer.append(60f)
        assertTrue(success)
        assertEquals(55f, buffer.mean, 1e-6f)
        assertEquals(0, buffer.numSuccessiveOutliers)
        assertEquals(2, buffer.size)

        // mean of 50, 60, 55
        success = buffer.append(55f)
        assertTrue(success)
        assertEquals(55f, buffer.mean, 1e-6f)
        assertEquals(0, buffer.numSuccessiveOutliers)
        assertEquals(3, buffer.size)

        // mean of 60, 55, 65
        success = buffer.append(65f)
        assertTrue(success)
        assertEquals(60f, buffer.mean, 1e-6f)
        assertEquals(0, buffer.numSuccessiveOutliers)
        assertEquals(3, buffer.size)

        // mean of 55, 65, 61
        success = buffer.append(61f)
        assertTrue(success)
        assertEquals((55f + 65f + 61f) / 3, buffer.mean, 1e-6f)
        assertEquals(0, buffer.numSuccessiveOutliers)
        assertEquals(3, buffer.size)

        // mean of 55, 65, 61  & outlier found
        success = buffer.append(15f)
        assertFalse(success)
        assertEquals((55f + 65f + 61f) / 3, buffer.mean, 1e-6f)
        assertEquals(1, buffer.numSuccessiveOutliers)
        assertEquals(3, buffer.size)

        // another outlier found
        success = buffer.append(95f)
        assertFalse(success)
        assertEquals((55f + 65f + 61f) / 3, buffer.mean, 1e-6f)
        assertEquals(2, buffer.numSuccessiveOutliers)
        assertEquals(3, buffer.size)

        // max outlier is 2, so now, the buffer must be cleared
        success = buffer.append(95f)
        assertFalse(success)
        assertEquals(0f, buffer.mean)
        assertEquals(0, buffer.numSuccessiveOutliers)
        assertEquals(0, buffer.size)
    }

    @Test
    fun smoother() {
        val smoother = OutlierRemovingSmoother(
            3,
            10f,
            100f,
            0.3f,
            2,
            2
        )

        assertEquals(0f, smoother.smoothedValue)

        // we defined that minimum are 2 values to smooth, only one available
        var smoothedValue = smoother(50f)
        assertEquals(0f, smoothedValue)

        // mean of 50, 60
        smoothedValue = smoother(60f)
        assertEquals(55f, smoothedValue)

        // mean of 50, 60, 55
        smoothedValue = smoother(55f)
        assertEquals(55f, smoothedValue, 1e-6f)

        // mean of 50, 60, 55, outlier 90
        smoothedValue = smoother(90f)
        assertEquals(0f, smoothedValue) // failed to add a value, 0f must be returned
        assertEquals(55f, smoother.smoothedValue) // we still have a valid value

        // mean of 50, 60, 55, outlier 90, 92
        smoothedValue = smoother(92f)
        assertEquals(0f, smoothedValue) // failed to add a value, 0f must be returned
        assertEquals(55f, smoother.smoothedValue) // we still have a valid value

        // mean 90, 92, 91
        smoothedValue = smoother(91f)
        assertEquals(91f, smoothedValue, 1e-6f)
        assertEquals(91f, smoother.smoothedValue) // we still have a valid value

        // mean 92, 91, 95 (outlier 20 must be ignored)
        smoother(20f)
        smoothedValue = smoother(95f)
        assertEquals((92f+91f+95f)/3, smoothedValue, 1e-6f)

        smoother(50f)
        smoother(22f)
        smoother(52f)
        smoothedValue = smoother(21f)
        assertEquals(21f, smoothedValue, 1e-6f)
    }
}