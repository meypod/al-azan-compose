package com.github.meypod.al_azan.core.presentation.components.calendar

import android.icu.text.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.calendar.CalendarSystem
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.util.formatDate
import com.github.meypod.al_azan.core.domain.util.localeFirstDayOfWeek
import com.github.meypod.al_azan.core.presentation.AlAzanThemePaddedPreview
import com.github.meypod.al_azan.core.presentation.components.CompactOutlinedTextField
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * Outlined field showing [date] in the first of [calendars] (the second, if any, as supporting text);
 * tapping anywhere on it opens [CalendarDatePickerDialog].
 */
@Composable
fun CalendarDateField(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    calendars: List<PickerCalendar>,
    weekLocale: String,
    numberingSystem: NumberingSystem,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    firstDayOfWeek: DayOfWeek = remember(weekLocale) { localeFirstDayOfWeek(weekLocale) },
    isMarked: (picked: LocalDate, date: LocalDate) -> Boolean = { _, _ -> false },
) {
    require(calendars.isNotEmpty()) { "CalendarDateField needs at least one calendar" }
    var picking by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    fun formatted(system: CalendarSystem) = formatDate(date, system, DateFormat.YEAR_ABBR_MONTH_WEEKDAY_DAY, numberingSystem)

    CompactOutlinedTextField(
        value = formatted(calendars.first().system),
        onValueChange = {},
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClickLabel = stringResource(R.string.choose_date),
            onClick = { picking = true },
        ),
        readOnly = true,
        displayOnly = true,
        interactionSource = interactionSource,
        label = label,
        supportingText = calendars.getOrNull(1)?.let { secondary ->
            { Text(formatted(secondary.system), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        trailingIcon = { Icon(painterResource(R.drawable.calendar_month_outline), contentDescription = null) },
        fixedLabel = true,
    )

    if (picking) {
        CalendarDatePickerDialog(
            initial = date,
            onConfirm = {
                picking = false
                onDateChange(it)
            },
            onDismiss = { picking = false },
            calendars = calendars,
            weekLocale = weekLocale,
            numberingSystem = numberingSystem,
            firstDayOfWeek = firstDayOfWeek,
            isMarked = isMarked,
        )
    }
}

@Preview
@Composable
private fun CalendarDateFieldPreview() {
    AlAzanThemePaddedPreview {
        CalendarDateField(
            date = LocalDate(2026, 11, 3),
            onDateChange = {},
            calendars = listOf(
                PickerCalendar(CalendarSystem("gregorian", "en"), "Gregorian"),
                PickerCalendar(CalendarSystem("islamic-umalqura", "en"), "Lunar"),
            ),
            weekLocale = "en",
            numberingSystem = NumberingSystem.Default,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
