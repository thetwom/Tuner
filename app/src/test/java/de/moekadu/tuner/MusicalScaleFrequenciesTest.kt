package de.moekadu.tuner

import de.moekadu.tuner.temperaments2.MusicalScaleFrequencies
import de.moekadu.tuner.temperaments2.StretchTuning
import de.moekadu.tuner.temperaments2.centsToFrequency
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicalScaleFrequenciesTest {

    @Test
    fun equalTemperament() {
        val cents = (0..1200 step 100).map { it.toDouble() }.toDoubleArray()
        val referenceFrequency = 440f
        val frequencyMin = 10f
        val frequencyMax = 16000f

        val frequencies = MusicalScaleFrequencies.create(
            cents,
             0,
            referenceFrequency,
            frequencyMin,
            frequencyMax,
            stretchTuning = StretchTuning()
        )

        // check that octaves double/half frequencies
        assertEquals(referenceFrequency, frequencies[0])
        assertEquals(2 * referenceFrequency, frequencies[cents.size - 1])
        assertEquals(referenceFrequency / 2, frequencies[-cents.size + 1])

        // check bounds
        assert(frequencies[frequencies.indexStart] >= frequencyMin)
        assert(frequencies[frequencies.indexEnd - 1] <= frequencyMax)

        // check equal temperament frequencies
        for (i in frequencies.indexStart + 1 until frequencies.indexEnd) {
            val previousFrequency = frequencies[i-1].toDouble()
            val frequency = frequencies[i].toDouble()
            val frequencyEqualTemperament = centsToFrequency(100.0, previousFrequency)
            assertEquals(frequency, frequencyEqualTemperament, 1e-3)
        }

        // check closest
        val indexRef = frequencies.getClosestFrequencyIndex(referenceFrequency)
        assertEquals(0, indexRef)

        val indexMin = frequencies.getClosestFrequencyIndex(frequencyMin)
        assertEquals(indexMin, frequencies.indexStart)

        val indexMax = frequencies.getClosestFrequencyIndex(frequencyMax)
        assertEquals(indexMax, frequencies.indexEnd - 1)

        val indexOctave = frequencies.getClosestFrequencyIndex(2 * referenceFrequency)
        assertEquals(cents.size - 1, indexOctave)

        // frequency index as float
        val indexFloatRef = frequencies.getFrequencyIndex(referenceFrequency)
        assertEquals(0f, indexFloatRef)
        val frequency1 = frequencies[1]
        val indexFloat1 = frequencies.getFrequencyIndex(frequency1)
        assertEquals(1f, indexFloat1)
        val frequencyM1 = frequencies[-1]
        val indexFloatM1 = frequencies.getFrequencyIndex(frequencyM1)
        assertEquals(-1f, indexFloatM1)

        val frequency03 = centsToFrequency(30.0, referenceFrequency.toDouble()).toFloat()
        val indexFloat03 = frequencies.getFrequencyIndex(frequency03)
        assertEquals(0.3f, indexFloat03, 1e-5f)
        val frequency07 = centsToFrequency(70.0, referenceFrequency.toDouble()).toFloat()
        val indexFloat07 = frequencies.getFrequencyIndex(frequency07)
        assertEquals(0.7f, indexFloat07, 1e-5f)

        // test index as float outside range
        val frequencyEndP5 = centsToFrequency(500.0, frequencies[indexMax].toDouble())
        val indexEndP5 = frequencies.getFrequencyIndex(frequencyEndP5.toFloat())
        assertEquals(indexMax + 5f, indexEndP5, 1e-5f)

        val frequencyBeginM5 = centsToFrequency(-500.0, frequencies[indexMin].toDouble())
        val indexBeginM5 = frequencies.getFrequencyIndex(frequencyBeginM5.toFloat())
        assertEquals(indexMin - 5f, indexBeginM5, 1e-5f)
    }

    @Test
    fun referenceNoteOutside() {
        val cents = (0..1200 step 100).map { it.toDouble() }.toDoubleArray()
        val referenceFrequency = 440f
        val frequencyMin = 500f
        val frequencyMax = 16000f

        val frequencies = MusicalScaleFrequencies.create(
            cents,
            0,
            referenceFrequency,
            frequencyMin,
            frequencyMax,
            stretchTuning = StretchTuning()
        )

        assert(frequencies.indexStart > 0)
        assertEquals(frequencies[cents.size - 1], 2 * referenceFrequency)

        // check closest
        // - reference frequency is not part of scale, so in this case it will not return 0!
        val indexRef = frequencies.getClosestFrequencyIndex(referenceFrequency)
        assertEquals(frequencies.indexStart, indexRef)
        // - octave is part of scale
        val indexOctave = frequencies.getClosestFrequencyIndex(2 * referenceFrequency)
        assertEquals(cents.size - 1, indexOctave)
    }

    @Test
    fun referenceNoteOutside2() {
        val cents = (0..1200 step 100).map { it.toDouble() }.toDoubleArray()
        val referenceFrequency = 440f
        val frequencyMin = 16f
        val frequencyMax = 300f

        val frequencies = MusicalScaleFrequencies.create(
            cents,
            0,
            referenceFrequency,
            frequencyMin,
            frequencyMax,
            stretchTuning = StretchTuning()
        )

        assert(frequencies.indexEnd <= 0)
        assertEquals(frequencies[-cents.size + 1], referenceFrequency / 2)

        // check closest
        // - reference frequency is not part of scale, so in this case it will not return 0!
        val indexRef = frequencies.getClosestFrequencyIndex(referenceFrequency)
        assertEquals(frequencies.indexEnd - 1, indexRef)
        // - octave is part of scale
        val indexOctave = frequencies.getClosestFrequencyIndex(referenceFrequency / 2)
        assertEquals(-cents.size + 1, indexOctave)
    }

    @Test
    fun equalTemperamentIndexByFloat() {
        val cents = (0..1200 step 100).map { it.toDouble() }.toDoubleArray()
        val referenceFrequency = 440f
        val frequencyMin = 10f
        val frequencyMax = 16000f

        val frequencies = MusicalScaleFrequencies.create(
            cents,
            0,
            referenceFrequency,
            frequencyMin,
            frequencyMax,
            stretchTuning = StretchTuning()
        )

        val frequencyInt = frequencies[0]
        val frequencyFloat = frequencies[0f]
        assertEquals(frequencyInt, frequencyFloat)

        val frequencyFloat05 = frequencies[0.5f]
        val frequencyFloat05Check = centsToFrequency(50.0, frequencyFloat.toDouble())
        assertEquals(frequencyFloat05Check, frequencyFloat05.toDouble(), 1e-4)

        val frequencyFloatM05 = frequencies[-0.5f]
        val frequencyFloatM05Check = centsToFrequency(-50.0, frequencyFloat.toDouble())
        assertEquals(frequencyFloatM05Check, frequencyFloatM05.toDouble(), 1e-4)

        // Check out of bounds access
        val frequencyBegin = frequencies[frequencies.indexStart]
        val frequencyBeginM05 = frequencies[frequencies.indexStart - 0.5f]
        val frequencyBeginM05Check = centsToFrequency(-50.0, frequencyBegin.toDouble())
        assertEquals(frequencyBeginM05Check, frequencyBeginM05.toDouble(), 1e-4)

        val frequencyBeginM5 = frequencies[frequencies.indexStart - 5f]
        val frequencyBeginM5Check = centsToFrequency(-500.0, frequencyBegin.toDouble())
        assertEquals(frequencyBeginM5Check, frequencyBeginM5.toDouble(), 1e-4)

        val frequencyEnd = frequencies[frequencies.indexEnd - 1]
        val frequencyEnd05 = frequencies[frequencies.indexEnd - 0.5f]
        val frequencyEnd05Check = centsToFrequency(50.0, frequencyEnd.toDouble())
        assertEquals(frequencyEnd05Check, frequencyEnd05.toDouble(), 1e-3)

        val frequencyEnd5 = frequencies[frequencies.indexEnd + 4f]
        val frequencyEnd5Check = centsToFrequency(500.0, frequencyEnd.toDouble())
        assertEquals(frequencyEnd5Check, frequencyEnd5.toDouble(), 1e-2)

    }
    }