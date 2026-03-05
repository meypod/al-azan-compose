package com.github.meypod.al_azan.main.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.i18n
import com.github.meypod.al_azan.core.domain.utils.formatInstant
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.util.dashedBorder
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

@Immutable
data class ShariaTimeRowUiState(
    val prayer: Prayer,
    val instant: Instant?,
    val locale: String = "en-US",
    val numberingSystem: String? = null,
    val is24Hours: Boolean = true,
)

@Composable
fun ShariaTimeRow(state: ShariaTimeRowUiState) {
    Row(Modifier.padding(dimensionResource(R.dimen.element_padding))) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    if (state.prayer.isNonPrayer) {
                        Modifier.dashedBorder(MaterialTheme.colorScheme.outline)
                    } else {
                        Modifier
                    },
                )
                .padding(dimensionResource(R.dimen.element_padding)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(state.prayer.i18n())
            if (state.instant == null) {
                Text("--:--")
            } else {
                Text(
                    formatInstant(
                        state.instant,
                        state.locale,
                        "gregorian",
                        if (state.is24Hours) "HH:mm" else "hh:mm",
                        state.numberingSystem,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ShariaTimeRowPreview() {
    AlAzanTheme {
        Column {
            ShariaTimeRow(
                ShariaTimeRowUiState(
                    Prayer.Fajr,
                    Clock.System.now().plus(12.toDuration(DurationUnit.HOURS)),
                    is24Hours = false,

                ),
            )
            ShariaTimeRow(
                ShariaTimeRowUiState(
                    Prayer.Sunrise,
                    Clock.System.now(),
                ),
            )
            ShariaTimeRow(
                ShariaTimeRowUiState(
                    Prayer.Dhuhr,
                    null,
                ),
            )
        }
    }
}
