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
import kotlinx.coroutines.channels.Channel
import kotlin.math.max
import kotlin.math.roundToInt

/** Generator of sound samples.
 * @param scope Coroutine scope used for the sample data generator.
 */
@SuppressLint("MissingPermission")
class SoundSource(
    private val scope: CoroutineScope,
    private val overlap: Float = 0.25f,
    private val windowSize: Int = 4096,
    private val sampleRate: Int = 44100,
    private val testFunction: ((frame: Int, dt: Float) -> Float)? = null,
    private val waveWriter: WaveWriter? = null
) {
    /** Job which is generating sound. */
    private var sourceJob: Job? = null

     /** Pool which allows to recyle the sample data. */
    private var memoryPool = MemoryPoolSampleData()

    /** Channel used to communicate the sound samples. */
    val outputChannel = Channel<MemoryPool<SampleData>.RefCountedMemory>(Channel.CONFLATED)

    init {
        sourceJob = scope.launch {

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
                    "Tuner",
                    "SoundSource.createAudioRecordJob: Not able to acquire audio resource"
                )
            } else {
                record?.startRecording()

                val recordData =
                    if (record != null)
                        FloatArray(record.bufferSizeInFrames / 2)
                    else
                        FloatArray(minBufferSize / 8)

                val sampleDataList = ArrayList<MemoryPool<SampleData>.RefCountedMemory>()
                var nextStartingDataFrame = 0
                var currentFrame = 0
                while (true) {
                    if (!isActive)
                        break

                    val numRead = if (testFunction != null) {
                        for (i in recordData.indices)
                            recordData[i] = testFunction!!(currentFrame + i, 1f / sampleRate)
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
                            sampleDataList.add(memoryPool.get(windowSize, sampleRate, nextStartingDataFrame))
                            nextStartingDataFrame += max(1, ((1.0 - overlap) * windowSize).roundToInt())
                        }
                        // Log.v("TestRecordFlow", "SoundSource: sampleDataList.size = ${sampleDataList.size}, nextStartingDataFrame=$nextStartingDataFrame")
                        sampleDataList
                            .map { it.apply { memory.addData(currentFrame, recordData) } }
                            .filter { it.memory.isFull }
                            .map { outputChannel.send(it) }
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
    }

    fun stopSampling() {
        sourceJob?.cancel()
        sourceJob = null
    }
}
