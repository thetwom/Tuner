package de.moekadu.tuner

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.collections.ArrayList

/// Generator of sound samples
/**
 * @param scope Coroutine scope used for the sample data generator.
 */
class SoundSource(val scope: CoroutineScope) {

    /// Job which is generating sound.
    private var sourceJob: Job? = null

    /// Channel used to communicate the sound samples
    private val outputChannel = Channel<SampleData>(Channel.CONFLATED)

    val flow
        get() = outputChannel.consumeAsFlow()

    /// Test function which can be used instead of the microphone data.
    /**
     * The underlying function must have the following shape:
     *   {timeInSeconds -> sampleValueAtGivenTime}
     *   example: {t -> kotlin.math.sin(2f * kotlin.math.PI.toFloat() * 440.0f * t)}
     * If this function is null, we will use data from the microphone.
     */
    var testFunction: ((Float) -> Float)? = null
        set(value) {
            field = value
            if (sourceJob != null)
                restartSampling()
        }

    /// Overlap of two succeeding sample data chunks.
    /**
     * examples:
     * 0f -> no overlap
     * 0.5f -> 50% overlap
     */
    var overlap = 0.25f
        set(value) {
            if (field != value) {
                require(overlap < 1f)
                field = value
                if (sourceJob != null)
                    restartSampling()
            }
        }

    /// Sample data chunk size which is passed through the communication channel.
    var windowSize = 4096
        set(value) {
            if (field != value) {
                field = value
                if (sourceJob != null)
                    restartSampling()
            }
        }

    /// Sample rate to be used.
    var sampleRate = 44100
        set(value) {
            if (field != value) {
                field = value
                if (sourceJob != null)
                    restartSampling()
            }
        }

    fun restartSampling()
    {
        stopSampling()
        sourceJob = createAudioRecordJob(sampleRate, overlap, windowSize, outputChannel, scope, testFunction)
    }

    fun stopSampling() {
        sourceJob?.cancel()
        sourceJob = null
    }
}

/// Create audio record job.
/**
 * @param sampleRate Sample rate to be used.
 * @param overlap Overlap between two succeeding chunks sent to into the channel.
 * @param windowSize Chunk size of sample data which is sent into the channel.
 * @param channel Channel into which the sampled data is sent.
 * @param testFunction If non-null, this test function replaces the microphone. Must have the
 *  following shape: {timeInSeconds -> sampleValueAtGivenTime}
 *    example: {t -> kotlin.math.sin(2f * kotlin.math.PI.toFloat() * 440.0f * t)}
 */
fun createAudioRecordJob(sampleRate: Int, overlap: Float, windowSize: Int, channel: SendChannel<SampleData>,
                         scope: CoroutineScope, testFunction: ((Float) -> Float)?) : Job {

    return scope.launch(Dispatchers.IO) {

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )

        val record =
            if (testFunction == null)
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_FLOAT,
                    minBufferSize
                )
            else
                null

        if (record?.state == AudioRecord.STATE_UNINITIALIZED) {
            Log.v(
                "TestRecordFlow",
                "SoundSource.createAudioRecordJob: Not able to acquire audio resource"
            )
        } else {
            record?.startRecording()

            val recordData =
                if (record != null)
                    FloatArray(record.bufferSizeInFrames / 2)
                else
                    FloatArray(minBufferSize / 8)

            val sampleDataList = ArrayList<SampleData>()
            var nextStartingDataFrame = 0
            var currentFrame = 0
            while (true) {
                if (!isActive)
                    break

                val numRead =
                    when {
                        testFunction != null -> {
                            for (i in recordData.indices)
                                recordData[i] = testFunction((currentFrame + i).toFloat() / sampleRate)
                            delay((1000 * recordData.size.toFloat() / sampleRate).toLong())
                            recordData.size
                        }
                        record != null -> {
                            record.read(recordData, 0, recordData.size, AudioRecord.READ_BLOCKING)
                        }
                        else -> {
                            0
                        }
                    }

                //Log.v("TestRecordFlow", "SoundSource: numRead=$numRead, currentFrame=$currentFrame, windowSize=$windowSize")
                if (numRead > 0) {
                    // add empty sampleData objects to the data queue
                    while (nextStartingDataFrame <= currentFrame + numRead) {
                        sampleDataList.add(SampleData(windowSize, nextStartingDataFrame, sampleRate))
                        nextStartingDataFrame += max(1, ((1.0 - overlap) * windowSize).roundToInt())
                    }
                    // Log.v("TestRecordFlow", "SoundSource: sampleDataList.size = ${sampleDataList.size}, nextStartingDataFrame=$nextStartingDataFrame")
                    sampleDataList
                        .map { it.apply { addData(currentFrame, recordData) } }
                        .filter { it.isFull }
                        .map { channel.send(it) }
                    sampleDataList.removeAll { it.isFull }

                    //for(s in sampleDataList)
                    //    Log.v("TestRecordFlow", "is full: ${s.isFull}")

                    currentFrame += numRead
                }
            }
            record?.stop()
            record?.release()
        }
    }
}
