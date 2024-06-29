package de.moekadu.tuner.tuner

import de.moekadu.tuner.instruments.InstrumentResources
import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.misc.MemoryPool
import de.moekadu.tuner.misc.WaveWriter
import de.moekadu.tuner.notedetection.AcousticZeroWeighting
import de.moekadu.tuner.notedetection.FrequencyDetectionCollectedResults
import de.moekadu.tuner.notedetection.FrequencyDetectionResultCollector
import de.moekadu.tuner.notedetection.FrequencyEvaluationResult
import de.moekadu.tuner.notedetection.FrequencyEvaluator
import de.moekadu.tuner.notedetection.launchSoundSourceJob
import de.moekadu.tuner.notedetection.testFunction
import de.moekadu.tuner.preferences.PreferenceResources2
import de.moekadu.tuner.temperaments.MusicalNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Tuner(
    private val pref: PreferenceResources2,
    private val instruments: InstrumentResources,
    private val scope: CoroutineScope,
    private val onResultAvailableListener: OnResultAvailableListener
) {
    interface OnResultAvailableListener {
        fun onFrequencyDetected(result: FrequencyDetectionCollectedResults)
        fun onFrequencyEvaluated(result: FrequencyEvaluationResult)
    }

    /** Writer class which allows dumping recorded data into wav-files. */
    val waveWriter = WaveWriter() // TODO: instantiate this only if enabled?

    private val _userDefinedTargetNote = MutableStateFlow<MusicalNote?>(null)
    private val userDefinedTargetNote = _userDefinedTargetNote.asStateFlow()

    private sealed class Command {
        data class Reconnect(val scope: CoroutineScope, val listener: OnResultAvailableListener) : Command()
        data object Disconnect : Command()
        data object ChangePreferences : Command()
    }

    private val channel = Channel<Command>(Channel.CONFLATED)
    private val restartChannel = Channel<Command>(Channel.CONFLATED)

    init {
        scope.launch {
            pref.overlap.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            pref.windowSize.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            pref.windowing.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            pref.numMovingAverage.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            pref.toleranceInCents.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            pref.pitchHistoryNumFaultyValues.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            pref.maxNoise.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            pref.minHarmonicEnergyContent.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            pref.sensitivity.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            pref.musicalScale.collect { restartChannel.trySend(Command.ChangePreferences) }
        }
        scope.launch {
            instruments.instrument.collect { restartChannel.trySend(Command.ChangePreferences) }
        }

        scope.launch {
            var job: Job? = null
            var reconnect: Command.Reconnect? = null
            merge(channel.consumeAsFlow(), restartChannel.consumeAsFlow()).collect { v ->
                job?.cancelAndJoin()
                job = when (v) {
                    is Command.Reconnect -> {
                        reconnect = v
                        v.scope.launch { run(v.listener) }
                    }
                    is Command.Disconnect -> {
                        reconnect = null
                        null
                    }
                    is Command.ChangePreferences -> {
                        reconnect?.let { it.scope.launch { run(it.listener) } }
                    }
                }
            }
        }
    }
    fun connect() {
        channel.trySend(Command.Reconnect(scope, onResultAvailableListener))
    }

    fun disconnect() {
        channel.trySend(Command.Disconnect)
    }
    private suspend fun run(onResultAvailableListener: OnResultAvailableListener)
            = coroutineScope {

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
            waveWriter = waveWriter
        )

        launch(Dispatchers.Default) {
            val frequencyDetectionResultCollector = FrequencyDetectionResultCollector(
                frequencyMin = DefaultValues.FREQUENCY_MIN,
                frequencyMax = DefaultValues.FREQUENCY_MAX,
                subharmonicsTolerance = 0.05f,
                subharmonicsPeakRatio = 0.8f,
                harmonicTolerance = 0.1f,
                minimumFactorOverLocalMean = 2f,
                maxGapBetweenHarmonics = 10,
                maxNumHarmonicsForInharmonicity = 8,
                windowType = pref.windowing.value,
                acousticWeighting = AcousticZeroWeighting()
            )

            for (sampleData in soundSourceJobAndChannel.channel) {
                val result = frequencyDetectionResultCollector.collectResults(sampleData)
                result.incRef()
                frequencyDetectionResultsChannel.trySend(result)
                sampleData.decRef() // sampleData is not needed anymore, so we can decrement ref to allow recycling
                withContext(Dispatchers.Main) {
                    onResultAvailableListener.onFrequencyDetected(result.memory) // better send this to freqEval flow and directlya afterwards run the listener
                }
                result.decRef()
            }
        }

        launch(Dispatchers.Main) {
            val freqEvaluator = FrequencyEvaluator(
                pref.numMovingAverage.value,
                pref.toleranceInCents.value.toFloat(),
                pref.pitchHistoryNumFaultyValues.value,
                pref.maxNoise.value,
                pref.minHarmonicEnergyContent.value,
                pref.sensitivity.value.toFloat(),
                pref.musicalScale.value,
                instruments.instrument.value.instrument,
            )

            frequencyDetectionResultsChannel
                .consumeAsFlow()
                .combine(userDefinedTargetNote) {  noteDetectionResult, targetNote ->
                    val result = freqEvaluator.evaluate(noteDetectionResult.memory, targetNote)
                    noteDetectionResult.decRef()
                    result
                }
                .flowOn(Dispatchers.Default)
                .collect {
                    onResultAvailableListener.onFrequencyEvaluated(it)
                }
        }


    }
}