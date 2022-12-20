package de.moekadu.tuner

import de.moekadu.tuner.notedetection.AcousticZeroWeighting
import de.moekadu.tuner.notedetection.Harmonics
import de.moekadu.tuner.notedetection.InharmonicityDetector
import de.moekadu.tuner.notedetection.computeInharmonicity
import org.junit.Assert.assertEquals
import org.junit.Test

class InharmonicityTest {

    @Test
    fun test() {
        assertEquals(0f, computeInharmonicity(100f, 10, 50f, 5), 1e-6f)
        // harmonicity is 1 if the frequency ratio is twice the ideal ratio (double stretching)
        assertEquals(1f, computeInharmonicity(100f, 10, 25f, 5), 1e-6f)
        // harmonicity is -1 if the frequency ratio 0 (zero stretching)
        assertEquals(-1f, computeInharmonicity(50f, 10, 50f, 5), 1e-6f)

    }

    @Test
    fun detectCorrectExtractionOfHarmonis() {
        val harmonics = Harmonics(20)
        harmonics.addHarmonic(5, 50f, 50, 10f)
        harmonics.addHarmonic(3, 30f, 30, 2f)
        harmonics.addHarmonic(8, 80f, 80, 15f)
        harmonics.addHarmonic(2, 20f, 20, 8f)
        harmonics.addHarmonic(7, 70f, 70, 1f)

        val acousticWeighting = AcousticZeroWeighting()

        val detector = InharmonicityDetector(3)
        val sortedHarmonics = detector.extractAndSortHarmonicsWithHighestAmplitude(harmonics)
        assertEquals(8, sortedHarmonics[0]?.harmonicNumber)
        assertEquals(5, sortedHarmonics[1]?.harmonicNumber)
        assertEquals(2, sortedHarmonics[2]?.harmonicNumber)
        val inharmonicity = detector.computeInharmonicity(harmonics, acousticWeighting)
        assertEquals(0f, inharmonicity, 1e-6f)
    }

    @Test
    fun testThatZeroAmplitudesAreIgnored() {
        val harmonics = Harmonics(20)
        harmonics.addHarmonic(5, 50f, 50, 10f)
        harmonics.addHarmonic(3, 200f, 30, 0f)
        harmonics.addHarmonic(8, 80f, 80, 15f)
        harmonics.addHarmonic(2, 2000f, 20, 0f)
        harmonics.addHarmonic(7, 78f, 70, 0f)

        val acousticWeighting = AcousticZeroWeighting()

        val detector = InharmonicityDetector(3)
        val sortedHarmonics = detector.extractAndSortHarmonicsWithHighestAmplitude(harmonics)
        assertEquals(8, sortedHarmonics[0]?.harmonicNumber)
        assertEquals(5, sortedHarmonics[1]?.harmonicNumber)
        assertEquals(0f, sortedHarmonics[2]?.spectrumAmplitudeSquared)
        val inharmonicity = detector.computeInharmonicity(harmonics, acousticWeighting)
        assertEquals(0f, inharmonicity, 1e-6f)
    }

    @Test
    fun testNoStretching() {
        val harmonics = Harmonics(20)
        harmonics.addHarmonic(5, 50f, 50, 11f)
        harmonics.addHarmonic(4, 50f, 50, 10f)
        harmonics.addHarmonic(3, 200f, 30, 0f)

        val acousticWeighting = AcousticZeroWeighting()

        val detector = InharmonicityDetector(4)
        val sortedHarmonics = detector.extractAndSortHarmonicsWithHighestAmplitude(harmonics)
        assertEquals(5, sortedHarmonics[0]?.harmonicNumber)
        assertEquals(4, sortedHarmonics[1]?.harmonicNumber)
        assertEquals(3, sortedHarmonics[2]?.harmonicNumber)
        assertEquals(null, sortedHarmonics[3])
        val inharmonicity = detector.computeInharmonicity(harmonics, acousticWeighting)
        assertEquals(-1f, inharmonicity, 1e-6f)
    }
}