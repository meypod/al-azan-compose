package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.util.drawVerticalScrollbar
import com.github.meypod.al_azan.core.presentation.util.fadeScrollEdges

/** Window-space top/bottom (px) of the scrolling content viewport, below the app bar / above the bottom bar. */
data class PageScrollViewportBounds(
    val topPx: Float,
    val bottomPx: Float,
)

/** Bounds of the nearest [ScreenScaffold] scroll viewport, or null when the host page isn't scrollable. */
val LocalPageScrollViewportBounds = compositionLocalOf<PageScrollViewportBounds?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleIcon: Int? = null,
    navigationIcon: @Composable () -> Unit = { BackButton(onBackClick) },
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    snackbarHost: @Composable () -> Unit = { AppSnackbarHost(LocalSnackbarController.current.hostState) },
    scrollable: Boolean = true,
    scrollState: ScrollState? = null,
    contentPadding: PaddingValues? = null,
    verticalArrangement: Arrangement.Vertical? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedPadding = contentPadding ?: PaddingValues(dimensionResource(R.dimen.page_padding))
    val resolvedArrangement =
        verticalArrangement ?: Arrangement.spacedBy(dimensionResource(R.dimen.element_padding))
    val fallbackScrollState = rememberScrollState()
    val resolvedScrollState = if (scrollable) (scrollState ?: fallbackScrollState) else null
    Scaffold(
        modifier = modifier,
        snackbarHost = snackbarHost,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = navigationIcon,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.icon_padding)),
                    ) {
                        titleIcon?.let {
                            Icon(painterResource(it), contentDescription = null)
                        }
                        Text(title, modifier = Modifier.semantics { heading() })
                    }
                },
                actions = actions,
            )
        },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        bottomBar = bottomBar,
    ) { paddingValues ->
        var viewportBounds by remember { mutableStateOf<PageScrollViewportBounds?>(null) }
        val inner = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .then(
                if (resolvedScrollState != null) {
                    Modifier
                        // Capture the scroll viewport (post app-bar inset, pre content padding) so
                        // descendants detect the real content edges, not the raw window edges. Only
                        // scrollable screens need this; the node is fixed so it doesn't fire on scroll.
                        .onGloballyPositioned { coords ->
                            val top = coords.localToWindow(Offset.Zero).y
                            viewportBounds = PageScrollViewportBounds(top, top + coords.size.height)
                        }
                        .fadeScrollEdges(resolvedScrollState, Orientation.Vertical)
                        .drawVerticalScrollbar(resolvedScrollState)
                        .verticalScroll(resolvedScrollState)
                } else {
                    Modifier
                },
            )
            .padding(resolvedPadding)
        CompositionLocalProvider(
            LocalPageScrollViewportBounds provides if (resolvedScrollState != null) viewportBounds else null,
        ) {
            Column(
                modifier = inner,
                verticalArrangement = resolvedArrangement,
                content = content,
            )
        }
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(R.drawable.arrow_back),
            contentDescription = stringResource(R.string.back_button),
        )
    }
}

@Preview
@Composable
private fun ScreenScaffoldPreview() {
    AlAzanThemePreview {
        ScreenScaffold(
            title = "Settings",
            onBackClick = {},
            titleIcon = R.drawable.settings_filled,
        ) {
            Text("Body content")
            Text("Another row")
        }
    }
}
