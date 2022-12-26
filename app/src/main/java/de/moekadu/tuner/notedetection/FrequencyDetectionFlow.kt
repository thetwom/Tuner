package de.moekadu.tuner.notedetection

import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.misc.MemoryPool
import de.moekadu.tuner.misc.WaveWriter
import de.moekadu.tuner.preferences.PreferenceResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

fun frequencyDetectionFlow(pref: PreferenceResources, waveWriter: WaveWriter?) = frequencyDetectionFlow(
    overlap = pref.overlap.value,
    windowSize = pref.windowSize.value,
    sampleRate = DefaultValues.SAMPLE_RATE,
    testFunction = testFunction,
    waveWriter = waveWriter,
    frequencyMin = DefaultValues.FREQUENCY_MIN,
    frequencyMax = DefaultValues.FREQUENCY_MAX,
    subharmonicsTolerance = 0.05f,
    subharmonicsPeakRatio = 0.9f,
    harmonicTolerance = 0.1f,
    minimumFactorOverLocalMean = 5f,
    maxGapBetweenHarmonics = 10,
    maxNumHarmonicsForInharmonicity = 8,
    windowType = pref.windowing.value,
    acousticWeighting = AcousticZeroWeighting() //AcousticCWeighting() // TODO: set weighting from preferences, also in TunerViewModel
)


fun frequencyDetectionFlow(
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
    maxNumHarmonicsForInharmonicity: Int = 8,
    windowType: WindowingFunction = WindowingFunction.Tophat,
    acousticWeighting: AcousticWeighting = AcousticCWeighting()
): Flow<MemoryPool<FrequencyDetectionCollectedResults>.RefCountedMemory> {
    return flow {
        val soundSource = SoundSource(
            CoroutineScope(coroutineContext + Dispatchers.IO),
            overlap = overlap,
            windowSize = windowSize,
            sampleRate = sampleRate,
            testFunction = testFunction,
            waveWriter = waveWriter
        )
        val resultChannel = Channel<MemoryPool<FrequencyDetectionCollectedResults>.RefCountedMemory>(3, BufferOverflow.DROP_OLDEST)

        val frequencyDetectionResultCollector = FrequencyDetectionResultCollector(
            frequencyMin, frequencyMax,
            subharmonicsTolerance, subharmonicsPeakRatio,
            harmonicTolerance, minimumFactorOverLocalMean, maxGapBetweenHarmonics,
            maxNumHarmonicsForInharmonicity,
            windowType, acousticWeighting
        )

        CoroutineScope(coroutineContext + Dispatchers.Default).launch {
            for (sampleData in soundSource.outputChannel) {
//                Log.v("Tuner", "frequencyDetectionFlow: collecting sample data: time = ${sampleData.memory.framePosition}")
                resultChannel.send(frequencyDetectionResultCollector.collectResults(sampleData))
                sampleData.decRef() // not needed anymore
            }
        }

        for (result in resultChannel)
            emit(result)
    }
}

