package de.moekadu.tuner.misc

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels

class WaveWriter(val numSamples: Long) {

    enum class State {Recording, OnHold}
    private var state = State.OnHold
    private val mutex = Mutex()

    /**
     * Called from any other thread
     */
    suspend fun addData(inputFramePosition: Int, input: FloatArray) {
        mutex.withLock {
            if (state == State.Recording) {

            }
        }
    }

    /** Call this to start sampling. */
    suspend fun record() {
        mutex.withLock {
            state = State.Recording
        }
    }

    /** Stop sampling and store data. */
    suspend fun hold() {
        mutex.withLock {
            state = State.OnHold
        }
    }

    suspend fun writeSnapshot(uri: Uri?) {
        mutex.withLock {
            if (state == State.OnHold) {

            }
        }
    }
}

fun writeWave(context: Context?, uri: Uri?, sampleRate: Int, data: FloatArray) {
    if (uri == null || context == null)
        return
    val bitsPerSample: Short = 32
    val numChannels: Short = 2
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