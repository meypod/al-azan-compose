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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

@Stable
fun Modifier.dashedBorder(
    borderColor: Color,
    strokeWidth: Float = 2f,
    dashWidth: Float = 20f,
    dashGap: Float = 20f,
): Modifier =
    this.drawBehind {
        val paint = android.graphics
            .Paint()
            .apply {
                color = borderColor.toArgb()
                style = android.graphics.Paint.Style.STROKE
                pathEffect = android.graphics.DashPathEffect(
                    floatArrayOf(dashWidth, dashGap),
                    0f,
                )
                this.strokeWidth = strokeWidth
            }
        val borderRect = android.graphics.RectF(
            0f + strokeWidth / 2,
            0f + strokeWidth / 2,
            size.width - strokeWidth / 2,
            size.height - strokeWidth / 2,
        )
        drawContext.canvas.nativeCanvas.drawRoundRect(
            borderRect,
            30f,
            30f,
            paint,
        )
    }
