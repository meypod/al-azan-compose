package com.github.meypod.al_azan.core.presentation.util

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Detects a horizontal fling and maps it to logical next/prev navigation.
 *
 * The decision happens on release from the gesture's velocity, mirroring how a scroll fling works:
 * a quick flick triggers even if short, a slow drag does not. A small [minDistance] guards against
 * jittery taps. No content animation — this only fires the navigation callbacks.
 *
 * Direction is layout-aware: in LTR a leftward fling advances ([onNext]); in RTL it goes back
 * ([onPrev]), keeping the gesture consistent with the reading direction of dated content.
 */
fun Modifier.swipeNavigate(
    onNext: () -> Unit,
    onPrev: () -> Unit,
    minVelocity: Dp = 320.dp,
    minDistance: Dp = 32.dp,
): Modifier =
    composed {
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        pointerInput(onNext, onPrev, isRtl) {
            val minVelocityPx = minVelocity.toPx()
            val minDistancePx = minDistance.toPx()
            var total = 0f
            val tracker = VelocityTracker()
            detectHorizontalDragGestures(
                onDragStart = {
                    total = 0f
                    tracker.resetTracking()
                },
                onDragEnd = {
                    val velocity = tracker.calculateVelocity().x
                    if (abs(velocity) < minVelocityPx || abs(total) < minDistancePx) return@detectHorizontalDragGestures
                    // Content flung leftward (negative velocity) advances in LTR; RTL flips it.
                    val forward = if (isRtl) velocity > 0 else velocity < 0
                    if (forward) onNext() else onPrev()
                },
                onHorizontalDrag = { change, dragAmount ->
                    total += dragAmount
                    tracker.addPosition(change.uptimeMillis, change.position)
                },
            )
        }
    }
