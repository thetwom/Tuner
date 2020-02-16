package de.moekadu.tuner

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun circularRecordData_Test() {
        var counter = 0
        val circRec = CircularRecordData(10)
        val write = circRec.lockWrite(5)
        assertNotNull(write)
        write?.let {

            for (i in 0 until write.size) {
                write.data[write.offset + i] = counter.toFloat()
                counter++
            }
            circRec.unlockWrite(write)
        }

        val read = circRec.lockRead(0, 5)
        val someFloats = FloatArray(5)
        read.copyToFloatArray(someFloats)
        circRec.unlockRead(read)

        val write2 = circRec.lockWrite(5)
        assertNotNull(write2)
        write2?.let {
            for (i in 0 until write2.size) {
                write2.data[write2.offset + i] = counter.toFloat()
                counter++
            }
            circRec.unlockWrite(write2)
        }

        //val read2 = circRec.lockRead(5, 5)
        //val read5 = circRec.lockRead(4, 5)
        //circRec.unlockRead(read5)

        val write3 = circRec.lockWrite(5)
        assertNotNull(write3)
        write3?.let {
            for (i in 0 until write3.size) {
                write3.data[write3.offset + i] = counter.toFloat()
                counter++
            }
            circRec.unlockWrite(write3)
        }

        val read3 = circRec.lockRead(5, 5)
        read3.copyToFloatArray(someFloats)
        circRec.unlockRead(read3)
    }
}
