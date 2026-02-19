package com.github.meypod.al_azan.core.presentation.util

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale

fun Modifier.patternedBackground(
    pattern: ImageBitmap,
    backgroundColor: Color,
    patternAlpha: Float = 0.03f,
): Modifier =
    drawWithCache {
        val brush = ShaderBrush(ImageShader(pattern, TileMode.Repeated, TileMode.Repeated))
        onDrawBehind {
            drawRect(color = backgroundColor)
            drawRect(brush = brush, alpha = patternAlpha)
        }
    }

@Composable
fun rememberPatternImageBitmap(
    @DrawableRes patternResId: Int,
    compactSize: Dp = 140.dp,
    expandedSize: Dp = 150.dp,
    expandedWidthBreakpoint: Dp = 600.dp,
): ImageBitmap {
    val resources = LocalResources.current
    val containerSize = LocalWindowInfo.current.containerDpSize
    val density = LocalDensity.current

    return remember(resources, containerSize, density, patternResId, compactSize, expandedSize, expandedWidthBreakpoint) {
        val original = BitmapFactory.decodeResource(resources, patternResId)
        val sizeDp = if (containerSize.width >= expandedWidthBreakpoint) expandedSize else compactSize
        val sizePx = with(density) { sizeDp.roundToPx().coerceAtLeast(1) }
        original.scale(sizePx, sizePx).asImageBitmap()
    }
}
