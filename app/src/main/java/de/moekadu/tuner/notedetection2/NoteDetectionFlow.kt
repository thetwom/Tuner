package de.moekadu.tuner.notedetection2

import de.moekadu.tuner.misc.MemoryPool
import de.moekadu.tuner.misc.WaveWriter
import de.moekadu.tuner.notedetection.WindowingFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

fun noteDetectionFlow(
    overlap: Float = 0.25f,
    windowSize: Int = 4096,
    sampleRate: Int = 44100,
    testFunction: ((frame: Int, dt: Float) -> Float)? = null,
    waveWriter: WaveWriter? = null,
    frequencyMin: Float,
    frequencyMax: Float,
    subharmonicsTolerance: Float = 0.05f,
    subharmonicsPeakRatio: Float = 0.9f,
    harmonicTolerance: Float = 0.1f,
    minimumFactorOverLocalMean: Float = 5f,
    maxGapBetweenHarmonics: Int = 10,
    windowType: WindowingFunction = WindowingFunction.Tophat,
    acousticWeighting: AcousticWeighting = AcousticCWeighting()
): Flow<MemoryPool<CollectedResults>.RefCountedMemory> {
    return flow {
        val soundSource = SoundSource(
            CoroutineScope(coroutineContext + Dispatchers.IO),
            overlap = overlap,
            windowSize = windowSize,
            sampleRate = sampleRate,
            testFunction = testFunction,
            waveWriter = waveWriter
        )
        val resultChannel = Channel<MemoryPool<CollectedResults>.RefCountedMemory>(2, BufferOverflow.DROP_OLDEST)

        val resultCollector = ResultCollector(
            frequencyMin, frequencyMax,
            subharmonicsTolerance, subharmonicsPeakRatio,
            harmonicTolerance, minimumFactorOverLocalMean, maxGapBetweenHarmonics,
            windowType, acousticWeighting
        )

        CoroutineScope(coroutineContext + Dispatchers.Default).launch {
            for (sampleData in soundSource.outputChannel) {
                resultChannel.send(resultCollector.collectResults(sampleData))
                sampleData.decRef() // not needed anymore
            }
        }

        for (result in resultChannel)
            emit(result)
    }
}

