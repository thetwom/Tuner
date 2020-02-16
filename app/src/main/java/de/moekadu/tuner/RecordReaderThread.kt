package de.moekadu.tuner

import android.media.AudioRecord
import android.os.Handler
import android.os.HandlerThread
import android.os.Message

class RecordReaderThread(val uiHandler: Handler) : HandlerThread("Tuner:RecoredReaderThread") {
    lateinit var handler : Handler

    companion object{
        const val READDATA = 200001
        const val FINISHWRITE = 200002
    }

    class RecordAndData(val record : AudioRecord, val dataBuffer : CircularRecordData.WriteBuffer) {

    }

    override fun onLooperPrepared() {
        super.onLooperPrepared()

        handler = object : Handler(looper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                if(msg.what == READDATA){
                    val obj = msg.obj
                    when(obj) {
                        is RecordAndData -> readAudioRecord(obj.record, obj.dataBuffer)
                    }
                }
            }
        }
    }

    private fun readAudioRecord(recorder: AudioRecord, dataBuffer: CircularRecordData.WriteBuffer) {
        recorder.read(dataBuffer.data, dataBuffer.offset, recorder.positionNotificationPeriod, AudioRecord.READ_BLOCKING)
        val message = uiHandler.obtainMessage(FINISHWRITE, dataBuffer)
        uiHandler.sendMessage(message)
    }


}