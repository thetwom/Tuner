package de.moekadu.tuner.ui.plot

//import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.unit.IntRect

data class Transformation(
    val viewPortScreen: IntRect,
    val viewPortRaw: Rect,
    val viewPortCornerRadius: Float = 0f
) {
    val matrixRawToScreen = Matrix().apply {
        translate(viewPortScreen.left.toFloat(), viewPortScreen.center.y.toFloat())
        scale(viewPortScreen.width / viewPortRaw.width, viewPortScreen.height / viewPortRaw.height)
        translate(-viewPortRaw.left, -viewPortRaw.center.y)
//        Log.v("Tuner", "Transformation: create, from ${viewPortRaw} to $viewPortScreen")
//        Log.v("Tuner", "Transformation: create, translate: ${-viewPortRaw.left}, ${-viewPortRaw.center.y}")
//
//        Log.v("Tuner", "Transformation: create, scale: ${viewPortScreen.width / viewPortRaw.width}, ${viewPortScreen.height / viewPortRaw.height}")
//
//        Log.v("Tuner", "Transformation: create, translate: ${viewPortScreen.left}, ${viewPortScreen.center.y}")
    }
//    private val matrixRawToScreen = Matrix().apply {
//        setRectToRect(viewPortRaw, viewPortScreenFloat, Matrix.ScaleToFit.FILL)
//        postScale(1f, -1f, 0f, viewPortScreenFloat.centerY())
//    }
    val matrixScreenToRaw = Matrix().apply {
        setFrom(matrixRawToScreen)
        invert()
    }

    fun toScreen(rect: Rect) = matrixRawToScreen.map(rect)
    fun toScreen(point: Offset) = matrixRawToScreen.map(point)

    fun toRaw(rect: Rect) = matrixScreenToRaw.map(rect)
    fun toRaw(point: Offset) = matrixScreenToRaw.map(point)

}