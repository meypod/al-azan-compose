package com.github.meypod.al_azan.core.presentation.util

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastSumBy
import kotlin.math.pow

private const val EDGE_MASK_GRADIENT_STEPS = 6

/**
 * Fades the start/end edges of a scroll container using an alpha mask.
 *
 * - Fade is only applied on an edge when there is more content to scroll in that direction.
 * - Fade strength ramps up gradually as you move away from the edge (based on current scroll offset).
 */
fun Modifier.fadeScrollEdges(
    state: ScrollState,
    orientation: Orientation,
    edgeLength: Dp = 45.dp,
    minEdgeAlpha: Float = 0f,
    edgeGradientExponent: Float = 1f,
): Modifier {
    val clampedMinAlpha = minEdgeAlpha.coerceIn(0f, 1f)
    val clampedEdgeExponent = edgeGradientExponent.coerceAtLeast(0.01f)
    return graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }.drawWithContent {
        drawContent()

        val edgePx = edgeLength.toPx()
        val mainAxisSize = if (orientation == Orientation.Horizontal) size.width else size.height
        if (edgePx <= 0f || mainAxisSize <= 0f || state.maxValue <= 0) return@drawWithContent

        val startProgress = (state.value.toFloat() / edgePx).coerceIn(0f, 1f)
        val endProgress = ((state.maxValue - state.value).toFloat() / edgePx).coerceIn(0f, 1f)

        val startAlphaAtEdge = edgeAlphaAtEdge(progress = startProgress, minEdgeAlpha = clampedMinAlpha)
        val endAlphaAtEdge = edgeAlphaAtEdge(progress = endProgress, minEdgeAlpha = clampedMinAlpha)

        val brush = buildEdgeMaskBrush(
            orientation = orientation,
            mainAxisSize = mainAxisSize,
            edgePx = edgePx,
            edgeGradientExponent = clampedEdgeExponent,
            startAlphaAtEdge = if (state.value > 0) startAlphaAtEdge else 1f,
            endAlphaAtEdge = if (state.value < state.maxValue) endAlphaAtEdge else 1f,
        ) ?: return@drawWithContent

        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }
}

fun Modifier.fadeScrollEdges(
    state: LazyListState,
    orientation: Orientation,
    edgeLength: Dp = 45.dp,
    minEdgeAlpha: Float = 0f,
    edgeGradientExponent: Float = 1f,
): Modifier {
    val clampedMinAlpha = minEdgeAlpha.coerceIn(0f, 1f)
    val clampedEdgeExponent = edgeGradientExponent.coerceAtLeast(0.01f)
    return graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }.drawWithContent {
        drawContent()

        val edgePx = edgeLength.toPx()
        val mainAxisSize = if (orientation == Orientation.Horizontal) size.width else size.height
        if (edgePx <= 0f || mainAxisSize <= 0f) return@drawWithContent

        val layoutInfo = state.layoutInfo
        val items = layoutInfo.visibleItemsInfo
        if (layoutInfo.totalItemsCount <= 0 || items.isEmpty()) return@drawWithContent

        val viewportSize = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat().coerceAtLeast(0f)
        val itemsSize = items.fastSumBy { it.size }
        val estimatedItemSize = if (items.isEmpty()) 0f else itemsSize.toFloat() / items.size
        val totalSize = estimatedItemSize * layoutInfo.totalItemsCount
        val maxOffsetPx = (totalSize - viewportSize).coerceAtLeast(0f)
        val currentOffsetPx = (estimatedItemSize * state.firstVisibleItemIndex + state.firstVisibleItemScrollOffset)
            .coerceIn(0f, maxOffsetPx)

        val canScrollBackward = currentOffsetPx > 0f
        val canScrollForward = currentOffsetPx < maxOffsetPx

        val startDistancePx = currentOffsetPx
        val endDistancePx = maxOffsetPx - currentOffsetPx
        val startProgress = (startDistancePx / edgePx).coerceIn(0f, 1f)
        val endProgress = (endDistancePx / edgePx).coerceIn(0f, 1f)

        val brush = buildEdgeMaskBrush(
            orientation = orientation,
            mainAxisSize = mainAxisSize,
            edgePx = edgePx,
            edgeGradientExponent = clampedEdgeExponent,
            startAlphaAtEdge = if (canScrollBackward) edgeAlphaAtEdge(startProgress, clampedMinAlpha) else 1f,
            endAlphaAtEdge = if (canScrollForward) edgeAlphaAtEdge(endProgress, clampedMinAlpha) else 1f,
        ) ?: return@drawWithContent

        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }
}

fun Modifier.fadeVerticalScrollEdges(
    state: LazyGridState,
    edgeLength: Dp = 45.dp,
    minEdgeAlpha: Float = 0f,
    edgeGradientExponent: Float = 1f,
): Modifier {
    val clampedMinAlpha = minEdgeAlpha.coerceIn(0f, 1f)
    val clampedEdgeExponent = edgeGradientExponent.coerceAtLeast(0.01f)
    return graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }.drawWithContent {
        drawContent()

        val edgePx = edgeLength.toPx()
        val mainAxisSize = size.height
        if (edgePx <= 0f || mainAxisSize <= 0f) return@drawWithContent

        val layoutInfo = state.layoutInfo
        val items = layoutInfo.visibleItemsInfo
        if (layoutInfo.totalItemsCount <= 0 || items.isEmpty()) return@drawWithContent

        val canScrollBackward = state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0
        val startDistancePx =
            if (state.firstVisibleItemIndex > 0) edgePx else state.firstVisibleItemScrollOffset.toFloat()
        val startProgress = (startDistancePx / edgePx).coerceIn(0f, 1f)

        val last = items.last()
        val lastItemEnd = last.offset.y + last.size.height
        val canScrollForward =
            last.index < layoutInfo.totalItemsCount - 1 || lastItemEnd > layoutInfo.viewportEndOffset

        val remainingForwardPx = (lastItemEnd - layoutInfo.viewportEndOffset).coerceAtLeast(0).toFloat()
        val endProgress =
            if (last.index < layoutInfo.totalItemsCount - 1) 1f else (remainingForwardPx / edgePx).coerceIn(0f, 1f)

        val brush = buildEdgeMaskBrush(
            orientation = Orientation.Vertical,
            mainAxisSize = mainAxisSize,
            edgePx = edgePx,
            edgeGradientExponent = clampedEdgeExponent,
            startAlphaAtEdge = if (canScrollBackward) edgeAlphaAtEdge(startProgress, clampedMinAlpha) else 1f,
            endAlphaAtEdge = if (canScrollForward) edgeAlphaAtEdge(endProgress, clampedMinAlpha) else 1f,
        ) ?: return@drawWithContent

        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }
}

private fun edgeAlphaAtEdge(
    progress: Float,
    minEdgeAlpha: Float,
): Float {
    val t = progress.coerceIn(0f, 1f)
    return 1f - (1f - minEdgeAlpha) * t
}

private fun buildEdgeMaskBrush(
    orientation: Orientation,
    mainAxisSize: Float,
    edgePx: Float,
    edgeGradientExponent: Float,
    startAlphaAtEdge: Float,
    endAlphaAtEdge: Float,
): Brush? {
    if (startAlphaAtEdge >= 1f && endAlphaAtEdge >= 1f) return null

    val edgeFraction = (edgePx / mainAxisSize).coerceIn(0f, 0.5f)
    val solid = Color.Black

    if (edgeFraction <= 0f) return null

    val steps = EDGE_MASK_GRADIENT_STEPS
    val exp = edgeGradientExponent.coerceAtLeast(0.01f)

    fun lerp(
        a: Float,
        b: Float,
        t: Float,
    ): Float = a + (b - a) * t

    // Build stops in strictly increasing position order to avoid sorting/distinct allocations.
    val stops = ArrayList<Pair<Float, Color>>(steps * 2 + 6)

    val startEdgeAlpha = startAlphaAtEdge.coerceIn(0f, 1f)
    // Start edge: [0 .. edgeFraction]
    for (i in 0 until steps) {
        val t = i.toFloat() / steps
        val curved = t.pow(exp)
        val pos = edgeFraction * t
        val alpha = lerp(startEdgeAlpha, 1f, curved)
        stops.add(pos to solid.copy(alpha = alpha))
    }
    stops.add(edgeFraction to solid.copy(alpha = 1f))

    val endStart = 1f - edgeFraction
    if (endStart > edgeFraction) {
        stops.add(endStart to solid.copy(alpha = 1f))
    }

    // End edge: [endStart .. 1]
    val endEdgeAlpha = endAlphaAtEdge.coerceIn(0f, 1f)
    for (i in 1..steps) {
        val t = i.toFloat() / steps
        // Use distance-from-edge curve (mirror of start edge).
        val curved = (1f - t).pow(exp)
        val pos = endStart + edgeFraction * t
        val alpha = lerp(endEdgeAlpha, 1f, curved)
        stops.add(pos to solid.copy(alpha = alpha))
    }

    val colorStops = stops.toTypedArray()

    return when (orientation) {
        Orientation.Vertical -> Brush.verticalGradient(colorStops = colorStops)
        Orientation.Horizontal -> Brush.horizontalGradient(colorStops = colorStops)
    }
}

@Preview(widthDp = 400, heightDp = 400, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun EdgeFadeVerticalScrollPreview() {
    MaterialTheme {
        val state = rememberScrollState()
        Column(
            modifier = Modifier
                .fadeScrollEdges(state, orientation = Orientation.Vertical, edgeGradientExponent = 0.7f)
                .verticalScroll(state),
        ) {
            repeat(30) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Color.Red),
                )
            }
        }
    }
}

@Preview(widthDp = 400, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun EdgeFadeHorizontalScrollPreview() {
    MaterialTheme {
        val state = rememberScrollState()
        LaunchedEffect(Unit) {
            state.scrollTo(200)
        }
        Row(
            modifier = Modifier
                .fadeScrollEdges(state, orientation = Orientation.Horizontal, edgeGradientExponent = 2.5f)
                .height(96.dp)
                .horizontalScroll(state),
        ) {
            repeat(40) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(96.dp)
                        .background(Color.Red),
                )
            }
        }
    }
}

@Preview(widthDp = 400, heightDp = 400, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun EdgeFadeLazyListPreview() {
    MaterialTheme {
        val state = rememberLazyListState()
        LaunchedEffect(Unit) {
            state.scrollToItem(25)
        }
        LazyColumn(
            modifier = Modifier.fadeScrollEdges(state, orientation = Orientation.Vertical, edgeGradientExponent = 1f),
            state = state,
        ) {
            items(100) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Color.Red),
                )
            }
        }
    }
}
