package de.moekadu.tuner.misc

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class RestartableJob(
    private val scope: CoroutineScope,
    private val name: String,
    recreateJob: () -> Job
) {
    @Volatile
    private var isRunning = false
    private val startStopChannel = Channel<StatusChange>(Channel.CONFLATED)
    private val restartIfRunningChannel = Channel<StatusChange>(Channel.CONFLATED)

    private val resolvedStartStopChannel = Channel<StatusChange>(Channel.CONFLATED)
    private enum class StatusChange {
        Restart,
        Stop,
        RestartIfRunning
    }

    init {
        // this coroutine handles the actual job starting and stopping
        scope.launch {
            Log.v("Tuner", "RestartableJob($name): launching start-stop job")
            var job: Job? = null
            for (value in resolvedStartStopChannel) {
                delay(1) // avoid unnecessary restarts if many requests come at the same time
                val resolvedValue = resolvedStartStopChannel.tryReceive().getOrNull() ?: value
                Log.v("Tuner", "RestartableJob($name): start-job job, handling $resolvedValue")
                job?.cancelAndJoin()
                job = when (resolvedValue) {
                    StatusChange.Restart -> recreateJob()
                    StatusChange.Stop -> null
                    else -> throw RuntimeException("RestartableJob: Only Restart and Stop allowed for resolvedStartStopChannel")
                }
            }
        }.invokeOnCompletion {
            Log.v("Tuner", "RestartableJob($name): stopping start-stop job")
        }

        // this coroutine handles resolves starting, stopping but also restarting
        // it sends the actual starting/stopping through the resolvedStartStopChannel
        // to the job which was launched above
        scope.launch {
            Log.v("Tuner", "RestartableJob($name): launching resolving job (start-stop/restart)")
            val startStopFlow = startStopChannel.consumeAsFlow()
            val restartIfRunningFlow = restartIfRunningChannel.consumeAsFlow()
            var isJobRunning = false
            merge(startStopFlow, restartIfRunningFlow).collect {
                when (it) {
                    StatusChange.Restart -> {
                        resolvedStartStopChannel.send(StatusChange.Restart)
                        isJobRunning = true
                    }
                    StatusChange.Stop -> {
                        resolvedStartStopChannel.send(StatusChange.Stop)
                        isJobRunning = false
                    }
                    StatusChange.RestartIfRunning -> {
                        if (isJobRunning)
                            resolvedStartStopChannel.send(StatusChange.Restart)
                    }
                }
            }
        }.invokeOnCompletion {
            Log.v("Tuner", "RestartableJob($name): stopping resolving job (start-stop/restart)")
        }
    }

    fun restart() {
        Log.v("Tuner", "RestartableJob($name): restart")
        startStopChannel.trySend(StatusChange.Restart)
    }

    fun stop() {
        Log.v("Tuner", "RestartableJob($name): stop")
        startStopChannel.trySend(StatusChange.Stop)
    }

    fun restartIfRunning() {
        Log.v("Tuner", "RestartableJob($name): restart if running")
        restartIfRunningChannel.trySend(StatusChange.RestartIfRunning)
    }
}