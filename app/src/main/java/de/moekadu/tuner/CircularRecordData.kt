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

import android.util.Log
import kotlin.math.min

class CircularRecordData(size : Int) {

    inner class ReadBuffer(val startRead : Int, val size : Int) {
        private val dataIdxStart = dataIdx(startRead)
        var locked = true

        fun copyToFloatArray(floatArray: FloatArray) {
            require(floatArray.size <= size)
            data.copyInto(floatArray, 0, dataIdxStart, min(data.size, dataIdxStart + floatArray.size))
            val nRemain = dataIdxStart + floatArray.size - data.size
            if(nRemain > 0)
                data.copyInto(floatArray, floatArray.size - nRemain, 0, nRemain)
        }

        operator fun get(i : Int): Float {
            require(i < size)
            val idx = (dataIdxStart + i) % data.size
            return data[idx]
        }
    }

    inner class WriteBuffer(val startWrite: Int, val size : Int, val data : FloatArray) {
        val offset = dataIdx(startWrite)
        var locked = true
    }

    val data = FloatArray(size)
    private var idxMax = 0

    private val readBufferStartRead = ArrayList<Int>()

    private var numLockWrite = 0

    fun dataIdx(idx : Int) : Int {
        return idx % data.size
    }

    fun lockWrite(num : Int) : WriteBuffer?{
        val startIdxWrite = idxMax
        val endIdxWrite = startIdxWrite + num
        val minStartIdxRead = readBufferStartRead.min() ?: idxMax
        //Log.v("Tuner", "CircularRecordData:lockWrite: startIdxWrite="+ startIdxWrite)
        if(numLockWrite > 0 || endIdxWrite > minStartIdxRead + data.size) {
            Log.v("Tuner", "CircularRecordData:lockWrite: Refusing to grant write access, numLockWrite="+numLockWrite+" endIdxWrite="+endIdxWrite+" minStartIdxRead+data.size="+(minStartIdxRead+data.size))
            return null
        }
        require(data.size - dataIdx(idxMax) >= num) {"Writer buffer size to large -> data cannot be written inline"}

        numLockWrite = num
        return WriteBuffer(idxMax, num, data)
    }

    fun unlockWrite(writeBuffer: WriteBuffer) {
        require(writeBuffer.locked) {"Write buffer has already been unlocked"}
        idxMax += writeBuffer.size
        numLockWrite = 0
        writeBuffer.locked = false
    }

    fun lockRead(startIdx : Int, num : Int) : ReadBuffer{
        require(startIdx + num <= idxMax && startIdx >= idxMax - data.size + numLockWrite) {"Read buffer cannot be locked"}
        //Log.v("Tuner", "CircularRecordData:lockRead: startIdxRead="+ startIdx)
        readBufferStartRead.add(startIdx)
        return ReadBuffer(startIdx, num)
    }

    fun unlockRead(readBuffer: ReadBuffer){
        require(readBuffer.locked) {"Read buffer has already been unlocked"}
        readBufferStartRead.remove(readBuffer.startRead)
        readBuffer.locked = false
    }
}