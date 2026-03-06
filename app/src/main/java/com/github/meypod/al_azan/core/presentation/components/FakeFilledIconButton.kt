package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Stable
private fun IconButtonColors.containerColor(enabled: Boolean): Color = if (enabled) containerColor else disabledContainerColor

@Stable
private fun IconButtonColors.contentColor(enabled: Boolean): Color = if (enabled) contentColor else disabledContentColor

@Composable
fun FakeFilledIconButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = colors.containerColor(enabled),
        contentColor = colors.contentColor(enabled),
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            val contentColor = colors.contentColor(enabled)
            CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
        }
    }
}
