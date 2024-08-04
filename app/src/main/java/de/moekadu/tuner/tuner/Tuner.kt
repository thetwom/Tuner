package de.moekadu.tuner.tuner

import android.content.Context
import android.net.Uri
import de.moekadu.tuner.instruments.Instrument
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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Tuner(
    private val pref: PreferenceResources2,
    @Volatile private var instrument: Instrument,
    private val scope: CoroutineScope,
    private val onResultAvailableListener: OnResultAvailableListener
) {
    interface OnResultAvailableListener {
        fun onFrequencyDetected(result: FrequencyDetectionCollectedResults)
        fun onFrequencyEvaluated(result: FrequencyEvaluationResult)
    }

    private val _userDefinedTargetNote = MutableStateFlow<MusicalNote?>(null)
    private val userDefinedTargetNote = _userDefinedTargetNote.asStateFlow()

    private sealed class Command {
        data class Reconnect(
            val scope: CoroutineScope, val listener: OnResultAvailableListener
        ) : Command()
        data object Disconnect : Command()
        data object ChangePreferences : Command()
    }

    private val channel = Channel<Command>(Channel.CONFLATED)
    private val restartChannel = Channel<Command>(Channel.CONFLATED)

    private val waveWriter = WaveWriter()

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
            pref.waveWriterDurationInSeconds.collect { restartChannel.trySend(Command.ChangePreferences) }
        }

        scope.launch {
//            var job: Job? = null
            var reconnect: Command.Reconnect? = null
            merge(channel.consumeAsFlow(), restartChannel.consumeAsFlow())
                .map { v ->
                    when (v) {
                        is Command.Reconnect -> {
                            reconnect = v
                        }
                        is Command.Disconnect -> {
                            reconnect = null
                        }
                        else -> {}
                    }
                    reconnect
                }
                .buffer(1, BufferOverflow.DROP_OLDEST)
                .collectLatest { v ->
                    // Log.v("Tuner", "Tuner: buffer before delay")
                    // sometimes, several settings are changed at the same time,
                    // which would lead to many tuner restarts in a very short time.
                    // the small delay avoid these restarts.
                    delay(50)
                    // Log.v("Tuner", "Tuner: buffer after delay $v, $reconnect, $isActive")
                    if (isActive && v != null)
                        run(v.listener)
                    // Log.v("Tuner", "Tuner: after 'if run'")
            }
        }
    }
    fun connect() {
        channel.trySend(Command.Reconnect(scope, onResultAvailableListener))
    }

    fun disconnect() {
        channel.trySend(Command.Disconnect)
    }

    fun changeInstrument(instrument: Instrument) {
        this.instrument = instrument
        restartChannel.trySend(Command.ChangePreferences)
    }

    private suspend fun run(
        onResultAvailableListener: OnResultAvailableListener
    ) = coroutineScope {
//        Log.v("Tuner", "Tuner: start running again ...")
        waveWriter.setBufferSize(pref.sampleRate * pref.waveWriterDurationInSeconds.value)

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
            waveWriter = if (pref.waveWriterDurationInSeconds.value > 0) waveWriter else null
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
                instrument
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

    suspend fun storeCurrentWaveWriterSnapshot() {
        waveWriter.storeSnapshot()
    }

    suspend fun writeStoredWaveWriterSnapshot(context: Context, uri: Uri, sampleRate: Int) {
        waveWriter.writeStoredSnapshot(context, uri, sampleRate)
    }
}