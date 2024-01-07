package de.moekadu.tuner.misc

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
            var job: Job? = null
            for (value in resolvedStartStopChannel) {
                delay(1) // avoid unnecessary restarts if many requests come at the same time
                val resolvedValue = resolvedStartStopChannel.tryReceive().getOrNull() ?: value
                job?.cancelAndJoin()
                job = when (resolvedValue) {
                    StatusChange.Restart -> recreateJob()
                    StatusChange.Stop -> null
                    else -> throw RuntimeException("RestartableJob: Only Restart and Stop allowed for resolvedStartStopChannel")
                }
            }
        }

        // this coroutine handles resolves starting, stopping but also restarting
        // it sends the actual starting/stopping through the resolvedStartStopChannel
        // to the job which was launched above
        scope.launch {
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
        }
    }

    fun restart() {
        startStopChannel.trySend(StatusChange.Restart)
    }

    fun stop() {
        startStopChannel.trySend(StatusChange.Stop)
    }

    fun restartIfRunning() {
        restartIfRunningChannel.trySend(StatusChange.RestartIfRunning)
    }
}