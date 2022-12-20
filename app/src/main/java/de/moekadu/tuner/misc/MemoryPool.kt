package de.moekadu.tuner.misc

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
class MemoryPool<T>
{
    inner class RefCountedMemory(val memory: T) {
        private var refCount = 1
        private val mutex = Mutex()

        suspend inline fun <R> with(f: (T) -> R): R? {
            if (!incRef())
                return null
            val result = f(memory)
            decRef()
            return result
        }

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
        suspend fun decRef() {
            mutex.withLock {
                --refCount
                if (refCount == 0)
                    this@MemoryPool.recycle(this)
            }
        }
    }

    private val memoryChannel = Channel<T>(10, BufferOverflow.DROP_OLDEST)

    private fun recycle(memory: RefCountedMemory) {
        memoryChannel.trySend(memory.memory)
    }

    fun get(factory: () -> T, checker: (T) -> Boolean): RefCountedMemory {
        var memory: T?
        while (true) {
            memory = memoryChannel.tryReceive().getOrNull()
            if (memory == null || checker(memory))
                break
        }
        //return memory?.also{ it.resetRef() } ?: RefCountedMemory(factory())
        return if (memory == null) {
            val m = factory()
//            Log.v("Tuner", "MemoryPool.get: Allocating new memory, type: $m")
            RefCountedMemory(m)
        } else {
            return RefCountedMemory(memory)
        }
    }
}
