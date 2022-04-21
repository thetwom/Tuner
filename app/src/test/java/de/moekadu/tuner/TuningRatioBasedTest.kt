package de.moekadu.tuner

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class TuningRatioBasedTest {

    private fun testOctImpl(
        ratios: DoubleArray,
        referenceFrequency: Float,
        noteIndexAtReferenceFrequency: Int,
        rootNoteIndex: Int
    ) {
        val numNotesPerOctave = ratios.size - 1
        val octRatio = (ratios.last() / ratios.first()).toFloat()
        val tuning = TuningRatioBased(
            Tuning.EDO12, // not important here
            ratios,
            rootNoteIndex,
            noteIndexAtReferenceFrequency,
            referenceFrequency,
            referenceFrequency / 4.0f - 0.001f,
            2 * referenceFrequency + 0.001f
        )

        assertEquals(
            tuning.getNoteFrequency(noteIndexAtReferenceFrequency),
            referenceFrequency,
            1e-12f
        )
        assertEquals(
            tuning.getNoteFrequency(noteIndexAtReferenceFrequency + numNotesPerOctave),
            referenceFrequency * octRatio,
            1e-12f
        )
        assertEquals(
            tuning.getNoteFrequency(noteIndexAtReferenceFrequency - numNotesPerOctave),
            referenceFrequency / octRatio,
            1e-12f
        )
        assertEquals(
            tuning.getNoteFrequency(noteIndexAtReferenceFrequency - 2 * numNotesPerOctave),
            referenceFrequency / octRatio.pow(2),
            1e-12f
        )
        assertEquals(
            tuning.getToneIndexBegin(),
            noteIndexAtReferenceFrequency - 2 * numNotesPerOctave
        )
        assertEquals(
            noteIndexAtReferenceFrequency + numNotesPerOctave + 1,
            tuning.getToneIndexEnd()
        )
    }

    @Test
    fun testOct() {
        testOctImpl(doubleArrayOf(0.5, 0.6, 0.7, 0.8, 1.0), 43.4f, 0, 0)
        testOctImpl(doubleArrayOf(1.0, 1.2, 1.4, 1.5, 1.6, 1.7, 2.0), 100f, 0, 0)
        testOctImpl(doubleArrayOf(1.0, 1.2, 1.4, 1.5, 1.6, 1.7, 2.0), 100f, 0, 1)
        testOctImpl(doubleArrayOf(1.0, 1.2, 1.4, 1.5, 1.6, 1.7, 2.0), 100f, 0, 4)
        testOctImpl(doubleArrayOf(1.0, 1.2, 1.4, 1.5, 1.6, 1.7, 2.0), 100f, -3, 0)
        testOctImpl(doubleArrayOf(1.0, 1.2, 1.4, 1.5, 1.6, 1.7, 2.0), 100f, 4, 0)
        testOctImpl(doubleArrayOf(1.0, 1.2, 1.4, 1.5, 1.6, 1.7, 2.0), 100f, 1, 2)
        testOctImpl(doubleArrayOf(1.0, 1.2, 1.4, 1.5, 1.6, 1.7, 2.0), 100f, -2, 3)
        testOctImpl(doubleArrayOf(1.0, 1.2, 1.4, 1.5, 1.7, 2.0), 130f, 4, 2)
    }

    private fun testRatiosImpl(
        ratios: DoubleArray,
        referenceFrequency: Float,
        noteIndexAtReferenceFrequency: Int,
        rootNoteIndex: Int,
        testNoteIndex: Int
    ) {
        val tuning = TuningRatioBased(
            Tuning.EDO12, // not important here
            ratios,
            rootNoteIndex,
            noteIndexAtReferenceFrequency,
            referenceFrequency,
            referenceFrequency / 100,
            100 * referenceFrequency
        )

        val noteIndex = testNoteIndex
        val freq = tuning.getNoteFrequency(noteIndex)
        assertEquals(noteIndex, tuning.getClosestToneIndex(freq))

        val noteIndex2 = noteIndex + 1
        val freq2 = tuning.getNoteFrequency(noteIndex2)
        assertEquals(noteIndex2, tuning.getClosestToneIndex(freq2))

        val freqBetween = freq * (freq2 / freq).pow(0.5f)
        assertEquals(noteIndex, tuning.getClosestToneIndex(freqBetween - 0.001f))
        assertEquals(noteIndex2, tuning.getClosestToneIndex(freqBetween + 0.001f))

        val noteIndexF = noteIndex + 0.3f
        val freqF = tuning.getNoteFrequency(noteIndexF)
        assertEquals(noteIndexF, tuning.getToneIndex(freqF), 1e-6f)
    }

    @Test
    fun testRatios() {
        testRatiosImpl(doubleArrayOf(1.0, 1.3, 1.6, 1.9, 2.0), 100f, 0, 0, 12)
        testRatiosImpl(doubleArrayOf(1.0, 1.3, 1.6, 1.9, 2.0), 100f, 0, 0, 13)
        testRatiosImpl(doubleArrayOf(1.0, 1.3, 1.6, 1.9, 2.0), 100f, 0, 0, 14)
        testRatiosImpl(doubleArrayOf(1.0, 1.3, 1.6, 1.9, 2.0), 100f, 0, 0, 15)
        testRatiosImpl(doubleArrayOf(1.0, 1.3, 1.6, 1.9, 2.0), 100f, 0, 0, 16)
        testRatiosImpl(doubleArrayOf(1.0, 1.3, 1.6, 1.9, 2.0), 100f, 0, 0, 17)
        testRatiosImpl(doubleArrayOf(1.0, 1.3, 1.6, 1.9, 2.0), 100f, 0, 0, 18)
        testRatiosImpl(doubleArrayOf(1.0, 1.3, 1.6, 1.9, 2.0), 100f, 0, 0, 0)
        testRatiosImpl(doubleArrayOf(1.0, 1.3, 1.6, 1.9, 2.0), 100f, 0, 0, 1)
        testRatiosImpl(doubleArrayOf(1.0, 1.3, 1.6, 1.9, 2.0), 100f, 0, 0, -1)
        testRatiosImpl(doubleArrayOf(1.0, 1.3, 1.6, 1.9, 2.0), 100f, 0, 0, -2)
        testRatiosImpl(doubleArrayOf(1.0, 1.3, 1.6, 1.9, 2.0), 100f, 0, 0, -12)

        testRatiosImpl(doubleArrayOf(1.0, 1.3, 1.6, 1.9, 2.0), 100f, 3, 2, -12)
        testRatiosImpl(doubleArrayOf(1.0, 1.3, 1.6, 1.9, 2.0), 100f, 3, 1, -12)
        testRatiosImpl(doubleArrayOf(1.0, 1.3, 1.6, 1.9, 2.0), 100f, -4, 3, -12)
        testRatiosImpl(doubleArrayOf(1.0, 1.3, 1.6, 1.9, 2.0), 100f, -8, 2, -12)
    }

    @Test
    fun testBoundaries() {
        val ratios = doubleArrayOf(1.0, 2.0.pow(0.5), 2.0)
        val rootNoteIndex = 0
        val referenceFrequency = 100f
        val noteIndexAtReferenceFrequency = 0
        val freqMin = ratios[0].toFloat() * referenceFrequency -0.001f
        val freqMax = ratios.last().toFloat() * referenceFrequency + 0.001f
        val tuning = TuningRatioBased(
            Tuning.EDO12, // not important here
            ratios,
            rootNoteIndex, noteIndexAtReferenceFrequency, referenceFrequency,
            freqMin,
            freqMax
        )
        assertEquals(0, tuning.getToneIndexBegin())
        assertEquals(ratios.size, tuning.getToneIndexEnd())

        assertEquals(0.0f, tuning.getToneIndex(0.1f * freqMin), 1e-12f)
        assertEquals((ratios.size - 1).toFloat(), tuning.getToneIndex(10f * freqMax), 1e-12f)

        assertEquals(1, tuning.getClosestToneIndex(referenceFrequency * ratios[1].toFloat()))
        assertEquals(1f, tuning.getToneIndex(referenceFrequency * ratios[1].toFloat()), 1e-12f)

        assertTrue(tuning.getToneIndex(freqMin + 0.1f) > 0.0001f)
        assertFalse(tuning.getToneIndex(freqMin - 0.1f) > 0.0001f)

        assertTrue(tuning.getToneIndex(freqMax - 0.1f) < 1.999f)
        assertFalse(tuning.getToneIndex(freqMax + 0.1f) < 1.999f)

        assertEquals(0, tuning.getClosestToneIndex(0.1f * freqMin))
        assertEquals(ratios.size - 1, tuning.getClosestToneIndex(10f * freqMax))
    }

    @Test
    fun testTwelveTone() {

        val referenceNoteIndex = 0
        val ratios = doubleArrayOf(15.0/15.0, 16.0/15.0, 17.0/15.0, 18.0/15.0, 19.0/15.0, 20.0/15.0,
            21.0/15.0, 22.0/15.0, 23.0/15.0, 24.0/15.0, 25.0/15.0, 26.0/15.0, 30.0/15.0)
        val fRef = 440f
        //val fMin = fRef / 100
        //val fMax = fRef * 100
        val numNotesPerOctave = ratios.size - 1

        for (rootNote in -20 .. 20) {
            val tuning = TuningRatioBased(
                Tuning.EDO12, // not important here
                ratios, rootNote, referenceNoteIndex, fRef // , fMin, fMax
            )

            // check that the resulting frequencies are independent on which octave the reference note is
            for (r in -5..-5) {
                val tuning2 = TuningRatioBased(
                    Tuning.EDO12, // not important here
                    ratios, rootNote, referenceNoteIndex + r * numNotesPerOctave, fRef // , fMin, fMax
                )
                val numNotes = tuning.getToneIndexEnd() - tuning.getToneIndexBegin()
                val numNotes2 = tuning2.getToneIndexEnd() - tuning2.getToneIndexBegin()
                assertEquals(numNotes, numNotes2)

                for (i in 0 until numNotes) {
                    val iNote = tuning.getToneIndexBegin() + i
                    val iNote2 = tuning2.getToneIndexBegin() + i
                    val freq = tuning.getNoteFrequency(iNote)
                    val freq2 = tuning2.getNoteFrequency(iNote2)
                    assertEquals(freq, freq2, 1 - 12f)
                }
            }
        }
    }
}