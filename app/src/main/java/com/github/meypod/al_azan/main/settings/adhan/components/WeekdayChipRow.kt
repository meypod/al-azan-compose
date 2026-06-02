package com.github.meypod.al_azan.main.settings.adhan.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePaddedPreview
import com.github.meypod.al_azan.core.presentation.mapper.localized
import kotlinx.datetime.DayOfWeek

private val WEEK_ORDER_SUNDAY_FIRST: List<DayOfWeek> = listOf(
    DayOfWeek.SUNDAY,
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
)

enum class ChipAccent { Primary, Secondary, Tertiary }

@Composable
private fun ChipAccent.filterChipColors() =
    FilterChipDefaults.filterChipColors(
        selectedContainerColor = when (this) {
            ChipAccent.Primary -> MaterialTheme.colorScheme.primaryContainer
            ChipAccent.Secondary -> MaterialTheme.colorScheme.secondaryContainer
            ChipAccent.Tertiary -> MaterialTheme.colorScheme.tertiaryContainer
        },
        selectedLabelColor = when (this) {
            ChipAccent.Primary -> MaterialTheme.colorScheme.onPrimaryContainer
            ChipAccent.Secondary -> MaterialTheme.colorScheme.onSecondaryContainer
            ChipAccent.Tertiary -> MaterialTheme.colorScheme.onTertiaryContainer
        },
    )

@Composable
fun WeekdayChipRow(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
    accent: ChipAccent = ChipAccent.Primary,
    alignment: Alignment.Horizontal = Alignment.Start,
) {
    val chipColors = accent.filterChipColors()
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding_compact), alignment = alignment),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding_compact)),
        ) {
            WEEK_ORDER_SUNDAY_FIRST.forEach { day ->
                val isSelected = day in selected
                key(day.ordinal) {
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggle(day) },
                        label = { Text(day.localized(), style = MaterialTheme.typography.labelLarge) },
                        colors = chipColors,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun WeekdayChipRowPreview() {
    AlAzanThemePaddedPreview {
        WeekdayChipRow(
            selected = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            onToggle = {},
        )
    }
}

@Preview
@Composable
private fun WeekdayChipRowTertiaryPreview() {
    AlAzanThemePaddedPreview {
        WeekdayChipRow(
            selected = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            onToggle = {},
            accent = ChipAccent.Tertiary,
        )
    }
}
