package de.moekadu.tuner.ui.plot3

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import de.moekadu.tuner.ui.plot.Transformation

//data class Outline(
//    lineWidth: Dp,
//    lineColor: Color,
//    cornerRadius: Dp
//)
//@Composable
//fun Outline(
//    lineWidth: Dp,
//    lineColor: Color,
//    cornerRadius: Dp,
//    transformation: () -> Transformation
//) {
//    val lineColorResolved = lineColor.takeOrElse { MaterialTheme.colorScheme.onSurface }
//    Spacer(
//        modifier = Modifier
//            .fillMaxSize()
//            .drawBehind {
//                val viewPortScreen = transformation().viewPortScreen
//                val lineWidthPx = lineWidth.toPx()
//                val lineWidthPxHalf = 0.5f * lineWidthPx
//                val topLeft = Offset(
//                    viewPortScreen.left + lineWidthPxHalf,
//                    viewPortScreen.top + lineWidthPxHalf
//                )
//                val bottomRight = Offset(
//                    viewPortScreen.right - lineWidthPxHalf,
//                    viewPortScreen.bottom - lineWidthPxHalf,
//
//                )
//                val size = Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y)
//
//                val cornerRadiusPx = cornerRadius.toPx()
//                drawRoundRect(
//                    color = lineColorResolved,
//                    topLeft = Offset(
//                        viewPortScreen.left + lineWidthPxHalf,
//                        viewPortScreen.top + lineWidthPxHalf
//                    ),
//                    size = size,
//                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
//                    style = Stroke(lineWidthPx)
//                )
//            }
//    )
//}