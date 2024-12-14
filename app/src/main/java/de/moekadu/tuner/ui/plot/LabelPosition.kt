/*
* Copyright 2024 Michael Moessner
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
package de.moekadu.tuner.ui.plot

import androidx.compose.ui.geometry.Offset


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