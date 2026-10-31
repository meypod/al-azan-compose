package com.github.meypod.al_azan.core.domain.util

import android.content.res.Resources
import android.icu.text.DateFormat
import android.icu.text.DateIntervalFormat
import android.icu.util.DateInterval
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import java.text.FieldPosition
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import java.util.Date
import kotlin.math.abs
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration
import kotlin.time.toJavaInstant
import kotlinx.datetime.TimeZone as KotlinxTimeZone

fun formatInstant(
    instant: Instant,
    locale: String = "en-US",
    calendar: String = "gregorian",
    skeleton: String = DateFormat.YEAR_MONTH_DAY,
    numberingSystem: NumberingSystem = NumberingSystem.Default,
): String = dateFormatter(locale, calendar, skeleton, numberingSystem).format(Date.from(instant.toJavaInstant())).withoutEraPlaceholder()

/**
 * [instant] formatted like [formatInstant], but naming the weekday of [weekdayOf]. A day-shifted
 * calendar (the Hijri adjustment) moves the date, not the weekday it falls on.
 */
fun formatInstant(
    instant: Instant,
    weekdayOf: Instant,
    locale: String,
    calendar: String,
    skeleton: String,
    numberingSystem: NumberingSystem,
): String {
    val formatter = dateFormatter(locale, calendar, skeleton, numberingSystem)
    val weekday = FieldPosition(DateFormat.DAY_OF_WEEK_FIELD)
    val text = formatter.format(Date.from(instant.toJavaInstant()), StringBuffer(), weekday)
    if (weekday.endIndex > weekday.beginIndex) {
        val actual = FieldPosition(DateFormat.DAY_OF_WEEK_FIELD)
        val actualText = formatter.format(Date.from(weekdayOf.toJavaInstant()), StringBuffer(), actual)
        text.replace(weekday.beginIndex, weekday.endIndex, actualText.substring(actual.beginIndex, actual.endIndex))
    }
    return text.toString().withoutEraPlaceholder()
}

private fun dateFormatter(
    locale: String,
    calendar: String,
    skeleton: String,
    numberingSystem: NumberingSystem,
): DateFormat {
    val numbers = if (numberingSystem == NumberingSystem.Default) "" else ";numbers=${numberingSystem.value}"
    return try {
        DateFormat.getInstanceForSkeleton(skeleton, ULocale("$locale@calendar=$calendar$numbers"))
    } catch (e: IllegalArgumentException) {
        // Some OEM-modified ICU builds reject Islamic variants stock ICU supports ("Unknown calendar type").
        // Degrade to plain "islamic" rather than crash the caller (e.g. alarm scheduling).
        if (calendar == "islamic" || !calendar.startsWith("islamic")) throw e
        DateFormat.getInstanceForSkeleton(skeleton, ULocale("$locale@calendar=islamic$numbers"))
    }
}

// ICU has no era names for some calendars in many languages (Ethiopic) and prints a placeholder like
// "ERA1" instead; the year reads correctly without it.
private val ERA_PLACEHOLDER = Regex("\\s*\\bERA\\d\\b")

private fun String.withoutEraPlaceholder(): String = replace(ERA_PLACEHOLDER, "")

/** The span [start]..[end] in [calendar], collapsing what both ends share ("Aban – Azar 1405"). */
fun formatInstantRange(
    start: Instant,
    end: Instant,
    locale: String,
    calendar: String,
    skeleton: String,
    numberingSystem: NumberingSystem = NumberingSystem.Default,
): String {
    val numbers = if (numberingSystem == NumberingSystem.Default) "" else ";numbers=${numberingSystem.value}"
    val formatter = try {
        DateIntervalFormat.getInstance(skeleton, ULocale("$locale@calendar=$calendar$numbers"))
    } catch (e: IllegalArgumentException) {
        // Same OEM ICU fallback as [formatInstant].
        if (calendar == "islamic" || !calendar.startsWith("islamic")) throw e
        DateIntervalFormat.getInstance(skeleton, ULocale("$locale@calendar=islamic$numbers"))
    }
    return formatter.format(DateInterval(start.toEpochMilliseconds(), end.toEpochMilliseconds())).toString().withoutEraPlaceholder()
}

fun formatTimeOfDay(
    instant: Instant,
    is24Hour: Boolean,
    numberingSystem: NumberingSystem = NumberingSystem.Default,
    locale: String = "en-US",
): String =
    formatInstant(
        instant = instant,
        locale = locale,
        calendar = "gregorian",
        skeleton = if (is24Hour) DateFormat.HOUR24_MINUTE else DateFormat.HOUR_MINUTE,
        numberingSystem = numberingSystem,
    )

/** Formats [timestamp] (epoch millis) as a time-of-day string per the user's locale settings. */
fun Settings.formatTime(timestamp: Long): String =
    formatTimeOfDay(
        instant = Instant.fromEpochMilliseconds(timestamp),
        is24Hour = is24HourFormat,
        numberingSystem = numberingSystem,
        locale = selectedLocale,
    )

/**
 * Time-of-day for [timestamp] with a relative day appended once it isn't today: just the time today,
 * "<time>, Tomorrow" tomorrow, "<time>, <weekday>" within the week and "<time>, <date>" later. Used for
 * reschedule feedback, which is otherwise ambiguous once a skip pushes the next firing past today.
 * [nowMs] is the reference instant.
 */
fun Settings.formatRescheduleWhen(
    timestamp: Long,
    nowMs: Long,
    resources: Resources,
): String {
    val fire = Instant.fromEpochMilliseconds(timestamp)
    val now = Instant.fromEpochMilliseconds(nowMs)
    val time = formatTime(timestamp)
    val day = when {
        isSameGregorianDay(fire, now) -> return time
        isSameGregorianDay(fire, addDaysTimeZoneAware(now, 1)) -> resources.getString(R.string.tomorrow)
        // A weekday alone is ambiguous past the coming week (monthly or long-interval reminders).
        fire - now < 6.days -> formatInstant(fire, selectedLocale, "gregorian", DateFormat.WEEKDAY, numberingSystem)
        else -> formatInstant(fire, selectedLocale, "gregorian", DateFormat.ABBR_MONTH_WEEKDAY_DAY, numberingSystem)
    }
    return resources.getString(R.string.reschedule_when, time, day)
}

/**
 * A *past* [timestamp] rendered for a "missed" notice: just the time when it's today, but the full
 * date and time once it isn't — so the notice stays unambiguous after the device was off for days,
 * weeks, or longer. [nowMs] is the reference instant. Date stays Gregorian (matches [isSameGregorianDay]).
 */
fun Settings.formatMissedWhen(
    timestamp: Long,
    nowMs: Long,
): String {
    val fire = Instant.fromEpochMilliseconds(timestamp)
    if (isSameGregorianDay(fire, Instant.fromEpochMilliseconds(nowMs))) return formatTime(timestamp)
    val skeleton = DateFormat.YEAR_MONTH_DAY +
        if (is24HourFormat) DateFormat.HOUR24_MINUTE else DateFormat.HOUR_MINUTE
    return formatInstant(fire, selectedLocale, "gregorian", skeleton, numberingSystem)
}

fun isInRamadan(
    instant: Instant,
    arabicCalendar: String,
) = formatInstant(instant, "en", arabicCalendar, DateFormat.MONTH).contains("ramadan", ignoreCase = true)

/** Sha'ban is the Hijri month before Ramadan; "ban" is unique to it among the month names. */
fun isInShaaban(
    instant: Instant,
    arabicCalendar: String,
) = formatInstant(instant, "en", arabicCalendar, DateFormat.MONTH).contains("ban", ignoreCase = true)

/** The Hijri year for [instant] (e.g. "1446"), used as a stable per-year key. */
fun hijriYear(
    instant: Instant,
    arabicCalendar: String,
): String = formatInstant(instant, "en", arabicCalendar, DateFormat.YEAR)

private fun hijriDayOfMonth(
    instant: Instant,
    arabicCalendar: String,
): Int {
    val cal = icuCalendar(arabicCalendar)
    cal.timeInMillis = instant.toEpochMilliseconds()
    return cal.get(Calendar.DAY_OF_MONTH)
}

private const val RAMADAN_NOTICE_DAY_THRESHOLD = 24

/**
 * True in the last days of Sha'ban (Ramadan's start may be off) or of Ramadan (its end may be off) —
 * when the pre-calculated calendar is most likely to disagree with moon-sighting dates.
 */
fun isRamadanNoticeDue(
    instant: Instant,
    arabicCalendar: String,
): Boolean {
    if (hijriDayOfMonth(instant, arabicCalendar) <= RAMADAN_NOTICE_DAY_THRESHOLD) return false
    return isInRamadan(instant, arabicCalendar) || isInShaaban(instant, arabicCalendar)
}

fun addDaysTimeZoneAware(
    instant: Instant,
    days: Int,
): Instant {
    if (days == 0) return instant
    val step = (days / abs(days)).toDuration(DurationUnit.HOURS)
    var newInstant = instant.plus(days.toDuration(DurationUnit.DAYS))
    // Tricky daylight savings: if adding whole days lands on the same wall-clock date (e.g. a DST
    // transition at local midnight), nudge by one hour at a time until the date actually moves. Must
    // accumulate onto newInstant — reassigning from `instant` here would loop forever.
    while (isSameGregorianDay(instant, newInstant)) {
        newInstant = newInstant.plus(step)
    }
    return newInstant
}

/**
 * Extra whole-day shift for the Hijri date when the user opts into the Islamic convention that the
 * day begins at maghrib (sunset). Returns 1 once [now] reaches today's [maghrib], else 0.
 *
 * Used only where the displayed date always tracks the current day (e.g. the widget); the
 * interactive home screen does not apply this, since its day can be navigated independently.
 */
fun maghribHijriDayShift(
    now: Instant,
    maghrib: Instant?,
    enabled: Boolean,
): Int {
    if (!enabled || maghrib == null) return 0
    return if (now >= maghrib) 1 else 0
}

private val icuCalendarPrototypes = ConcurrentHashMap<Pair<String, String>, Calendar>()

/**
 * A fresh ICU calendar of type [calendar] in the default time zone. Cloned from a cached prototype:
 * creating one resolves locale and calendar data, which dominated scans checking many dates.
 */
internal fun icuCalendar(calendar: String): Calendar {
    val zone = TimeZone.getDefault()
    val prototype = icuCalendarPrototypes.getOrPut(calendar to zone.id) {
        val requested = runCatching { Calendar.getInstance(zone, ULocale("@calendar=$calendar")) }.getOrNull()
        // Some OEM ICU builds reject an Islamic variant, or quietly return a Gregorian calendar for it;
        // degrade to plain "islamic" as [formatInstant] does, so date math and formatting agree.
        if (requested != null && (requested.type == calendar || !calendar.startsWith("islamic"))) {
            requested
        } else {
            Calendar.getInstance(zone, ULocale("@calendar=islamic"))
        }
    }
    return prototype.clone() as Calendar
}

/**
 * Returns one [Instant] (local noon, to stay clear of DST boundaries) for every day of the
 * [calendar]-month that contains [anchor]. Month length follows the given calendar, so a Hijri
 * month yields 29/30 days while a Gregorian one yields 28-31.
 */
fun calendarMonthDays(
    anchor: Instant,
    calendar: String,
): List<Instant> {
    val cal = icuCalendar(calendar)
    cal.timeInMillis = anchor.toEpochMilliseconds()
    cal.set(Calendar.HOUR_OF_DAY, 12)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val firstDay = cal.getActualMinimum(Calendar.DAY_OF_MONTH)
    val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return (firstDay..lastDay).map { day ->
        cal.set(Calendar.DAY_OF_MONTH, day)
        Instant.fromEpochMilliseconds(cal.timeInMillis)
    }
}

fun addCalendarMonths(
    instant: Instant,
    months: Int,
    calendar: String,
): Instant {
    val cal = icuCalendar(calendar)
    cal.timeInMillis = instant.toEpochMilliseconds()
    cal.add(Calendar.MONTH, months)
    return Instant.fromEpochMilliseconds(cal.timeInMillis)
}

fun isSameCalendarMonth(
    a: Instant,
    b: Instant,
    calendar: String,
): Boolean =
    formatInstant(a, "en", calendar, DateFormat.YEAR_NUM_MONTH) ==
        formatInstant(b, "en", calendar, DateFormat.YEAR_NUM_MONTH)

fun isSameGregorianDay(
    a: Instant,
    b: Instant,
): Boolean =
    formatInstant(a, "en", "gregorian", DateFormat.YEAR_NUM_MONTH_DAY) ==
        formatInstant(b, "en", "gregorian", DateFormat.YEAR_NUM_MONTH_DAY)

/** Local calendar date of [instant] in the system time zone — the stable key for a skipped occurrence. */
fun Instant.toLocalDate(): LocalDate = toLocalDateTime(KotlinxTimeZone.currentSystemDefault()).date

fun getDayBeginning(instant: Instant): Instant {
    val utcInstant = instant.toJavaInstant()
    val zdt = utcInstant.atZone(ZoneId.systemDefault())
    val startOfDayZdt = zdt.withHour(0).withMinute(0).withSecond(0).withNano(0)
    val utcStartInstant = startOfDayZdt.toInstant()
    return Instant.fromEpochSeconds(utcStartInstant.epochSecond, utcStartInstant.nano.toLong())
}
