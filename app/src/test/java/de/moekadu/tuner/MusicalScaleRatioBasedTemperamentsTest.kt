package de.moekadu.tuner

import de.moekadu.tuner.temperaments.MusicalScaleRatioBasedTemperaments
import de.moekadu.tuner.temperaments.NoteNameScaleFactory
import de.moekadu.tuner.temperaments.TemperamentType
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class MusicalScaleRatioBasedTemperamentsTest {

    private fun testOctImpl(
        ratios: DoubleArray,
        referenceFrequency: Float,
        noteIndexAtReferenceFrequency: Int,
        rootNoteIndex: Int
    ) {
        val numNotesPerOctave = ratios.size - 1
        val noteNameScale = NoteNameScaleFactory.create(TemperamentType.EDO12, false)
        val octRatio = (ratios.last() / ratios.first()).toFloat()
        val temperament = MusicalScaleRatioBasedTemperaments(
            TemperamentType.EDO12, // not important here
            ratios,
            noteNameScale,
            noteNameScale.getNoteOfIndex(noteIndexAtReferenceFrequency),
            referenceFrequency,
            noteNameScale.getNoteOfIndex(rootNoteIndex),
            referenceFrequency / 4.0f - 0.001f,
            2 * referenceFrequency + 0.001f
        )

        assertEquals(
            temperament.getNoteFrequency(noteIndexAtReferenceFrequency),
            referenceFrequency,
            1e-12f
        )
        assertEquals(
            temperament.getNoteFrequency(noteIndexAtReferenceFrequency + numNotesPerOctave),
            referenceFrequency * octRatio,
            1e-12f
        )
        assertEquals(
            temperament.getNoteFrequency(noteIndexAtReferenceFrequency - numNotesPerOctave),
            referenceFrequency / octRatio,
            1e-12f
        )
        assertEquals(
            temperament.getNoteFrequency(noteIndexAtReferenceFrequency - 2 * numNotesPerOctave),
            referenceFrequency / octRatio.pow(2),
            1e-12f
        )
        assertEquals(
            temperament.noteIndexBegin,
            noteIndexAtReferenceFrequency - 2 * numNotesPerOctave
        )
        assertEquals(
            noteIndexAtReferenceFrequency + numNotesPerOctave + 1,
            temperament.noteIndexEnd
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
        val noteNameScale = NoteNameScaleFactory.create(TemperamentType.EDO12, false)
        val temperament = MusicalScaleRatioBasedTemperaments(
            TemperamentType.EDO12, // not important here
            ratios,
            noteNameScale,
            noteNameScale.getNoteOfIndex(noteIndexAtReferenceFrequency),
            referenceFrequency,
            noteNameScale.getNoteOfIndex(rootNoteIndex),
            referenceFrequency / 100,
            100 * referenceFrequency
        )

        val noteIndex = testNoteIndex
        val freq = temperament.getNoteFrequency(noteIndex)
        assertEquals(noteIndex, temperament.getClosestNoteIndex(freq))

        val noteIndex2 = noteIndex + 1
        val freq2 = temperament.getNoteFrequency(noteIndex2)
        assertEquals(noteIndex2, temperament.getClosestNoteIndex(freq2))

        val freqBetween = freq * (freq2 / freq).pow(0.5f)
        assertEquals(noteIndex, temperament.getClosestNoteIndex(freqBetween - 0.001f))
        assertEquals(noteIndex2, temperament.getClosestNoteIndex(freqBetween + 0.001f))

        val noteIndexF = noteIndex + 0.3f
        val freqF = temperament.getNoteFrequency(noteIndexF)
        assertEquals(noteIndexF, temperament.getNoteIndex(freqF), 1e-6f)
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

        val noteNameScale = NoteNameScaleFactory.create(TemperamentType.EDO12, false)
        val temperament = MusicalScaleRatioBasedTemperaments(
            TemperamentType.EDO12, // not important here
            ratios,
            noteNameScale,
            noteNameScale.getNoteOfIndex(noteIndexAtReferenceFrequency),
            referenceFrequency,
            noteNameScale.getNoteOfIndex(rootNoteIndex),
            freqMin,
            freqMax
        )

        assertEquals(0, temperament.noteIndexBegin)
        assertEquals(ratios.size, temperament.noteIndexEnd)

        assertEquals(0.0f, temperament.getNoteIndex(0.1f * freqMin), 1e-12f)
        assertEquals((ratios.size - 1).toFloat(), temperament.getNoteIndex(10f * freqMax), 1e-12f)

        assertEquals(1, temperament.getClosestNoteIndex(referenceFrequency * ratios[1].toFloat()))
        assertEquals(1f, temperament.getNoteIndex(referenceFrequency * ratios[1].toFloat()), 1e-12f)

        assertTrue(temperament.getNoteIndex(freqMin + 0.1f) > 0.0001f)
        assertFalse(temperament.getNoteIndex(freqMin - 0.1f) > 0.0001f)

        assertTrue(temperament.getNoteIndex(freqMax - 0.1f) < 1.999f)
        assertFalse(temperament.getNoteIndex(freqMax + 0.1f) < 1.999f)

        assertEquals(0, temperament.getClosestNoteIndex(0.1f * freqMin))
        assertEquals(ratios.size - 1, temperament.getClosestNoteIndex(10f * freqMax))
    }

    @Test
    fun testTwelveTone() {

        val noteIndexAtReferenceFrequency = 0
        val ratios = doubleArrayOf(15.0/15.0, 16.0/15.0, 17.0/15.0, 18.0/15.0, 19.0/15.0, 20.0/15.0,
            21.0/15.0, 22.0/15.0, 23.0/15.0, 24.0/15.0, 25.0/15.0, 26.0/15.0, 30.0/15.0)
        val referenceFrequency = 440f
        //val fMin = fRef / 100
        //val fMax = fRef * 100
        val numNotesPerOctave = ratios.size - 1
        val noteNameScale = NoteNameScaleFactory.create(TemperamentType.EDO12, false)

        for (rootNoteIndex in -20 .. 20) {
            val temperament = MusicalScaleRatioBasedTemperaments(
                TemperamentType.EDO12, // not important here
                ratios,
                noteNameScale,
                noteNameScale.getNoteOfIndex(noteIndexAtReferenceFrequency),
                referenceFrequency,
                noteNameScale.getNoteOfIndex(rootNoteIndex)
            )

            // check that the resulting frequencies are independent on which octave the reference note is
            for (r in -5..-5) {
                val temperament2 = MusicalScaleRatioBasedTemperaments(
                    TemperamentType.EDO12, // not important here
                    ratios, noteNameScale, noteNameScale.getNoteOfIndex(noteIndexAtReferenceFrequency + r * numNotesPerOctave),
                    referenceFrequency, noteNameScale.getNoteOfIndex(rootNoteIndex)
                )
                val numNotes = temperament.noteIndexEnd - temperament.noteIndexBegin
                val numNotes2 = temperament2.noteIndexEnd - temperament2.noteIndexBegin
                assertEquals(numNotes, numNotes2)

                for (i in 0 until numNotes) {
                    val iNote = temperament.noteIndexBegin + i
                    val iNote2 = temperament2.noteIndexBegin + i
                    val freq = temperament.getNoteFrequency(iNote)
                    val freq2 = temperament2.getNoteFrequency(iNote2)
                    assertEquals(freq, freq2, 1 - 12f)
                }
            }
        }
    }
}