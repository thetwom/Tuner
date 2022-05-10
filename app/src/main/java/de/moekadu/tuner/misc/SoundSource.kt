/*
 * Copyright 2020 Michael Moessner
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

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import de.moekadu.tuner.notedetection.SampleData
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlin.math.max
import kotlin.math.roundToInt

/// Generator of sound samples
/**
 * @param scope Coroutine scope used for the sample data generator.
 */
class SoundSource(private val scope: CoroutineScope) {

    fun interface SettingsChangedListener {
        fun onSettingsChanged(sampleRate: Int, windowSize: Int, overlap: Float)
    }
    var settingsChangedListener: SettingsChangedListener? = null

    /// Job which is generating sound.
    private var sourceJob: Job? = null

    private var memoryManager = MemoryManagerSampleData()

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
                settingsChangedListener?.onSettingsChanged(sampleRate, windowSize, value)
                if (sourceJob != null)
                    restartSampling()
            }
        }

    /// Sample data chunk size which is passed through the communication channel.
    var windowSize = 4096
        set(value) {
            if (field != value) {
                field = value
                settingsChangedListener?.onSettingsChanged(sampleRate, value, overlap)
                if (sourceJob != null)
                    restartSampling()
            }
        }

    /// Sample rate to be used.
    var sampleRate = 44100
        set(value) {
            if (field != value) {
                field = value
                settingsChangedListener?.onSettingsChanged(value, windowSize, overlap)
                if (sourceJob != null)
                    restartSampling()
            }
        }

    fun recycle(sampleData: SampleData) {
        memoryManager.recycleMemory(sampleData)
    }

    fun restartSampling()
    {
        stopSampling()
        sourceJob = createAudioRecordJob(sampleRate, overlap, windowSize, outputChannel, memoryManager, scope, testFunction)
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
@SuppressLint("MissingPermission")
fun createAudioRecordJob(sampleRate: Int, overlap: Float, windowSize: Int, channel: SendChannel<SampleData>,
                         memoryManager: MemoryManagerSampleData, scope: CoroutineScope, testFunction: ((Float) -> Float)?) : Job {

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
                        //sampleDataList.add(SampleData(windowSize, sampleRate, nextStartingDataFrame))
                        sampleDataList.add(memoryManager.getMemory(windowSize, sampleRate, nextStartingDataFrame))
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
