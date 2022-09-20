package de.moekadu.tuner.misc

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.database.getStringOrNull
import androidx.lifecycle.viewModelScope
import de.moekadu.tuner.fragments.TunerFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels
import kotlin.math.min

class WaveFileWriterIntent(fragment: TunerFragment) {
    private val _writeWave = fragment.registerForActivityResult(FileWriterContract()) { uri ->
        val viewModel = fragment.viewModel
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            viewModel.waveWriter?.writeSnapshot(
                viewModel.getApplication(),
                uri,
                viewModel.sampleRate
            )
            viewModel.waveWriter?.record()
        }
        //        val filename = getFilenameFromUri(fragment.context, uri)
//        if (filename == null) {
//            Toast.makeText(instrumentsFragment.requireContext(),
//                R.string.failed_to_archive_instruments, Toast.LENGTH_LONG).show()
//        } else {
//            instrumentsFragment.context?.let { context ->
//                Toast.makeText(context, context.getString(R.string.database_saved, filename), Toast.LENGTH_LONG).show()
//            }
//        }
    }

    fun launch() {
        _writeWave.launch("test")
    }

    private fun getFilenameFromUri(context: Context?, uri: Uri): String? {
        if (context == null)
            return null
        var filename: String? = null
        context.contentResolver?.query(
            uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            filename = cursor.getStringOrNull(nameIndex)
            cursor.close()
        }
        return filename
    }

}

class FileWriterContract : ActivityResultContract<String, Uri?>() {

    override fun createIntent(context: Context, input: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/wav"
            putExtra(Intent.EXTRA_TITLE, "tuner-export.wav")
            // default path
            // putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent?.data
    }
}

class WaveWriter(val maxSize: Int) {

    enum class State {Recording, OnHold}
    private var state = State.Recording
    private val mutex = Mutex()

    private val buffer = FloatArray(maxSize)
    private var insertPosition = 0L
    private var numValues = 0


    /**
     * Called from any other thread
     */
    suspend fun appendData(input: FloatArray, numInputValues: Int = input.size) {
        mutex.withLock {
            if (state == State.Recording) {
                var inputIndexBegin = 0
                while (inputIndexBegin < numInputValues) {
                    val bufferIndexBegin = (insertPosition % maxSize).toInt()
                    val numCopy = min(maxSize - bufferIndexBegin, numInputValues - inputIndexBegin)
                    input.copyInto(buffer, bufferIndexBegin, inputIndexBegin, inputIndexBegin + numCopy)
                    inputIndexBegin += numCopy
                    insertPosition += numCopy
                    numValues += numCopy
                }
            }
            numValues = min(numValues, maxSize)
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

    suspend fun writeSnapshot(context: Context?, uri: Uri?, sampleRate: Int) {
        val wavArray = mutex.withLock {
            if (state == State.OnHold) {
                val inlineArray = FloatArray(numValues)
                val bufferIndexBegin = ((insertPosition - numValues) % maxSize).toInt()
                val numCopyPart1 = min(numValues, maxSize - bufferIndexBegin)
                buffer.copyInto(inlineArray, 0, bufferIndexBegin, bufferIndexBegin + numCopyPart1)
                val numCopyPart2 = numValues - numCopyPart1
                buffer.copyInto(inlineArray, numCopyPart1, 0, numCopyPart2)
                inlineArray
            } else {
                null
            }
        }

        if (wavArray != null)
            writeWave(context, uri, sampleRate, wavArray)
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