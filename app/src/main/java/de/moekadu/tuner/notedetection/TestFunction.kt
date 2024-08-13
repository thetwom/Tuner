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
package de.moekadu.tuner.notedetection

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private enum class TestFunctionType {
    Off,
    Constant,
    ConstantAccurate,
    Linear,
    Exponential,
    Random
}

private val testFunctionType = TestFunctionType.Off

val testFunction: ((frame: Int, dt: Float) -> Float)?
    get() {
        return when (testFunctionType) {
            TestFunctionType.Off -> null
            TestFunctionType.Constant -> {
                // constant frequency test function, but suffers from inaccuracies at large times
                { frame: Int, dt: Float ->
                    val freq = 440f
                    sin(frame * dt * 2 * kotlin.math.PI.toFloat() * freq)
                }
            }
            TestFunctionType.ConstantAccurate -> {
                { frame: Int, dt: Float ->
                    val freqApprox = 660f
                    val numSteps = (1 / freqApprox / dt).roundToInt()
                    val freq = 1 / (numSteps * dt)
                    val frameMod = frame - (frame / numSteps) * numSteps
                    sin(frameMod * dt * 2 * kotlin.math.PI.toFloat() * freq)
                }
            }
            TestFunctionType.Linear -> {
                { frame: Int, dt: Float ->
                    val freq = 200f + 2f * frame * dt
                    (sin(frame * dt * 2 * kotlin.math.PI.toFloat() * freq)
                            + 0.3f * sin(2 * frame * dt * 2 * kotlin.math.PI.toFloat() * freq)
                            + 0.5f * sin(3 * frame * dt * 2 * kotlin.math.PI.toFloat() * freq))
                }
            }
            TestFunctionType.Exponential -> {
                { frame: Int, dt: Float ->
                    val freq = 200f * 2f.pow(frame * dt / 24f)
                    (sin(frame * dt * 2 * kotlin.math.PI.toFloat() * freq)
                            + 0.3f * sin(2 * frame * dt * 2 * kotlin.math.PI.toFloat() * freq)
                            + 0.5f * sin(3 * frame * dt * 2 * kotlin.math.PI.toFloat() * freq))
                }
            }
            TestFunctionType.Random -> {
                { frame: Int, t: Float ->
                    800f * Random.nextFloat()
                }
            }
        }
    }

