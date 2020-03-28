package de.moekadu.tuner

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import kotlin.math.PI

class PostprocessorThread(val size : Int, private val uiHandler : Handler) : HandlerThread("Tuner:PreprocessorThread") {
    lateinit var handler: Handler

    companion object {
        const val POSTPROCESS_AUDIO = 300001
        const val POSTPROCESSING_FINISHED = 300002
    }

    class PostprocessingResults {
        var frequency = 0.0f
    }

    class PreprocessingDataAndPostprocessingResults(val preprocessingResults: Array<PreprocessorThread.PreprocessingResults?>,
                                                    val postprocessingResults: PostprocessingResults,
                                                    val processingInterval: Int)

    override fun onLooperPrepared() {
        super.onLooperPrepared()

        handler = object : Handler(looper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                if (msg.what == POSTPROCESS_AUDIO) {
                    //Log.v("Tuner", "PostprocessorThread:onLooperPrepared : postprocess audio")
                    when (val obj = msg.obj) {
                        is PreprocessingDataAndPostprocessingResults -> postprocessData(obj)
                    }
                }
            }
        }
    }

    private fun postprocessData(preprocessingDataAndPostprocessingResults: PreprocessingDataAndPostprocessingResults) {
        //Log.v("Tuner", "PostprocessorThread:postprocessData")
        val preprocessingResults = preprocessingDataAndPostprocessingResults.preprocessingResults
        val postprocessingResults = preprocessingDataAndPostprocessingResults.postprocessingResults
        val processingInterval = preprocessingDataAndPostprocessingResults.processingInterval
        val dt = 1.0f / MainActivity.sampleRate

        val spec1 = preprocessingResults[0]?.spectrum
        val spec2 = preprocessingResults[1]?.spectrum
        if(spec1 != null && spec2 != null) {
            //val freqIdx = preprocessingResults[0]?.idxMaxFreq ?: 0
            val freqIdx = preprocessingResults[0]?.idxMaxPitch ?: 0
            if (freqIdx > 0) {
                val freq = freqIdx * MainActivity.sampleRate / spec1.size
                val phase1 = kotlin.math.atan2(spec1[2 * freqIdx + 1], spec1[2 * freqIdx])
                val phase2 = kotlin.math.atan2(spec2[2 * freqIdx + 1], spec2[2 * freqIdx])
                val phaseErrRaw =
                    phase2 - phase1 - 2.0f * PI.toFloat() * freq * processingInterval * dt
                val phaseErr =
                    phaseErrRaw - 2.0f * PI.toFloat() * kotlin.math.round(phaseErrRaw / (2.0f * PI.toFloat()))
                postprocessingResults.frequency =
                    processingInterval * dt * freq.toFloat() / (dt * processingInterval - phaseErr / (2.0f * PI.toFloat() * freq))
            }
            else {
                postprocessingResults.frequency = 0f
            }
        }
        val message =
            uiHandler.obtainMessage(POSTPROCESSING_FINISHED, preprocessingDataAndPostprocessingResults)
        uiHandler.sendMessage(message)
    }
}