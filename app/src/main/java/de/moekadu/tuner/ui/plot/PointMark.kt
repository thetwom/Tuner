package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlin.math.roundToInt

// TODO: Add an offset
// TODO: Use ParentDataModifier also for Horizontal/Vertical marks
class PointMark(
    initialPosition: Offset,
    initialAnchor: Anchor,
    initialScreenOffset: DpOffset = DpOffset.Zero,
    initialContent: @Composable (modifier: Modifier) -> Unit,
) {
    data class LayoutData(val position: Offset, val screenOffset: DpOffset, val anchor: Anchor): ParentDataModifier {
        override fun Density.modifyParentData(parentData: Any?) = this@LayoutData
    }
    var position by mutableStateOf(LayoutData(initialPosition, initialScreenOffset, initialAnchor))
        private set
    var content by mutableStateOf(initialContent)
        private set
    var anchor by mutableStateOf(initialAnchor)
        private set

    fun setPointMark(
        position: Offset? = null,
        anchor: Anchor? = null,
        screenOffset: DpOffset? = null,
        content: (@Composable (modifier: Modifier) -> Unit)? = null,
    ) {
        if (position != null || anchor != null || screenOffset != null) {
            this.position = LayoutData(
                position ?: this.position.position,
                screenOffset ?: this.position.screenOffset,
                anchor ?: this.position.anchor
            )
        }

        if (content != null)
            this.content = content
    }
}

private data class MeasuredPointMark(val position: PointMark.LayoutData, val placeable: Placeable)
class PointMarkGroup : PlotGroup {
    private val pointMarks = mutableMapOf<Int, PointMark>()

    fun setPointMark(
        key: Int = 0,
        position: Offset? = null,
        anchor: Anchor? = null,
        screenOffset: DpOffset? = null,
        content: (@Composable (modifier: Modifier) -> Unit)? = null,
    ) {
        pointMarks[key]?.setPointMark(position, anchor, screenOffset, content)
        if (key !in pointMarks) {
            pointMarks[key] = PointMark(
                position ?: Offset.Zero,
                anchor ?: Anchor.Center,
                screenOffset ?: DpOffset.Zero,
                content ?: { Text("x") }
            )
        }
    }

    @Composable
    override fun Draw(transformation: Transformation) {
        Layout(
            modifier = Modifier
                .fillMaxSize()
                .clip(transformation.rememberClipShape()),
            content = {
                pointMarks.forEach { it.value.content(it.value.position) }
            }
        ) {measureables, constraints ->
            val c = constraints.copy(minWidth = 0, minHeight = 0)
            val placeables = measureables.map {
                MeasuredPointMark(it.parentData as PointMark.LayoutData, it.measure(c))
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEach {
                    val p = it.placeable
                    val positionScreen = transformation.toScreen(it.position.position)
                    val x = positionScreen.x + it.position.screenOffset.x.toPx()
                    val y = positionScreen.y + it.position.screenOffset.y.toPx()
                    val w = p.width
                    val h = p.height
                    val anchor = it.position.anchor

                    when (anchor) {
                        Anchor.NorthWest -> p.place(x.roundToInt(), y.roundToInt())
                        Anchor.North -> p.place((x - 0.5f * w).roundToInt(), y.roundToInt())
                        Anchor.NorthEast -> p.place((x - w).roundToInt(), y.roundToInt())
                        Anchor.West -> p.place(x.roundToInt(), (y - 0.5f * h).roundToInt())
                        Anchor.Center -> p.place((x - 0.5f * w).roundToInt(), (y - 0.5f * h).roundToInt())
                        Anchor.East -> p.place((x - w).roundToInt(), (y - 0.5f * h).roundToInt())
                        Anchor.SouthWest -> p.place(x.roundToInt(), (y - h).roundToInt())
                        Anchor.South -> p.place((x - 0.5f * w).roundToInt(), (y - h).roundToInt())
                        Anchor.SouthEast -> p.place((x - w).roundToInt(), (y - h).roundToInt())
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberTransformation(
    screenWidth: Dp, screenHeight: Dp,
    viewPortRaw: Rect
): Transformation {
    val widthPx = with(LocalDensity.current) { screenWidth.roundToPx() }
    val heightPx = with(LocalDensity.current) { screenHeight.roundToPx() }

    val transformation = remember(widthPx, heightPx, viewPortRaw) {
        Transformation(IntRect(0, 0, widthPx, heightPx), viewPortRaw)
    }
    return transformation
}

@Preview(widthDp = 200, heightDp = 100, showBackground = true)
@Composable
private fun PointMarkGroupPreview() {
    TunerTheme {
        BoxWithConstraints {
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-10f, 5f, 10f, -5f)
            )

            val pointMarkGroup = remember {
                PointMarkGroup().also {
                    it.setPointMark(
                        key = 0,
                        position = Offset(-5f, -2f),
                        anchor = Anchor.NorthEast,
                        content = { modifier -> Text("-5,-2NE", modifier = modifier.background(Color.Cyan))}
                    )
                    it.setPointMark(
                        key = 1,
                        position = Offset(-5f, -2f),
                        anchor = Anchor.SouthWest,
                        content = { modifier -> Text("-5,-2SW", modifier = modifier.background(Color.Magenta))}
                    )
                    it.setPointMark(
                        key = 2,
                        position = Offset(-5f, -2f),
                        anchor = Anchor.SouthEast,
                        content = { modifier -> Text("-5,-2SE", modifier = modifier.background(Color.Green))}
                    )
                    it.setPointMark(
                        key = 3,
                        position = Offset(-5f, -2f),
                        anchor = Anchor.NorthWest,
                        content = { modifier -> Text("-5,-2NW", modifier = modifier.background(Color.Yellow))}
                    )

                    it.setPointMark(
                        key = 10,
                        position = Offset(5f, 2f),
                        anchor = Anchor.North,
                        content = { modifier -> Text("5,2N", modifier = modifier.background(Color.Cyan))}
                    )
                    it.setPointMark(
                        key = 11,
                        position = Offset(5f, 2f),
                        anchor = Anchor.South,
                        content = { modifier -> Text("5,2S", modifier = modifier.background(Color.Green))}
                    )

                }
            }
            pointMarkGroup.Draw(transformation = transformation)
        }
    }
}