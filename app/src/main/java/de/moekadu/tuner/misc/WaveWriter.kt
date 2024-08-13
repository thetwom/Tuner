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

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels
import kotlin.math.min

class WaveWriter {

    private val mutex = Mutex()

    private var buffer: FloatArray? = null
    private var insertPosition = 0L
    private var numValues = 0

    private var snapShot: FloatArray? = null

    /**
     * Called from any other thread
     */
    suspend fun appendData(input: FloatArray, numInputValues: Int = input.size) {
        mutex.withLock {
            val bufferLocal = buffer
            if (bufferLocal != null && bufferLocal.isNotEmpty()) {
                val maxSize = bufferLocal.size
                var inputIndexBegin = 0
                while (inputIndexBegin < numInputValues) {
                    val bufferIndexBegin = (insertPosition % maxSize).toInt()
                    val numCopy = min(maxSize - bufferIndexBegin, numInputValues - inputIndexBegin)
                    input.copyInto(bufferLocal, bufferIndexBegin, inputIndexBegin, inputIndexBegin + numCopy)
                    inputIndexBegin += numCopy
                    insertPosition += numCopy
                    numValues += numCopy
                }
                numValues = min(numValues, maxSize)
            }
        }
    }

    /**
     * Called from any other thread
     */
    suspend fun appendData(input: ShortArray, numInputValues: Int = input.size) {
        mutex.withLock {
            val bufferLocal = buffer
            if (bufferLocal != null && bufferLocal.isNotEmpty()) {
                val maxSize = bufferLocal.size
                val factor = 1f / Short.MAX_VALUE
                var inputIndexBegin = 0
                while (inputIndexBegin < numInputValues) {
                    val bufferIndexBegin = (insertPosition % maxSize).toInt()
                    val numCopy = min(maxSize - bufferIndexBegin, numInputValues - inputIndexBegin)
                    for (i in 0 until numCopy)
                        bufferLocal[bufferIndexBegin + i] = factor * input[inputIndexBegin + i]
                    inputIndexBegin += numCopy
                    insertPosition += numCopy
                    numValues += numCopy
                }
                numValues = min(numValues, maxSize)
            }
        }
    }

    /** Set size of how many values are kept in buffer.
     * @param numValues Number of values to store.
     */
    suspend fun setBufferSize(numValues: Int) {
        mutex.withLock {
            this.numValues = 0
            if ((buffer?.size ?: 0) != numValues)
                buffer = FloatArray(numValues)
        }
    }

    /** Store current buffer, such that it can be written to wave later on. */
    suspend fun storeSnapshot() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val bufferLocal = buffer
                if (bufferLocal != null) {
                    val maxSize = bufferLocal.size
                    val inlineArray = FloatArray(numValues)
                    val bufferIndexBegin = ((insertPosition - numValues) % maxSize).toInt()
                    val numCopyPart1 = min(numValues, maxSize - bufferIndexBegin)
                    bufferLocal.copyInto(
                        inlineArray,
                        0,
                        bufferIndexBegin,
                        bufferIndexBegin + numCopyPart1
                    )
                    val numCopyPart2 = numValues - numCopyPart1
                    bufferLocal.copyInto(inlineArray, numCopyPart1, 0, numCopyPart2)
                    snapShot = inlineArray
                } else {
                    snapShot = FloatArray(0)
                }
            }
        }
    }

    suspend fun writeStoredSnapshot(context: Context?, uri: Uri?, sampleRate: Int) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                snapShot?.let {
                    writeWave(context, uri, sampleRate, it)
                }
            }
        }
    }
}

fun writeWave(context: Context?, uri: Uri?, sampleRate: Int, data: FloatArray) {
    if (uri == null || context == null)
        return
    val bitsPerSample: Short = 32
    val numChannels: Short = 1
    val byteRate = sampleRate * numChannels * bitsPerSample / 8
    val blockAlign = (numChannels * (bitsPerSample / 8)).toShort()
    val dataSize = data.size * bitsPerSample / 8

    //val fileSizeInBytes = 44 + bitsPerSample / 8 * data.size
    context.contentResolver?.openOutputStream(uri, "wt")?.use { stream ->
        val channel = Channels.newChannel(stream)

        val buffer = ByteBuffer.allocate(64)
        buffer.clear()

        // general section
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.put("RIFF".toByteArray())  // chunk id (4 bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(dataSize + 38)  // chunk size (36 for pcm, 38 for ieee_float) (4 bytes)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.put("WAVE".toByteArray())  // format (4 bytes)

        // format section (size 36 or 38 depending if extension is present or not
        buffer.put("fmt ".toByteArray())  // subchunk id (4 bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(18)  // (Subchunk size) 16 is needed for PCM, i.e. "size of extension is not present", 18 for IEEE_FLOAT (4 bytes)
        buffer.putShort(3) // audio format, 1-> PCM, 3 -> IEEE_FLOAT (2 bytes)
        buffer.putShort(numChannels) // number of channels, 1-> mono (2 bytes)
        buffer.putInt(sampleRate) // sample rate (4 bytes)
        buffer.putInt(byteRate) // byte rate (4 bytes)
        buffer.putShort(blockAlign) // block align (2 bytes)
        buffer.putShort(bitsPerSample) // bits per samples (2 bytes)
        buffer.putShort(0) // size of extension for format region (only needed for non-pcm format) (2 bytes)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.put("data".toByteArray())  // format (4 bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(dataSize) // size of data in bytes (4 bytes)
        buffer.flip()
        channel.write(buffer)

        // data section
        val dataBuffer = ByteBuffer.allocate(data.size * (bitsPerSample / 8))
        dataBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val dataBufferFloat = dataBuffer.asFloatBuffer()
        dataBufferFloat.put(data)
        dataBufferFloat.flip()
        dataBuffer.position(dataBufferFloat.position() * (bitsPerSample / 8))
        dataBuffer.limit(dataBufferFloat.limit() * (bitsPerSample / 8))
        channel.write(dataBuffer)
        channel.close()
    }
}
