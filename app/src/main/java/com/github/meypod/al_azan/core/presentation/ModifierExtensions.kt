package com.github.meypod.al_azan.core.presentation

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

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

fun Modifier.drawCurvedTopPatternedBackground(
    pattern: ImageBitmap,
    backgroundColor: Color,
    patternAlpha: Float = 0.03f,
    curve: Float = 0.16f,
    elevation: Dp = 10.dp,
): Modifier =
    dropShadow(
        shape = CurvedTopShape(curve = curve),
        shadow =
            Shadow(
                radius = elevation,
                spread = 0.dp,
                color = Color.Black.copy(alpha = 0.1f),
                offset = DpOffset(x = 0.dp, y = 0.dp),
            ),
    ).drawWithCache {
        onDrawBehind {
            val brush = ShaderBrush(ImageShader(pattern, TileMode.Repeated, TileMode.Repeated))
            val path = createCurvedTopPath(size = size, curve = curve)
            drawPath(path = path, color = backgroundColor)
            drawPath(path = path, brush = brush, alpha = patternAlpha)
        }
    }

private fun createCurvedTopPath(
    size: Size,
    curve: Float,
): Path {
    val curveY = size.height * curve
    return Path().apply {
        moveTo(0f, curveY)
        quadraticTo(size.width / 2f, -curveY * 0.9f, size.width, curveY)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
}

private data class CurvedTopShape(
    val curve: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(path = createCurvedTopPath(size = size, curve = curve))
}
