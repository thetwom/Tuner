package de.moekadu.tuner

import android.media.AudioRecord
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import kotlin.reflect.typeOf

class RecordProcessorThread(val uiHandler : Handler) : HandlerThread("Tuner:RecordProcessorThread") {
    lateinit var handler: Handler

    var tmpData : FloatArray ?= null

    companion object {
        const val PROCESS_AUDIO = 100001
        const val PROCESSING_FINISHED = 100002
    }

    class ProcessingResults {
        var maxValue = 0.0f
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
                        is CircularRecordData.ReadBuffer -> processData(obj)
                    }
                }
            }
        }
    }

    fun processData(readBuffer: CircularRecordData.ReadBuffer) {
        if (tmpData == null || tmpData?.size != readBuffer.size)
            tmpData = FloatArray(readBuffer.size)
        tmpData?.let {
            readBuffer.copyToFloatArray(it)

            val processingResults = ProcessingResults()
            processingResults.maxValue = it.max() ?: 0.0f
            val readBufferAndProcessingResults =
                ReadBufferAndProcessingResults(readBuffer, processingResults)
            val message =
                uiHandler.obtainMessage(PROCESSING_FINISHED, readBufferAndProcessingResults)
            uiHandler.sendMessage(message)
        }
    }
}