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

package de.moekadu.tuner.notedetection

import androidx.annotation.StringRes
import de.moekadu.tuner.R
import kotlin.math.PI
import kotlin.math.cos

enum class WindowingFunction(@StringRes val stringResourceId: Int) {
    Tophat(R.string.no_window),
    Hamming(R.string.window_hamming),
    Hann(R.string.window_hann)
}

fun getWindow(window: WindowingFunction, size: Int) = FloatArray(size) { i ->
    when (window) {
        WindowingFunction.Tophat ->
            1.0f
        WindowingFunction.Hamming ->
            0.54f - 0.46f * cos(2.0f * PI.toFloat() * i.toFloat() / size.toFloat())
        WindowingFunction.Hann ->
            0.5f * (1.0f - cos(2.0f * PI.toFloat() * i.toFloat() / size.toFloat()))

    }
}

fun getWindowIntegral(window: WindowingFunction) =
     when(window) {
         WindowingFunction.Tophat -> 1.0f
         WindowingFunction.Hamming -> 0.54f
         WindowingFunction.Hann -> 0.5f
     }