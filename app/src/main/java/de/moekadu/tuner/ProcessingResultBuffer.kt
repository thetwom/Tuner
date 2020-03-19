package de.moekadu.tuner

import kotlin.RuntimeException

/// Buffer where we can store several preprocessing results
/**
 *
 * @param size Number of preprocessing results, we can store
 * @param init Constructor function for single elements
 */
class ProcessingResultBuffer<T>(val size : Int, init : () -> T ) {
    private val storage = Array<Any?>(size) { init() }
    private val readyToUnlockWriteFlag = BooleanArray(size) { true }
    private var numLockWrite = 0
    private val lockReadCount = IntArray(size) { 0 }
    private var numWriteFinished = 0

    fun lockRead(i: Int): T? {
        val ii = numWriteFinished + i
        if (ii < 0) {
            return null
//            throw RuntimeException(
//                "Not enough values available in ProcessingResultBuffer: Stored="
//                        + numWriteFinished + ", requested=" + i + " which means we need at least "
//                        + (-i) + " stored values."
//            )
        }

        if (i >= 0 || i < -size) {
            return null
//            throw RuntimeException("Invalid index for ProcessingResultBuffer: i=" + i + " allowed is only -1 >= i >= " + (-size))
        }

        val idx = ii % size
        if(size + i < numLockWrite)
            return null

        ++lockReadCount[idx]
        @Suppress("UNCHECKED_CAST")
        return storage[idx] as T
    }

    fun unlockRead(processingResults : T?) {
        if(processingResults == null)
            return

        val idx = storage.indexOf(processingResults)
        if(lockReadCount[idx] == 0)
            throw RuntimeException("ProcessingResultBuffer:unlockRead: Already unlocked")
        if(idx < 0)
            throw RuntimeException("ProcessingResultBuffer:unlockRead: Element not member of ProcessingResultBuffer")

        --lockReadCount[idx]
    }

    fun lockWrite(): T? {
        val idx = (numWriteFinished + numLockWrite) % size
        if (lockReadCount[idx] > 0 || numLockWrite == size)
            return null
        ++numLockWrite
        readyToUnlockWriteFlag[idx] = false
        @Suppress("UNCHECKED_CAST")
        return storage[idx] as T
    }

    fun unlockWrite(processingResults: T?): Int {
        if(processingResults == null)
            return 0

        val idx = storage.indexOf(processingResults)
        if(idx < 0)
            throw RuntimeException("ProcessingResultBuffer:unlockWrite: Element not member of ProcessingResultBuffer")
        if(readyToUnlockWriteFlag[idx])
            throw RuntimeException("ProcessingResultBuffer:unlockWrite: Element already ready to unlock")
        readyToUnlockWriteFlag[idx] = true

        var nUnlocked = 0
        for(i in 0 until numLockWrite) {
            if (!readyToUnlockWriteFlag[numWriteFinished % size])
                break
            ++numWriteFinished
            ++nUnlocked
        }
        numLockWrite -= nUnlocked
        return nUnlocked
    }
}
