package de.moekadu.tuner

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import kotlin.math.PI

class PostprocessorThread(val size : Int, val dt : Float, val processingInterval : Int, private val uiHandler : Handler) : HandlerThread("Tuner:PreprocessorThread") {
    lateinit var handler: Handler

    companion object {
        const val POSTPROCESS_AUDIO = 300001
        const val POSTPROCESSING_FINISHED = 300002
    }

    class PostprocessingResults {
        var frequencyBasedResults : FrequencyBasedPitchDetectorPost.Results? = null
    }

    private val frequencyBasedPitchDetectorPost = FrequencyBasedPitchDetectorPost(dt, processingInterval)

    var frequencyBasedResultsPrep : Array<FrequencyBasedPitchDetectorPrep.Results?>? = null

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

        if (frequencyBasedResultsPrep == null || (frequencyBasedResultsPrep?.size ?: 0) != preprocessingResults.size) {
            frequencyBasedResultsPrep =
                Array(preprocessingResults.size) { i -> preprocessingResults[i]?.frequencyBasedResults }
        }
        else {
            for (i in preprocessingResults.indices)
                frequencyBasedResultsPrep?.set(i, preprocessingResults[i]?.frequencyBasedResults)
        }

        if(postprocessingResults.frequencyBasedResults == null)
            postprocessingResults.frequencyBasedResults = FrequencyBasedPitchDetectorPost.Results()

        frequencyBasedResultsPrep?.let { prep ->
            postprocessingResults.frequencyBasedResults?.let { post ->
                frequencyBasedPitchDetectorPost.run(prep, post)
            }
        }

        val message =
            uiHandler.obtainMessage(POSTPROCESSING_FINISHED, preprocessingDataAndPostprocessingResults)
        uiHandler.sendMessage(message)
    }
}