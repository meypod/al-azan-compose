package com.github.meypod.al_azan.main.home

import android.icu.text.DateFormat
import android.icu.util.ULocale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.time.Instant
import kotlin.time.toJavaInstant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAction: (HomeUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerState) {
                Text(stringResource(R.string.app_name), modifier = Modifier.padding(dimensionResource(R.dimen.page_padding)))
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = {
                        Icon(painterResource(R.drawable.alarm), contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.reminder)) },
                    selected = false,
                    onClick = {
                        onAction(HomeUiAction.OnReminderLinkClick)
                    },
                )
                NavigationDrawerItem(
                    icon = {
                        Icon(painterResource(R.drawable.compass_outline), contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.qibla)) },
                    selected = false,
                    onClick = {
                        onAction(HomeUiAction.OnQiblaLinkClick)
                    },
                )
                NavigationDrawerItem(
                    icon = {
                        Icon(painterResource(R.drawable.counter), contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.counter)) },
                    selected = false,
                    onClick = {
                        onAction(HomeUiAction.OnCounterLinkClick)
                    },
                )
                NavigationDrawerItem(
                    icon = {
                        Icon(painterResource(R.drawable.settings), contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.settings)) },
                    selected = false,
                    onClick = {
                        onAction(HomeUiAction.OnSettingsLinkClick)
                    },
                )
                NavigationDrawerItem(
                    icon = {
                        Icon(painterResource(R.drawable.info_variant_outline), contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.about_us)) },
                    selected = false,
                    onClick = {
                        onAction(HomeUiAction.OnAboutUsLinkClick)
                    },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(painterResource(R.drawable.menu), contentDescription = stringResource(R.string.menu))
                        }
                    },
                    title = {
                        Text(formatInstant(uiState.currentInstant, uiState.locale, uiState.calendar))
                    },
                )
            },
        ) { paddingValues ->
            Column(
                modifier
                    .padding(paddingValues)
                    .padding(dimensionResource(R.dimen.page_padding)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
            }
        }
    }
}

private fun formatInstant(
    instant: Instant,
    locale: String,
    calendar: String,
): String {
    val formatter = DateFormat.getInstanceForSkeleton(DateFormat.YEAR_MONTH_DAY, ULocale("$locale@calendar=$calendar"))
    return formatter.format(Date.from(instant.toJavaInstant()))
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
)
@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
    device = Devices.TABLET,
)
@Composable
private fun HomePreview() {
    AlAzanTheme {
        HomeScreen(
            uiState = HomeUiState(
                calendar = "gregorian",
            ),
            onAction = {},
        )
    }
}
