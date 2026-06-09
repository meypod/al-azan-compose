package com.github.meypod.al_azan.main.upcoming_alarms

import android.icu.text.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.i18n
import com.github.meypod.al_azan.core.domain.util.addDaysTimeZoneAware
import com.github.meypod.al_azan.core.domain.util.formatInstant
import com.github.meypod.al_azan.core.domain.util.formatTimeOfDay
import com.github.meypod.al_azan.core.domain.util.isSameGregorianDay
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.InformationCard
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.mapper.reminderDisplayName
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import kotlin.time.Instant

@Composable
fun UpcomingAlarmsScreen(
    uiState: UpcomingAlarmsUiState,
    onAction: (UpcomingAlarmsUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // scrollable = false: the LazyColumn owns scrolling and drives the placement animations.
    ScreenScaffold(
        modifier = modifier,
        title = stringResource(R.string.upcoming_alarms),
        titleIcon = R.drawable.outline_calendar_month_24,
        onBackClick = { NavigationController.navigateBack() },
        scrollable = false,
    ) {
        InformationCard {
            Text(
                stringResource(R.string.upcoming_alarms_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!uiState.loading && uiState.alarms.isEmpty()) {
            Text(
                stringResource(R.string.upcoming_alarms_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.page_padding)),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
                items(uiState.alarms, key = { "${it.id}@${it.fireTimeMs}@${it.skipped}" }) { alarm ->
                    AlarmRow(
                        alarm = alarm,
                        whenText = alarmWhenText(alarm.fireTimeMs, uiState),
                        onSkip = { onAction(UpcomingAlarmsUiAction.OnSkip(alarm.id)) },
                        onReschedule = { onAction(UpcomingAlarmsUiAction.OnReschedule(alarm.id, alarm.fireTimeMs)) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: UpcomingAlarmUi,
    whenText: String,
    onSkip: () -> Unit,
    onReschedule: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Skipped rows read as disabled: muted tonal surface + dimmed text, with a Reschedule (undo) action.
    val contentColor =
        if (alarm.skipped) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    ACard(modifier = modifier, tonalElevation = if (alarm.skipped) 0.dp else 2.dp) { cardPadding ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = alarm.title(),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (alarm.skipped) {
                        "$whenText • ${stringResource(R.string.upcoming_alarm_skipped)}"
                    } else {
                        whenText
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (alarm.skipped) {
                OutlinedButton(onClick = onReschedule) {
                    Text(stringResource(R.string.reschedule))
                }
            } else {
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.skip))
                }
            }
        }
    }
}

@Composable
private fun UpcomingAlarmUi.title(): String {
    val prefix = stringResource(if (isAdhan) R.string.adhan else R.string.reminder)
    val resources = LocalResources.current
    val name = when {
        isAdhan -> prayer?.i18n()

        prayer != null -> reminderDisplayName(
            resources,
            reminderLabel.orEmpty(),
            reminderDuration,
            reminderDurationModifier,
            prayer,
        )

        else -> reminderLabel
    }
    return if (name.isNullOrBlank()) prefix else "$prefix • $name"
}

/** "<Today | Tomorrow | full weekday> · <clock>", localized and honoring the 24-hour setting. */
@Composable
private fun alarmWhenText(
    fireTimeMs: Long,
    state: UpcomingAlarmsUiState,
): String {
    val fire = Instant.fromEpochMilliseconds(fireTimeMs)
    val now = Instant.fromEpochMilliseconds(state.nowMs)
    val day = when {
        isSameGregorianDay(fire, now) -> stringResource(R.string.today)
        isSameGregorianDay(fire, addDaysTimeZoneAware(now, 1)) -> stringResource(R.string.tomorrow)
        else -> formatInstant(fire, state.locale, "gregorian", DateFormat.WEEKDAY, state.numberingSystem)
    }
    val time = formatTimeOfDay(fire, state.is24Hour, state.numberingSystem, state.locale)
    return stringResource(R.string.upcoming_alarm_when, day, time)
}

@Preview
@Preview(device = Devices.TABLET)
@Composable
private fun UpcomingAlarmsPreview() {
    AlAzanThemePreview {
        UpcomingAlarmsScreen(
            uiState = UpcomingAlarmsUiState(
                loading = false,
                alarms = listOf(
                    UpcomingAlarmUi("adhan_alarm", true, Prayer.Dhuhr, null, 1L, skipped = false),
                    UpcomingAlarmUi("reminder_alarm_1", false, Prayer.Fajr, "Wake up", 2L, skipped = true),
                    UpcomingAlarmUi("reminder_alarm_2", false, Prayer.Asr, "", 3L, skipped = false),
                ),
            ),
            onAction = {},
        )
    }
}
