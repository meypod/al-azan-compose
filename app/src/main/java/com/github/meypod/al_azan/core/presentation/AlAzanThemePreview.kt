package com.github.meypod.al_azan.core.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.github.meypod.al_azan.R

@Composable
fun AlAzanThemePreview(content: @Composable () -> Unit) {
    AlAzanTheme {
        Surface(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            content()
        }
    }
}

@Composable
fun AlAzanThemePaddedPreview(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlAzanTheme {
        Surface {
            Column(
                modifier = modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(dimensionResource(R.dimen.page_padding)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
                content = content,
            )
        }
    }
}
