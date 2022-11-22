package de.moekadu.tuner

import de.moekadu.tuner.notedetection2.computeInharmonicity
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
}