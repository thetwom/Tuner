package de.moekadu.tuner.tuner

import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.misc.MemoryPool
import de.moekadu.tuner.notedetection.AcousticZeroWeighting
import de.moekadu.tuner.notedetection.FrequencyDetectionCollectedResults
import de.moekadu.tuner.notedetection.FrequencyDetectionResultCollector
import de.moekadu.tuner.notedetection.FrequencyEvaluatorSimple
import de.moekadu.tuner.notedetection.launchSoundSourceJob
import de.moekadu.tuner.notedetection.testFunction
import de.moekadu.tuner.preferences.PreferenceResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Simple frequency detector.
 * This does not listen to changes of preferences and only detects frequencies, not notes.
 * @note Tuner starts after calling `connect()` and pauses when calling `disconnect()`.
 * @param scope Coroutine scope where to run the frequency detection.
 * @param onFrequencyAvailable Callback which is called on Dispatchers.Main, when a new frequency
 *   becomes available.
 * @param pref Preferences from which we take the detection parameters.
 */
class SimpleFrequencyDetector(
    private val scope: CoroutineScope,
    private val onFrequencyAvailable: (frequency: Float) -> Unit,
    private val pref: PreferenceResources
) {
    private enum class Command { Reconnect, Disconnect }
    private val channel = Channel<Command>(Channel.CONFLATED)

    init {
        scope.launch {
            channel.consumeAsFlow()
                .collectLatest { command ->
                    if (isActive && command == Command.Reconnect)
                        run()
                    // Log.v("Tuner", "Tuner: after 'if run'")
                }
        }
    }

    /** Start frequency detection. */
    fun connect() {
        channel.trySend(Command.Reconnect)
    }

    /** Pause frequency detection. */
    fun disconnect() {
        channel.trySend(Command.Disconnect)
    }

    private suspend fun run() = coroutineScope {
//        Log.v("Tuner", "Tuner: start running again ...")

        val frequencyDetectionResultsChannel =
            Channel<MemoryPool<FrequencyDetectionCollectedResults>.RefCountedMemory>(
                capacity = 2,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
                onUndeliveredElement = { launch { it.decRef() } }
            )

        val soundSourceJobAndChannel = launchSoundSourceJob(
            overlap = pref.overlap.value,
            windowSize = pref.windowSize.value,
            sampleRate = pref.sampleRate,
            testFunction = testFunction,
            waveWriter = null
        )

        launch(Dispatchers.Default) {
            val frequencyDetectionResultCollector = FrequencyDetectionResultCollector(
                frequencyMin = DefaultValues.FREQUENCY_MIN,
                frequencyMax = DefaultValues.FREQUENCY_MAX,
                subharmonicsTolerance = 0.1f,
                subharmonicsPeakRatio = 0.75f,
                harmonicTolerance = 0.11f,
                minimumFactorOverLocalMean = 3f,
                maxGapBetweenHarmonics = 5,
                maxNumHarmonicsForInharmonicity = 8,
                windowType = pref.windowing.value,
                acousticWeighting = AcousticZeroWeighting()
            )

            for (sampleData in soundSourceJobAndChannel.channel) {
                val result = frequencyDetectionResultCollector.collectResults(sampleData)
                result.incRef()
                frequencyDetectionResultsChannel.trySend(result)
                sampleData.decRef() // sampleData is not needed anymore, so we can decrement ref to allow recycling
                result.decRef()
            }
        }

        launch(Dispatchers.Main) {
            val freqEvaluator = FrequencyEvaluatorSimple(
                pref.numMovingAverage.value,
                pref.pitchHistoryNumFaultyValues.value,
                pref.maxNoise.value,
                pref.minHarmonicEnergyContent.value,
                pref.sensitivity.value.toFloat()
            )

            frequencyDetectionResultsChannel
                .consumeAsFlow()
                .map { noteDetectionResult ->
                    val result = freqEvaluator.evaluate(noteDetectionResult.memory)
                    noteDetectionResult.decRef()
                    result
                }
                .flowOn(Dispatchers.Default)
                .collect {
                    if (it > 0f)
                        onFrequencyAvailable(it)
                }
        }
    }
}