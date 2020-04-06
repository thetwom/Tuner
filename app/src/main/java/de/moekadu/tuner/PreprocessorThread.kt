package de.moekadu.tuner

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import kotlin.math.*


/// Preprocess a time spectrum
/**
 * @param size Number of audio data points which are processed here
 * @param dt Time step width between to two input data points
 * @param minimumFrequency Minimum allowed frequency which we sould detect
 * @param maximumFrequency Maximum allowd frequency which we should detect
 * @param uiHandler handler of ui thread, where we send oure results
 */
class PreprocessorThread(val size : Int, val dt : Float, val minimumFrequency : Float, val maximumFrequency : Float,
                         private val uiHandler : Handler) : HandlerThread("Tuner:PreprocessorThread") {
    lateinit var handler: Handler

    private val frequencyBasedPitchDetector = FrequencyBasedPitchDetectorPrep(size, dt, minimumFrequency, maximumFrequency)

    companion object {
        const val PREPROCESS_AUDIO = 200001
        const val PREPROCESSING_FINISHED = 200002
    }

    class PreprocessingResults {
        var frequencyBasedResults : FrequencyBasedPitchDetectorPrep.Results? = null
    }

    class ReadBufferAndProcessingResults(val readBuffer: CircularRecordData.ReadBuffer, val preprocessingResults: PreprocessingResults)

    override fun onLooperPrepared() {
        super.onLooperPrepared()

        handler = object : Handler(looper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                if (msg.what == PREPROCESS_AUDIO) {
                    //                    Log.v("Tuner", "test")
                    when (val obj = msg.obj) {
                        is ReadBufferAndProcessingResults -> preprocessData(obj)
                    }
                }
            }
        }
    }

    private fun preprocessData(readBufferAndProcessingResults: ReadBufferAndProcessingResults) {
        val readBuffer = readBufferAndProcessingResults.readBuffer
        val preprocessingResults = readBufferAndProcessingResults.preprocessingResults

        if (preprocessingResults.frequencyBasedResults == null)
            preprocessingResults.frequencyBasedResults = FrequencyBasedPitchDetectorPrep.Results(size)
        preprocessingResults.frequencyBasedResults?.let { results ->
            frequencyBasedPitchDetector.run(readBuffer, results)
        }

        val message =
            uiHandler.obtainMessage(PREPROCESSING_FINISHED, readBufferAndProcessingResults)
        uiHandler.sendMessage(message)
    }
}