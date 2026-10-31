package com.github.meypod.al_azan.core.presentation.components.calendar

import android.icu.text.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.calendar.CalendarSystem
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.util.CalendarMonth
import com.github.meypod.al_azan.core.domain.util.addCalendarMonths
import com.github.meypod.al_azan.core.domain.util.addCalendarYears
import com.github.meypod.al_azan.core.domain.util.calendarMonth
import com.github.meypod.al_azan.core.domain.util.calendarYearMonths
import com.github.meypod.al_azan.core.domain.util.formatDate
import com.github.meypod.al_azan.core.domain.util.localeFirstDayOfWeek
import com.github.meypod.al_azan.core.domain.util.narrowWeekdayNames
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import com.github.meypod.al_azan.core.domain.util.weekdaysFrom
import com.github.meypod.al_azan.core.presentation.AlAzanThemePaddedPreview
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

/** A calendar the picker can show, with the name its toggle displays. */
@Immutable
data class PickerCalendar(
    val system: CalendarSystem,
    val label: String,
)

/**
 * Month grid date picker that reads dates in any of [calendars], switchable with a toggle when there
 * is more than one. Dates are always real civil dates, so switching calendars keeps the selection.
 *
 * @param weekLocale locale of the weekday names heading the grid.
 * @param initialCalendarIndex the calendar shown first; [onCalendarIndexChange] reports toggles.
 * @param isMarked days to tint faintly, e.g. the occurrences that follow from the selection.
 * @param isSelectable days the user may pick; others are dimmed and inert (e.g. past days).
 */
@Composable
fun CalendarDatePicker(
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    calendars: List<PickerCalendar>,
    weekLocale: String,
    numberingSystem: NumberingSystem,
    modifier: Modifier = Modifier,
    firstDayOfWeek: DayOfWeek = remember(weekLocale) { localeFirstDayOfWeek(weekLocale) },
    today: LocalDate = remember { Clock.System.now().toLocalDate() },
    isMarked: (LocalDate) -> Boolean = { false },
    isSelectable: (LocalDate) -> Boolean = { true },
    initialCalendarIndex: Int = 0,
    onCalendarIndexChange: (Int) -> Unit = {},
    precision: CalendarPickerPrecision = CalendarPickerPrecision.Day,
) {
    require(calendars.isNotEmpty()) { "CalendarDatePicker needs at least one calendar" }
    val byMonth = precision == CalendarPickerPrecision.Month
    var calendarIndex by rememberSaveable { mutableIntStateOf(initialCalendarIndex) }
    val system = calendars[calendarIndex.coerceIn(calendars.indices)].system
    // Any day inside the shown month (or, by month, the shown year); saved as epoch days since
    // LocalDate is not Saveable.
    var shownEpochDay by rememberSaveable { mutableLongStateOf(selected.toEpochDays()) }
    val shown = LocalDate.fromEpochDays(shownEpochDay)
    val homeView = if (byMonth) PickerView.Months else PickerView.Days
    var view by rememberSaveable { mutableStateOf(homeView) }
    val month = remember(shown, system, numberingSystem) { calendarMonth(shown, system, numberingSystem) }
    // Follow a selection made from outside (e.g. a "Today" action) when it lands outside the shown
    // month (or year, by month).
    LaunchedEffect(selected) {
        val visible = if (byMonth) {
            formatDate(selected, system, DateFormat.YEAR, numberingSystem) == month.yearLabel
        } else {
            month.days.any { it.date == selected }
        }
        if (!visible) {
            shownEpochDay = selected.toEpochDays()
            view = homeView
        }
    }
    val show = { date: LocalDate ->
        shownEpochDay = date.toEpochDays()
        view = homeView
    }
    val pickMonth = { date: LocalDate -> if (byMonth) onSelect(date) else show(date) }
    // Arrows step months over the day grid and years over the month grid; hidden in the year list.
    val stepBy = { delta: Int ->
        shownEpochDay = (if (byMonth) addCalendarYears(shown, delta, system) else addCalendarMonths(shown, delta, system)).toEpochDays()
    }

    BoxWithConstraints(modifier) {
        val spacing = dimensionResource(R.dimen.element_padding)
        val density = LocalDensity.current
        // Measured, so the day rows can take exactly the height the toggle and header leave over.
        var chromeHeight by remember { mutableStateOf(0.dp) }
        val squareRow = maxWidth / 7
        val rowHeight = if (constraints.hasBoundedHeight) {
            ((maxHeight - chromeHeight - spacing - WEEKDAY_ROW_HEIGHT) / MAX_WEEKS).coerceIn(MIN_ROW_HEIGHT, squareRow)
        } else {
            squareRow
        }
        // Scrolls only when even minimum-height rows don't fit (e.g. a landscape phone).
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            Column(
                modifier = Modifier.onSizeChanged { chromeHeight = with(density) { it.height.toDp() } },
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                if (calendars.size > 1) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        calendars.forEachIndexed { index, calendar ->
                            SegmentedButton(
                                selected = index == calendarIndex,
                                onClick = {
                                    calendarIndex = index
                                    onCalendarIndexChange(index)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, calendars.size),
                            ) {
                                Text(calendar.label)
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val stepping = view == homeView
                    val (prevLabel, nextLabel) = if (byMonth) R.string.prev_year to R.string.next_year else R.string.prev_month to R.string.next_month
                    // Kept in layout when hidden, so the month and year buttons don't jump between views.
                    IconButton(
                        onClick = { stepBy(-1) },
                        enabled = stepping,
                        modifier = Modifier.alpha(if (stepping) 1f else 0f),
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = stringResource(prevLabel))
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (!byMonth) {
                            ViewToggleButton(
                                label = month.monthLabel,
                                open = view == PickerView.Months,
                                onClick = { view = if (view == PickerView.Months) homeView else PickerView.Months },
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        }
                        ViewToggleButton(
                            label = month.yearLabel,
                            open = view == PickerView.Years,
                            onClick = { view = if (view == PickerView.Years) homeView else PickerView.Years },
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                    IconButton(
                        onClick = { stepBy(1) },
                        enabled = stepping,
                        modifier = Modifier.alpha(if (stepping) 1f else 0f),
                    ) {
                        Icon(painterResource(R.drawable.arrow_forward), contentDescription = stringResource(nextLabel))
                    }
                }
            }
            // Same size in every view and month, so the dialog does not jump around.
            Box(Modifier.fillMaxWidth().height(WEEKDAY_ROW_HEIGHT + rowHeight * MAX_WEEKS)) {
                when (view) {
                    PickerView.Days -> DaysView(month, selected, today, weekLocale, firstDayOfWeek, rowHeight, isMarked, isSelectable, onSelect)
                    PickerView.Months -> MonthsView(
                        shown = shown,
                        highlighted = if (byMonth) selected else shown,
                        today = today,
                        system = system,
                        numberingSystem = numberingSystem,
                        onPick = pickMonth,
                    )
                    PickerView.Years -> YearsView(shown, today, system, numberingSystem, onPick = show)
                }
            }
        }
    }
}

/** Whether a [CalendarDatePicker] picks a day or a whole month (any day of it stands for the month). */
enum class CalendarPickerPrecision { Day, Month }

private enum class PickerView { Days, Months, Years }

private const val MAX_WEEKS = 6
private val WEEKDAY_ROW_HEIGHT = 24.dp

/** Smallest day row before the picker scrolls instead; still fits a day number comfortably. */
private val MIN_ROW_HEIGHT = 28.dp
private const val YEAR_RANGE = 100
private const val YEAR_COLUMNS = 3
private const val MONTH_COLUMNS = 3

@Composable
private fun ViewToggleButton(
    label: String,
    open: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Icon(
            painterResource(R.drawable.baseline_arrow_drop_down_24),
            contentDescription = null,
            modifier = Modifier.rotate(if (open) 180f else 0f),
        )
    }
}

@Composable
private fun DaysView(
    month: CalendarMonth,
    selected: LocalDate,
    today: LocalDate,
    weekLocale: String,
    firstDayOfWeek: DayOfWeek,
    rowHeight: Dp,
    isMarked: (LocalDate) -> Boolean,
    isSelectable: (LocalDate) -> Boolean,
    onSelect: (LocalDate) -> Unit,
) {
    val weekdays = remember(firstDayOfWeek) { weekdaysFrom(firstDayOfWeek) }
    val weekdayNames = remember(weekLocale) { narrowWeekdayNames(weekLocale) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(WEEKDAY_ROW_HEIGHT).clearAndSetSemantics {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            weekdays.forEach { day ->
                Text(
                    text = weekdayNames.getValue(day),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val leadingBlanks = weekdays.indexOf(month.days.first().date.dayOfWeek)
        val cells = List(leadingBlanks) { null } + month.days
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).height(rowHeight), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            DayCell(
                                label = day.label,
                                description = day.description,
                                isSelected = day.date == selected,
                                isToday = day.date == today,
                                isMarked = isMarked(day.date),
                                enabled = isSelectable(day.date),
                                onClick = { onSelect(day.date) },
                            )
                        }
                    }
                }
                repeat(7 - week.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MonthsView(
    shown: LocalDate,
    highlighted: LocalDate,
    today: LocalDate,
    system: CalendarSystem,
    numberingSystem: NumberingSystem,
    onPick: (LocalDate) -> Unit,
) {
    val months = remember(shown, system, numberingSystem) { calendarYearMonths(shown, system, numberingSystem) }
    val currentMonth = remember(today, system) { calendarYearMonths(today, system, numberingSystem).last { it.date <= today }.date }
    val highlightedMonth = remember(highlighted, system) { calendarYearMonths(highlighted, system, numberingSystem).last { it.date <= highlighted }.date }
    LazyVerticalGrid(columns = GridCells.Fixed(MONTH_COLUMNS), modifier = Modifier.fillMaxSize()) {
        items(months, key = { it.date.toEpochDays() }) { month ->
            SelectorCell(
                label = month.label,
                isSelected = month.date == highlightedMonth,
                isCurrent = month.date == currentMonth,
                onClick = { onPick(month.date) },
            )
        }
    }
}

@Composable
private fun YearsView(
    shown: LocalDate,
    today: LocalDate,
    system: CalendarSystem,
    numberingSystem: NumberingSystem,
    onPick: (LocalDate) -> Unit,
) {
    val offsets = -YEAR_RANGE..YEAR_RANGE
    val currentYear = remember(today, system, numberingSystem) { formatDate(today, system, DateFormat.YEAR, numberingSystem) }
    // Open with the shown year in view, a couple of rows from the top.
    val state = rememberLazyGridState(initialFirstVisibleItemIndex = (YEAR_RANGE - 2 * YEAR_COLUMNS).coerceAtLeast(0))
    LazyVerticalGrid(columns = GridCells.Fixed(YEAR_COLUMNS), state = state, modifier = Modifier.fillMaxSize()) {
        items(offsets.count(), key = { it }) { index ->
            val offset = offsets.first + index
            val date = remember(shown, system, offset) { addCalendarYears(shown, offset, system) }
            val label = formatDate(date, system, DateFormat.YEAR, numberingSystem)
            SelectorCell(
                label = label,
                isSelected = offset == 0,
                isCurrent = label == currentYear,
                onClick = { onPick(date) },
            )
        }
    }
}

@Composable
private fun SelectorCell(
    label: String,
    isSelected: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(4.dp)
            .clip(CircleShape)
            .then(if (isSelected) Modifier.background(colors.primary) else Modifier)
            // The current month or year is ringed like today in the day grid.
            .then(if (isCurrent && !isSelected) Modifier.border(1.dp, colors.primary, CircleShape) else Modifier)
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isSelected -> colors.onPrimary
                isCurrent -> colors.primary
                else -> colors.onSurface
            },
            fontWeight = if (isCurrent) FontWeight.Bold else null,
            maxLines = 1,
        )
    }
}

private const val MARKED_ALPHA = 0.16f
private const val DISABLED_ALPHA = 0.38f

@Composable
private fun DayCell(
    label: String,
    description: String,
    isSelected: Boolean,
    isToday: Boolean,
    isMarked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(2.dp)
            .fillMaxHeight()
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(
                when {
                    isSelected -> Modifier.background(colors.primary)
                    // A faint fill, so the today ring stays distinguishable on a marked day.
                    isMarked -> Modifier.background(colors.primary.copy(alpha = MARKED_ALPHA))
                    else -> Modifier
                },
            )
            .then(if (isToday && !isSelected) Modifier.border(1.dp, colors.primary, CircleShape) else Modifier)
            .selectable(selected = isSelected, enabled = enabled, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = description },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isSelected -> colors.onPrimary
                isToday -> colors.primary
                else -> colors.onSurface
            },
            fontWeight = if (isToday) FontWeight.Bold else null,
            modifier = Modifier
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clearAndSetSemantics {},
        )
    }
}

/**
 * [CalendarDatePicker] in a dialog; the choice is applied only on confirm. A "Today" action jumps to
 * and selects today. Fits narrow screens (unlike the Material date dialog's fixed 360dp width).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDatePickerDialog(
    initial: LocalDate,
    /** The picked date and the index of the calendar showing when confirmed. */
    onConfirm: (date: LocalDate, calendarIndex: Int) -> Unit,
    onDismiss: () -> Unit,
    calendars: List<PickerCalendar>,
    weekLocale: String,
    numberingSystem: NumberingSystem,
    firstDayOfWeek: DayOfWeek = remember(weekLocale) { localeFirstDayOfWeek(weekLocale) },
    isMarked: (picked: LocalDate, date: LocalDate) -> Boolean = { _, _ -> false },
    isSelectable: (LocalDate) -> Boolean = { true },
    today: LocalDate = remember { Clock.System.now().toLocalDate() },
    initialCalendarIndex: Int = 0,
    precision: CalendarPickerPrecision = CalendarPickerPrecision.Day,
) {
    var picked by rememberSaveable { mutableLongStateOf(initial.toEpochDays()) }
    var calendarIndex by rememberSaveable { mutableIntStateOf(initialCalendarIndex) }
    val padding = dimensionResource(R.dimen.page_padding)
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(padding)
            .widthIn(max = DIALOG_MAX_WIDTH),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(padding)) {
                CalendarDatePicker(
                    selected = LocalDate.fromEpochDays(picked),
                    onSelect = { picked = it.toEpochDays() },
                    calendars = calendars,
                    weekLocale = weekLocale,
                    numberingSystem = numberingSystem,
                    // Bounded by the dialog, so the grid shrinks to fit short screens.
                    modifier = Modifier.weight(1f, fill = false),
                    firstDayOfWeek = firstDayOfWeek,
                    today = today,
                    isMarked = { isMarked(LocalDate.fromEpochDays(picked), it) },
                    isSelectable = isSelectable,
                    initialCalendarIndex = initialCalendarIndex,
                    onCalendarIndexChange = { calendarIndex = it },
                    precision = precision,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { picked = today.toEpochDays() }, enabled = isSelectable(today)) { Text(stringResource(R.string.today)) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = { onConfirm(LocalDate.fromEpochDays(picked), calendarIndex) }) { Text(stringResource(R.string.okay)) }
                }
            }
        }
    }
}

private val DIALOG_MAX_WIDTH = 420.dp

@Preview
@Composable
private fun CalendarDatePickerPreview() {
    val today = LocalDate(2026, 10, 31)
    AlAzanThemePaddedPreview {
        CalendarDatePicker(
            selected = LocalDate(2026, 11, 3),
            onSelect = {},
            calendars = listOf(
                PickerCalendar(CalendarSystem("gregorian", "en"), "Gregorian"),
                PickerCalendar(CalendarSystem("islamic-umalqura", "en"), "Lunar"),
            ),
            weekLocale = "en",
            numberingSystem = NumberingSystem.Default,
            today = today,
            isMarked = { it > LocalDate(2026, 11, 3) && (it.toEpochDays() - LocalDate(2026, 11, 3).toEpochDays()) % 3 == 0L },
        )
    }
}

@Preview(locale = "fa")
@Composable
private fun CalendarDatePickerPersianPreview() {
    AlAzanThemePaddedPreview {
        CalendarDatePicker(
            selected = LocalDate(2026, 11, 3),
            onSelect = {},
            calendars = listOf(PickerCalendar(CalendarSystem("persian", "fa"), "Persian")),
            weekLocale = "fa",
            numberingSystem = NumberingSystem.Default,
            today = LocalDate(2026, 10, 31),
        )
    }
}

@Preview
@Composable
private fun CalendarMonthPickerPreview() {
    AlAzanThemePaddedPreview {
        CalendarDatePicker(
            selected = LocalDate(2026, 11, 3),
            onSelect = {},
            calendars = listOf(PickerCalendar(CalendarSystem("gregorian", "en"), "Gregorian")),
            weekLocale = "en",
            numberingSystem = NumberingSystem.Default,
            today = LocalDate(2026, 10, 31),
            precision = CalendarPickerPrecision.Month,
        )
    }
}
