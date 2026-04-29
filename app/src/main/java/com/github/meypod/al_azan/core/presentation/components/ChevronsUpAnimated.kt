package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePaddedPreview
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

/**
 * Stack of up-chevrons lit by a continuous "light" that sweeps bottom → top.
 *
 * A single float position animates linearly from below the stack (count + radius)
 * to above it (-radius). Each chevron's alpha is computed from its distance to
 * that position with a triangular falloff over [lightRadius] chevrons, so frames
 * look like a moving glow rather than a binary swap.
 *
 * At the start and end of each cycle the glow is outside the stack, leaving all
 * chevrons at [baselineAlpha]. [cycleDelayMs] pauses before repeating.
 */
@Composable
fun ChevronsUpAnimated(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    count: Int = 4,
    sweepDurationMs: Int = 1500,
    cycleDelayMs: Long = 1000L,
    baselineAlpha: Float = 0.25f,
    solidAlpha: Float = 1f,
    lightRadius: Float = 1.2f,
    chevronHeight: Dp = 20.dp,
    spacing: Dp = (-4).dp,
) {
    val startPos = count + lightRadius
    val endPos = -lightRadius
    val position = remember { Animatable(startPos) }

    LaunchedEffect(count, sweepDurationMs, cycleDelayMs, lightRadius) {
        while (true) {
            position.snapTo(startPos)
            position.animateTo(
                targetValue = endPos,
                animationSpec = tween(durationMillis = sweepDurationMs, easing = LinearEasing),
            )
            delay(cycleDelayMs)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        for (i in 0 until count) {
            val distance = (i.toFloat() - position.value).absoluteValue
            val intensity = (1f - distance / lightRadius).coerceAtLeast(0f)
            val alpha = baselineAlpha + (solidAlpha - baselineAlpha) * intensity
            Image(
                painter = painterResource(R.drawable.chevron_up_single),
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint.copy(alpha = alpha)),
                modifier = Modifier.requiredHeight(chevronHeight),
            )
        }
    }
}

@Preview
@Composable
private fun ChevronsUpAnimatedPreview() {
    AlAzanThemePaddedPreview(
        Modifier
            .background(Color(0xFF00585A)),
    ) {
        ChevronsUpAnimated()
    }
}
