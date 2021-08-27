package de.moekadu.tuner

import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

class MemoryManagerWorkingData {
    private val memoryChannel = Channel<WorkingData>(10, BufferOverflow.DROP_OLDEST)

    fun getMemory(size: Int, sampleRate: Int, framePosition: Int): WorkingData {
        var memory: WorkingData?
        while (true) {
            memory = memoryChannel.tryReceive().getOrNull()
            if (memory == null || (memory.size == size && memory.sampleRate == sampleRate))
                break
        }
        if (memory == null) {
            Log.v("Tuner", "MemoryManagerWorkingData.getMemory: create new memory")
            memory = WorkingData(size, sampleRate, framePosition)
        } else {
            memory.framePosition = framePosition
        }
        return memory
    }

    fun recycleMemory(workingData: WorkingData) {
        memoryChannel.trySend(workingData)
    }
}

class MemoryManagerSampleData {
    private val memoryChannel = Channel<SampleData>(10, BufferOverflow.DROP_OLDEST)

    fun getMemory(size: Int, sampleRate: Int, framePosition: Int): SampleData {
        var memory: SampleData?
        while (true) {
            memory = memoryChannel.tryReceive().getOrNull()
            if (memory == null || (memory.size == size && memory.sampleRate == sampleRate))
                break
        }
        if (memory == null) {
            Log.v("Tuner", "MemoryManagerSampleData.getMemory: create new memory")
            memory = SampleData(size, sampleRate, framePosition)
        } else {
            memory.reset(framePosition)
        }
        return memory
    }

    fun recycleMemory(sampleData: SampleData) {
        memoryChannel.trySend(sampleData)
    }
}