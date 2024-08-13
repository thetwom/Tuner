/*
* Copyright 2024 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.moekadu.tuner.misc

import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Memory pool class, which can be used as a base class for managing specific memory types.
 * This class would normally be wrapped like:
 * class MemoryPoolMyType {
 *   private val pool = MemoryPool<MyType>()
 *
 *   fun get(myParameter1: Int, myParameter2: Float) = pool.get(
 *      factory = { MyType(size, dt) },
 *      checker = { it.myParameter1 == myParameter1 && it.myParameter2 == myParameter2 }
 *   )
 * }
 *
 * This class would return recycled objects, if they match the checker argument or create
 * new objects if they don't match the checker or if no recycled object is available.
 *
 * The returned objects are recycled by using reference counts. Access the memory by
 *   obtainedObject.memory
 * Recycle the memory by calling
 *   obtainedObject.decRef()
 * If the object is shared with more instances, you can call also increase the refCount b
 *   obtainedObject.incRef()
}

 */
class MemoryPool<T>(capacity: Int = 10)
{
    inner class RefCountedMemory(val memory: T) {
        private var refCount = 1
        private val mutex = Mutex()

        /** Access the underlying memory in a save way.
         * The ref counts are handled by the function itself.
         * If the underlying memory is not available anymore, nothing will be done.
         * @param f Function which obtains as argument the underlying memory and can use it.
         * @return Return value of function or null if the underlying memory is not available
         *   anymore.
         */
        suspend inline fun <R> with(f: (T) -> R): R? {
            if (!incRef())
                return null
            val result = f(memory)
            decRef()
            return result
        }

        /** Increment the reference count.
         * @return True, if we successfully incremented the count. False if the underlying memory
         *   does not exist anymore.
         */
        suspend fun incRef(): Boolean {
            return mutex.withLock {
                if (refCount == 0) {
                    false
                } else {
                    ++refCount
                    true
                }
            }
        }

        /** Decrement the reference count. */
        suspend fun decRef() {
            mutex.withLock {
                --refCount
                if (refCount == 0)
                    this@MemoryPool.recycle(this)
            }
        }
    }

    /** Channel which stores the available objects which can be recycled. */
    private val memoryChannel = Channel<T>(capacity, BufferOverflow.DROP_OLDEST)

    /** Recycle a ref counted memory.
     * @param memory Memory to be recycled.
     */
    private fun recycle(memory: RefCountedMemory) {
        memoryChannel.trySend(memory.memory)
    }

    /** Obtain a new ref counted memory object.
     * @param factory Factory class which is used to create a new memory object. This will only
     *   be called if a new memory allocation is needed.
     * @param checker Function which checks if a recycled memory is appropriate for being used.
     *   This e.g. could check, if the required array size of a memory is correct. If the checker
     *   returns false, we will not use the proposed recycled object.
     * @return Ref counted memory.
     */
    fun get(factory: () -> T, checker: (T) -> Boolean): RefCountedMemory {
        var memory: T?
        while (true) {
            memory = memoryChannel.tryReceive().getOrNull()
            if (memory == null || checker(memory))
                break
        }

        return if (memory == null) {
            val m = factory()
            Log.v("Tuner", "MemoryPool.get: Allocating new memory, type: $m")
            RefCountedMemory(m)
        } else {
//            Log.v("Tuner", "MemoryPool.get: Recycling memory, type: $memory")
            return RefCountedMemory(memory)
        }
    }
}
