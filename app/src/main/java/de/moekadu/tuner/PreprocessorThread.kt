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
import android.util.Log


/// Preprocess a time spectrum
/**
 * @param size Number of audio data points which are processed here
 * @param dt Time step width between to two input data points
 * @param minimumFrequency Minimum allowed frequency which we should detect
 * @param maximumFrequency Maximum allowed frequency which we should detect
 * @param uiHandler handler of ui thread, where we send our results
 */
class PreprocessorThread(val size : Int, private val dt : Float, private val minimumFrequency : Float, private val maximumFrequency : Float,
                         private val uiHandler : Handler) : HandlerThread("Tuner:PreprocessorThread") {
    lateinit var handler: Handler

    private var frequencyBasedPitchDetector : FrequencyBasedPitchDetectorPrep? = null
    private var autocorrelationBasedPitchDetector : AutocorrelationBasedPitchDetectorPrep? = null

    companion object {
        const val PREPROCESS_FREQUENCY_BASED = 200001
        const val PREPROCESS_AUTOCORRELATION_BASED = 200002
        const val PREPROCESSING_FINISHED = 200010
    }

    class PreprocessingResults {
        var frequencyBasedResults : FrequencyBasedPitchDetectorPrep.Results? = null
        var autocorrelationBasedResults : AutocorrelationBasedPitchDetectorPrep.Results? = null
    }

    class ReadBufferAndProcessingResults(val readBuffer: CircularRecordData.ReadBuffer, val preprocessingResults: PreprocessingResults)

    override fun onLooperPrepared() {
        super.onLooperPrepared()

        handler = object : Handler(looper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                when (val obj = msg.obj) {
                    is ReadBufferAndProcessingResults -> {
                        if (msg.what == PREPROCESS_FREQUENCY_BASED) {
                            preprocessFrequencyBased(obj)
                        }
                        else if (msg.what == PREPROCESS_AUTOCORRELATION_BASED) {
                            preprocessAutocorrelationBased(obj)
                        }

                        val message = uiHandler.obtainMessage(PREPROCESSING_FINISHED, obj)
                        uiHandler.sendMessage(message)
                    }
                }

//                if (msg.what == PREPROCESS_FREQUENCY_BASED) {
//                    //                    Log.v("Tuner", "test")
//                    when (val obj = msg.obj) {
//                        is ReadBufferAndProcessingResults -> preprocessFrequencyBased(obj)
//                    }
//                }
//                else if (msg.what == PREPROCESS_AUTOCORRELATION_BASED) {
//                    //                    Log.v("Tuner", "test")
//                    when (val obj = msg.obj) {
//                        is ReadBufferAndProcessingResults -> preprocessAutocorrelationBased(obj)
//                    }
//                }
            }
        }
    }

    private fun preprocessFrequencyBased(readBufferAndProcessingResults: ReadBufferAndProcessingResults) {
        val readBuffer = readBufferAndProcessingResults.readBuffer
        val preprocessingResults = readBufferAndProcessingResults.preprocessingResults

        if (frequencyBasedPitchDetector == null)
            frequencyBasedPitchDetector = FrequencyBasedPitchDetectorPrep(size, dt, minimumFrequency, maximumFrequency)

        if (preprocessingResults.frequencyBasedResults == null)
            preprocessingResults.frequencyBasedResults = FrequencyBasedPitchDetectorPrep.Results(size, dt)

        preprocessingResults.frequencyBasedResults?.let { results ->
            frequencyBasedPitchDetector?.run(readBuffer, results)
        }
//        val message =
//            uiHandler.obtainMessage(PREPROCESSING_FINISHED, readBufferAndProcessingResults)
//        uiHandler.sendMessage(message)
    }

    private fun preprocessAutocorrelationBased(readBufferAndProcessingResults: ReadBufferAndProcessingResults) {
        Log.v("Tuner", "PreprocessorThread.preprocessAutocorrelationBased")
        val readBuffer = readBufferAndProcessingResults.readBuffer
        val preprocessingResults = readBufferAndProcessingResults.preprocessingResults

        if (autocorrelationBasedPitchDetector == null)
            autocorrelationBasedPitchDetector = AutocorrelationBasedPitchDetectorPrep(size, dt, minimumFrequency, maximumFrequency)

        if (preprocessingResults.autocorrelationBasedResults == null)
            preprocessingResults.autocorrelationBasedResults = AutocorrelationBasedPitchDetectorPrep.Results(size, dt)

        preprocessingResults.autocorrelationBasedResults?.let { results ->
                autocorrelationBasedPitchDetector?.run(readBuffer, results)
        }

//        val message =
//            uiHandler.obtainMessage(PREPROCESSING_FINISHED, readBufferAndProcessingResults)
//        uiHandler.sendMessage(message)
    }
}