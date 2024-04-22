package de.moekadu.tuner.ui.plot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt


enum class Anchor {
    NorthWest, North,  NorthEast,
    West,      Center, East,
    SouthWest, South,  SouthEast,
}

fun Anchor.place(x: Float, y: Float, w: Float, h: Float): Offset {
    return when (this) {
        Anchor.NorthWest -> Offset(x, y)
        Anchor.North -> Offset(x - 0.5f * w, y)
        Anchor.NorthEast -> Offset(x - w, y)
        Anchor.West -> Offset(x, y - 0.5f * h)
        Anchor.Center -> Offset(x - 0.5f * w, y - 0.5f * h)
        Anchor.East -> Offset(x - w, y - 0.5f * h)
        Anchor.SouthWest -> Offset(x, y - h)
        Anchor.South -> Offset(x - 0.5f * w, y - h)
        Anchor.SouthEast -> Offset(x - w, y - h)
    }
}

fun Anchor.place(
    x: Float, y: Float, w: Float, h: Float,
    horizontalLineWidth: Float, verticalLineWidth: Float
): Offset {
    val hL = 0.5f * horizontalLineWidth
    val vL = 0.5f * verticalLineWidth
    return when (this) {
        Anchor.NorthWest -> Offset(x + vL, y + hL)
        Anchor.North -> Offset(x - 0.5f * w, y + hL)
        Anchor.NorthEast -> Offset(x - w - vL, y + hL)
        Anchor.West -> Offset(x + vL, y - 0.5f * h)
        Anchor.Center -> Offset(x - 0.5f * w, y - 0.5f * h)
        Anchor.East -> Offset(x - w - vL, y - 0.5f * h)
        Anchor.SouthWest -> Offset(x + vL, y - h - hL)
        Anchor.South -> Offset(x - 0.5f * w, y - h - hL)
        Anchor.SouthEast -> Offset(x - w - vL, y - h - hL)
    }
}