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
package de.moekadu.tuner.temperaments

val rationalNumberTemperamentWerckmeisterVI = arrayOf(
    RationalNumber(1, 1), // C
    RationalNumber(196, 186), // C#
    RationalNumber(196, 175), // D
    RationalNumber(196, 165), // Eb
    RationalNumber(196, 156), // E
    RationalNumber(196, 147), // F
    RationalNumber(196, 139), // F#
    RationalNumber(196, 131), // G
    RationalNumber(196, 124), // G#
    RationalNumber(196, 117), // A
    RationalNumber(196, 110), // Bb
    RationalNumber(196, 104), // B
    RationalNumber(2, 1), // C2
)

val rationalNumberTemperamentPure = arrayOf(
    RationalNumber(1, 1), // C
    RationalNumber(16, 15), // C#
    RationalNumber(9, 8), // D
    RationalNumber(6, 5), // Eb
    RationalNumber(5, 4), // E
    RationalNumber(4, 3), // F
    RationalNumber(45, 32), // F#
    RationalNumber(3, 2), // G
    RationalNumber(8, 5), // G#
    RationalNumber(5, 3), // A
    RationalNumber(9, 5), // Bb (sometimes 16.0/9.0)
    RationalNumber(15, 8), // B
    RationalNumber(2, 1) // C2
)

val rationalNumberTemperamentTest = arrayOf(
    RationalNumber(20, 20), // C
    RationalNumber(21, 20), // C#
    RationalNumber(22, 20), // D
    RationalNumber(23, 20), // Eb
    RationalNumber(24, 20), // E
    RationalNumber(25, 20), // F
    RationalNumber(26, 20), // F#
    RationalNumber(27, 20), // G
    RationalNumber(28, 20), // G#
    RationalNumber(29, 20), // A
    RationalNumber(30, 20), // Bb (sometimes 16.0/9.0)
    RationalNumber(31, 20), // B
    RationalNumber(40, 20) // C2
)
