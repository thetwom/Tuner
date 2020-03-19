package de.moekadu.tuner

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import kotlin.math.absoluteValue
import kotlin.math.pow

class PreprocessorThread(val size : Int, private val uiHandler : Handler) : HandlerThread("Tuner:PreprocessorThread") {
    lateinit var handler: Handler

    private val fft = RealFFT(size, RealFFT.HAMMING_WINDOW)

    companion object {
        const val PREPROCESS_AUDIO = 200001
        const val PREPROCESSING_FINISHED = 200002
    }

    class PreprocessingResults(size : Int) {
        var maxValue = 0.0f
        val spectrum = FloatArray(size)
        val ampSpec = FloatArray(size/2)
        var idxMaxFreq = 0
        var idxMaxPitch = 0
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

        require(readBuffer.size == size)
        require(preprocessingResults.spectrum.size == size)

        preprocessingResults.maxValue = 0.0f
        for(i in 0 until readBuffer.size)
            preprocessingResults.maxValue = kotlin.math.max(preprocessingResults.maxValue, readBuffer[i].absoluteValue)
        //val processingResults = ProcessingResults(tmpData.max() ?: 0.0f, spectrum)

        fft.fft(readBuffer, preprocessingResults.spectrum)

        val numFrequencies = preprocessingResults.spectrum.size / 2
        preprocessingResults.idxMaxFreq = 0
        var maxAmp = 0.0f

        for(i in 0 until numFrequencies) {
            val amp = preprocessingResults.spectrum[2*i].pow(2) + preprocessingResults.spectrum[2*i+1].pow(2)
            if(amp > maxAmp){
                maxAmp = amp
                preprocessingResults.idxMaxFreq = i
            }
            preprocessingResults.ampSpec[i] = amp
        }

        var maxPitch = 0.0f
        val nHarmonics = 3
        for(i in 0 until numFrequencies) {
            var pitch = preprocessingResults.ampSpec[i]

            // instead of using exact multiples, we could find the local maximum in a certain range
            for(nH in 2 .. nHarmonics) {
                if (nH * i < numFrequencies)
                    pitch += preprocessingResults.ampSpec[nH * i]
            }
            if(pitch > maxPitch) {
                maxPitch = pitch
                preprocessingResults.idxMaxPitch = i
            }
        }

        val message =
            uiHandler.obtainMessage(PREPROCESSING_FINISHED, readBufferAndProcessingResults)
        uiHandler.sendMessage(message)
    }
}