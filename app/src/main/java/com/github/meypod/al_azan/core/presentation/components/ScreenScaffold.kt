package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.util.drawVerticalScrollbar
import com.github.meypod.al_azan.core.presentation.util.fadeScrollEdges

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleIcon: Int? = null,
    navigationIcon: @Composable () -> Unit = { BackButton(onBackClick) },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    snackbarHost: @Composable () -> Unit = { AppSnackbarHost(LocalSnackbarController.current.hostState) },
    scrollable: Boolean = true,
    contentPadding: PaddingValues? = null,
    verticalArrangement: Arrangement.Vertical? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedPadding = contentPadding ?: PaddingValues(dimensionResource(R.dimen.page_padding))
    val resolvedArrangement =
        verticalArrangement ?: Arrangement.spacedBy(dimensionResource(R.dimen.element_padding))
    val scrollState = if (scrollable) rememberScrollState() else null
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
                        Text(title)
                    }
                },
            )
        },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        bottomBar = bottomBar,
    ) { paddingValues ->
        val inner = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .then(
                if (scrollState != null) {
                    Modifier
                        .fadeScrollEdges(scrollState, Orientation.Vertical)
                        .drawVerticalScrollbar(scrollState)
                        .verticalScroll(scrollState)
                } else {
                    Modifier
                },
            )
            .padding(resolvedPadding)
        Column(
            modifier = inner,
            verticalArrangement = resolvedArrangement,
            content = content,
        )
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
