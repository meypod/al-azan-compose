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
import com.github.meypod.al_azan.core.domain.util.calendarMonthSpan
import com.github.meypod.al_azan.core.domain.util.formatDate
import com.github.meypod.al_azan.core.domain.util.formatDateSpan
import com.github.meypod.al_azan.core.domain.util.localeFirstDayOfWeek
import com.github.meypod.al_azan.core.presentation.AlAzanThemePaddedPreview
import com.github.meypod.al_azan.core.presentation.components.CompactOutlinedTextField
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * Outlined field showing [date] in calendars[[calendarIndex]] using [skeleton] (and, when
 * [showOtherCalendar], the next calendar as supporting text: the same date, or by month the span the
 * shown month covers); tapping anywhere on it opens
 * [CalendarDatePickerDialog] on that calendar. [onDateChange] also gets the calendar index showing in
 * the picker when confirmed, for screens whose view follows it.
 */
@Composable
fun CalendarDateField(
    date: LocalDate,
    onDateChange: (date: LocalDate, calendarIndex: Int) -> Unit,
    calendars: List<PickerCalendar>,
    weekLocale: String,
    numberingSystem: NumberingSystem,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    firstDayOfWeek: DayOfWeek = remember(weekLocale) { localeFirstDayOfWeek(weekLocale) },
    isMarked: (picked: LocalDate, date: LocalDate) -> Boolean = { _, _ -> false },
    calendarIndex: Int = 0,
    skeleton: String = DateFormat.YEAR_ABBR_MONTH_WEEKDAY_DAY,
    showOtherCalendar: Boolean = true,
    precision: CalendarPickerPrecision = CalendarPickerPrecision.Day,
    isSelectable: (LocalDate) -> Boolean = { true },
) {
    require(calendars.isNotEmpty()) { "CalendarDateField needs at least one calendar" }
    val shownIndex = calendarIndex.coerceIn(calendars.indices)
    var picking by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    fun formatted(system: CalendarSystem) = formatDate(date, system, skeleton, numberingSystem)

    // By month, the other calendar shows the span the month covers (a month rarely lines up with one).
    fun otherFormatted(system: CalendarSystem) =
        if (precision == CalendarPickerPrecision.Month) {
            formatDateSpan(calendarMonthSpan(date, calendars[shownIndex].system), system, skeleton, numberingSystem)
        } else {
            formatted(system)
        }

    CompactOutlinedTextField(
        value = formatted(calendars[shownIndex].system),
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
        supportingText = calendars.getOrNull((shownIndex + 1) % calendars.size)
            ?.takeIf { showOtherCalendar && calendars.size > 1 }
            ?.let { other -> { Text(otherFormatted(other.system), color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        trailingIcon = { Icon(painterResource(R.drawable.calendar_month_outline), contentDescription = null) },
        fixedLabel = true,
    )

    if (picking) {
        CalendarDatePickerDialog(
            initial = date,
            onConfirm = { picked, index ->
                picking = false
                onDateChange(picked, index)
            },
            onDismiss = { picking = false },
            calendars = calendars,
            weekLocale = weekLocale,
            numberingSystem = numberingSystem,
            firstDayOfWeek = firstDayOfWeek,
            isMarked = isMarked,
            initialCalendarIndex = shownIndex,
            precision = precision,
            isSelectable = isSelectable,
        )
    }
}

@Preview
@Composable
private fun CalendarDateFieldPreview() {
    AlAzanThemePaddedPreview {
        CalendarDateField(
            date = LocalDate(2026, 11, 3),
            onDateChange = { _, _ -> },
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
