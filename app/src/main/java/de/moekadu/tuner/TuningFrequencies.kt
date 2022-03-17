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

interface TuningFrequencies {

    fun getToneIndex(frequency : Float)  : Float
    fun getClosestToneIndex(frequency : Float)  : Int

    fun getNoteFrequency(noteIndex : Int) : Float
    fun getNoteFrequency(noteIndex : Float) : Float

//    fun getNoteName(context: Context, frequency : Float) : CharSequence
//    fun getNoteName(context: Context, toneIndex : Int, preferFlat : Boolean) : CharSequence
}
