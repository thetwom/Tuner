package de.moekadu.tuner

import de.moekadu.tuner.temperaments.NoteNameScaleFactory
import de.moekadu.tuner.temperaments.TemperamentType
import org.junit.Test

class NoteNameScaleTest {
    @Test
    fun testNotes() {
        val scale = NoteNameScaleFactory.create(TemperamentType.EDO12, false)
        val note = scale.getNoteOfIndex(0)

    }
}