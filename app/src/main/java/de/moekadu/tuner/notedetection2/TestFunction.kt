package de.moekadu.tuner.notedetection2

import kotlin.math.sin

// no test function
//    private val testFunction = null
//    // test function, which avoids excessive large times
//    private val testFunction = { frame: Int, dt: Float ->
//        val freqApprox = 660f
//        val numSteps = (1 / freqApprox / dt).roundToInt()
//        val freq = 1 / (numSteps * dt)
//        val frameMod = frame - (frame / numSteps) * numSteps
//        sin(frameMod * dt * 2 * kotlin.math.PI.toFloat() * freq)
//    }
//
//    // constant frequency test function, but suffers from inaccuracies at large times
//    private val testFunction = { frame: Int, dt: Float ->
//        val freq = 660f
//        sin(frame * dt * 2 * kotlin.math.PI.toFloat() * freq)
//    }

// test function with increasing frequency
val testFunction = { frame: Int, dt: Float ->
    val freq = 200f + 2f * frame * dt
    (sin(frame * dt * 2 * kotlin.math.PI.toFloat() * freq)
            + 0.3f * sin(2 * frame * dt * 2 * kotlin.math.PI.toFloat() * freq)
            + 0.5f * sin(3 * frame * dt * 2 * kotlin.math.PI.toFloat() * freq))
}

//    private val testFunction = { t: Float ->
//        val freq = 440f
//        sin(t * 2 * kotlin.math.PI.toFloat() * freq)
//        //800f * Random.nextFloat()
//        // 1f
//    }
