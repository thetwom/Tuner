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
        const val PREPROCESS_FREQUENCYBASED = 200001
        const val PREPROCESS_AUTOCORRELATIONBASED = 200002
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
                        if (msg.what == PREPROCESS_FREQUENCYBASED) {
                            preprocessFrequencyBased(obj)
                        }
                        else if (msg.what == PREPROCESS_AUTOCORRELATIONBASED) {
                            preprocessAutocorrelationBased(obj)
                        }

                        val message = uiHandler.obtainMessage(PREPROCESSING_FINISHED, obj)
                        uiHandler.sendMessage(message)
                    }
                }

//                if (msg.what == PREPROCESS_FREQUENCYBASED) {
//                    //                    Log.v("Tuner", "test")
//                    when (val obj = msg.obj) {
//                        is ReadBufferAndProcessingResults -> proprocessFrequencyBased(obj)
//                    }
//                }
//                else if (msg.what == PREPROCESS_AUTOCORRELATIONBASED) {
//                    //                    Log.v("Tuner", "test")
//                    when (val obj = msg.obj) {
//                        is ReadBufferAndProcessingResults -> proprocessAutocorrelationBased(obj)
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
            preprocessingResults.frequencyBasedResults = FrequencyBasedPitchDetectorPrep.Results(size)

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
            preprocessingResults.autocorrelationBasedResults = AutocorrelationBasedPitchDetectorPrep.Results(size)

        preprocessingResults.autocorrelationBasedResults?.let { results ->
                autocorrelationBasedPitchDetector?.run(readBuffer, results)
        }

//        val message =
//            uiHandler.obtainMessage(PREPROCESSING_FINISHED, readBufferAndProcessingResults)
//        uiHandler.sendMessage(message)
    }
}