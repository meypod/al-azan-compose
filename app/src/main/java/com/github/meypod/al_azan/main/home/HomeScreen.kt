package com.github.meypod.al_azan.main.home

import android.icu.text.DateFormat
import android.icu.util.ULocale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
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
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        onAction(HomeUiAction.OnMenuIconClick)
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
