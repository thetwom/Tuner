package de.moekadu.tuner.ui.plot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times

class HorizontalMark(
    horizontalLabelPosition: HorizontalLabelPosition,
    anchor: Anchor,
    maximumSize: DpSize
) : PlotItem {
    override val boundingBox = mutableStateOf(
        Rect(//0f, 30f, 15f, 15f
            Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY
        )
    )
    override val extraExtentsOnScreen = DpRect(
        left = if (horizontalLabelPosition == HorizontalLabelPosition.Left) {
            when (anchor){
                Anchor.North, Anchor.Center, Anchor.South -> 0.5f * maximumSize.width
                Anchor.NorthEast, Anchor.East, Anchor.SouthEast -> maximumSize.width
                else -> 0.dp
            }
        } else {
            0.dp
        },
        top = when (anchor) {
            Anchor.NorthWest, Anchor.North, Anchor.NorthEast -> 0.dp
            Anchor.West, Anchor.Center, Anchor.East -> 0.5f * maximumSize.height
            Anchor.SouthWest, Anchor.South, Anchor.SouthEast -> maximumSize.height
        },
        right = if (horizontalLabelPosition == HorizontalLabelPosition.Right) {
            when (anchor){
                Anchor.North, Anchor.Center, Anchor.South -> 0.5f * maximumSize.width
                Anchor.NorthWest, Anchor.West, Anchor.SouthWest -> maximumSize.width
                else -> 0.dp
            }
        } else {
            0.dp
        },
        bottom = when (anchor) {
            Anchor.NorthWest, Anchor.North, Anchor.NorthEast -> maximumSize.height
            Anchor.West, Anchor.Center, Anchor.East -> 0.5f * maximumSize.height
            Anchor.SouthWest, Anchor.South, Anchor.SouthEast -> 0.dp
        }
    )

    @Composable
    override fun Item(transformToScreen: Matrix) {

        TODO("Not yet implemented")
    }
}