package com.github.meypod.al_azan.core.presentation.util

import android.annotation.SuppressLint
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@SuppressLint("UnnecessaryComposedModifier") // The rule incorrectly detects as unnecessary
@Stable
fun Modifier.unifiedBorder(width: Dp = OutlinedTextFieldDefaults.UnfocusedBorderThickness) =
    composed {
        val color = MaterialTheme.colorScheme.onSurfaceVariant
        val shape = MaterialTheme.shapes.small
        this.border(width, color, shape)
    }

fun Modifier.bottomBorder(
    color: Color,
    thickness: Dp,
    horizontalPadding: Dp = 0.dp,
) = this.then(
    Modifier.drawWithContent {
        // Step 1: Draw the original content (e.g., text, input field)
        drawContent()

        // Step 2: Calculate pixel values for thickness and padding
        val thicknessPx = thickness.toPx() // Convert Dp to pixels
        val horizontalPaddingPx = horizontalPadding.toPx() // Convert Dp to pixels

        // Step 3: Define the border's position and size
        val borderTopLeft = Offset(
            x = horizontalPaddingPx, // Left inset from the element's start
            y = size.height - thicknessPx, // Bottom edge minus border thickness
        )
        val borderSize = Size(
            width = size.width - 2 * horizontalPaddingPx, // Total width minus left/right padding
            height = thicknessPx, // Border height (thickness)
        )

        // Step 4: Draw the bottom border as a rectangle
        drawRect(
            color = color,
            topLeft = borderTopLeft,
            size = borderSize,
        )
    },
)

private fun DrawScope.shapeBorderPath(shape: Shape, strokeWidth: Float): Path {
    val inset = strokeWidth / 2
    val outline = shape.createOutline(
        size = Size(size.width - strokeWidth, size.height - strokeWidth),
        layoutDirection = layoutDirection,
        density = this,
    )
    return Path().apply {
        addOutline(outline)
        translate(Offset(inset, inset))
    }
}

@Stable
fun Modifier.dashedBorder(
    borderColor: Color,
    shape: Shape,
    strokeWidth: Float = 3f,
    dashWidth: Float = 20f,
    dashGap: Float = 20f,
): Modifier =
    this.drawBehind {
        val path = shapeBorderPath(shape, strokeWidth)
        // Snap dash period so an integer number fits the perimeter exactly,
        // otherwise a ragged stub appears where the outline loop closes.
        val perimeter = android.graphics.PathMeasure(path.asAndroidPath(), true).length
        val period = dashWidth + dashGap
        val ratio = dashWidth / period
        val periods = (perimeter / period).roundToInt().coerceAtLeast(1)
        val snapped = perimeter / periods
        val on = snapped * ratio
        val off = snapped - on
        drawPath(
            path = path,
            color = borderColor,
            style = Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(on, off), 0f),
            ),
        )
    }

@Stable
fun Modifier.solidBorder(
    borderColor: Color,
    shape: Shape,
    strokeWidth: Float = 3f,
): Modifier =
    this.drawBehind {
        drawPath(
            path = shapeBorderPath(shape, strokeWidth),
            color = borderColor,
            style = Stroke(width = strokeWidth),
        )
    }
