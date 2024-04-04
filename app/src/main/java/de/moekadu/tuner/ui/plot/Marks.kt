package de.moekadu.tuner.ui.plot

import kotlinx.collections.immutable.ImmutableList

interface MarkLevel3 {
    fun getMarksRange(
        startValue: Float,
        endValue: Float,
        maxNumMarks: Int,
        labelHeightRaw: Float
    ): MarksRange3

    fun getMarkValue(level: Int, index: Int): Float
}

data class MarksRange3(
    val level: Int,
    val indexBegin: Int,
    val indexEnd: Int
)


class MarkLevelExplicitRanges3(
    private val marks: ImmutableList<FloatArray>
) : MarkLevel3 {
    override fun getMarksRange(
        startValue: Float,
        endValue: Float,
        maxNumMarks: Int,
        markSizeRaw: Float
    ): MarksRange3 {
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

        val i0 = marks[level].binarySearch(startValue - markSizeRaw)
        val iBegin = if (i0 >= 0) i0 else -i0 - 1
        val i1 = marks[level].binarySearch(endValue + markSizeRaw)
        val iEnd = if (i1 >= 0) i1 else -i1 - 1

        return MarksRange3(level, iBegin, iEnd)
    }

    override fun getMarkValue(level: Int, index: Int) = marks[level][index]
}