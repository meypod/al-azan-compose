package com.github.meypod.al_azan.main.prayer_log

import android.icu.text.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.calendar.secondaryCalendarSystem
import com.github.meypod.al_azan.core.domain.model.prayer_log.LOGGABLE_DAILY_PRAYERS
import com.github.meypod.al_azan.core.domain.model.prayer_log.PrayerRecordStatus
import com.github.meypod.al_azan.core.domain.util.formatInstant
import com.github.meypod.al_azan.core.domain.util.formatWithUnicodeDigits
import com.github.meypod.al_azan.core.domain.util.resolvedFirstDayOfWeek
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.calendar.CalendarDateField
import com.github.meypod.al_azan.core.presentation.components.calendar.CalendarPickerPrecision
import com.github.meypod.al_azan.core.presentation.components.calendar.rememberUserPickerCalendars
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.prayer_checkin.localNoonInstant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerLogScreen(
    uiState: PrayerLogUiState,
    onAction: (PrayerLogUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = stringResource(R.string.prayer_log_title),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
        actions = {
            if (uiState.selectedDate != uiState.today) {
                IconButton(onClick = { onAction(PrayerLogUiAction.OnTodayClick) }) {
                    Icon(
                        painter = painterResource(R.drawable.calendar_month_outline),
                        contentDescription = stringResource(R.string.today),
                    )
                }
            }
        },
    ) {
        DateNavigationCard(uiState, onAction)
        SummaryCard(uiState)
        PrayerListCard(uiState, onAction)
        RecentHistoryCard(uiState, onAction)
        if (!uiState.prayerCheckInEnabled) {
            CheckInDisabledHintCard(onOpenSettings = { onAction(PrayerLogUiAction.OnOpenCheckInSettingsClick) })
        }
    }
}

@Composable
private fun DateNavigationCard(
    uiState: PrayerLogUiState,
    onAction: (PrayerLogUiAction) -> Unit,
) {
    ACard { cardPadding ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { onAction(PrayerLogUiAction.OnPrevDayClick) }) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.back_button),
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                CalendarDateField(
                    date = uiState.selectedDate,
                    onDateChange = { date, _ -> onAction(PrayerLogUiAction.OnDatePicked(date)) },
                    calendars = rememberUserPickerCalendars(uiState.settings, uiState.hijriDateAdjustment),
                    weekLocale = uiState.settings.selectedLocale,
                    numberingSystem = uiState.settings.numberingSystem,
                    modifier = Modifier.fillMaxWidth(),
                    firstDayOfWeek = uiState.settings.resolvedFirstDayOfWeek(),
                    skeleton = DateFormat.YEAR_NUM_MONTH_DAY,
                    precision = CalendarPickerPrecision.Day,
                )
            }

            IconButton(onClick = { onAction(PrayerLogUiAction.OnNextDayClick) }) {
                Icon(
                    painter = painterResource(R.drawable.arrow_forward),
                    contentDescription = stringResource(R.string.next_button),
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(uiState: PrayerLogUiState) {
    val record = uiState.currentRecord
    val prayedCount = record?.prayedCount() ?: 0
    val totalCount = LOGGABLE_DAILY_PRAYERS.size
    val progress = prayedCount.toFloat() / totalCount.toFloat()
    val formattedCount = formatWithUnicodeDigits(
        stringResource(R.string.prayed_count_format, prayedCount, totalCount),
        uiState.settings.numberingSystem,
        uiState.settings.selectedLocale,
    )

    ACard { cardPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.prayer_log_streak),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = formattedCount,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
            )
        }
    }
}

@Composable
private fun PrayerListCard(
    uiState: PrayerLogUiState,
    onAction: (PrayerLogUiAction) -> Unit,
) {
    ACard { cardPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LOGGABLE_DAILY_PRAYERS.forEach { prayer ->
                val status = uiState.currentRecord?.statusFor(prayer)
                val timeLabel = uiState.prayerTimes[prayer].orEmpty()
                PrayerRow(
                    prayer = prayer,
                    timeLabel = timeLabel,
                    status = status,
                    onToggle = { onAction(PrayerLogUiAction.OnTogglePrayerStatus(prayer)) },
                    onSetStatus = { newStatus -> onAction(PrayerLogUiAction.OnSetPrayerStatus(prayer, newStatus)) },
                )
            }
        }
    }
}

@Composable
private fun PrayerRow(
    prayer: Prayer,
    timeLabel: String,
    status: PrayerRecordStatus?,
    onToggle: () -> Unit,
    onSetStatus: (PrayerRecordStatus?) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(prayer.stringRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (timeLabel.isNotBlank()) {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Box {
            StatusChip(
                status = status,
                onClick = { menuExpanded = true },
            )

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.mark_as_prayed)) },
                    onClick = {
                        onSetStatus(PrayerRecordStatus.Prayed)
                        menuExpanded = false
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.baseline_check_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.mark_as_missed)) },
                    onClick = {
                        onSetStatus(PrayerRecordStatus.Missed)
                        menuExpanded = false
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.baseline_close_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.clear_status)) },
                    onClick = {
                        onSetStatus(null)
                        menuExpanded = false
                    },
                )
            }
        }
    }
}

private data class StatusChipStyle(
    val containerColor: Color,
    val contentColor: Color,
    val labelRes: Int,
    val iconRes: Int?,
)

@Composable
private fun StatusChip(
    status: PrayerRecordStatus?,
    onClick: () -> Unit,
) {
    val style = when (status) {
        PrayerRecordStatus.Prayed -> StatusChipStyle(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            R.string.prayer_status_prayed,
            R.drawable.baseline_check_24,
        )
        PrayerRecordStatus.Missed -> StatusChipStyle(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            R.string.prayer_status_missed,
            R.drawable.baseline_close_24,
        )
        null -> StatusChipStyle(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            R.string.prayer_status_pending,
            null,
        )
    }

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = style.containerColor,
        contentColor = style.contentColor,
        modifier = Modifier.padding(start = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (style.iconRes != null) {
                Icon(
                    painter = painterResource(style.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = stringResource(style.labelRes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun RecentHistoryCard(
    uiState: PrayerLogUiState,
    onAction: (PrayerLogUiAction) -> Unit,
) {
    val recent = uiState.recentRecords
    if (recent.isEmpty()) return
    val secondaryCal = uiState.settings.secondaryCalendarSystem()

    ACard { cardPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.prayer_log_recent_history),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            recent.forEach { record ->
                val formattedDate = formatInstant(
                    instant = localNoonInstant(record.date),
                    locale = secondaryCal.locale,
                    calendar = secondaryCal.calendar,
                    skeleton = DateFormat.YEAR_ABBR_MONTH_DAY,
                    numberingSystem = uiState.settings.numberingSystem,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onAction(PrayerLogUiAction.OnDatePicked(record.date)) }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (record.date == uiState.selectedDate) FontWeight.Bold else FontWeight.Normal,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        LOGGABLE_DAILY_PRAYERS.forEach { prayer ->
                            val dotColor = when (record.statusFor(prayer)) {
                                PrayerRecordStatus.Prayed -> MaterialTheme.colorScheme.primary
                                PrayerRecordStatus.Missed -> MaterialTheme.colorScheme.error
                                null -> MaterialTheme.colorScheme.outlineVariant
                            }
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = dotColor,
                                modifier = Modifier.size(10.dp),
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckInDisabledHintCard(onOpenSettings: () -> Unit) {
    ACard { cardPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.prayer_checkin_disabled_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.prayer_checkin_open_settings))
            }
        }
    }
}
