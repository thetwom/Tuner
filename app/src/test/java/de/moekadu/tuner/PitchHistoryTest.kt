package de.moekadu.tuner

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PitchHistoryTest {

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Test
    fun addData() {
        val pitchHistory = PitchHistory(10, TuningEqualTemperament())
        pitchHistory.maxNumFaultyValues = 2

        pitchHistory.appendValue(440f)
        assertEquals(pitchHistory.history.value!!.size, 1)

        // first value initiating a pitch change, but before it is accepted, we need a second value to confirm
        pitchHistory.appendValue(1000f)
        assertEquals(pitchHistory.history.value!!.size, 1)

        // second value to perform a pitch change ... we defined two values to be enough,
        // so now this and the previous value are accepted.
        pitchHistory.appendValue(1000f)
        assertEquals(pitchHistory.history.value!!.size, 3)

        // one exceptional value in between will not be accepted
        pitchHistory.appendValue(40f)
        pitchHistory.appendValue(1000f)
        assertEquals(pitchHistory.history.value!!.size, 4)

        // switch back to pitch 440, we need two values to be accepted, but one error value in between
        // is ok.
        pitchHistory.appendValue(440f)
        assertEquals(pitchHistory.history.value!!.size, 4)
        pitchHistory.appendValue(240f) // one exception is allowed when changing to a new pitch
        pitchHistory.appendValue(440f)
        assertEquals(pitchHistory.history.value!!.size, 6)

        // just make sure, that the averaged values have the same size as to not averaged value
        assertEquals(pitchHistory.historyAveraged.value!!.size, pitchHistory.history.value!!.size)

        // add some more values close to our 440 pitch. Should be all excepted.
        pitchHistory.appendValue(441f)
        pitchHistory.appendValue(444f)
        pitchHistory.appendValue(450f)
        pitchHistory.appendValue(445f)
        assertEquals(pitchHistory.history.value!!.size, 10)

        // now we reached our size of 10 values. A new value won't increase the size any further
        pitchHistory.appendValue(445f)
        assertEquals(pitchHistory.history.value!!.size, 10)

        // also a pitch change should not change any the size anymore
        pitchHistory.appendValue(1000f)
        pitchHistory.appendValue(1000f)
        pitchHistory.appendValue(1000f)
        assertEquals(pitchHistory.history.value!!.size, 10)
    }
}