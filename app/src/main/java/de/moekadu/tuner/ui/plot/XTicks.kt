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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun rememberTextLabelWidth(
    testString: String,
    style: TextStyle = LocalTextStyle.current,
    density: Density = LocalDensity.current,
    paddingLeft: Dp = 0.dp,
    paddingRight: Dp = 0.dp,
    textMeasurer: TextMeasurer = rememberTextMeasurer()
): Float {
    return remember(textMeasurer, density, paddingLeft, paddingRight, style) {
        with(density) {
            (textMeasurer.measure(testString, style = style, density = density).size.width
                    + paddingLeft.toPx()
                    + paddingRight.toPx())
        }
    }
}

private fun computeRange(
    tickLevel: TickLevel,
    maxLabelWidth: Float,
    lineWidth: Dp,
    screenOffset: DpOffset,
    maxNumLabels: Int,
    transformation: Transformation,
    density: Density
): TicksRange {
    val screenOffsetPx = with(density) { screenOffset.x.toPx() }
    val lineWidthPx = with(density) { lineWidth.toPx() }
    val maxNumLabelsResolved = if (maxNumLabels <= 0)
        (transformation.viewPortScreen.width / maxLabelWidth / 1.001f).roundToInt()
    else
       maxNumLabels

    val labelWidthScreen = Rect(
        0f,
        0f,
        maxLabelWidth + 0.5f * lineWidthPx + screenOffsetPx.absoluteValue,
        1f
    )

    val labelWidthRaw = transformation.toRaw(labelWidthScreen).width

    val range = tickLevel.getTicksRange(
        transformation.viewPortRaw.left - labelWidthRaw,
        transformation.viewPortRaw.right + labelWidthRaw,
        maxNumLabelsResolved,
        labelWidthRaw
    )
    //myLog("rememberRange: $labelHeightScreen, raw=${transformation.viewPortRaw}, screen=${transformation.viewPortScreen}, range=$range")
    return range
}

private data class XTickLayoutData(val position: Float):
    ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@XTickLayoutData
}

private data class MeasuredXTick(
    val position: XTickLayoutData,
    val placeable: Placeable
)

@Composable
private fun XTickLabels(
    label: (@Composable (modifier: Modifier, level: Int, index: Int, x: Float) -> Unit)?,
    tickLevel: TickLevel,
    maxLabelWidth: Float,
    anchor: Anchor,
    verticalLabelPosition: Float,
    lineWidth: Dp,
    screenOffset: DpOffset,
    maxNumLabels: Int,
    clipLabelToPlotWindow: Boolean,
    transformation: () -> Transformation
) {
    Layout(
        content = {
            val density = LocalDensity.current
            val range by remember(tickLevel, maxLabelWidth, lineWidth, screenOffset, maxNumLabels, density, transformation) {
                derivedStateOf {
                    computeRange(
                        tickLevel,
                        maxLabelWidth,
                        lineWidth,
                        screenOffset,
                        maxNumLabels,
                        transformation(),
                        density
                    )
                }
            }
            label?.let { l ->
                for (i in range.indexBegin until range.indexEnd) {
                    val y = tickLevel.getTickValue(range.level, i)
                    //          modifier       , level      , index, y
                    key(i){ l(XTickLayoutData(y), range.level, i,     y) }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { measureables, constraints ->
        val c = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measureables.map {
            MeasuredXTick(
                it.parentData as XTickLayoutData,
                it.measure(c)
            )
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            val transform = transformation()
            placeables.forEach {
                val p = it.placeable
                val xOffset = Offset(it.position.position, 0f)
                val xTransformed = transform.toScreen(xOffset).x
                val vp = transform.viewPortScreen
                val visible = xTransformed in vp.left.toFloat() .. vp.right.toFloat()
                if (clipLabelToPlotWindow || visible) {
                    p.place(
                        anchor.place(
                            xTransformed + screenOffset.x.toPx(),
                            vp.top + (1f - verticalLabelPosition) * vp.height + screenOffset.y.toPx(),
                            p.width.toFloat(),
                            p.height.toFloat(),
                            0f,
                            lineWidth.toPx()
                        ).round()
                    )
                }
            }
        }
    }
}

@Composable
private fun XTicksLines(
    tickLevel: TickLevel,
    maxLabelWidth: Float,
    lineWidth: Dp,
    lineColor: Color,
    screenOffset: DpOffset,
    maxNumLabels: Int,
    transformation: () -> Transformation
) {
    val lineColorResolved = lineColor.takeOrElse {
        LocalContentColor.current.takeOrElse {
            MaterialTheme.colorScheme.outline
        }
    }
    val density = LocalDensity.current
    val range by remember(tickLevel, maxLabelWidth, lineWidth, screenOffset, maxNumLabels, density, transformation) {
        derivedStateOf {
            computeRange(
                tickLevel,
                maxLabelWidth,
                lineWidth,
                screenOffset,
                maxNumLabels,
                transformation(),
                density
            )
        }
    }

    Spacer(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val transformationInstance = transformation()
                for (i in range.indexBegin until range.indexEnd) {
                    val xOffset = Offset(tickLevel.getTickValue(range.level, i), 0f)
                    val xTransformed = transformationInstance.toScreen(xOffset).x
                    drawLine(
                        lineColorResolved,
                        Offset(
                            xTransformed,
                            transformationInstance.viewPortScreen.top.toFloat(),

                        ),
                        Offset(
                            xTransformed,
                            transformationInstance.viewPortScreen.bottom.toFloat(),
                        ),
                        strokeWidth = lineWidth.toPx()
                    )
                }
            }
    )
}

@Composable
fun XTicks(
    label: (@Composable (modifier: Modifier, level: Int, index: Int, x: Float) -> Unit)?,
    tickLevel: TickLevel,
    maxLabelWidth: Float,
    anchor: Anchor = Anchor.Center,
    verticalLabelPosition: Float = 0.5f,
    lineWidth: Dp = 1.dp,
    lineColor: Color = Color.Unspecified,
    screenOffset: DpOffset = DpOffset.Zero,
    maxNumLabels: Int = -1,
    clipLabelToPlotWindow: Boolean = true,
    transformation: () -> Transformation,
    clipped: Boolean
) {
    if (clipped) {
        XTicksLines(
            tickLevel,
            maxLabelWidth,
            lineWidth,
            lineColor,
            screenOffset,
            maxNumLabels,
            transformation
        )
    }

    if (clipped == clipLabelToPlotWindow) {
        XTickLabels(
            label = label,
            tickLevel = tickLevel,
            maxLabelWidth = maxLabelWidth,
            anchor = anchor,
            verticalLabelPosition = verticalLabelPosition,
            lineWidth = lineWidth,
            screenOffset = screenOffset,
            maxNumLabels = maxNumLabels,
            clipLabelToPlotWindow = clipLabelToPlotWindow,
            transformation = transformation
        )
    }
}
