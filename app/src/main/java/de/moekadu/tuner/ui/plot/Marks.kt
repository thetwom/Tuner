package de.moekadu.tuner.ui.plot

import kotlinx.collections.immutable.ImmutableList

interface MarkLevel {
    fun getMarksRange(
        startValue: Float,
        endValue: Float,
        maxNumMarks: Int,
        labelSizeRaw: Float
    ): MarksRange

    fun getMarkValue(level: Int, index: Int): Float
}

data class MarksRange(
    val level: Int,
    val indexBegin: Int,
    val indexEnd: Int
)

class MarkLevelExplicitRanges(
    private val marks: ImmutableList<FloatArray>
) : MarkLevel {
    override fun getMarksRange(
        startValue: Float,
        endValue: Float,
        maxNumMarks: Int,
        labelSizeRaw: Float
    ): MarksRange {
        var level = 0

        for (l in marks.size-1 downTo 0) {
            level = l
            val m = marks[l]
            val i0 = m.binarySearch(startValue)
            val iBegin = if (i0 >= 0) i0 else -i0 - 1
            val i1 = m.binarySearch(endValue)
            val iEnd = if (i1 >= 0) i1 else -i1 - 1

            if (iEnd - iBegin <= maxNumMarks)
                break
        }

        val i0 = marks[level].binarySearch(startValue - labelSizeRaw)
        val iBegin = if (i0 >= 0) i0 else -i0 - 1
        val i1 = marks[level].binarySearch(endValue + labelSizeRaw)
        val iEnd = if (i1 >= 0) i1 else -i1 - 1

        return MarksRange(level, iBegin, iEnd)
    }

    override fun getMarkValue(level: Int, index: Int) = marks[level][index]
}