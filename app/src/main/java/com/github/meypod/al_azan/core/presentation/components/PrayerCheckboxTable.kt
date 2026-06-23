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
    CheckboxTable(
        title = title,
        helpText = helpText,
        leftColumn = leftColumn,
        rightColumn = rightColumn,
        items = prayers,
        label = { it.i18n() },
        isChecked = isChecked,
        onToggle = onToggle,
        modifier = modifier,
    )
}

/**
 * A header + per-row checkbox table. Stateless: the whole row is the toggle target and the [Checkbox] is
 * decorative (accessibility comes from the row's [Role.Checkbox]). Any selection limit is the caller's
 * concern — enforce it where [onToggle] is handled.
 */
@Composable
fun <T> CheckboxTable(
    title: String,
    helpText: String,
    leftColumn: String,
    rightColumn: String,
    items: List<T>,
    label: @Composable (T) -> String,
    isChecked: (T) -> Boolean,
    onToggle: (T, Boolean) -> Unit,
    modifier: Modifier = Modifier,
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
        items.forEachIndexed { idx, item ->
            val checked = isChecked(item)
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = checked,
                        role = Role.Checkbox,
                        onValueChange = { onToggle(item, it) },
                    )
                    .padding(
                        vertical = dimensionResource(R.dimen.element_padding),
                        horizontal = dimensionResource(R.dimen.element_padding),
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label(item))
                Checkbox(checked, onCheckedChange = null)
            }
            if (idx != items.lastIndex) HorizontalDivider()
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
