package de.moekadu.tuner.ui.plot

import kotlinx.collections.immutable.ImmutableList
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

interface TickLevel {
    fun getTicksRange(
        startValue: Float,
        endValue: Float,
        maxNumTicks: Int,
        labelSizeRaw: Float
    ): TicksRange

    fun getTickValue(level: Int, index: Int): Float
}

data class TicksRange(
    val level: Int,
    val indexBegin: Int,
    val indexEnd: Int
)

/// Coarse ticks come first, then get finer
class TickLevelExplicitRanges(
    private val ticks: ImmutableList<FloatArray>
) : TickLevel {
    private val minimumDistance = ticks.map {
        var dMin = Float.MAX_VALUE
        for (i in 1 until it.size)
            dMin = min(dMin, (it[i] - it[i-1]).absoluteValue)
//        Log.v("Tuner", "dmin=$dMin, size=${it.size}")
        dMin
    }
    override fun getTicksRange(
        startValue: Float,
        endValue: Float,
        maxNumTicks: Int,
        labelSizeRaw: Float
    ): TicksRange {
        var level = 0

        val minimumAllowedDistance = (endValue - startValue).absoluteValue / (maxNumTicks + 1)
        for (l in ticks.size-1 downTo 0) {
            level = l
            if (minimumDistance[l] > minimumAllowedDistance)
                break
        }

        val i0 = ticks[level].binarySearch(startValue - labelSizeRaw)
        val iBegin = if (i0 >= 0) i0 else -i0 - 1
        val i1 = ticks[level].binarySearch(endValue + labelSizeRaw)
        val iEnd = if (i1 >= 0) i1 else -i1 - 1

        return TicksRange(level, iBegin, iEnd)
    }

    override fun getTickValue(level: Int, index: Int) = ticks[level][index]
}

/// smallest deltas come first
class TickLevelDeltaBased(
    private val deltas: ImmutableList<Float>
) : TickLevel {

    override fun getTicksRange(
        startValue: Float,
        endValue: Float,
        maxNumTicks: Int,
        labelSizeRaw: Float
    ): TicksRange {
        val minimumAllowedDistance = (endValue - startValue).absoluteValue / (maxNumTicks + 1)
        val dI = deltas.binarySearch(minimumAllowedDistance)
        val deltaIndex = min(if (dI >= 0) dI else -dI - 1, deltas.size - 1)
        val delta = deltas[deltaIndex]

        val iBegin = floor((startValue - labelSizeRaw) / delta).toInt()
        val iEnd = ceil((endValue + labelSizeRaw) / delta).toInt() + 1

        return TicksRange(deltaIndex, iBegin, iEnd)
    }

    override fun getTickValue(level: Int, index: Int) = index * deltas[level]
}