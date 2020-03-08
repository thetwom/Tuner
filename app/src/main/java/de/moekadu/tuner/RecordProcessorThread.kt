package de.moekadu.tuner

import android.media.AudioRecord
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.reflect.typeOf

class RecordProcessorThread(val size : Int, val uiHandler : Handler) : HandlerThread("Tuner:RecordProcessorThread") {
    lateinit var handler: Handler

    val fft = RealFFT(size, RealFFT.HAMMING_WINDOW)
    val spectrum = FloatArray(size) // this should be part of the input
    val tmpData = FloatArray(size)

    companion object {
        const val PROCESS_AUDIO = 100001
        const val PROCESSING_FINISHED = 100002
    }

    class ProcessingResults(size : Int) {
        var maxValue = 0.0f
        val spectrum = FloatArray(size)
        val ampSpec = FloatArray(size/2)
        var idxMaxFreq = 0
    }

    class ReadBufferAndProcessingResults(val readBuffer: CircularRecordData.ReadBuffer, val processingResults: ProcessingResults) {

    }

    override fun onLooperPrepared() {
        super.onLooperPrepared()

        handler = object : Handler(looper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                if (msg.what == PROCESS_AUDIO) {
                    val obj = msg.obj
//                    Log.v("Tuner", "test")
                    when (obj) {
                        is ReadBufferAndProcessingResults -> processData(obj)
                    }
                }
            }
        }
    }

    fun processData(readBufferAndProcessingResults: ReadBufferAndProcessingResults) {
        val readBuffer = readBufferAndProcessingResults.readBuffer
        val processingResults = readBufferAndProcessingResults.processingResults

        require(readBuffer.size == size)
        require(processingResults.spectrum.size == size)

        readBuffer.copyToFloatArray(tmpData)

        processingResults.maxValue = 0.0f
        for(i in 0 until readBuffer.size)
            processingResults.maxValue = kotlin.math.max(processingResults.maxValue, readBuffer[i].absoluteValue)
        //val processingResults = ProcessingResults(tmpData.max() ?: 0.0f, spectrum)

        fft.fft(readBuffer, processingResults.spectrum)

        val nFreqs = processingResults.spectrum.size / 2
        processingResults.idxMaxFreq = 0
        var maxAmp = 0.0f
        for(i in 0 until nFreqs) {
            val amp = processingResults.spectrum[2*i].pow(2) + processingResults.spectrum[2*i+1].pow(2)
            if(amp > maxAmp){
                maxAmp = amp
                processingResults.idxMaxFreq = i
            }
            processingResults.ampSpec[i] = amp
        }

        val message =
            uiHandler.obtainMessage(PROCESSING_FINISHED, readBufferAndProcessingResults)
        uiHandler.sendMessage(message)
    }
}