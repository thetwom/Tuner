package de.moekadu.tuner.notedetection

import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.misc.MemoryPool
import de.moekadu.tuner.misc.WaveWriter
import de.moekadu.tuner.preferences.PreferenceResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    subharmonicsPeakRatio = 0.8f,
    harmonicTolerance = 0.1f,
    minimumFactorOverLocalMean = 2f,
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
    subharmonicsPeakRatio: Float = 0.8f,
    harmonicTolerance: Float = 0.1f,
    minimumFactorOverLocalMean: Float = 2f,
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

        val frequencyDetectionResultCollector = FrequencyDetectionResultCollector(
            frequencyMin, frequencyMax,
            subharmonicsTolerance, subharmonicsPeakRatio,
            harmonicTolerance, minimumFactorOverLocalMean, maxGapBetweenHarmonics,
            maxNumHarmonicsForInharmonicity,
            windowType, acousticWeighting
        )

//        var lastUpdate = 0
        for (sampleData in soundSource.outputChannel) {
//            val diff = sampleData.memory.framePosition - lastUpdate
//            lastUpdate = sampleData.memory.framePosition
//            Log.v("Tuner", "frequencyDetectionFlow: collecting sample data: time = ${sampleData.memory.framePosition}, diff = $diff")
            val result = frequencyDetectionResultCollector.collectResults(sampleData)
            sampleData.decRef() // sampleData is not needed anymore, so we can decrement ref to allow recycling
            emit(result)
        }
    }
}

