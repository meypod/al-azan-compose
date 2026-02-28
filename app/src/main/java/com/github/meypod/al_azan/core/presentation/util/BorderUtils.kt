package com.github.meypod.al_azan.core.presentation.util

import android.annotation.SuppressLint
import androidx.compose.foundation.border
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
