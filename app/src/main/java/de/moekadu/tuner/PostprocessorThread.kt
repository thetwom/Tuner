/*
 * Copyright 2020 Michael Moessner
 *
 * This file is part of Tuner.
 *
 * Tuner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tuner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.moekadu.tuner

import android.os.Handler
import android.os.HandlerThread
import android.os.Message

class PostprocessorThread(val size : Int, private val dt : Float, private val processingInterval : Int, private val uiHandler : Handler) : HandlerThread("Tuner:PreprocessorThread") {
    lateinit var handler: Handler

    companion object {
        const val POSTPROCESS_FREQUENCY_BASED = 300001
        const val POSTPROCESS_AUTOCORRELATION_BASED = 300002
        const val POSTPROCESSING_FINISHED = 300010
    }

    class PostprocessingResults {
        var frequencyBasedResults : FrequencyBasedPitchDetectorPost.Results? = null
        var autocorrelationBasedResults : AutocorrelationBasedPitchDetectorPost.Results? = null
    }

    private var frequencyBasedPitchDetector : FrequencyBasedPitchDetectorPost ? = null
    private var autocorrelationBasedPitchDetector : AutocorrelationBasedPitchDetectorPost ? = null

    private var frequencyBasedResultsPrep : Array<FrequencyBasedPitchDetectorPrep.Results?>? = null
    private var autocorrelationBasedResultsPrep : Array<AutocorrelationBasedPitchDetectorPrep.Results?>? = null

    class PreprocessingDataAndPostprocessingResults(val preprocessingResults: Array<PreprocessorThread.PreprocessingResults?>,
                                                    val postprocessingResults: PostprocessingResults)

    override fun onLooperPrepared() {
        super.onLooperPrepared()

        handler = object : Handler(looper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                when (val obj = msg.obj) {
                    is PreprocessingDataAndPostprocessingResults -> {
                        if (msg.what == POSTPROCESS_FREQUENCY_BASED) {
                            postprocessFrequencyBased(obj)
                        }
                        else if (msg.what == POSTPROCESS_AUTOCORRELATION_BASED) {
                            postprocessAutocorrelationBased(obj)
                        }

                        val message = uiHandler.obtainMessage(POSTPROCESSING_FINISHED, obj)
                        uiHandler.sendMessage(message)
                    }
                }

//                if (msg.what == POSTPROCESS_FREQUENCY_BASED) {
//                    //Log.v("Tuner", "PostprocessorThread:onLooperPrepared : postprocess audio")
//                    when (val obj = msg.obj) {
//                        is PreprocessingDataAndPostprocessingResults -> postprocessFrequencyBased(obj)
//                    }
//                }
//                else if (msg.what == POSTPROCESS_AUTOCORRELATION_BASED) {
//                    //Log.v("Tuner", "PostprocessorThread:onLooperPrepared : postprocess audio")
//                    when (val obj = msg.obj) {
//                        is PreprocessingDataAndPostprocessingResults -> postprocessAutocorrelationBased(obj)
//                    }
//                }
            }
        }
    }

    private fun postprocessFrequencyBased(preprocessingDataAndPostprocessingResults: PreprocessingDataAndPostprocessingResults) {
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

        if(frequencyBasedPitchDetector == null)
            frequencyBasedPitchDetector =  FrequencyBasedPitchDetectorPost(dt, processingInterval)

        frequencyBasedResultsPrep?.let { prep ->
            postprocessingResults.frequencyBasedResults?.let { post ->
                frequencyBasedPitchDetector?.run(prep, post)
            }
        }
//
//        val message =
//            uiHandler.obtainMessage(POSTPROCESSING_FINISHED, preprocessingDataAndPostprocessingResults)
//        uiHandler.sendMessage(message)
    }

    private fun postprocessAutocorrelationBased(preprocessingDataAndPostprocessingResults: PreprocessingDataAndPostprocessingResults) {
        //Log.v("Tuner", "PostprocessorThread:postprocessData")
        val preprocessingResults = preprocessingDataAndPostprocessingResults.preprocessingResults
        val postprocessingResults = preprocessingDataAndPostprocessingResults.postprocessingResults

        if (autocorrelationBasedResultsPrep == null || (autocorrelationBasedResultsPrep?.size ?: 0) != preprocessingResults.size) {
            autocorrelationBasedResultsPrep =
                Array(preprocessingResults.size) { i -> preprocessingResults[i]?.autocorrelationBasedResults }
        }
        else {
            for (i in preprocessingResults.indices)
                autocorrelationBasedResultsPrep?.set(i, preprocessingResults[i]?.autocorrelationBasedResults)
        }

        if(postprocessingResults.autocorrelationBasedResults == null)
            postprocessingResults.autocorrelationBasedResults = AutocorrelationBasedPitchDetectorPost.Results()

        if(autocorrelationBasedPitchDetector == null)
            autocorrelationBasedPitchDetector = AutocorrelationBasedPitchDetectorPost(dt, processingInterval)

        autocorrelationBasedResultsPrep?.let { prep ->
            postprocessingResults.autocorrelationBasedResults?.let { post ->
                autocorrelationBasedPitchDetector?.run(prep, post)
            }
        }
//
//        val message =
//            uiHandler.obtainMessage(POSTPROCESSING_FINISHED, preprocessingDataAndPostprocessingResults)
//        uiHandler.sendMessage(message)
    }
}