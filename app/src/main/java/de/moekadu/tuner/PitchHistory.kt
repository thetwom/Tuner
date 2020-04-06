package de.moekadu.tuner

class PitchHistory(val size : Int) {
    private val pitchArray = FloatArray(size) { 0.0f }

    private var numFaultyValues = 0
    private val maybeFaultyValues = FloatArray(3) { 0.0f }

    private val allowedRatioToBeValid = 1.04f

    fun getCurrentValue() : Float {
        return pitchArray.last()
    }

    fun getHistory() : FloatArray {
        return pitchArray
    }

    fun addValue(value : Float) {
        val lastValue = pitchArray.last()

        if(value < allowedRatioToBeValid * lastValue && value > lastValue / allowedRatioToBeValid) {
            pitchArray.copyInto(pitchArray, 0, 1)
            pitchArray[pitchArray.lastIndex] = value
            return
        }

        if(numFaultyValues == 0) {
            maybeFaultyValues[0] = value
            return
        }

        val lastFaultyValue = maybeFaultyValues[numFaultyValues-1]
        if(value < allowedRatioToBeValid * lastFaultyValue && value > lastFaultyValue / allowedRatioToBeValid) {
            maybeFaultyValues[numFaultyValues] = value
            ++numFaultyValues
        }
        else {
            numFaultyValues = 0
            return
        }

        if(numFaultyValues == maybeFaultyValues.size) {
            pitchArray.copyInto(pitchArray, 0, maybeFaultyValues.size)
            maybeFaultyValues.copyInto(pitchArray, pitchArray.size-maybeFaultyValues.size, 0)
            numFaultyValues = 0
        }
        return
    }
}