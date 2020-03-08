package de.moekadu.tuner

import kotlin.RuntimeException

class ResultBuffer(val size : Int, val processingBufferSize : Int) {
    val storage = Array(size) { i -> RecordProcessorThread.ProcessingResults(processingBufferSize) }
    val readyToUnlockWriteFlag = BooleanArray(size) { i -> true}
    var numLockWrite = 0
    val lockReadCount = IntArray(size) {i -> 0}
    var numWriteFinished = 0

    fun lockRead(i: Int): RecordProcessorThread.ProcessingResults? {
        val ii = numWriteFinished + i
        if (ii < 0) {
            throw RuntimeException(
                "Not enough values available in ResultBuffer: Stored="
                        + numWriteFinished + ", requested=" + i + " which means we need at least "
                        + (-i) + " stored values."
            )
        }

        if (i >= 0 || i < -size) {
            throw RuntimeException("Invalid index for ResultBuffer: i=" + i + " allowed is only -1 >= i >= " + (-size))
        }

        val idx = ii % size
        if(size + i < numLockWrite)
            return null

        ++lockReadCount[idx]
        return storage[idx]
    }

    fun unlockRead(processingResults : RecordProcessorThread.ProcessingResults?) {
        if(processingResults == null)
            return

        val idx = storage.indexOf(processingResults)
        if(lockReadCount[idx] == 0)
            throw RuntimeException("ResultBuffer:unlockRead: Already unlocked")
        if(idx < 0)
            throw RuntimeException("ResultBuffer:unlockRead: Element not member of ResultBuffer")

        --lockReadCount[idx]
    }

    fun lockWrite(): RecordProcessorThread.ProcessingResults? {
        val idx = (numWriteFinished + numLockWrite) % size
        if (lockReadCount[idx] > 0 || numLockWrite == size)
            return null
        ++numLockWrite
        readyToUnlockWriteFlag[idx] = false
        return storage[idx]
    }

    fun unlockWrite(processingResults: RecordProcessorThread.ProcessingResults?): Int {
        if(processingResults == null)
            return 0

        val idx = storage.indexOf(processingResults)
        if(idx < 0)
            throw RuntimeException("ResultBuffer:unlockWrite: Element not member of ResultBuffer")
        if(readyToUnlockWriteFlag[idx] == true)
            throw RuntimeException("ResultBuffer:unlockWrite: Element already ready to unlock")
        readyToUnlockWriteFlag[idx] = true

        var nUnlocked = 0
        for(i in 0 until numLockWrite) {

            val idxnW = numWriteFinished % size
            if (!readyToUnlockWriteFlag[idxnW])
                break;

            ++numWriteFinished
            ++nUnlocked
        }
        numLockWrite -= nUnlocked
        return nUnlocked
    }
}
