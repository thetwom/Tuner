package de.moekadu.tuner.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.unit.IntRect
import de.moekadu.tuner.ui.plot.Transformation

import org.junit.Assert.assertEquals
import org.junit.Test

class TransformationTest {

    @Test
    fun matrixTest() {
        val mat = Matrix()
        mat.translate(1f)
        val point = Offset(10f, 20f)
        val pointTransformed = mat.map(point)
        assertEquals(point.x + 1f, pointTransformed.x, 1e-12f)

        mat.translate(1f)
        val pointTransformed2 = mat.map(point)
        assertEquals(point.x + 2f, pointTransformed2.x, 1e-12f)

        val mat2 = Matrix()
        mat2.scale(10f)
        mat2.translate(2f)
        val pointTransformed3 = mat2.map(point)
        assertEquals(10f * (point.x + 2f), pointTransformed3.x, 1e-12f)
    }

    @Test
    fun transformTest() {
        val raw = Rect(1f, 20f, 10f, 2f)
        val screen = IntRect(100, 150, 130, 200)
        val transformation = Transformation(screen, raw)

        val topLeftScreenFromTransformed = transformation.toScreen(raw.topLeft)
        assertEquals(screen.topLeft.x.toFloat(), topLeftScreenFromTransformed.x, 1e-12f)
        assertEquals(screen.topLeft.y.toFloat(), topLeftScreenFromTransformed.y, 1e-12f)

        val bottomLeftScreenFromTransformed = transformation.toScreen(raw.bottomLeft)
        assertEquals(screen.bottomLeft.x.toFloat(), bottomLeftScreenFromTransformed.x, 1e-12f)
        assertEquals(screen.bottomLeft.y.toFloat(), bottomLeftScreenFromTransformed.y, 1e-12f)

        val topRightScreenFromTransformed = transformation.toScreen(raw.topRight)
        assertEquals(screen.topRight.x.toFloat(), topRightScreenFromTransformed.x, 1e-12f)
        assertEquals(screen.topRight.y.toFloat(), topRightScreenFromTransformed.y, 1e-12f)

        val bottomRightScreenFromTransformed = transformation.toScreen(raw.bottomRight)
        assertEquals(screen.bottomRight.x.toFloat(), bottomRightScreenFromTransformed.x, 1e-12f)
        assertEquals(screen.bottomRight.y.toFloat(), bottomRightScreenFromTransformed.y, 1e-12f)
    }
}