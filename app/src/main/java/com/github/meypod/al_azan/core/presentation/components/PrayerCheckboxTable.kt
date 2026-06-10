package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.adhan.i18n
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview

@Composable
fun PrayerCheckboxTable(
    title: String,
    helpText: String,
    leftColumn: String,
    rightColumn: String,
    isChecked: (Prayer) -> Boolean,
    onToggle: (Prayer, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    prayers: List<Prayer> = SHARIA_TIMES_IN_ORDER,
) {
    Column(modifier) {
        SettingHeader(title, helpText)
        Spacer(Modifier.height(dimensionResource(R.dimen.element_padding)))
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(vertical = dimensionResource(R.dimen.element_padding), horizontal = dimensionResource(R.dimen.element_padding)),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(leftColumn, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(rightColumn, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
        prayers.forEachIndexed { idx, prayer ->
            val checked = isChecked(prayer)
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = checked,
                        role = Role.Checkbox,
                        onValueChange = { onToggle(prayer, it) },
                    )
                    .padding(
                        vertical = dimensionResource(R.dimen.element_padding),
                        horizontal = dimensionResource(R.dimen.element_padding),
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(prayer.i18n())
                Checkbox(checked, onCheckedChange = null)
            }
            if (idx != prayers.lastIndex) HorizontalDivider()
        }
    }
}

@Preview
@Composable
private fun PrayerCheckboxTablePreview() {
    AlAzanThemePreview {
        var hidden by remember { mutableStateOf(setOf(Prayer.Sunrise)) }
        ACard { cardPadding ->
            PrayerCheckboxTable(
                title = stringResource(R.string.show_prayer_times_title),
                helpText = stringResource(R.string.show_prayer_times_help),
                leftColumn = stringResource(R.string.time_column),
                rightColumn = stringResource(R.string.show_column),
                isChecked = { it !in hidden },
                onToggle = { prayer, checked ->
                    hidden = if (checked) hidden - prayer else hidden + prayer
                },
                modifier = Modifier.padding(cardPadding),
            )
        }
    }
}
