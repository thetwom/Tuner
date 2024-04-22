package de.moekadu.tuner.ui.plot

import kotlinx.collections.immutable.ImmutableList

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

class TickLevelExplicitRanges(
    private val ticks: ImmutableList<FloatArray>
) : TickLevel {
    override fun getTicksRange(
        startValue: Float,
        endValue: Float,
        maxNumTicks: Int,
        labelSizeRaw: Float
    ): TicksRange {
        var level = 0

        for (l in ticks.size-1 downTo 0) {
            level = l
            val m = ticks[l]
            val i0 = m.binarySearch(startValue)
            val iBegin = if (i0 >= 0) i0 else -i0 - 1
            val i1 = m.binarySearch(endValue)
            val iEnd = if (i1 >= 0) i1 else -i1 - 1

            if (iEnd - iBegin <= maxNumTicks)
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