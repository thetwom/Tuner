package de.moekadu.tuner

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.roundToInt

class SampleDataTest {
    @Test
    fun fillInOneStep() {
        val sampleData = SampleData(5, 44100, 13)
        val input = FloatArray(14) {i -> 13f + i}

        sampleData.addData(13, input)
        assertTrue(sampleData.isFull)
        sampleData.data.forEachIndexed {i, v ->
            assertEquals(v.roundToInt(), i + sampleData.framePosition)
        }
    }

    @Test
    fun fillInOneStep2() {
        val sampleData = SampleData(5, 44100, 13)
        val input = FloatArray(20) {i -> i.toFloat()}

        sampleData.addData(0, input)
        assertTrue(sampleData.isFull)
        sampleData.data.forEachIndexed {i, v ->
            assertEquals(v.roundToInt(), i + sampleData.framePosition)
        }
    }

    @Test
    fun fillInThreeSteps1() {
        val sampleData = SampleData(5, 44100, 13)
        val input1 = FloatArray(10) {i -> i.toFloat()}
        val input2 = FloatArray(5) {i -> 10f + i}
        val input3 = FloatArray(5) {i -> 15f + i}

        sampleData.addData(0, input1)
        assertFalse(sampleData.isFull)
        sampleData.addData(10, input2)
        assertFalse(sampleData.isFull)
        sampleData.addData(15, input3)
        assertTrue(sampleData.isFull)
        sampleData.data.forEachIndexed {i, v ->
            assertEquals(v.roundToInt(), i + sampleData.framePosition)
        }
    }

    @Test
    fun fillLargerFirst() {
        val sampleData = SampleData(5,  44100, 13)
        val input1 = FloatArray(10) {i -> 15f + i}
        val input2 = FloatArray(15) {i -> i.toFloat()}

        sampleData.addData(15, input1)
        assertFalse(sampleData.isFull)
        sampleData.addData(0, input2)
        assertTrue(sampleData.isFull)
        sampleData.data.forEachIndexed {i, v ->
            assertEquals(v.roundToInt(), i + sampleData.framePosition)
        }
    }
}