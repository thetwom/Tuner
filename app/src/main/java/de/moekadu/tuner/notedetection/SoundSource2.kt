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

package de.moekadu.tuner.notedetection

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import de.moekadu.tuner.misc.MemoryPool
import de.moekadu.tuner.misc.WaveWriter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlin.math.max
import kotlin.math.roundToInt

class SoundSourceJob(
    val job: Job,
    val channel: ReceiveChannel<MemoryPool<SampleData>.RefCountedMemory>
)

/** Generator of sound samples.
 * @param scope Coroutine scope used for the sample data generator.
 */
@SuppressLint("MissingPermission")
fun CoroutineScope.launchSoundSourceJob(
    overlap: Float = 0.25f,
    windowSize: Int = 4096,
    sampleRate: Int = 44100,
    testFunction: ((frame: Int, dt: Float) -> Float)? = null,
    waveWriter: WaveWriter? = null
): SoundSourceJob {
    val minBufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    val channelCapacity = computeRequiredChannelCapacity(
        audioRecordBufferSizeInFrames = minBufferSize / Short.SIZE_BYTES,
        windowSize = windowSize,
        overlap = overlap
    )

    val outputChannel = Channel<MemoryPool<SampleData>.RefCountedMemory>(
        channelCapacity,
        BufferOverflow.SUSPEND
    )
    val memoryPool =
        MemoryPoolSampleData(2 * channelCapacity) // trial shows that single channel capacity does not very well recycle data in extreme cases, double channel capacity seems to work fine

    val job =  launch(Dispatchers.IO) {
        val record =
            if (testFunction == null)
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize
                )
            else
                null

        if (record?.state == AudioRecord.STATE_UNINITIALIZED) {
            Log.v(
                "Tuner",
                "SoundSource.createAudioRecordJob: Not able to acquire audio resource"
            )
        } else {
            record?.startRecording()
            val recordData =
                if (record != null)
                    ShortArray(record.bufferSizeInFrames / 2)
                else
                    ShortArray(minBufferSize / Short.SIZE_BYTES / 2)

            val sampleDataList = ArrayList<MemoryPool<SampleData>.RefCountedMemory>()
            var nextStartingDataFrame = 0
            var currentFrame = 0
            while (true) {
                if (!isActive)
                    break

                val numRead = if (testFunction != null) {
                    for (i in recordData.indices) {
                        recordData[i] = (Short.MAX_VALUE * testFunction!!(
                            currentFrame + i, 1f / sampleRate
                        )).toInt().toShort()
                    }
                    delay((1000 * recordData.size.toFloat() / sampleRate).toLong())
                    recordData.size
                } else if (record != null) {
                    record.read(recordData, 0, recordData.size, AudioRecord.READ_BLOCKING)
                } else {
                    0
                }

                //Log.v("TestRecordFlow", "SoundSource: numRead=$numRead, currentFrame=$currentFrame, windowSize=$windowSize")
                if (numRead > 0) {
                    // add empty sampleData objects to the data queue
                    while (nextStartingDataFrame <= currentFrame + numRead) {
                        //sampleDataList.add(SampleData(windowSize, sampleRate, nextStartingDataFrame))
                        sampleDataList.add(
                            memoryPool.get(
                                windowSize,
                                sampleRate,
                                nextStartingDataFrame
                            )
                        )
                        nextStartingDataFrame += max(
                            1,
                            ((1.0 - overlap) * windowSize).roundToInt()
                        )
                    }
                    // Log.v("TestRecordFlow", "SoundSource: sampleDataList.size = ${sampleDataList.size}, nextStartingDataFrame=$nextStartingDataFrame")
                    sampleDataList
                        .map { it.apply { memory.addData(currentFrame, recordData) } }
                        .filter { it.memory.isFull }
                        .map {
//                                Log.v("Tuner", "SoundSource: sending sample data at frame: ${it.memory.framePosition}")
                            //outputChannel.send(it)
                            val sendStatus = outputChannel.trySend(it)
                            if (!sendStatus.isSuccess)
                                it.decRef()
                        }
                    sampleDataList.removeAll { it.memory.isFull }

                    //for(s in sampleDataList)
                    //    Log.v("TestRecordFlow", "is full: ${s.isFull}")

                    currentFrame += numRead

                    waveWriter?.appendData(recordData, numRead)
                }
            }
            record?.stop()
            record?.release()
        }
    }
    return SoundSourceJob(job, outputChannel)
}

private fun computeRequiredChannelCapacity(
    audioRecordBufferSizeInFrames: Int,
    windowSize: Int,
    overlap: Float
): Int {
    val updateRateInFrames = max(1, 1 + (windowSize * (1.0 - overlap)).toInt())
//        Log.v("Tuner", "SoundSource: windowSize = $windowSize, overlap = $overlap, updateRateInFrames = $updateRateInFrames" )
    // multiply by 2, to allow writing to the output channel within one iteration
    // without loosing data and additionally have enough capacity to store a second cycle
    return max(2, 2 * audioRecordBufferSizeInFrames / updateRateInFrames)
}
