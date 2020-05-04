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

import android.media.AudioRecord
import android.os.Handler
import android.os.HandlerThread
import android.os.Message

class RecordReaderThread(private val uiHandler: Handler) : HandlerThread("Tuner:RecordReaderThread") {
    lateinit var handler : Handler

    companion object{
        const val READ_DATA = 100001
        const val FINISH_WRITE = 100002
    }

    class RecordAndData(val record : AudioRecord, val dataBuffer : CircularRecordData.WriteBuffer)

    override fun onLooperPrepared() {
        super.onLooperPrepared()

        handler = object : Handler(looper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                if(msg.what == READ_DATA){
                    when(val obj = msg.obj) {
                        is RecordAndData -> readAudioRecord(obj.record, obj.dataBuffer)
                    }
                }
            }
        }
    }

    private fun readAudioRecord(recorder: AudioRecord, dataBuffer: CircularRecordData.WriteBuffer) {
        recorder.read(dataBuffer.data, dataBuffer.offset, recorder.positionNotificationPeriod, AudioRecord.READ_BLOCKING)
        val message = uiHandler.obtainMessage(FINISH_WRITE, dataBuffer)
        uiHandler.sendMessage(message)
    }


}